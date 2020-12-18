package picker.whj.com.photovideopicker;

import android.Manifest;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;
import com.whj.photovideopicker.PhotoPicker;
import com.whj.photovideopicker.listener.OnResultListener;
import com.whj.photovideopicker.utils.ImagePipelineConfigFactory;

import java.util.ArrayList;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RxPermissions permissions = new RxPermissions(this);
        permissions.request(Manifest.permission.CAMERA

                , Manifest.permission.WRITE_EXTERNAL_STORAGE
                , Manifest.permission.RECORD_AUDIO
                , Manifest.permission.ACCESS_WIFI_STATE
                , Manifest.permission.CHANGE_WIFI_STATE
                , Manifest.permission.CHANGE_NETWORK_STATE
                , Manifest.permission.ACCESS_NETWORK_STATE
                , Manifest.permission.READ_EXTERNAL_STORAGE
                , Manifest.permission.VIBRATE
        )
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                        } else {
                        }
                    }

                });

        findViewById(R.id.select_photo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto();
            }
        });

    }


    public void selectPhoto(){
        PhotoPicker.configIpAddress("111","222");
        PhotoPicker.builder(this)
                .modeType(PhotoPicker.ALL) //三种模式
                .choiceVideoNumber(1) //视频选择最大数量，默认9
                .choicePhotoNumber(9) //图片选择最大数量，默认9
                .setSupportShare(true) //是否支持分享，默认不支持
                .setIsTouPing(false) //是否是投屏，默认是完成
                .setIsNeedPicEdit(true) //是否支持裁剪
                .setPhotoCompress(false) //是否压缩图片
                .videoSaveDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath()) //指定视频存储文件夹
                .build(new OnResultListener() {

                    @Override
                    public void onPhotoResult(ArrayList<String> photos) {
                        Log.i("photos_size= ", photos.size()+"");
                    }

                    @Override
                    public void onVideoResult(ArrayList<String> videos) {

                    }

                    @Override
                    public void onPhotoShareResult(ArrayList<String> files) {
                    }

                    @Override
                    public void onVideoShareResult(ArrayList<String> files) {

                    }

                });
    }
}
