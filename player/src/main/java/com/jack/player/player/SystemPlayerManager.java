package com.jack.player.player;

import android.content.Context;
import android.media.AudioManager;
import android.media.PlaybackParams;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.view.Surface;

import com.jack.player.cache.ICacheManager;
import com.jack.player.model.VideoModel;
import com.jack.player.model.VideoOptionModel;
import com.jack.player.utils.Logger;

import java.util.List;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 系统播放器内核：AndroidMediaPlayer
 *
 * @author jack
 * @since 2020/10/24 11:01
 */
public class SystemPlayerManager extends BasePlayerManager {

    private Context context;

    private AndroidMediaPlayer mediaPlayer;

    private Surface surface;

    private boolean release;

    private long lastTotalRxBytes = 0;

    private long lastTimeStamp = 0;

    private boolean isPlaying = false;

    @Override
    public IMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    @Override
    public void initVideoPlayer(Context context, VideoModel model, List<VideoOptionModel> optionModelList, ICacheManager cacheManager) {
        this.context = context.getApplicationContext();
        mediaPlayer = new AndroidMediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        release = false;
        try {
            if (model.isCache() && cacheManager != null) {
                cacheManager.doCacheLogic(context, mediaPlayer, model.getUrl(), model.getMapHeadData(), model.getCachePath());
            } else {
                mediaPlayer.setDataSource(context, Uri.parse(model.getUrl()), model.getMapHeadData());
            }
            mediaPlayer.setLooping(model.isLooping());
            if (model.getSpeed() != 1 && model.getSpeed() > 0) {
                setSpeed(model.getSpeed());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        initSuccess(model);
    }

    @Override
    public void showDisplay(Surface surface) {
        if (surface == null && mediaPlayer != null && !release) {
            mediaPlayer.setSurface(null);
        } else if (surface != null) {
            this.surface = surface;
            if (mediaPlayer != null && surface.isValid() && !release) {
                mediaPlayer.setSurface(surface);
            }
            if (!isPlaying) {
                pause();
            }
        }
    }

    @Override
    public void setSpeed(float speed, boolean soundTouch) {
        setSpeed(speed);
    }

    @Override
    public void setNeedMute(boolean needMute) {
        try {
            if (mediaPlayer != null && !release) {
                if (needMute) {
                    mediaPlayer.setVolume(0, 0);
                } else {
                    mediaPlayer.setVolume(1, 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            release = true;
            mediaPlayer.release();
            mediaPlayer = null;
        }
        lastTotalRxBytes = 0;
        lastTimeStamp = 0;
    }

    @Override
    public int getBufferedPercentage() {
        return -1;
    }

    @Override
    public long getNetSpeed() {
        if (mediaPlayer != null) {
            return getNetSpeed(context);
        }
        return 0;
    }

    @Override
    public void setSpeedPlaying(float speed, boolean soundTouch) {

    }


    @Override
    public void start() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            isPlaying = true;
        }
    }

    @Override
    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            isPlaying = false;
        }
    }

    @Override
    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            isPlaying = false;
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
        return false;
    }

    private void setSpeed(float speed) {
        if (release) {
            return;
        }
        if (mediaPlayer != null && mediaPlayer.getInternalMediaPlayer() != null && mediaPlayer.isPlayable()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PlaybackParams playbackParams = new PlaybackParams();
                    playbackParams.setSpeed(speed);
                    mediaPlayer.getInternalMediaPlayer().setPlaybackParams(playbackParams);
                } else {
                    Logger.d(" not support setSpeed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long getNetSpeed(Context context) {
        if (context == null) {
            return 0;
        }
        // 获取某个网络UID接收字节的总和
        long nowTotalRxBytes = TrafficStats.getUidRxBytes(context.getApplicationInfo().uid) == TrafficStats.UNSUPPORTED ? 0 : (TrafficStats.getTotalRxBytes() / 1024);//转为KB
        long nowTimeStamp = System.currentTimeMillis();
        long calculationTime = (nowTimeStamp - lastTimeStamp);
        if (calculationTime == 0) {
            return calculationTime;
        }
        //毫秒转换
        long speed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000 / calculationTime);
        lastTimeStamp = nowTimeStamp;
        lastTotalRxBytes = nowTotalRxBytes;
        return speed;
    }
}
