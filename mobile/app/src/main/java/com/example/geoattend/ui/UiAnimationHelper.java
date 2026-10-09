package com.example.geoattend.ui;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextPaint;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;

import androidx.core.content.ContextCompat;
import com.example.geoattend.R;

public class UiAnimationHelper {

    /**
     * Animates text color from Blue to Pink smoothly in a seamless loop.
     */
    public static void attachTextGradientAnimation(final TextView textView) {
        textView.post(() -> {
            final float width = textView.getWidth();
            if (width <= 0) return;

            TextPaint paint = textView.getPaint();
            int[] colors = new int[]{
                    ContextCompat.getColor(textView.getContext(), R.color.blue_medium), // Blue
                    ContextCompat.getColor(textView.getContext(), R.color.pink_medium), // Pink
                    ContextCompat.getColor(textView.getContext(), R.color.blue_medium), // Blue
                    ContextCompat.getColor(textView.getContext(), R.color.pink_medium), // Pink
                    ContextCompat.getColor(textView.getContext(), R.color.blue_medium)  // Blue
            };

            LinearGradient shader = new LinearGradient(
                    0, 0, width * 3, 0,
                    colors, null, Shader.TileMode.REPEAT
            );
            paint.setShader(shader);

            Matrix matrix = new Matrix();
            ValueAnimator animator = ValueAnimator.ofFloat(0, width * 2);
            animator.setDuration(4000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setInterpolator(new LinearInterpolator());

            animator.addUpdateListener(animation -> {
                float translate = (float) animation.getAnimatedValue();
                matrix.setTranslate(-translate, 0);
                shader.setLocalMatrix(matrix);
                textView.invalidate();
            });
            animator.start();
        });
    }

    /**
     * Animates the second layer (pink radial blob) of the mesh background drawable.
     */
    public static void attachBackgroundAnimation(final View rootLayout) {
        if (!(rootLayout.getBackground() instanceof LayerDrawable)) return;

        LayerDrawable layerDrawable = (LayerDrawable) rootLayout.getBackground();
        if (layerDrawable.getNumberOfLayers() < 2) return;

        // Grab the second item (index 1) which is our pink radial blob
        if (!(layerDrawable.getDrawable(1) instanceof GradientDrawable)) return;
        final GradientDrawable pinkBlob = (GradientDrawable) layerDrawable.getDrawable(1);

        ValueAnimator bgAnimator = ValueAnimator.ofFloat(0f, 1f);
        bgAnimator.setDuration(6000);
        bgAnimator.setRepeatCount(ValueAnimator.INFINITE);
        bgAnimator.setRepeatMode(ValueAnimator.REVERSE);

        bgAnimator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            float animatedCenterX = 0.4f + (fraction * 0.2f); // Shifting slightly left/right
            float animatedCenterY = 0.1f + (fraction * 0.2f); // Shifting slightly up/down

            pinkBlob.setGradientCenter(animatedCenterX, animatedCenterY);
            rootLayout.invalidate();
        });
        bgAnimator.start();
    }

    public static void setLightStatusIcons(Window window) {
        if (window == null) return;

        // Wrap in decor view post to guarantee the window is attached and active before fetching controllers
        window.getDecorView().post(() -> {
            boolean isDarkMode = false;
            android.content.Context context = window.getContext();
            if (context != null) {
                int nightModeFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                isDarkMode = (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = window.getInsetsController();
                if (controller != null) {
                    if (isDarkMode) {
                        controller.setSystemBarsAppearance(
                                0,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        );
                    } else {
                        controller.setSystemBarsAppearance(
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        );
                    }
                }
            } else {
                View decorView = window.getDecorView();
                int flags = decorView.getSystemUiVisibility();
                if (isDarkMode) {
                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                } else {
                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                decorView.setSystemUiVisibility(flags);
            }
        });
    }
}