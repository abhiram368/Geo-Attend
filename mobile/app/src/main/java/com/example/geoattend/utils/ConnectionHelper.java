package com.example.geoattend.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import com.example.geoattend.Config;
import com.example.geoattend.HttpHelper;

public class ConnectionHelper {

    public interface ConnectionCallback {
        void onConnectionChecked(boolean hasInternet, boolean hasServer);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public static void checkServerConnection(Context context, ConnectionCallback callback) {
        if (!isNetworkAvailable(context)) {
            callback.onConnectionChecked(false, false);
            return;
        }

        // Send a lightweight GET request to backend URL
        String url = Config.getApiUrl(context);
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                callback.onConnectionChecked(true, true);
            }

            @Override
            public void onError(Exception e) {
                String msg = e.getMessage();
                boolean isServerDown = msg != null && (
                    msg.contains("Unable to connect") || 
                    msg.contains("failed to connect") || 
                    msg.contains("Connection refused") || 
                    msg.contains("timeout") ||
                    msg.contains("SocketTimeoutException")
                );
                callback.onConnectionChecked(true, !isServerDown);
            }
        });
    }
}
