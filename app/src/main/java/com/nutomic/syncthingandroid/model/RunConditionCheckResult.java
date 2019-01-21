package com.nutomic.syncthingandroid.model;

import com.nutomic.syncthingandroid.R;

import java.util.Collections;
import java.util.List;

public class RunConditionCheckResult {

    public enum BlockerReason {
        ON_BATTERY(R.string.reason_not_charging),
        ON_CHARGER(R.string.reason_not_on_battery_power),
        POWERSAVING_ENABLED(R.string.reason_not_while_power_saving),
        GLOBAL_SYNC_DISABLED(R.string.reason_not_while_auto_sync_data_disabled),
        WIFI_SSID_NOT_WHITELISTED(R.string.reason_not_on_whitelisted_wifi),
        WIFI_WIFI_IS_METERED(R.string.reason_not_nonmetered_wifi),
        NO_NETWORK_OR_FLIGHTMODE(R.string.reason_on_flight_mode),
        NO_MOBILE_CONNECTION(R.string.reason_not_on_mobile_data),
        NO_WIFI_CONNECTION(R.string.reason_not_on_wifi),

        private final int resId;

        BlockerReason(int resId) {
            this.resId = resId;
        }

        public int getResId() {
            return resId;
        }
    }

    public static final RunConditionCheckResult SHOULD_RUN = new RunConditionCheckResult();

    private final boolean mShouldRun;
    private final List<BlockerReason> mBlockReasons;

    /**
     * Use SHOULD_RUN instead.
     * Note: of course anybody could still construct it by providing an empty list to the other
     * constructor.
     */
    private RunConditionCheckResult() {
        this(Collections.emptyList());
    }

    public RunConditionCheckResult(List<BlockerReason> blockReasons) {
        mBlockReasons = Collections.unmodifiableList(blockReasons);
        mShouldRun = blockReasons.isEmpty();
    }


    public List<BlockerReason> getBlockReasons() {
        return mBlockReasons;
    }

    public boolean isShouldRun() {
        return mShouldRun;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RunConditionCheckResult that = (RunConditionCheckResult) o;

        if (mShouldRun != that.mShouldRun) return false;
        return mBlockReasons.equals(that.mBlockReasons);
    }

    @Override
    public int hashCode() {
        int result = (mShouldRun ? 1 : 0);
        result = 31 * result + mBlockReasons.hashCode();
        return result;
    }
}
