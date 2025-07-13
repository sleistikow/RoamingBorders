package com.example.roamingborders.monitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.RequiresApi;

public class MobileTrafficMonitor {

    public interface Callback {
        void onMobileStateChanged(boolean usingMobile);
    }

    private final ConnectivityManager cm;
    private final Callback cb;
    private ConnectivityManager.NetworkCallback nc;

    public MobileTrafficMonitor(Context ctx, Callback cb) {
        this.cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.cb = cb;
    }

    public void start() {
        if (nc != null) return;

        nc = new ConnectivityManager.NetworkCallback() {

            private void handle(Network net, NetworkCapabilities caps) {
                if (!net.equals(cm.getActiveNetwork())) return;

                boolean validated = caps != null
                        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
                boolean mobile    = caps != null
                        && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);

                cb.onMobileStateChanged(validated && mobile);
            }

            @Override
            public void onCapabilitiesChanged(Network net, NetworkCapabilities caps) {
                handle(net, caps);
            }

            @Override
            public void onLost(Network net) {
                if (net.equals(cm.getActiveNetwork())) {
                    cb.onMobileStateChanged(true);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {          // API 24+
            cm.registerDefaultNetworkCallback(nc);
        } else {
            // Fallback for API 23: observe all internet connections
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(req, nc);
        }

        Network net = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        nc.onCapabilitiesChanged(net, caps);
    }

    public void stop() {
        if (nc != null) {
            cm.unregisterNetworkCallback(nc);
            nc = null;
        }
    }
}
