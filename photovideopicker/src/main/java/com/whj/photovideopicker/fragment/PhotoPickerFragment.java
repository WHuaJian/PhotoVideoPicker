package com.whj.photovideopicker.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.collection.ArrayMap;
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

import com.otaliastudios.cameraview.CameraActivity;
import com.whj.photovideopicker.PhotoPreviewActivity;
import com.whj.photovideopicker.PickerMainActivity;
import com.whj.photovideopicker.R;
import com.whj.photovideopicker.adapter.PhotoGridAdapter;
import com.whj.photovideopicker.adapter.PopupDirectoryListAdapter;
import com.whj.photovideopicker.base.PickerBaseFragment;
import com.whj.photovideopicker.listener.OnItemCheckListener;
import com.whj.photovideopicker.model.Photo;
import com.whj.photovideopicker.model.PhotoDirectory;
import com.whj.photovideopicker.model.Video;
import com.whj.photovideopicker.utils.ImageCaptureManager;
import com.whj.photovideopicker.utils.LubanUtil;
import com.whj.photovideopicker.utils.MediaStoreHelper;
import com.whj.photovideopicker.utils.PhotoGridAutofitDecoration;
import com.whj.photovideopicker.utils.PickerUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.kareluo.imaging.IMGEditActivity;

import static android.app.Activity.RESULT_OK;
import static com.whj.photovideopicker.PhotoPicker.IS_COMPRESS;
import static com.whj.photovideopicker.PhotoPicker.IS_NEED_PIC_EDIT;
import static com.whj.photovideopicker.PhotoPicker.IS_TOUPING;
import static com.whj.photovideopicker.PhotoPicker.KEY_SELECTED_PHOTOS;
import static com.whj.photovideopicker.PhotoPicker.PHOTO;
import static com.whj.photovideopicker.PhotoPicker.PHOTO_EXTRA_MAX_COUNT;
import static com.whj.photovideopicker.PhotoPicker.PHOTO_SHARE;
import static com.whj.photovideopicker.PhotoPicker.RESULT_TYPE;
import static com.whj.photovideopicker.PhotoPicker.SUPPORT_SHARE;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 上午10:51
 */

public class PhotoPickerFragment extends PickerBaseFragment implements View.OnClickListener {

    public final static String EXTRA_SHOW_GIF = "SHOW_GIF";

    private static final int MY_PERMISSIONS_REQUEST_READ_STORE = 100;

    public int maxCount = 0;
    private boolean isSupportShare;

    private ImageCaptureManager captureManager;
    private PhotoGridAdapter photoGridAdapter;

    private PopupDirectoryListAdapter listAdapter;
    private List<PhotoDirectory> directories;

    private View mPopView;
    private PopupWindow mPopupWindow;
    private ListView lvAlbum;


    RecyclerView recyclerView;
    TextView mAlbum, tv_share, tv_finish;
    TextView mPreview;

    private boolean isNeedPicEdit, isTouping;
    private boolean isCompress;
    private List<String> mTempImages = new ArrayList<>();
    private Handler handler = new Handler();


    public static PhotoPickerFragment newInstance(int max_count, boolean isNeedPicEdit, boolean isCompress, boolean isSupportShare, boolean isTouping) {
        PhotoPickerFragment pickerFragment = new PhotoPickerFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(PHOTO_EXTRA_MAX_COUNT, max_count);
        bundle.putBoolean(SUPPORT_SHARE, isSupportShare);
        bundle.putBoolean(IS_NEED_PIC_EDIT, isNeedPicEdit);
        bundle.putBoolean(IS_COMPRESS, isCompress);
        bundle.putBoolean(IS_TOUPING, isTouping);
        pickerFragment.setArguments(bundle);
        return pickerFragment;
    }


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
        tv_finish = $(rootView, R.id.tv_finish);

