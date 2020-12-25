package com.jack.player.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jack.player.R;
import com.jack.player.utils.CommonUtil;
import com.jack.player.utils.Logger;
import com.jack.player.utils.NetworkUtils;
import com.jack.player.utils.OrientationUtils;
import com.jack.player.view.PlayView;

/**
 * 标准播放器，继承之后实现一些ui显示效果，如显示／隐藏ui，播放按键等
 *
 * @author jack
 * @since 2020/10/27 17:23
 */
public class  StandardVideoPlayer extends VideoPlayer {
    //亮度dialog
    protected Dialog mBrightnessDialog;
    //音量dialog
    protected Dialog mVolumeDialog;
    //触摸进度dialog
    protected Dialog mProgressDialog;
    //触摸进度条的progress
    protected ProgressBar mDialogProgressBar;
    //音量进度条的progress
    protected ProgressBar mDialogVolumeProgressBar;
    //亮度文本
    protected TextView mBrightnessDialogTv;
    //触摸移动显示文本
    protected TextView mDialogSeekTime;
    //触摸移动显示全部时间
    protected TextView mDialogTotalTime;
    //触摸移动方向icon
    protected ImageView mDialogIcon;
    //底部进度条（最底部）
    protected Drawable mBottomProgressDrawable;
    //进度条
    protected Drawable mProgressBarDrawable;
    protected Drawable mProgressBarThumbDrawable;
    //调节音量进度条
    protected Drawable mVolumeProgressDrawable;
    //中央显示进度条
    protected Drawable mDialogProgressBarDrawable;
    //mDialogSeekTime
    protected int mDialogProgressHighLightColor = -11;
    //mDialogTotalTime
    protected int mDialogProgressNormalColor = -11;

    /**
     * 如果需要不同布局区分功能，需要重载
     */
    public StandardVideoPlayer(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public StandardVideoPlayer(Context context) {
        super(context);
    }

    public StandardVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        //增加自定义ui
        if (mBottomProgressDrawable != null) {
            mBottomProgressBar.setProgressDrawable(mBottomProgressDrawable);
        }
        if (mProgressBarDrawable != null) {
            mProgressBar.setProgressDrawable(mProgressBarDrawable);
        }
        if (mProgressBarThumbDrawable != null) {
            mProgressBar.setThumb(mProgressBarThumbDrawable);
        }

    }

    /**
     * 继承后重写可替换为你需要的布局
     */
    @Override
    public int getLayoutId() {
        return R.layout.video_layout_standard;
    }

    /**
     * 开始播放（点击封面播放、网络出错重新播放、网络切换播放）
     */
    @Override
    public void startPlayLogic() {
        if (mVideoAllCallBack != null) {
            Logger.d("onClickStartThumb");
            mVideoAllCallBack.onClickStartThumb(mOriginUrl, mTitle, StandardVideoPlayer.this);
        }
        prepareVideo();
        startDismissControlViewTimer();
    }

