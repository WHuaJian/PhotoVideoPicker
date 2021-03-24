package com.whj.photovideopicker.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;


import com.whj.photovideopicker.utils.PickerUtils;

import uk.co.senab.photoview.PhotoView;


/**
 * Created by jiang on 2017/9/5.
 */

public class DragPhotoView extends PhotoView {

    public static final int TOUCHSLOP = ViewConfiguration.getTouchSlop();

    public static int RESET_TIME = 200;
    public static int TRANSLATE_X_RANGE = 100;
    public static int MAX_ALPHA = 255;
    public static float MIN_SCALE = 0.8f;
    private static float MAX_SCALE = 1f;
    public static float EXIT_MIN_PERCENT = 0.3f;
    private Activity mContext;

    private int max_translate = 0;
    //平移的相关参数
    private float mTranslateX;
    private float mTranslateY;
    private AnimatorSet mResetAnimator;
    private AnimatorSet mExitAnimator;
    private Paint mPaint;
    private int mAlpha = MAX_ALPHA;
    private boolean isInMovingMode = false;
    private float mScale = MAX_SCALE;

    public DragPhotoView(Context context) {
        this(context, null);
    }

    public DragPhotoView(Context context, AttributeSet attr) {
        this(context, attr, 0);
    }

    public DragPhotoView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
        mContext = (Activity) context;
        initPaint();
    }


    private void initPaint() {
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
//        if (mDraweeHolder == null) {
//            GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources())
//                    .setFadeDuration(300)
//                    .build();
//            mDraweeHolder = DraweeHolder.create(hierarchy, getContext());
//        }
    }

    private int mdownX;
    private int mdownY;


    @Override
    protected void onDraw(Canvas canvas) {
        mPaint.setAlpha(mAlpha);
        canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() + getPaddingLeft(), getHeight() + getPaddingTop(), mPaint);
        canvas.translate(mTranslateX, mTranslateY);
        canvas.scale(mScale, mScale, getWidth() / 2, getHeight() / 2);

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        max_translate = (int) (getHeight() * 0.75);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {


        if (getScale() == 1.0f) {
            final int action = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO)
                    ? event.getActionMasked()
                    : event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (actionDown(event)) {
                        return true;
                    } else {
                        return super.dispatchTouchEvent(event);
                    }
                case MotionEvent.ACTION_MOVE:
                    if (actionMove(event)) {
                        isInMovingMode = true;
                        return true;
                    } else {
                        return super.dispatchTouchEvent(event);
                    }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (actionUp(event)) {
                        return true;
                    } else {
                        return super.dispatchTouchEvent(event);
                    }
            }

        }

        return super.dispatchTouchEvent(event);
    }

    private boolean actionUp(MotionEvent event) {
        isInMovingMode = false;


        float percent = Math.abs(mTranslateY / max_translate);

        if (percent > EXIT_MIN_PERCENT) {
            return exitActivity();
        } else {
            return reset();
        }

    }

    private boolean exitActivity() {
        if (mExitAnimator != null) {
            mExitAnimator.cancel();
        } else {
            mExitAnimator = new AnimatorSet();
        }

        if (mScale == MAX_SCALE) {
            return false;
        }

        int max_translate_x_temp;
        if (mTranslateX > TRANSLATE_X_RANGE) {
            max_translate_x_temp = max_translate;
        } else if (mTranslateX < -TRANSLATE_X_RANGE) {
            max_translate_x_temp = max_translate * -1;
        } else {
            max_translate_x_temp = (int) mTranslateX;
        }

        int max_translate_y_temp = mTranslateY < 0 ? max_translate * -1 : max_translate;

        ValueAnimator animatorX = ValueAnimator.ofFloat(mTranslateX, max_translate_x_temp)
                .setDuration(RESET_TIME);
        animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTranslateX = (float) animation.getAnimatedValue();
                invalidate();

            }
        });
        ValueAnimator animatorY = ValueAnimator.ofFloat(mTranslateY, max_translate_y_temp)
                .setDuration(RESET_TIME);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTranslateY = (float) animation.getAnimatedValue();
            }
        });

        ValueAnimator animatorScale = ValueAnimator.ofInt(mAlpha, 0)
                .setDuration(RESET_TIME);
        animatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAlpha = (int) animation.getAnimatedValue();
                invalidate();
            }
        });

        animatorScale.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                exit();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                exit();
            }
        });


        mExitAnimator.playTogether(animatorX, animatorScale, animatorY);
        mExitAnimator.start();

        return true;
    }

    private void exit() {
        if (mContext != null) {
            mContext.finish();
            mContext.overridePendingTransition(0, 0);

        }
    }


    private boolean reset() {

        return postResetAnim();

    }

    private boolean postResetAnim() {

        if (mScale == MAX_SCALE)
            return false;

        if (mResetAnimator != null) {
            mResetAnimator.cancel();
        } else {
            mResetAnimator = new AnimatorSet();
        }
        ValueAnimator animatorX = ValueAnimator.ofFloat(mTranslateX, 0)
                .setDuration(RESET_TIME);
        animatorX.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTranslateX = (float) animation.getAnimatedValue();
                invalidate();

            }
        });
        ValueAnimator animatorY = ValueAnimator.ofFloat(mTranslateY, 0)
                .setDuration(RESET_TIME);
        animatorY.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mTranslateY = (float) animation.getAnimatedValue();
            }
        });


        ValueAnimator animatorScale = ValueAnimator.ofFloat(mScale, MAX_SCALE)
                .setDuration(RESET_TIME);
        animatorScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScale = (float) animation.getAnimatedValue();
            }
        });


        ValueAnimator animatorAlpha = ValueAnimator.ofInt(mAlpha, MAX_ALPHA)
                .setDuration(RESET_TIME);
        animatorAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAlpha = (int) animation.getAnimatedValue();
            }
        });
        mResetAnimator.playTogether(animatorX,
                animatorY, animatorAlpha, animatorScale);
        mResetAnimator.start();

        return true;

    }

    private boolean actionMove(MotionEvent event) {


        if (isInMovingMode) {
            actionTouchMove(event);
            return true;
        }
        if (event.getPointerCount() > 1) {
            return false;
        }

        if (getScale() > 1)
            return false;
        int x = (int) event.getX();
        int y = (int) event.getY();


        //判断是不是左右滑动
        int xL = Math.abs(x - mdownX);
        int yL = Math.abs(y - mdownY);


        // xL > yL 表示左右滑
        if (yL < TOUCHSLOP || xL > yL)
            return false;


        actionTouchMove(event);


        return true;


    }

    private void actionTouchMove(MotionEvent event) {
        int tempX = (int) (event.getX() - mdownX);
        int tempY = (int) (event.getY() - mdownY);

        mTranslateX = tempX;
        mTranslateY = tempY;

        float percent = Math.abs(mTranslateY / max_translate);


        mScale = 1 - percent;


        mAlpha = MAX_ALPHA - (int) (MAX_ALPHA * percent);

        if (mAlpha < 0) {
            mAlpha = 0;
        }
        if (mAlpha > MAX_ALPHA) {
            mAlpha = MAX_ALPHA;
        }


        if (mScale <= MIN_SCALE) {
            mScale = MIN_SCALE;
        }

        if (mScale >= MAX_SCALE) {
            mScale = MAX_SCALE;
        }

        invalidate();
    }

    private boolean actionDown(MotionEvent event) {
        mdownX = (int) event.getX();
        mdownY = (int) event.getY();
        return false;
    }

    private static final String TAG = "DragPhotoView";
