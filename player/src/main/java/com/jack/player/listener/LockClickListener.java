package com.jack.player.listener;

import android.view.View;

/**
 * 锁屏按钮的监听
 */

public interface LockClickListener {
    /**
     * 点击事件
     *
     * @param view 当前view
     * @param lock 是否锁屏
     */
    void onClick(View view, boolean lock);
}
