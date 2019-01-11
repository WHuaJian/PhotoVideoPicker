package com.whj.photovideopicker;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.StringDef;

import com.whj.photovideopicker.listener.OnResultListener;
import com.whj.photovideopicker.utils.OnResultRequest;
import com.whj.photovideopicker.utils.PickerUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 下午2:02
 */

public class PhotoPicker {
    public final static String PHOTO_EXTRA_MAX_COUNT = "PHOTO_EXTRA_MAX_COUNT";
    public final static String VIDEO_EXTRA_MAX_COUNT = "VIDEO_EXTRA_MAX_COUNT";
    public final static String VIDEO_SAVE_DIRECTORY = "VIDEO_SAVE_DIRCTORY";
    public final static String EXTRA_SHOW_CAMERA = "SHOW_CAMERA";
    public final static String EXTRA_SHOW_GIF = "SHOW_GIF";
    public final static String KEY_SELECTED_PHOTOS = "SELECTED_PHOTOS";
    public static final String MODE_TYPE_SELECT = "MODE_TYPE_SELECT"; //模式选择
    public static final String RESULT_TYPE = "RESULT_TYPE";
    public static final String SUPPORT_SHARE = "SUPPORT_SHARE"; //是否支持分享，默认不支持
    public static final String IS_TOUPING = "IS_TOUPING"; //是否是投屏
    public static final String IS_COMPRESS = "IS_COMPRESS"; //是否压缩图片
    public final static String IS_NEED_PIC_EDIT = "IS_NEED_PIC_EDIT"; //是否支持裁剪


    public static final String PHOTO = "photo";
    public static final String VIDEO = "video";
    public static final String ALL = "all";
    public static final String PHOTO_SHARE = "photo_share"; //分享给班级
    public static final String VIDEO_SHARE = "video_share"; //分享给班级


    public final static int DEFAULT_MAX_COUNT = 9;
    public static String IP_ADDRESS;
    public static String UDP_PORT;


    private WeakReference<Activity> activity;
    private @ModeType
    String modeType;
    private int choicePhotoNumber;
    private int choiceVideoNumber;
    private String videoDirectory;
    private boolean isSupportShare;
    private boolean is_touping;
    private boolean isNeedPicEdit;
    private boolean isCompress;


    /**
     * 当需要用到摄像头直播时使用该方法初始ip地址和udp端口号
     * */
    public static void configIpAddress(String ip,String port){
        IP_ADDRESS = ip;
        UDP_PORT = port;
    }

    protected PhotoPicker() {

    }

    public static Builder builder(Activity activity){
        return new Builder(activity);
    }

    /**
     *
     */
    public static class Builder {

        private PhotoPicker picker = new PhotoPicker();

        private Builder(Activity activity) {
            picker.activity = new WeakReference<>(activity);
        }

        /**
         * 是否显示视频
         */
        public Builder modeType(@ModeType String modeType) {
            picker.modeType = modeType;
            return this;

        }

        public Builder choicePhotoNumber(int number) {
            picker.choicePhotoNumber = number;
            return this;
        }

        public Builder choiceVideoNumber(int number) {
            picker.choiceVideoNumber = number;
            return this;
        }

        /**
         * 视频保存文件夹路径
         */
        public Builder videoSaveDirectory(String dir) {
            picker.videoDirectory = dir;
            return this;
        }

        /**
         * 是否支持分享
         *
         * @param isShare
         * @return
         */
        public Builder setSupportShare(boolean isShare) {
            picker.isSupportShare = isShare;
            return this;
        }

        /**
         * 是否是投屏
         * @param isTouPing
         * @return
         */
        public Builder setIsTouPing(boolean isTouPing) {
            picker.is_touping = isTouPing;
            return this;
        }


        /**
         * 是否对图片进行压缩
         * @param isCompress
         * @return
         */
        public Builder setPhotoCompress(boolean isCompress){
            picker.isCompress = isCompress;
            return this;
        }

        /**
         * 是否需要进截取界面
         * @param isNeedPicEdit
         * @return
         */
        public Builder setIsNeedPicEdit(boolean isNeedPicEdit){
            picker.isNeedPicEdit = isNeedPicEdit;
            return this;
        }

        public PhotoPicker build(final OnResultListener listener) {
            Intent intent = new Intent(picker.activity.get(), PickerMainActivity.class);
            intent.putExtra(MODE_TYPE_SELECT, picker.modeType);
            intent.putExtra(PHOTO_EXTRA_MAX_COUNT, picker.choicePhotoNumber);
            intent.putExtra(VIDEO_EXTRA_MAX_COUNT, picker.choiceVideoNumber);
            intent.putExtra(VIDEO_SAVE_DIRECTORY, picker.videoDirectory);
            intent.putExtra(SUPPORT_SHARE, picker.isSupportShare);
            intent.putExtra(IS_NEED_PIC_EDIT,picker.isNeedPicEdit);
            intent.putExtra(IS_TOUPING, picker.is_touping);
            intent.putExtra(IS_COMPRESS, picker.isCompress);
            OnResultRequest resultRequest = new OnResultRequest(picker.activity.get());
            resultRequest.startForResult(intent, new OnResultRequest.Callback() {
                @Override
                public void onActivityResult(int resultCode, Intent data) {
                    if (resultCode == Activity.RESULT_OK) {
                        ArrayList<String> result = data.getExtras().getStringArrayList(KEY_SELECTED_PHOTOS);
                        String type = data.getExtras().getString(RESULT_TYPE);
                        if (type.equals(PHOTO)) {
                            listener.onPhotoResult(result);
                        } else if (type.equals(VIDEO)) {
                            listener.onVideoResult(result);
                        } else if (type.equals(PHOTO_SHARE)) {
                            listener.onPhotoShareResult(result);
                        } else if (type.equals(VIDEO_SHARE)) {
                            listener.onVideoShareResult(result);
                        }
                    }
                }
            });

            return picker;
        }

    }

    @StringDef({PHOTO, VIDEO, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ModeType {

    }
}
