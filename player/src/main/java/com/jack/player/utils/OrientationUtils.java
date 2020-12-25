package com.jack.player.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.jack.player.video.BaseVideoPlayer;

import java.lang.ref.WeakReference;

/**
 * 处理屏幕旋转的的逻辑
 */

public class OrientationUtils {
    /**
     * 竖屏
     */
    private static final int LAND_TYPE_NULL = 0;
    /**
     * 横屏
     */
    private static final int LAND_TYPE_NORMAL = 1;
    /**
     * 反向横屏
     */
    private static final int LAND_TYPE_REVERSE = 2;
    /**
     * 监听设置方向时间间隔
     */
    private static final int INTERVAL = 2000;
    /**
     * 最后一次方向变化的时间
     */
    private long lastOrientationTime;
    private WeakReference<Activity> mActivity;
    private BaseVideoPlayer mVideoPlayer;
    /**
     * 实时监听手机旋转的角度
     */
    private OrientationEventListener mOrientationEventListener;
    private OrientationOption mOrientationOption;
    /**
     * 默认竖屏来自于：ActivityInfo
     */
    private int mScreenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    /**
     * 规划横竖屏的类型：LAND_TYPE_NULL、LAND_TYPE_NORMAL、LAND_TYPE_REVERSE
     */
    private int mIsLand = LAND_TYPE_NULL;
    /**
     * 是否由于点击back或横竖屏切换按钮导致的横竖屏切换
     */
    private boolean mClick = false;
    /**
     * 是否关闭横竖屏切换功能
     */
    private boolean mEnable = true;
    /**
     * 是否跟随系统
     */
    private boolean mRotateWithSystem = true;
    /**
     * 是否仅处理横屏
     */
    private boolean mIsOnlyRotateLand = false;


    public OrientationUtils(Activity activity, BaseVideoPlayer videoPlayer) {
        this(activity, videoPlayer, null);
    }

    public OrientationUtils(Activity activity, BaseVideoPlayer videoPlayer, OrientationOption orientationOption) {
        this.mActivity = new WeakReference<>(activity);
        this.mVideoPlayer = videoPlayer;
        if (orientationOption == null) {
            this.mOrientationOption = new OrientationOption();
        } else {
            this.mOrientationOption = orientationOption;
        }
        initGravity(activity);
        init();
    }

    /**
     * 监听全局方向改变
     */
    private void init() {
        final Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        final Context context = activity.getApplicationContext();
        mOrientationEventListener = new OrientationEventListener(context) {
            @SuppressLint("SourceLockedOrientationActivity")
            @Override
            public void onOrientationChanged(int rotation) {
                //为了不频繁执行以下方法
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - lastOrientationTime <= INTERVAL)
                    return;
                lastOrientationTime = currentTimeMillis;
                //方向是否自动随系统方向
                boolean autoRotateOn = (Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
                //方向被锁定且设置了随系统方向
                if (!autoRotateOn && mRotateWithSystem) {
                    if (!mIsOnlyRotateLand || getIsLand() == LAND_TYPE_NULL) {
                        return;
                    }
                }
                //是否根据视频尺寸，自动选择竖屏全屏或者横屏全屏，注意，这时候默认旋转无效
                if (mVideoPlayer != null && mVideoPlayer.isVerticalFullByVideoSize()) {
                    return;
                }
                // 设置竖屏
                if (((rotation >= 0) && (rotation <= mOrientationOption.getNormalPortraitAngleStart()))
                        || (rotation >= mOrientationOption.getNormalPortraitAngleEnd())) {
                    if (!mClick && mIsLand > LAND_TYPE_NULL && !mIsOnlyRotateLand) {
                        setPortrait();
                    }
                    mClick = false;
                }
                // 设置横屏
                else if (((rotation >= mOrientationOption.getNormalLandAngleStart())
                        && (rotation <= mOrientationOption.getNormalLandAngleEnd()))) {
                    if (!mClick && mIsLand != LAND_TYPE_NORMAL) {
                        setLandscape();
                    }
                    mClick = false;
                }
                // 设置反向横屏
                else if (rotation > mOrientationOption.getReverseLandAngleStart()
                        && rotation < mOrientationOption.getReverseLandAngleEnd()) {
                    if (!mClick && mIsLand != LAND_TYPE_REVERSE) {
                        setReverseLandscape();
                    }
                    mClick = false;
                }
            }
        };
        //开启监听
        mOrientationEventListener.enable();
    }

    /**
     * 设置反向横屏
     */
    private void setReverseLandscape() {
        mScreenType = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        if (mVideoPlayer.getFullscreenButton() != null)
            mVideoPlayer.getFullscreenButton().setImageResource(mVideoPlayer.getShrinkImageRes());
        mIsLand = LAND_TYPE_REVERSE;
    }

