package com.whj.photovideopicker.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;


import com.whj.photovideopicker.PickerMainActivity;
import com.whj.photovideopicker.R;
import com.whj.photovideopicker.VideoRecorderActivity;
import com.whj.photovideopicker.adapter.PopupVideoDirectoryListAdapter;
import com.whj.photovideopicker.adapter.VideoGirdAdapter;
import com.whj.photovideopicker.base.PickerBaseFragment;
import com.whj.photovideopicker.listener.OnItemCheckListener;
import com.whj.photovideopicker.model.Photo;
import com.whj.photovideopicker.model.Video;
import com.whj.photovideopicker.model.VideoDirectory;
import com.whj.photovideopicker.utils.MediaStoreHelper;
import com.whj.photovideopicker.utils.PhotoGridAutofitDecoration;
import com.whj.photovideopicker.utils.PickerUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.app.Activity.RESULT_OK;
import static android.os.Build.VERSION_CODES.M;
import static android.os.Build.VERSION_CODES.N;
import static com.whj.photovideopicker.PhotoPicker.KEY_SELECTED_PHOTOS;
import static com.whj.photovideopicker.PhotoPicker.RESULT_TYPE;
import static com.whj.photovideopicker.PhotoPicker.SUPPORT_SHARE;
import static com.whj.photovideopicker.PhotoPicker.VIDEO;
import static com.whj.photovideopicker.PhotoPicker.VIDEO_EXTRA_MAX_COUNT;
import static com.whj.photovideopicker.PhotoPicker.VIDEO_SAVE_DIRECTORY;
import static com.whj.photovideopicker.PhotoPicker.VIDEO_SHARE;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 上午11:10
 */

public class VideoPickerFragment extends PickerBaseFragment implements View.OnClickListener {


    private static final String CMR_W09 = "CMR-W09";
    private static final String SHT_W09 = "SHT-W09";

    private static final int MY_PERMISSIONS_REQUEST_READ_STORE = 100;
    public static final int REQUEST_TAKE_LENOVO = 124;

    public int maxCount = 1;
    private boolean isSupportShare;

    private VideoGirdAdapter mVideoAdapter;

    private PopupVideoDirectoryListAdapter listAdapter;

    private List<VideoDirectory> directories;
    private View mPopView;
    private PopupWindow mPopupWindow;
    private ListView lvAlbum;

    RecyclerView recyclerView;

    TextView mAlbum, tv_share;

    TextView mPreview;

    private String saveDirectory;


    @Override
    public int viewById() {
        return R.layout.fragment_photo_picker_layout;
    }


    @Override
    public void bindView(View rootView) {
        recyclerView = $(rootView, R.id.rv_photos);
        mAlbum = $(rootView, R.id.tv_album_ar);
        mPreview = $(rootView, R.id.tv_preview);
        tv_share = $(rootView, R.id.tv_share);

        mAlbum.setOnClickListener(this);
        mPreview.setOnClickListener(this);
        tv_share.setOnClickListener(this);
    }

    @Override
    public void afterView() {
        initViews();
        isNeedEncoder();
    }


    private boolean isNeedEncoder = false;

    private boolean isNeedEncoder() {
        String padModel = Build.MODEL;
        Log.d("padModel", "padModel = " + padModel);
        if (CMR_W09.equalsIgnoreCase(padModel) || SHT_W09.equalsIgnoreCase(padModel)) {
            isNeedEncoder = true;
            return true;
        }
        isNeedEncoder = false;
        return false;
    }


    @Override
    protected void onUserVisible() {

    }

    private List<String> mTempVideos = new ArrayList<>();

    public static VideoPickerFragment newInstance(int max_count, String dir, boolean isSupportShare) {
        VideoPickerFragment videoPickerFragment = new VideoPickerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(VIDEO_EXTRA_MAX_COUNT, max_count);
        bundle.putString(VIDEO_SAVE_DIRECTORY, dir);
        bundle.putBoolean(SUPPORT_SHARE, isSupportShare);
        videoPickerFragment.setArguments(bundle);
        return videoPickerFragment;
    }

