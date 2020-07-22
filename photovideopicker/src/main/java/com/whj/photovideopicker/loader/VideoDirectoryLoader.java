package com.whj.photovideopicker.loader;

import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.loader.content.CursorLoader;

/**
 * Created by user.
 */
public class VideoDirectoryLoader extends CursorLoader {

    final String[] VIDEO_PROJECTION = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
    };

    public VideoDirectoryLoader(Context context) {
        super(context);

        setProjection(VIDEO_PROJECTION);
        setUri(MediaStore.Files.getContentUri("external"));
        setSortOrder(MediaStore.Images.Media.DATE_TAKEN + " DESC");

        setSelection(
                MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                        + " AND " + MediaStore.MediaColumns.SIZE + ">0");
        String[] selectionArgs;
        selectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};

        setSelectionArgs(selectionArgs);
    }


    private VideoDirectoryLoader(Context context, Uri uri, String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
    }


}
