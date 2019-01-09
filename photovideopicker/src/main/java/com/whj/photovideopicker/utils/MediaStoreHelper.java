package com.whj.photovideopicker.utils;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.whj.photovideopicker.R;
import com.whj.photovideopicker.fragment.PhotoPickerFragment;
import com.whj.photovideopicker.loader.PhotoDirectoryLoader;
import com.whj.photovideopicker.loader.VideoDirectoryLoader;
import com.whj.photovideopicker.model.Photo;
import com.whj.photovideopicker.model.PhotoDirectory;
import com.whj.photovideopicker.model.VideoDirectory;

import java.util.ArrayList;
import java.util.List;

import static android.provider.BaseColumns._ID;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME;
import static android.provider.MediaStore.Images.ImageColumns.BUCKET_ID;
import static android.provider.MediaStore.MediaColumns.DATA;
import static android.provider.MediaStore.MediaColumns.DATE_ADDED;

/**
 * Created by user.
 */
public class MediaStoreHelper {

    public final static int INDEX_ALL_PHOTOS = 0;

    public static void getPhotoDirs(Fragment fragment, Bundle args, PhotosResultCallback resultCallback) {

        fragment.getLoaderManager()
                .initLoader(0, args, new PhotoDirLoaderCallbacks(fragment.getActivity(), resultCallback,MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
    }

    public static void getVideoDirs(Fragment fragment, Bundle args, VideosResultCallback resultCallback ) {

        fragment.getLoaderManager()
                .initLoader(1, args, new PhotoDirLoaderCallbacks(fragment.getActivity(), resultCallback,MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
    }
    static class PhotoDirLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        private Context context;
        private PhotosResultCallback resultCallback;
        private VideosResultCallback videosCallback;
        private int mType;

        public PhotoDirLoaderCallbacks(Context context, PhotosResultCallback resultCallback, int type) {
            this.context = context;
            this.resultCallback = resultCallback;
            this.mType = type;
        }
        public PhotoDirLoaderCallbacks(Context context, VideosResultCallback resultCallback, int type) {
            this.context = context;
            this.videosCallback = resultCallback;
            this.mType = type;
        }
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            if (mType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                return new PhotoDirectoryLoader(context, args.getBoolean(PhotoPickerFragment.EXTRA_SHOW_GIF, false));
            } else {
                return new VideoDirectoryLoader(context);
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            if (data == null) return;
            if (mType == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                List<PhotoDirectory> directories = new ArrayList<>();
                PhotoDirectory photoDirectoryAll = new PhotoDirectory();
                photoDirectoryAll.setName(context.getString(R.string.recent_photos));
                photoDirectoryAll.setId("ALL");
                while (data.moveToNext()) {
                    initPhotoDirectory(data, directories, photoDirectoryAll);
                }

                if (photoDirectoryAll.getPhotoPaths().size() > 0) {
                    photoDirectoryAll.setCoverPath(photoDirectoryAll.getPhotoPaths().get(0));
                }
                directories.add(INDEX_ALL_PHOTOS, photoDirectoryAll);
                if (resultCallback != null) {
                    resultCallback.onResultCallback(directories);
                    resultCallback.onResultPhotosClallback(photoDirectoryAll.getPhotos());
                }
            } else {
                List<VideoDirectory> directories = new ArrayList<>();
                VideoDirectory videoDirectoryAll = new VideoDirectory();
                videoDirectoryAll.setName(context.getString(R.string.recent_video));
                videoDirectoryAll.setId("ALL");
                while (data.moveToNext()) {
                    initVideoDirectory(data, directories, videoDirectoryAll);
                }
                if (videoDirectoryAll.getVideoPaths().size() > 0) {
                    videoDirectoryAll.setCoverPath(videoDirectoryAll.getVideoPaths().get(0));
                }
                directories.add(INDEX_ALL_PHOTOS, videoDirectoryAll);
                if (videosCallback != null) {
                    videosCallback.onResultCallback(directories);
                }
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }
    }

    private static void initVideoDirectory(Cursor data, List<VideoDirectory> directories, VideoDirectory videoDirectoryAll) {
        try{
            int vedioId = data.getInt(data.getColumnIndexOrThrow(_ID));
            String vedioBucketId = data.getString(data.getColumnIndexOrThrow(BUCKET_ID));
            String directoryName = data.getString(data.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
            String videoName = data.getString(data.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
            String path = data.getString(data.getColumnIndexOrThrow(DATA));
            Long duration = data.getLong(data.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
            long size = data.getLong(data.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

            VideoDirectory videoDirectory = new VideoDirectory();
            videoDirectory.setId(vedioBucketId);
            videoDirectory.setName(directoryName);

            if (!directories.contains(videoDirectory)) {
                videoDirectory.setCoverPath(path);
                videoDirectory.addVideo(vedioId, path,duration,size,directoryName);
                videoDirectory.setDateAdded(data.getLong(data.getColumnIndexOrThrow(DATE_ADDED)));
                directories.add(videoDirectory);
            } else {
                directories.get(directories.indexOf(videoDirectory)).addVideo(vedioId, path,duration,size,videoName);
            }

            videoDirectoryAll.addVideo(vedioId, path,duration,size,videoName);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private static void initPhotoDirectory(Cursor data, List<PhotoDirectory> directories, PhotoDirectory photoDirectoryAll) {
        try{
            int imageId = data.getInt(data.getColumnIndexOrThrow(_ID));
            String bucketId = data.getString(data.getColumnIndexOrThrow(BUCKET_ID));
            String name = data.getString(data.getColumnIndexOrThrow(BUCKET_DISPLAY_NAME));
            String path = data.getString(data.getColumnIndexOrThrow(DATA));

            PhotoDirectory photoDirectory = new PhotoDirectory();
            photoDirectory.setId(bucketId);
            photoDirectory.setName(name);

            if (!directories.contains(photoDirectory)) {
                photoDirectory.setCoverPath(path);
                photoDirectory.addPhoto(imageId, path);
                photoDirectory.setDateAdded(data.getLong(data.getColumnIndexOrThrow(DATE_ADDED)));
                directories.add(photoDirectory);
            } else {
                directories.get(directories.indexOf(photoDirectory)).addPhoto(imageId, path);
            }

            photoDirectoryAll.addPhoto(imageId, path);
        }catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }


    public interface PhotosResultCallback {
        void onResultCallback(List<PhotoDirectory> directories);
        void onResultPhotosClallback(List<Photo> photoList);
    }
    public interface VideosResultCallback {
        void onResultCallback(List<VideoDirectory> directories);
    }
}
