package com.jack.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.jack.player.utils.CommonUtil;
import com.jack.player.utils.Logger;

/**
 * 将Player播放器和View结合起来管理，单例
 *
 * @author jack
 * @since 2020/10/27 16:48
 */
public class VideoManager extends VideoBaseManager {
    /**
     * 全屏布局View的id
     */
    public static final int FULLSCREEN_ID = R.id.full_id;

    private VideoManager() {
        init();
    }

    /**
     * 单例管理器
     */
    public static VideoManager instance() {
        return SingletonInstance.INSTANCE;
    }

    /**
     * 退出全屏，主要用于返回键
     *
     * @return 当前播放是否全屏
     */
    public static boolean backFromWindowFull(Context context) {
        boolean backFrom = false;
        /**
         * 将Activity下的R.id.content作为父布局，直接添加播放布局进去
         */
        ViewGroup vp = (CommonUtil.scanForActivity(context)).findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(FULLSCREEN_ID);
        //全屏
        if (oldF != null) {
            backFrom = true;
            if (VideoManager.instance().lastListener() != null) {
                //退出全屏操作
                VideoManager.instance().lastListener().onBackFullscreen();
            }
        }
        return backFrom;
    }

    /**
     * 页面销毁了记得调用是否所有的video
     */
    public static void releaseAllVideos() {
        if (VideoManager.instance().listener() != null) {
            VideoManager.instance().listener().onCompletion();
        }
        VideoManager.instance().releaseMediaPlayer();
    }

    /**
     * 暂停播放
     */
    public static void onPause() {
        if (VideoManager.instance().listener() != null) {
            VideoManager.instance().listener().onVideoPause();
        }
    }

    /**
     * 恢复播放
     */
    public static void onResume() {
        if (VideoManager.instance().listener() != null) {
            VideoManager.instance().listener().onVideoResume();
        }
    }

    /**
     * 恢复暂停状态
     *
     * @param seek 是否产生seek动作,直播设置为true
     */
    public static void onResume(boolean seek) {
        if (VideoManager.instance().listener() != null) {
            VideoManager.instance().listener().onVideoResume(seek);
        }
    }

    /**
     * 当前是否全屏状态
     *
     * @return 当前是否全屏状态， true代表是。
     */
    public static boolean isFullState(Activity activity) {
        ViewGroup vp = (CommonUtil.scanForActivity(activity)).findViewById(Window.ID_ANDROID_CONTENT);
        final View full = vp.findViewById(FULLSCREEN_ID);
        return full != null;
    }

    /**
     * 设置调试
     */
    public void setDebug(int logLevel) {
        Logger.setLogLevel(logLevel);
    }

    private static class SingletonInstance {
        @SuppressLint("StaticFieldLeak")
        private static VideoManager INSTANCE = new VideoManager();
    }
}
