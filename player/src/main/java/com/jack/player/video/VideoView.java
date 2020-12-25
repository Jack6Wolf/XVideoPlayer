package com.jack.player.video;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.InflateException;
import android.view.Surface;
import android.view.WindowManager;

import com.jack.player.R;
import com.jack.player.listener.MediaPlayerListener;
import com.jack.player.listener.VideoAllCallBack;
import com.jack.player.utils.CommonUtil;
import com.jack.player.utils.Logger;
import com.jack.player.utils.NetInfoModule;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * 视频回调与状态处理等相关层
 *
 * @author jack
 * @since 2020/10/27 14:21
 */
public abstract class VideoView extends PlayerRenderView implements MediaPlayerListener {
    //正常（准备之前的状态）
    public static final int CURRENT_STATE_NORMAL = 0;
    //准备中
    public static final int CURRENT_STATE_PREPAREING = 1;
    //播放中
    public static final int CURRENT_STATE_PLAYING = 2;
    //开始缓冲
    public static final int CURRENT_STATE_PLAYING_BUFFERING_START = 3;
    //暂停
    public static final int CURRENT_STATE_PAUSE = 5;
    //自动播放结束
    public static final int CURRENT_STATE_AUTO_COMPLETE = 6;
    //错误状态
    public static final int CURRENT_STATE_ERROR = 7;

    //避免切换时频繁setup
    public static final int CHANGE_DELAY_TIME = 2000;

    //当前的播放状态
    protected int mCurrentState = -1;

    //播放的tag，防止错误，因为普通的url也可能重复
    protected int mPlayPosition = -22;

    //屏幕宽度
    protected int mScreenWidth;

    //屏幕高度
    protected int mScreenHeight;

    //缓冲百分比
    protected int mBufferPoint;

    //记录缓冲时的播放状态
    protected int mBackUpPlayingBufferState = -1;

    //从哪个开始播放（初始开始播放就seek，比如切换网络重新播放就会有这个需求）
    protected long mSeekOnStart = -1;

    //当前的播放位置
    protected long mCurrentPosition;

    //保存切换时的时间，避免频繁契合
    protected long mSaveChangeViewTime = 0;

    //播放速度
    protected float mSpeed = 1;

    //是否播边边缓冲
    protected boolean mCache = false;

    //当前是否全屏
    protected boolean mIfCurrentIsFullscreen = false;

    //循环
    protected boolean mLooping = false;

    //是否播放过
    protected boolean mHadPlay = false;

    //是否发送了网络改变
    protected boolean mNetChanged = false;

    //是否不变调
    protected boolean mSoundTouch = false;

    //是否需要显示暂停锁定效果
    protected boolean mShowPauseCover = false;

    //是否准备完成前调用了暂停
    protected boolean mPauseBeforePrepared = false;

    //Prepared之后是否自动开始播放
    protected boolean mStartAfterPrepared = true;
    //Prepared（完成）
    protected boolean mHadPrepared = false;
    //是否播放器当长时间失去音频焦点时。true：释放播放器 ，false：暂停播放
    protected boolean mReleaseWhenLossAudio = true;
    //音频焦点的监听
    protected AudioManager mAudioManager;
    //播放的tag，防止错误，因为普通的url也可能重复
    protected String mPlayTag = "";

    //上下文
    protected Context mContext;

    //原来的url
    protected String mOriginUrl;

    //转化后的URL
    protected String mUrl;

    //标题
    protected String mTitle;

    //网络状态
    protected String mNetSate = "NORMAL";

    // 是否需要覆盖拓展类型
    protected String mOverrideExtension;

    //缓存路径，可不设置
    protected File mCachePath;

    //视频回调
    protected VideoAllCallBack mVideoAllCallBack;

    //http request header
    protected Map<String, String> mMapHeadData = new HashMap<>();

