package com.sleistikow.roamingborders;

import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

        RadioButton whitelistButton = new RadioButton(ctx);
        whitelistButton.setText(ctx.getString(R.string.mode_whitelist));
        whitelistButton.setId(ViewGroup.generateViewId());
        RadioButton blacklistButton = new RadioButton(ctx);
        blacklistButton.setText(ctx.getString(R.string.mode_blacklist));
        blacklistButton.setId(ViewGroup.generateViewId());

        RadioGroup radioGroup = new RadioGroup(ctx);
        radioGroup.setOrientation(LinearLayout.HORIZONTAL);
        radioGroup.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        radioGroup.addView(whitelistButton);
        radioGroup.addView(blacklistButton);
        radioGroup.check(whitelistButton.getId());

        layout.addView(radioGroup);

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
                }
            };
            input.addTextChangedListener(watcher);

            ok.setOnClickListener(v -> {
                String txt = input.getText().toString().trim();
                boolean checked = whitelistButton.isChecked();
                listener.accept(txt, checked);
                dlg.dismiss();
            });
        });

        dlg.show();
    }
}

