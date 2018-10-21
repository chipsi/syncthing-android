package com.nutomic.syncthingandroid.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ImmutableMap;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.activities.MainActivity;
import com.nutomic.syncthingandroid.activities.SettingsActivity;
import com.nutomic.syncthingandroid.activities.TipsAndTricksActivity;
import com.nutomic.syncthingandroid.activities.WebGuiActivity;
import com.nutomic.syncthingandroid.http.ImageGetRequest;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.RestApi;
import com.nutomic.syncthingandroid.service.SyncthingService;

import java.net.URL;


/**
 * Displays information about the local device.
 */
public class DrawerFragment extends Fragment implements SyncthingService.OnServiceStateChangeListener,
        View.OnClickListener {

    private SyncthingService.State mServiceState = SyncthingService.State.INIT;

    private static final String TAG = "DrawerFragment";

    /**
     * These buttons might be accessible if the screen is big enough
     * or the user can scroll the drawer to access them.
     */
    private TextView mVersion = null;
    private TextView mDrawerActionShowQrCode;
    private TextView mDrawerActionWebGui;
    private TextView mDrawerActionImportExport;
    private TextView mDrawerActionRestart;
    private TextView mDrawerTipsAndTricks;

    /**
     * These buttons are always visible.
     */
    private TextView mDrawerActionSettings;
    private TextView mDrawerActionExit;

    private MainActivity mActivity;
    private SharedPreferences sharedPreferences = null;

    @Override
    public void onServiceStateChange(SyncthingService.State currentState) {
        mServiceState = currentState;
        updateButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLabels();
        updateButtons();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Populates views and menu.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mActivity = (MainActivity) getActivity();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

        mVersion                    = view.findViewById(R.id.version);
        mDrawerActionShowQrCode     = view.findViewById(R.id.drawerActionShowQrCode);
        mDrawerActionWebGui         = view.findViewById(R.id.drawerActionWebGui);
        mDrawerActionImportExport   = view.findViewById(R.id.drawerActionImportExport);
        mDrawerActionRestart        = view.findViewById(R.id.drawerActionRestart);
        mDrawerTipsAndTricks        = view.findViewById(R.id.drawerActionTipsAndTricks);
        mDrawerActionSettings       = view.findViewById(R.id.drawerActionSettings);
        mDrawerActionExit           = view.findViewById(R.id.drawerActionExit);

        // Add listeners to buttons.
        mDrawerActionShowQrCode.setOnClickListener(this);
        mDrawerActionWebGui.setOnClickListener(this);
        mDrawerActionImportExport.setOnClickListener(this);
        mDrawerActionRestart.setOnClickListener(this);
        mDrawerTipsAndTricks.setOnClickListener(this);
        mDrawerActionSettings.setOnClickListener(this);
        mDrawerActionExit.setOnClickListener(this);

        updateLabels();
        updateButtons();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Update static info labels.
     */
    private void updateLabels() {
        if (sharedPreferences != null && mVersion != null) {
            mVersion.setText(sharedPreferences.getString(Constants.PREF_LAST_BINARY_VERSION, ""));
        }
    }

    /**
     * Update action button availability.
     */
    private void updateButtons() {
        Boolean synthingRunning = mServiceState == SyncthingService.State.ACTIVE;

        // Show buttons if syncthing is running.
        mVersion.setVisibility(synthingRunning ? View.VISIBLE : View.GONE);
        mDrawerActionShowQrCode.setVisibility(synthingRunning ? View.VISIBLE : View.GONE);
        mDrawerActionWebGui.setVisibility(synthingRunning ? View.VISIBLE : View.GONE);
        mDrawerActionRestart.setVisibility(synthingRunning ? View.VISIBLE : View.GONE);
        mDrawerTipsAndTricks.setVisibility(View.VISIBLE);
        mDrawerActionExit.setVisibility(View.VISIBLE);
    }

    /**
     * Gets QRCode and displays it in a Dialog.
     */
    private void showQrCode() {
        RestApi restApi = mActivity.getApi();
        if (restApi == null) {
            Toast.makeText(mActivity, R.string.syncthing_terminated, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String apiKey = restApi.getGui().apiKey;
            String deviceId = restApi.getLocalDevice().deviceID;
            URL url = restApi.getUrl();
            //The QRCode request takes one paramteer called "text", which is the text to be converted to a QRCode.
            new ImageGetRequest(mActivity, url, ImageGetRequest.QR_CODE_GENERATOR, apiKey,
                    ImmutableMap.of("text", deviceId),qrCodeBitmap -> {
                mActivity.showQrCodeDialog(deviceId, qrCodeBitmap);
                mActivity.closeDrawer();
            }, error -> Toast.makeText(mActivity, R.string.could_not_access_deviceid, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "showQrCode", e);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.drawerActionShowQrCode:
                showQrCode();
                break;
            case R.id.drawerActionWebGui:
                startActivity(new Intent(mActivity, WebGuiActivity.class));
                mActivity.closeDrawer();
                break;
            case R.id.drawerActionImportExport:
                intent = new Intent(mActivity, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_OPEN_SUB_PREF_SCREEN, "category_import_export");
                startActivity(intent);
                mActivity.closeDrawer();
                break;
            case R.id.drawerActionRestart:
                mActivity.showRestartDialog();
                mActivity.closeDrawer();
                break;
            case R.id.drawerActionTipsAndTricks:
                startActivity(new Intent(mActivity, TipsAndTricksActivity.class));
                mActivity.closeDrawer();
                break;
            case R.id.drawerActionSettings:
                startActivity(new Intent(mActivity, SettingsActivity.class));
                mActivity.closeDrawer();
                break;
            case R.id.drawerActionExit:
                if (sharedPreferences != null && sharedPreferences.getBoolean(Constants.PREF_START_SERVICE_ON_BOOT, false)) {
                    /**
                     * App is running as a service. Show an explanation why exiting syncthing is an
                     * extraordinary request, then ask the user to confirm.
                     */
                    AlertDialog mExitConfirmationDialog = new AlertDialog.Builder(mActivity)
                            .setTitle(R.string.dialog_exit_while_running_as_service_title)
                            .setMessage(R.string.dialog_exit_while_running_as_service_message)
                            .setPositiveButton(R.string.yes, (d, i) -> {
                                doExit();
                            })
                            .setNegativeButton(R.string.no, (d, i) -> {})
                            .show();
                } else {
                    // App is not running as a service.
                    doExit();
                }
                mActivity.closeDrawer();
                break;
        }
    }

    private void doExit() {
        if (mActivity == null || mActivity.isFinishing()) {
            return;
        }
        Log.i(TAG, "Exiting app on user request");
        mActivity.stopService(new Intent(mActivity, SyncthingService.class));
        mActivity.finish();
    }
}
