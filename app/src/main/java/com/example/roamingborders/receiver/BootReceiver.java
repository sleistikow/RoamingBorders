package com.example.roamingborders.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.roamingborders.service.CellMonitorService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        CellMonitorService.ensureRunning(context);

        /*
        // Device-protected Storage, wenn vor Entsperren gestartet
        Context prefCtx = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ? ctx.createDeviceProtectedStorageContext()
                : ctx;

        boolean wanted = PreferenceManager
                .getDefaultSharedPreferences(prefCtx)
                .getBoolean("firewall_enabled", true);

        if (wanted) {
            ContextCompat.startForegroundService(
                    ctx, new Intent(ctx, NullVPN.class));
        }

         */
    }
}
