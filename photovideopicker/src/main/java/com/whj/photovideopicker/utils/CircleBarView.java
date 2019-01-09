package com.whj.photovideopicker.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.TextView;

import com.whj.photovideopicker.R;

/**
 * Created by wu_curry on 2018/12/12.
 */

public class CircleBarView extends View {
    private Paint bgPaint; //圆弧背景画笔
    private Paint progressPaint; //圆弧画笔

    private RectF mRectF; //绘制圆弧的矩形区域
    private CircleBarAnim anim;

    private float progressNum;//可以更新的进度条数值
    private float maxNum;//进度条最大值

    private int progressColor;//进度条圆弧颜色
    private int bgColor;//背景圆弧颜色
    private float startAngle;//背景圆弧的起始角度
    private float sweepAngle;//背景圆弧扫过的角度
    private float barWidth;//圆弧进度条宽度

    private int defaultSize;//自定义View默认的宽高
    private float progressSweepAngle;//进度条圆弧扫过的角度

    private TextView textView;
    private OnAnimationListener onAnimationListener;

    private Status status = Status.START;

    public enum Status{
        START,
        ISPLAYING,
        FINISH
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public CircleBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleBarView);
        progressColor = typedArray.getColor(R.styleable.CircleBarView_progress_color, Color.GREEN);
        bgColor = typedArray.getColor(R.styleable.CircleBarView_bg_color, Color.GRAY);
        startAngle = typedArray.getFloat(R.styleable.CircleBarView_start_angle, 0);
        sweepAngle = typedArray.getFloat(R.styleable.CircleBarView_sweep_angle, 360);
        barWidth = typedArray.getDimension(R.styleable.CircleBarView_bar_width, dip2px(context, 10));
        typedArray.recycle();


        progressNum = 0;
        maxNum = 100;
        defaultSize = dip2px(context, 100);
        mRectF = new RectF();

        progressPaint = new Paint();
        progressPaint.setStyle(Paint.Style.STROKE);//只描边，不填充
        progressPaint.setColor(progressColor);
        progressPaint.setAntiAlias(true);//设置抗锯齿
        progressPaint.setStrokeWidth(barWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);//设置画笔为圆角

        bgPaint = new Paint();
        bgPaint.setStyle(Paint.Style.FILL_AND_STROKE);//只描边，不填充
        bgPaint.setColor(bgColor);
        bgPaint.setAntiAlias(true);//设置抗锯齿
        bgPaint.setStrokeWidth(barWidth);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);

        anim = new CircleBarAnim();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int height = measureSize(defaultSize, heightMeasureSpec);
        int width = measureSize(defaultSize, widthMeasureSpec);
        int min = Math.min(width, height);// 获取View最短边的长度
        setMeasuredDimension(min, min);// 强制改View为以最短边为长度的正方形

        if (min >= barWidth * 2) {
            mRectF.set(barWidth / 2, barWidth / 2, min - barWidth / 2, min - barWidth / 2);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(mRectF, startAngle, sweepAngle, false, bgPaint);

        switch (status){
            case START:
                progressPaint.setColor(progressColor);
                canvas.drawArc(mRectF, startAngle, sweepAngle, false, progressPaint);
                break;
            case ISPLAYING:
                progressPaint.setColor(progressColor);
                canvas.drawArc(mRectF, startAngle, sweepAngle, false, progressPaint);
                progressPaint.setColor(getResources().getColor(R.color._5AC8FA));
                canvas.drawArc(mRectF, startAngle, progressSweepAngle, false, progressPaint);
                break;
            case FINISH:
                progressPaint.setColor(progressColor);
                canvas.drawArc(mRectF, startAngle, sweepAngle, false, progressPaint);
                progressPaint.setColor(getResources().getColor(R.color._5AC8FA));
                canvas.drawArc(mRectF, startAngle, progressSweepAngle, false, progressPaint);
                break;
            default:
                progressPaint.setColor(progressColor);
                canvas.drawArc(mRectF, startAngle, sweepAngle, false, progressPaint);
                break;
        }

    }

    public class CircleBarAnim extends Animation {

        public CircleBarAnim() {

        }

        //interpolatedTime从0渐变成1,到1时结束动画,持续时间由setDuration（time）方法设置
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            progressSweepAngle = interpolatedTime * sweepAngle * progressNum / maxNum;
            if (onAnimationListener != null) {
                if (textView != null) {
                    textView.setText(onAnimationListener.howToChangeText(interpolatedTime, progressNum, maxNum));
                }
                onAnimationListener.howToChangeProgressColor(progressPaint, interpolatedTime, progressNum, maxNum);
                if(interpolatedTime == 1){
                    onAnimationListener.onFinishAnim();
                }
            }
            postInvalidate();
        }


    }

    private int measureSize(int defaultSize, int measureSpec) {
        int result = defaultSize;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else if (specMode == MeasureSpec.AT_MOST) {
            result = Math.min(result, specSize);
        }
        return result;
    }

    /**
     * 设置进度条最大值
     *
     * @param maxNum
     */
    public void setMaxNum(float maxNum) {
        this.maxNum = maxNum;
    }

    /**
     * 设置进度条数值
     *
     * @param progressNum 进度条数值
     * @param time        动画持续时间
     */
    public void setProgressNum(float progressNum, int time) {
        this.progressNum = progressNum;
        anim.setDuration(time);
        LinearInterpolator lin = new LinearInterpolator();
        anim.setInterpolator(lin);
    }

    /**
     * 设置显示文字的TextView
     *
     * @param textView
     */
    public void setTextView(TextView textView) {
        this.textView = textView;
    }

    public interface OnAnimationListener {

        /**
         * 如何处理要显示的文字内容
         *
         * @param interpolatedTime 从0渐变成1,到1时结束动画
         * @param updateNum        进度条数值
         * @param maxNum           进度条最大值
         * @return
         */
        String howToChangeText(float interpolatedTime, float updateNum, float maxNum);

        /**
         * 如何处理进度条的颜色
         *
         * @param paint            进度条画笔
         * @param interpolatedTime 从0渐变成1,到1时结束动画
         * @param updateNum        进度条数值
         * @param maxNum           进度条最大值
         */
        void howToChangeProgressColor(Paint paint, float interpolatedTime, float updateNum, float maxNum);

        /**
         * 动画已经结束
         */
        void onFinishAnim();

    }

    public void setOnAnimationListener(OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void finish(){
        this.clearAnimation();
    }

    public void start(){
        this.startAnimation(anim);
    }
}
