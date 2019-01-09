package com.whj.photovideopicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.whj.photovideopicker.base.PickerBaseActivity;
import com.whj.photovideopicker.view.PhotoViewPager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import uk.co.senab.photoview.PhotoView;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/7/31 上午11:33
 */

public class PhotoPreviewActivity extends PickerBaseActivity {

    private ImageView tvBack;
    private TextView topbar_right_icon;
    private ArrayList<String> photos;
    private PhotoViewPager viewPager;
    private int position;
    public static final String KEY_FILES = "KEY_FILES";
    public static final String KEY_POSITION = "KEY_POSITION";

    private String picCount;

    @Override
    public int viewById() {
        return R.layout.activity_photo_preview_layout;
    }

    @Override
    public void bindView() {
        tvBack = $(R.id.topbar_left);
        viewPager = $(R.id.photo_view_pager);
        topbar_right_icon = $(R.id.topbar_right_icon);
        tvBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPreviewActivity.this.finish();
            }
        });
    }

    @Override
    public void afterView() {
        picCount = getResources().getString(R.string.pic_count);
        photos = getIntent().getStringArrayListExtra(KEY_FILES);
        position = getIntent().getIntExtra(KEY_POSITION, 0);
        viewPager.setAdapter(new SamplePagerAdapter());
        viewPager.setCurrentItem(position);
        viewPager.setOffscreenPageLimit(0);
        topbar_right_icon.setText(String.format(picCount, new Object[]{position + 1, photos.size()}));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                topbar_right_icon.setText(String.format(picCount, new Object[]{position + 1, photos.size()}));
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    class SamplePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return photos.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            PhotoView photoView = new PhotoView(PhotoPreviewActivity.this);
            container.addView(photoView, container.getLayoutParams().width, container.getLayoutParams().height);
            if (TextUtils.isEmpty(photos.get(position))) {
                photoView.setImageURI(Uri.parse("http://error"));
            } else {
                if (photos.get(position).startsWith("http")) {
                    try {
                        new HttpBitmapAsyncTask(photoView).execute(photos.get(position));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    photoView.setImageURI(Uri.parse("file://" + photos.get(position)));
                }
            }

            return photoView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
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
