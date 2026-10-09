package com.example.geoattend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.geoattend.ui.IndicatorHelper;
import com.example.geoattend.ui.UiAnimationHelper; // Your custom helper package
import com.facebook.shimmer.ShimmerFrameLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import android.widget.Button;
import android.widget.ProgressBar;
import android.graphics.Color;
import java.util.TimeZone;

public class HistoryFragment extends Fragment {

    private final android.os.Handler progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateLiveShiftProgress();
            progressHandler.postDelayed(this, 1000);
        }
    };

    private View historyRootLayout; // Reference for background animation
    private ShimmerFrameLayout shimmerContainer;
    private View realContentContainer;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvCalendarGrid;
    private TextView tvMonthTitle;
    private TextView tvShiftProgress;
    private TextView tvStreakCount;
    private ProgressBar progressShiftTube;

    // Core Module Cards
    private View cardCalendarSurface;
    private View cardLiveShift;
    private View cardStreakMetric;
    private View cardProductivityTrend;
    private LinearLayout barChartContainer;

    private Calendar currentMonthCalendar;

    // Local Attendance Log helper
    private static class UserAttendanceLog {
        String id;
        String locationName;
        double distance;
        String status;
        int year, month, day;
        String timeFormatted; // e.g. "10:15 AM"
        String timeIso;
        String checkOutTime;
        String checkOutStatus;

        UserAttendanceLog(String id, String locationName, double distance, String status, String isoTime, String checkOutTime, String checkOutStatus) {
            this.id = id;
            this.locationName = locationName;
            this.distance = distance;
            this.status = status;
            this.timeIso = isoTime;
            this.checkOutTime = checkOutTime;
            this.checkOutStatus = checkOutStatus;
            try {
                long checkInTimeMs = NotificationHelper.parseIsoUtcToMillis(isoTime);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(checkInTimeMs);
                this.year = cal.get(Calendar.YEAR);
                this.month = cal.get(Calendar.MONTH);
                this.day = cal.get(Calendar.DAY_OF_MONTH);

                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                String ampm = hour >= 12 ? "PM" : "AM";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                this.timeFormatted = String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, ampm);
            } catch (Exception e) {
                this.year = 0;
                this.month = 0;
                this.day = 0;
                this.timeFormatted = "";
            }
        }

        public String getDurationFormatted() {
            if (checkOutTime == null || checkOutTime.equals("null") || checkOutTime.isEmpty()) {
                return "Active";
            }
            try {
                long checkInMs = NotificationHelper.parseIsoUtcToMillis(timeIso);
                long checkOutMs = NotificationHelper.parseIsoUtcToMillis(checkOutTime);
                long diffMs = checkOutMs - checkInMs;
                if (diffMs < 0) diffMs = 0;
                long diffSecs = diffMs / 1000;
                long hours = diffSecs / 3600;
                long mins = (diffSecs % 3600) / 60;
                return hours + "h " + mins + "m";
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }

    private final List<UserAttendanceLog> userLogs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        // 1. Bind Layout Containers
        historyRootLayout = view.findViewById(R.id.historyRootLayout);
        shimmerContainer = view.findViewById(R.id.shimmerContainer);
        realContentContainer = view.findViewById(R.id.realContentContainer);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        rvCalendarGrid = view.findViewById(R.id.rvCalendarGrid);
        
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(this::loadAllDashboardDataAsync);
            // Customize SwipeRefreshLayout colors to look premium
            swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.primary),
                ContextCompat.getColor(requireContext(), R.color.blue_medium)
            );
        }
        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        tvShiftProgress = view.findViewById(R.id.tvShiftProgress);
        tvStreakCount = view.findViewById(R.id.tvStreakCount);

        cardCalendarSurface = view.findViewById(R.id.cardCalendarSurface);
        cardLiveShift = view.findViewById(R.id.cardLiveShift);
        cardStreakMetric = view.findViewById(R.id.cardStreakMetric);
        cardProductivityTrend = view.findViewById(R.id.cardProductivityTrend);
        barChartContainer = view.findViewById(R.id.barChartContainer);
        progressShiftTube = view.findViewById(R.id.progressShiftTube);

        Button btnPrev = view.findViewById(R.id.btnPrev);
        Button btnNext = view.findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(v -> {
            Calendar minLimit = Calendar.getInstance();
            minLimit.add(Calendar.MONTH, -6);
            
            Calendar temp = (Calendar) currentMonthCalendar.clone();
            temp.add(Calendar.MONTH, -1);
            
            if (temp.get(Calendar.YEAR) > minLimit.get(Calendar.YEAR) ||
                (temp.get(Calendar.YEAR) == minLimit.get(Calendar.YEAR) && temp.get(Calendar.MONTH) >= minLimit.get(Calendar.MONTH))) {
                currentMonthCalendar.add(Calendar.MONTH, -1);
                buildAndBindCalendar();
            } else {
                IndicatorHelper.showError(getView(), "Cannot view history older than 6 months");
            }
        });

        btnNext.setOnClickListener(v -> {
            Calendar maxLimit = Calendar.getInstance();
            maxLimit.add(Calendar.MONTH, 6);
            
            Calendar temp = (Calendar) currentMonthCalendar.clone();
            temp.add(Calendar.MONTH, 1);
            
            if (temp.get(Calendar.YEAR) < maxLimit.get(Calendar.YEAR) ||
                (temp.get(Calendar.YEAR) == maxLimit.get(Calendar.YEAR) && temp.get(Calendar.MONTH) <= maxLimit.get(Calendar.MONTH))) {
                currentMonthCalendar.add(Calendar.MONTH, 1);
                buildAndBindCalendar();
            } else {
                IndicatorHelper.showError(getView(), "Cannot view history further than 6 months");
            }
        });

        // 2. ATTACH THE BACKGROUND BLOB ANIMATION FREELY RIGHT AWAY
        UiAnimationHelper.attachBackgroundAnimation(historyRootLayout);

        // Configure Layout grid management
        rvCalendarGrid.setLayoutManager(new GridLayoutManager(getContext(), 7));
        rvCalendarGrid.setHasFixedSize(true);

        currentMonthCalendar = Calendar.getInstance();
        shimmerContainer.startShimmer();
        loadAllDashboardDataAsync();

        // Edge-to-Edge boundary adjustments & dynamic system bar layout scaling
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int topPadding = insets.top + dpToPx(60);
            int bottomPadding = insets.bottom + dpToPx(72 + 24);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), bottomPadding);
            return windowInsets;
        });

        return view;
    }

    private void loadAllDashboardDataAsync() {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        if (userId == null) {
            IndicatorHelper.showError(getView(), "Error: User session not found.");
            return;
        }

        String url = Config.getApiUrl(getContext()) + "/attendance-logs/user/" + userId;
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                try {
                    JSONArray arr = new JSONArray(response);
                    userLogs.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        userLogs.add(new UserAttendanceLog(
                                obj.getString("id"),
                                obj.getString("location_name"),
                                obj.getDouble("calculated_distance"),
                                obj.getString("status"),
                                obj.getString("check_in_time"),
                                obj.optString("check_out_time", ""),
                                obj.optString("check_out_status", "")
                        ));
                    }

                    shimmerContainer.stopShimmer();
                    shimmerContainer.setVisibility(View.GONE);
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                        swipeRefreshLayout.setVisibility(View.VISIBLE);
                    }
                    realContentContainer.setVisibility(View.VISIBLE);

                    buildAndBindCalendar();
                    populateProductivityTrendGraph();
                    runStaggeredEntranceWave();
                    updateStreakAndShiftStats();
                } catch (Exception e) {
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                    IndicatorHelper.showError(getView(), "Error parsing logs: " + e.getMessage());
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                IndicatorHelper.showError(getView(), "Failed to fetch attendance logs: " + e.getMessage());

                shimmerContainer.stopShimmer();
                shimmerContainer.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                    swipeRefreshLayout.setVisibility(View.VISIBLE);
                }
                realContentContainer.setVisibility(View.VISIBLE);
                buildAndBindCalendar();
                runStaggeredEntranceWave();
            }
        });
    }

    private void runStaggeredEntranceWave() {
        if (getContext() == null) return;

        // Use SharedPreferences to remember state even if the Fragment object is killed
        SharedPreferences animPrefs = getContext().getSharedPreferences("AnimationTracker", Context.MODE_PRIVATE);
        boolean hasAnimatedBefore = animPrefs.getBoolean("history_animated", false);

        if (hasAnimatedBefore) {
            return; // Exit right away without setting any animations
        }

        // Save true so it will skip next time
        animPrefs.edit().putBoolean("history_animated", true).apply();

        Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_wave);

        cardCalendarSurface.startAnimation(slideUp);
        slideUp.setStartOffset(100);
        cardLiveShift.startAnimation(slideUp);
        slideUp.setStartOffset(200);
        cardStreakMetric.startAnimation(slideUp);
        slideUp.setStartOffset(300);
        cardProductivityTrend.startAnimation(slideUp);

        for (int i = 0; i < barChartContainer.getChildCount(); i++) {
            View bar = barChartContainer.getChildAt(i);
            Animation barGrow = AnimationUtils.loadAnimation(getContext(), R.anim.item_animation_wave);
            barGrow.setStartOffset(400 + (i * 30));
            bar.startAnimation(barGrow);
        }
    }

    private void buildAndBindCalendar() {
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText(monthYearFormat.format(currentMonthCalendar.getTime()));

        List<CalendarDay> daysList = new ArrayList<>();
        Calendar cal = (Calendar) currentMonthCalendar.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeekColumn = cal.get(Calendar.DAY_OF_WEEK);
        int blankPaddingDays = firstDayOfWeekColumn - 2;
        if (blankPaddingDays < 0) {
            blankPaddingDays = 6;
        }

        for (int i = 0; i < blankPaddingDays; i++) {
            daysList.add(new CalendarDay("", "", "DEFAULT", false));
        }

        int totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar today = Calendar.getInstance();

        for (int dayNumber = 1; dayNumber <= totalDaysInMonth; dayNumber++) {
            boolean isToday = (today.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                    && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                    && today.get(Calendar.DAY_OF_MONTH) == dayNumber);

            String subtext = "";
            String status = "DEFAULT";

            List<UserAttendanceLog> logsOnThisDay = new ArrayList<>();
            for (UserAttendanceLog log : userLogs) {
                if (log.year == cal.get(Calendar.YEAR)
                        && log.month == cal.get(Calendar.MONTH)
                        && log.day == dayNumber) {
                    logsOnThisDay.add(log);
                }
            }

            if (!logsOnThisDay.isEmpty()) {
                boolean hasVerified = false;
                for (UserAttendanceLog log : logsOnThisDay) {
                    if ("verified".equalsIgnoreCase(log.status)) {
                        hasVerified = true;
                        break;
                    }
                }

                if (hasVerified) {
                    status = "PRESENT";
                    long totalDuration = getTotalDurationForDayMs(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), dayNumber);
                    if (totalDuration > 0) {
                        subtext = formatDuration(totalDuration);
                    } else {
                        boolean hasActive = false;
                        for (UserAttendanceLog log : logsOnThisDay) {
                            if ("verified".equalsIgnoreCase(log.status) && (log.checkOutTime == null || log.checkOutTime.isEmpty() || log.checkOutTime.equals("null"))) {
                                hasActive = true;
                                break;
                            }
                        }
                        subtext = hasActive ? "Active" : "0h 0m";
                    }
                } else {
                    status = "LEAVE";
                    subtext = "Flagged In";
                }
            } else {
                subtext = "";
                status = "DEFAULT";
            }

            if (isToday && subtext.isEmpty()) {
                subtext = "Today";
            }

            daysList.add(new CalendarDay(String.valueOf(dayNumber), subtext, status, isToday));
        }

        CalendarAdapter adapter = new CalendarAdapter(daysList);
        adapter.resetAnimationTracker();
        rvCalendarGrid.setAdapter(adapter);
    }

    private void updateStreakAndShiftStats() {
        if (getContext() == null) return;

        int streak = 0;
        Calendar checkCal = Calendar.getInstance();
        boolean hasStreak = true;

        for (int i = 0; i < 30; i++) {
            boolean foundVerified = false;
            for (UserAttendanceLog log : userLogs) {
                if (log.year == checkCal.get(Calendar.YEAR)
                        && log.month == checkCal.get(Calendar.MONTH)
                        && log.day == checkCal.get(Calendar.DAY_OF_MONTH)
                        && "verified".equalsIgnoreCase(log.status)) {
                    foundVerified = true;
                    break;
                }
            }

            if (!foundVerified) {
                if (i == 0) {
                    checkCal.add(Calendar.DAY_OF_MONTH, -1);
                    continue;
                } else {
                    hasStreak = false;
                }
            } else {
                streak++;
                checkCal.add(Calendar.DAY_OF_MONTH, -1);
            }

            if (!hasStreak) {
                break;
            }
        }

        if (tvStreakCount != null) {
            tvStreakCount.setText(streak + (streak == 1 ? " Day" : " Days"));
        }

        updateLiveShiftProgress();
    }

    private long getTotalDurationForDayMs(int year, int month, int day) {
        long totalMs = 0;
        Calendar today = Calendar.getInstance();
        boolean isToday = (today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month && today.get(Calendar.DAY_OF_MONTH) == day);

        for (UserAttendanceLog log : userLogs) {
            if (log.year == year && log.month == month && log.day == day) {
                if ("verified".equalsIgnoreCase(log.status)) {
                    long checkInMs = NotificationHelper.parseIsoUtcToMillis(log.timeIso);
                    if (log.checkOutTime != null && !log.checkOutTime.isEmpty() && !log.checkOutTime.equals("null")) {
                        if ("verified".equalsIgnoreCase(log.checkOutStatus)) {
                            long checkOutMs = NotificationHelper.parseIsoUtcToMillis(log.checkOutTime);
                            long diff = checkOutMs - checkInMs;
                            if (diff > 0) {
                                totalMs += diff;
                            }
                        }
                    } else if (isToday) {
                        long diff = System.currentTimeMillis() - checkInMs;
                        if (diff > 0) {
                            totalMs += diff;
                        }
                    }
                }
            }
        }

        if (isToday && totalMs == 0 && getContext() != null) {
            SharedPreferences sessionPrefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            boolean isClockedIn = sessionPrefs.getBoolean("is_clocked_in", false);
            if (isClockedIn) {
                String checkInTimeStr = sessionPrefs.getString("last_check_in_time", null);
                if (checkInTimeStr != null) {
                    long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                    Calendar checkInCal = Calendar.getInstance();
                    checkInCal.setTimeInMillis(checkInMs);
                    if (checkInCal.get(Calendar.YEAR) == year && checkInCal.get(Calendar.MONTH) == month && checkInCal.get(Calendar.DAY_OF_MONTH) == day) {
                        long diff = System.currentTimeMillis() - checkInMs;
                        if (diff > 0) {
                            totalMs += diff;
                        }
                    }
                }
            }
        }
        return totalMs;
    }

    private String formatDuration(long durationMs) {
        long secs = durationMs / 1000;
        long hours = secs / 3600;
        long mins = (secs % 3600) / 60;
        return hours + "h " + mins + "m";
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void populateProductivityTrendGraph() {
        if (getContext() == null || barChartContainer == null) return;

        SharedPreferences sessionPrefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int shiftDurationHours = sessionPrefs.getInt("shift_duration_hours", 8);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        SimpleDateFormat dayOfWeekFormat = new SimpleDateFormat("EEE", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            final int year = cal.get(Calendar.YEAR);
            final int month = cal.get(Calendar.MONTH);
            final int day = cal.get(Calendar.DAY_OF_MONTH);
            final String dayName = dayOfWeekFormat.format(cal.getTime());
            final String dateStr = dateFormat.format(cal.getTime());

            long durationMs = getTotalDurationForDayMs(year, month, day);
            double hours = durationMs / (1000.0 * 60.0 * 60.0);

            int heightDp = (int) (hours * 10);
            if (heightDp > 100) heightDp = 100;
            if (heightDp < 6) heightDp = 6;

            int heightPx = dpToPx(heightDp);

            if (i < barChartContainer.getChildCount()) {
                View bar = barChartContainer.getChildAt(i);
                ViewGroup.LayoutParams params = bar.getLayoutParams();
                params.height = heightPx;
                bar.setLayoutParams(params);

                if (hours >= shiftDurationHours) {
                    bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.success)));
                } else if (hours > 0) {
                    bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_medium)));
                } else {
                    bar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.gray_light)));
                }

                bar.setOnClickListener(v -> {
                    String durationFormatted = formatDuration(durationMs);
                    IndicatorHelper.showInfo(getView(), dayName + ", " + dateStr + ": " + durationFormatted + " spent");
                });
            }

            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateLiveShiftProgress() {
        if (getContext() == null) return;

        Calendar today = Calendar.getInstance();
        SharedPreferences sessionPrefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        int shiftDurationHours = sessionPrefs.getInt("shift_duration_hours", 8);
        long durationMs = (long) shiftDurationHours * 60 * 60 * 1000;

        boolean isClockedIn = sessionPrefs.getBoolean("is_clocked_in", false);
        boolean hasTodayLogs = false;
        if (isClockedIn) {
            hasTodayLogs = true;
        } else {
            for (UserAttendanceLog log : userLogs) {
                if (log.year == today.get(Calendar.YEAR)
                        && log.month == today.get(Calendar.MONTH)
                        && log.day == today.get(Calendar.DAY_OF_MONTH)) {
                    hasTodayLogs = true;
                    break;
                }
            }
        }

        if (hasTodayLogs) {
            long totalCompletedMs = getTotalDurationForDayMs(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
            long secs = totalCompletedMs / 1000;
            long hours = secs / 3600;
            long mins = (secs % 3600) / 60;

            String progressText = String.format(Locale.getDefault(), "%dh %02dm / %dh", hours, mins, shiftDurationHours);
            if (tvShiftProgress != null) {
                tvShiftProgress.setText(progressText);
            }

            int percent = (int) ((totalCompletedMs * 100) / durationMs);
            if (percent > 100) percent = 100;
            if (percent < 0) percent = 0;
            if (progressShiftTube != null) {
                progressShiftTube.setProgress(percent);
            }
        } else {
            String progressText = String.format(Locale.getDefault(), "0h 00m / %dh", shiftDurationHours);
            if (tvShiftProgress != null) {
                tvShiftProgress.setText(progressText);
            }
            if (progressShiftTube != null) {
                progressShiftTube.setProgress(0);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        progressHandler.post(progressRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressRunnable);
    }

}