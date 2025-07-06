package com.example.roamingborders.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.roamingborders.data.ListManager;
import com.example.roamingborders.model.ListConfig;
import com.example.roamingborders.util.NotificationHelper;
import com.example.roamingborders.vpn.NullVpnService;

import java.util.Locale;
import java.util.concurrent.Executor;

public class CellMonitorService extends Service {
    private static final int NOTIF_ID = 2001;
    private TelephonyManager tm;
    private ListManager listManager;

    public static void enqueue(Context ctx, boolean immediateCheck) {
        Intent i = new Intent(ctx, CellMonitorService.class);
        i.putExtra("check", immediateCheck);
        ContextCompat.startForegroundService(ctx, i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        listManager = new ListManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, NotificationHelper.buildPersistent(this));

        boolean immediate = intent != null && intent.getBooleanExtra("check", false);
        PhoneStateListener phoneListener = new PhoneStateListener() {
            @Override public void onServiceStateChanged(ServiceState state) { evaluate(); }
        };
        tm.listen(phoneListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        if (immediate) evaluate();
        return START_STICKY;
    }

    private void evaluate() {
        String iso = tm.getNetworkCountryIso().toUpperCase(Locale.US);
        ListConfig cfg = listManager.getActiveConfig();
        boolean blocked = cfg.isBlocked(iso);
        if (blocked) NullVpnService.ensureRunning(this);
        else NullVpnService.ensureStopped(this);
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
