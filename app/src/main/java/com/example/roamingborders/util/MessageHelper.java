package com.example.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.roamingborders.R;

public class MessageHelper {
    public static void showVpnInfo(Context ctx, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.disclaimer_vpn_title)
                .setMessage(R.string.disclaimer_vpn_text)
                .setPositiveButton(R.string.dialog_ok, listener)
                .setCancelable(false)
                .show();
    }
}
