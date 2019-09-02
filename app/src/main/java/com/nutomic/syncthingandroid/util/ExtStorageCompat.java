package com.nutomic.syncthingandroid.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;

public class ExtStorageCompat {

    private static final String TAG = "ExtStorageCompat";

    private ExtStorageCompat() {
    }

    /**
     * Source: https://stackoverflow.com/questions/56468539/getexternalstoragepublicdirectory-deprecated-in-android-q
     */
    public static File getExternalStoragePublicDirectory(Context context, String type) {
        // ToDo: Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, type);

        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri == null) {
            return null;
        }
        return new File(uri.toString());
    }
}
