package com.jack.player.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnticipateInterpolator;

import com.jack.player.R;


/**
 * 播放暂停切换View
 */
public class PlayView extends View {
    /**
     * 播放
     */
    public static int STATE_PLAY = 0;
    /**
     * 暂停
     */
    public static int STATE_PAUSE = 1;
    /**
     * 里圈颜色
     */
    public static int DEFAULT_LINE_COLOR = Color.WHITE;
    /**
     * 外圈圆的颜色
     */
    public static int DEFAULT_BG_LINE_COLOR = 0xfffafafa;

    public static int DEFAULT_LINE_WIDTH = 4;

    public static int DEFAULT_BG_LINE_WIDTH = 4;

    public static int DEFAULT_DURATION = 1200;

    private int mCurrentState = STATE_PAUSE;

    private Paint mPaint, mBgPaint;

    private int mWidth;

    private int mCenterX, mCenterY;

    private int mCircleRadius;

    private RectF mRectF, mBgRectF;

    private float mFraction = 1;

    private Path mPath, mDstPath;
    /**
     * Path路径点的坐标追踪器
     */
    private PathMeasure mPathMeasure;

    private float mPathLength;

    private int mDuration;

    public PlayView(Context context) {
        super(context);
    }

    public PlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        @SuppressLint("CustomViewStyleable") TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.play);
        int lineColor = ta.getColor(R.styleable.play_play_line_color, DEFAULT_LINE_COLOR);
        int bgLineColor = ta.getColor(R.styleable.play_play_bg_line_color, DEFAULT_BG_LINE_COLOR);
        int lineWidth = ta.getInteger(R.styleable.play_play_line_width, dp2px(DEFAULT_LINE_WIDTH));
        int bgLineWidth = ta.getInteger(R.styleable.play_play_bg_line_width, dp2px(DEFAULT_BG_LINE_WIDTH));
        ta.recycle();
        //LAYER_TYPE_SOFTWARE
        //无论硬件加速是否打开，都会有一张Bitmap（software layer），并在上面对WebView进行软渲染。
        //好处：
        //在进行动画，使用software可以只画一次View树，很省。
        //什么时候不要用：
        //View树经常更新时不要用。尤其是在硬件加速打开时，每次更新消耗的时间更多。因为渲染完这张Bitmap后还需要再把这张Bitmap渲染到hardware layer上面去。
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //描边
        mPaint.setStyle(Paint.Style.STROKE);
        //圆角的笔触
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setColor(lineColor);
        mPaint.setStrokeWidth(lineWidth);
        //将Path的各个连接线段之间的夹角用一种更平滑的方式连接
        mPaint.setPathEffect(new CornerPathEffect(1));

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setStyle(Paint.Style.STROKE);
        mBgPaint.setStrokeCap(Paint.Cap.ROUND);
        mBgPaint.setColor(bgLineColor);
        mBgPaint.setStrokeWidth(bgLineWidth);

        mPath = new Path();
        mDstPath = new Path();
        mPathMeasure = new PathMeasure();

        mDuration = DEFAULT_DURATION;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w * 9 / 10;
        int mHeight = h * 9 / 10;
        mCircleRadius = mWidth / dp2px(4);
        mCenterX = w / 2;
        mCenterY = h / 2;
        mRectF = new RectF(mCenterX - mCircleRadius, mCenterY + 0.6f * mCircleRadius,
                mCenterX + mCircleRadius, mCenterY + 2.6f * mCircleRadius);
        mBgRectF = new RectF(mCenterX - mWidth / 2f, mCenterY - mHeight / 2f, mCenterX + mWidth / 2f, mCenterY + mHeight / 2f);
        mPath.moveTo(mCenterX - mCircleRadius, mCenterY + 1.8f * mCircleRadius);
        mPath.lineTo(mCenterX - mCircleRadius, mCenterY - 1.8f * mCircleRadius);
        mPath.lineTo(mCenterX + mCircleRadius, mCenterY);
        mPath.close();
        //不闭合
        mPathMeasure.setPath(mPath, false);
        mPathLength = mPathMeasure.getLength();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(mCenterX, mCenterY, mWidth / 2f, mBgPaint);
        if (mFraction < 0) {    //嗷~~ 弹性部分
            canvas.drawLine(mCenterX + mCircleRadius, mCenterY - 1.6f * mCircleRadius + 10 * mCircleRadius * mFraction,
                    mCenterX + mCircleRadius, mCenterY + 1.6f * mCircleRadius + 10 * mCircleRadius * mFraction, mPaint);

            canvas.drawLine(mCenterX - mCircleRadius, mCenterY - 1.6f * mCircleRadius,
                    mCenterX - mCircleRadius, mCenterY + 1.6f * mCircleRadius, mPaint);

            canvas.drawArc(mBgRectF, -105, 360, false, mPaint);
        } else if (mFraction <= 0.3) {  //嗷~~ 右侧直线和下方曲线
            canvas.drawLine(mCenterX + mCircleRadius, mCenterY - 1.6f * mCircleRadius + mCircleRadius * 3.2f / 0.3f * mFraction,
                    mCenterX + mCircleRadius, mCenterY + 1.6f * mCircleRadius, mPaint);

            canvas.drawLine(mCenterX - mCircleRadius, mCenterY - 1.6f * mCircleRadius,
                    mCenterX - mCircleRadius, mCenterY + 1.6f * mCircleRadius, mPaint);

            if (mFraction != 0)
                canvas.drawArc(mRectF, 0f, 180f / 0.3f * mFraction, false, mPaint);

            canvas.drawArc(mBgRectF, -105 + 360 * mFraction, 360 * (1 - mFraction), false, mPaint);
        } else if (mFraction <= 0.6) {  //嗷~~ 下方曲线和三角形
            canvas.drawArc(mRectF, 180f / 0.3f * (mFraction - 0.3f), 180 - 180f / 0.3f * (mFraction - 0.3f), false, mPaint);

            mDstPath.reset();
            //mDstPath.lineTo(0 ,0);
            mPathMeasure.getSegment(0.02f * mPathLength, 0.38f * mPathLength + 0.42f * mPathLength / 0.3f * (mFraction - 0.3f),
                    mDstPath, true);
            canvas.drawPath(mDstPath, mPaint);

            canvas.drawArc(mBgRectF, -105 + 360 * mFraction, 360 * (1 - mFraction), false, mPaint);
        } else if (mFraction <= 0.8) {  //嗷~~ 三角形
            mDstPath.reset();
            mPathMeasure.getSegment(0.02f * mPathLength + 0.2f * mPathLength / 0.2f * (mFraction - 0.6f)
                    , 0.8f * mPathLength + 0.2f * mPathLength / 0.2f * (mFraction - 0.6f),
                    mDstPath, true);
            canvas.drawPath(mDstPath, mPaint);

            canvas.drawArc(mBgRectF, -105 + 360 * mFraction, 360 * (1 - mFraction), false, mPaint);
        } else {    //嗷~~ 弹性部分
            mDstPath.reset();
            //mDstPath.lineTo(0 ,0);
            mPathMeasure.getSegment(10 * mCircleRadius * (mFraction - 1)
                    , mPathLength,
                    mDstPath, true);
            canvas.drawPath(mDstPath, mPaint);
        }
    }

    public void play() {
        if (mCurrentState == STATE_PLAY) {
            return;
        }
        mCurrentState = STATE_PLAY;
        //属性动画
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1.f, 100.f);
        valueAnimator.setDuration(mDuration);
        valueAnimator.setInterpolator(new AnticipateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //getAnimatedValue()和getAnimatedFraction()的区别
                //getAnimatedValue()的返回值是一个Object，表示当仅有一个属性处于动画状态时，由该ValueAnimator计算的当前属性值。
                //getAnimatedFraction()的返回值是一个float,其值在0.0和1.0之间，其线性度是由插值器决定的(fraction = t / duration ),而animatedValue值是由相关属性值 * fraction所得的。
                mFraction = 1 - valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }
    }

    public void pause() {
        if (mCurrentState == STATE_PAUSE) {
            return;
        }
        mCurrentState = STATE_PAUSE;
        //属性动画
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(1.f, 100.f);
        valueAnimator.setDuration(mDuration);
        valueAnimator.setInterpolator(new AnticipateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction = valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        if (!valueAnimator.isRunning()) {
            valueAnimator.start();
        }
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(int currentState) {
        this.mCurrentState = currentState;
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                getContext().getResources().getDisplayMetrics());
    }
}
