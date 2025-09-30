package com.sleistikow.roamingborders;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CH_VPN = "vpn";
    private static final String CH_MON = "monitor";
    private static final String CH_PERM = "permissions";
    private static final String GROUP_FG  = "roaming_foreground";

    private static final int NOTE_ID = 7; // Arbitrary but unique id.

    public static Notification buildVpn(Context ctx) {

        createChannels(ctx);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CH_VPN)
                .setSmallIcon(R.drawable.outline_cell_tower_24)
                .setContentTitle(ctx.getString(R.string.notification_vpn_title))
                .setContentText(ctx.getString(R.string.notification_vpn_text))
                .setOngoing(true)
                .setGroup(GROUP_FG)
                .setSortKey("A")
                .setPriority(NotificationCompat.PRIORITY_HIGH);  // API <= 25

        /*

        // We could add a stop action here, however, the user could accidentally press stop and risk
        // roaming fees and secondly, the main activity would also need to be informed that VPN
        // function received an override.
        PendingIntent stop = PendingIntent.getService(
                ctx, 0,
                new Intent(ctx, NullVpnService.class)
                        .setAction(NullVpnService.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(R.drawable.stop_button,
                ctx.getString(R.string.notification_vpn_action_stop),
                stop);

        Instead, we open the main activity...
         */
        Intent ui = new Intent(ctx, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, ui,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pi);

        return builder.build();
    }

    public static Notification buildPersistent(Context ctx) {
        createChannels(ctx);

        Intent ui = new Intent(ctx, MainActivity.class)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, ui,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(ctx, CH_MON)
                .setSmallIcon(R.drawable.running_service_icon)
                .setContentTitle(ctx.getString(R.string.notification_monitoring_title))
                .setContentText(ctx.getString(R.string.notification_monitoring_text))
                .setOngoing(true)
                .setGroup(GROUP_FG)
                .setSortKey("B")
                .setPriority(NotificationCompat.PRIORITY_MIN) // API <= 25
                .setContentIntent(pi)
                .build();
    }

    public static void showMissingPermissions(Context ctx) {
        createChannels(ctx);

        Intent ui = new Intent(ctx, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(
                ctx, 0, ui,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification note = new NotificationCompat.Builder(ctx, CH_PERM)
                .setSmallIcon(R.drawable.outline_info_24)
                .setContentTitle(ctx.getString(R.string.notification_permissions_missing_title))
                .setContentText(ctx.getString(R.string.notification_permissions_missing_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)    // API <= API 25
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build();

        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        nm.notify(NOTE_ID, note);
    }

    private static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);

            if (nm.getNotificationChannel(CH_VPN) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_VPN, "VPN",
                        NotificationManager.IMPORTANCE_DEFAULT);
                ch.setShowBadge(false);
                ch.setSound(null, null);
                ch.enableVibration(false);
                nm.createNotificationChannel(ch);
            }
            if (nm.getNotificationChannel(CH_MON) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_MON, "Monitoring",
                        NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                ch.setSound(null, null);
                ch.enableVibration(false);
                nm.createNotificationChannel(ch);
            }
            if(nm.getNotificationChannel(CH_PERM) == null) {
                NotificationChannel ch = new NotificationChannel(
                        CH_PERM,
                        "Permissions",
                        NotificationManager.IMPORTANCE_HIGH);
                ch.enableVibration(true);
                nm.createNotificationChannel(ch);
            }
        }
    }

}
