package com.whj.photovideopicker;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.unisky.live.ULAVRecorder;
import com.whj.photovideopicker.base.PickerBaseActivity;
import com.whj.photovideopicker.utils.CircleBarView;

import java.text.DecimalFormat;


public class VideoRecorderActivity extends PickerBaseActivity {

    private String videoPath;

    private TextView icon_close;
    private SurfaceView record_preview;
    private CircleBarView circleBarView;
    private TextView text_progress;

    private ULAVRecorder mRecorder; //获取摄像头数据

    private Handler handler = new Handler();

    private int RECORD_TIME_MAX = 60 * 1000;

    private boolean isPlaying = false;

    private String dirPath;

    @Override
    public int viewById() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        return R.layout.tools_activity_video_recorder;
    }

    @Override
    public void bindView() {
        icon_close = $(R.id.icon_close);
        record_preview = $(R.id.record_preview);
        circleBarView = $(R.id.circleBarView);
        text_progress = $(R.id.text_progress);
    }

    @Override
    public void afterView() {
        dirPath = getIntent().getStringExtra("dir_path");
        initRecorder();
        initListener();
    }

    private void initListener() {
        icon_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        circleBarView.setOnAnimationListener(new CircleBarView.OnAnimationListener() {
            @Override
            public String howToChangeText(float interpolatedTime, float updateNum, float maxNum) {
                DecimalFormat decimalFormat = new DecimalFormat("0");
                return decimalFormat.format(interpolatedTime * updateNum / maxNum * (RECORD_TIME_MAX / 1000)) + "秒";
            }

            @Override
            public void howToChangeProgressColor(Paint paint, float interpolatedTime, float updateNum, float maxNum) {

            }

            @Override
            public void onFinishAnim() {
                stopRecorder(true);
            }
        });

        circleBarView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isPlaying) {
                    startRecorder();
                    circleBarView.setStatus(CircleBarView.Status.START);
                    circleBarView.setStatus(CircleBarView.Status.ISPLAYING);
                } else {
                    circleBarView.setStatus(CircleBarView.Status.FINISH);
                    stopRecorder(true);
                }
            }
        });

        circleBarView.setTextView(text_progress);
        circleBarView.setMaxNum(RECORD_TIME_MAX / 1000);
        circleBarView.setProgressNum(RECORD_TIME_MAX / 1000, RECORD_TIME_MAX);
    }

    private void initRecorder() {
        mRecorder = new ULAVRecorder(this);
        mRecorder.setVideoSurfaceView(record_preview);
    }

    private void startRecorder() {
        videoPath = dirPath + "/" + (System.currentTimeMillis() / 1000) + ".ts";
        circleBarView.start();
        mRecorder.setOutputFile(videoPath);
        mRecorder.start();
        isPlaying = true;
    }

    private void stopRecorder(boolean isNeedResult) {
        circleBarView.finish();
        mRecorder.stop();
        isPlaying = false;
        Intent intent = new Intent();
        if (isNeedResult) {
            intent.putExtra("video_return_path_key", videoPath);
            setResult(RESULT_OK, intent);
        }
        finish();
    }


    @Override
    public void onBackPressed() {
        if (isPlaying) {
            showDialog();
        } else {
            super.onBackPressed();
        }
    }

    private void showDialog() {
        new AlertDialog.Builder(this)
                .setMessage("您当前正在拍摄中，是否要退出？")
                .setPositiveButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopRecorder(false);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).create().show();
    }
}
