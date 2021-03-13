package com.nutomic.syncthingandroid.views;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.AttributeSet;
import android.util.Log;
import java.util.List;

public class WifiSsidPreference extends WifiSsidPreferenceBase {

    public WifiSsidPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WifiSsidPreference(Context context) {
        super(context, null);
    }

}
