package com.whj.photovideopicker.base;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.LayoutRes;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.whj.photovideopicker.utils.PickerUtils;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 上午10:37
 */

public abstract class PickerBaseActivity extends AppCompatActivity {

    private boolean isNeedDispatchKeyBord = true;

    public boolean isNeedDispatchKeyBord() {
        return isNeedDispatchKeyBord;
    }

    public void setNeedDispatchKeyBord(boolean needDispatchKeyBord) {
        isNeedDispatchKeyBord = needDispatchKeyBord;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(PickerUtils.isPad(this)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
        setContentView(viewById());

        bindView();
        afterView();
        afterView(savedInstanceState);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (!isNeedDispatchKeyBord())
                return super.dispatchTouchEvent(ev);
            View view = getCurrentFocus();
            if (isHideInput(view, ev)) {
                HideSoftInput(view.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isHideInput(View v, MotionEvent ev) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            if (ev.getX() > left && ev.getX() < right && ev.getY() > top
                    && ev.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    private void HideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token,
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }


    @UiThread
    public void toast(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
    }


    @LayoutRes
    public abstract int viewById();

    @UiThread
    public abstract void afterView();

    @UiThread
    public void afterView(Bundle savedInstanceState){

    };

    public void bindView() {

    }

    @SuppressWarnings("unchecked")
    public final <E extends View> E $(int id) {
        return (E) findViewById(id);
    }


}
