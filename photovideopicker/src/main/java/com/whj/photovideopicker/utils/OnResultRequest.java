package com.whj.photovideopicker.utils;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;

import com.whj.photovideopicker.fragment.OnResultBlankFragment;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/4/27 上午11:56
 */

public class OnResultRequest {

    private OnResultBlankFragment fragment;

    public OnResultRequest(Activity activity) {
        fragment = getEventDispatchFragment(activity);
    }

    private OnResultBlankFragment getEventDispatchFragment(Activity activity) {
        final FragmentManager fragmentManager = activity.getFragmentManager();

        OnResultBlankFragment fragment = findEventDispatchFragment(fragmentManager);
        if (fragment == null) {
            fragment = new OnResultBlankFragment();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, OnResultBlankFragment.TAG)
                    .commitAllowingStateLoss();
            fragmentManager.executePendingTransactions();
        }
        return fragment;
    }

    private OnResultBlankFragment findEventDispatchFragment(FragmentManager manager) {
        return (OnResultBlankFragment) manager.findFragmentByTag(OnResultBlankFragment.TAG);
    }

    public void startForResult(Intent intent, Callback callback) {
        fragment.startForResult(intent, callback);
    }

    public interface Callback {

        void onActivityResult(int resultCode, Intent data);
    }

}
