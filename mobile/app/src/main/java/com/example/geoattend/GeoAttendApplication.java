package com.example.geoattend;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

public class GeoAttendApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        SharedPreferences themePrefs = getSharedPreferences("ThemeSettings", Context.MODE_PRIVATE);
        boolean isDark = themePrefs.getBoolean("dark_mode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
