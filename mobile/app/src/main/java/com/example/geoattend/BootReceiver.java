package com.example.geoattend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            
            // Reschedule Shift Start reminder if enabled
            boolean startEnabled = prefs.getBoolean("shift_start_reminder", false);
            String savedTime = prefs.getString("shift_start_time", null);
            if (startEnabled && savedTime != null) {
                String[] parts = savedTime.split(":");
                if (parts.length == 2) {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    NotificationHelper.scheduleShiftStartReminder(context, hour, minute);
                }
            }

            // Reschedule Shift End reminder if enabled and clocked in
            boolean endReminderEnabled = prefs.getBoolean("shift_end_reminder", false);
            boolean isClockedIn = prefs.getBoolean("is_clocked_in", false);
            String checkInTimeStr = prefs.getString("last_check_in_time", null);
            int durationHours = prefs.getInt("shift_duration_hours", 8);
            if (endReminderEnabled && isClockedIn && checkInTimeStr != null) {
                long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                NotificationHelper.scheduleShiftEndReminder(context, checkInMs, durationHours);
            }
        }
    }
}
