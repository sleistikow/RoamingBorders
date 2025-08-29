package com.sleistikow.roamingborders.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;

import com.sleistikow.roamingborders.R;

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

    public static void showSwitchFromRunningPreset(Context ctx, Listener positiveListener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_switch_active_preset_title)
                .setMessage(R.string.message_confirm_switch_active_preset_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> positiveListener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, null)
                .setCancelable(true)
                .show();
    }

    public static void showRemoveCurrentlyBlockedCountry(Context ctx, Listener positiveListener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_removing_current_country_title)
                .setMessage(R.string.message_confirm_removing_current_country_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> positiveListener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, null)
                .setCancelable(true)
                .show();
    }

    public static void showGuardStateConfirmation(Context ctx, Listener positiveListener, Listener negativeListener) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.message_confirm_guard_state_title)
                .setMessage(R.string.message_confirm_guard_state_text)
                .setPositiveButton(R.string.dialog_yes, (d, i) -> positiveListener.onActionTriggered())
                .setNegativeButton(R.string.dialog_no, (d, i) -> negativeListener.onActionTriggered())
                .setCancelable(true)
                .setOnCancelListener(d -> negativeListener.onActionTriggered())
                .show();
    }


    private static void openWebPage(Context context, String url) {
        CustomTabsIntent intent = new CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build();
        intent.launchUrl(context, Uri.parse(url));
    }

    private static void openRatingPage(Context ctx) {
        String packageName = ctx.getPackageName();
        try {
            // First try to open Play Store app.
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName)));
        } catch (android.content.ActivityNotFoundException e) {
            openWebPage(ctx, "https://play.google.com/store/apps/details?id=" + packageName);
        }
    }

    public static void showDonationBox(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle(R.string.donation_title)
                .setMessage(R.string.donation_text)
                .setPositiveButton(R.string.donation_yes, (d, w) -> openWebPage(ctx, ctx.getString(R.string.donation_link)))
                .setNeutralButton(R.string.donation_neutral, (d, w) -> openRatingPage(ctx))
                .setNegativeButton(R.string.donation_no, null)
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
