package com.example.geoattend;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageView;
import com.example.geoattend.utils.ConnectionHelper;
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

public class LoginActivity extends AppCompatActivity {

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private TextView tvLogoTitle;
    private TextView tvForgotPasswordLink;
    private TextView tvCreateAccountLink;
    private TextInputEditText etEmail;
    private TextInputEditText etPassword;
    private MaterialButton btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Stretch layout under the status bar (MUST stay before setContentView)
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // 2. Load the XML layout first
        setContentView(R.layout.activity_login);

        // 3. NOW it is safe to force system icons to turn black
        UiAnimationHelper.setLightStatusIcons(getWindow());

        // Bind layout entities
        tvLogoTitle = findViewById(R.id.tvLogoTitle);
        tvForgotPasswordLink = findViewById(R.id.tvForgotPasswordLink);
        tvCreateAccountLink = findViewById(R.id.tvCreateAccountLink);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnContinue = findViewById(R.id.btnContinue);

        // --- ATTACH REUSABLE ANIMATIONS VIA THE HELPER CLASS ---
        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        android.view.View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        // Navigation actions
        if (tvForgotPasswordLink != null) {
            tvForgotPasswordLink.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            });
        }

        if (tvCreateAccountLink != null) {
            tvCreateAccountLink.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }

        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                handleLogin();
            });
        }

        TextView tvSecure = findViewById(R.id.tvSecure); // Ensure you add this ID to your existing layout
        TextView tvSupport = findViewById(R.id.tvSupport); // Ensure you add this ID to your existing layout
        TextView tvHelp = findViewById(R.id.tvHelp); // Ensure you add this ID to your existing layout

        tvSecure.setOnClickListener(v -> startActivity(new Intent(this, SecureActivity.class)));
        tvSupport.setOnClickListener(v -> startActivity(new Intent(this, SupportActivity.class)));
        tvHelp.setOnClickListener(v -> startActivity(new Intent(this, HelpActivity.class)));

        // Google Sign-In setup
        MaterialButton btnGoogle = findViewById(R.id.btnGoogle);
        // Replace with your Web Client ID from Google Cloud Console
        String defaultWebClientId = "205996194633-4566eqpm54i7cug8vi08lbm16s118c7c.apps.googleusercontent.com";

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
                        IndicatorHelper.showError(this, "Google sign-in cancelled");
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

        // Perform startup network & server health check before autologin
        performConnectionCheck();
    }

    private void performConnectionCheck() {
        View layoutConnectionCheck = findViewById(R.id.layoutConnectionCheck);
        ProgressBar pbConnectionCheck = findViewById(R.id.pbConnectionCheck);
        ImageView ivConnectionError = findViewById(R.id.ivConnectionError);
        TextView tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        MaterialButton btnRetryConnection = findViewById(R.id.btnRetryConnection);
        TextView tvConfigureUrl = findViewById(R.id.tvConfigureUrl);

        if (layoutConnectionCheck == null) return;

        // Reset state to loading
        layoutConnectionCheck.setVisibility(View.VISIBLE);
        pbConnectionCheck.setVisibility(View.VISIBLE);
        ivConnectionError.setVisibility(View.GONE);
        tvConnectionStatus.setText("Checking connection to backend...");
        btnRetryConnection.setVisibility(View.GONE);
        tvConfigureUrl.setVisibility(View.GONE);

        // First check local network
        if (!ConnectionHelper.isNetworkAvailable(this)) {
            pbConnectionCheck.setVisibility(View.GONE);
            ivConnectionError.setVisibility(View.VISIBLE);
            tvConnectionStatus.setText("No internet connection detected. Please check your network settings.");
            btnRetryConnection.setVisibility(View.VISIBLE);
            btnRetryConnection.setOnClickListener(v -> performConnectionCheck());
            return;
        }

        // Then check backend server
        ConnectionHelper.checkServerConnection(this, (hasInternet, hasServer) -> {
            if (hasServer) {
                // Connection successful! Fade out overlay and proceed with autologin check
                layoutConnectionCheck.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            layoutConnectionCheck.setVisibility(View.GONE);
                            SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
                            if (prefs.contains("user_id")) {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                        })
                        .start();
            } else {
                pbConnectionCheck.setVisibility(View.GONE);
                ivConnectionError.setVisibility(View.VISIBLE);
                tvConnectionStatus.setText("Could not establish connection to the server at:\n" + Config.getApiUrl(this));
                btnRetryConnection.setVisibility(View.VISIBLE);
                tvConfigureUrl.setVisibility(View.VISIBLE);

                btnRetryConnection.setOnClickListener(v -> performConnectionCheck());
                tvConfigureUrl.setOnClickListener(v -> showUrlConfigDialog("Failed to connect to backend server."));
            }
        });
    }

    private void handleLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

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

        btnContinue.setEnabled(false);
        IndicatorHelper.showInfo(this, "Logging in...");

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("email", email);
            bodyJson.put("password", password);

            String url = Config.getApiUrl(this) + "/login";
            HttpHelper.post(url, bodyJson.toString(), new HttpHelper.Callback() {
                @Override
                public void onSuccess(String response) {
                    btnContinue.setEnabled(true);
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

                        IndicatorHelper.showSuccess(LoginActivity.this, "Welcome " + fullName + "!");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } catch (Exception e) {
                        IndicatorHelper.showError(LoginActivity.this, "Response parsing error: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    btnContinue.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("404")) {
                        IndicatorHelper.showError(LoginActivity.this, "User not found. Please sign up.");
                    } else {
                        showUrlConfigDialog("Failed to connect to backend server.\nDetails: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            btnContinue.setEnabled(true);
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }

    private void showUrlConfigDialog(String errorMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Connection Error");
        builder.setMessage(errorMessage + "\n\nWould you like to update the server base URL?");
        
        final EditText input = new EditText(this);
        input.setText(Config.getApiUrl(this));
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                Config.setApiUrl(LoginActivity.this, newUrl);
                IndicatorHelper.showSuccess(LoginActivity.this, "API URL updated to: " + newUrl);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
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
            IndicatorHelper.showError(this, "Google authentication failed: Status code " + e.getStatusCode());
        }
    }

    private void verifyGoogleAuthOnBackend(String idToken) {
        IndicatorHelper.showInfo(this, "Connecting to Google Auth server...");
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

                        IndicatorHelper.showSuccess(LoginActivity.this, "Welcome " + fullName + "!");
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } catch (Exception e) {
                        IndicatorHelper.showError(LoginActivity.this, "Google Sign-In failed to parse response: " + e.getMessage());
                    }
                }

                @Override
                public void onError(Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("400")) {
                        IndicatorHelper.showError(LoginActivity.this, "Access Denied: Your email domain is not in the institutional whitelist constraint.");
                    } else {
                        IndicatorHelper.showError(LoginActivity.this, "Google Sign-In connection failed: " + msg);
                    }
                }
            });
        } catch (Exception e) {
            IndicatorHelper.showError(this, "Error building request: " + e.getMessage());
        }
    }
}