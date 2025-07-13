package com.example.roamingborders.monitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import java.util.Objects;

public final class MobileTrafficMonitor {

    public interface Callback {
        void onMobileStateChanged(boolean usingMobile);
    }

    public MobileTrafficMonitor(Context ctx, Callback cb) {
        this.cm = (ConnectivityManager)
                Objects.requireNonNull(ctx.getSystemService(Context.CONNECTIVITY_SERVICE));
        this.cb = cb;
    }

    public void start() {
        if (callback != null) return;
        registerCallback();
        dispatch(cm.getActiveNetwork(), cm.getNetworkCapabilities(cm.getActiveNetwork()));
    }

    public void stop() {
        if (callback != null) {
            cm.unregisterNetworkCallback(callback);
            callback = null;
            lastState = null;
        }
    }

    private final ConnectivityManager cm;
    private final Callback            cb;
    private ConnectivityManager.NetworkCallback callback;
    private Boolean lastState;

    private void registerCallback() {

        callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onCapabilitiesChanged(Network net, NetworkCapabilities caps) {
                if (net.equals(cm.getActiveNetwork())) dispatch(net, caps);
            }
            @Override public void onLost(Network net) {
                if (net.equals(cm.getActiveNetwork())) dispatch(null, null);
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cm.registerDefaultNetworkCallback(callback);               // API 24+
        } else {
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();                                          // API 23
            cm.registerNetworkCallback(req, callback);
        }
    }

    private void dispatch(Network defNet, NetworkCapabilities caps) {

        boolean validated = caps != null &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);

        boolean hasWifi = caps != null &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);

        boolean hasCell = caps != null &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

        boolean hasVpn = caps != null &&
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN);

        boolean usingMobile;

        if (!validated) {
            // No internet: conservatively assume that signal is required.
            usingMobile = true;

        } else if (hasCell) {
            usingMobile = true;

        } else if (hasVpn) {
            /*
            // VPN: prüfen, ob darunter WLAN oder Mobilfunk steckt
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {          // API 29+
                List<Network> underlying = caps.getUnderlyingNetworks();
                boolean cell = false, wifi = false;
                if (underlying != null) {
                    for (Network u : underlying) {
                        NetworkCapabilities uc = cm.getNetworkCapabilities(u);
                        if (uc == null) continue;
                        wifi |= uc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                        cell |= uc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
                    }
                }
                usingMobile = cell && !wifi;   // nur wenn ausschließlich CELLULAR
            } else
            */
            {

                // API 23–28: Transportflags des VPN enthalten Unternetze
                usingMobile = !hasWifi;        // validiertes WLAN → false, sonst true
            }
        } else {
            usingMobile = false;
        }

        if (lastState == null || lastState != usingMobile) {
            lastState = usingMobile;
            cb.onMobileStateChanged(usingMobile);
        }
    }
}
