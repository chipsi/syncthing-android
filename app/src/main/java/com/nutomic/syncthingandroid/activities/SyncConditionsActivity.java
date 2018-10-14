package com.nutomic.syncthingandroid.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.common.collect.Sets;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.SyncthingApp;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.SyncthingService;
import com.nutomic.syncthingandroid.service.SyncthingServiceBinder;
import com.nutomic.syncthingandroid.util.Util;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import static android.support.v4.view.MarginLayoutParamsCompat.setMarginEnd;
import static android.support.v4.view.MarginLayoutParamsCompat.setMarginStart;
import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Activity that allows selecting a directory in the local file system.
 */
public class SyncConditionsActivity extends SyncthingActivity
        implements SyncthingService.OnServiceStateChangeListener {

    private static final String TAG = "SyncConditionsActivity";

    private static final String EXTRA_OBJECT_PREFIX_AND_ID =
            "com.nutomic.syncthingandroid.activities.SyncConditionsActivity.OBJECT_PREFIX_AND_ID";

    private static final String EXTRA_OBJECT_READABLE_NAME =
            "com.nutomic.syncthingandroid.activities.SyncConditionsActivity.OBJECT_READABLE_NAME";

    // UI elements
    private SwitchCompat mSyncOnWifi;
    private SwitchCompat mSyncOnWhitelistedWifi;
    private ViewGroup mWifiSsidContainer;
    private SwitchCompat mSyncOnMeteredWifi;
    private SwitchCompat mSyncOnMobileData;

    /**
     * Shared preferences names for custom per-folder settings.
     */
    private String mObjectPrefixAndId;
    private String mPrefSyncOnWifi;
    private String mPrefSyncOnWhitelistedWifi;
    private String mPrefSelectedWhitelistSsid;
    private String mPrefSyncOnMeteredWifi;
    private String mPrefSyncOnMobileData;

    private String mObjectReadableName;
    private boolean mFolderNeedsToUpdate = false;

    @Inject
    SharedPreferences mPreferences;

    public static Intent createIntent(Context context, String objectPrefixAndId, String objectReadableName) {
        Intent intent = new Intent(context, SyncConditionsActivity.class);
        intent.putExtra(EXTRA_OBJECT_PREFIX_AND_ID, objectPrefixAndId);
        intent.putExtra(EXTRA_OBJECT_READABLE_NAME, objectReadableName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((SyncthingApp) getApplication()).component().inject(this);

        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_OBJECT_PREFIX_AND_ID) ||
                !intent.hasExtra(EXTRA_OBJECT_READABLE_NAME)) {
            Log.e(TAG, "onCreate extra missing");
            return;
        }
        mObjectReadableName = intent.getStringExtra(EXTRA_OBJECT_READABLE_NAME);

        // Display content and get views.
        setContentView(R.layout.activity_sync_conditions);
        mSyncOnWifi = findViewById(R.id.sync_on_wifi_title);
        mSyncOnWhitelistedWifi = findViewById(R.id.sync_on_whitelisted_wifi_title);
        mWifiSsidContainer = findViewById(R.id.wifiSsidContainer);
        mSyncOnMeteredWifi = findViewById(R.id.sync_on_metered_wifi_title);
        mSyncOnMobileData = findViewById(R.id.sync_on_mobile_data_title);

        // Generate shared preferences names.
        mObjectPrefixAndId = intent.getStringExtra(EXTRA_OBJECT_PREFIX_AND_ID);
        mPrefSyncOnWifi = mObjectPrefixAndId + "_" + Constants.PREF_RUN_ON_WIFI;
        mPrefSyncOnWhitelistedWifi = mObjectPrefixAndId + "_" + "use_wifi_whitelist";
        mPrefSelectedWhitelistSsid = mObjectPrefixAndId + "_" + Constants.PREF_WIFI_SSID_WHITELIST;
        mPrefSyncOnMeteredWifi = mObjectPrefixAndId + "_" + Constants.PREF_RUN_ON_METERED_WIFI;
        mPrefSyncOnMobileData = mObjectPrefixAndId + "_" + Constants.PREF_RUN_ON_MOBILE_DATA;

        /**
         * Load global run conditions.
         */
        Boolean globalRunOnWifiEnabled = mPreferences.getBoolean(Constants.PREF_RUN_ON_WIFI, false);
        Boolean globalWhitelistEnabled = !mPreferences.getStringSet(Constants.PREF_WIFI_SSID_WHITELIST, new HashSet<>())
                .isEmpty();
        Set<String> globalWhitelistedSsid = mPreferences.getStringSet(Constants.PREF_WIFI_SSID_WHITELIST, new HashSet<>());
        Boolean globalRunOnMeteredWifiEnabled = mPreferences.getBoolean(Constants.PREF_RUN_ON_METERED_WIFI, false);
        Boolean globalRunOnMobileDataEnabled = mPreferences.getBoolean(Constants.PREF_RUN_ON_MOBILE_DATA, false);

        /**
         * Load custom folder preferences. If unset, use global setting as default.
         */
        mSyncOnWifi.setChecked(mPreferences.getBoolean(mPrefSyncOnWifi, globalRunOnWifiEnabled));
        mSyncOnWifi.setEnabled(globalRunOnWifiEnabled);

        mSyncOnWhitelistedWifi.setChecked(mPreferences.getBoolean(mPrefSyncOnWhitelistedWifi, globalWhitelistEnabled));
        mSyncOnWhitelistedWifi.setEnabled(globalWhitelistEnabled);

        mSyncOnMeteredWifi.setChecked(mPreferences.getBoolean(mPrefSyncOnMeteredWifi, globalRunOnMeteredWifiEnabled));
        mSyncOnMeteredWifi.setEnabled(globalRunOnMeteredWifiEnabled);

        mSyncOnMobileData.setChecked(mPreferences.getBoolean(mPrefSyncOnMobileData, globalRunOnMobileDataEnabled));
        mSyncOnMobileData.setEnabled(globalRunOnMobileDataEnabled);

        // Read selected WiFi Ssid whitelist items.
        Set<String> selectedWhitelistedSsid = mPreferences.getStringSet(mPrefSelectedWhitelistSsid, new HashSet<>());
        // Removes any network that is no longer part of the global WiFi Ssid whitelist.
        selectedWhitelistedSsid.retainAll(globalWhitelistedSsid);

        // Populate WiFi Ssid whitelist.
        mWifiSsidContainer.removeAllViews();
        // from JavaDoc: Note that you must not modify the set instance returned by this call.
        // therefore required to make a defensive copy of the elements
        globalWhitelistedSsid = new HashSet<>(globalWhitelistedSsid);
        if (!globalWhitelistEnabled) {
            // Add empty WiFi Ssid ListView.
            int height = (int) TypedValue.applyDimension(COMPLEX_UNIT_DIP, 48, getResources().getDisplayMetrics());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(WRAP_CONTENT, height);
            int dividerInset = getResources().getDimensionPixelOffset(R.dimen.material_divider_inset);
            int contentInset = getResources().getDimensionPixelOffset(R.dimen.abc_action_bar_content_inset_material);
            setMarginStart(params, dividerInset);
            setMarginEnd(params, contentInset);
            TextView emptyView = new TextView(mWifiSsidContainer.getContext());
            emptyView.setGravity(CENTER_VERTICAL);
            emptyView.setText(R.string.wifi_ssid_whitelist_empty);
            mWifiSsidContainer.addView(emptyView, params);
        } else {
            for (String wifiSsid : globalWhitelistedSsid) {
                // Strip quotes and add  WiFi Ssid to view.
                wifiSsid = wifiSsid.replaceFirst("^\"", "").replaceFirst("\"$", "");
                LayoutInflater layoutInflater = getLayoutInflater();
                layoutInflater.inflate(R.layout.item_wifi_ssid_form, mWifiSsidContainer);
                SwitchCompat wifiSsidView = (SwitchCompat) mWifiSsidContainer.getChildAt(mWifiSsidContainer.getChildCount()-1);
                wifiSsidView.setOnCheckedChangeListener(null);
                wifiSsidView.setChecked(selectedWhitelistedSsid.contains(wifiSsid));
                wifiSsidView.setText(wifiSsid);
                wifiSsidView.setTag(wifiSsid);
                wifiSsidView.setOnCheckedChangeListener(mCheckedListener);
            }
        }
        mWifiSsidContainer.setEnabled(!globalWhitelistEnabled);
    }

    private final CompoundButton.OnCheckedChangeListener mCheckedListener =
            new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton view, boolean isChecked) {
            switch (view.getId()) {
                default:
                    mFolderNeedsToUpdate = true;
                    break;
            }
        }
    };

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        SyncthingServiceBinder syncthingServiceBinder = (SyncthingServiceBinder) iBinder;
        syncthingServiceBinder.getService().registerOnServiceStateChangeListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SyncthingService syncthingService = getService();
        if (syncthingService != null) {
            syncthingService.unregisterOnServiceStateChangeListener(this::onServiceStateChange);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Cancel without saving changes.
     */
    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onServiceStateChange(SyncthingService.State currentState) {
        if (!isFinishing() && currentState != SyncthingService.State.ACTIVE) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
    }

}
