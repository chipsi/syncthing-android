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

import static com.nutomic.syncthingandroid.service.RunConditionMonitor.ACTION_UPDATE_SHOULDRUN_DECISION;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileForce extends TileService {
    public QuickSettingsTileForce() {

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
            setTileState(tile, Constants.BTNSTATE_NO_FORCE_START_STOP != mPreferences.getInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, Constants.BTNSTATE_NO_FORCE_START_STOP));
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        boolean isForceOn = Constants.BTNSTATE_NO_FORCE_START_STOP != mPreferences.getInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, Constants.BTNSTATE_NO_FORCE_START_STOP);
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, isForceOn ? Constants.BTNSTATE_NO_FORCE_START_STOP : Constants.BTNSTATE_FORCE_START);
        editor.apply();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        Intent intent = new Intent(ACTION_UPDATE_SHOULDRUN_DECISION);
        localBroadcastManager.sendBroadcast(intent);
        // if status fragement is open, force start stop button is not updated!

        setTileState(tile, !isForceOn);
        tile.updateTile();
    }

    private void setTileState(Tile tile, boolean isForceOn) {
        tile.setState(isForceOn ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel(isForceOn ? "follow run conditions" : "force start");
        tile.updateTile();
    }

}
