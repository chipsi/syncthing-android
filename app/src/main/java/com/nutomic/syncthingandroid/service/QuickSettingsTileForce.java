package com.nutomic.syncthingandroid.service;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.nutomic.syncthingandroid.R;

import javax.inject.Inject;

import static com.nutomic.syncthingandroid.service.RunConditionMonitor.ACTION_UPDATE_SHOULDRUN_DECISION;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickSettingsTileForce extends TileService {
    public QuickSettingsTileForce() {

    }
    private Context mContext;
    @Inject
    SharedPreferences mPreferences;
    private Resources res;

    @Override
    public void onStartListening() {
        Tile tile = getQsTile();
        if (tile != null) {
            mContext = getApplication().getApplicationContext();
            res = mContext.getResources();
            mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            boolean syncthingRunning = false;
            for (ActivityManager.RunningServiceInfo service : am.getRunningServices(Integer.MAX_VALUE)) {
                if (SyncthingService.class.getName().equals(service.service.getClassName())) {
                    syncthingRunning = true;
                }
            }
            if (!syncthingRunning) {
                tile.setState(Tile.STATE_UNAVAILABLE);
                tile.updateTile();
                return;
            }

            updateTileState(tile, mPreferences.getInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, Constants.BTNSTATE_NO_FORCE_START_STOP));
        }
        super.onStartListening();
    }

    @Override
    public void onClick() {
        Tile tile = getQsTile();
        int newState;
        switch (mPreferences.getInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, Constants.BTNSTATE_NO_FORCE_START_STOP)) {
            case Constants.BTNSTATE_FORCE_START:
                newState = Constants.BTNSTATE_FORCE_STOP;
                break;
            case Constants.BTNSTATE_NO_FORCE_START_STOP:
                newState = Constants.BTNSTATE_FORCE_START;
                break;
            case Constants.BTNSTATE_FORCE_STOP:
            default:
                newState = Constants.BTNSTATE_NO_FORCE_START_STOP;
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(Constants.PREF_BTNSTATE_FORCE_START_STOP, newState);
        editor.apply();

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
        Intent intent = new Intent(ACTION_UPDATE_SHOULDRUN_DECISION);
        localBroadcastManager.sendBroadcast(intent);
        // if status fragement is open, force start stop button is not updated!

        updateTileState(tile, newState);
        tile.updateTile();
    }

    private void updateTileState(Tile tile, int force) {
        switch (force) {
            case Constants.BTNSTATE_FORCE_START:
                tile.setLabel(res.getString(R.string.qs_force_running));
                tile.setState(Tile.STATE_ACTIVE);
                break;
            case Constants.BTNSTATE_FORCE_STOP:
                tile.setLabel(res.getString(R.string.qs_force_stopped));
                tile.setState(Tile.STATE_ACTIVE);
                break;
            case Constants.BTNSTATE_NO_FORCE_START_STOP:
            default:
                tile.setLabel(res.getString(R.string.qs_force_none));
                tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

}
