package com.whj.photovideopicker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.whj.photovideopicker.base.PickerBaseActivity;
import com.whj.photovideopicker.utils.ImageCaptureManager;
import com.whj.photovideopicker.utils.PickerUtils;
import com.whj.photovideopicker.view.DragPhotoView;
import com.whj.photovideopicker.view.PhotoViewPager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import me.kareluo.imaging.IMGEditActivity;
import uk.co.senab.photoview.PhotoView;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/7/31 上午11:33
 */

public class PhotoPreviewActivity extends PickerBaseActivity {

    public static final String KEY_FILES = "KEY_FILES";
    public static final String KEY_POSITION = "KEY_POSITION";
    public static final String KEY_NEED_PICEDIT = "KEY_NEED_PICEDIT";

    private ArrayList<String> mList = new ArrayList<>();
    private DragPhotoView[] mPhotoViews;
    private PhotoViewPager mViewPager;
    private int mPhotoPosition = 0;
    private int mPageSelectedPosition = 0;
    private TextView mPercent_all, tvImageEdit, tvBack;
    private Boolean isNeedPicEdit;

    @Override
    public int viewById() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return R.layout.activity_photo_preview_layout;
    }

    @Override
    public void bindView() {
        mViewPager = $(R.id.images);
        mPercent_all = $(R.id.page_all);
        tvImageEdit = $(R.id.tvImageEdit);
        tvBack = $(R.id.tvBack);
    }


    @Override
    public void afterView(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            ArrayList<String> temp = savedInstanceState.getStringArrayList(KEY_FILES);
            if (temp != null && temp.size() > 0) {
                mPhotoPosition = savedInstanceState.getInt(KEY_POSITION, 0);
                initData(temp);
                initViewPager();
            } else {
                Toast.makeText(this, "图片列表为空", Toast.LENGTH_SHORT).show();
            }

        } else {
            ArrayList<String> temps = getIntent().getStringArrayListExtra(KEY_FILES);
            mPhotoPosition = getIntent().getIntExtra(KEY_POSITION, 0);
            initData(temps);
            initViewPager();
        }
        isNeedPicEdit = getIntent().getBooleanExtra(KEY_NEED_PICEDIT, false);
        broadcastManager = LocalBroadcastManager.getInstance(this);
        tvImageEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PhotoPreviewActivity.this, IMGEditActivity.class);
                intent.putExtra(IMGEditActivity.EXTRA_IMAGE_URI, Uri.fromFile(new File(mList.get(mPageSelectedPosition))));
                intent.putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, PickerUtils.createFilePath().getAbsolutePath());
                intent.putExtra(IMGEditActivity.IS_DELETE_OLD_PICTURE, false);
                intent.putExtra(IMGEditActivity.IS_TAKE_PICTURE, false);
                startActivityForResult(intent, IMGEditActivity.IMG_EDIT_REQUEST_CODE);
            }
        });

        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSelf();
            }
        });

        if (isNeedPicEdit) {
            if (mList.get(0).startsWith("http")) {
                tvImageEdit.setVisibility(View.GONE);
            } else {
                tvImageEdit.setVisibility(View.VISIBLE);
            }
        } else {
            tvImageEdit.setVisibility(View.GONE);
        }
    }

    private LocalBroadcastManager broadcastManager;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMGEditActivity.IMG_EDIT_REQUEST_CODE && resultCode == RESULT_OK) {
            String path = data.getStringExtra("edit_pic_path");
            boolean isTakePhoto = data.getBooleanExtra("isTakePhoto", false);

            mList.set(mPageSelectedPosition, path);
            mViewPager.getAdapter().notifyDataSetChanged();

            data.setAction("photo_edit");
            broadcastManager.sendBroadcast(data);
        }
    }

    @Override
    public void afterView() {

    }


    private void initViewPager() {

        System.gc();
        mViewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return mList.size();
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                View view = View.inflate(PhotoPreviewActivity.this, R.layout.k12_photo_layout_photopager, null);
                final DragPhotoView photoView = view.findViewById(R.id.photoView);
                ImageView rotateLeft = view.findViewById(R.id.rotateLeft);
                ImageView rotateRight = view.findViewById(R.id.rotateRight);


                if (TextUtils.isEmpty(mList.get(position))) {
                    photoView.setImageURI(Uri.parse("http://error"));
                } else {
                    if (mList.get(position).startsWith("http")) {
                        try {
                            new HttpBitmapAsyncTask(photoView).execute(mList.get(position));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        photoView.setImageUri("file://" + mList.get(position));
                    }
                }

                photoView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finishSelf();
                    }
                });

                //设置点击事件
                rotateLeft.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
                        photoView.setImageBitmap(rotaingImageView(90, bitmap));
                    }
                });
                rotateRight.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();
                        photoView.setImageBitmap(rotaingImageView(-90, bitmap));
                    }
                });

                container.addView(view, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                return view;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(mPhotoViews[position]);
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public int getItemPosition(Object object) {
                return POSITION_NONE;
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mPageSelectedPosition = position;
                mPercent_all.setText(mPageSelectedPosition + 1 + "/" + mList.size());

                if (isNeedPicEdit) {
                    if (mList.get(position).startsWith("http")) {
                        tvImageEdit.setVisibility(View.GONE);
                    } else {
                        tvImageEdit.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvImageEdit.setVisibility(View.GONE);
                }


            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setCurrentItem(mPhotoPosition);
        String url = mList.get(mPhotoPosition);
        mPercent_all.setText(mPageSelectedPosition + 1 + "/" + mList.size());
    }

    @Override
    public void onBackPressed() {
        finishSelf();
    }

    /**
     * 旋转图片
     *
     * @return 处理后的Bitmap
     */
    public Bitmap rotaingImageView(float degrees, Bitmap bitmap) {
        if (degrees == 0 || null == bitmap) {
            return bitmap;
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        return bmp;
    }

    private void initData(ArrayList<String> temps) {

        mList.clear();
        mPhotoViews = new DragPhotoView[temps.size()];
        for (int i = 0; i < temps.size(); i++) {
            mList.add(temps.get(i));
            mPhotoViews[i] = View.inflate(this, R.layout.photopager_item_viewpager, null).findViewById(R.id.photoView);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.gc();
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putStringArrayList(KEY_FILES, mList);
        outState.putInt(KEY_POSITION, mPhotoPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {

            ArrayList<String> temp = savedInstanceState.getStringArrayList(KEY_FILES);
            if (temp != null && temp.size() > 0) {
                mList.clear();
                mList.addAll(temp);
            }

            mPhotoPosition = savedInstanceState.getInt(KEY_POSITION, 0);
        }
    }

    private void finishSelf() {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        finish();
        overridePendingTransition(0, 0);
    }


    private class HttpBitmapAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private PhotoView mView;

        public HttpBitmapAsyncTask(PhotoView photoView) {
            mView = photoView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            URL myFileUrl = null;
            Bitmap bitmap = null;
            try {
                myFileUrl = new URL(strings[0]);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                HttpURLConnection conn = (HttpURLConnection) myFileUrl.openConnection();
                conn.setConnectTimeout(0);
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                bitmap = BitmapFactory.decodeStream(is);
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mView.setImageBitmap(bitmap);
        }
    }
}
