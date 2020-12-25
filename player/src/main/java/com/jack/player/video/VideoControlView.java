package com.jack.player.video;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jack.player.R;
import com.jack.player.listener.LockClickListener;
import com.jack.player.listener.StateUiListener;
import com.jack.player.listener.VideoProgressListener;
import com.jack.player.utils.CommonUtil;
import com.jack.player.utils.Logger;

import java.io.File;
import java.util.Map;

/**
 * 播放UI的显示、控制层、手势处理等
 *
 * @author jack
 * @since 2020/10/27 16:16
 */
public abstract class VideoControlView extends VideoView implements View.OnClickListener, View.OnTouchListener, SeekBar.OnSeekBarChangeListener {
    //当前播放进度
    protected int mDownPosition;
    //手势调节音量的大小
    protected int mGestureDownVolume;
    //手势偏差值
    protected int mThreshold = 80;
    //跳转过后的进度
    protected int mSeekTimePosition;
    //手动滑动的起始偏移位置
    protected int mSeekEndOffset;
    //退出全屏显示的案件图片
    protected int mShrinkImageRes = -1;
    //全屏显示的案件图片
    protected int mEnlargeImageRes = -1;
    //触摸显示后隐藏的时间
    protected int mDismissControlTime = 2500;
    //触摸的X（视图坐标）
    protected float mDownX;
    //触摸的Y（视图坐标）
    protected float mDownY;
    //移动的Y
    protected float mMoveY;
    //亮度
    protected float mBrightnessData = -1;
    //触摸滑动进度的比例系数
    protected float mSeekRatio = 1;
    //触摸的是否进度条
    protected boolean mTouchingProgressBar = false;
    //是否改变音量
    protected boolean mChangeVolume = false;
    //是否改变播放进度
    protected boolean mChangePosition = false;
    //触摸显示虚拟按键
    protected boolean mShowVKey = false;
    //是否改变亮度
    protected boolean mBrightness = false;
    //是否首次触摸
    protected boolean mFirstTouch = false;
    //是否隐藏虚拟按键
    protected boolean mHideKey = true;
    //是否需要显示流量提示
    protected boolean mNeedShowWifiTip = true;
    //是否支持非全屏滑动触摸有效
    protected boolean mIsTouchWiget = true;
    //是否支持全屏滑动触摸有效
    protected boolean mIsTouchWigetFull = true;
    //是否点击封面播放
    protected boolean mThumbPlay;
    //锁定屏幕点击
    protected boolean mLockCurScreen;
    //是否需要锁定屏幕功能
    protected boolean mNeedLockFull = true;
    //lazy的setup
    protected boolean mSetUpLazy = false;
    //是否在触摸进度条
    protected boolean mHadSeekTouch = false;
    //是否延迟一会设置进度和时间
    protected boolean mPostProgress = false;
    //是否延迟一会隐藏控制按键
    protected boolean mPostDismiss = false;
    //拖动进度条的时候是否更新mCurrentTimeTextView
    protected boolean isShowDragProgressTextOnSeekBar = false;
    //播放按键
    protected View mStartButton;
    //loading view
    protected View mLoadingProgressBar;
    //进度条
    protected SeekBar mProgressBar;
    //全屏按键
    protected ImageView mFullscreenButton;
    //返回按键
    protected ImageView mBackButton;
    //锁定图标
    protected ImageView mLockScreen;
    //时间显示
    protected TextView mCurrentTimeTextView, mTotalTimeTextView;
    //title
    protected TextView mTitleTextView;
    //顶部:back+title
    protected ViewGroup mTopContainer;
    //底部:current+SeekBar+total+fullscreen
    protected ViewGroup mBottomContainer;
    //封面（外部设置添加进来的）
    protected View mThumbImageView;
    //封面父布局(可以往里添加一个ImageView作为视频海报)
    protected RelativeLayout mThumbImageViewLayout;
    //底部进度条（最底部）
    protected ProgressBar mBottomProgressBar;
    //点击锁屏的回调
    protected LockClickListener mLockClickListener;
    //播放状态的监听
    protected StateUiListener mStateUiListener;
    //进度监听
    protected VideoProgressListener mVideoProgressListener;
    //手势识别
    protected GestureDetector gestureDetector = new GestureDetector(getContext().getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            touchDoubleUp();
            return super.onDoubleTap(e);
        }

