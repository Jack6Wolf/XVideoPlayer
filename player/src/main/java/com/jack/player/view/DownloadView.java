package com.jack.player.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.StringDef;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.jack.player.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * 下载显示View
 */

public class DownloadView extends View {

    public static final int STATE_PRE = 0;

    public static final int STATE_DOWNLOADING = 1;

    public static final int STATE_END = 2;

    public static final int STATE_RESET = 3;

    private static final int DEFAULT_LINE_COLOR = Color.WHITE;

    private static final int DEFAULT_BG_LINE_COLOR = 0xff3a3f45;

    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;

    private static final int DEFAULT_LINE_WIDTH = 9;

    private static final int DEFAULT_BG_LINE_WIDTH = 9;

    private static final int DEFAULT_TEXT_SIZE = 14;

    private static final int DEFAULT_STATE = STATE_PRE;

    private static final int DEFAULT_RIPPLE_SPEED = 2;

    private static final int DEFAULT_DOWNLOAD_TIME = 2000;

    private static final String DEFAULT_DOWNLOAD_UNIT = DownloadUnit.B;
    private ValueAnimator mValueAnimator;
    private int mCurrentState;
    private float mCurrentRippleX;
    private double mCurrentSize, mTotalSize;
    private int mTextSize;
    private int mDownloadTime;
    private String mUnit;
    private Paint mPaint, mBgPaint, mTextPaint;
    private Path mPath;
    private RectF mRectF, mClipRectF;
    private float mFraction;

    private float mCenterX, mCenterY;

    private float mBaseLength, mCircleRadius, mBaseRippleLength;

    public DownloadView(Context context) {
        super(context);
    }

    public DownloadView(Context context, AttributeSet attrs) {
        super(context, attrs);

        @SuppressLint("CustomViewStyleable") TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.download);
        int lineColor = ta.getColor(R.styleable.download_download_line_color, DEFAULT_LINE_COLOR);
        int bgLineColor = ta.getColor(R.styleable.download_download_bg_line_color, DEFAULT_BG_LINE_COLOR);
        int textColor = ta.getColor(R.styleable.download_download_text_color, DEFAULT_TEXT_COLOR);
        int lineWidth = ta.getInteger(R.styleable.download_download_line_width, DEFAULT_LINE_WIDTH);
        int bgLineWidth = ta.getInteger(R.styleable.download_download_bg_line_width, DEFAULT_BG_LINE_WIDTH);
        int textSize = ta.getInteger(R.styleable.download_download_text_size, DEFAULT_TEXT_SIZE);
        ta.recycle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(lineColor);

        mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBgPaint.setStyle(Paint.Style.STROKE);
        mBgPaint.setStrokeCap(Paint.Cap.ROUND);
        mBgPaint.setStrokeWidth(bgLineWidth);
        mBgPaint.setColor(bgLineColor);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        mPath = new Path();