        mAlbum.setOnClickListener(this);
        mPreview.setOnClickListener(this);
        tv_share.setOnClickListener(this);
        tv_finish.setOnClickListener(this);
    }

    private LocalBroadcastManager broadcastManager;
    private PhotoEditReceiver receiver;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        maxCount = getArguments().getInt(PHOTO_EXTRA_MAX_COUNT);
        isSupportShare = getArguments().getBoolean(SUPPORT_SHARE, false);
        isNeedPicEdit = getArguments().getBoolean(IS_NEED_PIC_EDIT, false);
        isCompress = getArguments().getBoolean(IS_COMPRESS, false);
        isTouping = getArguments().getBoolean(IS_TOUPING, false);
        directories = new ArrayList<>();
        photoGridAdapter = new PhotoGridAdapter(getActivity(), directories);
        listAdapter = new PopupDirectoryListAdapter(getActivity(), directories);
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_STORE);
        } else {

            initConfig();

        }
        captureManager = new ImageCaptureManager(getActivity());
        receiver = new PhotoEditReceiver();
        broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.registerReceiver(receiver, new IntentFilter("photo_edit"));

    }

    private int getTextString() {
        if (isTouping) {
            return R.string.done_with_count;
        }

        return R.string.done_with_count_finish;
    }

    private class PhotoEditReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("photo_edit")) {
                if (captureManager == null) {
                    FragmentActivity activity = getActivity();
                    captureManager = new ImageCaptureManager(activity);
                }
                final String path = intent.getStringExtra("edit_pic_path");
                String old_pic_path = intent.getStringExtra("old_pic_path");
                String originPath = intent.getStringExtra("originPath");
                isTakePhoto = intent.getBooleanExtra("isTakePhoto", false);

                Photo oldPhoto = getSelectedPhoto(old_pic_path);
                if (oldPhoto != null) {
                    photoGridAdapter.getSelectedPhotos().remove(oldPhoto);
                }

                if (!isTakePhoto) {
                    tempLists.put(originPath, path);
                }
                captureManager.galleryAddPic(path);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendBroadFalse(path);
                    }
                }, 1000);
            }

        }
    }

    @Override
    public void afterView() {
        initViews();
    }

    @Override
    protected void onUserVisible() {
    }

    public void clearTempList(){
        tempLists.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        tempLists.clear();
    }

    public int getPhotoSelectNumer() {
        int select = 0;
        ArrayList<String> arrayList = getSelectedPhotoPaths();
        if (arrayList != null && !arrayList.isEmpty()) {
            select = arrayList.size();
        }

        return select;
    }


    private ArrayMap<String, String> tempLists = new ArrayMap<>();
    private boolean isTakePhoto;

    /**
     * 把裁剪的图片变成选中状态，并清除之前的选中
     */
    private void addEditPhoto() {
        try {
            for (Map.Entry<String, String> path : tempLists.entrySet()) {
                Photo photo = getAllPhoto(path.getValue());
                if (photo != null) {
                    if(!photoGridAdapter.getSelectedPhotos().contains(photo)){
                        photoGridAdapter.getSelectedPhotos().add(photo);
                    }
                }
            }
            photoGridAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Photo getAllPhoto(String path) {
        for (Photo photo : photoGridAdapter.getCurrentPhotos()) {
            if (photo.getPath().equals(path)) {
                return photo;
            }
        }
        return null;
    }

    private Photo getSelectedPhoto(String path) {
        for (Photo photo : photoGridAdapter.getSelectedPhotos()) {
            if (photo.getPath().equals(path)) {
                return photo;
            }
        }
        return null;
    }

    private void initConfig() {
        Bundle mediaStoreArgs = new Bundle();
        MediaStoreHelper.getPhotoDirs(this, mediaStoreArgs,
                new MediaStoreHelper.PhotosResultCallback() {
                    @Override
                    public void onResultCallback(List<PhotoDirectory> dirs) {
                        mTempImages.clear();
                        directories.clear();
                        directories.addAll(dirs);
                        photoGridAdapter.notifyDataSetChanged();
                        listAdapter.notifyDataSetChanged();
                        Log.i("photo-size ", "" + directories.get(0).getPhotos().size() + "   " + photoGridAdapter.getCurrentPhotos().size());
                        if (!isTakePhoto && !tempLists.isEmpty()) {
                            addEditPhoto();
//                            tempLists.clear();
                        }

                        if (dirs != null && dirs.size() != 0) {
                            for (int i = 0; i < dirs.size(); i++) {
                                if (dirs.get(i).getPhotos() != null && dirs.get(i).getPhotos().size() != 0) {
                                    for (int j = 0; j < dirs.get(i).getPhotos().size(); j++) {
                                        mTempImages.add(dirs.get(i).getPhotos().get(j).getPath());
                                    }
                                }
                            }
                        }


                    }

                    @Override
                    public void onResultPhotosClallback(List<Photo> photoList) {

                    }
                });
    }


    private void initViews() {
        if (isSupportShare) {
            tv_share.setVisibility(View.VISIBLE);
        } else {
            tv_share.setVisibility(View.GONE);
        }
        tv_finish.setText(getTextString());

        mPreview.setVisibility(View.VISIBLE);
        mPreview.setText("预览");
        initShareText(0);
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), PickerUtils.getSpanNumber(getActivity()));
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new PhotoGridAutofitDecoration(PickerUtils.getSpanNumber(getActivity()), 1));
        recyclerView.setAdapter(photoGridAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        photoGridAdapter.setOnCameraClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
//                    if (!isNeedPicEdit) {
//                        Intent intent = captureManager.dispatchTakePictureIntent();
//                        startActivityForResult(intent, ImageCaptureManager.REQUEST_TAKE_PHOTO);
//                    } else {
//                        Intent intent = new Intent(getActivity(), CameraActivity.class);
//                        startActivityForResult(intent, CameraActivity.TAKE_PHOTO_CODE);
//                    }
                    tempLists.clear();
                    Intent intent = new Intent(getActivity(), CameraActivity.class);
                    startActivityForResult(intent, CameraActivity.TAKE_PHOTO_CODE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        photoGridAdapter.setOnItemCheckListener(new OnItemCheckListener() {
            @Override
            public boolean OnItemCheck(int position, Photo photo, final boolean isCheck, int selectedItemCount) {

                int total = selectedItemCount + (isCheck ? -1 : 1);

                tv_finish.setEnabled(total > 0);
                if (total > maxCount) {
                    toast(getString(R.string.over_max_count_tips, maxCount));
                    return false;
                }
                if (maxCount <= 1) {
                    List<Photo> photos = photoGridAdapter.getSelectedPhotos();
                    if (!photos.contains(photo)) {
                        photos.clear();
                        photoGridAdapter.notifyDataSetChanged();
                    }
                    ((PickerMainActivity) getActivity()).initRightText(total > 1 ? 1 : total, maxCount);
                    initShareText(total > 1 ? 1 : total);
                    return true;
                }
                ((PickerMainActivity) getActivity()).initRightText(total, maxCount);
                initShareText(total);

                return true;
            }

            @Override
            public boolean OnItemCheck(int position, Video video, boolean isCheck, int selectedItemCount) {
                return false;
            }
        });
        initListPopupWindow();
    }

    private void initShareText(int number) {
//        tv_share.setText(getString(R.string.share_to_class, number, maxCount));
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

                PhotoDirectory directory = directories.get(position);
                mAlbum.setText(directory.getName());
                photoGridAdapter.setCurrentDirectoryIndex(position);
                photoGridAdapter.notifyDataSetChanged();
                listAdapter.setCheckPosition(position);
            }
        });
    }

    private void showAlbum() {
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.black)));
        mPopupWindow.setFocusable(true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.setOutsideTouchable(true);
//        mPopupWindow.showAtLocation(mBottom, Gravity.NO_GRAVITY,0,getResources().getDimensionPixelOffset(R.dimen.bottom_bar_height));
        mPopupWindow.showAsDropDown(mAlbum, 0, 0);
    }

    private void hideAlbum() {
        mPopupWindow.dismiss();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ImageCaptureManager.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            if (captureManager == null) {
                FragmentActivity activity = getActivity();
                captureManager = new ImageCaptureManager(activity);
            }
            if (directories.size() > 0) {
                final String path = captureManager.getCurrentPhotoPath();
                String directCamera = Environment.getExternalStorageDirectory() + "/DCIM/Camera";
                File file = new File(directCamera);
                if (file.exists() && file.isDirectory()) {
                    if (PickerUtils.isCustomDevice()) { //定制pad
                        PickerUtils.delete(path);
                    }
                }
                captureManager.galleryAddPic(path);
            }
        } else if (requestCode == CameraActivity.TAKE_PHOTO_CODE && resultCode == RESULT_OK) {
            if (captureManager == null) {
                FragmentActivity activity = getActivity();
                captureManager = new ImageCaptureManager(activity);
            }
            final String path = data.getStringExtra("photo_path");
            captureManager.galleryAddPic(path);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendBroadFalse(path);
                }
            }, 1000);
