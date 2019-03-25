package com.nutomic.syncthingandroid.util;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.nutomic.syncthingandroid.service.SyncTriggerJobService;

public class JobUtils {

    private static final String TAG = "JobUtils";

    private static final int TOLERATED_INACCURACY_IN_SECONDS = 120;

    public static void scheduleSyncTriggerServiceJob(Context context, int delayInSeconds) {
        ComponentName serviceComponent = new ComponentName(context, SyncTriggerJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);

        // Wait at least "delayInSeconds".
        builder.setMinimumLatency(delayInSeconds * 1000);

        // Maximum tolerated delay.
        builder.setOverrideDeadline((delayInSeconds + TOLERATED_INACCURACY_IN_SECONDS) * 1000);

        // Schedule the start of "SyncTriggerJobService" in "X" seconds.
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.schedule(builder.build());
        Log.e(TAG, "Scheduled job to run SyncTriggerJobService in " + Integer.toString(delayInSeconds / 60) + " minutes.");
    }

    public static void cancelAllScheduledJobs(Context context) {
        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        jobScheduler.cancelAll();
    }
}
