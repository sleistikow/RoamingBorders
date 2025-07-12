package com.example.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class MessageHelper {
    public static void showVpnInfo(Context ctx, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(ctx)
                .setTitle("Disclaimer")
                .setMessage("This app uses a VPN to block data traffic. This needs to be granted in the following dialog.")
                .setPositiveButton("OK", listener)
                .setCancelable(false)
                .show();
    }
}
