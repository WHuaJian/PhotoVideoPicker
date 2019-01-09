package com.whj.photovideopicker.base;


import com.whj.photovideopicker.listener.OnResultListener;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 下午1:57
 */

public class BasePickerListener {

    public OnResultListener listener;

    public void setListener(OnResultListener listener) {
        this.listener = listener;
    }
}
