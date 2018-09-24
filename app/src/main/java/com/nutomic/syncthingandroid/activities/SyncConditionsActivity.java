package com.nutomic.syncthingandroid.activities;

// import android.annotation.SuppressLint;
import android.app.Activity;
// import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
// import android.os.Build;
import android.os.Bundle;
// import android.os.Environment;
import android.os.IBinder;
/*
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
*/
import android.view.Menu;
import android.view.MenuItem;
/*
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
*/

import com.google.common.collect.Sets;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.SyncthingApp;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.SyncthingService;
import com.nutomic.syncthingandroid.service.SyncthingServiceBinder;
import com.nutomic.syncthingandroid.util.Util;

/*
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
*/

/**
 * Activity that allows selecting a directory in the local file system.
 */
public class SyncConditionsActivity extends SyncthingActivity
        implements SyncthingService.OnServiceStateChangeListener {

    private static final String EXTRA_OBJECT_PREFIX_AND_ID =
            "com.nutomic.syncthingandroid.activities.SyncConditionsActivity.OBJECT_PREFIX_AND_ID";

    private static final String EXTRA_OBJECT_READABLE_NAME =
            "com.nutomic.syncthingandroid.activities.SyncConditionsActivity.OBJECT_READABLE_NAME";

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

        /*
        if (getIntent().hasExtra(EXTRA_INITIAL_DIRECTORY)) {
            displayFolder(new File(getIntent().getStringExtra(EXTRA_INITIAL_DIRECTORY)));
        } else {
            displayRoot();
        }

        Boolean prefUseRoot = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Constants.PREF_USE_ROOT, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && !prefUseRoot) {
            Toast.makeText(this, R.string.kitkat_external_storage_warning, Toast.LENGTH_LONG)
                    .show();
        }
        */
    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // ToDo getMenuInflater().inflate(R.menu.folder_picker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
            case R.id.create_folder:
                final EditText et = new EditText(this);
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.create_folder)
                        .setView(et)
                        .setPositiveButton(android.R.string.ok,
                                (dialogInterface, i) -> createFolder(et.getText().toString())
                        )
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                dialog.setOnShowListener(dialogInterface -> ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .showSoftInput(et, InputMethodManager.SHOW_IMPLICIT));
                dialog.show();
                return true;
            case R.id.select:
                Intent intent = new Intent()
                        .putExtra(EXTRA_RESULT_DIRECTORY, Util.formatPath(mLocation.getAbsolutePath()));
                setResult(Activity.RESULT_OK, intent);
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            */
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Cancel without saving changes.
     */
    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
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
