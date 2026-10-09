package com.example.geoattend;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class ShiftEndReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean enabled = prefs.getBoolean("shift_end_reminder", false);
        boolean isClockedIn = prefs.getBoolean("is_clocked_in", false);
        
        if (enabled && isClockedIn) {
            NotificationHelper.createNotificationChannels(context);
            NotificationHelper.showShiftEndReminderNotification(context);
        }
    }
}
