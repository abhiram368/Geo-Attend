package com.example.geoattend;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.geoattend.ui.UiAnimationHelper;

public class HelpActivity extends AppCompatActivity {

    private TextView tvLogoTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Stretch layout cleanly under the status bar (match registration flow)
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        // 2. Load the helper layout file
        setContentView(R.layout.activity_help);

        // 3. Force system icons to turn dark for readability
        UiAnimationHelper.setLightStatusIcons(getWindow());

        // 4. Initialize UI views
        tvLogoTitle = findViewById(R.id.tvLogoTitle);
        ImageButton btnBack = findViewById(R.id.btnBack);
        View rootLayout = findViewById(R.id.rootLayout);

        // 5. Attach reusable visual/mesh gradient effects
        if (tvLogoTitle != null) {
            UiAnimationHelper.attachTextGradientAnimation(tvLogoTitle);
        }
        if (rootLayout != null) {
            UiAnimationHelper.attachBackgroundAnimation(rootLayout);
        }

        // 6. Navigation: Pop back to previous activity stack
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }
}