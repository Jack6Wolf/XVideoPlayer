package com.jack.player.render.listener;

import android.view.Surface;

/**
 * Surface状态变化回调
 */

public interface ISurfaceListener {
    /**
     * 创建Surface
     */
    void onSurfaceAvailable(Surface surface);

    /**
     * 创建Surface尺寸发生改变 如横竖屏切换
     */
    void onSurfaceSizeChanged(Surface surface, int width, int height);

    /**
     * Surface销毁
     */
    boolean onSurfaceDestroyed(Surface surface);

    /**
     * 接收一帧数据，就回调一次。TextureView#onSurfaceTextureUpdated()
     */
    void onSurfaceUpdated(Surface surface);
}
