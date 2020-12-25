package com.jack.player.listener;


import com.jack.player.model.VideoModel;

import tv.danmaku.ijk.media.player.IMediaPlayer;

/**
 * 播放器初始化成功回调
 */
public interface IPlayerInitSuccessListener {
    void onPlayerInitSuccess(IMediaPlayer player, VideoModel model);
}