    public int getVideoSelectNumber() {
        int select = 0;
        if (null != mVideoAdapter.getselectedVideos()) {
            select = mVideoAdapter.getselectedVideos().size();
        }
        return select;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        maxCount = getArguments().getInt(VIDEO_EXTRA_MAX_COUNT);
        saveDirectory = getArguments().getString(VIDEO_SAVE_DIRECTORY);
        isSupportShare = getArguments().getBoolean(SUPPORT_SHARE, false);
        setRetainInstance(true);
        directories = new ArrayList<>();
        mVideoAdapter = new VideoGirdAdapter(getActivity(), directories);
        listAdapter = new PopupVideoDirectoryListAdapter(getActivity(), directories);
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORE);
        } else {
            initConfig();
        }

    }

    private void initShareText(int number) {
        tv_share.setText(getString(R.string.share_to_class, number, maxCount));
    }

    private void initConfig() {

        Bundle mediaStoreArgs = new Bundle();
        MediaStoreHelper.getVideoDirs(this, mediaStoreArgs,
                new MediaStoreHelper.VideosResultCallback() {
                    @Override
                    public void onResultCallback(List<VideoDirectory> dirs) {
                        mTempVideos.clear();
                        directories.clear();
                        directories.addAll(dirs);
                        mVideoAdapter.notifyDataSetChanged();
                        listAdapter.notifyDataSetChanged();

                        if (dirs != null && dirs.size() != 0) {
                            for (int i = 0; i < dirs.size(); i++) {
                                if (dirs.get(i).getVideos() != null && dirs.get(i).getVideos().size() != 0) {
                                    for (int j = 0; j < dirs.get(i).getVideos().size(); j++) {
                                        mTempVideos.add(dirs.get(i).getVideos().get(j).getPath());
                                    }
                                }
                            }
                        }
                    }
                });
    }


    private Uri fileUri;
    private String videoUrl;

    private void initViews() {
        if (isSupportShare) {
            tv_share.setVisibility(View.VISIBLE);
        } else {
            tv_share.setVisibility(View.GONE);
        }
        mPreview.setVisibility(View.VISIBLE);
        mAlbum.setText(getResources().getText(R.string.recent_video));

        initShareText(0);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), PickerUtils.getSpanNumber(getActivity()));
        recyclerView.addItemDecoration(new PhotoGridAutofitDecoration(6, 5));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mVideoAdapter);

        recyclerView.setItemAnimator(new DefaultItemAnimator());

        mVideoAdapter.setOnCameraClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (isNeedEncoder) {
                        Intent intent = new Intent(getActivity(), VideoRecorderActivity.class);
                        intent.putExtra("dir_path", saveDirectory);
                        startActivityForResult(intent, REQUEST_TAKE_LENOVO);
                    } else {
                        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        try {
                            if (Build.VERSION.SDK_INT > M) {
                                fileUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".fileprovider", createMediaFile());
                            } else {
                                fileUri = Uri.fromFile(createMediaFile()); // create a file to save the video
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name
                        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
                        //设置时长，录制到达时间，系统会自动保存视频，停止录制
                        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60 * 5);
                        // 限制大小 限制视频的大小，这里是100兆。当大小到达的时候，系统会自动停止录制
                        intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 1024 * 1024 * 100);
                        startActivityForResult(intent, REQUEST_TAKE_LENOVO);
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mVideoAdapter.setOnItemCheckListener(new OnItemCheckListener() {
            @Override
            public boolean OnItemCheck(int position, Photo path, boolean isCheck, int selectedItemCount) {
                return false;
            }

            @Override
            public boolean OnItemCheck(int position, Video video, final boolean isCheck, int selectedItemCount) {

                int total = selectedItemCount + (isCheck ? -1 : 1);

                ((PickerMainActivity) getActivity()).getRightText().setEnabled(total > 0);

                if (total > maxCount) {
                    toast(getString(R.string.over_max_count_video_tips, maxCount));
                    return false;
                }
                if (PickerUtils.getSizeInMB(video.getSize()) > 200) {
                    toast(getString(R.string.over_max_size_video_tips));
                    return false;
                }
                if (maxCount <= 1) {
                    List<Video> videos = mVideoAdapter.getselectedVideos();
                    if (!videos.contains(video)) {
                        videos.clear();
                        mVideoAdapter.notifyDataSetChanged();
                    }
                    ((PickerMainActivity) getActivity()).initRightText(total > 1 ? 1 : total, maxCount);
                    initShareText(total > 1 ? 1 : total);

                    return true;
                }

                ((PickerMainActivity) getActivity()).initRightText(total, maxCount);
                initShareText(total);
                return true;
            }
        });
        initListPopupWindow();
    }

    private File createMediaFile() throws Exception {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String imageFileName = "video_" + timeStamp;
        String suffix = ".mp4";
        videoUrl = saveDirectory + File.separator + imageFileName + suffix;
        File mediaFile = new File(videoUrl);
        return mediaFile;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_LENOVO && resultCode == RESULT_OK) {
            if (isNeedEncoder) {
                try{
                    final String videoPath = data.getStringExtra("video_return_path_key");
                    refreshGrally(videoPath);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendBroadFalse(videoPath, 0l);
                        }
                    }, 1000);
                }catch (Exception e){
                    toast(e.getMessage());
                }
            } else {
                try{
                    Uri mUri = data.getData();
                    final String videoPath = PickerUtils.getRealFilePath(getActivity(),mUri);
                    final long duration = PickerUtils.getVideoDuration(getActivity(),mUri);
                    refreshGrally(videoPath);
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendBroadFalse(videoPath,duration);
                        }
                    }, 1000);
                }catch (Exception e){
                    toast(e.getMessage());
                }


            }

        }
    }

    private void refreshGrally(String filePath) {
        Uri localUri = Uri.fromFile(new File(filePath));
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
        getActivity().sendBroadcast(localIntent);

    }

    private Handler handler = new Handler();

    /**
     * 解决某些机型广播媒体库无法及时更新问题
     */
    private void sendBroadFalse(String videoUrl, long duration) {

        if (!mTempVideos.isEmpty()) {
            if (!mTempVideos.contains(videoUrl)) {
                Video video = new Video(makeVideoTempId(8), videoUrl, duration, 10l, PickerUtils.getFileNameByPath(videoUrl));
                directories.get(0).getVideos().add(0, video);
                mVideoAdapter.notifyDataSetChanged();
                listAdapter.notifyDataSetChanged();
            }
        } else {
            Video video = new Video(makeVideoTempId(8), videoUrl, duration, 10l, PickerUtils.getFileNameByPath(videoUrl));
            directories.get(0).getVideos().add(0, video);
            mVideoAdapter.notifyDataSetChanged();
            listAdapter.notifyDataSetChanged();
        }
    }


    private int makeVideoTempId(int max) {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < max; i++) {
            str.append(random.nextInt(10));
        }
        return Integer.parseInt(str.toString());
    }

    private void initListPopupWindow() {
        mPopView = LayoutInflater.from(getActivity()).inflate(R.layout.album_listview, null);
        mPopupWindow = new PopupWindow(mPopView, ViewGroup.LayoutParams.MATCH_PARENT,
                PickerUtils.getScreenSize(getActivity())[1] - getResources().getDimensionPixelOffset(R.dimen.topbar_height)
                        - getResources().getDimensionPixelOffset(R.dimen.topbar_height));
        lvAlbum = (ListView) mPopView.findViewById(R.id.lv_ablum_ar);
        lvAlbum.setAdapter(listAdapter);

        lvAlbum.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                hideAlbum();

                VideoDirectory directory = directories.get(position);
                mAlbum.setText(directory.getName());
                mVideoAdapter.setCurrentDirectoryIndex(position);
                mVideoAdapter.notifyDataSetChanged();
                listAdapter.setCheckPosition(position);
            }
        });

    }

    private void showAlbum() {
        mPopupWindow.setFocusable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(false);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
//        mPopupWindow.showAtLocation(mBottom, Gravity.NO_GRAVITY,0,getResources().getDimensionPixelOffset(R.dimen.bottom_bar_height));
        mPopupWindow.showAsDropDown(mAlbum, 0, 0);
    }

    private void hideAlbum() {
        mPopupWindow.dismiss();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_REQUEST_READ_STORE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initConfig();
            } else {
                // Permission Denied
                toast("权限被禁止");
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        mTempVideos.clear();
        super.onDestroy();
    }


    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.tv_album_ar) {
            if (mPopupWindow.isShowing()) {
                hideAlbum();
            } else {
                showAlbum();
            }

        } else if (viewId == R.id.tv_preview) {
            if (null == mVideoAdapter.getselectedVideos() || mVideoAdapter.getselectedVideos().size() == 0) {
                toast("请选择视频");
            } else {
                ArrayList<String> videos = new ArrayList<>();
                for (int i = 0; i < mVideoAdapter.getselectedVideos().size(); i++) {
                    videos.add(mVideoAdapter.getselectedVideos().get(i).getPath());
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    File file = new File(videos.get(0));
                    intent.setDataAndType(getUriForFile(file), "video/*");
                    if (!(getActivity() instanceof Activity)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    toast("该视频不支持预览");
                }
            }

        } else if (viewId == R.id.tv_share) {

            if (null == mVideoAdapter.getselectedVideos() || mVideoAdapter.getselectedVideos().size() == 0) {
                toast("请选择视频");
            } else {
                ArrayList<String> videos = new ArrayList<>();
                for (int i = 0; i < mVideoAdapter.getselectedVideos().size(); i++) {
                    videos.add(mVideoAdapter.getselectedVideos().get(i).getPath());
                }
                Intent intent = new Intent();
                intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, videos);
                intent.putExtra(RESULT_TYPE, VIDEO_SHARE);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }

        }
    }

    private Uri getUriForFile(File file) {
        Uri uri;
        if (Build.VERSION.SDK_INT >= N) {
            uri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }


    public void finishChooseVideo() {
        if (null == mVideoAdapter.getselectedVideos() || mVideoAdapter.getselectedVideos().size() == 0) {
            toast("请选择视频");
        } else {
            ArrayList<String> videos = new ArrayList<>();
            for (int i = 0; i < mVideoAdapter.getselectedVideos().size(); i++) {
                videos.add(mVideoAdapter.getselectedVideos().get(i).getPath());
            }
            Intent intent = new Intent();
            intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, videos);
            intent.putExtra(RESULT_TYPE, VIDEO);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        }
    }

}
