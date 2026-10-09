package com.example.geoattend;

import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpHelper {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(String response);
        void onError(Exception e);
    }

    public static void get(final String urlString, final Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    final String result = response.toString();
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String result = "";
                    try {
                        java.io.InputStream errStream = conn.getErrorStream();
                        if (errStream != null) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8));
                            StringBuilder errResponse = new StringBuilder();
                            String line;
                            while ((line = in.readLine()) != null) {
                                errResponse.append(line);
                            }
                            in.close();
                            result = errResponse.toString();
                        }
                    } catch (Exception ignored) {}
                    String friendlyMessage = extractFriendlyDetail(result, responseCode);
                    final Exception e = new Exception(friendlyMessage);
                    mainHandler.post(() -> callback.onError(e));
                }
            } catch (final Exception e) {
                String friendlyMessage = e.getMessage();
                if (friendlyMessage != null && (friendlyMessage.contains("Unable to resolve host") || friendlyMessage.contains("ConnectException") || friendlyMessage.contains("SocketTimeoutException") || friendlyMessage.contains("route to host"))) {
                    friendlyMessage = "Unable to connect to the server. Please check your internet connection.";
                } else if (friendlyMessage == null) {
                    friendlyMessage = "A connection error occurred.";
                }
                final Exception friendlyException = new Exception(friendlyMessage, e);
                mainHandler.post(() -> callback.onError(friendlyException));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    public static void post(final String urlString, final String jsonBody, final Callback callback) {
        request("POST", urlString, jsonBody, callback);
    }

    public static void put(final String urlString, final String jsonBody, final Callback callback) {
        request("PUT", urlString, jsonBody, callback);
    }

    private static void request(final String method, final String urlString, final String jsonBody, final Callback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod(method);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Accept", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                BufferedReader in;
                if (responseCode >= 200 && responseCode < 300) {
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                } else {
                    in = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                final String result = response.toString();
                if (responseCode >= 200 && responseCode < 300) {
                    mainHandler.post(() -> callback.onSuccess(result));
                } else {
                    String friendlyMessage = extractFriendlyDetail(result, responseCode);
                    final Exception e = new Exception(friendlyMessage);
                    mainHandler.post(() -> callback.onError(e));
                }
            } catch (final Exception e) {
                String friendlyMessage = e.getMessage();
                if (friendlyMessage != null && (friendlyMessage.contains("Unable to resolve host") || friendlyMessage.contains("ConnectException") || friendlyMessage.contains("SocketTimeoutException") || friendlyMessage.contains("route to host"))) {
                    friendlyMessage = "Unable to connect to the server. Please check your internet connection.";
                } else if (friendlyMessage == null) {
                    friendlyMessage = "A connection error occurred.";
                }
                final Exception friendlyException = new Exception(friendlyMessage, e);
                mainHandler.post(() -> callback.onError(friendlyException));
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    private static String extractFriendlyDetail(String jsonResponse, int responseCode) {
        if (jsonResponse != null && !jsonResponse.isEmpty()) {
            try {
                int jsonStart = jsonResponse.indexOf("{");
                if (jsonStart != -1) {
                    String jsonStr = jsonResponse.substring(jsonStart);
                    org.json.JSONObject obj = new org.json.JSONObject(jsonStr);
                    if (obj.has("detail")) {
                        return obj.getString("detail");
                    }
                }
            } catch (Exception ignored) {}
        }
        return "Server error (" + responseCode + "). Please try again.";
    }
}
