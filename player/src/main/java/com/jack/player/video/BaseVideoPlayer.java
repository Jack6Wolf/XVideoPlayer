package com.jack.player.video;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.jack.player.utils.CommonUtil;
import com.jack.player.utils.Logger;
import com.jack.player.utils.OrientationOption;
import com.jack.player.utils.OrientationUtils;

import java.lang.reflect.Constructor;

/**
 * 处理全屏竖屏切换显示处理
 * 其中全屏实现：基于Window.ID_ANDROID_CONTENT给定id添加子View
 *
 * @author jack
 * @since 2020/10/27 16:34
 */
public abstract class BaseVideoPlayer extends VideoControlView {
    //保存系统状态ui
    protected int mSystemUiVisibility;
    //当前item框的屏幕位置
    protected int[] mListItemRect;
    //当前View的长宽
    protected int[] mListItemSize;
    //是否需要在利用window实现全屏幕的时候隐藏actionbar
    protected boolean mActionBar = true;
    //是否需要在利用window实现全屏幕的时候隐藏statusbar
    protected boolean mStatusBar = true;
    //是否使用全屏动画效果
    protected boolean mShowFullAnimation = false;
    //是否自动旋转和
    protected boolean mRotateViewAuto = true;
    //旋转使能后是否跟随系统设置
    protected boolean mRotateWithSystem = true;
    //当前全屏是否锁定全屏
    protected boolean mLockLand = false;
    //是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏，注意，这时候默认旋转无效
    //这个标志为和 mLockLand 冲突，需要和OrientationUtils使用
    protected boolean mAutoFullWithSize = false;
    //是否需要竖屏全屏的时候判断状态栏
    protected boolean isNeedAutoAdaptation = false;
    //全屏动画是否结束了
    protected boolean mFullAnimEnd = true;
    //旋转工具类
    protected OrientationUtils mOrientationUtils;
    //全屏返回监听，如果设置了，默认返回无效
    protected View.OnClickListener mBackFromFullScreenListener;
    //做一些延时任务
    protected Handler mInnerHandler = new Handler();
    //切换到全屏检查当前状态，videoView是否需要切换
    protected Runnable mCheckoutTask = new Runnable() {
        @Override
        public void run() {
            //获取全屏播放器对象
            VideoView videoView = getFullWindowPlayer();
            if (videoView != null && videoView.mCurrentState != mCurrentState) {
                if (videoView.mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START
                        && mCurrentState != CURRENT_STATE_PREPAREING) {
                    videoView.setStateAndUi(mCurrentState);
                }
            }
        }
    };
    //旋转时仅处理横屏
    private boolean mIsOnlyRotateLand = false;

    public BaseVideoPlayer(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public BaseVideoPlayer(Context context) {
        super(context);
    }

    public BaseVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseVideoPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
    }

    //-------------------------MediaPlayerListener------------------------

    /**
     * 退出全屏
     */
    @Override
    public void onBackFullscreen() {
        clearFullscreenLayout();
    }
    //-------------------------MediaPlayerListener------------------------

    /**
     * 处理锁屏屏幕触摸逻辑
     */
    @Override
    protected void lockTouchLogic() {
        super.lockTouchLogic();
        //没锁屏
        if (!mLockCurScreen) {
            if (mOrientationUtils != null)
                mOrientationUtils.setEnable(isRotateViewAuto());
        }
        //锁屏了不旋转
        else {
            if (mOrientationUtils != null)
                mOrientationUtils.setEnable(false);
        }
    }

    @Override
    public void onPrepared() {
        super.onPrepared();
        //确保开启竖屏检测的时候正常全屏
        checkAutoFullSizeWhenFull();
    }

    @Override
    public void onInfo(int what, int extra) {
        super.onInfo(what, extra);
        if (what == getVideoManager().getRotateInfoFlag()) {
            checkAutoFullSizeWhenFull();
        }
    }

