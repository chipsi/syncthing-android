package com.nutomic.syncthingandroid.views;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.databinding.ItemFolderListBinding;
import com.nutomic.syncthingandroid.model.Folder;
import com.nutomic.syncthingandroid.model.FolderStatus;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.RestApi;
import com.nutomic.syncthingandroid.service.SyncthingService;
import com.nutomic.syncthingandroid.util.Util;

import java.io.File;
import java.util.HashMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

/**
 * Generates item views for folder items.
 */
public class FoldersAdapter extends ArrayAdapter<Folder> {

    private static final String TAG = "FoldersAdapter";

    private final HashMap<String, FolderStatus> mLocalFolderStatuses = new HashMap<>();

    private final Context mContext;

    private Folder mFolder;

    public FoldersAdapter(Context context) {
        super(context, 0);
        mContext = context;
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ItemFolderListBinding binding = (convertView == null)
                ? DataBindingUtil.inflate(LayoutInflater.from(mContext), R.layout.item_folder_list, parent, false)
                : DataBindingUtil.bind(convertView);

        mFolder = getItem(position);
        binding.label.setText(TextUtils.isEmpty(mFolder.label) ? mFolder.id : mFolder.label);
        binding.directory.setText(mFolder.path);
        binding.override.setOnClickListener(v -> {
            // Send "Override changes" through our service to the REST API.
            Intent intent = new Intent(mContext, SyncthingService.class)
                    .putExtra(SyncthingService.EXTRA_FOLDER_ID, mFolder.id);
            intent.setAction(SyncthingService.ACTION_OVERRIDE_CHANGES);
            mContext.startService(intent);
        });
        binding.openFolder.setOnClickListener(this::onOpenFolderClick);
        updateFolderStatusView(binding);
        return binding.getRoot();
    }

    private void updateFolderStatusView(ItemFolderListBinding binding) {
        FolderStatus folderStatus = mLocalFolderStatuses.get(mFolder.id);
        if (folderStatus == null) {
            binding.items.setVisibility(GONE);
            binding.override.setVisibility(GONE);
            binding.size.setVisibility(GONE);
            setTextOrHide(binding.invalid, mFolder.invalid);
            return;
        }

        long neededItems = folderStatus.needFiles + folderStatus.needDirectories + folderStatus.needSymlinks + folderStatus.needDeletes;
        boolean outOfSync = folderStatus.state.equals("idle") && neededItems > 0;
        boolean overrideButtonVisible = mFolder.type.equals(Constants.FOLDER_TYPE_SEND_ONLY) && outOfSync;
        binding.override.setVisibility(overrideButtonVisible ? VISIBLE : GONE);
        if (outOfSync) {
            binding.state.setText(mContext.getString(R.string.status_outofsync));
            binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
        } else {
            if (mFolder.paused) {
                binding.state.setText(mContext.getString(R.string.state_paused));
                binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_black));
            } else {
                binding.state.setText(getLocalizedState(mContext, folderStatus));
                switch(folderStatus.state) {
                    case "idle":
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_green));
                        break;
                    case "scanning":
                    case "syncing":
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_blue));
                        break;
                    case "error":
                    default:
                        binding.state.setTextColor(ContextCompat.getColor(mContext, R.color.text_red));
                }
            }
        }
        binding.items.setVisibility(VISIBLE);
        binding.items.setText(mContext.getResources()
                .getQuantityString(R.plurals.files, (int) folderStatus.inSyncFiles, folderStatus.inSyncFiles, folderStatus.globalFiles));
        binding.size.setVisibility(VISIBLE);
        binding.size.setText(mContext.getString(R.string.folder_size_format,
                Util.readableFileSize(mContext, folderStatus.inSyncBytes),
                Util.readableFileSize(mContext, folderStatus.globalBytes)));
        setTextOrHide(binding.invalid, folderStatus.invalid);
    }

    /**
     * Returns the folder's state as a localized string.
     */
    private static String getLocalizedState(Context c, FolderStatus folderStatus) {
        switch (folderStatus.state) {
            case "idle":
                return c.getString(R.string.state_idle);
            case "scanning":
                return c.getString(R.string.state_scanning);
            case "syncing":
                int percentage = (folderStatus.globalBytes != 0)
                        ? Math.round(100 * folderStatus.inSyncBytes / folderStatus.globalBytes)
                        : 100;
                return c.getString(R.string.state_syncing, percentage);
            case "error":
                if (TextUtils.isEmpty(folderStatus.error)) {
                    return c.getString(R.string.state_error);
                }
                return c.getString(R.string.state_error) + " (" + folderStatus.error + ")";
            case "unknown":
                return c.getString(R.string.state_unknown);
            default:
                return folderStatus.state;
        }
    }

    /**
     * Requests updated folder status from the api for all visible items.
     */
    public void updateFolderStatus(RestApi api) {
        for (int i = 0; i < getCount(); i++) {
            api.getFolderStatus(getItem(i).id, this::onReceiveFolderStatus);
        }
    }

    private void onReceiveFolderStatus(String folderId, FolderStatus folderStatus) {
        mLocalFolderStatuses.put(folderId, folderStatus);
        notifyDataSetChanged();
    }

    private void setTextOrHide(TextView view, String text) {
        if (TextUtils.isEmpty(text)) {
            view.setVisibility(GONE);
        } else {
            view.setText(text);
            view.setVisibility(VISIBLE);
        }
    }

    private void onOpenFolderClick(View view) {
        // Try to find a compatible file manager app supporting the "resource/folder" Uri type.
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(new File(mFolder.path)), "resource/folder");
        intent.putExtra("org.openintents.extra.ABSOLUTE_PATH", mFolder.path);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            // Launch file manager.
            mContext.startActivity(intent);
            return;
        }

        /**
         * Fallback: Let the user choose from all Uri handling apps.
         * This allows the use of third-party file manager apps like
         * Root Explorer as they provide non-standardized Uri handlers.
         */
        /*
        Log.v(TAG, "openFolder: Fallback to application chooser to open folder.");
        intent.setDataAndType(Uri.parse(mFolder.path), "application/*");
        Intent chooserIntent = Intent.createChooser(intent, mContext.getString(R.string.open_file_manager));
        if (chooserIntent != null) {
            // Launch potential file manager app.
            mContext.startActivity(chooserIntent);
            return;
        }
        */

        // No compatible file manager app found.
        // ToDo
        // Toast.makeText(mContext, R.string.toast_no_file_manager, Toast.LENGTH_SHORT).show();
        final String appPackageName = "com.simplemobiletools.filemanager";
        try {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
        } catch (android.content.ActivityNotFoundException anfe) {
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }
}
