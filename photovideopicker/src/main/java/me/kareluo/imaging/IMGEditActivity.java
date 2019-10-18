package me.kareluo.imaging;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import me.kareluo.imaging.core.IMGMode;
import me.kareluo.imaging.core.IMGText;
import me.kareluo.imaging.core.file.IMGAssetFileDecoder;
import me.kareluo.imaging.core.file.IMGDecoder;
import me.kareluo.imaging.core.file.IMGFileDecoder;
import me.kareluo.imaging.core.util.IMGUtils;


/**
 * Created by felix on 2017/11/14 下午2:26.
 */

public class IMGEditActivity extends IMGEditBaseActivity {

    private static final int MAX_WIDTH = 1024;

    private static final int MAX_HEIGHT = 1024;

    public static final String EXTRA_IMAGE_URI = "IMAGE_URI";

    public static final String EXTRA_IMAGE_SAVE_PATH = "IMAGE_SAVE_PATH";

    public static final String IS_DELETE_OLD_PICTURE = "IS_DELETE_OLD_PICTURE";
    public static final String IS_TAKE_PICTURE = "IS_TAKE_PICTURE";

    public static final int IMG_EDIT_REQUEST_CODE = 100;

    private String photoPath;

    @Override
    public void onCreated() {

    }

    @Override
    public Bitmap getBitmap() {
        Intent intent = getIntent();
        if (intent == null) {
            return null;
        }

        Uri uri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
        if (uri == null) {
            return null;
        }

        IMGDecoder decoder = null;

        photoPath = uri.getPath();
        if (!TextUtils.isEmpty(photoPath)) {
            switch (uri.getScheme()) {
                case "asset":
                    decoder = new IMGAssetFileDecoder(this, uri);
                    break;
                case "file":
                    decoder = new IMGFileDecoder(uri);
                    break;
            }
        }

        if (decoder == null) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inJustDecodeBounds = true;

        decoder.decode(options);

        if (options.outWidth > MAX_WIDTH) {
            options.inSampleSize = IMGUtils.inSampleSize(Math.round(1f * options.outWidth / MAX_WIDTH));
        }

        if (options.outHeight > MAX_HEIGHT) {
            options.inSampleSize = Math.max(options.inSampleSize,
                    IMGUtils.inSampleSize(Math.round(1f * options.outHeight / MAX_HEIGHT)));
        }

        options.inJustDecodeBounds = false;

        Bitmap bitmap = decoder.decode(options);
        if (bitmap == null) {
            return null;
        }

        return bitmap;
    }

    @Override
    public void onText(IMGText text) {
        mImgView.addStickerText(text);
    }

    @Override
    public void onModeClick(IMGMode mode) {
        IMGMode cm = mImgView.getMode();
        if (cm == mode) {
            mode = IMGMode.NONE;
        }
        mImgView.setMode(mode);
        updateModeUI();

        if (mode == IMGMode.CLIP) {
            setOpDisplay(OP_CLIP);
        }
    }

    @Override
    public void onUndoClick() {
        IMGMode mode = mImgView.getMode();
        if (mode == IMGMode.DOODLE) {
            mImgView.undoDoodle();
        } else if (mode == IMGMode.MOSAIC) {
            mImgView.undoMosaic();
        }
    }

    @Override
    public void onCancelClick() {
        finish();
    }

    @Override
    public void onDoneClick() {
        boolean isTakePhoto = getIntent().getBooleanExtra(IS_TAKE_PICTURE,false);
        String path = getIntent().getStringExtra(EXTRA_IMAGE_SAVE_PATH);
        boolean isDelete = getIntent().getBooleanExtra(IS_DELETE_OLD_PICTURE, false);
        if (!TextUtils.isEmpty(path)) {
            if (isDelete) {
                deletePicture();
            }
            Bitmap bitmap = mImgView.saveBitmap();
            if (bitmap != null) {
                FileOutputStream fout = null;
                try {
                    fout = new FileOutputStream(path);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fout);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (fout != null) {
                        try {
                            fout.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                Intent intent = new Intent();
                intent.putExtra("edit_pic_path", path);
                intent.putExtra("old_pic_path", photoPath);
                intent.putExtra("isTakePhoto",isTakePhoto);
                setResult(RESULT_OK, intent);
                finish();
                return;
            }
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    private void deletePicture() {
        try {
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            ContentResolver mContentResolver = this.getContentResolver();
            String where = MediaStore.Images.Media.DATA + "='" + photoPath + "'";
            //删除图片
            mContentResolver.delete(uri, where, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCancelClipClick() {
        mImgView.cancelClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onDoneClipClick() {
        mImgView.doClip();
        setOpDisplay(mImgView.getMode() == IMGMode.CLIP ? OP_CLIP : OP_NORMAL);
    }

    @Override
    public void onResetClipClick() {
        mImgView.resetClip();
    }

    @Override
    public void onRotateClipClick() {
        mImgView.doRotate();
    }

    @Override
    public void onColorChanged(int checkedColor) {
        mImgView.setPenColor(checkedColor);
    }
}
