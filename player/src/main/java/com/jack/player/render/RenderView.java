package com.jack.player.render;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.jack.player.render.listener.ISurfaceListener;
import com.jack.player.render.view.IRenderView;
import com.jack.player.render.view.SurfaceRenderView;
import com.jack.player.render.view.TextureRenderView;
import com.jack.player.utils.MeasureHelper;
import com.jack.player.utils.VideoType;

/**
 * render绘制中间控件
 *
 * @author jack
 * @since 2020/10/27 10:23
 */
public class RenderView {

    protected IRenderView mShowView;

    public static void addToParent(ViewGroup textureViewContainer, View render) {
        int params = getTextureParams();
        if (textureViewContainer instanceof RelativeLayout) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(params, params);
            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            textureViewContainer.addView(render, layoutParams);
        } else if (textureViewContainer instanceof FrameLayout) {
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(params, params);
            layoutParams.gravity = Gravity.CENTER;
            textureViewContainer.addView(render, layoutParams);
        } else if (textureViewContainer instanceof LinearLayout) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(params, params);
            layoutParams.gravity = Gravity.CENTER;
            textureViewContainer.addView(render, layoutParams);
        }
    }

    /**
     * 获取布局参数
     */
    public static int getTextureParams() {
        boolean typeChanged = (VideoType.getShowType() != VideoType.AR_ASPECT_FIT_PARENT);
        return (typeChanged) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /*************************RenderView函数*************************/
    public void requestLayout() {
        if (mShowView != null) {
            mShowView.getRenderView().requestLayout();
        }
    }

    public void invalidate() {
        if (mShowView != null)
            mShowView.getRenderView().invalidate();
    }

    public float getRotation() {
        return mShowView.getRenderView().getRotation();
    }

    public void setRotation(float rotation) {
        if (mShowView != null)
            mShowView.getRenderView().setRotation(rotation);
    }

    public int getWidth() {
        return (mShowView != null) ? mShowView.getRenderView().getWidth() : 0;
    }

    public int getHeight() {
        return (mShowView != null) ? mShowView.getRenderView().getHeight() : 0;
    }

    public View getShowView() {
        if (mShowView != null)
            return mShowView.getRenderView();
        return null;
    }

    public ViewGroup.LayoutParams getLayoutParams() {
        return mShowView.getRenderView().getLayoutParams();
    }

    public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if (mShowView != null)
            mShowView.getRenderView().setLayoutParams(layoutParams);
    }

    /**
     * 添加播放的view
     */
    public void addView(Context context, ViewGroup textureViewContainer, int rotate,
                        ISurfaceListener surfaceListener,
                        MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (VideoType.getRenderType() == VideoType.TEXTURE) {
            mShowView = TextureRenderView.addTextureView(context, textureViewContainer, rotate, surfaceListener, videoParamsListener);
        } else {
            mShowView = SurfaceRenderView.addSurfaceView(context, textureViewContainer, rotate, surfaceListener, videoParamsListener);
        }
    }

}
