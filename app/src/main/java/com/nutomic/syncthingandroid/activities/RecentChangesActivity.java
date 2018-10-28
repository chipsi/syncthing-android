package com.nutomic.syncthingandroid.activities;

// import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
// import android.text.format.Formatter;
import android.util.Log;
import android.view.View;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.views.ChangeListAdapter;
import com.nutomic.syncthingandroid.views.ChangeListAdapter.ItemClickListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Holds a RecyclerView that shows tips and tricks.
 */
public class RecentChangesActivity extends SyncthingActivity {

    private static final String TAG = "RecentChangesActivity";

    private RecyclerView mRecyclerView;
    private ChangeListAdapter mRecentChangeAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_changes);
        mRecyclerView = findViewById(R.id.changes_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecentChangeAdapter = new ChangeListAdapter(this);

        // Fill title and text content.
        // mRecentChangeAdapter.add(getString(R.string.tip_sync_on_local_network_title), getString(R.string.tip_sync_on_local_network_text));

        // Set onClick listener and add adapter to recycler view.
        mRecentChangeAdapter.setOnClickListener(
            new ItemClickListener() {
                @Override
                public void onItemClick(View view, String itemTitle, String itemText) {
                    Log.v(TAG, "User clicked item with title \'" + itemTitle + "\'");
                    /**
                     * Future improvement:
                     * Collapse texts to the first three lines and open a DialogFragment
                     * if the user clicks an item from the list.
                     */
                }
            }
        );
        mRecyclerView.setAdapter(mRecentChangeAdapter);
    }
}
