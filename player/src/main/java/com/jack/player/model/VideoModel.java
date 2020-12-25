package com.jack.player.model;

import java.io.File;
import java.util.Map;

/**
 * 视频内部接受数据model
 */

public class VideoModel {

    /**
     * 视频url
     */
    String url;
    /**
     * 缓存地址
     */
    File mCachePath;
    /**
     * 播放请求头
     */
    Map<String, String> mapHeadData;
    /**
     * 播放倍数
     */
    float speed = 1;
    /**
     * 是否循环播放
     */
    boolean looping;
    /**
     * 是否使用缓存
     */
    boolean isCache;
    /**
     * 其他扩展
     */
    String overrideExtension;

    public VideoModel(String url, Map<String, String> mapHeadData, boolean loop, float speed, boolean isCache, File cachePath, String overrideExtension) {
        this.url = url;
        this.mapHeadData = mapHeadData;
        this.looping = loop;
        this.speed = speed;
        this.isCache = isCache;
        this.mCachePath = cachePath;
        this.overrideExtension = overrideExtension;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getMapHeadData() {
        return mapHeadData;
    }

    public void setMapHeadData(Map<String, String> mapHeadData) {
        this.mapHeadData = mapHeadData;
    }

    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean cache) {
        isCache = cache;
    }

    public File getCachePath() {
        return mCachePath;
    }

    public void setCachePath(File cachePath) {
        this.mCachePath = cachePath;
    }

    public String getOverrideExtension() {
        return overrideExtension;
    }

    public void setOverrideExtension(String overrideExtension) {
        this.overrideExtension = overrideExtension;
    }
}
