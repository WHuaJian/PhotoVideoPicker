package com.whj.photovideopicker.base;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/5/11 上午10:48
 */

public abstract class PickerBaseFragment extends Fragment {

    public View rootView;
    protected Context mContext;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        afterExtra();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getActivity();
        if (rootView == null) {
            rootView = inflater.inflate(viewById(), container, false);
        }
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }

        bindView(rootView);
        return rootView;
    }

    public void bindView(View rootView) {

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        afterView();
    }

    @LayoutRes
    public abstract int viewById();

    @SuppressWarnings("unchecked")
    public final <E extends View> E $(View view, int id) {
        return (E) view.findViewById(id);
    }

    public View getRootView() {
        return rootView;
    }

    @UiThread
    public abstract void afterView();

    @UiThread
    public void afterExtra() {

    }

    @UiThread
    public void toast(String msg) {
        Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
    }

    private boolean hasEntered;

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            onUserVisible();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!hasEntered) {
            if (getUserVisibleHint()) {
                onUserVisible();
            }
            hasEntered = true;
        }
    }


    protected abstract void onUserVisible();

    public ProgressDialog progress;

    public void showProgress() {
        if (progress != null) {
            progress.dismiss();
        }
        progress = ProgressDialog.show(getActivity(), "", "请稍候...", true, false);
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);
    }

    public void dismissProgress() {
        if (progress != null) {
            progress.dismiss();
        }
    }


}
