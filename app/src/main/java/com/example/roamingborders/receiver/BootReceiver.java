package com.example.roamingborders.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

import com.example.roamingborders.MainActivity;
import com.example.roamingborders.service.CellMonitorService;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        // At this point we assume that
        Intent prepareIntent = VpnService.prepare(context);
        if (prepareIntent == null && !MainActivity.isKillSwitchActive(context)) {
            CellMonitorService.ensureRunning(context);
        }
    }
}
