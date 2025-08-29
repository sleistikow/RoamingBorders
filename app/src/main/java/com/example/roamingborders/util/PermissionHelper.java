package com.sleistikow.roamingborders.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static boolean mandatoryPermissionsGranted(Context ctx) {
        return getMissingPermissions(ctx, true).isEmpty() && isVpnPermissionGranted(ctx);
    }

    public static boolean isVpnPermissionGranted(Context ctx) {
        return VpnService.prepare(ctx) == null;
    }

    public static List<String> getRequiredPermissions(boolean mandatoryOnly) {
        List<String> required = new ArrayList<>();

        // We need this for receiving telephony callbacks.
        // TODO: double check!
        required.add(Manifest.permission.READ_PHONE_STATE);

        // We need this to post notifications.
        if (!mandatoryOnly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        return required;
    }

    public static List<String> getMissingPermissions(Context ctx, boolean mandatoryOnly) {
        List<String> missing = new ArrayList<>();
        for (String req : getRequiredPermissions(mandatoryOnly)) {
            if (ContextCompat.checkSelfPermission(ctx, req) != PackageManager.PERMISSION_GRANTED) {
                missing.add(req);
            }
        }
        return missing;
    }

}
