package com.owl.lyra.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

public class BooleanDialog {
    private AlertDialog dialog;
    private String mTitle;
    private String mMessage;
    private String mPositiveBtnText;
    private String mNegativeBtnText;

    public BooleanDialog(String title, String message, String positiveBtnText, String negativeBtnText) {
        mTitle = title;
        mMessage = message;
        mPositiveBtnText = positiveBtnText;
        mNegativeBtnText = negativeBtnText;
    }

    public void buildDialog(Context context, OkCallbackInterface okCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(mTitle);
        builder.setMessage(mMessage);

        // Set up the buttons
        builder.setPositiveButton(mPositiveBtnText, (dialog, which) -> {
            dialog.dismiss();
            okCallback.call();
        });
        builder.setNegativeButton(mNegativeBtnText, (dialog, which) -> dialog.cancel());

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void buildDialog(Context context, OkCallbackInterface okCallback, CancelCallbackInterface cancelCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(mTitle);
        builder.setMessage(mMessage);

        // Set up the buttons
        builder.setPositiveButton(mPositiveBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                okCallback.call();
            }
        });
        builder.setNegativeButton(mNegativeBtnText, (dialog, which) -> {
            dialog.cancel();
            cancelCallback.call();
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void showDialog() {
        System.out.println(dialog);
        dialog.show();
    }
}
