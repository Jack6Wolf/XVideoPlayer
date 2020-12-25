package com.jack.player.render.view;

import android.view.View;

import com.jack.player.render.listener.ISurfaceListener;
import com.jack.player.utils.MeasureHelper;

/**
 * 渲染View的对外接口
 */
public interface IRenderView {
    /**
     * 当前view高度，必须
     */
    int getSizeH();

    /**
     * 当前view宽度，必须
     */
    int getSizeW();

    /**
     * 实现该接口的view，必须
     */
    View getRenderView();

    ISurfaceListener getISurfaceListener();

    /**
     * suface变化监听，必须
     */
    void setISurfaceListener(ISurfaceListener iSurfaceListener);

    /**
     * 渲染view通过MeasureFormVideoParamsListener获取视频的相关参数，必须
     */
    void setVideoParamsListener(MeasureHelper.MeasureFormVideoParamsListener listener);
}
