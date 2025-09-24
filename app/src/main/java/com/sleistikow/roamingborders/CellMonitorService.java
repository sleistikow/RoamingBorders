package com.sleistikow.roamingborders;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class CellMonitorService extends Service {
    private static final int NOTIF_ID = 2001;
    private static final String ACTION_STOP = "STOP_MONITORING";

    private TelephonyManager tm;
    private Object serviceStateListener;

    //private ConnectivityManager cm;
    //private ConnectivityManager.NetworkCallback networkCallback;

    private ListManager listManager;

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

    public static boolean isCurrentlyBlocking() {
        return NullVpnService.isRunning();
    }

    public static String getCurrentCountry() {
        if(instance == null || instance.tm == null) return "";
        return instance.tm.getNetworkCountryIso().toUpperCase(Locale.US);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //cm = getSystemService(ConnectivityManager.class);
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        listManager = new ListManager(getApplicationContext());
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
            //cm.unregisterNetworkCallback(networkCallback);

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
            NullVpnService.ensureStopped(getApplicationContext());
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID,
                        NotificationHelper.buildPersistent(getApplicationContext()),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, NotificationHelper.buildPersistent(getApplicationContext()));
            }
        } catch (SecurityException se) {
            //Log.e("CellMonitorService", "startForeground failed", se);
            stopSelf();
            return START_NOT_STICKY;
        }

        /*
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(@NonNull Network network,
                                              @NonNull NetworkCapabilities caps) {
                boolean isCell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                //boolean notRoaming = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
                if (isCell) {
                    evaluate();
                }
            }

            @Override
            public void onLost(@NonNull Network network) {
                NullVpnService.ensureStopped(getApplicationContext());
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24+
            cm.registerDefaultNetworkCallback(networkCallback);
        } else { // API 23
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(req, networkCallback);
        }
        */

        if (Build.VERSION.SDK_INT >= 31)
        {
            class MyTelephonyCallback extends TelephonyCallback implements TelephonyCallback.ServiceStateListener {
                @Override
                public void onServiceStateChanged(@NonNull ServiceState serviceState) {
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

        mobileTrafficMonitor = new MobileTrafficMonitor(getApplicationContext(), usingMobile -> {
            if(usingMobile != usingMobileTraffic) {
                usingMobileTraffic = usingMobile;
                evaluate();
            }
        });
        mobileTrafficMonitor.start();

        return START_STICKY;
    }

    private void evaluate() {
        String iso = getCurrentCountry();
        if(iso.isEmpty()) {
            // This is the case of SIM ejection.
            return;
        }

        CountryList cfg = listManager.loadActiveConfig();
        if(cfg == null) {
            NullVpnService.ensureStopped(getApplicationContext());
            return;
        }

        boolean blocked = cfg.isBlocked(iso);
        if (blocked && usingMobileTraffic) NullVpnService.ensureRunning(getApplicationContext());
        else NullVpnService.ensureStopped(getApplicationContext());
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
