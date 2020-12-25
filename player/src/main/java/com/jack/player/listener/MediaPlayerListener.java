package com.jack.player.listener;

/**
 * 媒体播放监听
 *
 * @author jack
 * @since 2020/10/27 14:22
 */
public interface MediaPlayerListener {
    /**
     * 播放器准备资源完毕
     */
    void onPrepared();

    /**
     * 播放媒体资源完成
     */
    void onAutoCompletion();

    /**
     * 播放器所有工作完成，一般时机释放资源播放器
     */
    void onCompletion();

    /**
     * 媒体资源缓冲进度百分比
     *
     * @param percent 百分比
     */
    void onBufferingUpdate(int percent);

    /**
     * seek完毕
     */
    void onSeekComplete();

    /**
     * 播放器失败返回信息
     *
     * @param what  错误类型
     * @param extra 错误码
     */
    void onError(int what, int extra);

    /**
     * 播放信息返回码监听
     *
     * @param what  播放信息返回码
     * @param extra 扩展
     */
    void onInfo(int what, int extra);

    /**
     * 播放器size变化
     */
    void onVideoSizeChanged();

    /**
     * 退出全屏
     */
    void onBackFullscreen();

    /**
     * 播放暂停
     */
    void onVideoPause();

    /**
     * 播放继续
     */
    void onVideoResume();

    /**
     * 继续播放是否需要跳转，针对直播
     *
     * @param seek
     */
    void onVideoResume(boolean seek);
}
