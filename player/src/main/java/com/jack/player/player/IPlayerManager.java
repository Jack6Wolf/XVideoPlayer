package com.jack.player.player;

import android.content.Context;
import android.view.Surface;

import com.jack.player.cache.ICacheManager;
import com.jack.player.model.VideoModel;
import com.jack.player.model.VideoOptionModel;

import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 播放器差异管理接口
 */

public interface IPlayerManager {

    IMediaPlayer getMediaPlayer();

    /**
     * 初始化播放内核
     *
     * @param model           播放器所需初始化内容
     * @param optionModelList 配置信息
     * @param cacheManager    缓存管理
     */
    void initVideoPlayer(Context context, VideoModel model, List<VideoOptionModel> optionModelList, ICacheManager cacheManager);

    /**
     * 设置渲染显示
     */
    void showDisplay(Surface surface);

    /**
     * 是否静音
     */
    void setNeedMute(boolean needMute);

    /**
     * 单独设置 setVolume ，和 setNeedMute 互斥 float 0.0 - 1.0
     */
    void setVolume(float left, float right);

    /**
     * 释放渲染
     */
    void releaseSurface();

    /**
     * 释放内核
     */
    void release();

    /**
     * 缓冲进度
     */
    int getBufferedPercentage();

    /**
     * 网络速度
     */
    long getNetSpeed();

    /**
     * 播放中设置速度
     */
    void setSpeedPlaying(float speed, boolean soundTouch);

    /**
     * Surface是否支持外部lockCanvas，来自定义暂停时的绘制画面
     * exoplayer目前不支持，因为外部lock后，切换surface会导致异常
     */
    boolean isSurfaceSupportLockCanvas();

    /**
     * 播放前设置播放速度
     *
     * @param speed      播放速度
     * @param soundTouch 音调是否变化
     */
    void setSpeed(float speed, boolean soundTouch);

    /**
     * 开始播放
     */
    void start();

    /**
     * 停止播放
     */
    void stop();

    /**
     * 暂停播放
     */
    void pause();

    /**
     * 获取Video宽
     */
    int getVideoWidth();

    /**
     * 获取Video高
     */
    int getVideoHeight();

    /**
     * 是否正在播放
     */
    boolean isPlaying();

    /**
     * seek跳转
     */
    void seekTo(long time);

    /**
     * 获取当前播放position
     */
    long getCurrentPosition();

    /**
     * 媒体资源总时长
     */
    long getDuration();

    /**
     * 横向的像素点数
     */
    int getVideoSarNum();

    /**
     * 纵向的像素点数
     */
    int getVideoSarDen();
}
