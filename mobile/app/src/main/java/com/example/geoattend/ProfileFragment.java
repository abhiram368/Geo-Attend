package com.example.geoattend;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.example.geoattend.ui.IndicatorHelper;
import androidx.fragment.app.Fragment;
import java.io.ByteArrayOutputStream;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import java.util.Locale;
import androidx.biometric.BiometricPrompt;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvUserEmail;
    private TextView tvUserPhone;
    private TextView tvFaceStatus;
    private Button btnRegisterFace;
    private TextView tvFingerprintStatus;
    private Button btnRegisterFingerprint;
    private SharedPreferences sessionPrefs;
    private TextView tvShiftStartTime;
    private TextView tvShiftDurationValue;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    IndicatorHelper.showSuccess(getView(), "Notification permission granted.");
                } else {
                    com.example.geoattend.utils.PermissionGuideHelper.showNotificationGuide(getContext());
                }
                updatePermissionsUI();
            });

    private final ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    boolean isRegistered = sessionPrefs.getBoolean("face_registered", false);
                    showFaceCaptureDialog(isRegistered);
                } else {
                    com.example.geoattend.utils.PermissionGuideHelper.showCameraGuide(getContext());
                }
                updatePermissionsUI();
            });

    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    IndicatorHelper.showSuccess(getView(), "Location permission granted.");
                } else {
                    com.example.geoattend.utils.PermissionGuideHelper.showLocationGuide(getContext());
                }
                updatePermissionsUI();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserPhone = view.findViewById(R.id.tvUserPhone);
        
        TextView tvEditDetails = view.findViewById(R.id.tvEditDetailsLink);
        TextView btnGiveFeedback = view.findViewById(R.id.btnGiveFeedback);
        TextView btnResetPassword = view.findViewById(R.id.btnResetPassword);
        TextView tvSupportEmail = view.findViewById(R.id.tvSupportEmail);
        Button btnCopyEmail = view.findViewById(R.id.btnCopyEmail);
        
        SwitchCompat switchShiftStart = view.findViewById(R.id.switchShiftStart);
        SwitchCompat switchShiftEnd = view.findViewById(R.id.switchShiftEnd);
        tvShiftStartTime = view.findViewById(R.id.tvShiftStartTime);
        TextView btnLogout = view.findViewById(R.id.btnLogout);
        SwitchCompat switchShiftEndReminder = view.findViewById(R.id.switchShiftEndReminder);
        View layoutShiftDuration = view.findViewById(R.id.layoutShiftDuration);
        tvShiftDurationValue = view.findViewById(R.id.tvShiftDurationValue);

        sessionPrefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE);

        int currentDuration = sessionPrefs.getInt("shift_duration_hours", 8);
        tvShiftDurationValue.setText(currentDuration + " hours");
        switchShiftEndReminder.setChecked(sessionPrefs.getBoolean("shift_end_reminder", false));

        layoutShiftDuration.setOnClickListener(v -> showShiftDurationPickerDialog());

        switchShiftEndReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionPrefs.edit().putBoolean("shift_end_reminder", isChecked).apply();
            String status = isChecked ? "enabled" : "disabled";
            IndicatorHelper.showInfo(getView(), "Shift End Reminder " + status);

            if (isChecked) {
                checkNotificationPermission();
                boolean isClockedIn = sessionPrefs.getBoolean("is_clocked_in", false);
                String checkInTimeStr = sessionPrefs.getString("last_check_in_time", null);
                int durationHours = sessionPrefs.getInt("shift_duration_hours", 8);
                if (isClockedIn && checkInTimeStr != null) {
                    long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                    NotificationHelper.scheduleShiftEndReminder(getContext(), checkInMs, durationHours);
                }
            } else {
                NotificationHelper.cancelShiftEndReminder(getContext());
            }
        });

        // 1. Initial UI population from SharedPreferences session cache
        populateSessionUI();

        // 2. Fetch the latest user info from the backend to keep it synced
        fetchLatestUserProfile();

        // 3. Setup Reminder Switch toggles
        boolean startEnabled = sessionPrefs.getBoolean("shift_start_reminder", false);
        switchShiftStart.setChecked(startEnabled);
        switchShiftEnd.setChecked(sessionPrefs.getBoolean("shift_end_countdown", false));

        if (startEnabled) {
            String savedTime = sessionPrefs.getString("shift_start_time", null);
            if (savedTime != null) {
                String[] parts = savedTime.split(":");
                if (parts.length == 2) {
                    updateShiftStartTimeUI(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                }
            } else {
                tvShiftStartTime.setText("Reminder Time: Not Set");
                tvShiftStartTime.setVisibility(View.VISIBLE);
            }
        } else {
            tvShiftStartTime.setVisibility(View.GONE);
        }

        switchShiftStart.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionPrefs.edit().putBoolean("shift_start_reminder", isChecked).apply();
            String status = isChecked ? "enabled" : "disabled";
            IndicatorHelper.showInfo(getView(), "Shift Start Reminder " + status);

            if (isChecked) {
                checkNotificationPermission();
                String savedTime = sessionPrefs.getString("shift_start_time", null);
                if (savedTime != null) {
                    String[] parts = savedTime.split(":");
                    if (parts.length == 2) {
                        int h = Integer.parseInt(parts[0]);
                        int m = Integer.parseInt(parts[1]);
                        updateShiftStartTimeUI(h, m);
                        NotificationHelper.scheduleShiftStartReminder(getContext(), h, m);
                    }
                } else {
                    showShiftStartTimePickerDialog();
                }
            } else {
                tvShiftStartTime.setVisibility(View.GONE);
                NotificationHelper.cancelShiftStartReminder(getContext());
            }
        });

        tvShiftStartTime.setOnClickListener(v -> {
            showShiftStartTimePickerDialog();
        });

        switchShiftEnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionPrefs.edit().putBoolean("shift_end_countdown", isChecked).apply();
            String status = isChecked ? "enabled" : "disabled";
            IndicatorHelper.showInfo(getView(), "Shift End Countdown " + status);

            if (isChecked) {
                checkNotificationPermission();
                boolean isClockedIn = sessionPrefs.getBoolean("is_clocked_in", false);
                String checkInTimeStr = sessionPrefs.getString("last_check_in_time", null);
                if (isClockedIn && checkInTimeStr != null) {
                    long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                    NotificationHelper.showShiftEndCounterNotification(getContext(), checkInMs);
                }
            } else {
                NotificationHelper.cancelShiftEndCounterNotification(getContext());
            }
        });

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> handleLogout());
        }

        // 4. Register Action Listeners
        tvEditDetails.setOnClickListener(v -> showEditProfileDialog());
        btnGiveFeedback.setOnClickListener(v -> showGiveFeedbackDialog());
        btnResetPassword.setOnClickListener(v -> showResetPasswordDialog());
        
        tvFaceStatus = view.findViewById(R.id.tvFaceStatus);
        btnRegisterFace = view.findViewById(R.id.btnRegisterFace);
        tvFingerprintStatus = view.findViewById(R.id.tvFingerprintStatus);
        btnRegisterFingerprint = view.findViewById(R.id.btnRegisterFingerprint);

        btnRegisterFace.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                boolean isRegistered = sessionPrefs.getBoolean("face_registered", false);
                showFaceCaptureDialog(isRegistered);
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnRegisterFingerprint.setOnClickListener(v -> {
            boolean isRegistered = sessionPrefs.getBoolean("fingerprint_registered", false);
            handleFingerprintRegister(isRegistered);
        });

        // 5. Support Email Clipboard Action
        btnCopyEmail.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Support Email Address Label", tvSupportEmail.getText().toString());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                IndicatorHelper.showSuccess(getView(), "Developer email address copied to clipboard!");
            }
        });

        View layoutPermissionLocation = view.findViewById(R.id.layoutPermissionLocation);
        View layoutPermissionCamera = view.findViewById(R.id.layoutPermissionCamera);
        View layoutPermissionNotification = view.findViewById(R.id.layoutPermissionNotification);

        if (layoutPermissionLocation != null) {
            layoutPermissionLocation.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    IndicatorHelper.showInfo(getView(), "Location permission already granted.");
                } else {
                    requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            });
        }

        if (layoutPermissionCamera != null) {
            layoutPermissionCamera.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    IndicatorHelper.showInfo(getView(), "Camera permission already granted.");
                } else {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
            });
        }

        if (layoutPermissionNotification != null) {
            layoutPermissionNotification.setOnClickListener(v -> {
                boolean granted = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                } else {
                    granted = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
                }

                if (granted) {
                    IndicatorHelper.showInfo(getView(), "Notification permission already granted.");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        com.example.geoattend.utils.PermissionGuideHelper.showNotificationGuide(getContext());
                    }
                }
            });
        }

        // Edge-to-Edge boundary adjustments & dynamic system bar layout scaling
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int topPadding = insets.top + dpToPx(60);
            int bottomPadding = insets.bottom + dpToPx(72 + 24);
            v.setPadding(v.getPaddingLeft(), topPadding, v.getPaddingRight(), bottomPadding);
            return windowInsets;
        });

        return view;
    }

    private void populateSessionUI() {
        String name = sessionPrefs.getString("full_name", "Employee Name");
        String email = sessionPrefs.getString("email", "employee@company.com");
        String phone = sessionPrefs.getString("phone", "");
        
        tvUserName.setText(name);
        tvUserEmail.setText(email);
        tvUserPhone.setText(phone.isEmpty() ? "No phone number added" : phone);

        boolean faceReg = sessionPrefs.getBoolean("face_registered", false);
        boolean fingerReg = sessionPrefs.getBoolean("fingerprint_registered", false);

        if (tvFaceStatus != null) {
            tvFaceStatus.setText(faceReg ? "Registered" : "Not Registered");
            tvFaceStatus.setTextColor(ContextCompat.getColor(requireContext(), faceReg ? R.color.success_text : R.color.error_text));
        }
        if (btnRegisterFace != null) {
            btnRegisterFace.setText(faceReg ? "UPDATE" : "REGISTER");
        }

        if (tvFingerprintStatus != null) {
            tvFingerprintStatus.setText(fingerReg ? "Registered" : "Not Registered");
            tvFingerprintStatus.setTextColor(ContextCompat.getColor(requireContext(), fingerReg ? R.color.success_text : R.color.error_text));
        }
        if (btnRegisterFingerprint != null) {
            btnRegisterFingerprint.setText(fingerReg ? "UPDATE" : "REGISTER");
        }
    }

    private void fetchLatestUserProfile() {
        if (getContext() == null) return;
        String userId = sessionPrefs.getString("user_id", "");
        if (userId.isEmpty()) return;

        String url = Config.getApiUrl(getContext()) + "/users/" + userId;
        HttpHelper.get(url, new HttpHelper.Callback() {
            @Override
            public void onSuccess(String response) {
                if (!isAdded()) return;
                try {
                    JSONObject userObj = new JSONObject(response);
                    String name = userObj.getString("full_name");
                    String email = userObj.getString("email");
                    String phone = userObj.optString("phone", "");
                    boolean faceRegistered = userObj.optBoolean("face_registered", false);
                    boolean fingerprintRegistered = userObj.optBoolean("fingerprint_registered", false);
 
                    sessionPrefs.edit()
                            .putString("full_name", name)
                            .putString("email", email)
                            .putString("phone", phone)
                            .putBoolean("face_registered", faceRegistered)
                            .putBoolean("fingerprint_registered", fingerprintRegistered)
                            .apply();
 
                    populateSessionUI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                // Keep displaying whatever was loaded from cache
                e.printStackTrace();
            }
        });
    }

    private void showEditProfileDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
        }

        ImageView btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);
        TextInputEditText etFullName = dialogView.findViewById(R.id.etDialogFullName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        Button btnSubmit = dialogView.findViewById(R.id.btnDialogActionSubmit);

        // Pre-fill
        etFullName.setText(sessionPrefs.getString("full_name", ""));
        etPhone.setText(sessionPrefs.getString("phone", ""));

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String name = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";

            if (name.isEmpty()) {
                etFullName.setError("Name is required");
                etFullName.requestFocus();
                return;
            }

            btnSubmit.setEnabled(false);
            try {
                JSONObject body = new JSONObject();
                body.put("full_name", name);
                body.put("phone", phone);

                String userId = sessionPrefs.getString("user_id", "");
                String url = Config.getApiUrl(getContext()) + "/users/" + userId;

                HttpHelper.put(url, body.toString(), new HttpHelper.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        if (!isAdded()) return;
                        IndicatorHelper.showSuccess(getView(), "Profile updated successfully!");
                        fetchLatestUserProfile(); // Refreshes session and UI
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        btnSubmit.setEnabled(true);
                        IndicatorHelper.showError(getView(), "Failed to update profile: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                btnSubmit.setEnabled(true);
                IndicatorHelper.showError(getView(), "Error: " + e.getMessage());
            }
        });

        dialog.show();
    }

    private void showGiveFeedbackDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_submit_feedback, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
        }

        ImageView btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);
        TextInputEditText etFeedback = dialogView.findViewById(R.id.etDialogFeedback);
        Button btnSubmit = dialogView.findViewById(R.id.btnDialogActionSubmit);

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String message = etFeedback.getText() != null ? etFeedback.getText().toString().trim() : "";

            if (message.isEmpty()) {
                etFeedback.setError("Feedback message cannot be empty");
                etFeedback.requestFocus();
                return;
            }

            btnSubmit.setEnabled(false);
            try {
                JSONObject body = new JSONObject();
                body.put("user_id", sessionPrefs.getString("user_id", ""));
                body.put("message", message);

                String url = Config.getApiUrl(getContext()) + "/feedbacks";

                HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        if (!isAdded()) return;
                        IndicatorHelper.showSuccess(getView(), "Feedback submitted! Thank you.");
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        btnSubmit.setEnabled(true);
                        IndicatorHelper.showError(getView(), "Failed to send feedback: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                btnSubmit.setEnabled(true);
                IndicatorHelper.showError(getView(), "Error: " + e.getMessage());
            }
        });

        dialog.show();
    }

    private void showResetPasswordDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
        }

        ImageView btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etDialogNewPassword);
        Button btnSubmit = dialogView.findViewById(R.id.btnDialogActionSubmit);

        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        btnSubmit.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";

            if (newPassword.isEmpty() || newPassword.length() < 4) {
                etNewPassword.setError("Password must be at least 4 characters long");
                etNewPassword.requestFocus();
                return;
            }

            btnSubmit.setEnabled(false);
            try {
                JSONObject body = new JSONObject();
                body.put("password", newPassword);

                String userId = sessionPrefs.getString("user_id", "");
                String url = Config.getApiUrl(getContext()) + "/users/" + userId + "/password";

                HttpHelper.put(url, body.toString(), new HttpHelper.Callback() {
                    @Override
                    public void onSuccess(String response) {
                        if (!isAdded()) return;
                        IndicatorHelper.showSuccess(getView(), "Password reset successfully!");
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded()) return;
                        btnSubmit.setEnabled(true);
                        IndicatorHelper.showError(getView(), "Failed to reset password: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                btnSubmit.setEnabled(true);
                IndicatorHelper.showError(getView(), "Error: " + e.getMessage());
            }
        });

        dialog.show();
    }

    private void handleFingerprintRegister(boolean isUpdate) {
        if (getContext() == null) return;
        
        if (!BiometricSecurityHelper.isBiometricAvailable(getContext())) {
            IndicatorHelper.showError(getView(), "Biometric hardware not available or enrolled.");
            return;
        }

        try {
            // Delete old key first to generate a fresh one
            BiometricSecurityHelper.deleteKey();
            
            BiometricSecurityHelper.generateKeyPair();
            java.security.Signature signature = BiometricSecurityHelper.getSignatureInstance();
            if (signature == null) {
                IndicatorHelper.showError(getView(), "Failed to initialize signature.");
                return;
            }

            BiometricSecurityHelper.showBiometricPrompt(this, signature, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        IndicatorHelper.showError(getView(), "Biometric registration cancelled: " + errString);
                    });
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    if (getActivity() == null) return;

                    getActivity().runOnUiThread(() -> {
                        IndicatorHelper.showInfo(getView(), "Biometric verified. Registering with server...");
                        
                        try {
                            String publicKeyStr = BiometricSecurityHelper.getPublicKeyBase64();
                            String userId = sessionPrefs.getString("user_id", "");
                            
                            JSONObject body = new JSONObject();
                            body.put("user_id", userId);
                            
                            if (isUpdate) {
                                body.put("request_type", "fingerprint");
                                body.put("new_public_key", publicKeyStr);
                                
                                String url = Config.getApiUrl(getContext()) + "/request-biometric-update";
                                HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            IndicatorHelper.showSuccess(getView(), "Update request submitted for Admin approval.");
                                            fetchLatestUserProfile();
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            IndicatorHelper.showError(getView(), "Failed to request update: " + e.getMessage());
                                        });
                                    }
                                });
                            } else {
                                body.put("public_key", publicKeyStr);
                                String url = Config.getApiUrl(getContext()) + "/register-biometric";
                                HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                                    @Override
                                    public void onSuccess(String response) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            IndicatorHelper.showSuccess(getView(), "Fingerprint registered successfully!");
                                            fetchLatestUserProfile();
                                        });
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        if (getActivity() == null) return;
                                        getActivity().runOnUiThread(() -> {
                                            IndicatorHelper.showError(getView(), "Failed to register fingerprint: " + e.getMessage());
                                        });
                                    }
                                });
                            }
                        } catch (Exception e) {
                            IndicatorHelper.showError(getView(), "Payload error: " + e.getMessage());
                        }
                    });
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(getView(), "KeyStore error: " + e.getMessage());
        }
    }

    private Camera mCamera;
    private TextureView faceTextureView;

    private void showFaceCaptureDialog(boolean isUpdate) {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_face_capture, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setCanceledOnTouchOutside(false);
        }

        ImageView btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);
        faceTextureView = dialogView.findViewById(R.id.faceTextureView);
        TextView tvCaptureStatus = dialogView.findViewById(R.id.tvCaptureStatus);
        Button btnCapture = dialogView.findViewById(R.id.btnCaptureFace);

        View viewGlowRing = dialogView.findViewById(R.id.viewGlowRing);
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

        faceTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                try {
                    mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    if (mCamera == null) {
                        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    }
                    if (mCamera != null) {
                        mCamera.setPreviewTexture(surface);
                        setCameraDisplayOrientation(mCamera);
                        mCamera.startPreview();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    tvCaptureStatus.setText("Failed to start front camera preview.");
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {}

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                releaseCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {}
        });

        btnDismiss.setOnClickListener(v -> {
            if (viewGlowRing != null) {
                viewGlowRing.clearAnimation();
            }
            releaseCamera();
            dialog.dismiss();
        });

        btnCapture.setOnClickListener(v -> {
            if (faceTextureView == null || !faceTextureView.isAvailable()) {
                IndicatorHelper.showError(getView(), "Camera not ready.");
                return;
            }
            
            btnCapture.setEnabled(false);
            tvCaptureStatus.setText("Capturing and uploading frame...");

            try {
                Bitmap bitmap = faceTextureView.getBitmap();
                if (bitmap == null) {
                    throw new Exception("TextureView bitmap frame is null");
                }
                
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
                byte[] bytes = outputStream.toByteArray();
                String base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP);

                String userId = sessionPrefs.getString("user_id", "");
                JSONObject body = new JSONObject();
                body.put("user_id", userId);

                if (isUpdate) {
                    body.put("request_type", "face");
                    body.put("new_face_image", base64Image);
                    String url = Config.getApiUrl(getContext()) + "/request-biometric-update";
                    HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                IndicatorHelper.showSuccess(getView(), "Face update request submitted for Admin approval.");
                                releaseCamera();
                                fetchLatestUserProfile();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                btnCapture.setEnabled(true);
                                tvCaptureStatus.setText("Failed to upload frame. Try again.");
                                IndicatorHelper.showError(getView(), "Upload failed: " + e.getMessage());
                            });
                        }
                    });
                } else {
                    body.put("face_image", base64Image);
                    String url = Config.getApiUrl(getContext()) + "/register-face";
                    HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                IndicatorHelper.showSuccess(getView(), "Face profile registered successfully!");
                                releaseCamera();
                                fetchLatestUserProfile();
                                dialog.dismiss();
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            if (getActivity() == null) return;
                            getActivity().runOnUiThread(() -> {
                                btnCapture.setEnabled(true);
                                tvCaptureStatus.setText("Failed to register. Try again.");
                                IndicatorHelper.showError(getView(), "Registration failed: " + e.getMessage());
                            });
                        }
                    });
                }
            } catch (Exception e) {
                btnCapture.setEnabled(true);
                tvCaptureStatus.setText("Error capturing face. Try again.");
                IndicatorHelper.showError(getView(), "Error: " + e.getMessage());
            }
        });

        dialog.show();
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

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void showShiftStartTimePickerDialog() {
        if (getContext() == null) return;
        
        int defaultHour = 9;
        int defaultMinute = 0;
        String savedTime = sessionPrefs.getString("shift_start_time", null);
        if (savedTime != null) {
            String[] parts = savedTime.split(":");
            if (parts.length == 2) {
                defaultHour = Integer.parseInt(parts[0]);
                defaultMinute = Integer.parseInt(parts[1]);
            }
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
            sessionPrefs.edit().putString("shift_start_time", timeStr).apply();
            
            updateShiftStartTimeUI(hourOfDay, minute);
            
            NotificationHelper.scheduleShiftStartReminder(getContext(), hourOfDay, minute);
        }, defaultHour, defaultMinute, false);

        timePickerDialog.show();
    }

    private void updateShiftStartTimeUI(int hour, int minute) {
        if (tvShiftStartTime == null) return;
        String amPm = hour >= 12 ? "PM" : "AM";
        int hour12 = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        String formatted = String.format(Locale.getDefault(), "Reminder Time: %02d:%02d %s", hour12, minute, amPm);
        tvShiftStartTime.setText(formatted);
        tvShiftStartTime.setVisibility(View.VISIBLE);
    }

    private void showShiftDurationPickerDialog() {
        if (getContext() == null) return;

        String[] hoursOptions = new String[]{"4 hours", "5 hours", "6 hours", "7 hours", "8 hours", "9 hours", "10 hours", "11 hours", "12 hours"};
        int[] hoursValues = new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12};

        int currentHours = sessionPrefs.getInt("shift_duration_hours", 8);
        int selectedIndex = 4; // default 8 hours
        for (int i = 0; i < hoursValues.length; i++) {
            if (hoursValues[i] == currentHours) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Select Shift Duration")
                .setSingleChoiceItems(hoursOptions, selectedIndex, (dialog, which) -> {
                    int chosenHours = hoursValues[which];
                    sessionPrefs.edit().putInt("shift_duration_hours", chosenHours).apply();
                    tvShiftDurationValue.setText(chosenHours + " hours");
                    dialog.dismiss();
                    IndicatorHelper.showSuccess(getView(), "Shift duration set to " + chosenHours + " hours");

                    // Reschedule reminder if enabled and clocked in
                    boolean endReminderEnabled = sessionPrefs.getBoolean("shift_end_reminder", false);
                    boolean isClockedIn = sessionPrefs.getBoolean("is_clocked_in", false);
                    String checkInTimeStr = sessionPrefs.getString("last_check_in_time", null);
                    if (endReminderEnabled && isClockedIn && checkInTimeStr != null) {
                        long checkInMs = NotificationHelper.parseIsoUtcToMillis(checkInTimeStr);
                        NotificationHelper.scheduleShiftEndReminder(getContext(), checkInMs, chosenHours);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleLogout() {
        if (getContext() == null) return;
        
        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    NotificationHelper.cancelShiftStartReminder(getContext());
                    NotificationHelper.cancelShiftEndCounterNotification(getContext());
                    
                    sessionPrefs.edit().clear().apply();
                    
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePermissionsUI();
    }

    private void updatePermissionsUI() {
        if (getContext() == null || getView() == null) return;

        TextView tvLocation = getView().findViewById(R.id.tvLocationPermissionStatus);
        TextView tvCamera = getView().findViewById(R.id.tvCameraPermissionStatus);
        TextView tvNotification = getView().findViewById(R.id.tvNotificationPermissionStatus);

        boolean locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        boolean notificationGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            notificationGranted = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        }

        if (tvLocation != null) {
            tvLocation.setText(locationGranted ? "Allowed" : "Denied");
            tvLocation.setTextColor(ContextCompat.getColor(requireContext(), locationGranted ? R.color.success_text : R.color.error_text));
        }

        if (tvCamera != null) {
            tvCamera.setText(cameraGranted ? "Allowed" : "Denied");
            tvCamera.setTextColor(ContextCompat.getColor(requireContext(), cameraGranted ? R.color.success_text : R.color.error_text));
        }

        if (tvNotification != null) {
            tvNotification.setText(notificationGranted ? "Allowed" : "Denied");
            tvNotification.setTextColor(ContextCompat.getColor(requireContext(), notificationGranted ? R.color.success_text : R.color.error_text));
        }
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

    private void setCameraDisplayOrientation(Camera camera) {
        if (camera == null || getActivity() == null) return;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        Camera.getCameraInfo(cameraId, info);
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case android.view.Surface.ROTATION_0: degrees = 0; break;
            case android.view.Surface.ROTATION_90: degrees = 90; break;
            case android.view.Surface.ROTATION_180: degrees = 180; break;
            case android.view.Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}