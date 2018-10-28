package com.nutomic.syncthingandroid.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
// import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nutomic.syncthingandroid.R;

import java.util.ArrayList;

public class ChangeListAdapter extends RecyclerView.Adapter<ChangeListAdapter.ViewHolder> {

    // private static final String TAG = "ChangeListAdapter";

    private ArrayList<ChangeEntry> mChangeData = new ArrayList<ChangeEntry>();
    private ItemClickListener mOnClickListener;
    private LayoutInflater mLayoutInflater;

    private class ChangeEntry {
        public String title;
        public String text;

        public ChangeEntry(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    public interface ItemClickListener {
        void onItemClick(View view, String title, String text);
    }

    public ChangeListAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    public void clear() {
        mChangeData.clear();
    }

    public void add(String title, String text) {
        mChangeData.add(new ChangeEntry(title, text));
    }

    public void setOnClickListener(ItemClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TextView changeTitle;
        public TextView changeText;
        public View layout;

        public ViewHolder(View view) {
            super(view);
            changeTitle = view.findViewById(R.id.change_title);
            changeText = view.findViewById(R.id.change_text);
            view.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            String title = mChangeData.get(position).title;
            String text = mChangeData.get(position).text;
            if (mOnClickListener != null) {
                mOnClickListener.onItemClick(view, title, text);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mLayoutInflater.inflate(R.layout.item_recent_change, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        viewHolder.changeTitle.setText(mChangeData.get(position).title);
        viewHolder.changeText.setText(mChangeData.get(position).text);
    }

    @Override
    public int getItemCount() {
        return mChangeData.size();
    }

}
