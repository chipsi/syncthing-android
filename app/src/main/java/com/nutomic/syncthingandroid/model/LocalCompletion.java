package com.nutomic.syncthingandroid.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.text.TextUtils;

/**
 * This class caches local folder synchronization
 * completion indicators defined in {@link CompletionInfo}
 * according to Syncthing's "FolderSummary" event JSON result schema.
 * Completion model of Syncthing's web UI is completion[folderId]
 */
public class LocalCompletion {

    private static final String TAG = "LocalCompletion";

    private Boolean ENABLE_VERBOSE_LOG = false;

    HashMap<String, CompletionInfo> folderMap =
        new HashMap<String, CompletionInfo>();

    public LocalCompletion(Boolean enableVerboseLog) {
        ENABLE_VERBOSE_LOG = enableVerboseLog;
    }

    /**
     * Updates folder information in the cache model
     * after a config update.
     */
    public void updateFromConfig(final List<Folder> newFolders) {
        // Handle folders that were removed from the config.
        List<String> removedFolders = new ArrayList<>();
        Boolean folderFound;
        for (String folderId : folderMap.keySet()) {
            folderFound = false;
            for (Folder folder : newFolders) {
                if (folder.id.equals(folderId)) {
                    folderFound = true;
                    break;
                }
            }
            if (!folderFound) {
                removedFolders.add(folderId);
            }
        }
        for (String folderId : removedFolders) {
            if (ENABLE_VERBOSE_LOG) {
                Log.v(TAG, "updateFromConfig: Remove folder '" + folderId + "' from cache model");
            }
            if (folderMap.containsKey(folderId)) {
                folderMap.remove(folderId);
            }
        }

        // Handle folders that were added to the config.
        for (Folder folder : newFolders) {
            if (!folderMap.containsKey(folder.id)) {
                if (ENABLE_VERBOSE_LOG) {
                    Log.v(TAG, "updateFromConfig: Add folder '" + folder.id + "' to cache model.");
                }
                folderMap.put(folder.id, new CompletionInfo());
            }
        }
    }

    /**
     * Calculates local folder sync completion percentage across all folders.
     */
    public int getTotalFolderCompletion() {
        int folderCount = 0;
        double sumCompletion = 0;
        for (Map.Entry<String, CompletionInfo> folder : folderMap.entrySet()) {
            sumCompletion += folder.getValue().completion;
            folderCount++;
        }
        if (folderCount == 0) {
            return 100;
        }
        return (int) Math.floor(sumCompletion / folderCount);
    }

    /**
     * Returns local folder sync completion percentage.
     */
    public int getFolderCompletion(String folderId) {
        int folderCount = 0;
        double sumCompletion = 0;
        if (!folderMap.containsKey(folderId)) {
            return 100;
        }
        return (int) Math.floor(folderMap.get(folderId).completion);
    }

    /**
     * Set completionInfo within the completion[folderId] model.
     */
    public void setCompletionInfo(String folderId,
                                    CompletionInfo completionInfo) {
        if (ENABLE_VERBOSE_LOG) {
            Log.v(TAG, "setCompletionInfo: Storing " + completionInfo.completion + "% for folder \"" +
                    folderId + "\".");
        }
        // Add folder or update existing folder entry.
        folderMap.put(folderId, completionInfo);
    }
}
