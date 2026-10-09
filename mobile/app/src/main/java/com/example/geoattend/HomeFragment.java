package com.example.geoattend;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.graphics.Bitmap;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.util.Base64;
import android.widget.ProgressBar;
import java.io.ByteArrayOutputStream;
import android.widget.TextView;

import java.security.Signature;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private boolean isLocationAllowed = false;
    private boolean isBiometricVerifying = false;
    private boolean isCurrentlyCheckedIn = false;

    // Core Map Components
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;

    // Layout Views
    private MaterialCardView cardLocationStatus;
    private TextView tvLocationStatusText;
    private ImageView ivLocationStatusIcon;
    private ProgressBar pbMapLoading;
    private TextView tvMapStatus;

    // Date & Time, Office Location Views
    private TextView tvCurrentDate;
    private TextView tvCurrentTime;
    private TextView tvOfficeAddress;

    // Dual Biometric Selection Views
    private View layoutVerifyFace;
    private View layoutVerifyFingerprint;

    // Overlay Biometric Views
    private View overlayBiometricContainer;
    private View cardFaceVerification;
    private View cardFingerprintVerification;
    private TextureView cameraTextureView;
    private View layoutCameraFallback;
    private View viewScannerLine;
    private ImageView btnCloseFaceVerification;
    private ImageView btnCloseFingerprintVerification;

    // Camera and Clock references
    private android.hardware.Camera mCamera;
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Runnable clockRunnable;

    // Campus Boundaries Telemetry
    private static class CampusGeofence {
        int id;
        String name;
        double latitude;
        double longitude;
        double radius;

        CampusGeofence(int id, String name, double latitude, double longitude, double radius) {
            this.id = id;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
            this.radius = radius;
        }
    }

    private final List<CampusGeofence> campusGeofences = new ArrayList<>();
    private final List<Circle> mapCircles = new ArrayList<>();
    private Location lastKnownLocation = null;

    // Custom helper method delegating to the unified IndicatorHelper snackbar system
    private void showErrorMessage(String message, int length) {
        View container = getView();
        if (container != null) {
            String lower = message.toLowerCase();
            if (lower.contains("success") || lower.contains("verified") || lower.contains("done")) {
                com.example.geoattend.ui.IndicatorHelper.showSuccess(container, message);
            } else if (lower.contains("fail") || lower.contains("error") || lower.contains("denied") 
                    || lower.contains("invalid") || lower.contains("cancel") || lower.contains("required")
                    || lower.contains("invalidated")) {
                com.example.geoattend.ui.IndicatorHelper.showError(container, message);
            } else {
                com.example.geoattend.ui.IndicatorHelper.showInfo(container, message);
            }
        }
    }

    // Permission Request Handler
    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                enableLiveLocation();
            } else {
                com.example.geoattend.utils.PermissionGuideHelper.showLocationGuide(getContext());
            }
        });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                if (layoutCameraFallback != null) layoutCameraFallback.setVisibility(View.GONE);
                openCameraPreview();
            } else {
                if (layoutCameraFallback != null) {
                    layoutCameraFallback.setVisibility(View.VISIBLE);
                    TextView tvFallback = layoutCameraFallback.findViewById(R.id.tvCameraFallbackText);
                    if (tvFallback != null) {
                        tvFallback.setText("Camera permission denied.\nTap to open Settings.");
                    }
                }
                com.example.geoattend.utils.PermissionGuideHelper.showCameraGuide(getContext());
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1. Initialize Interactive UI Components
        cardLocationStatus = view.findViewById(R.id.cardLocationStatus);
        tvLocationStatusText = view.findViewById(R.id.tvLocationStatusText);
        ivLocationStatusIcon = view.findViewById(R.id.ivLocationStatusIcon);

        tvCurrentDate = view.findViewById(R.id.tvCurrentDate);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvOfficeAddress = view.findViewById(R.id.tvOfficeAddress);

        layoutVerifyFace = view.findViewById(R.id.layoutVerifyFace);
        layoutVerifyFingerprint = view.findViewById(R.id.layoutVerifyFingerprint);

        overlayBiometricContainer = view.findViewById(R.id.overlayBiometricContainer);
        cardFaceVerification = view.findViewById(R.id.cardFaceVerification);
        cardFingerprintVerification = view.findViewById(R.id.cardFingerprintVerification);
        cameraTextureView = view.findViewById(R.id.cameraTextureView);
        layoutCameraFallback = view.findViewById(R.id.layoutCameraFallback);
        viewScannerLine = view.findViewById(R.id.viewScannerLine);
        btnCloseFaceVerification = view.findViewById(R.id.btnCloseFaceVerification);
        btnCloseFingerprintVerification = view.findViewById(R.id.btnCloseFingerprintVerification);

        pbMapLoading = view.findViewById(R.id.pbMapLoading);
        tvMapStatus = view.findViewById(R.id.tvMapStatus);

        // 2. Setup Google Maps and Fused Location Provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        mapView = view.findViewById(R.id.mapBackground);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // 3. Refresh Location and Boundaries on Pill Click
        cardLocationStatus.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                refreshLocationAndBoundaries();
            }
        });

        // 4. Biometric Click Handlers
        layoutVerifyFace.setOnClickListener(v -> {
            if (isBiometricVerifying) return;
            startBiometricCheckIn(true);
        });

        layoutVerifyFingerprint.setOnClickListener(v -> {
            if (isBiometricVerifying) return;
            startBiometricCheckIn(false);
        });

        overlayBiometricContainer.setOnClickListener(v -> {
            closeBiometricOverlay();
        });

        cardFaceVerification.setOnClickListener(v -> {
            // consume click to prevent dim overlay from closing it
        });

        cardFingerprintVerification.setOnClickListener(v -> {
            // consume click to prevent dim overlay from closing it
        });

        btnCloseFaceVerification.setOnClickListener(v -> closeBiometricOverlay());
        btnCloseFingerprintVerification.setOnClickListener(v -> closeBiometricOverlay());

        // 5. Setup Clock Runnable
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTimeUI();
                clockHandler.postDelayed(this, 1000);
            }
        };

        if (layoutCameraFallback != null) {
            layoutCameraFallback.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    com.example.geoattend.utils.PermissionGuideHelper.showCameraGuide(getContext());
                }
            });
        }

        // Edge-to-Edge boundary adjustments & dynamic system bar layout scaling
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int topPadding = insets.top + dpToPx(60);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), v.getPaddingBottom());
            
            View scrollView = v.findViewById(R.id.homeNestedScrollView);
            if (scrollView != null) {
                int bottomPadding = insets.bottom + dpToPx(72 + 24);
                scrollView.setPadding(scrollView.getPaddingLeft(), scrollView.getPaddingTop(), scrollView.getPaddingRight(), bottomPadding);
            }
            return windowInsets;
        });

        // 6. Paint Initial Layout States
        updateLocationPillUI(false, "", 0.0);
        updateDateTimeUI();

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Strip default Zoom buttons for an elegant background aesthetic
        googleMap.getUiSettings().setZoomControlsEnabled(false);

        // Listen for when tiles finish loading to discard the loader view smoothly
        googleMap.setOnMapLoadedCallback(() -> {
            if (pbMapLoading != null) pbMapLoading.setVisibility(View.GONE);
            if (tvMapStatus != null) tvMapStatus.setVisibility(View.GONE);
        });

        // Verify Device Permissions before pulling coordinates
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableLiveLocation();
        } else {
            checkLocationStatus();
        }

        // Fetch boundaries from API
        fetchCampusBoundaries();
    }

    @SuppressLint("MissingPermission")
    private void enableLiveLocation() {
        if (googleMap == null) return;

        // Render the default blue location dot indicator directly inside map background layer
        googleMap.setMyLocationEnabled(true);

        // Fetch the last known tracking coordinates and pan the camera instantly
        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location != null) {
                lastKnownLocation = location;
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f)); // 16f street-level view
                checkLocationStatus();
            }
        });
    }

    private void fetchCampusBoundaries() {
        if (getContext() == null) return;
        String url = Config.getApiUrl(getContext()) + "/campus-boundaries";
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONArray arr = new JSONArray(response);
                    campusGeofences.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        if (obj.getBoolean("is_active")) {
                            campusGeofences.add(new CampusGeofence(
                                    obj.getInt("id"),
                                    obj.getString("location_name"),
                                    obj.getDouble("center_latitude"),
                                    obj.getDouble("center_longitude"),
                                    obj.getDouble("radius_meters")
                            ));
                        }
                    }
                    drawBoundariesOnMap();
                    checkLocationStatus();
                } catch (Exception e) {
                    showErrorMessage("Error parsing boundaries: " + e.getMessage(), Snackbar.LENGTH_SHORT);
                }
            }

            @Override
            public void onError(Exception e) {
                showErrorMessage("Failed to load boundaries: " + e.getMessage(), Snackbar.LENGTH_SHORT);
            }
        });
    }

    private void drawBoundariesOnMap() {
        if (googleMap == null) return;

        // Remove existing circles
        for (Circle circle : mapCircles) {
            circle.remove();
        }
        mapCircles.clear();

        // Draw new circles
        for (CampusGeofence geofence : campusGeofences) {
            LatLng center = new LatLng(geofence.latitude, geofence.longitude);
            Circle circle = googleMap.addCircle(new CircleOptions()
                    .center(center)
                    .radius(geofence.radius)
                    .strokeColor(ContextCompat.getColor(requireContext(), R.color.primary_alpha_88))
                    .strokeWidth(3f)
                    .fillColor(ContextCompat.getColor(requireContext(), R.color.primary_alpha_33)));
            mapCircles.add(circle);
        }
    }

    private void checkLocationStatus() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isLocationAllowed = false;
            updateLocationPillUI(false, "", -1.0);
            return;
        }

        if (lastKnownLocation == null) {
            isLocationAllowed = false;
            updateLocationPillUI(false, "", -2.0);
            return;
        }

        isLocationAllowed = false;
        String nearestName = "";
        double minDistance = Double.MAX_VALUE;

        for (CampusGeofence geofence : campusGeofences) {
            float[] results = new float[1];
            Location.distanceBetween(
                    lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                    geofence.latitude, geofence.longitude,
                    results
            );
            double distance = results[0];

            if (distance <= geofence.radius) {
                isLocationAllowed = true;
                nearestName = geofence.name;
                minDistance = distance;
                break;
            } else {
                if (distance < minDistance) {
                    minDistance = distance;
                    nearestName = geofence.name;
                }
            }
        }

        updateLocationPillUI(isLocationAllowed, nearestName, minDistance);
        updateOfficeLocationUI();
    }

    private void refreshLocationAndBoundaries() {
        if (getContext() == null) return;
        showErrorMessage("Refreshing coordinates...", Snackbar.LENGTH_SHORT);
        fetchCampusBoundaries();
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (googleMap != null) {
                try {
                    googleMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null) {
                    lastKnownLocation = location;
                    checkLocationStatus();
                }
            });
        } else {
            checkLocationStatus();
        }
    }

    private void updateLocationPillUI(boolean allowed, String siteName, double distance) {
        if (allowed) {
            cardLocationStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.success_card_bg));
            tvLocationStatusText.setText("LOCATION VERIFIED: " + siteName.toUpperCase());
            tvLocationStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_text));
            ivLocationStatusIcon.setImageResource(R.drawable.ic_check);
            ivLocationStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.success_text));
        } else {
            cardLocationStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.error_card_bg));
            if (distance == -1.0) {
                tvLocationStatusText.setText("LOCATION PERMISSION DENIED (TAP TO ENABLE)");
                tvLocationStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text));
                ivLocationStatusIcon.setImageResource(R.drawable.ic_close);
                ivLocationStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_text));
            } else if (distance == -2.0) {
                tvLocationStatusText.setText("WAITING FOR GPS LOCK...");
                tvLocationStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text));
                ivLocationStatusIcon.setImageResource(R.drawable.ic_send);
                ivLocationStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_text));
            } else {
                if (siteName.isEmpty()) {
                    tvLocationStatusText.setText("OUT OF ALLOWED LOCATION");
                } else {
                    tvLocationStatusText.setText(String.format(Locale.getDefault(), "OUTSIDE %s (%.0fm)", siteName.toUpperCase(), distance));
                }
                tvLocationStatusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_text));
                ivLocationStatusIcon.setImageResource(R.drawable.ic_close);
                ivLocationStatusIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.error_text));
            }
        }
    }

    private void performCheckIn(String userId) {
        if (getContext() == null || lastKnownLocation == null) return;

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("user_id", userId);
            bodyJson.put("device_latitude", lastKnownLocation.getLatitude());
            bodyJson.put("device_longitude", lastKnownLocation.getLongitude());

            String url = Config.getApiUrl(getContext()) + "/check-in";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    if (getContext() == null) return;
                    try {
                        JSONObject logObj = new JSONObject(response);
                        String status = logObj.getString("status");
                        String siteName = logObj.getString("location_name");
                        double distance = logObj.getDouble("calculated_distance");

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        if ("verified".equalsIgnoreCase(status)) {
                            builder.setTitle("Attendance Verified ✅");
                            builder.setMessage(String.format(Locale.getDefault(), "Successfully checked in at %s.\nDistance: %.1fm", siteName, distance));
                        } else {
                            builder.setTitle("Flagged Out-of-Bounds ⚠️");
                            builder.setMessage(String.format(Locale.getDefault(), "Attendance logged but FLAGGED as out-of-bounds from %s.\nDistance: %.1fm", siteName, distance));
                        }
                        builder.setPositiveButton("OK", null);
                        builder.show();

                        // Refresh location status after punch
                        refreshLocationAndBoundaries();
                        fetchTodayAttendanceStatus();
                    } catch (Exception e) {
                        showErrorMessage("Check-in response error: " + e.getMessage(), Snackbar.LENGTH_LONG);
                    }
                }

                @Override
                public void onError(Exception e) {
                    showErrorMessage("Check-in failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                }
            });
        } catch (Exception e) {
            showErrorMessage("Error preparing check-in payload: " + e.getMessage(), Snackbar.LENGTH_SHORT);
        }
    }

    private void updateOfficeLocationUI() {
        if (tvOfficeAddress == null) return;
        if (campusGeofences.isEmpty()) {
            tvOfficeAddress.setText("No active campus boundaries");
            return;
        }

        if (lastKnownLocation != null) {
            CampusGeofence nearest = null;
            double minDistance = Double.MAX_VALUE;
            for (CampusGeofence geofence : campusGeofences) {
                float[] results = new float[1];
                Location.distanceBetween(
                        lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(),
                        geofence.latitude, geofence.longitude,
                        results
                );
                double distance = results[0];
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = geofence;
                }
            }
            if (nearest != null) {
                tvOfficeAddress.setText(nearest.name);
            } else {
                tvOfficeAddress.setText(campusGeofences.get(0).name);
            }
        } else {
            tvOfficeAddress.setText(campusGeofences.get(0).name);
        }
    }

    private void updateDateTimeUI() {
        if (getContext() == null) return;
        java.util.Calendar cal = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.getDefault());
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault());

        String dateStr = dateFormat.format(cal.getTime());
        String timeStr = timeFormat.format(cal.getTime());

        if (tvCurrentDate != null) tvCurrentDate.setText(dateStr);
        if (tvCurrentTime != null) tvCurrentTime.setText(timeStr);
    }

    private void startBiometricCheckIn(boolean isFace) {
        if (getContext() == null) return;

        SharedPreferences prefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        boolean faceRegistered = prefs.getBoolean("face_registered", false);
        boolean fingerprintRegistered = prefs.getBoolean("fingerprint_registered", false);

        if (isFace && !faceRegistered) {
            showRegistrationRequiredDialog("Face Profile");
            return;
        }
        if (!isFace && !fingerprintRegistered) {
            showRegistrationRequiredDialog("Fingerprint Profile");
            return;
        }

        String userId = prefs.getString("user_id", null);
        if (userId == null) {
            showErrorMessage("Error: User session not found. Please log in again.", Snackbar.LENGTH_LONG);
            return;
        }

        if (lastKnownLocation == null) {
            showErrorMessage("Error: Waiting for GPS location lock.", Snackbar.LENGTH_SHORT);
            return;
        }

        isBiometricVerifying = true;

        if (isFace) {
            overlayBiometricContainer.setVisibility(View.VISIBLE);
            cardFaceVerification.setVisibility(View.VISIBLE);
            cardFingerprintVerification.setVisibility(View.GONE);
            startScannerAnimation();
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                if (layoutCameraFallback != null) layoutCameraFallback.setVisibility(View.GONE);
                openCameraPreview();
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }

            ProgressBar pbFace = cardFaceVerification.findViewById(R.id.pbFaceVerifying);
            TextView tvFace = cardFaceVerification.findViewById(R.id.tvFaceStatus);
            pbFace.setVisibility(View.VISIBLE);
            tvFace.setText("Verifying face profile...");
            tvFace.setTextColor(ContextCompat.getColor(requireContext(), R.color.on_surface));

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded()) return;
                captureFaceAndCheckIn(userId);
            }, 2000);

        } else {
            overlayBiometricContainer.setVisibility(View.GONE);
            cardFaceVerification.setVisibility(View.GONE);
            cardFingerprintVerification.setVisibility(View.GONE);

            // If KeyStore key pair is not generated, auto-enroll first
            if (!BiometricSecurityHelper.isKeyGenerated()) {
                enrollBiometrics(userId, isFace);
            } else {
                getChallengeAndAuthenticate(userId, isFace);
            }
        }
    }

    private void showRegistrationRequiredDialog(String biometricType) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Registration Required")
                .setMessage("You have not registered your " + biometricType + " yet. Please register it from your Profile settings first.")
                .setPositiveButton("Go to Profile", (dialog, which) -> {
                    try {
                        closeBiometricOverlay();
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).selectTab(2);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    closeBiometricOverlay();
                })
                .setOnCancelListener(dialog -> {
                    closeBiometricOverlay();
                })
                .show();
    }

    private void captureFaceAndCheckIn(String userId) {
        if (cameraTextureView == null || !cameraTextureView.isAvailable()) {
            showErrorMessage("Camera not ready.", Snackbar.LENGTH_SHORT);
            closeBiometricOverlay();
            return;
        }

        try {
            Bitmap bitmap = cameraTextureView.getBitmap();
            if (bitmap == null) {
                throw new Exception("Captured frame bitmap is null");
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
            byte[] bytes = outputStream.toByteArray();
            String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

            double lat = lastKnownLocation.getLatitude();
            double lon = lastKnownLocation.getLongitude();

            JSONObject body = new JSONObject();
            body.put("user_id", userId);
            body.put("device_latitude", lat);
            body.put("device_longitude", lon);
            body.put("face_image", base64Image);

            TextView tvFace = cardFaceVerification.findViewById(R.id.tvFaceStatus);
            tvFace.setText("Verifying face recognition with server...");

            String endpoint = isCurrentlyCheckedIn ? "/check-out-face" : "/check-in-face";
            String url = Config.getApiUrl(getContext()) + endpoint;
            HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String checkInResponse) {
                    if (getActivity() == null) return;
                    try {
                        JSONObject logObj = new JSONObject(checkInResponse);
                        String status = logObj.getString("status");
                        String siteName = logObj.getString("location_name");
                        double distance = logObj.getDouble("calculated_distance");

                        getActivity().runOnUiThread(() -> {
                            ProgressBar pbFace = cardFaceVerification.findViewById(R.id.pbFaceVerifying);
                            pbFace.setVisibility(View.GONE);
                            tvFace.setText("Face Verified ✅");
                            tvFace.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_text));

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                if (!isAdded()) return;
                                closeBiometricOverlay();

                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                String actionWord = isCurrentlyCheckedIn ? "checked out" : "checked in";
                                String titleWord = isCurrentlyCheckedIn ? "Clock Out Completed" : "Attendance Verified";
                                if ("verified".equalsIgnoreCase(status)) {
                                    builder.setTitle(titleWord + " ✅");
                                    builder.setMessage(String.format(Locale.getDefault(), "Successfully %s at %s.\nDistance: %.1fm", actionWord, siteName, distance));
                                } else {
                                    builder.setTitle("Flagged Out-of-Bounds ⚠️");
                                    builder.setMessage(String.format(Locale.getDefault(), "Attendance logged but FLAGGED as out-of-bounds from %s.\nDistance: %.1fm", siteName, distance));
                                }
                                builder.setPositiveButton("OK", null);
                                builder.show();

                                refreshLocationAndBoundaries();
                                fetchTodayAttendanceStatus();
                            }, 800);
                        });
                    } catch (Exception e) {
                        getActivity().runOnUiThread(() -> {
                            showErrorMessage("Check-in parsing error: " + e.getMessage(), Snackbar.LENGTH_LONG);
                            closeBiometricOverlay();
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        showErrorMessage("Face verification check-in failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                        closeBiometricOverlay();
                    });
                }
            });

        } catch (Exception e) {
            showErrorMessage("Error capturing face preview: " + e.getMessage(), Snackbar.LENGTH_SHORT);
            closeBiometricOverlay();
        }
    }

    private void enrollBiometrics(String userId, boolean isFace) {
        try {
            BiometricSecurityHelper.generateKeyPair();
            String pubKeyBase64 = BiometricSecurityHelper.getPublicKeyBase64();

            JSONObject body = new JSONObject();
            body.put("user_id", userId);
            body.put("public_key", pubKeyBase64);

            String url = Config.getApiUrl(getContext()) + "/register-biometric";
            HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        showErrorMessage("Biometrics registered successfully.", Snackbar.LENGTH_SHORT);
                        getChallengeAndAuthenticate(userId, isFace);
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        showErrorMessage("Failed to register public key on backend: " + e.getMessage(), Snackbar.LENGTH_LONG);
                        closeBiometricOverlay();
                    });
                }
            });
        } catch (Exception e) {
            showErrorMessage("KeyStore error: " + e.getMessage(), Snackbar.LENGTH_LONG);
            closeBiometricOverlay();
        }
    }

    private void getChallengeAndAuthenticate(String userId, boolean isFace) {
        if (getContext() == null) return;
        String url = Config.getApiUrl(getContext()) + "/get-biometric-challenge/" + userId;
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() == null) return;
                try {
                    JSONObject obj = new JSONObject(response);
                    String challenge = obj.getString("challenge");
                    getActivity().runOnUiThread(() -> {
                        authenticateWithBiometrics(userId, challenge, isFace);
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(() -> {
                        showErrorMessage("Failed to parse challenge: " + e.getMessage(), Snackbar.LENGTH_SHORT);
                        closeBiometricOverlay();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showErrorMessage("Failed to fetch challenge: " + e.getMessage(), Snackbar.LENGTH_SHORT);
                    closeBiometricOverlay();
                });
            }
        });
    }

    private void authenticateWithBiometrics(String userId, String challenge, boolean isFace) {
        Signature signature = BiometricSecurityHelper.getSignatureInstance();
        if (signature == null) {
            // Key pair was invalidated due to biometric changes. Deleting and re-enrolling.
            showErrorMessage("Key Store credentials invalidated. Re-enrolling...", Snackbar.LENGTH_SHORT);
            BiometricSecurityHelper.deleteKey();
            enrollBiometrics(userId, isFace);
            return;
        }

        BiometricSecurityHelper.showBiometricPrompt(this, signature, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showErrorMessage("Biometric cancel: " + errString, Snackbar.LENGTH_SHORT);
                    closeBiometricOverlay();
                });
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (getActivity() == null) return;

                try {
                    getActivity().runOnUiThread(() -> {
                        if (isFace) {
                            TextView tvFace = cardFaceVerification.findViewById(R.id.tvFaceStatus);
                            tvFace.setText("Verifying signature with server...");
                        } else {
                            showErrorMessage("Verifying attendance signature...", Snackbar.LENGTH_INDEFINITE);
                        }
                    });

                    // Construct verification payload
                    java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
                    df.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                    String timestamp = df.format(new java.util.Date());

                    double lat = lastKnownLocation.getLatitude();
                    double lon = lastKnownLocation.getLongitude();
                    String latStr = String.format(Locale.US, "%.6f", lat);
                    String lonStr = String.format(Locale.US, "%.6f", lon);
                    String payload = challenge + ":" + timestamp + ":" + latStr + ":" + lonStr;

                    // Cryptographic signature compute using BiometricPrompt result
                    Signature sig = result.getCryptoObject().getSignature();
                    sig.update(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    byte[] sigBytes = sig.sign();
                    String signatureBase64 = Base64.encodeToString(sigBytes, Base64.NO_WRAP);

                    JSONObject body = new JSONObject();
                    body.put("user_id", userId);
                    body.put("device_latitude", lat);
                    body.put("device_longitude", lon);
                    body.put("challenge", challenge);
                    body.put("timestamp", timestamp);
                    body.put("signature", signatureBase64);

                    String endpoint = isCurrentlyCheckedIn ? "/check-out-biometric" : "/check-in-biometric";
                    String url = Config.getApiUrl(getContext()) + endpoint;
                    HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                        @Override
                        public void onSuccess(String checkInResponse) {
                            if (getActivity() == null) return;
                            try {
                                JSONObject logObj = new JSONObject(checkInResponse);
                                String status = logObj.getString("status");
                                String siteName = logObj.getString("location_name");
                                double distance = logObj.getDouble("calculated_distance");

                                getActivity().runOnUiThread(() -> {
                                    if (isFace) {
                                        ProgressBar pbFace = cardFaceVerification.findViewById(R.id.pbFaceVerifying);
                                        TextView tvFace = cardFaceVerification.findViewById(R.id.tvFaceStatus);
                                        pbFace.setVisibility(View.GONE);
                                        tvFace.setText("Face Verified ✅");
                                        tvFace.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_text));
                                    } else {
                                        ProgressBar pbFingerprint = cardFingerprintVerification.findViewById(R.id.pbFingerprintVerifying);
                                        TextView tvFingerprint = cardFingerprintVerification.findViewById(R.id.tvFingerprintStatus);
                                        pbFingerprint.setVisibility(View.GONE);
                                        tvFingerprint.setText("Fingerprint Verified ✅");
                                        tvFingerprint.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_text));
                                    }

                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        if (!isAdded()) return;
                                        closeBiometricOverlay();

                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        String actionWord = isCurrentlyCheckedIn ? "checked out" : "checked in";
                                        String titleWord = isCurrentlyCheckedIn ? "Clock Out Completed" : "Attendance Verified";
                                        if ("verified".equalsIgnoreCase(status)) {
                                            builder.setTitle(titleWord + " ✅");
                                            builder.setMessage(String.format(Locale.getDefault(), "Successfully %s at %s.\nDistance: %.1fm", actionWord, siteName, distance));
                                        } else {
                                            builder.setTitle("Flagged Out-of-Bounds ⚠️");
                                            builder.setMessage(String.format(Locale.getDefault(), "Attendance logged but FLAGGED as out-of-bounds from %s.\nDistance: %.1fm", siteName, distance));
                                        }
                                        builder.setPositiveButton("OK", null);
                                        builder.show();

                                        refreshLocationAndBoundaries();
                                        fetchTodayAttendanceStatus();
                                    }, 800);
                                });
                            } catch (Exception e) {
                                getActivity().runOnUiThread(() -> {
                                    showErrorMessage("Check-in parsing error: " + e.getMessage(), Snackbar.LENGTH_LONG);
                                    closeBiometricOverlay();
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                showErrorMessage("Check-in failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                                closeBiometricOverlay();
                            });
                        }
                    });
                } catch (Exception e) {
                    getActivity().runOnUiThread(() -> {
                        showErrorMessage("Signature signing failed: " + e.getMessage(), Snackbar.LENGTH_LONG);
                        closeBiometricOverlay();
                    });
                }
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
    }

    private void openCameraPreview() {
        try {
            if (mCamera != null) {
                releaseCamera();
            }
            mCamera = android.hardware.Camera.open(android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT);
            if (mCamera == null) {
                mCamera = android.hardware.Camera.open(android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK);
            }

            if (mCamera != null && cameraTextureView != null) {
                cameraTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(@NonNull android.graphics.SurfaceTexture surface, int width, int height) {
                        try {
                            if (mCamera != null) {
                                mCamera.setPreviewTexture(surface);
                                setCameraDisplayOrientation(mCamera);
                                mCamera.startPreview();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (layoutCameraFallback != null) layoutCameraFallback.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(@NonNull android.graphics.SurfaceTexture surface, int width, int height) {}

                    @Override
                    public boolean onSurfaceTextureDestroyed(@NonNull android.graphics.SurfaceTexture surface) {
                        releaseCamera();
                        return true;
                    }

                    @Override
                    public void onSurfaceTextureUpdated(@NonNull android.graphics.SurfaceTexture surface) {}
                });

                if (cameraTextureView.isAvailable()) {
                    mCamera.setPreviewTexture(cameraTextureView.getSurfaceTexture());
                    setCameraDisplayOrientation(mCamera);
                    mCamera.startPreview();
                }
            } else {
                if (layoutCameraFallback != null) layoutCameraFallback.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (layoutCameraFallback != null) layoutCameraFallback.setVisibility(View.VISIBLE);
        }
    }

    private void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startScannerAnimation() {
        if (viewScannerLine == null) return;
        viewScannerLine.setVisibility(View.VISIBLE);

        float density = getResources().getDisplayMetrics().density;
        float translateDistance = 220 * density;

        android.view.animation.TranslateAnimation animation = new android.view.animation.TranslateAnimation(
                0, 0,
                0, translateDistance
        );
        animation.setDuration(1200);
        animation.setRepeatCount(android.view.animation.Animation.INFINITE);
        animation.setRepeatMode(android.view.animation.Animation.REVERSE);
        viewScannerLine.startAnimation(animation);

        View viewGlowRing = cardFaceVerification != null ? cardFaceVerification.findViewById(R.id.viewGlowRing) : null;
        if (viewGlowRing != null) {
            android.view.animation.ScaleAnimation pulse = new android.view.animation.ScaleAnimation(
                    0.96f, 1.04f,
                    0.96f, 1.04f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                    android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
            );
            pulse.setDuration(1200);
            pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
            pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
            viewGlowRing.startAnimation(pulse);
        }
    }

    private void stopScannerAnimation() {
        if (viewScannerLine != null) {
            viewScannerLine.clearAnimation();
            viewScannerLine.setVisibility(View.GONE);
        }
        View viewGlowRing = cardFaceVerification != null ? cardFaceVerification.findViewById(R.id.viewGlowRing) : null;
        if (viewGlowRing != null) {
            viewGlowRing.clearAnimation();
        }
    }

    private void closeBiometricOverlay() {
        if (overlayBiometricContainer != null) {
            overlayBiometricContainer.setVisibility(View.GONE);
        }
        releaseCamera();
        stopScannerAnimation();

        ImageView ivFingerprintPulse = cardFingerprintVerification != null ?
                cardFingerprintVerification.findViewById(R.id.ivFingerprintPulse) : null;
        if (ivFingerprintPulse != null) {
            ivFingerprintPulse.clearAnimation();
        }
        isBiometricVerifying = false;
    }

    // --- FORWARDED LIFE-CYCLE ROUTINES REQUIRED BY GOOGLE MAPS API ---
    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        refreshLocationAndBoundaries();
        fetchTodayAttendanceStatus();

        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
            clockHandler.post(clockRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();

        if (clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
        releaseCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    private void updateBiometricActionUI() {
        TextView tvActionHeader = getView() != null ? getView().findViewById(R.id.tvActionHeader) : null;
        TextView tvActionSubflight = getView() != null ? getView().findViewById(R.id.tvActionSubflight) : null;
        if (tvActionHeader != null) {
            tvActionHeader.setText(isCurrentlyCheckedIn ? "Clock Out" : "Clock In");
        }
        if (tvActionSubflight != null) {
            tvActionSubflight.setText(isCurrentlyCheckedIn ? "Select verification method to Clock Out" : "Select verification method");
        }
    }

    private void fetchTodayAttendanceStatus() {
        if (getContext() == null) return;
        SharedPreferences prefs = getContext().getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);
        if (userId == null) return;

        String url = Config.getApiUrl(getContext()) + "/users/" + userId + "/attendance-status";
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(response);
                    isCurrentlyCheckedIn = obj.getBoolean("can_clock_out");

                    boolean faceRegistered = obj.optBoolean("face_registered", false);
                    boolean fingerprintRegistered = obj.optBoolean("fingerprint_registered", false);
                    String checkInTimeStr = obj.optString("check_in_time", null);

                    SharedPreferences.Editor editor = prefs.edit()
                         .putBoolean("face_registered", faceRegistered)
                         .putBoolean("fingerprint_registered", fingerprintRegistered)
                         .putBoolean("is_clocked_in", isCurrentlyCheckedIn);

                    if (isCurrentlyCheckedIn && checkInTimeStr != null && !checkInTimeStr.equals("null")) {
                        editor.putString("last_check_in_time", checkInTimeStr);
                    } else {
                        editor.remove("last_check_in_time");
                    }
                    editor.apply();

                    boolean isShiftEndEnabled = prefs.getBoolean("shift_end_countdown", false);
                    if (isShiftEndEnabled && isCurrentlyCheckedIn && checkInTimeStr != null && !checkInTimeStr.equals("null")) {
                        long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                        NotificationHelper.showShiftEndCounterNotification(getContext(), checkInMs);
                    } else {
                        NotificationHelper.cancelShiftEndCounterNotification(getContext());
                    }

                    boolean isShiftEndReminderEnabled = prefs.getBoolean("shift_end_reminder", false);
                    if (isShiftEndReminderEnabled && isCurrentlyCheckedIn && checkInTimeStr != null && !checkInTimeStr.equals("null")) {
                        long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                        int durationHours = prefs.getInt("shift_duration_hours", 8);
                        NotificationHelper.scheduleShiftEndReminder(getContext(), checkInMs, durationHours);
                    } else {
                        NotificationHelper.cancelShiftEndReminder(getContext());
                    }

                    updateBiometricActionUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mCamera != null) {
            try {
                setCameraDisplayOrientation(mCamera);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return dp;
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setCameraDisplayOrientation(android.hardware.Camera camera) {
        if (camera == null || getActivity() == null) return;
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        int cameraId = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case android.view.Surface.ROTATION_0: degrees = 0; break;
            case android.view.Surface.ROTATION_90: degrees = 90; break;
            case android.view.Surface.ROTATION_180: degrees = 180; break;
            case android.view.Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}