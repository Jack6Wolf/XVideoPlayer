package com.jack.player;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Surface;

import com.jack.player.cache.CacheFactory;
import com.jack.player.cache.ICacheManager;
import com.jack.player.listener.IPlayerInitSuccessListener;
import com.jack.player.listener.MediaPlayerListener;
import com.jack.player.model.VideoModel;
import com.jack.player.model.VideoOptionModel;
import com.jack.player.player.BasePlayerManager;
import com.jack.player.player.IPlayerManager;
import com.jack.player.player.PlayerFactory;
import com.jack.player.utils.Logger;
import com.jack.player.utils.VideoType;
import com.jack.player.video.VideoViewBridge;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.player.IMediaPlayer;


/**
 * 基类管理器，将Player播放器和View结合起来
 * VideoViewBridge接口说明可以查阅VideoViewBridge类
 *
 * @author jack
 * @since 2020/10/27 16:48
 */
public abstract class VideoBaseManager implements IMediaPlayer.OnPreparedListener, IMediaPlayer.OnCompletionListener,
        IMediaPlayer.OnBufferingUpdateListener, IMediaPlayer.OnSeekCompleteListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnVideoSizeChangedListener, IMediaPlayer.OnInfoListener, ICacheManager.ICacheAvailableListener, VideoViewBridge {
    /**
     * 播放器媒体资源准备完成（可以开始播放了）
     */
    protected static final int HANDLER_PREPARE = 0;
    /**
     * 释放播放器、缓存资源...
     */
    protected static final int HANDLER_RELEASE = 2;
    /**
     * 释放surface
     */
    protected static final int HANDLER_RELEASE_SURFACE = 3;
    /**
     * 媒体资源播放、启动超时错误码
     */
    protected static final int BUFFER_TIME_OUT_ERROR = -192;//外部超时错误码

    protected Context context;
    /**
     * 切换回主线程Handler：HANDLER_PREPARE、HANDLER_RELEASE、HANDLER_RELEASE_SURFACE
     */
    protected MediaHandler mMediaHandler;
    /**
     * 切换回主线程Handler：超时、onPrepared、onAutoCompletion、onBufferingUpdate、onSeekComplete、onError、onInfo、onVideoSizeChanged
     */
    protected Handler mainThreadHandler;
    /**
     * 横竖屏媒体资源播放回调
     */
    protected WeakReference<MediaPlayerListener> listener;
    /**
     * 横屏退出onBackFullscreen回调，再次切换横屏最终会代替listener
     */
    protected WeakReference<MediaPlayerListener> lastListener;
    /**
     * Player初始化成功回调
     */
    protected IPlayerInitSuccessListener mPlayerInitSuccessListener;

    /**
     * 配置ijk option
     */
    protected List<VideoOptionModel> optionModelList;

    /**
     * 播放的tag，防止错位置，因为普通的url也可能重复
     */
    protected String playTag = "";

    /**
     * 播放内核管理
     */
    protected IPlayerManager playerManager;

    /**
     * 缓存管理
     */
    protected ICacheManager cacheManager;

    /**
     * 当前播放的视频宽的高
     */
    protected int currentVideoWidth = 0;

    /**
     * 当前播放的视屏的高
     */
    protected int currentVideoHeight = 0;

    /**
     * 当前视频的最后状态
     */
    protected int lastState;

    /**
     * 播放的tag，防止错位置，因为普通的url也可能重复
     */
    protected int playPosition = -22;

    /**
     * 缓存百分比
     */
    protected int bufferPoint;
    /**
     * 缓冲百分比
     */
    protected int percent;

    /**
     * 播放超时时长
     */
    protected int timeOut = VideoType.START_PLAYER_TIME_OUT;

    /**
     * 是否需要静音
     */
    protected boolean needMute = false;

    /**
     * 是否需要外部超时判断
     */
    protected boolean needTimeOutOther;
    /**
     * 超时播放器启动超时、播放中途缓冲超时回调
     */
    private Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            if (listener() != null) {
                Logger.e("time out for error listener");
                listener().onError(BUFFER_TIME_OUT_ERROR, BUFFER_TIME_OUT_ERROR);
            }
        }
    };

    protected void init() {
        mMediaHandler = new MediaHandler(Looper.getMainLooper());
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }


    protected IPlayerManager getPlayManager() {
        return PlayerFactory.getPlayManager();
    }

    protected ICacheManager getCacheManager() {
        return CacheFactory.getCacheManager();
    }

    //---------------------------------------------IMediaPlayer---------------------------------------------------
    @Override
    public void onPrepared(IMediaPlayer mp) {
        Logger.d("onPrepared:");
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onPrepared();
                }
            }
        });
    }

    @Override
    public void onCompletion(IMediaPlayer mp) {
        Logger.d("onCompletion:");
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onAutoCompletion();
                }
            }
        });
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer mp, final int percent) {
        Logger.d("onBufferingUpdate:" + percent);
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    VideoBaseManager.this.percent = percent;
                    listener().onBufferingUpdate(percent);
                }
            }
        });
    }

    @Override
    public void onSeekComplete(IMediaPlayer mp) {
        Logger.d("onSeekComplete:");
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onSeekComplete();
                }
            }
        });
    }

    @Override
    public boolean onError(IMediaPlayer mp, final int what, final int extra) {
        Logger.d("onError:" + what);
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                cancelTimeOutBuffer();
                if (listener() != null) {
                    listener().onError(what, extra);
                }
            }
        });
        return true;
    }

    @Override
    public boolean onInfo(IMediaPlayer mp, final int what, final int extra) {
        Logger.d("onInfo:" + what);
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (needTimeOutOther) {
                    //媒体信息缓冲开始（buffer没有数据了开始回调）
                    if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                        startTimeOutBuffer();
                    }
                    //媒体信息缓冲结束（buffer里缓冲数据了开始回调）
                    else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                        cancelTimeOutBuffer();
                    }
                }
                if (listener() != null) {
                    listener().onInfo(what, extra);
                }
            }
        });
        return false;
    }

    @Override
    public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sar_num, int sar_den) {
        Logger.d("onVideoSizeChanged:" + width);
        currentVideoWidth = mp.getVideoWidth();
        currentVideoHeight = mp.getVideoHeight();
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener() != null) {
                    listener().onVideoSizeChanged();
                }
            }
        });
    }
    //---------------------------------------------IMediaPlayer end---------------------------------------------------

    /**
     * CacheManager
     *
     * @param cacheFile         缓存File
     * @param url               缓存url
     * @param percentsAvailable 缓存百分比
     */
    @Override
    public void onCacheAvailable(File cacheFile, String url, int percentsAvailable) {
        bufferPoint = percentsAvailable;
        Logger.d("下载缓存中:" + url + "，进度:" + percentsAvailable);
    }


    //--------------------------------------------------VideoViewBridge-------------------------------------------------------
    @Override
    public MediaPlayerListener listener() {
        if (listener == null)
            return null;
        return listener.get();
    }

    @Override
    public MediaPlayerListener lastListener() {
        if (lastListener == null)
            return null;
        return lastListener.get();
    }

    @Override
    public void setListener(MediaPlayerListener listener) {
        if (listener == null)
            this.listener = null;
        else
            this.listener = new WeakReference<>(listener);
    }

    @Override
    public void setLastListener(MediaPlayerListener lastListener) {
        if (lastListener == null)
            this.lastListener = null;
        else
            this.lastListener = new WeakReference<>(lastListener);
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeed(speed, soundTouch);
        }
    }

    @Override
    public void prepare(String url, Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath) {
        prepare(url, mapHeadData, loop, speed, cache, cachePath, null);
    }

    @Override
    public void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension) {
        if (TextUtils.isEmpty(url)) return;
        Message msg = new Message();
        msg.what = HANDLER_PREPARE;
        msg.obj = new VideoModel(url, mapHeadData, loop, speed, cache, cachePath, overrideExtension);
        sendMessage(msg);
        if (needTimeOutOther) {
            startTimeOutBuffer();
        }
    }

    @Override
    public void releaseMediaPlayer() {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE;
        sendMessage(msg);
        playTag = "";
        playPosition = -22;
        bufferPoint = 0;
        setNeedMute(false);
    }

    @Override
    public void setDisplay(Surface holder) {
        showDisplay(holder);
    }

    @Override
    public void releaseSurface(Surface holder) {
        Message msg = new Message();
        msg.what = HANDLER_RELEASE_SURFACE;
        msg.obj = holder;
        sendMessage(msg);
    }

    @Override
    public int getCurrentVideoWidth() {
        return currentVideoWidth;
    }

    @Override
    public void setCurrentVideoWidth(int currentVideoWidth) {
        this.currentVideoWidth = currentVideoWidth;
    }

    @Override
    public int getCurrentVideoHeight() {
        return currentVideoHeight;
    }

    @Override
    public void setCurrentVideoHeight(int currentVideoHeight) {
        this.currentVideoHeight = currentVideoHeight;
    }

    @Override
    public String getPlayTag() {
        return playTag;
    }

    @Override
    public void setPlayTag(String playTag) {
        this.playTag = playTag;
    }

    @Override
    public int getPlayPosition() {
        return playPosition;
    }

    @Override
    public void setPlayPosition(int playPosition) {
        this.playPosition = playPosition;
    }

    @Override
    public boolean isCacheFile() {
        return cacheManager != null && cacheManager.hadCached();
    }

    /**
     * 这里只是用于点击时判断是否已经缓存
     * 所以每次直接通过一个CacheManager对象判断即可
     */
    @Override
    public boolean cachePreview(Context context, File cacheDir, String url) {
        if (getCacheManager() != null) {
            return getCacheManager().cachePreview(context, cacheDir, url);
        }
        return false;
    }

    /**
     * 网络速度ijk.getTcpSpeed
     */
    @Override
    public long getNetSpeed() {
        if (playerManager != null) {
            return playerManager.getNetSpeed();
        }
        return 0;
    }

    @Override
    public void clearCache(Context context, File cacheDir, String url) {
        clearDefaultCache(context, cacheDir, url);
    }

    @Override
    public int getBufferedPercentage() {
        return percent;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        if (playerManager != null) {
            playerManager.setSpeedPlaying(speed, soundTouch);
        }
    }

    @Override
    public IPlayerManager getPlayer() {
        return playerManager;
    }

    @Override
    public ICacheManager getCacheCore() {
        return cacheManager;
    }

    @Override
    public void start() {
        if (playerManager != null) {
            playerManager.start();
        }
    }

    @Override
    public void stop() {
        if (playerManager != null) {
            playerManager.stop();
        }
    }

    @Override
    public void pause() {
        if (playerManager != null) {
            playerManager.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (playerManager != null) {
            return playerManager.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (playerManager != null) {
            return playerManager.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (playerManager != null) {
            return playerManager.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (playerManager != null) {
            playerManager.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (playerManager != null) {
            return playerManager.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (playerManager != null) {
            return playerManager.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (playerManager != null) {
            return playerManager.getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (playerManager != null) {
            return playerManager.getVideoSarDen();
        }
        return 0;
    }

    @Override
    public int getRotateInfoFlag() {
        return IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED;
    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        if (playerManager != null) {
            return playerManager.isSurfaceSupportLockCanvas();
        }
        return false;
    }
    //-----------------------------------------------VideoViewBridge end-----------------------------------------------------


    /**
     * 删除默认所有缓存文件
     */
    public void clearAllDefaultCache(Context context) {
        clearDefaultCache(context, null, null);
    }

    /**
     * 清除缓存
     *
     * @param cacheDir 缓存目录，为空是使用默认目录
     * @param url      指定url缓存，为空时清除所有
     */
    private void clearDefaultCache(Context context, @Nullable File cacheDir, @Nullable String url) {
        if (cacheManager != null) {
            cacheManager.clearCache(context, cacheDir, url);
        } else {
            if (getCacheManager() != null) {
                getCacheManager().clearCache(context, cacheDir, url);
            }
        }
    }

    protected void sendMessage(Message message) {
        mMediaHandler.sendMessage(message);
    }

    /**
     * 初始化播放器，加载媒体资源
     */
    private void initVideo(Message msg) {
        try {
            currentVideoWidth = 0;
            currentVideoHeight = 0;
            if (playerManager != null) {
                playerManager.release();
            }
            playerManager = getPlayManager();
            cacheManager = getCacheManager();
            if (cacheManager != null) {
                cacheManager.setCacheAvailableListener(this);
            }
            if (playerManager instanceof BasePlayerManager) {
                ((BasePlayerManager) playerManager)
                        .setPlayerInitSuccessListener(mPlayerInitSuccessListener);
            }
            if (msg.obj instanceof VideoModel)
                playerManager.initVideoPlayer(context, (VideoModel) msg.obj, optionModelList, cacheManager);
            //是否静音
            setNeedMute(needMute);
            IMediaPlayer mediaPlayer = playerManager.getMediaPlayer();
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setOnBufferingUpdateListener(this);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnInfoListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 启动播放超时机制
     */
    protected void startTimeOutBuffer() {
        // 启动定时
        Logger.e("startTimeOutBuffer");
        mainThreadHandler.postDelayed(mTimeOutRunnable, timeOut);

    }

    /**
     * 取消超时处理机制
     */
    protected void cancelTimeOutBuffer() {
        Logger.d("cancelTimeOutBuffer");
        // 取消定时
        if (needTimeOutOther)
            mainThreadHandler.removeCallbacks(mTimeOutRunnable);
    }

    private void releaseSurface(Message msg) {
        if (msg.obj != null) {
            if (playerManager != null) {
                playerManager.releaseSurface();
            }
        }
    }

    /**
     * 播放器输出视频
     */
    private void showDisplay(Surface surface) {
        if (playerManager != null) {
            playerManager.showDisplay(surface);
        }
    }

    /**
     * 初始化VideoManager必须调用
     */
    public void initContext(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<VideoOptionModel> getOptionModelList() {
        return optionModelList;
    }

    /**
     * 设置IJK视频的option
     */
    public void setOptionModelList(List<VideoOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }

    public boolean isNeedMute() {
        return needMute;
    }

    /**
     * 是否需要静音
     */
    public void setNeedMute(boolean needMute) {
        this.needMute = needMute;
        if (playerManager != null) {
            playerManager.setNeedMute(needMute);
        }
    }

    public int getTimeOut() {
        return timeOut;
    }

    public boolean isNeedTimeOutOther() {
        return needTimeOutOther;
    }

    /**
     * 是否需要在buffer缓冲时，增加外部超时判断
     * <p>
     * 超时后会走onError接口，播放器通过onPlayError回调出
     * <p>
     * 错误码为 ： BUFFER_TIME_OUT_ERROR = -192
     * <p>
     * 由于onError之后执行VideoPlayer的OnError，如果不想触发错误，
     * 可以重载onError，在super之前拦截处理。
     * <p>
     * public void onError(int what, int extra){
     * do you want before super and return;
     * super.onError(what, extra)
     * }
     *
     * @param timeOut          超时时间，毫秒 默认10000
     * @param needTimeOutOther 是否需要启动超时监测机制
     */
    public void setTimeOut(int timeOut, boolean needTimeOutOther) {
        this.timeOut = timeOut;
        this.needTimeOutOther = needTimeOutOther;
    }

    public IPlayerManager getCurPlayerManager() {
        return playerManager;
    }

    public ICacheManager getCurCacheManager() {
        return cacheManager;
    }

    public IPlayerInitSuccessListener getPlayerPreparedSuccessListener() {
        return mPlayerInitSuccessListener;
    }

    /**
     * 播放器初始化后接口
     */
    public void setPlayerInitSuccessListener(IPlayerInitSuccessListener listener) {
        this.mPlayerInitSuccessListener = listener;
    }

    private class MediaHandler extends Handler {

        MediaHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case HANDLER_PREPARE:
                    initVideo(msg);
                    break;
                case HANDLER_RELEASE:
                    if (playerManager != null) {
                        playerManager.release();
                    }
                    if (cacheManager != null) {
                        cacheManager.release();
                    }
                    cancelTimeOutBuffer();
                    break;
                case HANDLER_RELEASE_SURFACE:
                    releaseSurface(msg);
                    break;
                default:
                    break;
            }
        }

    }
}
