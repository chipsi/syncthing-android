package com.nutomic.syncthingandroid.util;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Environment;
// import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class PermissionUtil {

    // private static final String TAG = "PermissionUtil";

    public static boolean haveStoragePermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        int permissionState = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

}