    //网络监听
    protected NetInfoModule mNetInfoModule;
    /**
     * 监听是否有外部其他多媒体开始播放
     */
    protected AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                //你已经获得音频焦点
                case AudioManager.AUDIOFOCUS_GAIN:
                    onGankAudio();
                    break;
                //你已经失去音频焦点很长时间了，必须终止所有的音频播放。
                //因为长时间的失去焦点后，不应该在期望有焦点返回，这是一个尽可能清除不用资源的好位置。
                //例如，应该在此时释放MediaPlayer对象；
                case AudioManager.AUDIOFOCUS_LOSS:
                    onLossAudio();
                    break;
                //这说明你临时失去了音频焦点，但是在不久就会再返回来。
                //此时，你必须终止所有的音频播放，但是保留你的播放资源，因为可能不久就会返回来。
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    onLossTransientAudio();
                    break;
                //这说明你已经临时失去了音频焦点，但允许你安静的播放音频（低音量），
                //而不是完全的终止音频播放。目前所有的情况下，oFocusChange的时候停止mediaPlayer */
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    onLossTransientCanDuck();
                    break;
                default:
                    break;
            }
        }
    };

    public VideoView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public VideoView(Context context, Boolean fullFlag) {
        super(context);
        mIfCurrentIsFullscreen = fullFlag;
        init(context);
    }

    //------------------------------MeasureFormVideoParamsListener--------------------------------------------------
    @Override
    public int getCurrentVideoWidth() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getCurrentVideoHeight() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoHeight();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (getVideoManager() != null) {
            return getVideoManager().getVideoSarDen();
        }
        return 0;
    }
    //------------------------------MeasureFormVideoParamsListener--------------------------------------------------

    protected Context getActivityContext() {
        return CommonUtil.getActivityContext(getContext());
    }

    protected void init(Context context) {
        if (getActivityContext() != null) {
            this.mContext = getActivityContext();
        } else {
            this.mContext = context;
        }
        initInflate(mContext);
        mRenderViewContainer = findViewById(R.id.surface_container);
        if (mRenderViewContainer == null)
            throw new IllegalArgumentException("该子布局必须设置，它是搭载播放器父布局！");

        //解决可视化编辑器无法识别自定义控件的问题
        if (isInEditMode())
            return;
        mScreenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = mContext.getResources().getDisplayMetrics().heightPixels;
        mAudioManager = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private void initInflate(Context context) {
        try {
            inflate(context, getLayoutId(), this);
        } catch (InflateException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始播放逻辑(特指初始化开始播放和播放完成重新播放)
     */
    protected void startButtonLogic() {
        if (mVideoAllCallBack != null && (mCurrentState == CURRENT_STATE_NORMAL
                || mCurrentState == CURRENT_STATE_AUTO_COMPLETE)) {
            Logger.d("onClickStartIcon");
            mVideoAllCallBack.onClickStartIcon(mOriginUrl, mTitle, this);
        } else if (mVideoAllCallBack != null) {
            Logger.d("onClickStartError");
            mVideoAllCallBack.onClickStartError(mOriginUrl, mTitle, this);
        }
        prepareVideo();
    }

    /**
     * 开始状态视频播放，初始化播放器，加载媒体资源
     */
    protected void prepareVideo() {
        startPrepare();
    }

    private void startPrepare() {
        //初始化开始播放和播放完成重新播放都为null
        if (getVideoManager().listener() != null) {
            getVideoManager().listener().onCompletion();
        }
        if (mVideoAllCallBack != null) {
            Logger.d("onStartPrepared");
            mVideoAllCallBack.onStartPrepared(mOriginUrl, mTitle, this);
        }
        getVideoManager().setListener(this);
        getVideoManager().setPlayTag(mPlayTag);
        getVideoManager().setPlayPosition(mPlayPosition);
        //注册音频焦点监听
        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        try {
            if (mContext instanceof Activity) {
                //保持屏幕常亮
                ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBackUpPlayingBufferState = -1;
        getVideoManager().prepare(mUrl, (mMapHeadData == null) ? new HashMap<String, String>() : mMapHeadData, mLooping, mSpeed, mCache, mCachePath, mOverrideExtension);
        //重置seekbar进度和时间等
        setStateAndUi(CURRENT_STATE_PREPAREING);
    }

    /**
     * 获得了Audio Focus
     */
    private void onGankAudio() {
    }

    /**
     * 失去了Audio Focus，并将会持续很长的时间
     */
    private void onLossAudio() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //长时间失去焦点，最好关闭播放，清空所有资源
                if (mReleaseWhenLossAudio) {
                    releaseVideos();
                } else {
                    //暂停
                    onVideoPause();
                }

            }
        });
    }

    /**
     * 暂时失去Audio Focus，并会很快再次获得
     */
    private void onLossTransientAudio() {
        try {
            //暂停
            this.onVideoPause();
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    /**
     * 暂时失去AudioFocus，但是可以继续播放，不过可以降低音量
     */
    private void onLossTransientCanDuck() {
    }


    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param title         title
     */
    public boolean setUp(String url, boolean cacheWithPlay, String title) {
        return setUp(url, cacheWithPlay, null, title);
    }


    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param mapHeadData   头部信息
     * @param title         title
     */
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, Map<String, String> mapHeadData, String title) {
        if (setUp(url, cacheWithPlay, cachePath, title)) {
            if (this.mMapHeadData != null) {
                this.mMapHeadData.clear();
            } else {
                this.mMapHeadData = new HashMap<>();
            }
            if (mapHeadData != null) {
                this.mMapHeadData.putAll(mapHeadData);
            }
            return true;
        }
        return false;
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param title         title
     */
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, String title) {
        return setUp(url, cacheWithPlay, cachePath, title, true);
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param title         title
     * @param changeState   是否修改状态
     */
    public boolean setUp(String url, boolean cacheWithPlay, File cachePath, String title, boolean changeState) {
        mCache = cacheWithPlay;
        mCachePath = cachePath;
        mOriginUrl = url;
        if (isCurrentMediaListener() &&
                (System.currentTimeMillis() - mSaveChangeViewTime) < CHANGE_DELAY_TIME)
            return false;
        mCurrentState = CURRENT_STATE_NORMAL;
        this.mUrl = url;
        this.mTitle = title;
        if (changeState)
            setStateAndUi(CURRENT_STATE_NORMAL);
        return true;
    }

    /**
     * 重置
     */
    public void onVideoReset() {
        setStateAndUi(CURRENT_STATE_NORMAL);
    }

    /**
     * 处理因切换网络而导致的问题,重启播放器开始播放
     */
    private void netWorkErrorLogic() {
        //记录上次播放位置
        final long currentPosition = getCurrentPositionWhenPlaying();
        Logger.d("******* Net State Changed. renew player to connect *******" + currentPosition);
        getVideoManager().releaseMediaPlayer();
        postDelayed(new Runnable() {
            @Override
            public void run() {
                //设置历史播放位置
                setSeekOnStart(currentPosition);
                //开始播放
                startPlayLogic();
            }
        }, 500);
    }

    /**
     * 播放错误的时候，删除缓存文件
     */
    private void deleteCacheFileWhenError() {
        clearCurrentCache();
        Logger.d("Link Or mCache Error, Please Try Again " + mOriginUrl);
        if (mCache) {
            Logger.d("mCache Link " + mUrl);
        }
        mUrl = mOriginUrl;
    }


    //-----------------------------------------MediaPlayerListener------------------------------------------------

    /**
     * 暂停状态
     */
    @Override
    public void onVideoPause() {
        if (mCurrentState == CURRENT_STATE_PREPAREING) {
            mPauseBeforePrepared = true;
        }
        try {
            if (getVideoManager() != null &&
                    getVideoManager().isPlaying()) {
                setStateAndUi(CURRENT_STATE_PAUSE);
                mCurrentPosition = getVideoManager().getCurrentPosition();
                if (getVideoManager() != null)
                    getVideoManager().pause();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 恢复暂停状态
     */
    @Override
    public void onVideoResume() {
        onVideoResume(false);
    }

    /**
     * 恢复暂停状态
     *
     * @param seek 是否产生seek动作
     */
    @Override
    public void onVideoResume(boolean seek) {
        mPauseBeforePrepared = false;
        if (mCurrentState == CURRENT_STATE_PAUSE) {
            try {
                if (mCurrentPosition >= 0 && getVideoManager() != null) {
                    if (seek) {
                        getVideoManager().seekTo(mCurrentPosition);
                    }
                    getVideoManager().start();
                    setStateAndUi(CURRENT_STATE_PLAYING);
                    if (mAudioManager != null && !mReleaseWhenLossAudio) {
                        mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    }
                    mCurrentPosition = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 准备完成状态
     */
    @Override
    public void onPrepared() {
        if (mCurrentState != CURRENT_STATE_PREPAREING) return;
        mHadPrepared = true;
        if (mVideoAllCallBack != null && isCurrentMediaListener()) {
            Logger.d("onPrepared");
            mVideoAllCallBack.onPrepared(mOriginUrl, mTitle, this);
        }
        //不自动播放，立马暂停
        if (!mStartAfterPrepared) {
            setStateAndUi(CURRENT_STATE_PAUSE);
            onVideoPause();
        }
        //自动播放
        else {
            startAfterPrepared();
        }
    }

    /**
     * 播放完成状态
     */
    @Override
    public void onAutoCompletion() {
        setStateAndUi(CURRENT_STATE_AUTO_COMPLETE);
        mSaveChangeViewTime = 0;
        mCurrentPosition = 0;
        if (mRenderViewContainer.getChildCount() > 0) {
            mRenderViewContainer.removeAllViews();
        }
        if (!mIfCurrentIsFullscreen)
            getVideoManager().setLastListener(null);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        if (mContext instanceof Activity) {
            try {
                ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        releaseNetWorkState();
        if (mVideoAllCallBack != null && isCurrentMediaListener()) {
            Logger.d("onAutoComplete");
            mVideoAllCallBack.onAutoComplete(mOriginUrl, mTitle, this);
        }
        mHadPlay = false;
    }

    /**
     * 播放器释放状态
     */
    @Override
    public void onCompletion() {
        //先setState
        setStateAndUi(CURRENT_STATE_NORMAL);
        mSaveChangeViewTime = 0;
        mCurrentPosition = 0;
        if (mRenderViewContainer.getChildCount() > 0) {
            mRenderViewContainer.removeAllViews();
        }
        if (!mIfCurrentIsFullscreen) {
            getVideoManager().setListener(null);
            getVideoManager().setLastListener(null);
        }
        getVideoManager().setCurrentVideoHeight(0);
        getVideoManager().setCurrentVideoWidth(0);
        mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        if (mContext instanceof Activity) {
            try {
                ((Activity) mContext).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        releaseNetWorkState();
        mHadPlay = false;
    }

    /**
     * seek完状态
     */
    @Override
    public void onSeekComplete() {
        Logger.d("onSeekComplete");
    }

    /**
     * 播放出错状态
     *
     * @param what  错误类型
     * @param extra 错误码
     */
    @Override
    public void onError(int what, int extra) {
        //网络变化导致出错
        if (mNetChanged) {
            mNetChanged = false;
            netWorkErrorLogic();
            if (mVideoAllCallBack != null) {
                mVideoAllCallBack.onPlayError(mOriginUrl, mTitle, this);
            }
            return;
        }
        //38：调用start的时机不对
        if (what != 38 && what != -38) {
            setStateAndUi(CURRENT_STATE_ERROR);
            deleteCacheFileWhenError();
            if (mVideoAllCallBack != null) {
                mVideoAllCallBack.onPlayError(mOriginUrl, mTitle, this);
            }
        }
    }

    @Override
    public void onInfo(int what, int extra) {
        //内存没有数据了，媒体信息开始缓冲
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            mBackUpPlayingBufferState = mCurrentState;
            //避免在onPrepared之前就进入了buffering，导致一直loading
            if (mHadPlay && mCurrentState != CURRENT_STATE_PREPAREING && mCurrentState > 0)
                setStateAndUi(CURRENT_STATE_PLAYING_BUFFERING_START);
        }
        //缓冲下来新数据，媒体信息缓冲结束
        else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            if (mBackUpPlayingBufferState != -1) {
                //缓冲结束可以，开始播放了
                if (mBackUpPlayingBufferState == CURRENT_STATE_PLAYING_BUFFERING_START) {
                    mBackUpPlayingBufferState = CURRENT_STATE_PLAYING;
                }
                if (mHadPlay && mCurrentState != CURRENT_STATE_PREPAREING && mCurrentState > 0)
                    setStateAndUi(mBackUpPlayingBufferState);
                mBackUpPlayingBufferState = -1;
            }
        }
        //媒体信息视频旋转改变
        else if (what == getVideoManager().getRotateInfoFlag()) {
            mRotate = extra;
            Logger.d("Video Rotate Info " + extra);
            if (mRenderView != null)
                mRenderView.setRotation(mRotate);
        }
    }

    /**
     * 尺寸发生改变
     */
    @Override
    public void onVideoSizeChanged() {
        int mVideoWidth = getVideoManager().getCurrentVideoWidth();
        int mVideoHeight = getVideoManager().getCurrentVideoHeight();
        if (mVideoWidth != 0 && mVideoHeight != 0 && mRenderView != null) {
            mRenderView.requestLayout();
        }
    }

    /**
     * 设置播放绘制
     */
    @Override
    protected void setDisplay(Surface surface) {
        getVideoManager().setDisplay(surface);
    }

    /**
     * 释放surface
     */
    @Override
    protected void releaseSurface(Surface surface) {
        getVideoManager().releaseSurface(surface);
    }

    /**
     * 针对IjkMediaPlayer内核的bug处理，暂停逻辑
     */
    @Override
    protected void showPauseCover() {
        if (getVideoManager().getPlayer() != null && getVideoManager().getPlayer().getMediaPlayer() instanceof IjkMediaPlayer) {
            //强制刷新一帧画面
            getVideoManager().getPlayer().getMediaPlayer().imageRefresh();
        }
    }

    /**
     * 针对IjkMediaPlayer内核的bug处理，暂停逻辑
     */
    @Override
    protected void releasePauseCover() {

    }

    //-----------------------------------------MediaPlayerListener------------------------------------------------

    /**
     * 清除当前缓存
     */
    private void clearCurrentCache() {
        if (getVideoManager().isCacheFile() && mCache) {
            //是否为缓存文件
            Logger.d("Play Error " + mUrl);
            mUrl = mOriginUrl;
            getVideoManager().clearCache(mContext, mCachePath, mOriginUrl);
        } else if (mUrl.contains("127.0.0.1")) {
            getVideoManager().clearCache(getContext(), mCachePath, mOriginUrl);
        }

    }

    /**
     * 获取当前播放进度
     */
    public int getCurrentPositionWhenPlaying() {
        int position = 0;
        if (mCurrentState == CURRENT_STATE_PLAYING || mCurrentState == CURRENT_STATE_PAUSE) {
            try {
                position = (int) getVideoManager().getCurrentPosition();
            } catch (Exception e) {
                e.printStackTrace();
                return position;
            }
        }
        if (position == 0 && mCurrentPosition > 0) {
            return (int) mCurrentPosition;
        }
        return position;
    }

    /**
     * 获取当前总时长
     */
    public int getDuration() {
        int duration = 0;
        try {
            duration = (int) getVideoManager().getDuration();
        } catch (Exception e) {
            e.printStackTrace();
            return duration;
        }
        return duration;
    }

    /**
     * 释放播放器
     */
    public void release() {
        mSaveChangeViewTime = 0;
        if (isCurrentMediaListener() &&
                (System.currentTimeMillis() - mSaveChangeViewTime) > CHANGE_DELAY_TIME) {
            releaseVideos();
            //注销网络广播监听
            unListenerNetWorkState();
        }
    }

    /**
     * prepared成功之后会开始播放
     */
    protected void startAfterPrepared() {
        //是否准备完成，未完成则准备加载资源
        if (!mHadPrepared) {
            prepareVideo();
        }
        try {
            //开始播放
            if (getVideoManager() != null) {
                getVideoManager().start();
            }
            setStateAndUi(CURRENT_STATE_PLAYING);
            if (getVideoManager() != null && mSeekOnStart > 0) {
                getVideoManager().seekTo(mSeekOnStart);
                mSeekOnStart = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //添加Render渲染层
        addTextureView();
        //创建网络监听
        createNetWorkState();
        listenerNetWorkState();
        mHadPlay = true;
        //是否准备完成前调用了暂停
        if (mPauseBeforePrepared) {
            onVideoPause();
            mPauseBeforePrepared = false;
        }
    }

    /**
     * 退出window层播放全屏效果会触发不等于返回false
     */
    protected boolean isCurrentMediaListener() {
        return getVideoManager().listener() != null
                && getVideoManager().listener() == this;
    }

    /**
     * 创建网络监听
     */
    protected void createNetWorkState() {
        if (mNetInfoModule == null) {
            mNetInfoModule = new NetInfoModule(mContext.getApplicationContext(), new NetInfoModule.NetChangeListener() {
                @Override
                public void changed(String state) {
                    if (!mNetSate.equals(state)) {
                        Logger.d("******* change network state ******* " + state);
                        mNetChanged = true;
                    }
                    mNetSate = state;
                }
            });
            mNetSate = mNetInfoModule.getCurrentConnectionType();
        }
    }

    /**
     * 启动android.net.conn.CONNECTIVITY_CHANGE广播监听网络
     */
    private void listenerNetWorkState() {
        if (mNetInfoModule != null) {
            mNetInfoModule.onHostResume();
        }
    }

    /**
     * 取消网络监听广播注册
     */
    protected void unListenerNetWorkState() {
        if (mNetInfoModule != null) {
            mNetInfoModule.onHostPause();
        }
    }

    /**
     * 释放网络监听
     */
    protected void releaseNetWorkState() {
        if (mNetInfoModule != null) {
            mNetInfoModule.onHostPause();
            mNetInfoModule = null;
        }
    }

    /************************* 需要继承处理部分 *************************/

    /**
     * 退出全屏
     *
     * @return 是否在全屏界面
     */
    protected abstract boolean backFromFull(Context context);

    /**
     * 释放播放器
     */
    protected abstract void releaseVideos();

    /**
     * 设置播放显示状态
     */
    protected abstract void setStateAndUi(int state);

    /**
     * 获取管理器桥接的实现
     */
    public abstract VideoViewBridge getVideoManager();

    /**
     * 当前UI
     */
    public abstract int getLayoutId();

    /**
     * 开始播放
     */
    public abstract void startPlayLogic();


    /************************* 公开接口 *************************/

    /**
     * 获取当前播放状态
     */
    public int getCurrentState() {
        return mCurrentState;
    }

    /**
     * 根据状态判断是否播放中
     */
    public boolean isInPlayingState() {
        return (mCurrentState >= 0 && mCurrentState != CURRENT_STATE_NORMAL
                && mCurrentState != CURRENT_STATE_AUTO_COMPLETE && mCurrentState != CURRENT_STATE_ERROR);
    }

    /**
     * 播放tag防止错误，因为普通的url也可能重复
     */
    public String getPlayTag() {
        return mPlayTag;
    }

    /**
     * 播放tag防止错误，因为普通的url也可能重复
     *
     * @param playTag 保证不重复就好
     */
    public void setPlayTag(String playTag) {
        this.mPlayTag = playTag;
    }


    public int getPlayPosition() {
        return mPlayPosition;
    }

    /**
     * 设置播放位置防止错位
     */
    public void setPlayPosition(int playPosition) {
        this.mPlayPosition = playPosition;
    }

    /**
     * 网络速度
     * 注意，这里如果是开启了缓存，因为读取本地代理，缓存成功后还是存在速度的
     * 再打开已经缓存的本地文件，网络速度才会回0.因为是播放本地文件了
     */
    public long getNetSpeed() {
        return getVideoManager().getNetSpeed();
    }

    /**
     * 网络速度
     * 注意，这里如果是开启了缓存，因为读取本地代理，缓存成功后还是存在速度的
     * 再打开已经缓存的本地文件，网络速度才会回0.因为是播放本地文件了
     */
    public String getNetSpeedText() {
        long speed = getNetSpeed();
        return CommonUtil.getTextSpeed(speed);
    }

    public long getSeekOnStart() {
        return mSeekOnStart;
    }

    /**
     * 从哪里开始播放
     * 目前有时候前几秒有跳动问题，毫秒
     * 需要在startPlayLogic之前，即播放开始之前
     */
    public void setSeekOnStart(long seekOnStart) {
        this.mSeekOnStart = seekOnStart;
    }

    /**
     * 缓冲进度
     */
    public int getBuffterPoint() {
        return mBufferPoint;
    }

    /**
     * 是否全屏
     */
    public boolean isIfCurrentIsFullscreen() {
        return mIfCurrentIsFullscreen;
    }

    public void setIfCurrentIsFullscreen(boolean ifCurrentIsFullscreen) {
        this.mIfCurrentIsFullscreen = ifCurrentIsFullscreen;
    }

    public boolean isLooping() {
        return mLooping;
    }

    /**
     * 设置循环
     */
    public void setLooping(boolean looping) {
        this.mLooping = looping;
    }


    /**
     * 设置播放过程中的回调
     */
    public void setVideoAllCallBack(VideoAllCallBack mVideoAllCallBack) {
        this.mVideoAllCallBack = mVideoAllCallBack;
    }

    public float getSpeed() {
        return mSpeed;
    }

    /**
     * 播放速度
     */
    public void setSpeed(float speed) {
        setSpeed(speed, false);
    }

    /**
     * 播放速度
     *
     * @param speed      速度
     * @param soundTouch 是否对6.0下开启变速不变调
     */
    public void setSpeed(float speed, boolean soundTouch) {
        this.mSpeed = speed;
        this.mSoundTouch = soundTouch;
        if (getVideoManager() != null) {
            getVideoManager().setSpeed(speed, soundTouch);
        }
    }

    /**
     * 播放中生效的播放数据
     */
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        setSpeed(speed, soundTouch);
        getVideoManager().setSpeedPlaying(speed, soundTouch);
    }

    public boolean isShowPauseCover() {
        return mShowPauseCover;
    }

    /**
     * 是否需要加载显示暂停的cover图片
     * 打开状态下，暂停退到后台，再回到前台不会显示黑屏，但可以对某些机型有概率出现OOM
     * 关闭情况下，暂停退到后台，再回到前台显示黑屏
     *
     * @param showPauseCover 默认false
     */
    public void setShowPauseCover(boolean showPauseCover) {
        this.mShowPauseCover = showPauseCover;
    }

    /**
     * 跳转到指定位置播放
     */
    public void seekTo(long position) {
        try {
            if (getVideoManager() != null && position > 0) {
                getVideoManager().seekTo(position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isStartAfterPrepared() {
        return mStartAfterPrepared;
    }

    /**
     * 准备成功之后立即播放
     *
     * @param startAfterPrepared 默认true，false的时候需要在prepared后调用startAfterPrepared()
     */
    public void setStartAfterPrepared(boolean startAfterPrepared) {
        this.mStartAfterPrepared = startAfterPrepared;
    }

    public boolean isReleaseWhenLossAudio() {
        return mReleaseWhenLossAudio;
    }

    /**
     * 长时间失去音频焦点，暂停播放器
     *
     * @param releaseWhenLossAudio 默认true，false的时候只会暂停
     */
    public void setReleaseWhenLossAudio(boolean releaseWhenLossAudio) {
        this.mReleaseWhenLossAudio = releaseWhenLossAudio;
    }

    public Map<String, String> getMapHeadData() {
        return mMapHeadData;
    }

    /**
     * 单独设置mapHeader
     */
    public void setMapHeadData(Map<String, String> headData) {
        if (headData != null) {
            this.mMapHeadData = headData;
        }
    }

    public String getOverrideExtension() {
        return mOverrideExtension;
    }

    /**
     * 是否需要覆盖拓展类型，目前只针对exoPlayer内核模式有效
     *
     * @param overrideExtension 比如传入 m3u8,mp4,avi 等类型
     */
    public void setOverrideExtension(String overrideExtension) {
        this.mOverrideExtension = overrideExtension;
    }


}
