package com.unisky.live;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageButton;

import com.unisky.live.mlive.KPKit;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author kenping.liu
 * @date 2014.08.08
 * 
 */
@SuppressLint("NewApi")
public class ULAVRecorder implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    // 编码选项
    private ULAVCodecOption mCodecOption;

    // 音频录制和编码
    private AudioRecord mAudioRecord;
    private ULAudioEncoder mAudioEncoder;

    // 视频录制和编码
    private Camera mVideoCamera;
    private SurfaceView mVideoPrevewView;
    private SurfaceHolder mVideoSurfaceHolder;
    private ULVideoEncoder mVideoEncoder;

    // PTS产生器
    private ULAVPTS mPTS;
    // TS 混合器
    private ULAVMuxer mAVMuxer;

    // 线程和控制
    private AtomicBoolean mRunning;
    private BlockingQueue<ULAVData> mVideoDataQueue;
    private Thread mAudioThread;
    private Thread mVideoThread;

    // 输出文件
    private String mOutputFile;
    private boolean                 flashBulbIsOn;
    private Parameters parameter;
    public static boolean isFront;


    private Parameters parameters;
    private int max;
    private int current;

    private Activity activity;
    private CameraInfo cameraInfo;
//    public static int orientationDegree = 0;
    public ULAVRecorder(Activity activity)
    {


//        mAudioRecord = null;
//        mVideoCamera = null;
//        mVideoPrevewView = null;
//        mVideoSurfaceHolder = null;
//        mRunning = new AtomicBoolean(false);
//
//        mCodecOption = new ULAVCodecOption();
//        mAVMuxer = new ULAVMuxer();
//        mPTS = new ULAVPTS();
//        mAudioEncoder = new ULAudioEncoder(mCodecOption, mPTS, mAVMuxer);
//        mVideoEncoder = new ULVideoEncoder(mCodecOption, mPTS, mAVMuxer);
//        mVideoDataQueue = new LinkedBlockingDeque<ULAVData>(5);
//
//        // 默认使用 640*480
//        // mCodecOption.setVideoProfile(ULAVCodecOption.VIDEO_RESOLUTION_288P,
//        // ULAVCodecOption.VIDEO_BITRATE_LOW);
//        mCodecOption.setVideoProfile(ULAVCodecOption.VIDEO_RESOLUTION_480P, ULAVCodecOption.VIDEO_BITRATE_MID);
//        // mCodecOption.setVideoProfile(ULAVCodecOption.VIDEO_RESOLUTION_720P,
//        // ULAVCodecOption.VIDEO_BITRATE_HIGH);

        this(new ULAVCodecOption(ULAVCodecOption.VIDEO_RESOLUTION_720P, ULAVCodecOption.VIDEO_BITRATE_HIGH));
        mVideoCamera = Camera.open(getCameraInfo());
        parameters = mVideoCamera.getParameters();
        this.activity = activity;

    }

    public ULAVRecorder(ULAVCodecOption option)
    {
        mAudioRecord = null;
        mVideoCamera = null;
        mVideoPrevewView = null;
        mVideoSurfaceHolder = null;
        mRunning = new AtomicBoolean(false);

        mCodecOption = option;
        mAVMuxer = new ULAVMuxer();
        mPTS = new ULAVPTS();
        mAudioEncoder = new ULAudioEncoder(mCodecOption, mPTS, mAVMuxer);
        mVideoEncoder = new ULVideoEncoder(mCodecOption, mPTS, mAVMuxer);
        mVideoDataQueue = new LinkedBlockingDeque<ULAVData>(5);
    }

    public ULAVCodecOption codecOption()
    {
        return mCodecOption;
    }

    public int getCameraInfo(){
        int defaultId = -1;
         cameraInfo = new CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
            if(isFront){
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {//默认后置
                     defaultId=i;
                    return defaultId;
                }
            }else{
                if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {//默认后置
                    defaultId=i;
                    return defaultId;
                }
            }
        }
        return defaultId;
    }

    public String outputFile()
    {
        return mOutputFile;
    }

    public ULAVRecorder setOutputFile(String outputFile)
    {
        mOutputFile = outputFile;
        mAVMuxer.setupCodecOption(mCodecOption);
        mAVMuxer.open(outputFile);
        return this;
    }

    public ULAVRecorder setVideoProfile(int resolution, int bitrate)
    {
        if (!mRunning.get())
        {
            mCodecOption.setVideoProfile(resolution, bitrate);
        }
        return this;
    }

    @SuppressWarnings("deprecation")
    public ULAVRecorder setVideoSurfaceView(SurfaceView surfaceView)
    {
        mVideoPrevewView = surfaceView;
        // 绑定SurfaceView，取得SurfaceHolder对象
        mVideoSurfaceHolder = mVideoPrevewView.getHolder();
        // 预览大小设置
        mVideoSurfaceHolder.setFixedSize(mCodecOption.previewSizeVideoWidth, mCodecOption.previewSizeVideoHeight);
        mVideoSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        mVideoSurfaceHolder.addCallback(ULAVRecorder.this);
        return this;
    }
    public  Camera.Size getSupportedPictureSize(){
        if(mVideoCamera==null){
            return null;
        }
        List<Camera.Size> picturesizeList = mVideoCamera.getParameters().getSupportedPictureSizes();
        return getSupportedSizes(picturesizeList, mCodecOption);
    }

    public Camera.Size getSupportedPreviewSize(){
        if(mVideoCamera==null){
            return null;
        }
        List<Camera.Size> previewsizeList = mVideoCamera.getParameters().getSupportedPreviewSizes();
        return getSupportedSizes(previewsizeList, mCodecOption);
    }

    public Camera.Size getSupportedPreviewSize(Camera camera, ULAVCodecOption option){
        if(camera==null){
            return null;
        }
        List<Camera.Size> previewsizeList = camera.getParameters().getSupportedPreviewSizes();
        return getSupportedSizes(previewsizeList, option);
    }

    private Camera.Size getSupportedSizes(List<Camera.Size> list, ULAVCodecOption option){
         Collections.sort(list,new Comparator<Camera.Size>() {

                    @Override
                    public int compare(Camera.Size lhs, Camera.Size rhs) {
                        int w= Integer.compare(lhs.width, rhs.width);
                        if(w==0){
                            return Integer.compare(lhs.height,rhs.height);
                        }
                        return w;
                    }
                });
         Camera.Size selectSize = null;
         for (Camera.Size size : list) {
             if(size.width >= option.videoWidth){
                 if(size.height >= option.videoHeight){
                     selectSize = size;
                     break;
                 }
             }
         }
         return selectSize;
    }
    public void start()
    {
        mRunning.set(true);
        mVideoDataQueue.clear();
        mPTS.reset();
        if (mCodecOption.videoEnabled)
        {
            mVideoEncoder.open();
            mVideoThread = new Thread(new Runnable()
            {
                // private long elapsed;

                @Override
                public void run()
                {
                    // elapsed = 0;
                    Log.i("ulav", "mVideoThread start");
                    mVideoEncoder.open();

                    while (mRunning.get())
                    {
                        try
                        {
                            ULAVData data = mVideoDataQueue.take();
                            // long ts = System.currentTimeMillis();
                            mVideoEncoder.offerVideoData(data.data, data.pts);
                            mVideoEncoder.drainEncodedData();
                            // elapsed += (System.currentTimeMillis() - ts);
                            // if ((mVideoEncoder.mFrames % 100) == 0)
                            // {
                            // Log.i("ulav", "Video Encodeer: frames=" +
                            // mVideoEncoder.mFrames + ", Elpased="
                            // + elapsed + ",Period=" + (elapsed /
                            // mVideoEncoder.mFrames));
                            // }
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }

                    mVideoEncoder.close();
                    Log.i("ulav", "mVideoThread stop");
                }
            });
            mVideoThread.setPriority(Thread.MAX_PRIORITY);
            mVideoThread.start();
        }
        if (mCodecOption.audioEnabled)//音频
        {
            mAudioThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    // Log.i("ulav", "mAudioThread start");
                    mAudioEncoder.open();
                    prepareAudioRecorder();
                    mAudioRecord.startRecording();
                    while (mRunning.get())
                    {
                        mAudioEncoder.offerAudioData(mAudioRecord, false);
                        mAudioEncoder.drainEncodedData();
                    }
                    mAudioRecord.stop();
                    mAudioEncoder.offerAudioData(mAudioRecord, true);
                    mAudioEncoder.drainEncodedData();
                    mAudioEncoder.close();
                    Log.i("ulav", "mAudioThread stop");
                }
            }, "Audio");
            mAudioThread.setPriority(Thread.MAX_PRIORITY);
            mAudioThread.start();
        }
    }

    public void stop()
    {
        mRunning.set(false);
        KPKit.terminalThread(mAudioThread);
        KPKit.terminalThread(mVideoThread);
        mAudioThread = null;
        mVideoThread = null;
//        if(mVideoSurfaceHolder != null){
//            mVideoSurfaceHolder.removeCallback(this);
//            mVideoSurfaceHolder.getSurface().release();
//        }
//        if(mVideoCamera == null){
//            return;
//        }
//        try{
//            mVideoCamera.setPreviewCallback(null);
//            mVideoCamera.stopPreview();
//            mVideoCamera.release();
//            mVideoCamera = null;
//        }catch (Exception e){
//            e.printStackTrace();
//        }finally {
//            System.gc();
//        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {

        if (mRunning.get() && null != data)
        {
//            int width = camera.getParameters().getPreviewSize().width;
//            int height = camera.getParameters().getPreviewSize().height;
//            int rotation = getCorrectCameraOrientation(getCameraInfo(),cameraInfo);
//            Logger.i("Rotation---"+new SimpleDateFormat("yyyy年-MM月dd日-HH时mm分ss秒").format(System.currentTimeMillis())+rotation);
//            if (rotation == 90) {
//                data = Utility.rotateYUV420Degree90(data,width,height);
//            } else if (rotation == 180) {
//                data = Utility.rotateYUV420Degree180(data, width, height);
//            }else if (rotation ==270){
//                data = Utility.rotateYUV420Degree270(data, width, height);
//            }
//


            // Log.i("", data.length+"");
            mVideoDataQueue.offer(new ULAVData(true, data, mPTS.pts()));
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        prepareVideoCamera();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        releaseCamera();
    }

    public void switchCamera(){

    	Log.d("切换摄像头", "切换摄像头");
    	int cameraCount = 0;
//		CameraInfo cameraInfo = new CameraInfo();

		cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数
		for (int i = 0; i < cameraCount; i++) {
			Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
			if (!isFront) {
				// 现在是后置，变更为前置
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置
																					// CAMERA_FACING_BACK后置
					isFront = true;
					releaseCamera();
					mVideoCamera = Camera.open(i);// 打开当前选中的摄像头
		             parameters = mVideoCamera.getParameters();
		            // mCodecOption.videoFPS
//		            parameters.setPreviewFpsRange(25000, 30000);
		            parameters.setPreviewSize(mCodecOption.previewSizeVideoWidth, mCodecOption.previewSizeVideoHeight);
		            parameters.setPreviewFormat(ImageFormat.YV12);
//		            parameters.setPictureSize(mCodecOption.videoWidth, mCodecOption.videoHeight);

		            mVideoCamera.setParameters(parameters);
		            try {
						mVideoCamera.setPreviewDisplay(mVideoSurfaceHolder);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

		            mVideoCamera.setPreviewCallback(ULAVRecorder.this);
		            mVideoCamera.startPreview();

					try {
						mVideoCamera.setPreviewDisplay(mVideoSurfaceHolder);// 通过surfaceview显示取景画面
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mVideoCamera.startPreview();// 开始预览
					break;
				}
			} else {
				// 现在是前置， 变更为后置
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {// 代表摄像头的方位，CAMERA_FACING_FRONT前置
																				// CAMERA_FACING_BACK后置
					isFront = false;
					releaseCamera();
					mVideoCamera = Camera.open(i);// 打开当前选中的摄像头

		            Parameters parameters = mVideoCamera.getParameters();
		            // mCodecOption.videoFPS
//		            parameters.setPreviewFpsRange(25000, 30000);
		            parameters.setPreviewSize(mCodecOption.previewSizeVideoWidth, mCodecOption.previewSizeVideoHeight);
		            parameters.setPreviewFormat(ImageFormat.YV12);
//		            parameters.setPictureSize(mCodecOption.videoWidth, mCodecOption.videoHeight);

		            mVideoCamera.setParameters(parameters);
		            try {
						mVideoCamera.setPreviewDisplay(mVideoSurfaceHolder);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

		            mVideoCamera.setPreviewCallback(ULAVRecorder.this);
		            mVideoCamera.startPreview();

					try {
						mVideoCamera.setPreviewDisplay(mVideoSurfaceHolder);// 通过surfaceview显示取景画面
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mVideoCamera.startPreview();// 开始预览
					break;
				}
			}

		}
    }
    public void flashBulb(ImageButton flashBulb){
    	if (!flashBulbIsOn) {
			// 打开闪光灯
			// Camera camera = Camera.open();
    		mVideoCamera.startPreview();
			parameter = mVideoCamera.getParameters();
			parameter.setFlashMode(Parameters.FLASH_MODE_TORCH);
			mVideoCamera.setParameters(parameter);
//			flashBulb.setImageResource(R.drawable.torch_on);
			flashBulbIsOn = true;
		} else {
			// 关闭闪光灯
			parameter = mVideoCamera.getParameters();
			parameter.setFlashMode(Parameters.FLASH_MODE_OFF);
			mVideoCamera.setParameters(parameter);
//			flashBulb.setImageResource(R.drawable.torch_off);
			flashBulbIsOn = false;
		}
    }
    public void prepareVideoCamera()
    {
        try
        {
//            releaseCamera();


            mVideoCamera = Camera.open(getCameraInfo());
            parameters = mVideoCamera.getParameters();
            // mCodecOption.videoFPS
//            parameters.setPreviewFpsRange(25000, 30000);
            parameters.setPreviewSize(mCodecOption.previewSizeVideoWidth, mCodecOption.previewSizeVideoHeight);
            parameters.setPreviewFormat(ImageFormat.YV12);
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//            parameters.setPictureSize(mCodecOption.videoWidth, mCodecOption.videoHeight);

//            mVideoCamera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
//                @Override
//                public void onAutoFocusMoving(boolean b, Camera camera) {
//                    if(b){
////                        prepareVideoCamera();
////                        camera.cancelAutoFocus();
//                    }
//                }
//            });
            mVideoCamera.setParameters(parameters);
            mVideoCamera.setPreviewDisplay(mVideoSurfaceHolder);

            mVideoCamera.setPreviewCallback(ULAVRecorder.this);
            mVideoCamera.startPreview();
            mVideoCamera.cancelAutoFocus();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void releaseCamera()
    {
        if (null != mVideoCamera)
        {
            // ！！这个必须在前，不然退出出错
            mVideoCamera.setPreviewCallback(null);
            mVideoCamera.stopPreview();
            mVideoCamera.release();
            mVideoCamera = null;
        }
    }

    private void prepareAudioRecorder()
    {
        int min_buffer_size = AudioRecord.getMinBufferSize(mCodecOption.audioSampleRate, mCodecOption.audioChannels,
                ULAVCodecOption.AUDIO_FORMAT);
        int buffer_size = ULAVCodecOption.AUDIO_SAMPLES_PER_FRAME * 10;
        if (buffer_size < min_buffer_size)
        {
            buffer_size = ((min_buffer_size / ULAVCodecOption.AUDIO_SAMPLES_PER_FRAME) + 1)
                    * ULAVCodecOption.AUDIO_SAMPLES_PER_FRAME * 2;
        }
        // mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, //
        // source
        // ULAVCodecOption.AUDIO_SAMPLE_RATE, // sample rate, hz
        // ULAVCodecOption.AUDIO_CHANNEL_CONFIG, // channels
        // ULAVCodecOption.AUDIO_FORMAT, // audio format
        // buffer_size); // buffer size (bytes)
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, mCodecOption.audioSampleRate,
                mCodecOption.audioChannels, ULAVCodecOption.AUDIO_FORMAT, buffer_size);
    }
    public boolean isRunning(){
        return mRunning !=null && mRunning.get();
    }


    public  void setParametersZoom(int zoom){
        parameters.setZoom(zoom);
        mVideoCamera.setParameters(parameters);
    }
    //设置最大放大比例
    public  int getMax(){
        max=parameters.getMaxZoom();
        return max;
    }
    //设置当前放大比例
    public  int getCurrentZoom(){
        current=parameters.getZoom();
        return current;
    }

//    public int getCorrectCameraOrientation( int cameraId,Camera.CameraInfo info) {
//        Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        Logger.i("rotation---"+rotation);
//        int degrees = 0;
//
//        switch(rotation){
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//
//        }
//
//        int result;
//        if(info.facing==Camera.CameraInfo.CAMERA_FACING_FRONT){
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;
//        }else{
//            Logger.i("orientation---"+info.orientation);
//            result = (info.orientation - degrees + 360) % 360;
//        }
////        orientationDegree = result;
//        Logger.i("resylt = "+ result);
//        mVideoCamera.setDisplayOrientation(result);
//        return result;
//    }

}
