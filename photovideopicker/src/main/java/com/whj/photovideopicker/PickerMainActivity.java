package com.whj.photovideopicker;

import android.content.Intent;
import android.os.Environment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.whj.photovideopicker.adapter.PhotoPickerPagerAdapter;
import com.whj.photovideopicker.base.PickerBaseActivity;
import com.whj.photovideopicker.base.PickerBaseFragment;
import com.whj.photovideopicker.fragment.PhotoPickerFragment;
import com.whj.photovideopicker.fragment.VideoPickerFragment;
import com.whj.photovideopicker.model.MenuModel;
import com.whj.photovideopicker.utils.ImagePipelineConfigFactory;
import com.whj.photovideopicker.utils.PickerUtils;

import java.util.ArrayList;
import java.util.List;

import static com.whj.photovideopicker.PhotoPicker.ALL;
import static com.whj.photovideopicker.PhotoPicker.DEFAULT_MAX_COUNT;
import static com.whj.photovideopicker.PhotoPicker.IS_COMPRESS;
import static com.whj.photovideopicker.PhotoPicker.IS_NEED_PIC_EDIT;
import static com.whj.photovideopicker.PhotoPicker.IS_TOUPING;
import static com.whj.photovideopicker.PhotoPicker.MODE_TYPE_SELECT;
import static com.whj.photovideopicker.PhotoPicker.PHOTO;
import static com.whj.photovideopicker.PhotoPicker.PHOTO_EXTRA_MAX_COUNT;
import static com.whj.photovideopicker.PhotoPicker.SUPPORT_SHARE;
import static com.whj.photovideopicker.PhotoPicker.VIDEO;
import static com.whj.photovideopicker.PhotoPicker.VIDEO_EXTRA_MAX_COUNT;
import static com.whj.photovideopicker.PhotoPicker.VIDEO_SAVE_DIRECTORY;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 上午10:35
 */

public class PickerMainActivity extends PickerBaseActivity {
    private TextView finishTv;
    private ImageView mBack;
    private List<PickerBaseFragment> mFragments = new ArrayList<>();
    private List<MenuModel> menuModels = new ArrayList<>();

    private int maxPhotoCount = DEFAULT_MAX_COUNT;
    private int maxVideoCount = DEFAULT_MAX_COUNT;
    public String videoDirectory;
    private String DEFAULT_VIDEO_SAVE_DIRECTORY;
    private boolean isSupportShare;
    private boolean isTouping;
    private boolean isNeedPicEdit;
    private boolean isCompress;
    private @PhotoPicker.ModeType
    String modeType;

    private ViewPager viewPager;
    private TabLayout photo_tablayout;
    private PhotoPickerPagerAdapter adapter;

    private PhotoPickerFragment photoPickerFragment;
    private VideoPickerFragment videoPickerFragment;

    private int currentIndex = 0;

    @Override
    public int viewById() {
        return R.layout.activity_picker_main_layout;
    }

    @Override
    public void bindView() {
        finishTv = $(R.id.topbar_right_icon);
        mBack = $(R.id.topbar_left_icon);
        viewPager = $(R.id.viewPager);
        photo_tablayout = $(R.id.photo_tablayout);
    }


    @Override
    public void afterView() {
        Fresco.initialize(getApplication(), ImagePipelineConfigFactory.getImagePipelineConfig(getApplication()));

        DEFAULT_VIDEO_SAVE_DIRECTORY = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).getAbsolutePath();

        maxPhotoCount = getIntent().getIntExtra(PHOTO_EXTRA_MAX_COUNT, DEFAULT_MAX_COUNT);
        maxVideoCount = getIntent().getIntExtra(VIDEO_EXTRA_MAX_COUNT, DEFAULT_MAX_COUNT);
        videoDirectory = getIntent().getExtras().getString(VIDEO_SAVE_DIRECTORY, DEFAULT_VIDEO_SAVE_DIRECTORY);
        isSupportShare = getIntent().getExtras().getBoolean(SUPPORT_SHARE, false);
        isTouping = getIntent().getExtras().getBoolean(IS_TOUPING, false);
        isCompress = getIntent().getExtras().getBoolean(IS_COMPRESS, false);
        isNeedPicEdit = getIntent().getExtras().getBoolean(IS_NEED_PIC_EDIT,false);
        modeType = getIntent().getExtras().getString(MODE_TYPE_SELECT, ALL);

        switchMode();

    }

    /**
     * 三种模式
     */
    private void switchMode() {
        photoPickerFragment = PhotoPickerFragment.newInstance(maxPhotoCount, isNeedPicEdit,isCompress,isSupportShare);
        videoPickerFragment = VideoPickerFragment.newInstance(maxVideoCount, videoDirectory, isSupportShare);
        switch (modeType) {
            case PHOTO:
                mFragments.add(photoPickerFragment);
                menuModels.add(new MenuModel(1, "图片", false));
                break;
            case VIDEO:
                mFragments.add(videoPickerFragment);
                menuModels.add(new MenuModel(2, "视频", false));
                break;
            case ALL:
                mFragments.add(photoPickerFragment);
                mFragments.add(videoPickerFragment);
                menuModels.add(new MenuModel(1, "图片", true));
                menuModels.add(new MenuModel(2, "视频", false));
                break;
        }
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        photo_tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentIndex = tab.getPosition();
                if (tab.getPosition() == 0) {
                    initRightText(photoPickerFragment.getPhotoSelectNumer(), maxPhotoCount);
                } else {
                    initRightText(videoPickerFragment.getVideoSelectNumber(), maxVideoCount);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        initRightText(0, maxPhotoCount);
//        PickerUtils.setTabIndicatorWidth(this, photo_tablayout);
        photo_tablayout.setSelectedTabIndicatorHeight(0);
        setTabLayout();

        finishTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (modeType) {
                    case PHOTO:
                        photoPickerFragment.finishChooseImg();
                        break;
                    case VIDEO:
                        videoPickerFragment.finishChooseVideo();
                        break;
                    case ALL:
                        if(currentIndex == 0){
                            photoPickerFragment.finishChooseImg();
                        }else{
                            videoPickerFragment.finishChooseVideo();
                        }
                        break;
                }
            }
        });

    }

    public void initRightText(int selectCount, int maxCount) {
        finishTv.setText((getString(getTextString(), selectCount, maxCount)));
    }

    public TextView getRightText() {
        return finishTv;
    }

    private void setTabLayout() {
        adapter = new PhotoPickerPagerAdapter(getSupportFragmentManager(), viewPager) {
            @Override
            public CharSequence setTabTitle(int position) {
                return menuModels.get(position).getTabName();
            }

            @Override
            public int getFragmentSize() {
                return mFragments.size();
            }

            @Override
            public Fragment getItemFragment(int position) {
                return mFragments.get(position);
            }
        };
        viewPager.setAdapter(adapter);
        photo_tablayout.setupWithViewPager(viewPager);
        viewPager.setOffscreenPageLimit(mFragments.size());
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private int getTextString() {
        if (isTouping) {
            return R.string.done_with_count;
        }

        return R.string.done_with_count_finish;
    }

    @Override
    protected void onDestroy() {
        mFragments.clear();
        photo_tablayout.removeAllTabs();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == VideoPickerFragment.REQUEST_TAKE_LENOVO){
            videoPickerFragment.onActivityResult(requestCode,resultCode,data);
        }
    }
}


