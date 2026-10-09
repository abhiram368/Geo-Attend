package com.example.geoattend.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.geoattend.R;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.ContextCompat;

public class IndicatorHelper {

    public static void showError(View container, String message) {
        showSnackbar(container, message, Snackbar.LENGTH_LONG, true);
    }
    
    public static void showError(android.app.Activity activity, String message) {
        if (activity != null) {
            showError(activity.findViewById(android.R.id.content), message);
        }
    }

    public static void showSuccess(View container, String message) {
        showSnackbar(container, message, Snackbar.LENGTH_SHORT, false);
    }
    
    public static void showSuccess(android.app.Activity activity, String message) {
        if (activity != null) {
            showSuccess(activity.findViewById(android.R.id.content), message);
        }
    }

    public static void showInfo(View container, String message) {
        showSnackbar(container, message, Snackbar.LENGTH_SHORT, false);
    }
    
    public static void showInfo(android.app.Activity activity, String message) {
        if (activity != null) {
            showInfo(activity.findViewById(android.R.id.content), message);
        }
    }

    private static void showSnackbar(View container, String message, int length, boolean isError) {
        if (container == null) return;
        
        Snackbar snackbar = Snackbar.make(container, message, length);
        
        // Find bottom nav if possible to anchor
        View root = container.getRootView();
        if (root != null) {
            Context context = container.getContext();
            int bottomNavViewId = context.getResources().getIdentifier("bottomNavView", "id", context.getPackageName());
            int customBottomBarId = context.getResources().getIdentifier("customBottomBar", "id", context.getPackageName());
            
            View bottomNav = null;
            if (bottomNavViewId != 0) {
                bottomNav = root.findViewById(bottomNavViewId);
            }
            if (bottomNav == null && customBottomBarId != 0) {
                bottomNav = root.findViewById(customBottomBarId);
            }
            if (bottomNav != null && bottomNav.getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(bottomNav);
            }
        }
        
        View snackbarView = snackbar.getView();
        Context context = container.getContext();
        
        // Determine light/dark mode
        int nightModeFlags = context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        // Background shape
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setCornerRadius(24f); // Rounded corners
        
        // Color palette based on theme and state
        int bgColor;
        int textColor;
        int actionColor;
        
        if (isDarkMode) {
            if (isError) {
                bgColor = ContextCompat.getColor(context, R.color.indicator_red_dark); // Dark Red
                textColor = ContextCompat.getColor(context, R.color.indicator_red_light); // Light Red Text
                actionColor = ContextCompat.getColor(context, R.color.error);
            } else {
                bgColor = ContextCompat.getColor(context, R.color.text_primary); // Slate Dark
                textColor = ContextCompat.getColor(context, R.color.indicator_slate_light); // Off-white
                actionColor = ContextCompat.getColor(context, R.color.indicator_sky_blue); // Sky
            }
        } else {
            if (isError) {
                bgColor = ContextCompat.getColor(context, R.color.indicator_red_bg); // Light Red
                textColor = ContextCompat.getColor(context, R.color.indicator_red_text_dark); // Dark Red Text
                actionColor = ContextCompat.getColor(context, R.color.error);
            } else {
                bgColor = ContextCompat.getColor(context, R.color.indicator_slate_dark); // Slate Dark for contrast in light mode
                textColor = ContextCompat.getColor(context, R.color.white);
                actionColor = ContextCompat.getColor(context, R.color.indicator_sky_blue);
            }
        }
        
        background.setColor(bgColor);
        snackbarView.setBackground(background);
        
        // Add margins
        ViewGroup.LayoutParams layoutParams = snackbarView.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginParams = (ViewGroup.MarginLayoutParams) layoutParams;
            marginParams.setMargins(40, 0, 40, 24); // Floating style
            snackbarView.setLayoutParams(marginParams);
        }
        
        // Text styling
        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
        if (textView != null) {
            textView.setTextColor(textColor);
            textView.setTextSize(14);
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textView.setMaxLines(3);
        }
        
        // Action Button styling
        View actionButton = snackbarView.findViewById(com.google.android.material.R.id.snackbar_action);
        if (actionButton != null && actionButton instanceof TextView) {
            TextView actionTextView = (TextView) actionButton;
            actionTextView.setTextColor(actionColor);
            actionTextView.setTypeface(Typeface.create("sans-serif-bold", Typeface.BOLD));
        }
        
        snackbar.show();
    }
}
