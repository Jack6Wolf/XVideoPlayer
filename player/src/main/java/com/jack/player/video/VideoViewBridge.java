package com.jack.player.video;

import android.content.Context;
import android.view.Surface;

import com.jack.player.cache.ICacheManager;
import com.jack.player.listener.MediaPlayerListener;
import com.jack.player.player.IPlayerManager;

import java.io.File;
import java.util.Map;

/**
 * 播放器Manager与View之间的接口
 *
 * @author jack
 * @since 2020/10/27 16:41
 */
public interface VideoViewBridge {
    /**
     * 主要为了防止内存泄露
     */
    MediaPlayerListener listener();

    /**
     * 配合listener使用，当从全屏退出时，listener随全屏View生命周期结束，lastListener接替它的工作
     */
    MediaPlayerListener lastListener();

    void setListener(MediaPlayerListener listener);

    void setLastListener(MediaPlayerListener lastListener);

    /**
     * tag和position都是属于标记flag，不参与播放器实际工作，只是用于防止错误等等
     */
    String getPlayTag();

    void setPlayTag(String playTag);

    int getPlayPosition();

    void setPlayPosition(int playPosition);

    /**
     * 开始准备播放
     *
     * @param url         播放url
     * @param mapHeadData 头部信息
     * @param loop        是否循环
     * @param speed       播放速度
     * @param cache       是否缓存
     * @param cachePath   缓存目录，可以为空，为空时使用默认
     */
    void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath);

    /**
     * 开始准备播放
     *
     * @param url               播放url
     * @param mapHeadData       头部信息
     * @param loop              是否循环
     * @param speed             播放速度
     * @param cache             是否缓存
     * @param cachePath         缓存目录，可以为空，为空时使用默认
     * @param overrideExtension 是否需要覆盖拓展类型
     */
    void prepare(final String url, final Map<String, String> mapHeadData, boolean loop, float speed, boolean cache, File cachePath, String overrideExtension);

    /**
     * 获取当前播放内核
     */
    IPlayerManager getPlayer();

    /**
     * 获取缓存内核
     */
    ICacheManager getCacheCore();

    /**
     * 针对某些内核，缓冲百分比
     */
    int getBufferedPercentage();

    /**
     * 释放播放器
     */
    void releaseMediaPlayer();

    /**
     * 获取当前Video宽
     */
    int getCurrentVideoWidth();

    /**
     * 设置当前Video宽
     */
    void setCurrentVideoWidth(int currentVideoWidth);

    /**
     * 获取当前Video高
     */
    int getCurrentVideoHeight();

    /**
     * 设置当前Video高
     */
    void setCurrentVideoHeight(int currentVideoHeight);

    /**
     * 设置渲染
     */
    void setDisplay(Surface holder);

    /**
     * 释放surface
     */
    void releaseSurface(Surface surface);

    /**
     * 播放中的url是否已经缓存
     */
    boolean isCacheFile();

    /**
     * 是否已经完全缓存到本地，主要用于开始播放前判断，是否提示用户
     *
     * @param cacheDir 缓存目录，为空是使用默认目录
     * @param url      指定url缓存
     */
    boolean cachePreview(Context context, File cacheDir, String url);

    /**
     * 清除缓存
     *
     * @param cacheDir 缓存目录，为空是使用默认目录
     * @param url      指定url缓存，为空时清除所有
     */
    void clearCache(Context context, File cacheDir, String url);

    /**
     * 网络速度
     */
    long getNetSpeed();

    /**
     * 播放速度修改
     *
     * @param speed 播放速度
     */
    void setSpeed(float speed, boolean soundTouch);

    /**
     * 播放速度修改
     *
     * @param speed      播放速度
     * @param soundTouch 语速是否变化
     */
    void setSpeedPlaying(float speed, boolean soundTouch);

    /**
     * 获取Rotate选择的flag，目前只有ijk用到
     */
    int getRotateInfoFlag();

    void start();

    void stop();

    void pause();

    int getVideoWidth();

    int getVideoHeight();

    boolean isPlaying();

    void seekTo(long time);

    long getCurrentPosition();

    long getDuration();

    int getVideoSarNum();

    int getVideoSarDen();

    /**
     * Surface是否支持外部lockCanvas，来自定义暂停时的绘制画面
     * exoplayer目前不支持，因为外部lock后，切换surface会导致异常
     */
    boolean isSurfaceSupportLockCanvas();
}

