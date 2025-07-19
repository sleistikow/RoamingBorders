package com.example.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.roamingborders.R;

public class MessageHelper {

    public interface Listener {
        void onActionTriggered();
    }

    public static void showVpnInfo(Context ctx, Listener listener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.disclaimer_vpn_title)
                .setMessage(R.string.disclaimer_vpn_text)
                .setPositiveButton(R.string.dialog_ok, (d, i) -> listener.onActionTriggered())
                .setCancelable(false)
                .show();
    }

    public static void showDeletePreset(Context ctx, Listener listener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_preset_deletion_title)
                .setMessage(R.string.message_preset_deletion_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> listener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, null)
                .setCancelable(true)
                .show();
    }

    public static void showActivePresetDeletionNotPossible(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_active_preset_deletion_impossible_title)
                .setMessage(R.string.message_active_preset_deletion_impossible_text)
                .setPositiveButton(R.string.dialog_ok, null)
                .setCancelable(true)
                .show();
    }

    public static void showPredefinedPresetDeletionNotPossible(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_predefined_preset_deletion_impossible_title)
                .setMessage(R.string.message_predefined_preset_deletion_impossible_text)
                .setPositiveButton(R.string.dialog_ok, null)
                .setCancelable(true)
                .show();
    }

    public static void showSwitchFromRunningPreset(Context ctx, Listener positiveListener, Listener negativeListener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_kill_switch_title)
                .setMessage(R.string.message_confirm_kill_switch_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> positiveListener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, (d, i) -> negativeListener.onActionTriggered())
                .setCancelable(true)
                .setOnCancelListener(d -> negativeListener.onActionTriggered())
                .show();
    }

    public static void showKillSwitchConfirmation(Context ctx, Listener positiveListener, Listener negativeListener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_kill_switch_title)
                .setMessage(R.string.message_confirm_kill_switch_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> positiveListener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, (d, i) -> negativeListener.onActionTriggered())
                .setCancelable(true)
                .setOnCancelListener(d -> negativeListener.onActionTriggered())
                .show();
    }

    public static void showInfoBox(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.app_info)
                .setMessage("© 2025 Simon Leistkow\ndeveloper.sleistikow@proton.me")
                .setPositiveButton(R.string.dialog_ok, null)
                .setCancelable(true)
                .show();
    }
}
