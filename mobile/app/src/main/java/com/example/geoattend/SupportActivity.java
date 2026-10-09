package com.example.geoattend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.geoattend.ui.IndicatorHelper;
import com.example.geoattend.ui.UiAnimationHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

public class SupportActivity extends AppCompatActivity {

    private TextInputEditText etSupportMessage;
    private MaterialButton btnSubmitSupport;
    private TextView tvLogoTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Stretch layout under the status bar
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // 2. Load the XML layout
        setContentView(R.layout.activity_support);

        // 3. Force system icons to turn black
        UiAnimationHelper.setLightStatusIcons(getWindow());

        tvLogoTitle = findViewById(R.id.tvLogoTitle);

        // Reusable animations
        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        etSupportMessage = findViewById(R.id.etSupportMessage);
        btnSubmitSupport = findViewById(R.id.btnSubmitSupport);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Check if user is logged in (session user_id is present)
        SharedPreferences prefs = getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String userId = prefs.getString("user_id", "");

        if (TextUtils.isEmpty(userId)) {
            if (etSupportMessage != null) {
                etSupportMessage.setHint("Please log in to submit support tickets. For login/account issues, contact kattaabhiram368@gmail.com directly.");
                etSupportMessage.setEnabled(false);
                etSupportMessage.setFocusable(false);
            }
            if (btnSubmitSupport != null) {
                btnSubmitSupport.setEnabled(false);
                btnSubmitSupport.setText("Login Required");
            }
        } else {
            btnSubmitSupport.setOnClickListener(v -> {
                String message = etSupportMessage.getText().toString().trim();
                if (TextUtils.isEmpty(message)) {
                    IndicatorHelper.showError(SupportActivity.this, "Please enter your message context.");
                    return;
                }

                btnSubmitSupport.setEnabled(false);
                try {
                    JSONObject body = new JSONObject();
                    body.put("user_id", userId);
                    body.put("message", message);

                    String url = Config.getApiUrl(this) + "/support-requests";
                    HttpHelper.post(url, body.toString(), new HttpHelper.Callback() {
                        @Override
                        public void onSuccess(String response) {
                            runOnUiThread(() -> {
                                IndicatorHelper.showSuccess(SupportActivity.this, "Ticket submitted successfully!");
                                etSupportMessage.setText("");
                                btnSubmitSupport.setEnabled(true);
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                IndicatorHelper.showError(SupportActivity.this, "Failed to submit ticket: " + e.getMessage());
                                btnSubmitSupport.setEnabled(true);
                            });
                        }
                    });
                } catch (Exception e) {
                    IndicatorHelper.showError(this, "Error: " + e.getMessage());
                    btnSubmitSupport.setEnabled(true);
                }
            });
        }
    }
}