        /**
         * 单机
         */
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            //不改变音量，不改变进度，不改变亮度
            if (!mChangePosition && !mChangeVolume && !mBrightness) {
                //触摸显示和隐藏控制UI逻辑
                onClickUiToggle();
            }
            return super.onSingleTapConfirmed(e);
        }

        /**
         * 长按
         */
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            touchLongPress(e);
        }
    });
    //隐藏控制UI，只有播放画面
    private Runnable dismissControlTask = new Runnable() {
        @Override
        public void run() {
            if (mCurrentState != CURRENT_STATE_NORMAL
                    && mCurrentState != CURRENT_STATE_ERROR
                    && mCurrentState != CURRENT_STATE_AUTO_COMPLETE) {
                if (getActivityContext() != null) {
                    hideAllWidget();
                    setViewShowState(mLockScreen, GONE);
                    if (mHideKey && mIfCurrentIsFullscreen && mShowVKey) {
                        CommonUtil.hideNavKey(mContext);
                    }
                }
                if (mPostDismiss) {
                    postDelayed(this, mDismissControlTime);
                }
            }
        }
    };
    //设置进度和播放时间等 1s设置一次
    private Runnable progressTask = new Runnable() {
        @Override
        public void run() {
//            if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE) {
//                setTextAndProgress(0);
//            }
            if (mPostProgress) {
                postDelayed(this, 1000);
            }
        }
    };

    public VideoControlView(@NonNull Context context) {
        super(context);
    }

    public VideoControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoControlView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public VideoControlView(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void init(Context context) {
        super.init(context);
        mStartButton = findViewById(R.id.start);
        mTitleTextView = findViewById(R.id.title);
        mBackButton = findViewById(R.id.back);
        mFullscreenButton = findViewById(R.id.fullscreen);
        mProgressBar = findViewById(R.id.progress);
        mCurrentTimeTextView = findViewById(R.id.current);
        mTotalTimeTextView = findViewById(R.id.total);
        mBottomContainer = findViewById(R.id.layout_bottom);
        mTopContainer = findViewById(R.id.layout_top);
        mBottomProgressBar = findViewById(R.id.bottom_progressbar);
        mThumbImageViewLayout = findViewById(R.id.thumb);
        mLockScreen = findViewById(R.id.lock_screen);
        mLoadingProgressBar = findViewById(R.id.loading);

        if (mStartButton != null) {
            mStartButton.setOnClickListener(this);
        }
        if (mFullscreenButton != null) {
            mFullscreenButton.setOnClickListener(this);
            mFullscreenButton.setOnTouchListener(this);
        }
        if (mProgressBar != null) {
            mProgressBar.setOnSeekBarChangeListener(this);
        }
        if (mBottomContainer != null) {
            mBottomContainer.setOnClickListener(this);
        }
        if (mRenderViewContainer != null) {
            mRenderViewContainer.setOnClickListener(this);
            mRenderViewContainer.setOnTouchListener(this);
        }
        if (mProgressBar != null) {
            mProgressBar.setOnTouchListener(this);
        }
        if (mThumbImageViewLayout != null) {
            mThumbImageViewLayout.setVisibility(GONE);
            mThumbImageViewLayout.setOnClickListener(this);
        }
        if (mThumbImageView != null && !mIfCurrentIsFullscreen && mThumbImageViewLayout != null) {
            addThumbImage(mThumbImageView);
        }
        if (mBackButton != null)
            mBackButton.setOnClickListener(this);
        if (mLockScreen != null) {
            mLockScreen.setVisibility(GONE);
            mLockScreen.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //播放结束或出错直接当成无效点击
                    if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE ||
                            mCurrentState == CURRENT_STATE_ERROR) {
                        return;
                    }
                    lockTouchLogic();
                    if (mLockClickListener != null) {
                        mLockClickListener.onClick(v, mLockCurScreen);
                    }
                }
            });
        }
        if (getActivityContext() != null) {
            mSeekEndOffset = CommonUtil.dip2px(getActivityContext(), 50);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Logger.d(VideoControlView.this.hashCode() + "------------------------------ dismiss onDetachedFromWindow");
        //避免内存泄露
        cancelProgressTimer();
        cancelDismissControlViewTimer();
    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        //设置成解锁状态，然后隐藏
        if (mLockCurScreen) {
            lockTouchLogic();
            mLockScreen.setVisibility(GONE);
        }
    }

    @Override
    public void onError(int what, int extra) {
        super.onError(what, extra);
        //设置成解锁状态，然后隐藏
        if (mLockCurScreen) {
            lockTouchLogic();
            mLockScreen.setVisibility(GONE);
        }
    }

    /**
     * 设置播放显示状态
     */
    @Override
    protected void setStateAndUi(int state) {
        mCurrentState = state;
        if ((state == CURRENT_STATE_NORMAL && isCurrentMediaListener())
                || state == CURRENT_STATE_AUTO_COMPLETE || state == CURRENT_STATE_ERROR) {
            //资源还未load
            mHadPrepared = false;
        }
        switch (mCurrentState) {
            case CURRENT_STATE_NORMAL:
                if (isCurrentMediaListener()) {
                    Logger.d(VideoControlView.this.hashCode() + "------------------------------ dismiss CURRENT_STATE_NORMAL");
                    cancelProgressTimer();
                    getVideoManager().releaseMediaPlayer();
                    mBufferPoint = 0;
                    mSaveChangeViewTime = 0;
                    if (mAudioManager != null) {
                        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
                    }
                }
                releaseNetWorkState();
                break;
            case CURRENT_STATE_PREPAREING:
                resetProgressAndTime();
                break;
            case CURRENT_STATE_PLAYING:
                if (isCurrentMediaListener()) {
                    Logger.d(VideoControlView.this.hashCode() + "------------------------------ CURRENT_STATE_PLAYING");
                    startProgressTimer();
                }
                break;
            case CURRENT_STATE_PAUSE:
                Logger.d(VideoControlView.this.hashCode() + "------------------------------ CURRENT_STATE_PAUSE");
                startProgressTimer();
                break;
            case CURRENT_STATE_ERROR:
                if (isCurrentMediaListener()) {
                    getVideoManager().releaseMediaPlayer();
                }
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                Logger.d(VideoControlView.this.hashCode() + "------------------------------ dismiss CURRENT_STATE_AUTO_COMPLETE");
                cancelProgressTimer();
                if (mProgressBar != null) {
                    mProgressBar.setProgress(100);
                }
                if (mCurrentTimeTextView != null && mTotalTimeTextView != null) {
                    mCurrentTimeTextView.setText(mTotalTimeTextView.getText());
                }
                if (mBottomProgressBar != null) {
                    mBottomProgressBar.setProgress(100);
                }
                break;
            default:
                break;
        }
        resolveUIState(state);
        if (mStateUiListener != null) {
            mStateUiListener.onStateChanged(state);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        //全屏首先隐藏虚拟按键
        if (mHideKey && mIfCurrentIsFullscreen) {
            CommonUtil.hideNavKey(mContext);
        }
        if (i == R.id.start) {
            clickStartIcon();
        } else if (i == R.id.surface_container && mCurrentState == CURRENT_STATE_ERROR) {
            if (mVideoAllCallBack != null) {
                Logger.d("onClickStartError");
                mVideoAllCallBack.onClickStartError(mOriginUrl, mTitle, this);
            }
            prepareVideo();
        } else if (i == R.id.thumb) {
            if (!mThumbPlay) {
                return;
            }
            if (TextUtils.isEmpty(mUrl)) {
                Logger.e("********" + getResources().getString(R.string.no_url));
                //Toast.makeText(getActivityContext(), getResources().getString(R.string.no_url), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mCurrentState == CURRENT_STATE_NORMAL) {
                if (isShowNetConfirm()) {
                    showWifiDialog();
                    return;
                }
                startPlayLogic();
            } else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
                onClickUiToggle();
            }
        } else if (i == R.id.surface_container) {
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                if (mIfCurrentIsFullscreen) {
                    Logger.d("onClickBlankFullscreen");
                    mVideoAllCallBack.onClickBlankFullscreen(mOriginUrl, mTitle, VideoControlView.this);
                } else {
                    Logger.d("onClickBlank");
                    mVideoAllCallBack.onClickBlank(mOriginUrl, mTitle, VideoControlView.this);
                }
            }
            startDismissControlViewTimer();
        } else if (i == R.id.fullscreen) {
            //只需要处理切到横屏
            clickFullscreen();
        }
    }

    /**
     * 亮度、进度、音频进度
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        //获取点击事件距离控件左边的距离
        float x = event.getX();
        //获取点击事件距离控件顶边的距离
        float y = event.getY();
        //锁屏状态全部拦截
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            onClickUiToggle();
            startDismissControlViewTimer();
            return true;
        }
        //关于fullscreen直接只执行onClick事件
        if (id == R.id.fullscreen) {
            return false;
        }
        //视频渲染窗口
        if (id == R.id.surface_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    touchSurfaceDown(x, y);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = x - mDownX;
                    float deltaY = y - mDownY;
                    float absDeltaX = Math.abs(deltaX);
                    float absDeltaY = Math.abs(deltaY);
                    //横竖屏都支持触摸调节音量、亮度、进度
                    if ((mIfCurrentIsFullscreen && mIsTouchWigetFull) || (mIsTouchWiget && !mIfCurrentIsFullscreen)) {
                        if (!mChangePosition && !mChangeVolume && !mBrightness) {
                            touchSurfaceMoveFullLogic(absDeltaX, absDeltaY);
                        }
                    }
                    touchSurfaceMove(deltaX, deltaY, y);
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    //设置进度和时间和回调
                    touchSurfaceUp();
                    Logger.d(VideoControlView.this.hashCode() + "------------------------------ surface_container ACTION_UP");
                    startProgressTimer();
                    //不要和隐藏虚拟按键后，滑出虚拟按键冲突
                    if (mHideKey && mShowVKey) {
                        return true;
                    }
                    break;
                default:
                    break;
            }
            gestureDetector.onTouchEvent(event);
        }
        //进度条seekbar
        else if (id == R.id.progress) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    cancelDismissControlViewTimer();
                case MotionEvent.ACTION_MOVE:
                    cancelProgressTimer();
                    ViewParent vpdown = getParent();
                    while (vpdown != null) {
                        //告诉父组件不要拦截事件
                        vpdown.requestDisallowInterceptTouchEvent(true);
                        vpdown = vpdown.getParent();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    startDismissControlViewTimer();
                    Logger.d(VideoControlView.this.hashCode() + "------------------------------ progress ACTION_UP");
                    startProgressTimer();
                    ViewParent vpup = getParent();
                    while (vpup != null) {
                        vpup.requestDisallowInterceptTouchEvent(false);
                        vpup = vpup.getParent();
                    }
                    mBrightnessData = -1f;
                    break;
            }
        }
        return false;
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param title         title
     */
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, String title) {
        return setUp(url, cacheWithPlay, null, title);
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param title         title
     */
    @Override
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, String title) {
        if (super.setUp(url, cacheWithPlay, cachePath, title)) {
            if (title != null && mTitleTextView != null) {
                mTitleTextView.setText(title);
            }
            if (mIfCurrentIsFullscreen) {
                if (mFullscreenButton != null)
                    mFullscreenButton.setImageResource(getShrinkImageRes());
            } else {
                if (mFullscreenButton != null)
                    mFullscreenButton.setImageResource(getEnlargeImageRes());
            }
            return true;
        }
        return false;
    }

    //---------------------------------------OnSeekBarChangeListener-----------------------------------
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        showDragProgressTextOnSeekBar(fromUser, progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mHadSeekTouch = true;
    }

    /***
     * 拖动进度条
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mVideoAllCallBack != null && isCurrentMediaListener()) {
            if (isIfCurrentIsFullscreen()) {
                Logger.d("onClickSeekbarFullscreen");
                mVideoAllCallBack.onClickSeekbarFullscreen(mOriginUrl, mTitle, this);
            } else {
                Logger.d("onClickSeekbar");
                mVideoAllCallBack.onClickSeekbar(mOriginUrl, mTitle, this);
            }
        }
        if (getVideoManager() != null && mHadPlay) {
            try {
                int time = seekBar.getProgress() * getDuration() / 100;
                getVideoManager().seekTo(time);
            } catch (Exception e) {
                Logger.w(e.toString());
            }
        }
        mHadSeekTouch = false;
    }

    //---------------------------------------OnSeekBarChangeListener-----------------------------------


    @Override
    public void onPrepared() {
        setTextAndProgress(0, true);
        super.onPrepared();
        if (mCurrentState != CURRENT_STATE_PREPAREING) return;
        startProgressTimer();
        Logger.d(VideoControlView.this.hashCode() + "------------------------------ surface_container onPrepared");
    }

    @Override
    public void onBufferingUpdate(final int percent) {
        post(new Runnable() {
            @Override
            public void run() {
                if (mCurrentState != CURRENT_STATE_NORMAL && mCurrentState != CURRENT_STATE_PREPAREING) {
                    if (percent != 0) {
                        setTextAndProgress(percent);
                        mBufferPoint = percent;
                        // TODO: 2020/12/21 可以通过网速做一个切换码率的建议提示
                        Logger.d("Net speed: " + getNetSpeedText() + " percent " + percent);
                    }
                    if (mProgressBar == null) {
                        return;
                    }
                    //循环清除进度
                    if (mLooping && mHadPlay && percent == 0 && mProgressBar.getProgress() >= (mProgressBar.getMax() - 1)) {
                        loopSetProgressAndTime();
                    }
                }
            }
        });
    }

    /**
     * 增对列表优化，在播放前的时候才进行setup
     */
    @Override
    protected void prepareVideo() {
        if (mSetUpLazy) {
            super.setUp(mOriginUrl,
                    mCache,
                    mCachePath,
                    mMapHeadData,
                    mTitle);
        }
        super.prepareVideo();
    }

    /**
     * surface_container按下
     */
    protected void touchSurfaceDown(float x, float y) {
        mTouchingProgressBar = true;
        mDownX = x;
        mDownY = y;
        mMoveY = 0;
        mChangeVolume = false;
        mBrightness = false;
        mChangePosition = false;
        mShowVKey = false;
        mFirstTouch = true;
    }

    /**
     * 设置时间mCurrentTimeTextView
     */
    protected void showDragProgressTextOnSeekBar(boolean fromUser, int progress) {
        if (fromUser && isShowDragProgressTextOnSeekBar) {
            int duration = getDuration();
            if (mCurrentTimeTextView != null)
                mCurrentTimeTextView.setText(CommonUtil.stringForTime(progress * duration / 100));
        }
    }

    /**
     * 计算进度、音量、亮度的值
     */
    protected void touchSurfaceMove(float deltaX, float deltaY, float y) {
        if (Math.abs(deltaY) < mThreshold && Math.abs(deltaX) < mThreshold)
            return;
        int curWidth = 0;
        int curHeight = 0;
        if (getActivityContext() != null) {
            curWidth = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenHeight : mScreenWidth;
            curHeight = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenWidth : mScreenHeight;
        }
        if (mChangePosition) {
            int totalTimeDuration = getDuration();
            mSeekTimePosition = (int) (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio);
            if (mSeekTimePosition > totalTimeDuration)
                mSeekTimePosition = totalTimeDuration;
            if (mSeekTimePosition < 0)
                mSeekTimePosition = 0;
            String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
            String totalTime = CommonUtil.stringForTime(totalTimeDuration);
            //显示中间进度Dialog
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        } else if (mChangeVolume) {
            //y轴
            deltaY = -deltaY;
            //当前手机最大音量
            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int deltaV = (int) (max * deltaY * 3 / curHeight);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);
            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 3 * 100 / curHeight);
            //显示音量Dialog
            showVolumeDialog(-deltaY, volumePercent);
        } else if (mBrightness) {
            float percent = (-deltaY / curHeight);
            onBrightnessSlide(percent);
            mDownY = y;
        }
    }

    /**
     * 根据触摸移动方向分辨是调节进度还是音量和亮度
     */
    protected void touchSurfaceMoveFullLogic(float absDeltaX, float absDeltaY) {
        //音量和亮度调节各占宽度的一半调节范围
        int curWidth = 0;
        if (getActivityContext() != null) {
            curWidth = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenHeight : mScreenWidth;
        }
        //移动最小像素80，否则认为误触
        if (absDeltaX > mThreshold || absDeltaY > mThreshold) {
            cancelProgressTimer();
            //移动的角度接近于水平，则认为调整播放器进度
            if (Math.atan(Math.abs(absDeltaY / absDeltaX)) < Math.PI / 4) {
                //防止全屏虚拟按键
                int screenWidth = CommonUtil.getScreenWidth(getContext());
                if (Math.abs(screenWidth - mDownX) > mSeekEndOffset) {
                    mChangePosition = true;
                    mDownPosition = getCurrentPositionWhenPlaying();
                } else {
                    mShowVKey = true;
                }
            } else {
                int screenHeight = CommonUtil.getScreenHeight(getContext());
                boolean noEnd = Math.abs(screenHeight - mDownY) > mSeekEndOffset;
                if (mFirstTouch) {
                    mBrightness = (mDownX < curWidth * 0.5f) && noEnd;
                    mFirstTouch = false;
                }
                if (!mBrightness) {
                    mChangeVolume = noEnd;
                    //取得当前手机的音量
                    mGestureDownVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
                mShowVKey = !noEnd;
            }
        }
    }

    /**
     * 设置进度和时间
     */
    protected void touchSurfaceUp() {
        if (mChangePosition) {
            int duration = getDuration();
            int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
            if (progress < 0)
                progress = 0;
            if (mBottomProgressBar != null)
                mBottomProgressBar.setProgress(progress);
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
        }
        mTouchingProgressBar = false;
        dismissProgressDialog();
        dismissVolumeDialog();
        dismissBrightnessDialog();
        if (mChangePosition && getVideoManager() != null && (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE)) {
            try {
                getVideoManager().seekTo(mSeekTimePosition);
            } catch (Exception e) {
                e.printStackTrace();
            }
            int duration = getDuration();
            int progress = mSeekTimePosition * 100 / (duration == 0 ? 1 : duration);
            if (progress < 0)
                progress = 0;
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                mVideoAllCallBack.onTouchScreenSeekPosition(mOriginUrl, mTitle, this);
            }
        } else if (mBrightness) {
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                Logger.d("onTouchScreenSeekLight");
                mVideoAllCallBack.onTouchScreenSeekLight(mOriginUrl, mTitle, this);
            }
        } else if (mChangeVolume) {
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                Logger.d("onTouchScreenSeekVolume");
                mVideoAllCallBack.onTouchScreenSeekVolume(mOriginUrl, mTitle, this);
            }
        }
    }

    /**
     * 双击暂停/播放
     * 如果不需要，重载为空方法即可
     */
    protected void touchDoubleUp() {
        //还未播放过，直接不处理
        if (!mHadPlay) {
            return;
        }
        //和点击播放暂停按钮一样处理
        clickStartIcon();
    }

    /**
     * 长按
     */
    protected void touchLongPress(MotionEvent e) {
    }

    /**
     * 处理播放各状态改变下控制UI的显示
     */
    protected void resolveUIState(int state) {
        switch (state) {
            case CURRENT_STATE_NORMAL:
                changeUiToNormal();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_PREPAREING:
                changeUiToPreparingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING:
                changeUiToPlayingShow();
                startDismissControlViewTimer();
                break;
            case CURRENT_STATE_PAUSE:
                changeUiToPauseShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_ERROR:
                changeUiToError();
                break;
            case CURRENT_STATE_AUTO_COMPLETE:
                changeUiToCompleteShow();
                cancelDismissControlViewTimer();
                break;
            case CURRENT_STATE_PLAYING_BUFFERING_START:
                changeUiToPlayingBufferingShow();
                break;
            default:
                break;
        }
    }

    /**
     * 播放按键点击
     */
    private void clickStartIcon() {
        if (TextUtils.isEmpty(mUrl)) {
            Logger.e("********" + getResources().getString(R.string.no_url));
            return;
        }
        //还未准备或出错，需要重新初始化播放器，加载媒体资源
        if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_ERROR) {
            if (isShowNetConfirm()) {
                //展示正在使用移动网络流量提醒
                showWifiDialog();
                return;
            }
            //初始化播放
            startButtonLogic();
        }
        //播放中
        else if (mCurrentState == CURRENT_STATE_PLAYING) {
            try {
                //暂停播放
                onVideoPause();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setStateAndUi(CURRENT_STATE_PAUSE);
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                if (mIfCurrentIsFullscreen) {
                    Logger.d("onClickStopFullscreen");
                    mVideoAllCallBack.onClickStopFullscreen(mOriginUrl, mTitle, this);
                } else {
                    Logger.d("onClickStop");
                    mVideoAllCallBack.onClickStop(mOriginUrl, mTitle, this);
                }
            }
        }
        //暂停中
        else if (mCurrentState == CURRENT_STATE_PAUSE) {
            if (mVideoAllCallBack != null && isCurrentMediaListener()) {
                if (mIfCurrentIsFullscreen) {
                    Logger.d("onClickResumeFullscreen");
                    mVideoAllCallBack.onClickResumeFullscreen(mOriginUrl, mTitle, this);
                } else {
                    Logger.d("onClickResume");
                    mVideoAllCallBack.onClickResume(mOriginUrl, mTitle, this);
                }
            }
            //资源加载还未完毕，当成加载完之后自动播放处理
            if (!mHadPlay && !mStartAfterPrepared) {
                startAfterPrepared();
            }
            try {
                getVideoManager().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
            setStateAndUi(CURRENT_STATE_PLAYING);
        }
        //播放完成，重新播放
        else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            startButtonLogic();
        }
    }

    /**
     * 处理锁屏屏幕触摸逻辑
     */
    protected void lockTouchLogic() {
        if (mLockCurScreen) {
            mLockScreen.setImageResource(R.drawable.unlock);
            mLockCurScreen = false;
        } else {
            mLockScreen.setImageResource(R.drawable.lock);
            mLockCurScreen = true;
            hideAllWidget();
        }
    }

    /**
     * 开启进度和时间设置的定时器
     */
    protected void startProgressTimer() {
        cancelProgressTimer();
        mPostProgress = true;
        post(progressTask);
    }

    /**
     * 关闭进度和时间设置的定时器
     */
    protected void cancelProgressTimer() {
        mPostProgress = false;
        removeCallbacks(progressTask);
    }

    /**
     * 设置缓冲进度
     */
    protected void setTextAndProgress(int secProgress) {
        setTextAndProgress(secProgress, false);
    }

    /**
     * 设置进度和时间
     *
     * @param secProgress 缓冲进度
     * @param forceChange 是否强制刷新
     */
    protected void setTextAndProgress(int secProgress, boolean forceChange) {
        int position = getCurrentPositionWhenPlaying();
        int duration = getDuration();
        int progress = position * 100 / (duration == 0 ? 1 : duration);
        if (progress < 0) {
            progress = 0;
        }
        setProgressAndTime(progress, secProgress, position, duration, forceChange);
    }

    protected void setProgressAndTime(int progress, int secProgress, int currentTime, int totalTime, boolean forceChange) {
        if (mVideoProgressListener != null && mCurrentState == CURRENT_STATE_PLAYING) {
            mVideoProgressListener.onProgress(progress, secProgress, currentTime, totalTime);
        }
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        if (mHadSeekTouch) {
            return;
        }
        if (!mTouchingProgressBar) {
            if (progress != 0 || forceChange) mProgressBar.setProgress(progress);
        }
        if (getVideoManager().getBufferedPercentage() > 0) {
            secProgress = getVideoManager().getBufferedPercentage();
        }
        if (secProgress > 94) secProgress = 100;
        setSecondaryProgress(secProgress);
        mTotalTimeTextView.setText(CommonUtil.stringForTime(totalTime));
        if (currentTime > 0)
            mCurrentTimeTextView.setText(CommonUtil.stringForTime(currentTime));
        if (mBottomProgressBar != null) {
            if (progress != 0 || forceChange) mBottomProgressBar.setProgress(progress);
            setSecondaryProgress(secProgress);
        }
    }

    /**
     * 设置缓冲进度
     */
    protected void setSecondaryProgress(int secProgress) {
        if (mProgressBar != null) {
            if (secProgress != 0) {
                mProgressBar.setSecondaryProgress(secProgress);
            }
        }
        if (mBottomProgressBar != null) {
            if (secProgress != 0) {
                mBottomProgressBar.setSecondaryProgress(secProgress);
            }
        }
    }

    /**
     * 重置进度和时间
     */
    private void resetProgressAndTime() {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        mProgressBar.setProgress(0);
        mProgressBar.setSecondaryProgress(0);
        mCurrentTimeTextView.setText(CommonUtil.stringForTime(0));
        mTotalTimeTextView.setText(CommonUtil.stringForTime(0));

        if (mBottomProgressBar != null) {
            mBottomProgressBar.setProgress(0);
            mBottomProgressBar.setSecondaryProgress(0);
        }
    }

    /**
     * 循环播放重新设置进度和时间
     */
    protected void loopSetProgressAndTime() {
        if (mProgressBar == null || mTotalTimeTextView == null || mCurrentTimeTextView == null) {
            return;
        }
        mProgressBar.setProgress(0);
        mProgressBar.setSecondaryProgress(0);
        mCurrentTimeTextView.setText(CommonUtil.stringForTime(0));
        if (mBottomProgressBar != null)
            mBottomProgressBar.setProgress(0);
    }

    /**
     * 开启关闭控件UI的任务
     */
    protected void startDismissControlViewTimer() {
        cancelDismissControlViewTimer();
        mPostDismiss = true;
        postDelayed(dismissControlTask, mDismissControlTime);
    }

    /**
     * 关闭隐藏控制UI，只有播放画面 定时器
     */
    protected void cancelDismissControlViewTimer() {
        mPostDismiss = false;
        removeCallbacks(dismissControlTask);
    }

    /**
     * 添加视频显示封面View
     */
    protected void addThumbImage(View thumb) {
        if (mThumbImageViewLayout != null) {
            mThumbImageViewLayout.removeAllViews();
            mThumbImageViewLayout.addView(thumb);
            ViewGroup.LayoutParams layoutParams = thumb.getLayoutParams();
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            thumb.setLayoutParams(layoutParams);
        }
    }

    protected void setViewShowState(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    /**
     * 滑动改变亮度
     */
    protected void onBrightnessSlide(float percent) {
        //当前屏幕亮度
        mBrightnessData = ((Activity) (mContext)).getWindow().getAttributes().screenBrightness;
        if (mBrightnessData <= 0.00f) {
            mBrightnessData = 0.50f;
        } else if (mBrightnessData < 0.01f) {
            mBrightnessData = 0.01f;
        }
        WindowManager.LayoutParams lpa = ((Activity) (mContext)).getWindow().getAttributes();
        lpa.screenBrightness = mBrightnessData + percent;
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f;
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f;
        }
        showBrightnessDialog(lpa.screenBrightness);
        ((Activity) (mContext)).getWindow().setAttributes(lpa);
    }

    /**
     * 是否需要显示使用流量提示
     */
    private boolean isShowNetConfirm() {
        return !mOriginUrl.startsWith("file") && !mOriginUrl.startsWith("android.resource") && !CommonUtil.isWifiConnected(getContext())
                && mNeedShowWifiTip && !getVideoManager().cachePreview(mContext.getApplicationContext(), mCachePath, mOriginUrl);
    }

    //-------------------------------继承之后可自定义ui与显示隐藏 ---------------------------------------

    /**
     * 显示使用移动流量的Dialog
     */
    protected abstract void showWifiDialog();

    /**
     * 显示中间进度Dialog
     *
     * @param deltaX            滑动方向
     * @param seekTime          当前时间
     * @param seekTimePosition  当前位置
     * @param totalTime         总时间
     * @param totalTimeDuration 总位置
     */
    protected abstract void showProgressDialog(float deltaX,
                                               String seekTime, int seekTimePosition,
                                               String totalTime, int totalTimeDuration);

    /**
     * 关闭进度Dialog
     */
    protected abstract void dismissProgressDialog();

    /**
     * 显示音量Dialog
     *
     * @param deltaY        滑动距离
     * @param volumePercent 音量百分比
     */
    protected abstract void showVolumeDialog(float deltaY, int volumePercent);

    /**
     * 关闭音量Dialog
     */
    protected abstract void dismissVolumeDialog();

    /**
     * 显示亮度Dialog
     *
     * @param percent 亮度百分比
     */
    protected abstract void showBrightnessDialog(float percent);

    /**
     * 关闭亮度Dialog
     */
    protected abstract void dismissBrightnessDialog();

    /**
     * 点击触摸显示和隐藏逻辑
     */
    protected abstract void onClickUiToggle();

    /**
     * 隐藏所有的控制UI
     */
    protected abstract void hideAllWidget();

    /**
     * CURRENT_STATE_NORMAL状态下的UI
     */
    protected abstract void changeUiToNormal();

    /**
     * CURRENT_STATE_PREPAREING
     */
    protected abstract void changeUiToPreparingShow();

    /**
     * CURRENT_STATE_PLAYING
     */
    protected abstract void changeUiToPlayingShow();

    /**
     * CURRENT_STATE_PAUSE
     */
    protected abstract void changeUiToPauseShow();

    /**
     * CURRENT_STATE_ERROR
     */
    protected abstract void changeUiToError();

    /**
     * CURRENT_STATE_AUTO_COMPLETE
     */
    protected abstract void changeUiToCompleteShow();

    /**
     * CURRENT_STATE_PLAYING_BUFFERING_START
     */
    protected abstract void changeUiToPlayingBufferingShow();

    /**
     * 竖屏点击了切换到横屏的按钮操作（只需要处理切到横屏）
     */
    protected abstract void clickFullscreen();
    //-------------------------------继承之后可自定义ui与显示隐藏 ---------------------------------------


    //-------------------------------开放接口--------------------------------------------

    /**
     * 在点击播放的时候才进行真正setup
     */
    public boolean setUpLazy(String url, boolean cacheWithPlay, File cachePath, Map<String, String> mapHeadData, String title) {
        mOriginUrl = url;
        mCache = cacheWithPlay;
        mCachePath = cachePath;
        mSetUpLazy = true;
        mTitle = title;
        mMapHeadData = mapHeadData;
        if (isCurrentMediaListener() &&
                (System.currentTimeMillis() - mSaveChangeViewTime) < CHANGE_DELAY_TIME)
            return false;
        mUrl = "waiting";
        mCurrentState = CURRENT_STATE_NORMAL;
        return true;
    }

    /**
     * 初始化为正常状态
     */
    public void initUIState() {
        setStateAndUi(CURRENT_STATE_NORMAL);
    }

    /**
     * 封面布局
     */
    public RelativeLayout getThumbImageViewLayout() {
        return mThumbImageViewLayout;
    }

    /***
     * 清除封面
     */
    public void clearThumbImageView() {
        if (mThumbImageViewLayout != null) {
            mThumbImageViewLayout.removeAllViews();
        }
    }

    public View getThumbImageView() {
        return mThumbImageView;
    }

    /***
     * 设置封面
     */
    public void setThumbImageView(View view) {
        if (mThumbImageViewLayout != null) {
            mThumbImageView = view;
            addThumbImage(view);
        }
    }

    /**
     * title
     */
    public TextView getTitleTextView() {
        return mTitleTextView;
    }


    /**
     * 获取播放按键
     */
    public View getStartButton() {
        return mStartButton;
    }

    /**
     * 获取全屏按键
     */
    public ImageView getFullscreenButton() {
        return mFullscreenButton;
    }

    /**
     * 获取返回按键
     */
    public ImageView getBackButton() {
        return mBackButton;
    }

    /**
     * 获取切换全屏展示ImageRes，可重写
     */
    public int getEnlargeImageRes() {
        if (mEnlargeImageRes == -1) {
            return R.drawable.video_enlarge;
        }
        return mEnlargeImageRes;
    }

    /**
     * 设置右下角 显示切换到全屏 的按键资源
     * 必须在setUp之前设置
     * 不设置使用默认
     */
    public void setEnlargeImageRes(int mEnlargeImageRes) {
        this.mEnlargeImageRes = mEnlargeImageRes;
    }

    /**
     * 获取切换竖屏展示ImageRes，可重写
     */
    public int getShrinkImageRes() {
        if (mShrinkImageRes == -1) {
            return R.drawable.video_shrink;
        }
        return mShrinkImageRes;
    }

    /**
     * 设置右下角 显示退出全屏 的按键资源
     * 必须在setUp之前设置
     * 不设置使用默认
     */
    public void setShrinkImageRes(int mShrinkImageRes) {
        this.mShrinkImageRes = mShrinkImageRes;
    }

    /**
     * 是否可以全屏滑动界面改变进度，声音等
     * 默认 true
     */
    public void setIsTouchWigetFull(boolean isTouchWigetFull) {
        this.mIsTouchWigetFull = isTouchWigetFull;
    }

    /**
     * 是否点击封面可以播放
     */
    public void setThumbPlay(boolean thumbPlay) {
        this.mThumbPlay = thumbPlay;
    }

    /**
     * 是否隐藏虚拟按键
     */
    public boolean isHideKey() {
        return mHideKey;
    }

    /**
     * 全屏隐藏虚拟按键，默认打开
     */
    public void setHideKey(boolean hideKey) {
        this.mHideKey = hideKey;
    }

    public boolean isNeedShowWifiTip() {
        return mNeedShowWifiTip;
    }

    /**
     * 是否需要显示流量提示,默认true
     */
    public void setNeedShowWifiTip(boolean needShowWifiTip) {
        this.mNeedShowWifiTip = needShowWifiTip;
    }

    public boolean isTouchWiget() {
        return mIsTouchWiget;
    }

    /**
     * 是否可以滑动界面改变进度，声音等
     * 默认true
     */
    public void setIsTouchWiget(boolean isTouchWiget) {
        this.mIsTouchWiget = isTouchWiget;
    }

    public boolean isTouchWigetFull() {
        return mIsTouchWigetFull;
    }

    public float getSeekRatio() {
        return mSeekRatio;
    }

    /**
     * 调整触摸滑动快进的比例
     *
     * @param seekRatio 滑动快进的比例，默认1。数值越大，滑动的产生的seek越小
     */
    public void setSeekRatio(float seekRatio) {
        if (seekRatio < 0) {
            return;
        }
        this.mSeekRatio = seekRatio;
    }

    public boolean isNeedLockFull() {
        return mNeedLockFull;
    }

    /**
     * 是否需要全屏锁定屏幕功能
     * 如果单独使用请设置setIfCurrentIsFullscreen为true
     */
    public void setNeedLockFull(boolean needLoadFull) {
        this.mNeedLockFull = needLoadFull;
    }

    /**
     * 锁屏点击
     */
    public void setLockClickListener(LockClickListener lockClickListener) {
        this.mLockClickListener = lockClickListener;
    }

    public int getDismissControlTime() {
        return mDismissControlTime;
    }

    /**
     * 设置触摸显示控制ui的消失时间
     *
     * @param dismissControlTime 毫秒，默认2500
     */
    public void setDismissControlTime(int dismissControlTime) {
        this.mDismissControlTime = dismissControlTime;
    }

    /**
     * 进度回调
     */
    public void setVideoProgressListener(VideoProgressListener videoProgressListener) {
        this.mVideoProgressListener = videoProgressListener;
    }

    public boolean isShowDragProgressTextOnSeekBar() {
        return isShowDragProgressTextOnSeekBar;
    }

    /**
     * 拖动进度条时，是否在 seekbar 开始部位显示拖动进度
     * 默认 false
     */
    public void setShowDragProgressTextOnSeekBar(boolean showDragProgressTextOnSeekBar) {
        isShowDragProgressTextOnSeekBar = showDragProgressTextOnSeekBar;
    }

    /***
     * 状态监听
     */
    public StateUiListener getStateUiListener() {
        return mStateUiListener;
    }

    public void setStateUiListener(StateUiListener gsyStateUiListener) {
        this.mStateUiListener = gsyStateUiListener;
    }
    //-------------------------------开放接口--------------------------------------------
}
