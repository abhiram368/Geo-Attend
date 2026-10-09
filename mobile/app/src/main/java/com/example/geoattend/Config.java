package com.example.geoattend;

import android.content.Context;
import android.content.SharedPreferences;

public class Config {
    private static final String PREFS_NAME = "GeoAttendConfig";
    private static final String KEY_API_URL = "api_url";
    private static final String DEFAULT_API_URL = "http://10.0.2.2:8000";

    private static String apiUrl = null;

    public static String getApiUrl(Context context) {
        if (apiUrl == null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            apiUrl = prefs.getString(KEY_API_URL, DEFAULT_API_URL);
        }
        return apiUrl;
    }

    public static void setApiUrl(Context context, String newUrl) {
        apiUrl = newUrl;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_URL, newUrl).apply();
    }
}
