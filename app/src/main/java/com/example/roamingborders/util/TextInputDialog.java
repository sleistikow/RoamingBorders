package com.example.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.example.roamingborders.R;

public class TextInputDialog {

    public interface OnTextEntered {
        void onOk(String name, boolean whitelist);
    }

    public static void ask(Context ctx, String title, OnTextEntered listener) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(input);

        CheckBox checkBox = new CheckBox(ctx);
        checkBox.setText("Whitelist");
        checkBox.setChecked(true);
        layout.addView(checkBox);

        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("OK", (d, w) -> {
                    String txt = input.getText().toString().trim();
                    boolean checked = checkBox.isChecked();
                    if (!txt.isEmpty()) listener.onOk(txt, checked);
                })
                .setNegativeButton("Abbrechen", (d, w) -> d.cancel())
                .show();
    }
}

