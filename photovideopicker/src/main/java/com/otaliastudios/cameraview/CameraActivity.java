package com.otaliastudios.cameraview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.whj.photovideopicker.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wu_curry on 2018/11/20.
 */

public class CameraActivity extends AppCompatActivity implements View.OnClickListener{

    public static final int TAKE_PHOTO_CODE = 2;

    private CameraView camera;
    private ImageButton take_photo_btn;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        setContentView(R.layout.cameraview_camera_activity);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        findview();
        camera.setLifecycleOwner(this);


    }


    private void findview(){
        camera = findViewById(R.id.camera);
        take_photo_btn = findViewById(R.id.take_photo_btn);

        take_photo_btn.setOnClickListener(this);
        camera.addCameraListener(new CameraListener() {
            @Override
            public void onCameraOpened(CameraOptions options) {

            }

            @Override
            public void onCameraClosed() {

            }

            @Override
            public void onCameraError(@NonNull CameraException exception) {
                Toast.makeText(CameraActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onPictureTaken(byte[] jpeg) {
                FileOutputStream fileOutputStream = null;
                File image = null;
                boolean isSuccess = true;
                try{
                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                    String imageFileName = "JPEG_" + timeStamp + "_";
                    File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

                    if (!storageDir.exists()) {
                        if (!storageDir.mkdir()) {
                            Log.e("TAG", "Throwing Errors....");
                            throw new IOException();
                        }
                    }

                    image = File.createTempFile(
                            imageFileName,  /* prefix */
                            ".jpg",         /* suffix */
                            storageDir      /* directory */
                    );
                    fileOutputStream = new FileOutputStream(image);
                    fileOutputStream.write(jpeg);
                }catch (Exception e){
                    isSuccess = false;
                    e.printStackTrace();
                }finally {
                    if(fileOutputStream != null){
                        try {
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(isSuccess){
                    Intent intent = new Intent();
                    if(image != null){
                        intent.putExtra("photo_path",image.getAbsolutePath());
                        setResult(RESULT_OK,intent);
                        finish();
                    }else{
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }else{
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.take_photo_btn) {
            camera.capturePicture();
        }
    }
}
