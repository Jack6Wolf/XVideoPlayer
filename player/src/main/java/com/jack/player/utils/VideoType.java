package com.jack.player.utils;

/**
 * video的一些默认配置
 *
 * @author jack
 * @since 2020/10/27 10:28
 */
public class VideoType {
    public final static int AR_ASPECT_FIT_PARENT = 0;    //维持原有比例适应窗口，内容完整，可能出现上下黑边
    public final static int AR_ASPECT_FILL_PARENT = 1;   //维持原有比例填充窗口，内容可能缺边，但不出现黑边
    public final static int AR_ASPECT_WRAP_CONTENT = 2;
    public final static int AR_MATCH_PARENT = 3;         //窗口拉伸，没黑边，没有内容缺边，比例失衡
    public final static int AR_16_9_FIT_PARENT = 4;
    public final static int AR_4_3_FIT_PARENT = 5;
    public final static int SCREEN_TYPE_CUSTOM = -5;    //自定义比例，需要设置 sScreenScaleRatio

    /**
     * 启动播放器超时时长
     */
    public final static int START_PLAYER_TIME_OUT = 10000;
    /**
     * SurfaceView，与动画全屏的效果不是很兼容 默认
     */
    public final static int SUFRACE = 1;
    /**
     * TextureView
     */
    public final static int TEXTURE = 0;
    /**
     * 自定义的显示比例
     */
    private static float sScreenScaleRatio = 0;
    /**
     * 渲染类型
     */
    private static int sRenderType = TEXTURE;
    /**
     * 是否使用硬解码优化
     */
    private static boolean sTextureMediaPlay = false;
    /**
     * 显示比例类型
     */
    private static int TYPE = AR_ASPECT_FIT_PARENT;
    /**
     * 硬解码标志
     */
    private static boolean MEDIA_CODEC_FLAG = false;

    /**
     * 使能硬解码渲染优化
     */
    public static void enableMediaCodecTexture() {
        sTextureMediaPlay = true;
    }

    /**
     * 关闭硬解码渲染优化
     */
    public static void disableMediaCodecTexture() {
        sTextureMediaPlay = false;
    }

    /**
     * 是否开启硬解码渲染优化
     */
    public static boolean isMediaCodecTexture() {
        return sTextureMediaPlay;
    }

    public static int getRenderType() {
        return sRenderType;
    }

    /**
     * 渲染控件
     */
    public static void setRenderType(int renderType) {
        sRenderType = renderType;
    }

    public static int getShowType() {
        return TYPE;
    }

    /**
     * 设置显示比例,注意，这是全局生效的
     */
    public static void setShowType(int type) {
        TYPE = type;
    }

    public static float getScreenScaleRatio() {
        return sScreenScaleRatio;
    }

    /***
     * SCREEN_TYPE_CUSTOM 下自定义显示比例
     * @param screenScaleRatio  高宽比，如 16：9
     */
    public static void setScreenScaleRatio(float screenScaleRatio) {
        VideoType.sScreenScaleRatio = screenScaleRatio;
    }

    /**
     * 使能硬解码，播放前设置
     */
    public static void enableMediaCodec() {
        MEDIA_CODEC_FLAG = true;
    }

    /**
     * 关闭硬解码，播放前设置
     */
    public static void disableMediaCodec() {
        MEDIA_CODEC_FLAG = false;
    }

    /**
     * 是否开启硬解码
     */
    public static boolean isMediaCodec() {
        return MEDIA_CODEC_FLAG;
    }
}
