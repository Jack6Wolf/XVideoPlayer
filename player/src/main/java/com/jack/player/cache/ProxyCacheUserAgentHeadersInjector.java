package com.jack.player.cache;

import com.danikula.videocache.headers.HeaderInjector;
import com.jack.player.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 for android video cache header
 */
public class ProxyCacheUserAgentHeadersInjector implements HeaderInjector {

    public final static Map<String, String> mMapHeadData = new HashMap<>();

    @Override
    public Map<String, String> addHeaders(String url) {
        Logger.d("****** proxy addHeaders ****** " + mMapHeadData.size());
        return mMapHeadData;
    }
}