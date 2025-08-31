package com.sleistikow.roamingborders.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sleistikow.roamingborders.MainActivity;
import com.sleistikow.roamingborders.service.CellMonitorService;
import com.sleistikow.roamingborders.util.NotificationHelper;
import com.sleistikow.roamingborders.util.PermissionHelper;

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
