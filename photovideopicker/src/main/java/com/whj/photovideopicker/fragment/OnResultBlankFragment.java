package com.whj.photovideopicker.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;

import com.whj.photovideopicker.utils.OnResultRequest;


/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/4/27 上午11:55
 */

public class OnResultBlankFragment extends android.app.Fragment {

    public static final String TAG = "on_act_result_event_dispatcher";

    private SparseArray<OnResultRequest.Callback> mCallbacks = new SparseArray<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void startForResult(Intent intent, OnResultRequest.Callback callback) {
        mCallbacks.put(callback.hashCode(), callback);
        startActivityForResult(intent, callback.hashCode());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        OnResultRequest.Callback callback = mCallbacks.get(requestCode);
        mCallbacks.remove(requestCode);

        if (callback != null) {
            callback.onActivityResult(resultCode, data);
        }
    }

}