    /**
     * 设置横屏
     */
    private void setLandscape() {
        mScreenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (mVideoPlayer.getFullscreenButton() != null)
            mVideoPlayer.getFullscreenButton().setImageResource(mVideoPlayer.getShrinkImageRes());
        mIsLand = LAND_TYPE_NORMAL;
    }

    /**
     * 设置竖屏
     */
    private void setPortrait() {
        mScreenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (mVideoPlayer.getFullscreenButton() != null)
            mVideoPlayer.getFullscreenButton().setImageResource(mVideoPlayer.getEnlargeImageRes());
        mIsLand = LAND_TYPE_NULL;
    }

    /**
     * 初始化屏幕方向，统一设置mIsLand、mScreenType
     */
    private void initGravity(Activity activity) {
        if (mIsLand == LAND_TYPE_NULL) {
            int defaultRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            if (defaultRotation == Surface.ROTATION_0) {
                //竖向为正方向。 如：手机、小米平板
                mIsLand = LAND_TYPE_NULL;
                mScreenType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            } else if (defaultRotation == Surface.ROTATION_270) {
                //反向横屏。 如：三星、sony平板
                mIsLand = LAND_TYPE_REVERSE;
                mScreenType = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
            //ROTATION_90、ROTATION_180都算为普通横屏，不识别选转180
            else {
                //横屏
                mIsLand = LAND_TYPE_NORMAL;
                mScreenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            }
        }
    }

    /**
     * 设置activity屏幕方向
     */
    private void setRequestedOrientation(int requestedOrientation) {
        final Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        try {
            activity.setRequestedOrientation(requestedOrientation);
        } catch (IllegalStateException exception) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O || Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {
                Logger.e("OrientationUtils", exception);
            } else {
                exception.printStackTrace();
            }
        }
    }

    /**
     * 点击切换横竖屏的逻辑，比如竖屏的时候点击了就是切换到横屏不会受屏幕的影响
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public void resolveOrientationByClick() {
        //自动选择竖屏全屏或者横屏全屏，注意，这时候默认旋转无效
        if (mIsLand == LAND_TYPE_NULL && mVideoPlayer != null && mVideoPlayer.isVerticalFullByVideoSize()) {
            return;
        }
        mClick = true;
        Activity activity = mActivity.get();
        if (activity == null) {
            return;
        }
        //竖屏变横屏
        if (mIsLand == LAND_TYPE_NULL) {
            int request = activity.getRequestedOrientation();
            if (request == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                mScreenType = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                mIsLand = LAND_TYPE_REVERSE;
            } else {
                mScreenType = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                mIsLand = LAND_TYPE_NORMAL;
            }
            setRequestedOrientation(mScreenType);
            if (mVideoPlayer.getFullscreenButton() != null)
                mVideoPlayer.getFullscreenButton().setImageResource(mVideoPlayer.getShrinkImageRes());
        }
        //横屏变竖屏
        else {
            setPortrait();
        }

    }

    /**
     * 返回键退出全屏
     * 列表返回的样式判断。因为立即旋转会导致界面跳动的问题
     */
    @SuppressLint("SourceLockedOrientationActivity")
    public int backToPortraitVideo() {
        //横屏
        if (mIsLand > LAND_TYPE_NULL) {
            mClick = true;
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (mVideoPlayer != null && mVideoPlayer.getFullscreenButton() != null)
                mVideoPlayer.getFullscreenButton().setImageResource(mVideoPlayer.getEnlargeImageRes());
            mIsLand = LAND_TYPE_NULL;
            return 500;
        }
        return LAND_TYPE_NULL;
    }


    public boolean isEnable() {
        return mEnable;
    }

    /**
     * 开启监听开关
     */
    public void setEnable(boolean enable) {
        this.mEnable = enable;
        if (mEnable) {
            mOrientationEventListener.enable();
        } else {
            mOrientationEventListener.disable();
        }
    }

    /**
     * 禁用横竖屏监听功能 onDestory调用
     */
    public void releaseListener() {
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }
    }

    public int getIsLand() {
        return mIsLand;
    }

    public void setIsLand(int isLand) {
        this.mIsLand = isLand;
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
    }

    public boolean isOnlyRotateLand() {
        return mIsOnlyRotateLand;
    }

    /**
     * 旋转时仅处理横屏
     */
    public void setOnlyRotateLand(boolean onlyRotateLand) {
        this.mIsOnlyRotateLand = onlyRotateLand;
    }

    public OrientationOption getOrientationOption() {
        return mOrientationOption;
    }

    public void setOrientationOption(OrientationOption orientationOption) {
        this.mOrientationOption = orientationOption;
    }
}
