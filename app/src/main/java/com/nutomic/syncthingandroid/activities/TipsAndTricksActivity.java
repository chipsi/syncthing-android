package com.nutomic.syncthingandroid.activities;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
// import android.util.Log;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.views.TipListAdapter;

import java.util.ArrayList;

/**
 * Holds a RecyclerView that shows tips and tricks.
 */
public class TipsAndTricksActivity extends SyncthingActivity {

    // private static final String TAG = "TipsAndTricksActivity";

    private RecyclerView mRecyclerView;
    private TipListAdapter mTipListAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips_and_tricks);
        mRecyclerView = findViewById(R.id.tip_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Add adapter and tip content.
        mTipListAdapter = new TipListAdapter(this);
        for (int i = 1; i < 100; i++) {
            mTipListAdapter.add("Title " + i, "Text " + i + "Today I want to tell you a story about Syncthing 1.\nToday I want to tell you a story about Syncthing 2.\nToday I want to tell you a story about Syncthing 3. Today I want to tell you a story about Syncthing 4. Today I want to tell you a story about Syncthing 5. Today I want to tell you a story about Syncthing 6. Today I want to tell you a story about Syncthing 7.\nTest.");
        }
        mRecyclerView.setAdapter(mTipListAdapter);
    }

}
