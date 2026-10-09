package com.example.geoattend.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

public class PermissionGuideHelper {

    public static void showLocationGuide(Context context) {
        showGuide(context, 
            "Location Permission Required", 
            "To track your check-ins and verify you are inside the campus boundaries, Location permission is required.\n\n" +
            "Please follow these simple steps to enable it:\n" +
            "1. Click 'Go to Settings' below.\n" +
            "2. Tap on 'Permissions'.\n" +
            "3. Select 'Location' and choose 'Allow all the time' or 'Allow only while using the app'."
        );
    }

    public static void showCameraGuide(Context context) {
        showGuide(context, 
            "Camera Permission Required", 
            "To capture and verify your face profile for secure biometric sign-ins, Camera permission is required.\n\n" +
            "Please follow these simple steps to enable it:\n" +
            "1. Click 'Go to Settings' below.\n" +
            "2. Tap on 'Permissions'.\n" +
            "3. Toggle 'Camera' to allowed/active."
        );
    }

    public static void showNotificationGuide(Context context) {
        showGuide(context, 
            "Notification Permission Required", 
            "To receive daily shift start reminders, countdowns, and attendance verification notifications, Notification permission is required.\n\n" +
            "Please follow these simple steps to enable it:\n" +
            "1. Click 'Go to Settings' below.\n" +
            "2. Tap on 'Notifications'.\n" +
            "3. Toggle 'All Geo-Attend notifications' to active/allowed."
        );
    }

    private static void showGuide(Context context, String title, String instructions) {
        if (context == null) return;

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(instructions)
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                        intent.setData(uri);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
