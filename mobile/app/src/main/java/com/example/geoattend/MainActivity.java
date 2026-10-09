package com.example.geoattend;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.geoattend.databinding.ActivityMainBinding;
import com.example.geoattend.ui.UiAnimationHelper;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private TextView tvLogoTitle;
    private TextView tvTopBarCounter;

    private Handler counterHandler = new Handler(Looper.getMainLooper());
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;

    private Runnable counterRunnable = new Runnable() {
        @Override
        public void run() {
            SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            boolean isClockedIn = prefs.getBoolean("is_clocked_in", false);
            String checkInTimeStr = prefs.getString("last_check_in_time", null);

            if (isClockedIn && checkInTimeStr != null) {
                long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                if (isToday(checkInMs)) {
                    long elapsedSeconds = (System.currentTimeMillis() - checkInMs) / 1000;
                    if (elapsedSeconds < 0) elapsedSeconds = 0;
                    long hours = elapsedSeconds / 3600;
                    long minutes = (elapsedSeconds % 3600) / 60;
                    long seconds = elapsedSeconds % 60;
                    String formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);

                    if (tvTopBarCounter != null) {
                        tvTopBarCounter.setText("(" + formatted + ")");
                        tvTopBarCounter.setVisibility(View.VISIBLE);
                    }
                    counterHandler.postDelayed(this, 1000);
                    return;
                }
            }

            if (tvTopBarCounter != null) {
                tvTopBarCounter.setVisibility(View.GONE);
            }
        }
    };

    private boolean isToday(long timestampMs) {
        Calendar today = Calendar.getInstance();
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(timestampMs);

        return today.get(Calendar.YEAR) == time.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == time.get(Calendar.DAY_OF_YEAR);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // reset the flag every clean boot
        getSharedPreferences("AnimationTracker", MODE_PRIVATE).edit().clear().apply();

        // Create notification channels
        NotificationHelper.createNotificationChannels(this);

        // 1. Inflate view binding matching our single unified layout file
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        setContentView(binding.getRoot());

        UiAnimationHelper.setLightStatusIcons(getWindow());

        tvLogoTitle = findViewById(R.id.tvLogoTitle); // Initialized your text view

        // --- ATTACH REUSABLE ANIMATIONS VIA THE HELPER CLASS ---
        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        android.view.View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        tvTopBarCounter = findViewById(R.id.tvTopBarCounter);

        // 3. Setup Side Drawer Menu Architecture
        if (binding.navView != null) {
            mAppBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_home_pulse, R.id.nav_attendance_history, R.id.nav_profile_security)
                    .setOpenableLayout(binding.drawerLayout)
                    .build();

            binding.navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_secure) {
                    startActivity(new Intent(MainActivity.this, SecureActivity.class));
                    if (binding.drawerLayout != null) binding.drawerLayout.close();
                    return true;
                } else if (id == R.id.nav_support) {
                    startActivity(new Intent(MainActivity.this, SupportActivity.class));
                    if (binding.drawerLayout != null) binding.drawerLayout.close();
                    return true;
                } else if (id == R.id.nav_help) {
                    startActivity(new Intent(MainActivity.this, HelpActivity.class));
                    if (binding.drawerLayout != null) binding.drawerLayout.close();
                    return true;
                }

                androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.mainViewPager);
                if (viewPager != null) {
                    if (id == R.id.nav_home_pulse) {
                        viewPager.setCurrentItem(0, true);
                        if (binding.drawerLayout != null) binding.drawerLayout.close();
                        return true;
                    } else if (id == R.id.nav_attendance_history) {
                        viewPager.setCurrentItem(1, true);
                        if (binding.drawerLayout != null) binding.drawerLayout.close();
                        return true;
                    } else if (id == R.id.nav_profile_security) {
                        viewPager.setCurrentItem(2, true);
                        if (binding.drawerLayout != null) binding.drawerLayout.close();
                        return true;
                    }
                }
                return false;
            });

            updateDrawerHeader();
        }

        // 4. Setup ViewPager2 swipe gesture adapter & bindings
        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.mainViewPager);
        if (viewPager != null) {
            viewPager.setAdapter(new MainPagerAdapter(this));
            viewPager.setOffscreenPageLimit(2);

            viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    updateTabVisuals(position);
                    animateIndicator(position);
                }
            });

            View tabHome = findViewById(R.id.tabHome);
            View tabHistory = findViewById(R.id.tabHistory);
            View tabProfile = findViewById(R.id.tabProfile);

            if (tabHome != null) {
                tabHome.setOnClickListener(v -> viewPager.setCurrentItem(0, true));
            }
            if (tabHistory != null) {
                tabHistory.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
            }
            if (tabProfile != null) {
                tabProfile.setOnClickListener(v -> viewPager.setCurrentItem(2, true));
            }
        }

        // 5. Open Navigation Drawer cleanly on menu/back button click
        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v -> {
                if (binding.drawerLayout != null) {
                    binding.drawerLayout.open();
                }
            });
        }

        // 6. Monitor SharedPreferences changes for live updates
        preferenceChangeListener = (sharedPreferences, key) -> {
            if ("is_clocked_in".equals(key) || "last_check_in_time".equals(key)) {
                counterHandler.removeCallbacks(counterRunnable);
                counterHandler.post(counterRunnable);
            } else if ("full_name".equals(key) || "email".equals(key)) {
                updateDrawerHeader();
            }
        };
        getSharedPreferences("UserSession", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        // Start counter on load
        counterHandler.post(counterRunnable);

        // Edge-to-Edge System Bar Margins & Paddings via Window Insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.customTopBar, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
            android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = insets.top + dpToPx(60);
            v.setLayoutParams(lp);
            return windowInsets;
        });

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.customBottomBar, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom + dpToPx(16);
            v.setLayoutParams(mlp);
            return windowInsets;
        });

        // Pre-request permissions if they are not already granted
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        java.util.List<String> permissionsNeeded = new java.util.ArrayList<>();
        permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissionsNeeded.add(android.Manifest.permission.CAMERA);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS);
        }

        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
        for (String perm : permissionsNeeded) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            StringBuilder message = new StringBuilder("Welcome to Geo-Attend!\n\nTo provide a seamless attendance tracking experience, we need the following permissions:\n\n");
            
            boolean needsLoc = permissionsToRequest.contains(android.Manifest.permission.ACCESS_FINE_LOCATION);
            boolean needsCam = permissionsToRequest.contains(android.Manifest.permission.CAMERA);
            boolean needsNotif = false;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                needsNotif = permissionsToRequest.contains(android.Manifest.permission.POST_NOTIFICATIONS);
            }

            if (needsLoc) {
                message.append("• Location Permission: Used to verify your presence inside the campus boundaries during check-in/out.\n\n");
            }
            if (needsCam) {
                message.append("• Camera Permission: Used to capture your face profile for secure biometric check-in verification.\n\n");
            }
            if (needsNotif) {
                message.append("• Notifications Permission: Used to send shift start reminders, countdown timers, and check-in confirmation updates.\n\n");
            }

            message.append("Please approve the permission requests on the next screen to proceed.");

            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage(message.toString())
                    .setPositiveButton("Proceed", (dialog, which) -> {
                        String[] targetArray = permissionsToRequest.toArray(new String[0]);
                        androidx.core.app.ActivityCompat.requestPermissions(this, targetArray, 100);
                    })
                    .setNegativeButton("Not Now", null)
                    .show();
        }
    }

    private void updateDrawerHeader() {
        if (binding.navView != null) {
            View headerView = binding.navView.getHeaderView(0);
            if (headerView != null) {
                TextView tvNavHeaderName = headerView.findViewById(R.id.tvNavHeaderName);
                TextView tvNavHeaderSubflight = headerView.findViewById(R.id.tvNavHeaderSubflight);

                SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                String fullName = prefs.getString("full_name", "Employee");
                String email = prefs.getString("email", "employee@company.com");

                if (tvNavHeaderName != null) tvNavHeaderName.setText(fullName);
                if (tvNavHeaderSubflight != null) tvNavHeaderSubflight.setText(email);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (preferenceChangeListener != null) {
            getSharedPreferences("UserSession", MODE_PRIVATE)
                    .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }
        counterHandler.removeCallbacks(counterRunnable);
    }

    public void selectTab(int index) {
        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.mainViewPager);
        if (viewPager != null) {
            viewPager.setCurrentItem(index, true);
        }
    }

    private static class MainPagerAdapter extends androidx.viewpager2.adapter.FragmentStateAdapter {
        public MainPagerAdapter(@androidx.annotation.NonNull androidx.fragment.app.FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @androidx.annotation.NonNull
        @Override
        public androidx.fragment.app.Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new com.example.geoattend.HomeFragment();
                case 1:
                    return new com.example.geoattend.HistoryFragment();
                case 2:
                default:
                    return new com.example.geoattend.ProfileFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    private void updateTabVisuals(int activeIndex) {
        ImageView ivTabHome = findViewById(R.id.ivTabHome);
        TextView tvTabHome = findViewById(R.id.tvTabHome);
        ImageView ivTabHistory = findViewById(R.id.ivTabHistory);
        TextView tvTabHistory = findViewById(R.id.tvTabHistory);
        ImageView ivTabProfile = findViewById(R.id.ivTabProfile);
        TextView tvTabProfile = findViewById(R.id.tvTabProfile);

        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(this, R.color.text_secondary);

        if (ivTabHome != null) ivTabHome.setImageTintList(android.content.res.ColorStateList.valueOf(activeIndex == 0 ? activeColor : inactiveColor));
        if (tvTabHome != null) tvTabHome.setTextColor(activeIndex == 0 ? activeColor : inactiveColor);

        if (ivTabHistory != null) ivTabHistory.setImageTintList(android.content.res.ColorStateList.valueOf(activeIndex == 1 ? activeColor : inactiveColor));
        if (tvTabHistory != null) tvTabHistory.setTextColor(activeIndex == 1 ? activeColor : inactiveColor);

        if (ivTabProfile != null) ivTabProfile.setImageTintList(android.content.res.ColorStateList.valueOf(activeIndex == 2 ? activeColor : inactiveColor));
        if (tvTabProfile != null) tvTabProfile.setTextColor(activeIndex == 2 ? activeColor : inactiveColor);
    }

    private void animateIndicator(int index) {
        View customBottomBar = findViewById(R.id.customBottomBar);
        View indicator = findViewById(R.id.vNavigationIndicator);
        if (customBottomBar == null || indicator == null) return;

        customBottomBar.post(() -> {
            int width = customBottomBar.getWidth() - dpToPx(16); // paddingHorizontal of FrameLayout
            int tabWidth = width / 3;

            ViewGroup.LayoutParams lp = indicator.getLayoutParams();
            if (lp.width != tabWidth - dpToPx(16)) {
                lp.width = tabWidth - dpToPx(16);
                indicator.setLayoutParams(lp);
            }

            float targetX = index * tabWidth + dpToPx(8);
            indicator.animate()
                    .translationX(targetX)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        });
    }

    @Override
    public void onConfigurationChanged(@androidx.annotation.NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.mainViewPager);
        if (viewPager != null) {
            animateIndicator(viewPager.getCurrentItem());
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}