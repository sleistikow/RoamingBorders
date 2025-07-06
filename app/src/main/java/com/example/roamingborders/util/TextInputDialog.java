package com.example.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

public class TextInputDialog {

    public interface OnTextEntered {
        void onOk(String text);
    }

    public static void ask(Context ctx, String title, OnTextEntered listener) {
        EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String txt = input.getText().toString().trim();
                    if (!txt.isEmpty()) listener.onOk(txt);
                })
                .setNegativeButton("Abbrechen", (d, w) -> d.cancel())
                .show();
    }
}

