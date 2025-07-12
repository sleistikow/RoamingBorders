package com.example.roamingborders.monitor;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import androidx.annotation.RequiresApi;

/**
 * Meldet, ob der *aktuelle* Default-Datenpfad Mobilfunk nutzt.
 * Übergibt true  ⇒ Traffic geht über MOBILE
 *           false ⇒ Traffic geht NICHT über MOBILE (val. WLAN oder gar kein Netz)
 */
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

    /** Registrierung – am besten in onStart() / onResume() */
    public void start() {
        if (nc != null) return;                     // schon aktiv

        nc = new ConnectivityManager.NetworkCallback() {

            private void handle(Network net, NetworkCapabilities caps) {
                // Wir interessieren uns NUR für das aktuelle Default-Netz
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
                // kein Default-Netz mehr → sicherheitshalber „MOBILE=true?“
                if (net.equals(cm.getActiveNetwork())) {
                    cb.onMobileStateChanged(true);
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {          // API 24+
            cm.registerDefaultNetworkCallback(nc);                     // 1-Zeiler
        } else {
            // Fallback für API 23: alle Internet-Netze beobachten
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(req, nc);
        }

        // Initialzustand melden
        Network net = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(net);
        nc.onCapabilitiesChanged(net, caps);
    }

    /** Abmeldung – z. B. in onStop() / onPause() */
    public void stop() {
        if (nc != null) {
            cm.unregisterNetworkCallback(nc);
            nc = null;
        }
    }
}