//    private CloseableReference<CloseableImage> imageReference = null;
    public void setImageUri(String url) {
        PickerUtils.loadImage(mContext,this,url);

//        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(url)).build();
//        ImagePipeline imagePipeline = Fresco.getImagePipeline();
//        final DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, this);
//        DraweeController controller = Fresco.newDraweeControllerBuilder()
//                .setOldController(mDraweeHolder.getController())
//                .setImageRequest(imageRequest)
//                .setControllerListener(new BaseControllerListener<ImageInfo>() {
//                    @Override
//                    public void onFinalImageSet(String s, @Nullable ImageInfo imageInfo, @Nullable Animatable animatable) {
//                        try {
//                            imageReference = dataSource.getResult();
//                            if (imageReference != null) {
//                                CloseableImage image = imageReference.get();
//                                // do something with the image
//                                if (image != null && image instanceof CloseableStaticBitmap) {
//                                    CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) image;
//                                    Bitmap bitmap = closeableStaticBitmap.getUnderlyingBitmap();
//                                    if (bitmap != null) {
//                                        setImageBitmap(bitmap);
//                                    }
//                                }
//                            }
//                        } finally {
//                            dataSource.close();
//                            CloseableReference.closeSafely(imageReference);
//                        }
//                    }
//                })
//                .setTapToRetryEnabled(true)
//                .build();
//        mDraweeHolder.setController(controller);

    }

//    DraweeHolder<GenericDraweeHierarchy> mDraweeHolder;

    @Override
    protected void onDetachedFromWindow() {
//        mDraweeHolder.onDetach();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onAttachedToWindow() {
        init();
//        mDraweeHolder.onAttach();
        super.onAttachedToWindow();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected boolean verifyDrawable(Drawable dr) {
//        if (dr == mDraweeHolder.getHierarchy().getTopLevelDrawable()) {
//            return true;
//        }
        return false;
    }

    @Override
    public void onStartTemporaryDetach() {
        super.onStartTemporaryDetach();
//        mDraweeHolder.onDetach();
    }

    @Override
    public void onFinishTemporaryDetach() {
        super.onFinishTemporaryDetach();
//        mDraweeHolder.onAttach();
    }

}
