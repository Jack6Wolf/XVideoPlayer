package com.jack.player.listener;

/**
 * 播放状态变化监听
 */
public interface StateUiListener {
    /**
     * 状态变化
     *
     * @param state 播放状态VideoView:CURRENT_STATE_NORMAL/CURRENT_STATE_PREPAREING......
     */
    void onStateChanged(int state);
}