//            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
//
//            if (!storageDir.exists()) {
//                if (!storageDir.mkdir()) {
//                    Log.e("TAG", "Throwing Errors....");
//                }
//            }
//            File saveFile = null;
//            try {
//                saveFile = File.createTempFile(
//                        UUID.randomUUID().toString(),  /* prefix */
//                        ".jpg",         /* suffix */
//                        storageDir      /* directory */
//                );
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            Intent intent = new Intent(getActivity(), IMGEditActivity.class);
//            intent.putExtra(IMGEditActivity.EXTRA_IMAGE_URI, Uri.fromFile(new File(path)));
//            intent.putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, saveFile.getAbsolutePath());
//            intent.putExtra(IMGEditActivity.IS_DELETE_OLD_PICTURE, true);
//            intent.putExtra(IMGEditActivity.IS_TAKE_PICTURE, true);
//            startActivityForResult(intent, IMGEditActivity.IMG_EDIT_REQUEST_CODE);
        } else if (requestCode == IMGEditActivity.IMG_EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            if (captureManager == null) {
                FragmentActivity activity = getActivity();
                captureManager = new ImageCaptureManager(activity);
            }
            String path = data.getStringExtra("edit_pic_path");
            isTakePhoto = data.getBooleanExtra("isTakePhoto", false);
            captureManager.galleryAddPic(path);
        }
    }


    /**
     * 解决某些机型广播媒体库无法及时更新问题
     */
    private void sendBroadFalse(String imagePath) {
        if (!mTempImages.isEmpty()) {
            if (!mTempImages.contains(imagePath)) {
                Photo photo = new Photo(makeVideoTempId(8), imagePath);
                directories.get(0).getPhotos().add(0, photo);
                photoGridAdapter.notifyDataSetChanged();
                listAdapter.notifyDataSetChanged();
            }
        } else {
            Photo photo = new Photo(makeVideoTempId(8), imagePath);
            directories.get(0).getPhotos().add(0, photo);
            photoGridAdapter.notifyDataSetChanged();
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


    public PhotoGridAdapter getPhotoGridAdapter() {
        return photoGridAdapter;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        captureManager.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        captureManager.onRestoreInstanceState(savedInstanceState);
        super.onViewStateRestored(savedInstanceState);
    }

    public ArrayList<String> getSelectedPhotoPaths() {
        if (photoGridAdapter == null) {
            return new ArrayList<>();
        }
        return photoGridAdapter.getSelectedPhotoPaths();
    }

    public void finishChooseImg() {
        if (null == getSelectedPhotoPaths() || getSelectedPhotoPaths().size() == 0) {
            toast("请选择图片");
        } else {
            mPreview.setClickable(false);
            mPreview.setEnabled(false);
            if (isCompress) {
                compressImages(getSelectedPhotoPaths());
            } else {
                Intent intent = new Intent();
                intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, getSelectedPhotoPaths());
                intent.putExtra(RESULT_TYPE, PHOTO);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
        }
    }

    /**
     * 进入裁剪
     */
    public void capturePicture() {
        if (null == getSelectedPhotoPaths() || getSelectedPhotoPaths().size() == 0) {
            toast("请选择图片");
        } else {
            String path = getSelectedPhotoPaths().get(0);

            Intent intent = new Intent(getActivity(), IMGEditActivity.class);
            intent.putExtra(IMGEditActivity.EXTRA_IMAGE_URI, Uri.fromFile(new File(path)));
            intent.putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, PickerUtils.createFilePath().getAbsolutePath());
            intent.putExtra(IMGEditActivity.IS_DELETE_OLD_PICTURE, false);
            intent.putExtra(IMGEditActivity.IS_TAKE_PICTURE, false);
            startActivityForResult(intent, IMGEditActivity.IMG_EDIT_REQUEST_CODE);
        }
    }

    private void compressImages(ArrayList<String> selectedPhotoPaths) {
        showProgress();
        LubanUtil.compress(getActivity(), selectedPhotoPaths, new LubanUtil.ICompressCallBack() {
            @Override
            public void onComplete(ArrayList<String> files) {
                dismissProgress();
                Intent intent = new Intent();
                intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, files);
                intent.putExtra(RESULT_TYPE, PHOTO);
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
        });
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
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        broadcastManager.unregisterReceiver(receiver);
        if (directories == null) {
            return;
        }

        for (PhotoDirectory directory : directories) {
            directory.getPhotoPaths().clear();
            directory.getPhotos().clear();
            directory.setPhotos(null);
        }
        directories.clear();
        directories = null;
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
            tempLists.clear();
            ArrayList<String> arrayList = getSelectedPhotoPaths();
            if (arrayList == null || arrayList.isEmpty()) {
                toast("请选择图片");
                return;
            }
            Intent intent = new Intent(getActivity(), PhotoPreviewActivity.class);
            intent.putExtra(PhotoPreviewActivity.KEY_FILES, arrayList);
            intent.putExtra(PhotoPreviewActivity.KEY_POSITION, 0);
            intent.putExtra(PhotoPreviewActivity.KEY_NEED_PICEDIT, isNeedPicEdit);
            getActivity().startActivity(intent);
            getActivity().overridePendingTransition(0, 0);

        } else if (viewId == R.id.tv_share) {
            ArrayList<String> arrayList = getSelectedPhotoPaths();
            if (arrayList == null || arrayList.isEmpty()) {
                toast("请选择图片");
                return;
            }
            Intent intent = new Intent();
            intent.putStringArrayListExtra(KEY_SELECTED_PHOTOS, arrayList);
            intent.putExtra(RESULT_TYPE, PHOTO_SHARE);
            getActivity().setResult(RESULT_OK, intent);
            getActivity().finish();
        } else if (viewId == R.id.tv_finish) {
            finishChooseImg();
        }
    }
}
