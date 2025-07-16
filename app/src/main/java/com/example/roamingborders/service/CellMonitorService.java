package com.example.roamingborders.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.roamingborders.MainActivity;
import com.example.roamingborders.R;
import com.example.roamingborders.data.ListManager;
import com.example.roamingborders.model.ListConfig;
import com.example.roamingborders.monitor.MobileTrafficMonitor;
import com.example.roamingborders.util.NotificationHelper;
import com.example.roamingborders.vpn.NullVpnService;

import java.util.Locale;

public class CellMonitorService extends Service {
    private static final int NOTIF_ID = 2001;
    private static final String ACTION_STOP = "STOP_MONITORING";
    private TelephonyManager tm;
    private ListManager listManager;

    private Object serviceStateListener;
    private MobileTrafficMonitor mobileTrafficMonitor;
    private boolean usingMobileTraffic = false;

    private static CellMonitorService instance;

    public static void ensureRunning(Context ctx) {
        if(instance == null) {
            Intent intent = new Intent(ctx, CellMonitorService.class);
            ContextCompat.startForegroundService(ctx, intent);
        }
        else {
            instance.evaluate();
        }
    }

    public static void ensureStopped(Context ctx) {
        if(instance != null) {
            Intent intent = new Intent(ctx, CellMonitorService.class).setAction(ACTION_STOP);
            ContextCompat.startForegroundService(ctx, intent);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        listManager = new ListManager(this);
        instance = this;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(serviceStateListener != null) {
            if (Build.VERSION.SDK_INT >= 31) {
                tm.unregisterTelephonyCallback((TelephonyCallback) serviceStateListener);
            } else {
                tm.listen((PhoneStateListener) serviceStateListener, PhoneStateListener.LISTEN_NONE);
            }
            serviceStateListener = null;
        }
        if(mobileTrafficMonitor != null) {
            mobileTrafficMonitor.stop();
            mobileTrafficMonitor = null;
        }

        instance = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            // Stop the VPN service in case it's still running.
            NullVpnService.ensureStopped(this);
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+: 3‑arg overload verlangt expliziten Typ → SERVICE_TYPE_DATA_SYNC
                startForeground(NOTIF_ID,
                        NotificationHelper.buildPersistent(this),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                // Ältere APIs nutzen 2‑arg Variante (Typ kommt aus Manifest)
                startForeground(NOTIF_ID, NotificationHelper.buildPersistent(this));
            }
        } catch (SecurityException se) {
            //Log.e("CellMonitorService", "startForeground failed", se);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (Build.VERSION.SDK_INT >= 31)
        {
            class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.ServiceStateListener {
                @Override
                public void onServiceStateChanged(@NonNull ServiceState serviceState) {
                    // Your logic here
                    evaluate();
                }
            }
            MyTelephonyCallback callback = new MyTelephonyCallback();

            tm.registerTelephonyCallback(this.getMainExecutor(), callback);
            serviceStateListener = callback;
        }
        else {
            PhoneStateListener phoneListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState state) {
                    evaluate();
                }
            };
            tm.listen(phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE);
            serviceStateListener = phoneListener;
        }

        mobileTrafficMonitor = new MobileTrafficMonitor(this, usingMobile -> {
            if(usingMobile != usingMobileTraffic) {
                usingMobileTraffic = usingMobile;
                evaluate();
            }
        });
        mobileTrafficMonitor.start();

        return START_STICKY;
    }

    private void evaluate() {
        String iso = tm.getNetworkCountryIso().toUpperCase(Locale.US);
        if(iso.isEmpty()) {
            // This is the case of SIM ejection.
            return;
        }

        ListConfig cfg = listManager.loadActiveConfig();
        if(cfg == null) {
            NullVpnService.ensureStopped(this);
            return;
        }

        boolean blocked = cfg.isBlocked(iso);
        if (blocked && usingMobileTraffic) NullVpnService.ensureRunning(this);
        else NullVpnService.ensureStopped(this);
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
