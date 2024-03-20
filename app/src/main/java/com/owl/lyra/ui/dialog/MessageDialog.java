package com.owl.lyra.ui.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

public class MessageDialog {
    private AlertDialog dialog;
    private String mTitle;
    private String mMessage;

    public MessageDialog(String title, String message) {
        mTitle = title;
        mMessage = message;
    }

    public void buildDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(mTitle);
        builder.setMessage(mMessage);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void showDialog() {
        System.out.println(dialog);
        dialog.show();
    }
}