    /**
     * 显示wifi确定框，如需要自定义继承重写即可
     */
    @Override
    protected void showWifiDialog() {
        if (!NetworkUtils.isAvailable(mContext)) {
            //Toast.makeText(mContext, getResources().getString(R.string.no_net), Toast.LENGTH_LONG).show();
            startPlayLogic();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivityContext());
        builder.setMessage(getResources().getString(R.string.tips_not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.tips_not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                startPlayLogic();
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.tips_not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    /**
     * 触摸显示滑动进度dialog，如需要自定义继承重写即可，记得重写dismissProgressDialog
     */
    @Override
    @SuppressWarnings("ResourceType")
    protected void showProgressDialog(float deltaX, String seekTime, int seekTimePosition, String totalTime, int totalTimeDuration) {
        if (mProgressDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(getProgressDialogLayoutId(), null);
            if (localView.findViewById(getProgressDialogProgressId()) instanceof ProgressBar) {
                mDialogProgressBar = localView.findViewById(getProgressDialogProgressId());
                if (mDialogProgressBarDrawable != null) {
                    mDialogProgressBar.setProgressDrawable(mDialogProgressBarDrawable);
                }
            }
            if (localView.findViewById(getProgressDialogCurrentDurationTextId()) instanceof TextView) {
                mDialogSeekTime = localView.findViewById(getProgressDialogCurrentDurationTextId());
            }
            if (localView.findViewById(getProgressDialogAllDurationTextId()) instanceof TextView) {
                mDialogTotalTime = localView.findViewById(getProgressDialogAllDurationTextId());
            }
            if (localView.findViewById(getProgressDialogImageId()) instanceof ImageView) {
                mDialogIcon = localView.findViewById(getProgressDialogImageId());
            }
            mProgressDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mProgressDialog.setContentView(localView);
            Window window = mProgressDialog.getWindow();
            if (window != null) {
                window.addFlags(Window.FEATURE_ACTION_BAR);
                window.addFlags(32);
                window.addFlags(16);
                window.setLayout(getWidth(), getHeight());
                if (mDialogProgressNormalColor != -11 && mDialogTotalTime != null) {
                    mDialogTotalTime.setTextColor(mDialogProgressNormalColor);
                }
                if (mDialogProgressHighLightColor != -11 && mDialogSeekTime != null) {
                    mDialogSeekTime.setTextColor(mDialogProgressHighLightColor);
                }
                WindowManager.LayoutParams localLayoutParams = window.getAttributes();
                localLayoutParams.gravity = Gravity.TOP;
                localLayoutParams.width = getWidth();
                localLayoutParams.height = getHeight();
                int[] location = new int[2];
                getLocationOnScreen(location);
                localLayoutParams.x = location[0];
                localLayoutParams.y = location[1];
                window.setAttributes(localLayoutParams);
            }
        }
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
        if (mDialogSeekTime != null) {
            mDialogSeekTime.setText(seekTime);
        }
        if (mDialogTotalTime != null) {
            mDialogTotalTime.setText(" / " + totalTime);
        }
        if (totalTimeDuration > 0)
            if (mDialogProgressBar != null) {
                mDialogProgressBar.setProgress(seekTimePosition * 100 / totalTimeDuration);
            }
        if (deltaX > 0) {
            if (mDialogIcon != null) {
                mDialogIcon.setBackgroundResource(R.drawable.video_forward_icon);
            }
        } else {
            if (mDialogIcon != null) {
                mDialogIcon.setBackgroundResource(R.drawable.video_backward_icon);
            }
        }

    }

    @Override
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * 触摸音量dialog，如需要自定义继承重写即可，记得重写dismissVolumeDialog
     */
    @Override
    protected void showVolumeDialog(float deltaY, int volumePercent) {
        if (mVolumeDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(getVolumeLayoutId(), null);
            if (localView.findViewById(getVolumeProgressId()) instanceof ProgressBar) {
                mDialogVolumeProgressBar = localView.findViewById(getVolumeProgressId());
                if (mVolumeProgressDrawable != null && mDialogVolumeProgressBar != null) {
                    mDialogVolumeProgressBar.setProgressDrawable(mVolumeProgressDrawable);
                }
            }
            mVolumeDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mVolumeDialog.setContentView(localView);
            Window window = mVolumeDialog.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                WindowManager.LayoutParams localLayoutParams = window.getAttributes();
                localLayoutParams.gravity = Gravity.TOP | Gravity.START;
                localLayoutParams.width = getWidth();
                localLayoutParams.height = getHeight();
                int[] location = new int[2];
                getLocationOnScreen(location);
                localLayoutParams.x = location[0];
                localLayoutParams.y = location[1];
                window.setAttributes(localLayoutParams);
            }
        }
        if (!mVolumeDialog.isShowing()) {
            mVolumeDialog.show();
        }
        if (mDialogVolumeProgressBar != null) {
            mDialogVolumeProgressBar.setProgress(volumePercent);
        }
    }

    @Override
    protected void dismissVolumeDialog() {
        if (mVolumeDialog != null) {
            mVolumeDialog.dismiss();
            mVolumeDialog = null;
        }
    }


    /**
     * 触摸亮度dialog，如需要自定义继承重写即可，记得重写dismissBrightnessDialog
     */
    @Override
    protected void showBrightnessDialog(float percent) {
        if (mBrightnessDialog == null) {
            View localView = LayoutInflater.from(getActivityContext()).inflate(getBrightnessLayoutId(), null);
            if (localView.findViewById(getBrightnessTextId()) instanceof TextView) {
                mBrightnessDialogTv = localView.findViewById(getBrightnessTextId());
            }
            mBrightnessDialog = new Dialog(getActivityContext(), R.style.video_style_dialog_progress);
            mBrightnessDialog.setContentView(localView);
            Window window = mBrightnessDialog.getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                WindowManager.LayoutParams localLayoutParams = window.getAttributes();
                localLayoutParams.gravity = Gravity.TOP | Gravity.END;
                localLayoutParams.width = getWidth();
                localLayoutParams.height = getHeight();
                int[] location = new int[2];
                getLocationOnScreen(location);
                localLayoutParams.x = location[0];
                localLayoutParams.y = location[1];
                window.setAttributes(localLayoutParams);
            }
        }
        if (!mBrightnessDialog.isShowing()) {
            mBrightnessDialog.show();
        }
        if (mBrightnessDialogTv != null)
            mBrightnessDialogTv.setText((int) (percent * 100) + "%");
    }


    @Override
    protected void dismissBrightnessDialog() {
        if (mBrightnessDialog != null) {
            mBrightnessDialog.dismiss();
            mBrightnessDialog = null;
        }
    }

    @Override
    protected void cloneParams(BaseVideoPlayer from, BaseVideoPlayer to) {
        super.cloneParams(from, to);
        StandardVideoPlayer sf = (StandardVideoPlayer) from;
        StandardVideoPlayer st = (StandardVideoPlayer) to;
        if (st.mProgressBar != null && sf.mProgressBar != null) {
            st.mProgressBar.setProgress(sf.mProgressBar.getProgress());
            st.mProgressBar.setSecondaryProgress(sf.mProgressBar.getSecondaryProgress());
        }
        if (st.mTotalTimeTextView != null && sf.mTotalTimeTextView != null) {
            st.mTotalTimeTextView.setText(sf.mTotalTimeTextView.getText());
        }
        if (st.mCurrentTimeTextView != null && sf.mCurrentTimeTextView != null) {
            st.mCurrentTimeTextView.setText(sf.mCurrentTimeTextView.getText());
        }
    }

    /**
     * 将自定义的效果也设置到全屏
     *
     * @param actionBar 是否有actionBar，有的话需要隐藏
     * @param statusBar 是否有状态bar，有的话需要隐藏
     */
    @Override
    public BaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        BaseVideoPlayer baseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        if (baseVideoPlayer != null) {
            StandardVideoPlayer standardVideoPlayer = (StandardVideoPlayer) baseVideoPlayer;
            standardVideoPlayer.setLockClickListener(mLockClickListener);
            standardVideoPlayer.setNeedLockFull(isNeedLockFull());
            initFullUI();
            //比如你自定义了返回案件，但是因为返回按键底层已经设置了返回事件，所以你需要在这里重新增加的逻辑
        }
        return baseVideoPlayer;
    }

