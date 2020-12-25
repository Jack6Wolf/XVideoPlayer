package com.jack.player.video;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jack.player.render.RenderView;
import com.jack.player.render.listener.ISurfaceListener;
import com.jack.player.utils.Logger;
import com.jack.player.utils.MeasureHelper;
import com.jack.player.utils.VideoType;

/**
 * 渲染层，搭载SufaceView/TextureView
 *
 * @author jack
 * @since 2020/10/27 11:49
 */
public abstract class PlayerRenderView extends FrameLayout implements ISurfaceListener, MeasureHelper.MeasureFormVideoParamsListener {
    private static final String TAG = "PlayerRenderView";
    //native绘制
    protected Surface mSurface;

    //渲染控件
    protected RenderView mRenderView;

    //渲染控件父类
    protected ViewGroup mRenderViewContainer;
    //画面选择角度
    protected int mRotate;

    public PlayerRenderView(@NonNull Context context) {
        super(context);
    }

    public PlayerRenderView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerRenderView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /******************** start render  listener****************************/
    @Override
    public void onSurfaceAvailable(Surface surface) {
        mSurface = surface;
        setDisplay(mSurface);
        pauseLogic();
        Logger.d(TAG, "onSurfaceAvailable");
    }

    @Override
    public void onSurfaceSizeChanged(Surface surface, int width, int height) {
        Logger.d(TAG, "onSurfaceSizeChanged");
    }

    @Override
    public boolean onSurfaceDestroyed(Surface surface) {
        //清空释放
        setDisplay(null);
        //同一消息队列中去release
        releaseSurface(surface);
        Logger.d(TAG, "onSurfaceDestroyed");
        return true;
    }

    @Override
    public void onSurfaceUpdated(Surface surface) {
        //如果播放的是暂停全屏了
        releasePauseCover();
        Logger.d(TAG, "onSurfaceUpdated");
    }

    /******************** end render listener****************************/


    /**
     * 针对IjkMediaPlayer内核的bug处理，暂停逻辑
     */
    protected void pauseLogic() {
        //显示暂停切换显示的图片
        showPauseCover();
    }

    /**
     * 针对IjkMediaPlayer内核的bug处理，暂停逻辑
     * TextureView暂停时使用绘制画面显示暂停、避免黑屏
     */
    protected abstract void showPauseCover();

    /**
     * 针对IjkMediaPlayer内核的bug处理，暂停逻辑
     * 清除暂停画面
     */
    protected abstract void releasePauseCover();

    /**
     * 添加播放的view
     * 继承后重载addTextureView，继承GSYRenderView后实现自己的IGSYRenderView类，既可以使用自己自定义的显示层
     */
    protected void addTextureView() {
        mRenderView = new RenderView();
        mRenderView.addView(getContext(), mRenderViewContainer, (int) getRotation(), this, this);
    }

    /**
     * 获取布局参数
     *
     * @return
     */
    protected int getTextureParams() {
        boolean typeChanged = (VideoType.getShowType() != VideoType.AR_ASPECT_FIT_PARENT);
        return (typeChanged) ? ViewGroup.LayoutParams.WRAP_CONTENT : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    /**
     * 调整TextureView去适应比例变化
     */
    protected void changeTextureViewShowType() {
        if (mRenderView != null) {
            int params = getTextureParams();
            ViewGroup.LayoutParams layoutParams = mRenderView.getLayoutParams();
            layoutParams.width = params;
            layoutParams.height = params;
            mRenderView.setLayoutParams(layoutParams);
        }
    }


    /**
     * 获取渲染的代理层
     */
    public RenderView getRenderProxy() {
        return mRenderView;
    }

    //设置播放
    protected abstract void setDisplay(Surface surface);

    //释放
    protected abstract void releaseSurface(Surface surface);
}
