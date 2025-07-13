package com.example.roamingborders.util;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.example.roamingborders.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collection;

public class TextInputDialog {

    public interface OnTextEntered {
        void accept(String name, boolean whitelist);
    }

    public static void ask(Context ctx, String title, Collection<String> takenNames, OnTextEntered listener) {
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText input = new EditText(ctx);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        layout.addView(input);

        CheckBox checkBox = new CheckBox(ctx);
        checkBox.setText("Whitelist");
        checkBox.setChecked(true);
        layout.addView(checkBox);

        AlertDialog dlg = new MaterialAlertDialogBuilder(ctx)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, (d,w)->d.dismiss())
                .create();

        dlg.setOnShowListener(d -> {

            Button ok = dlg.getButton(DialogInterface.BUTTON_POSITIVE);
            ok.setEnabled(false);

            TextWatcher watcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

                @Override
                public void afterTextChanged(Editable editable) {
                    String txt = editable.toString().trim();
                    boolean valid = !txt.isEmpty() && !takenNames.contains(txt);
                    ok.setEnabled(valid);
                    /*
                    TextInputLayout til = layout.findViewById(R.id.tilName);
                    til.setError(valid ? null :
                            (txt.isEmpty() ? ctx.getString(R.string.err_empty) :
                                    ctx.getString(R.string.err_exists)));
                     */
                }
            };
            input.addTextChangedListener(watcher);

            // ② Erst *jetzt* den tatsächlichen OnClick registrieren
            ok.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                boolean checked = checkBox.isChecked();
                listener.accept(txt, checked);
                dlg.dismiss();
            });
        });

        dlg.show();
    }
}

