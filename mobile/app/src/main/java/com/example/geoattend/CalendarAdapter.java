package com.example.geoattend;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import androidx.core.content.ContextCompat;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final List<CalendarDay> dayList;

    private int lastAnimatedPosition = -1;

    public CalendarAdapter(List<CalendarDay> dayList) {
        this.dayList = dayList;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarDay day = dayList.get(position);

        // Set Date Number text
        holder.tvDayNumber.setText(day.getDayNumber());
        holder.tvDaySubtext.setText(day.getSubtext());

        // Reset text and background colors first
        holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_dark));
        holder.tvDaySubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_muted));

        // Skip setup for blank padding days
        if (day.getDayNumber().isEmpty()) {
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
            return;
        }

        holder.itemView.setClickable(true);

        // 1. Set Custom background & text colors based on status
        switch (day.getStatus()) {
            case "PRESENT":
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_day_present);
                holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_text));
                holder.tvDaySubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_text));
                break;
            case "LEAVE":
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_day_leave);
                holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error_text));
                holder.tvDaySubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.error_text));
                break;
            case "HOLIDAY":
                holder.itemView.setBackgroundResource(R.drawable.bg_calendar_day_present);
                holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_text));
                holder.tvDaySubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.success_text));
                break;
            default:
                if (day.isToday()) {
                    holder.itemView.setBackgroundResource(R.drawable.bg_calendar_today_border);
                    holder.tvDayNumber.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
                }
                break;
        }

        // Set Click Action to show day information
        holder.itemView.setOnClickListener(v -> {
            String msg;
            switch (day.getStatus()) {
                case "PRESENT":
                    msg = "Day " + day.getDayNumber() + ": Present - Completed " + day.getSubtext();
                    break;
                case "LEAVE":
                    msg = "Day " + day.getDayNumber() + ": Flagged Check-In (" + day.getSubtext() + ")";
                    break;
                case "HOLIDAY":
                    msg = "Day " + day.getDayNumber() + ": Holiday (" + day.getSubtext() + ")";
                    break;
                default:
                    msg = "Day " + day.getDayNumber() + ": No attendance logs";
                    break;
            }
            com.example.geoattend.ui.IndicatorHelper.showInfo(holder.itemView, msg);
        });

        // APPLY THE HIGH-FIDELITY SCALE-IN ANIMATION HERE
        if (position > lastAnimatedPosition) {
            Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.anim_calendar_cell);
            // Stagger the animation: wait 15ms per index position for smooth grid cascading
            animation.setStartOffset(position * 15);
            holder.itemView.startAnimation(animation);
            lastAnimatedPosition = position;
        }

        // 2. Set Colored Status Pill Dots dynamically (kept for backwards compatibility if needed, otherwise hidden)
        View dot = holder.viewStatusDot;
        if (dot != null) {
            dot.setVisibility(View.GONE);
        }
    }

    public void resetAnimationTracker() {
        this.lastAnimatedPosition = -1;
    }

    @Override
    public int getItemCount() {
        return dayList.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber, tvDaySubtext;
        View viewStatusDot;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tvDayNumber);
            tvDaySubtext = itemView.findViewById(R.id.tvDaySubtext);
            viewStatusDot = itemView.findViewById(R.id.viewStatusDot);
        }
    }
}