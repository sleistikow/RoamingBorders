package com.example.roamingborders.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.roamingborders.MainActivity;
import com.example.roamingborders.service.CellMonitorService;
import com.example.roamingborders.util.NotificationHelper;
import com.example.roamingborders.util.PermissionHelper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        if (!MainActivity.isFirstStart(context) || !MainActivity.isGuardDisabled(context)) {
            if (PermissionHelper.mandatoryPermissionsGranted(context)) {
                CellMonitorService.ensureRunning(context);
            } else {
                NotificationHelper.showMissingPermissions(context);
            }
        }
    }
}
