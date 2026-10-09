package com.example.geoattend;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geoattend.ui.UiAnimationHelper;

public class SecureActivity extends AppCompatActivity {

    private TextView tvLogoTitle; // Added your logo text view declaration

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Stretch layout under the status bar (MUST stay before setContentView)
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // 2. Load the XML layout first
        setContentView(R.layout.activity_secure);

        // 3. NOW it is safe to force system icons to turn black
        UiAnimationHelper.setLightStatusIcons(getWindow());

        tvLogoTitle = findViewById(R.id.tvLogoTitle); // Initialized your text view

        // --- ATTACH REUSABLE ANIMATIONS VIA THE HELPER CLASS ---
        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }

        android.view.View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Simulates navigating backward
            }
        });
    }
}