    //-------------------------------------各类UI的状态显示-------------------------------------

    /**
     * 点击触摸显示和隐藏逻辑
     */
    @Override
    protected void onClickUiToggle() {
        //如果用户全屏且选择了锁屏，只能控制锁屏UI的展示
        if (mIfCurrentIsFullscreen && mLockCurScreen && mNeedLockFull) {
            setViewShowState(mLockScreen, VISIBLE);
            return;
        }
        //当前媒体资源加载中
        if (mCurrentState == CURRENT_STATE_PREPAREING) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPrepareingClear();
                } else {
                    changeUiToPreparingShow();
                }
            }
        }
        //播放中
        else if (mCurrentState == CURRENT_STATE_PLAYING) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPlayingClear();
                } else {
                    changeUiToPlayingShow();
                }
            }
        }
        //暂停中
        else if (mCurrentState == CURRENT_STATE_PAUSE) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPauseClear();
                } else {
                    changeUiToPauseShow();
                }
            }
        }
        //播放完成
        else if (mCurrentState == CURRENT_STATE_AUTO_COMPLETE) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToCompleteClear();
                } else {
                    changeUiToCompleteShow();
                }
            }
        }
        //缓冲中
        else if (mCurrentState == CURRENT_STATE_PLAYING_BUFFERING_START) {
            if (mBottomContainer != null) {
                if (mBottomContainer.getVisibility() == View.VISIBLE) {
                    changeUiToPlayingBufferingClear();
                } else {
                    changeUiToPlayingBufferingShow();
                }
            }
        }
    }

    /**
     * 隐藏所有的控制UI
     */
    @Override
    protected void hideAllWidget() {
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomProgressBar, VISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
    }


    @Override
    protected void changeUiToNormal() {
        Logger.d("changeUiToNormal");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, VISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    @Override
    protected void changeUiToPreparingShow() {
        Logger.d("changeUiToPreparingShow");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, VISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, GONE);
    }

    @Override
    protected void changeUiToPlayingShow() {
        Logger.d("changeUiToPlayingShow");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    @Override
    protected void changeUiToPauseShow() {
        Logger.d("changeUiToPauseShow");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    @Override
    protected void changeUiToPlayingBufferingShow() {
        Logger.d("changeUiToPlayingBufferingShow");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, VISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, GONE);
    }

    @Override
    protected void clickFullscreen() {
        if (mOrientationUtils == null) {
            Activity activity = CommonUtil.scanForActivity(getContext());
            if (activity == null)
                return;
            mOrientationUtils = new OrientationUtils(activity, this);
        }
        //直接横屏
        mOrientationUtils.resolveOrientationByClick();
        //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
        startWindowFullscreen(getContext(), true, true);
    }

    @Override
    protected void changeUiToCompleteShow() {
        Logger.d("changeUiToCompleteShow");
        setViewShowState(mTopContainer, VISIBLE);
        setViewShowState(mBottomContainer, VISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, VISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    @Override
    protected void changeUiToError() {
        Logger.d("changeUiToError");
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissVolumeDialog();
        dismissBrightnessDialog();
    }

    /**
     * 触摸进度dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected int getProgressDialogLayoutId() {
        return R.layout.video_progress_dialog;
    }

    /**
     * 触摸进度dialog的进度条id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected int getProgressDialogProgressId() {
        return R.id.duration_progressbar;
    }

    /**
     * 触摸进度dialog的当前时间文本
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected int getProgressDialogCurrentDurationTextId() {
        return R.id.tv_current;
    }

    /**
     * 触摸进度dialog全部时间文本
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected int getProgressDialogAllDurationTextId() {
        return R.id.tv_duration;
    }

    /**
     * 触摸进度dialog的图片id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showProgressDialog方法
     */
    protected int getProgressDialogImageId() {
        return R.id.duration_image_tip;
    }

    /**
     * 音量dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showVolumeDialog方法
     */
    protected int getVolumeLayoutId() {
        return R.layout.video_volume_dialog;
    }

    /**
     * 音量dialog的百分比进度条 id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showVolumeDialog方法
     */
    protected int getVolumeProgressId() {
        return R.id.volume_progressbar;
    }


    /**
     * 亮度dialog的layoutId
     * 继承后重写可返回自定义
     * 有自定义的实现逻辑可重载showBrightnessDialog方法
     */
    protected int getBrightnessLayoutId() {
        return R.layout.video_brightness;
    }

    /**
     * 亮度dialog的百分比text id
     * 继承后重写可返回自定义，如果没有可返回空
     * 有自定义的实现逻辑可重载showBrightnessDialog方法
     */
    protected int getBrightnessTextId() {
        return R.id.app_video_brightness;
    }

    protected void changeUiToPrepareingClear() {
        Logger.d("changeUiToPrepareingClear");
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, VISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, GONE);
    }

    protected void changeUiToPlayingClear() {
        Logger.d("changeUiToPlayingClear");
        changeUiToClear();
        setViewShowState(mBottomProgressBar, VISIBLE);
    }

    protected void changeUiToPauseClear() {
        Logger.d("changeUiToPauseClear");
        changeUiToClear();
        setViewShowState(mBottomProgressBar, VISIBLE);
    }

    protected void changeUiToPlayingBufferingClear() {
        Logger.d("changeUiToPlayingBufferingClear");
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, VISIBLE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, VISIBLE);
        setViewShowState(mLockScreen, GONE);
        updateStartImage();
    }

    protected void changeUiToClear() {
        Logger.d("changeUiToClear");
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, INVISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, INVISIBLE);
        setViewShowState(mBottomProgressBar, INVISIBLE);
        setViewShowState(mLockScreen, GONE);

    }

    protected void changeUiToCompleteClear() {
        Logger.d("changeUiToCompleteClear");
        setViewShowState(mTopContainer, INVISIBLE);
        setViewShowState(mBottomContainer, INVISIBLE);
        setViewShowState(mStartButton, VISIBLE);
        setViewShowState(mLoadingProgressBar, GONE);
        setViewShowState(mThumbImageViewLayout, VISIBLE);
        setViewShowState(mBottomProgressBar, VISIBLE);
        setViewShowState(mLockScreen, (mIfCurrentIsFullscreen && mNeedLockFull) ? VISIBLE : GONE);
        updateStartImage();
    }

    /**
     * 更新开始按键显示
     */
    protected void updateStartImage() {
        if (mStartButton instanceof PlayView) {
            PlayView playView = (PlayView) mStartButton;
            playView.setDuration(300);
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                playView.play();
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                playView.pause();
            } else {
                playView.pause();
            }
        } else if (mStartButton instanceof ImageView) {
            ImageView imageView = (ImageView) mStartButton;
            if (mCurrentState == CURRENT_STATE_PLAYING) {
                imageView.setImageResource(R.drawable.video_click_pause_selector);
            } else if (mCurrentState == CURRENT_STATE_ERROR) {
                imageView.setImageResource(R.drawable.video_click_error_selector);
            } else {
                imageView.setImageResource(R.drawable.video_click_play_selector);
            }
        }
    }

    /**
     * 全屏的UI逻辑
     */
    private void initFullUI() {
        if (mBottomProgressDrawable != null) {
            setBottomProgressBarDrawable(mBottomProgressDrawable);
        }
        if (mProgressBarDrawable != null && mProgressBarThumbDrawable != null) {
            setBottomShowProgressBarDrawable(mProgressBarDrawable, mProgressBarThumbDrawable);
        }
        if (mVolumeProgressDrawable != null) {
            setDialogVolumeProgressBar(mVolumeProgressDrawable);
        }
        if (mDialogProgressBarDrawable != null) {
            setDialogProgressBar(mDialogProgressBarDrawable);
        }
        if (mDialogProgressHighLightColor >= 0 && mDialogProgressNormalColor >= 0) {
            setDialogProgressColor(mDialogProgressHighLightColor, mDialogProgressNormalColor);
        }
    }

    /**
     * 进度条-弹出的
     */
    public void setBottomShowProgressBarDrawable(Drawable drawable, Drawable thumb) {
        mProgressBarDrawable = drawable;
        mProgressBarThumbDrawable = thumb;
        if (mProgressBar != null) {
            mProgressBar.setProgressDrawable(drawable);
            mProgressBar.setThumb(thumb);
        }
    }

    /**
     * 底部进度条-非弹出
     */
    public void setBottomProgressBarDrawable(Drawable drawable) {
        mBottomProgressDrawable = drawable;
        if (mBottomProgressBar != null) {
            mBottomProgressBar.setProgressDrawable(drawable);
        }
    }

    /**
     * 声音进度条
     */
    public void setDialogVolumeProgressBar(Drawable drawable) {
        mVolumeProgressDrawable = drawable;
    }


    /**
     * 中间进度条
     */
    public void setDialogProgressBar(Drawable drawable) {
        mDialogProgressBarDrawable = drawable;
    }

    /**
     * 中间进度条字体颜色
     */
    public void setDialogProgressColor(int highLightColor, int normalColor) {
        mDialogProgressHighLightColor = highLightColor;
        mDialogProgressNormalColor = normalColor;
    }

    /**
     * 重新开启进度查询以及控制view消失的定时任务
     * 用于解决通过removeview方式做全屏切换导致的定时任务停止的问题
     */
    public void restartTimerTask() {
        startProgressTimer();
        startDismissControlViewTimer();
    }
}