        mTextSize = textSize;
        mCurrentState = DEFAULT_STATE;
        mUnit = DEFAULT_DOWNLOAD_UNIT;
        mDownloadTime = DEFAULT_DOWNLOAD_TIME;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCenterX = (float) w / 2;
        mCenterY = (float) h / 2;
        mCircleRadius = (float) w * 5 / 12;
        mBaseLength = mCircleRadius / 3;
        mBaseRippleLength = 4.4f * mBaseLength / 12;
        mCurrentRippleX = mCenterX - mBaseRippleLength * 10;
        mRectF = new RectF(mCenterX - mCircleRadius, mCenterY - mCircleRadius, mCenterX + mCircleRadius, mCenterY + mCircleRadius);
        mClipRectF = new RectF(mCenterX - 6 * mBaseRippleLength, 0, mCenterX + 6 * mBaseRippleLength, (float) h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        switch (mCurrentState) {
            case STATE_PRE:
                if (mFraction <= 0.4) {
                    canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                    canvas.drawLine(mCenterX - mBaseLength, mCenterY, mCenterX, mCenterY + mBaseLength, mPaint);
                    canvas.drawLine(mCenterX, mCenterY + mBaseLength, mCenterX + mBaseLength, mCenterY, mPaint);
                    canvas.drawLine(mCenterX, mCenterY + mBaseLength - 1.3f * mBaseLength / 0.4f * mFraction,
                            mCenterX, mCenterY - 1.6f * mBaseLength + 1.3f * mBaseLength / 0.4f * mFraction, mPaint);
                } else if (mFraction <= 0.6) {
                    canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                    canvas.drawCircle(mCenterX, mCenterY - 0.3f * mBaseLength, 2, mPaint);
                    canvas.drawLine(mCenterX - mBaseLength - mBaseLength * 1.2f / 0.2f * (mFraction - 0.4f), mCenterY, mCenterX, mCenterY + mBaseLength - mBaseLength / 0.2f * (mFraction - 0.4f), mPaint);
                    canvas.drawLine(mCenterX, mCenterY + mBaseLength - mBaseLength / 0.2f * (mFraction - 0.4f), mCenterX + mBaseLength + mBaseLength * 1.2f / 0.2f * (mFraction - 0.4f), mCenterY, mPaint);
                } else if (mFraction <= 1) {
                    canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                    canvas.drawCircle(mCenterX, mCenterY - 0.3f * mBaseLength - (mCircleRadius - 0.3f * mBaseLength) / 0.4f * (mFraction - 0.6f), 2, mPaint);
                    canvas.drawLine(mCenterX - mBaseLength * 2.2f, mCenterY, mCenterX + mBaseLength * 2.2f, mCenterY, mPaint);
                } else {
                    canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                    canvas.drawCircle(mCenterX, mCenterY - mCircleRadius - mBaseLength * 3 * (mFraction - 1), 3, mPaint);
                    canvas.drawLine(mCenterX - mBaseLength * 2.2f, mCenterY, mCenterX + mBaseLength * 2.2f, mCenterY, mPaint);
                }
                break;
            case STATE_DOWNLOADING:
                if (mFraction <= 0.2) {
                    mTextPaint.setTextSize(mTextSize / 0.2f * mFraction);
                }
                canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                canvas.drawArc(mRectF, -90, 359.99f * mFraction, false, mPaint);
                mPath.reset();
                mCurrentRippleX += DEFAULT_RIPPLE_SPEED;
                if (mCurrentRippleX > mCenterX - mBaseRippleLength * 6)
                    mCurrentRippleX = mCenterX - mBaseRippleLength * 10;
                mPath.moveTo(mCurrentRippleX, mCenterY);
                for (int i = 0; i < 4; i++) {
                    mPath.rQuadTo(mBaseRippleLength, -(1 - mFraction) * mBaseRippleLength, mBaseRippleLength * 2, 0);
                    mPath.rQuadTo(mBaseRippleLength, (1 - mFraction) * mBaseRippleLength, mBaseRippleLength * 2, 0);
                }
                canvas.save();
                canvas.clipRect(mClipRectF);
                canvas.drawPath(mPath, mPaint);
                canvas.restore();
                if (mCurrentSize > 0) {
                    canvas.drawText(String.format("%.2f", mCurrentSize) + mUnit, mCenterX, mCenterY + 1.4f * mBaseLength, mTextPaint);
                }
                break;
            case STATE_END:
                canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mPaint);
                if (mFraction <= 0.5) {
                    mTextPaint.setTextSize(mTextSize - mTextSize / 0.2f * mFraction);
                } else {
                    mTextPaint.setTextSize(0);
                }
                if (mCurrentSize > 0) {
                    canvas.drawText(String.format("%.2f", mCurrentSize) + mUnit, mCenterX, mCenterY + 1.4f * mBaseLength, mTextPaint);
                }
                canvas.drawLine(mCenterX - mBaseLength * 2.2f + mBaseLength * 1.2f * mFraction, mCenterY,
                        mCenterX - mBaseLength * 0.5f, mCenterY + mBaseLength * 0.5f * mFraction * 1.3f, mPaint);
                canvas.drawLine(mCenterX - mBaseLength * 0.5f, mCenterY + mBaseLength * 0.5f * mFraction * 1.3f,
                        mCenterX + mBaseLength * 2.2f - mBaseLength * mFraction, mCenterY - mBaseLength * mFraction * 1.3f, mPaint);
                break;
            case STATE_RESET:
                canvas.drawCircle(mCenterX, mCenterY, mCircleRadius, mBgPaint);
                canvas.drawLine(mCenterX - mBaseLength, mCenterY,
                        mCenterX - mBaseLength * 0.5f + mBaseLength * 0.5f * mFraction, mCenterY + mBaseLength * 0.65f + mBaseLength * 0.35f * mFraction, mPaint);
                canvas.drawLine(mCenterX - mBaseLength * 0.5f + mBaseLength * 0.5f * mFraction, mCenterY + mBaseLength * 0.65f + mBaseLength * 0.35f * mFraction,
                        mCenterX + mBaseLength * 1.2f - mBaseLength * 0.2f * mFraction, mCenterY - 1.3f * mBaseLength + 1.3f * mBaseLength * mFraction, mPaint);
                canvas.drawLine(mCenterX - mBaseLength * 0.5f + mBaseLength * 0.5f * mFraction, mCenterY + mBaseLength * 0.65f + mBaseLength * 0.35f * mFraction,
                        mCenterX - mBaseLength * 0.5f + mBaseLength * 0.5f * mFraction,
                        mCenterY + mBaseLength * 0.65f - mBaseLength * 2.25f * mFraction, mPaint);
                break;
        }
    }

    public void reset() {
        mFraction = 0;
        mCurrentState = STATE_PRE;
        mCurrentSize = 0;
        invalidate();
        if (mValueAnimator != null) {
            mValueAnimator.removeAllListeners();
            mValueAnimator.removeAllUpdateListeners();
            if (mValueAnimator.isRunning())
                mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    public void start() {
        if (mValueAnimator != null) {
            mValueAnimator.removeAllListeners();
            mValueAnimator.removeAllUpdateListeners();
            if (mValueAnimator.isRunning())
                mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mCurrentState = STATE_DOWNLOADING;
        mValueAnimator = ValueAnimator.ofFloat(1.f, 100.f);
        mValueAnimator.setDuration(1000);
        mValueAnimator.setInterpolator(new OvershootInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction = valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentState = STATE_DOWNLOADING;
                downloadAnim();
            }
        });
        mValueAnimator.start();
    }

    private void downloadAnim() {
        if (mValueAnimator != null) {
            mValueAnimator.removeAllListeners();
            mValueAnimator.removeAllUpdateListeners();
            if (mValueAnimator.isRunning())
                mValueAnimator.cancel();
            mValueAnimator = null;
        }
        if (mCurrentState != STATE_DOWNLOADING) {
            return;
        }
        mValueAnimator = ValueAnimator.ofFloat(1.f, 100.f);
        mValueAnimator.setDuration(mDownloadTime);
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction = valueAnimator.getAnimatedFraction();
                if (mTotalSize > 0)
                    mCurrentSize = mFraction * mTotalSize;
                invalidate();
            }
        });
        mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCurrentState = STATE_END;
                endAnim();
            }
        });
        mValueAnimator.start();
    }

    private void endAnim() {
        if (mValueAnimator != null) {
            mValueAnimator.removeAllListeners();
            mValueAnimator.removeAllUpdateListeners();
            if (mValueAnimator.isRunning())
                mValueAnimator.cancel();
            mValueAnimator = null;
        }
        mValueAnimator = ValueAnimator.ofFloat(1.f, 100.f);
        mValueAnimator.setDuration(700);
        mValueAnimator.setInterpolator(new OvershootInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mFraction = valueAnimator.getAnimatedFraction();
                invalidate();
            }
        });
        mValueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFraction = 0;
                mCurrentState = STATE_RESET;
            }
        });
        mValueAnimator.start();
    }

    public void setDownloadConfig(int downloadTime, double downloadFileSize, @DownloadUnit String unit) {
        mDownloadTime = downloadTime;
        mTotalSize = downloadFileSize;
        mUnit = unit;
    }

    public int getCurrentState() {
        return mCurrentState;
    }

    @StringDef(value = {DownloadUnit.GB, DownloadUnit.MB, DownloadUnit.KB, DownloadUnit.B})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DownloadUnit {
        String GB = "GB";
        String MB = "MB";
        String KB = "KB";
        String B = "B";
    }

}
