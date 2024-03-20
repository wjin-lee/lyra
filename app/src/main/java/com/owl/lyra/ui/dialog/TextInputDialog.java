package com.owl.lyra.ui.dialog;

//import
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.owl.lyra.R;

public class TextInputDialog extends Fragment {

    private AlertDialog dialog;
    private String mTitle;
    private String mMessage;
    private String mPositiveBtnText;
    private String mNegativeBtnText;
//    private Callable mSuccessCallback;
//    private  Callable mCancelCallback;
    private String mText;


    public TextInputDialog(String title, String message, String positiveBtnText, String negativeBtnText) {
        mTitle = title;
        mMessage = message;
        mPositiveBtnText = positiveBtnText;
        mNegativeBtnText = negativeBtnText;
    }

    public void buildDialog(Context context, OkTextCallbackInterface okCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(mTitle);
        builder.setMessage(mMessage);

        View viewInflated = LayoutInflater.from(context).inflate(R.layout.text_input_dialog, null);
        // Set up the input
        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
        // Specify the type of input expected; this, for owl, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);


        builder.setView(viewInflated);
        // Set up the buttons
        builder.setPositiveButton(mPositiveBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mText = input.getText().toString();
                input.setText("");
                okCallback.call(mText);
            }
        });
        builder.setNegativeButton(mNegativeBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                input.setText("");
            }
        });

        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void buildDialog(Context context, OkTextCallbackInterface okCallback, CancelCallbackInterface cancelCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(mTitle);

        View viewInflated = LayoutInflater.from(context).inflate(R.layout.text_input_dialog, null);
        // Set up the input
        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
        // Specify the type of input expected; this, for owl, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);


        builder.setView(viewInflated);
        // Set up the buttons
        builder.setPositiveButton(mPositiveBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mText = input.getText().toString();
                input.setText("");
                okCallback.call(mText);
            }
        });
        builder.setNegativeButton(mNegativeBtnText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                input.setText("");
                cancelCallback.call();
            }
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
    }

    public void showDialog() {
        mText = "";
        System.out.println(dialog);
        dialog.show();
    }

    public String getText() {
        return mText;
    }
}
