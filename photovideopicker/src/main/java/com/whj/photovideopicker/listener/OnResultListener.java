package com.whj.photovideopicker.listener;

import java.util.ArrayList;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 下午1:51
 */

public abstract class OnResultListener {

    public void onPhotoResult(ArrayList<String> photos) {
    }

    ; //图片集合

    public void onVideoResult(ArrayList<String> videos) {
    }

    ; //视频集合

    public void onPhotoShareResult(ArrayList<String> files){ //图片分享给班级

    }
    public void onVideoShareResult(ArrayList<String> files){ //视屏分享给班级

    }
}