    /**
     * 查找ID_ANDROID_CONTENT父组件
     */
    private ViewGroup getViewGroup() {
        if (CommonUtil.scanForActivity(getContext()) == null)
            return null;
        return (ViewGroup) (CommonUtil.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
    }

    /**
     * 移除没用的全屏VideoView
     */
    private void removeVideo(ViewGroup vp, int id) {
        View old = vp.findViewById(id);
        if (old != null) {
            if (old.getParent() != null) {
                ViewGroup viewGroup = (ViewGroup) old.getParent();
                vp.removeView(viewGroup);
            }
        }
    }

    /**
     * 保存大小和状态
     */
    private void saveLocationStatus(Context context, boolean statusBar, boolean actionBar) {
        //一个控件在其整个屏幕上的坐标位置
        getLocationOnScreen(mListItemRect);
        if (context instanceof Activity) {
            int statusBarH = CommonUtil.getStatusBarHeight(context);
            int actionBerH = CommonUtil.getActionBarHeight((Activity) context);
            boolean isTranslucent = false;
            //是否是沉浸式状态栏
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                isTranslucent = ((WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS & ((Activity) context).getWindow().getAttributes().flags)
                        == WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
            Logger.d("*************isTranslucent*************** " + isTranslucent);
            //减去状态栏高度
            if (statusBar && !isTranslucent) {
                mListItemRect[1] = mListItemRect[1] - statusBarH;
            }
            if (actionBar) {
                mListItemRect[1] = mListItemRect[1] - actionBerH;
            }
        }
        mListItemSize[0] = getWidth();
        mListItemSize[1] = getHeight();
    }

    /**
     * 克隆切换参数
     */
    protected void cloneParams(BaseVideoPlayer from, BaseVideoPlayer to) {
        to.mHadPlay = from.mHadPlay;
        to.mPlayTag = from.mPlayTag;
        to.mPlayPosition = from.mPlayPosition;
        to.mNeedShowWifiTip = from.mNeedShowWifiTip;
        to.mShrinkImageRes = from.mShrinkImageRes;
        to.mEnlargeImageRes = from.mEnlargeImageRes;
        to.mRotate = from.mRotate;
        to.mShowPauseCover = from.mShowPauseCover;
        to.mDismissControlTime = from.mDismissControlTime;
        to.mSeekRatio = from.mSeekRatio;
        to.mNetChanged = from.mNetChanged;
        to.mNetSate = from.mNetSate;
        to.mRotateWithSystem = from.mRotateWithSystem;
        to.mBackUpPlayingBufferState = from.mBackUpPlayingBufferState;
        to.mBackFromFullScreenListener = from.mBackFromFullScreenListener;
        to.mVideoProgressListener = from.mVideoProgressListener;
        to.mHadPrepared = from.mHadPrepared;
        to.mStartAfterPrepared = from.mStartAfterPrepared;
        to.mPauseBeforePrepared = from.mPauseBeforePrepared;
        to.mReleaseWhenLossAudio = from.mReleaseWhenLossAudio;
        to.mVideoAllCallBack = from.mVideoAllCallBack;
        to.mActionBar = from.mActionBar;
        to.mStatusBar = from.mStatusBar;
        to.mAutoFullWithSize = from.mAutoFullWithSize;
        to.mOverrideExtension = from.mOverrideExtension;
        if (from.mSetUpLazy) {
            to.setUpLazy(from.mOriginUrl, from.mCache, from.mCachePath, from.mMapHeadData, from.mTitle);
            to.mUrl = from.mUrl;
        } else {
            to.setUp(from.mOriginUrl, from.mCache, from.mCachePath, from.mMapHeadData, from.mTitle);
        }
        to.setLooping(from.isLooping());
        to.setIsTouchWigetFull(from.mIsTouchWigetFull);
        to.setSpeed(from.getSpeed(), from.mSoundTouch);
        to.setStateAndUi(from.mCurrentState);
    }


    /**
     * 设置全屏下的参数，及展示
     */
    protected void resolveFullVideoShow(Context context, final BaseVideoPlayer baseVideoPlayer, final FrameLayout frameLayout) {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) baseVideoPlayer.getLayoutParams();
        lp.setMargins(0, 0, 0, 0);
        lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.CENTER;
        baseVideoPlayer.setLayoutParams(lp);
        baseVideoPlayer.setIfCurrentIsFullscreen(true);
        mOrientationUtils = new OrientationUtils((Activity) context, baseVideoPlayer, getOrientationOption());
        mOrientationUtils.setEnable(isRotateViewAuto());
        mOrientationUtils.setRotateWithSystem(mRotateWithSystem);
        mOrientationUtils.setOnlyRotateLand(mIsOnlyRotateLand);
        baseVideoPlayer.mOrientationUtils = mOrientationUtils;
        final boolean isVertical = isVerticalFullByVideoSize();
        final boolean isLockLand = isLockLandByAutoFullSize();
        if (isShowFullAnimation()) {
            mInnerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //autoFull模式下，非横屏视频视频不横屏，并且不自动旋转
                    if (!isVertical && isLockLand && mOrientationUtils != null && mOrientationUtils.getIsLand() != 1) {
                        mOrientationUtils.resolveOrientationByClick();
                    }
                    baseVideoPlayer.setVisibility(VISIBLE);
                    frameLayout.setVisibility(VISIBLE);
                }
            }, 300);
        } else {
            //autoFull模式下，非横屏视频视频不横屏，并且不自动旋转
            if (!isVertical && isLockLand && mOrientationUtils != null) {
                mOrientationUtils.resolveOrientationByClick();
            }
            baseVideoPlayer.setVisibility(VISIBLE);
            frameLayout.setVisibility(VISIBLE);
        }

        if (mVideoAllCallBack != null) {
            Logger.d("onEnterFullscreen");
            mVideoAllCallBack.onEnterFullscreen(mOriginUrl, mTitle, baseVideoPlayer);
        }
        mIfCurrentIsFullscreen = true;
        checkoutState();
        checkAutoFullWithSizeAndAdaptation(baseVideoPlayer);
    }

    /**
     * 恢复到竖屏状态
     *
     * @param oldF        全屏状态下的VideoView
     * @param vp          ID_ANDROID_CONTENT父组件
     * @param videoPlayer oldF
     */
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, VideoPlayer videoPlayer) {
        //先移除全屏VideoView
        if (oldF != null && oldF.getParent() != null) {
            ViewGroup viewGroup = (ViewGroup) oldF.getParent();
            vp.removeView(viewGroup);
        }
        //全屏videoPlayer里的参数全部复制过来当前View
        if (videoPlayer != null) {
            cloneParams(videoPlayer, this);
        }
        if (mCurrentState == CURRENT_STATE_NORMAL || mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            createNetWorkState();
        }
        getVideoManager().setListener(getVideoManager().lastListener());
        getVideoManager().setLastListener(null);
        setStateAndUi(mCurrentState);
        addTextureView();
        mSaveChangeViewTime = System.currentTimeMillis();
        if (mVideoAllCallBack != null) {
            Logger.d("onQuitFullscreen");
            mVideoAllCallBack.onQuitFullscreen(mOriginUrl, mTitle, this);
        }
        mIfCurrentIsFullscreen = false;
        if (mHideKey) {
            CommonUtil.showNavKey(mContext, mSystemUiVisibility);
        }
        CommonUtil.showSupportActionBar(mContext, mActionBar, mStatusBar);
        if (getFullscreenButton() != null) {
            getFullscreenButton().setImageResource(getEnlargeImageRes());
        }
    }

    /**
     * 退出window层播放全屏效果
     */
    @SuppressWarnings("ResourceType")
    protected void clearFullscreenLayout() {
        if (!mFullAnimEnd) {
            return;
        }
        mIfCurrentIsFullscreen = false;
        int delay = 0;
        if (mOrientationUtils != null) {
            //切换竖屏
            delay = mOrientationUtils.backToPortraitVideo();
            mOrientationUtils.releaseListener();
            mOrientationUtils = null;
        }
        //不使用切换动画
        if (!mShowFullAnimation) {
            delay = 0;
        }

        final ViewGroup vp = getViewGroup();
        final View oldF = vp != null ? vp.findViewById(getFullId()) : null;
        //有全屏
        if (oldF != null) {
            //此处fix bug#265，推出全屏的时候，虚拟按键问题
            VideoPlayer videoPlayer = (VideoPlayer) oldF;
            videoPlayer.mIfCurrentIsFullscreen = false;
        }
        mInnerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                backToNormal();
            }
        }, delay);
    }

    /**
     * 回到正常竖屏效果
     */
    @SuppressWarnings("ResourceType")
    protected void backToNormal() {
        final ViewGroup vp = getViewGroup();
        final View oldF = vp != null ? vp.findViewById(getFullId()) : null;
        final VideoPlayer videoPlayer;
        //有全屏
        if (oldF != null) {
            videoPlayer = (VideoPlayer) oldF;
            //动画
            if (mShowFullAnimation) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(vp);
                }
                LayoutParams lp = (LayoutParams) videoPlayer.getLayoutParams();
                lp.setMargins(mListItemRect[0], mListItemRect[1], 0, 0);
                lp.width = mListItemSize[0];
                lp.height = mListItemSize[1];
                //注意配置回来，不然动画效果会不对
                lp.gravity = Gravity.NO_GRAVITY;
                videoPlayer.setLayoutParams(lp);
                mInnerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolveNormalVideoShow(oldF, vp, videoPlayer);
                    }
                }, 400);
            } else {
                resolveNormalVideoShow(oldF, vp, videoPlayer);
            }
        }
        //没ID_ANDROID_CONTENT全屏View
        else {
            resolveNormalVideoShow(null, vp, null);
        }
    }

    /**
     * 检查状态
     */
    protected void checkoutState() {
        removeCallbacks(mCheckoutTask);
        mInnerHandler.postDelayed(mCheckoutTask, 500);
    }

    /**
     * 是否竖屏模式的全屏
     */
    protected boolean isVerticalVideo() {
        boolean isVertical = false;
        int videoHeight = getCurrentVideoHeight();
        int videoWidth = getCurrentVideoWidth();
        Logger.d("VideoBase isVerticalVideo  videoHeight " + videoHeight + " videoWidth " + videoWidth);
        Logger.d("VideoBase isVerticalVideo  mRotate " + mRotate);
        if (videoHeight > 0 && videoWidth > 0) {
            if (mRotate == 90 || mRotate == 270) {
                isVertical = videoWidth > videoHeight;
            } else {
                isVertical = videoHeight > videoWidth;
            }
        }
        return isVertical;
    }

    /**
     * 是否根据autoFullSize调整lockLand
     */
    protected boolean isLockLandByAutoFullSize() {
        boolean isLockLand = mLockLand;
        if (isAutoFullWithSize()) {
            isLockLand = isVerticalVideo();
        }
        return isLockLand;
    }

    /**
     * 确保开启竖屏检测的时候正常全屏
     */
    protected void checkAutoFullSizeWhenFull() {
        if (mIfCurrentIsFullscreen) {
            //确保开启竖屏检测的时候正常全屏
            boolean isV = isVerticalFullByVideoSize();
            Logger.d("BaseVideoPlayer onPrepared isVerticalFullByVideoSize " + isV);
            if (isV) {
                //返回竖屏
                if (mOrientationUtils != null) {
                    mOrientationUtils.backToPortraitVideo();
                    //处理在未开始播放的时候点击全屏
                    checkAutoFullWithSizeAndAdaptation(this);
                }
            }
        }
    }

    /**
     * 检测是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏；
     * 并且适配在竖屏横屏时，由于刘海屏或者打孔屏占据空间，导致标题显示被遮盖的问题
     *
     * @param baseVideoPlayer 将要显示的播放器对象
     */
    protected void checkAutoFullWithSizeAndAdaptation(final BaseVideoPlayer baseVideoPlayer) {
        if (baseVideoPlayer != null) {
            //判断是否自动选择；判断是否是竖直的视频；判断是否隐藏状态栏
            if (isNeedAutoAdaptation &&
                    isAutoFullWithSize() && isVerticalVideo() && isFullHideStatusBar()) {
                mInnerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        baseVideoPlayer.getCurrentPlayer().autoAdaptation();
                    }
                }, 100);
            }
        }
    }

    /**
     * 自动适配在竖屏全屏时，
     * 由于刘海屏或者打孔屏占据空间带来的影响(某些机型在全屏时会自动将布局下移（或者添加padding），
     * 例如三星S10、小米8；但是也有一些机型在全屏时不会处理，此时，就为了兼容这部分机型)
     */
    protected void autoAdaptation() {
        Context context = getContext();
        if (isVerticalVideo()) {
            int[] location = new int[2];
            getLocationOnScreen(location);
            /*同时判断系统是否有自动将布局从statusbar下方开始显示，根据在屏幕中的位置判断*/
            //如果系统没有将布局下移，那么此时处理
            if (location[1] == 0) {
                setPadding(0, CommonUtil.getStatusBarHeight(context), 0, 0);
                Logger.d("竖屏，系统未将布局下移");
            } else {
                Logger.d("竖屏，系统将布局下移；y:" + location[1]);
            }
        }
    }

    /**
     * 获取全屏Id
     */
    protected abstract int getFullId();

    //----------------------------开放接口 ----------------------------------

    /**
     * 是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏，注意，这时候默认旋转无效
     */
    public boolean isVerticalFullByVideoSize() {
        return isVerticalVideo() && isAutoFullWithSize();
    }

    /**
     * 旋转处理
     *
     * @param activity         页面
     * @param newConfig        配置
     * @param orientationUtils 旋转工具类
     */
    public void onConfigurationChanged(Activity activity, Configuration newConfig, OrientationUtils orientationUtils) {
        onConfigurationChanged(activity, newConfig, orientationUtils, true, true);
    }

    /**
     * 旋转处理
     *
     * @param activity         页面
     * @param newConfig        配置
     * @param orientationUtils 旋转工具类
     * @param hideActionBar    是否隐藏actionbar
     * @param hideStatusBar    是否隐藏statusbar
     */
    public void onConfigurationChanged(Activity activity, Configuration newConfig, OrientationUtils orientationUtils, boolean hideActionBar, boolean hideStatusBar) {
        super.onConfigurationChanged(newConfig);
        //如果旋转了就全屏
        if (newConfig.orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
            if (!isIfCurrentIsFullscreen()) {
                //全屏
                startWindowFullscreen(activity, hideActionBar, hideStatusBar);
            }
        } else {
            //新版本isIfCurrentIsFullscreen的标志位内部提前设置了，所以不会和手动点击冲突
            if (isIfCurrentIsFullscreen() && !isVerticalFullByVideoSize()) {
                backFromFull(activity);
            }
            if (orientationUtils != null) {
                orientationUtils.setEnable(true);
            }
        }

    }

    /**
     * 可配置旋转 OrientationUtils
     */
    public OrientationOption getOrientationOption() {
        return null;
    }

    /**
     * 利用window.ID_ANDROID_CONTENT层播放全屏效果
     *
     * @param actionBar 是否有actionBar，有的话需要隐藏
     * @param statusBar 是否有状态bar，有的话需要隐藏
     */
    @SuppressWarnings("ResourceType, unchecked")
    public BaseVideoPlayer startWindowFullscreen(final Context context, final boolean actionBar, final boolean statusBar) {
        //先记录一下当前Activity的SystemUiVisibility
        mSystemUiVisibility = ((Activity) context).getWindow().getDecorView().getSystemUiVisibility();
        //隐藏actionbar/statusbar/Nav
        CommonUtil.hideSupportActionBar(context, actionBar, statusBar);
        if (mHideKey) {
            CommonUtil.hideNavKey(context);
        }
        this.mActionBar = actionBar;
        this.mStatusBar = statusBar;
        mListItemRect = new int[2];
        mListItemSize = new int[2];
        final ViewGroup vp = getViewGroup();
        if (vp != null)
            removeVideo(vp, getFullId());
        if (mRenderViewContainer.getChildCount() > 0) {
            mRenderViewContainer.removeAllViews();
        }
        saveLocationStatus(context, statusBar, actionBar);
        //切换时关闭非全屏定时器
        cancelProgressTimer();
        boolean hadNewConstructor = true;
        try {
            BaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
        } catch (Exception e) {
            hadNewConstructor = false;
        }
        try {
            //通过被重载的不同构造器来选择
            Constructor<BaseVideoPlayer> constructor;
            final BaseVideoPlayer baseVideoPlayer;
            if (!hadNewConstructor) {
                constructor = (Constructor<BaseVideoPlayer>) BaseVideoPlayer.this.getClass().getConstructor(Context.class);
                baseVideoPlayer = constructor.newInstance(mContext);
            } else {
                constructor = (Constructor<BaseVideoPlayer>) BaseVideoPlayer.this.getClass().getConstructor(Context.class, Boolean.class);
                baseVideoPlayer = constructor.newInstance(mContext, true);
            }
            baseVideoPlayer.setId(getFullId());
            baseVideoPlayer.setIfCurrentIsFullscreen(true);
            baseVideoPlayer.setVideoAllCallBack(mVideoAllCallBack);
            cloneParams(this, baseVideoPlayer);
            if (baseVideoPlayer.getFullscreenButton() != null) {
                baseVideoPlayer.getFullscreenButton().setImageResource(getShrinkImageRes());
                baseVideoPlayer.getFullscreenButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mBackFromFullScreenListener == null) {
                            clearFullscreenLayout();
                        } else {
                            mBackFromFullScreenListener.onClick(v);
                        }
                    }
                });
            }
            if (baseVideoPlayer.getBackButton() != null) {
                baseVideoPlayer.getBackButton().setVisibility(VISIBLE);
                baseVideoPlayer.getBackButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mBackFromFullScreenListener == null) {
                            clearFullscreenLayout();
                        } else {
                            mBackFromFullScreenListener.onClick(v);
                        }
                    }
                });
            }
            final FrameLayout.LayoutParams lpParent = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            final FrameLayout frameLayout = new FrameLayout(context);
            frameLayout.setBackgroundColor(Color.BLACK);
            if (mShowFullAnimation) {
                mFullAnimEnd = false;
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getWidth(), getHeight());
                lp.setMargins(mListItemRect[0], mListItemRect[1], 0, 0);
                frameLayout.addView(baseVideoPlayer, lp);
                if (vp != null)
                    vp.addView(frameLayout, lpParent);
                mInnerHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            TransitionManager.beginDelayedTransition(vp);
                        }
                        resolveFullVideoShow(context, baseVideoPlayer, frameLayout);
                        mFullAnimEnd = true;
                    }
                }, 300);
            } else {
                FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(getWidth(), getHeight());
                frameLayout.addView(baseVideoPlayer, lp);
                if (vp != null)
                    vp.addView(frameLayout, lpParent);
                baseVideoPlayer.setVisibility(INVISIBLE);
                frameLayout.setVisibility(INVISIBLE);
                resolveFullVideoShow(context, baseVideoPlayer, frameLayout);
            }
            baseVideoPlayer.addTextureView();
            baseVideoPlayer.startProgressTimer();
            getVideoManager().setLastListener(this);
            getVideoManager().setListener(baseVideoPlayer);
            checkoutState();
            return baseVideoPlayer;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public boolean isShowFullAnimation() {
        return mShowFullAnimation;
    }

    /**
     * 全屏动画
     *
     * @param showFullAnimation 是否使用全屏动画效果
     */
    public void setShowFullAnimation(boolean showFullAnimation) {
        this.mShowFullAnimation = showFullAnimation;
    }

    public boolean isRotateViewAuto() {
        if (mAutoFullWithSize) {
            return false;
        }
        return mRotateViewAuto;
    }

    /**
     * 是否开启自动旋转
     */
    public void setRotateViewAuto(boolean rotateViewAuto) {
        this.mRotateViewAuto = rotateViewAuto;
        if (mOrientationUtils != null) {
            mOrientationUtils.setEnable(rotateViewAuto);
        }
    }

    public boolean isLockLand() {
        return mLockLand;
    }

    /**
     * 一全屏就锁屏横屏，默认false竖屏，可配合setRotateViewAuto使用
     */
    public void setLockLand(boolean lockLand) {
        this.mLockLand = lockLand;
    }


    public boolean isRotateWithSystem() {
        return mRotateWithSystem;
    }

    /**
     * 是否更新系统旋转，false的话，系统禁止旋转也会跟着旋转
     *
     * @param rotateWithSystem 默认true
     */
    public void setRotateWithSystem(boolean rotateWithSystem) {
        this.mRotateWithSystem = rotateWithSystem;
        if (mOrientationUtils != null) {
            mOrientationUtils.setRotateWithSystem(rotateWithSystem);
        }
    }

    /**
     * 获取全屏播放器对象
     *
     * @return VideoView 如果没有则返回空。
     */
    @SuppressWarnings("ResourceType")
    public VideoView getFullWindowPlayer() {
        ViewGroup vp = (CommonUtil.scanForActivity(getContext())).findViewById(Window.ID_ANDROID_CONTENT);
        final View full = vp.findViewById(getFullId());
        VideoView videoView = null;
        if (full != null) {
            videoView = (VideoView) full;
        }
        return videoView;
    }


    /**
     * 获取当前长在播放的播放控件
     */
    public BaseVideoPlayer getCurrentPlayer() {
        if (getFullWindowPlayer() != null) {
            return (BaseVideoPlayer) getFullWindowPlayer();
        }
        return this;
    }

    /**
     * 全屏返回监听，如果设置了，默认返回动作无效
     * 包含返回键和全屏返回按键，前提是这两个按键存在
     */
    public void setBackFromFullScreenListener(View.OnClickListener backFromFullScreenListener) {
        this.mBackFromFullScreenListener = backFromFullScreenListener;
    }

    public boolean isFullHideActionBar() {
        return mActionBar;
    }

    public void setFullHideActionBar(boolean actionBar) {
        this.mActionBar = actionBar;
    }

    public boolean isFullHideStatusBar() {
        return mStatusBar;
    }

    public void setFullHideStatusBar(boolean statusBar) {
        this.mStatusBar = statusBar;
    }

    public int getSaveBeforeFullSystemUiVisibility() {
        return mSystemUiVisibility;
    }

    public void setSaveBeforeFullSystemUiVisibility(int systemUiVisibility) {
        this.mSystemUiVisibility = systemUiVisibility;
    }

    public boolean isAutoFullWithSize() {
        return mAutoFullWithSize;
    }

    /**
     * 是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏，注意，这时候默认旋转无效
     *
     * @param autoFullWithSize 默认false
     */
    public void setAutoFullWithSize(boolean autoFullWithSize) {
        this.mAutoFullWithSize = autoFullWithSize;
    }


    public boolean isNeedAutoAdaptation() {
        return isNeedAutoAdaptation;
    }

    /**
     * 是否需要适配在竖屏横屏时，由于刘海屏或者打孔屏占据空间，导致标题显示被遮盖的问题
     *
     * @param needAutoAdaptation 默认false
     */
    public void setNeedAutoAdaptation(boolean needAutoAdaptation) {
        isNeedAutoAdaptation = needAutoAdaptation;
    }

    public boolean isOnlyRotateLand() {
        return mIsOnlyRotateLand;
    }

    /**
     * 旋转时仅处理横屏
     */
    public void setOnlyRotateLand(boolean onlyRotateLand) {
        this.mIsOnlyRotateLand = onlyRotateLand;
        if (mOrientationUtils != null) {
            mOrientationUtils.setOnlyRotateLand(mIsOnlyRotateLand);
        }
    }

    //----------------------------开放接口 ----------------------------------


}
