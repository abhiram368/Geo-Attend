package com.example.geoattend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.geoattend.ui.IndicatorHelper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.geoattend.ui.UiAnimationHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private ImageButton btnBack;
    private TextView tvLogoTitle;
    private TextView tvLoginLink;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnCreateAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_register);

        UiAnimationHelper.setLightStatusIcons(getWindow());

        btnBack = findViewById(R.id.btnBack);
        tvLogoTitle = findViewById(R.id.tvLogoTitle);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvLoginLink != null) {
            tvLoginLink.setOnClickListener(v -> finish());
        }

        if (btnCreateAccount != null) {
            btnCreateAccount.setOnClickListener(v -> handleRegistration());
        }

        TextView tvSecure = findViewById(R.id.tvSecure);
        TextView tvSupport = findViewById(R.id.tvSupport);
        TextView tvHelp = findViewById(R.id.tvHelp);

        if (tvSecure != null) tvSecure.setOnClickListener(v -> startActivity(new Intent(this, SecureActivity.class)));
        if (tvSupport != null) tvSupport.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        if (tvHelp != null) tvHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        MaterialButton btnGoogle = findViewById(R.id.btnGoogle);
        String defaultWebClientId = "68282361730-g4p12b322a312q8c4c3b99912b3226a.apps.googleusercontent.com";

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        IndicatorHelper.showError(this, "Google Sign-In cancelled");
                    }
                }
        );

        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> {
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                    googleSignInLauncher.launch(signInIntent);
                });
            });
        }
    }

    private void handleRegistration() {
        final String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        final String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        final String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnCreateAccount.setEnabled(false);
        IndicatorHelper.showInfo(this, "Sending OTP verification...");

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);
            bodyJson.put("full_name", fullName);

            String url = Config.getApiUrl(this) + "/register/send-otp";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    btnCreateAccount.setEnabled(true);
                    showOtpVerificationDialog(fullName, email, password);
                }

                @Override
                public void onError(Exception e) {
                    btnCreateAccount.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("400")) {
                        IndicatorHelper.showError(RegisterActivity.this, "Failed to send OTP: Institutional whitelist constraint mismatch or email already registered.");
                    } else {
                        IndicatorHelper.showError(RegisterActivity.this, "Network error: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            btnCreateAccount.setEnabled(true);
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void showOtpVerificationDialog(final String fullName, final String email, final String password) {
        // Fix: Replaced invalid theme resource with Theme_Light_NoTitleBar
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Light_NoTitleBar);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_verify_otp, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Enforce matching dimensions across full view bounds dynamically
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        TextView tvSubtitle = dialogView.findViewById(R.id.tvDialogSubtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText("We have sent a 6-digit verification OTP code to " + email + ". Please enter it below to verify.");
        }

        final TextInputEditText etOtp = dialogView.findViewById(R.id.etDialogOtp);
        final MaterialButton btnSubmit = dialogView.findViewById(R.id.btnDialogActionSubmit);
        final TextView tvTimer = dialogView.findViewById(R.id.tvTimer);
        final TextView btnResend = dialogView.findViewById(R.id.btnResendOtp);
        View btnDismiss = dialogView.findViewById(R.id.btnDismissDialog);

        final CountDownTimer timer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvTimer != null) {
                    tvTimer.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                if (tvTimer != null) {
                    tvTimer.setText("Didn't receive code?");
                }
                if (btnResend != null) {
                    btnResend.setVisibility(View.VISIBLE);
                }
            }
        }.start();

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(v -> {
                timer.cancel();
                dialog.dismiss();
            });
        }

        if (btnResend != null) {
            btnResend.setOnClickListener(v -> {
                btnResend.setVisibility(View.GONE);
                timer.start();
                resendRegisterOtp(fullName, email);
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String otp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";
                if (otp.length() != 6) {
                    etOtp.setError("Please enter a valid 6-digit code");
                    return;
                }

                btnSubmit.setEnabled(false);
                verifyRegistrationOtpAndCreate(dialog, timer, fullName, email, password, otp);
            });
        }
    }

    private void resendRegisterOtp(String fullName, String email) {
        IndicatorHelper.showInfo(this, "Resending verification code...");
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);
            bodyJson.put("full_name", fullName);

            String url = Config.getApiUrl(this) + "/register/send-otp";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    IndicatorHelper.showSuccess(RegisterActivity.this, "A new code has been sent to your email.");
                }

                @Override
                public void onError(Exception e) {
                    IndicatorHelper.showError(RegisterActivity.this, "Failed to resend code: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void verifyRegistrationOtpAndCreate(final AlertDialog dialog, final CountDownTimer timer,
                                                final String fullName, final String email, final String password, String otp) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);
            bodyJson.put("full_name", fullName);
            bodyJson.put("password", password);
            bodyJson.put("otp", otp);

            String url = Config.getApiUrl(this) + "/register/verify";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    timer.cancel();
                    dialog.dismiss();
                    try {
                        JSONObject userObj = new JSONObject(response);
                        String userId = userObj.getString("id");
                        String userEmail = userObj.getString("email");
                        String userFullName = userObj.getString("full_name");
                        String role = userObj.getString("role");
                        String phone = userObj.optString("phone", "");
                        boolean faceRegistered = userObj.optBoolean("face_registered", false);
                        boolean fingerprintRegistered = userObj.optBoolean("fingerprint_registered", false);

                        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", userId)
                                .putString("email", userEmail)
                                .putString("full_name", userFullName)
                                .putString("role", role)
                                .putString("phone", phone)
                                .putBoolean("face_registered", faceRegistered)
                                .putBoolean("fingerprint_registered", fingerprintRegistered)
                                .apply();

                        IndicatorHelper.showSuccess(RegisterActivity.this, "Account verified and created successfully!");

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        IndicatorHelper.showError(RegisterActivity.this, "Registration verification response error: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    View btnSubmit = dialog.findViewById(R.id.btnDialogActionSubmit);
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                    }
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("400")) {
                        IndicatorHelper.showError(RegisterActivity.this, "Verification failed: Invalid or expired OTP code.");
                    } else {
                        IndicatorHelper.showError(RegisterActivity.this, "Verification network error: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            String idToken = account.getIdToken();
            if (idToken != null) {
                verifyGoogleAuthOnBackend(idToken);
            } else {
                IndicatorHelper.showError(this, "Failed to retrieve Google Identity Token.");
            }
        } catch (ApiException e) {
            IndicatorHelper.showError(this, "Google Sign-In failed: Status code " + e.getStatusCode());
        }
    }

    private void verifyGoogleAuthOnBackend(String idToken) {
        IndicatorHelper.showInfo(this, "Creating/Authenticating account via Google...");
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("id_token", idToken);

            String url = Config.getApiUrl(this) + "/google-auth";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject userObj = new JSONObject(response);
                        String userId = userObj.getString("id");
                        String userEmail = userObj.getString("email");
                        String fullName = userObj.getString("full_name");
                        String role = userObj.getString("role");
                        String phone = userObj.optString("phone", "");
                        boolean faceRegistered = userObj.optBoolean("face_registered", false);
                        boolean fingerprintRegistered = userObj.optBoolean("fingerprint_registered", false);

                        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString("user_id", userId)
                                .putString("email", userEmail)
                                .putString("full_name", fullName)
                                .putString("role", role)
                                .putString("phone", phone)
                                .putBoolean("face_registered", faceRegistered)
                                .putBoolean("fingerprint_registered", fingerprintRegistered)
                                .apply();

                        IndicatorHelper.showSuccess(RegisterActivity.this, "Welcome " + fullName + "!");

                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } catch (Exception e) {
                        IndicatorHelper.showError(RegisterActivity.this, "Google Sign-In failed to parse response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("400")) {
                        IndicatorHelper.showError(RegisterActivity.this, "Access Denied: Your email domain is not in the institutional whitelist constraint.");
                    } else {
                        IndicatorHelper.showError(RegisterActivity.this, "Google Sign-In connection failed: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }
}