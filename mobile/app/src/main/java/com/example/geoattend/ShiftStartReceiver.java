package com.example.geoattend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ShiftStartReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("shift_start_reminder", false);
        if (!enabled) return;

        // Show reminder notification
        NotificationHelper.createNotificationChannels(context);
        NotificationHelper.showShiftStartNotification(context);

        // Schedule next reminder for tomorrow
        String savedTime = prefs.getString("shift_start_time", null);
        if (savedTime != null) {
            String[] parts = savedTime.split(":");
            if (parts.length == 2) {
                int hour = Integer.parseInt(parts[0]);
                int minute = Integer.parseInt(parts[1]);
                NotificationHelper.scheduleShiftStartReminder(context, hour, minute);
            }
        }
    }
}
