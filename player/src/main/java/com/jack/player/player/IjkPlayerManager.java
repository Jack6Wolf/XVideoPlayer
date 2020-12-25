package com.jack.player.player;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.view.Surface;

import com.jack.player.cache.ICacheManager;
import com.jack.player.model.VideoModel;
import com.jack.player.model.VideoOptionModel;
import com.jack.player.utils.Logger;
import com.jack.player.utils.RawDataSourceProvider;
import com.jack.player.utils.VideoType;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkLibLoader;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * IJKPLayer播放器器
 */

public class IjkPlayerManager extends BasePlayerManager {
    private static final String TAG = "IjkPlayerManager";
    /**
     * log level
     */
    private static int logLevel = Logger.getLogLevel();

    private static IjkLibLoader ijkLibLoader;

    private IjkMediaPlayer mediaPlayer;

    private List<VideoOptionModel> optionModelList = new ArrayList<>();

    private Surface surface;

    public static void setIjkLibLoader(IjkLibLoader ijkLibLoader) {
        IjkPlayerManager.ijkLibLoader = ijkLibLoader;
    }

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void initVideoPlayer(Context context, VideoModel model, List<VideoOptionModel> optionModelList, ICacheManager cacheManager) {
        mediaPlayer = (ijkLibLoader == null) ? new IjkMediaPlayer() : new IjkMediaPlayer(ijkLibLoader);
        Logger.e(TAG, "initVideoPlayer:" + mediaPlayer.hashCode());
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnNativeInvokeListener(new IjkMediaPlayer.OnNativeInvokeListener() {
            @Override
            public boolean onNativeInvoke(int i, Bundle bundle) {
                return true;
            }
        });
        String url = model.getUrl();
        try {
            //开启硬解码
            if (VideoType.isMediaCodec()) {
                Logger.d("enable mediaCodec");
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                mediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
            }
            if (model.isCache() && cacheManager != null) {
                cacheManager.doCacheLogic(context, mediaPlayer, url, model.getMapHeadData(), model.getCachePath());
            } else {
                if (!TextUtils.isEmpty(url)) {
                    Uri uri = Uri.parse(url);
                    if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())) {
                        RawDataSourceProvider rawDataSourceProvider = RawDataSourceProvider.create(context, uri);
                        mediaPlayer.setDataSource(rawDataSourceProvider);
                    } else if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                        ParcelFileDescriptor descriptor;
                        try {
                            descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                            if (descriptor != null) {
                                FileDescriptor fileDescriptor = descriptor.getFileDescriptor();
                                mediaPlayer.setDataSource(fileDescriptor);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        mediaPlayer.setDataSource(url, model.getMapHeadData());
                    }
                }
            }
            mediaPlayer.setLooping(model.isLooping());
            if (model.getSpeed() != 1 && model.getSpeed() > 0) {
                mediaPlayer.setSpeed(model.getSpeed());
            }
            IjkMediaPlayer.native_setLogLevel(logLevel);
            initIJKOption(mediaPlayer, optionModelList);
            initSuccess(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void showDisplay(Surface surface) {
        if (surface == null && mediaPlayer != null) {
            mediaPlayer.setSurface(null);
        } else {
            this.surface = surface;
            if (mediaPlayer != null && surface.isValid()) {
                mediaPlayer.setSurface(surface);
            }
        }
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        if (speed > 0) {
            try {
                if (mediaPlayer != null) {
                    mediaPlayer.setSpeed(speed);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (soundTouch) {
                VideoOptionModel videoOptionModel = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
                optionModelList.add(videoOptionModel);
            }
        }
    }

    @Override
    public void setNeedMute(boolean needMute) {
        if (mediaPlayer != null) {
            if (needMute) {
                mediaPlayer.setVolume(0, 0);
            } else {
                mediaPlayer.setVolume(1, 1);
            }
        }
    }

    @Override
    public void setVolume(float left, float right) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(left, right);
        }
    }

    @Override
    public void releaseSurface() {
        if (surface != null) {
            //surface.release();
            surface = null;
        }
    }

    @Override
    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public int getBufferedPercentage() {
        return -1;
    }

    @Override
    public long getNetSpeed() {
        if (mediaPlayer != null) {
            return mediaPlayer.getTcpSpeed();
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {
        if (mediaPlayer != null && speed > 0) {
            try {
                mediaPlayer.setSpeed(speed);
            } catch (Exception e) {
                e.printStackTrace();
            }
            VideoOptionModel videoOptionModel = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", (soundTouch) ? 1 : 0);
            optionModelList.add(videoOptionModel);
        }
    }

    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    @Override
    public int getVideoWidth() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoWidth();
        }
        return 0;
    }

    @Override
    public int getVideoHeight() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoHeight();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void seekTo(long time) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(time);
        }
    }

    @Override
    public long getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarNum();
        }
        return 1;
    }

    @Override
    public int getVideoSarDen() {
        if (mediaPlayer != null) {
            return mediaPlayer.getVideoSarDen();
        }
        return 1;
    }

    @Override
    public boolean isSurfaceSupportLockCanvas() {
        return true;
    }

    /**
     * 设置ijk options选项
     */
    private void initIJKOption(IjkMediaPlayer ijkMediaPlayer, List<VideoOptionModel> optionModelList) {
        if (optionModelList != null && optionModelList.size() > 0) {
            for (VideoOptionModel videoOptionModel : optionModelList) {
                if (videoOptionModel.getValueType() == VideoOptionModel.VALUE_TYPE_INT) {
                    ijkMediaPlayer.setOption(videoOptionModel.getCategory(),
                            videoOptionModel.getName(), videoOptionModel.getValueInt());
                } else {
                    ijkMediaPlayer.setOption(videoOptionModel.getCategory(),
                            videoOptionModel.getName(), videoOptionModel.getValueString());
                }
            }
            this.optionModelList.addAll(optionModelList);
        }
    }

    public List<VideoOptionModel> getOptionModelList() {
        return optionModelList;
    }

    public void setOptionModelList(List<VideoOptionModel> optionModelList) {
        this.optionModelList = optionModelList;
    }
}