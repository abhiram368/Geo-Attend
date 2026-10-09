package com.example.geoattend;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;
import com.example.geoattend.ui.IndicatorHelper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.geoattend.ui.UiAnimationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvLogoTitle;
    private TextInputEditText etEmail;
    private AppCompatButton btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        setContentView(R.layout.activity_forgot_password);

        UiAnimationHelper.setLightStatusIcons(getWindow());

        btnBack = findViewById(R.id.btnBack);
        tvLogoTitle = findViewById(R.id.tvLogoTitle);
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        android.view.View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        TextView tvSecure = findViewById(R.id.tvSecure);
        TextView tvSupport = findViewById(R.id.tvSupport);
        TextView tvHelp = findViewById(R.id.tvHelp);

        tvSecure.setOnClickListener(v -> startActivity(new Intent(this, SecureActivity.class)));
        tvSupport.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        tvHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnResetPassword.setOnClickListener(v -> handlePasswordReset());
    }

    private void handlePasswordReset() {
        final String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email address is required");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        btnResetPassword.setEnabled(false);
        IndicatorHelper.showInfo(this, "Sending password reset code...");

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);

            String url = Config.getApiUrl(this) + "/forgot-password/send-otp";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    btnResetPassword.setEnabled(true);
                    showResetPasswordDialog(email);
                }

                @Override
                public void onError(Exception e) {
                    btnResetPassword.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("404")) {
                        IndicatorHelper.showError(ForgotPasswordActivity.this, "This email is not registered in the system.");
                    } else {
                        IndicatorHelper.showError(ForgotPasswordActivity.this, "Failed to send reset code: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            btnResetPassword.setEnabled(true);
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void showResetPasswordDialog(final String email) {
        // Fix applied here: Use a standard platform base theme style block
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Light_NoTitleBar);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_reset_password_otp, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create();
        dialog.show();

        // Ensure window configurations match layout width-height metrics accurately
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        final TextInputEditText etOtp = dialogView.findViewById(R.id.etDialogOtp);
        final TextInputEditText etNewPassword = dialogView.findViewById(R.id.etDialogNewPassword);
        final TextInputEditText etConfirmPassword = dialogView.findViewById(R.id.etDialogConfirmPassword);
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
                resendResetOtp(email);
            });
        }

        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String otp = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";
                String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString() : "";
                String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString() : "";

                if (otp.length() != 6) {
                    etOtp.setError("Please enter a valid 6-digit code");
                    etOtp.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(newPassword)) {
                    etNewPassword.setError("New password is required");
                    etNewPassword.requestFocus();
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    etConfirmPassword.setError("Passwords do not match");
                    etConfirmPassword.requestFocus();
                    return;
                }

                btnSubmit.setEnabled(false);
                verifyOtpAndResetPassword(dialog, timer, email, otp, newPassword);
            });
        }
    }

    private void resendResetOtp(String email) {
        IndicatorHelper.showInfo(this, "Resending reset code...");
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);

            String url = Config.getApiUrl(this) + "/forgot-password/send-otp";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    IndicatorHelper.showSuccess(ForgotPasswordActivity.this, "A new reset code has been sent to your email.");
                }

                @Override
                public void onError(Exception e) {
                    IndicatorHelper.showError(ForgotPasswordActivity.this, "Failed to resend code: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void verifyOtpAndResetPassword(final AlertDialog dialog, final CountDownTimer timer,
                                           final String email, String otp, String newPassword) {
        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);
            bodyJson.put("otp", otp);
            bodyJson.put("new_password", newPassword);

            String url = Config.getApiUrl(this) + "/forgot-password/verify";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    timer.cancel();
                    dialog.dismiss();
                    IndicatorHelper.showSuccess(ForgotPasswordActivity.this, "Password updated successfully! Please log in.");
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    View btnSubmit = dialog.findViewById(R.id.btnDialogActionSubmit);
                    if (btnSubmit != null) {
                        btnSubmit.setEnabled(true);
                    }
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("400")) {
                        IndicatorHelper.showError(ForgotPasswordActivity.this, "Verification failed: Invalid or expired OTP code.");
                    } else {
                        IndicatorHelper.showError(ForgotPasswordActivity.this, "Verification network error: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }
}