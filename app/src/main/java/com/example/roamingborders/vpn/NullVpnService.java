package com.example.roamingborders.vpn;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;

import androidx.core.content.ContextCompat;

import com.example.roamingborders.util.NotificationHelper;

import java.io.FileInputStream;
import java.io.IOException;

public class NullVpnService extends VpnService {
    private static final int NOTIF_ID = 2002;
    private ParcelFileDescriptor tun;
    private static NullVpnService instance;

    public static void ensureRunning(Context ctx) {
        if (instance == null) {
            Intent i = new Intent(ctx, NullVpnService.class);
            ContextCompat.startForegroundService(ctx, i);
        }
    }

    public static void ensureStopped(Context ctx) {
        if (instance != null) instance.stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (tun == null) establishTun();
        startForeground(NOTIF_ID, NotificationHelper.buildVpn(this));
        return START_STICKY;
    }

    private void establishTun() {
        Builder b = new Builder();
        b.setSession("Roaming Borders NullVPN");
        b.addAddress("10.0.0.2", 32);
        b.addRoute("0.0.0.0", 0);
        b.setBlocking(true);
        try {
            tun = b.establish();
        } catch (Exception e) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { if (tun != null) tun.close(); } catch (IOException ignored) {}
        instance = null;
    }

    /*
    private void drain() {
        byte[] buf = new byte[32767];
        try (FileInputStream in = new FileInputStream(tun.getFileDescriptor())) {
            while (in.read(buf) >= 0) { } // Drop
        } catch (IOException ignored) {}
        stopSelf();
    }
    */
}
