package com.example.roamingborders.vpn;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.NotificationHelper;

import java.io.FileInputStream;
import java.io.IOException;

public class NullVpnService extends VpnService {
    private static final int NOTIF_ID = 2002;
    public static final String ACTION_STOP = "STOP_VPN";
    private ParcelFileDescriptor tun;
    private static NullVpnService instance;

    public static void ensureRunning(Context ctx) {
        if (instance == null) {
            Intent intent = new Intent(ctx, NullVpnService.class);
            ContextCompat.startForegroundService(ctx, intent);
        }
    }

    public static void ensureStopped(Context ctx) {
        if (instance != null) {
            Intent intent = new Intent(ctx, NullVpnService.class).setAction(ACTION_STOP);
            ContextCompat.startForegroundService(ctx, intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        try {
            ServiceCompat.startForeground(
                    this, NOTIF_ID, NotificationHelper.buildVpn(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

            if (tun == null) establishVpn();
        } catch (Exception se) {
            //Log.e("NullVpnService", "startForeground failed", se);
            stopSelf();
        }

        return START_STICKY;
    }

    private void establishVpn() {
        Builder b = new Builder()
            .setSession("Roaming Borders NullVPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .setBlocking(true);

        try {
            tun = b.establish();
        } catch (Exception e) {
            stopSelf();
        }
    }

    private void stopVpn() {
        try {
            if (tun != null) {
                tun.close();
                tun = null;
            }
        } catch (IOException e) {
            //Log.e("VPN", "Closing tun failed", e);
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopVpn();
        instance = null;
    }

    @Override
    public void onRevoke() {   // foreign VPN took over
        // TODO: hier muss ggf. die Activity informiert werden und der Toggle deaktiviert werden
        stopVpn();
    }
}
