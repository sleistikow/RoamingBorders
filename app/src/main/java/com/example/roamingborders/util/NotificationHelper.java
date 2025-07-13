package com.example.roamingborders.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.roamingborders.R;
import com.example.roamingborders.vpn.NullVpnService;

public class NotificationHelper {

    private static final String CH_VPN = "vpn";
    private static final String CH_MON = "monitor";
    private static final String GROUP_FG  = "roaming_foreground";

    public static Notification buildVpn(Context ctx) {

        createChannels(ctx);

        PendingIntent stop = PendingIntent.getService(
                ctx, 0,
                new Intent(ctx, NullVpnService.class)
                        .setAction(NullVpnService.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(ctx, CH_VPN)
                .setSmallIcon(R.drawable.outline_cell_tower_24)
                .setContentTitle(ctx.getString(R.string.notification_vpn_title))
                .setContentText(ctx.getString(R.string.notification_vpn_text))
                .setOngoing(false)
                .setGroup(GROUP_FG)
                .setSortKey("A")
                .setPriority(NotificationCompat.PRIORITY_HIGH)  // API <= 25
                .addAction(R.drawable.stop_button,
                        ctx.getString(R.string.notification_vpn_action_stop),
                        stop)
                .build();
    }

    public static Notification buildPersistent(Context ctx) {
        createChannels(ctx);
        return new NotificationCompat.Builder(ctx, CH_MON)
                .setSmallIcon(R.drawable.running_service_icon)
                .setContentTitle(ctx.getString(R.string.notification_monitoring_title))
                .setContentText(ctx.getString(R.string.notification_monitoring_text))
                .setOngoing(true)
                .setGroup(GROUP_FG)
                .setSortKey("B")
                .setPriority(NotificationCompat.PRIORITY_MIN) // API <= 25
                .build();
    }

    private static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);

            if (nm.getNotificationChannel(CH_VPN) == null) {
                NotificationChannel vpn = new NotificationChannel(
                        CH_VPN, "VPN",
                        NotificationManager.IMPORTANCE_HIGH);
                vpn.setShowBadge(false);
                vpn.setSound(null, null);
                vpn.enableVibration(false);
                nm.createNotificationChannel(vpn);
            }
            if (nm.getNotificationChannel(CH_MON) == null) {
                NotificationChannel mon = new NotificationChannel(
                        CH_MON, "Monitoring",
                        NotificationManager.IMPORTANCE_MIN);
                mon.setShowBadge(false);
                mon.setSound(null, null);
                mon.enableVibration(false);
                nm.createNotificationChannel(mon);
            }
        }
    }

}
