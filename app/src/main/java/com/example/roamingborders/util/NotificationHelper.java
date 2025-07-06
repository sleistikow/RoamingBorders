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

    private static final String CHANNEL_VPN = "vpn";
    private static final String CHANNEL_MON = "monitor";

    public static Notification buildPersistent(Context ctx) {
        createChannels(ctx);
        return new NotificationCompat.Builder(ctx, CHANNEL_MON)
                //.setSmallIcon(R.drawable.ic_cell_tower) // TODO
                .setContentTitle(ctx.getString(R.string.app_name))
                .setContentText("Roaming-Überwachung aktiv …")
                .setOngoing(true)
                .build();
    }

    public static Notification buildVpn(Context ctx) {
        createChannels(ctx);
        PendingIntent stop = PendingIntent.getService(ctx, 0,
                new Intent(ctx, NullVpnService.class).setAction("stop"),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(ctx, CHANNEL_VPN)
                //.setSmallIcon(R.drawable.ic_vpn_lock) // TODO
                .setContentTitle("NullVPN aktiv")
                .setContentText("Tippen zum Stoppen")
                .setContentIntent(stop)
                .setOngoing(true)
                .build();
    }

    private static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            if (nm.getNotificationChannel(CHANNEL_VPN) == null) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL_VPN, "VPN",
                        NotificationManager.IMPORTANCE_LOW));
            }
            if (nm.getNotificationChannel(CHANNEL_MON) == null) {
                nm.createNotificationChannel(new NotificationChannel(CHANNEL_MON, "Monitoring",
                        NotificationManager.IMPORTANCE_MIN));
            }
        }
    }
}
