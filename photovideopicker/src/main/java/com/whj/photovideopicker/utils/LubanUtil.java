package com.whj.photovideopicker.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.whj.photovideopicker.model.Photo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import top.zibin.luban.CompressionPredicate;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/6/29 下午5:53
 */

public class LubanUtil {


    /**
     * 采用Rxjava同步压缩，解决多张大图压缩OOM问题
     * */
    public static Single<List<String>> compress(final Context context, final ArrayList<String> selectedPhotoPaths) {
        return Flowable.fromIterable(selectedPhotoPaths)
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        return Luban.with(context).load(s).get().get(0).getAbsolutePath();
                    }
                }).toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

    }


    /**
     * 刚方法废弃
     * */
    public static void compress(final Context context, final List<String> files, final ICompressCallBack callBack) {
        final ArrayList<String> mResult = new ArrayList<>();
        Luban.with(context)
                .load(files)// 传人要压缩的图片列表
                .ignoreBy(100)
                .filter(new CompressionPredicate() {
                    @Override
                    public boolean apply(String path) {
                        return !(TextUtils.isEmpty(path) || path.toLowerCase().endsWith(".gif"));
                    }
                })
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        mResult.add(file.getAbsolutePath());
                        if (mResult.size() == files.size()) {
                            successCallBack(mResult, callBack);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.v("error ", e.getMessage());
                    }
                }).launch();
    }


    private static void successCallBack(final ArrayList<String> path, final ICompressCallBack callBack) {
        if (callBack == null)
            return;
        synchronized (LubanUtil.class) {
            if (path == null || path.isEmpty())
                return;
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    callBack.onComplete(path);
                }
            });

        }

    }

    public interface ICompressCallBack {
        void onComplete(ArrayList<String> files);
    }
}
