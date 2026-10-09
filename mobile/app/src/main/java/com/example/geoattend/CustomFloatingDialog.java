package com.example.geoattend;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class CustomFloatingDialog extends DialogFragment {

    private final String title;
    private final String actionHint;
    private final DialogSubmitCallback callback;

    public interface DialogSubmitCallback {
        void onSubmit(String inputDataText);
    }

    public CustomFloatingDialog(String title, String actionHint, DialogSubmitCallback callback) {
        this.title = title;
        this.actionHint = actionHint;
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_floating_window, container, false);

        // Strip default underlying dialog borders to reveal our rounded card styles
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            // Prevents dismissing the dialog accidentally by clicking on the background overlay area
            getDialog().setCanceledOnTouchOutside(false);
        }

        TextView tvTitle = view.findViewById(R.id.dialogTitle);
        Button btnSubmit = view.findViewById(R.id.btnDialogActionSubmit);
        TextView etInput = view.findViewById(R.id.etDialogInputField);
        View btnClose = view.findViewById(R.id.btnDismissDialog);

        tvTitle.setText(title);
        btnSubmit.setText(actionHint);

        btnClose.setOnClickListener(v -> dismiss());

        btnSubmit.setOnClickListener(v -> {
            if (callback != null) {
                callback.onSubmit(etInput.getText().toString());
            }
            dismiss();
        });

        return view;
    }
}