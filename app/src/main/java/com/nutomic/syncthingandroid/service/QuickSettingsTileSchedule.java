package com.nutomic.syncthingandroid.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import javax.inject.Inject;

import static com.nutomic.syncthingandroid.service.RunConditionMonitor.ACTION_SYNC_TRIGGER_FIRED;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileSchedule extends TileService {
    public QuickSettingsTileSchedule() {

    }
    private Context mContext;
    @Inject
    SharedPreferences mPreferences;

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile != null) {
            mContext = getApplication().getApplicationContext();
            mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            if (!mPreferences.getBoolean(Constants.PREF_RUN_ON_TIME_SCHEDULE,false)) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.setLabel("time schedule disabled");
                tile.updateTile();
                return;
            }
            // how to see if syncthing is running?

            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel("start/stop 5 min");
            tile.updateTile();
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        Intent intent = new Intent(ACTION_SYNC_TRIGGER_FIRED);
        localBroadcastManager.sendBroadcast(intent);
    }
}
