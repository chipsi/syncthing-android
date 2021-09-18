package com.nutomic.syncthingandroid.service;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import com.nutomic.syncthingandroid.activities.PhotoShootActivity;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileCamera extends TileService {
    public QuickSettingsTileCamera() {
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
    }

    @Override
    public void onClick() {
        super.onClick();
        startActivity(new Intent(this, PhotoShootActivity.class));
    }
}
