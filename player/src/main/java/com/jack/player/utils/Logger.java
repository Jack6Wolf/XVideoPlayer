package com.jack.player.utils;

import android.util.Log;

/**
 * 日志打印工具类
 */
public class Logger {

    private static final String TAG = "[LOGGER]";
    private static int logLevel = Log.ERROR;

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setLogLevel(int level) {
        logLevel = level;
    }

    /**
     * Get The Current Function Name
     *
     * @return
     */
    private static String getFunctionName(StackTraceElement[] sts) {
        if (sts == null) {
            return null;
        }
        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }
            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }
            if (st.getClassName().equals(Logger.class.getName())) {
                continue;
            }
            return "[ " + Thread.currentThread().getName() + ": "
                    + st.getFileName() + ":" + st.getLineNumber() + " "
                    + st.getMethodName() + " ]";
        }
        return null;
    }

    /**
     * Thread.currentThread().getStackTrace();比较耗时？云捕上报出一些ANR，少请求一次看看效果
     *
     * @param sts
     * @return
     */
    private static String getTag(StackTraceElement[] sts) {
        if (sts == null) {
            return null;
        }
        String tag = "StarTimes";
        for (StackTraceElement st : sts) {
            if (st.isNativeMethod()) {
                continue;
            }
            if (st.getClassName().equals(Thread.class.getName())) {
                continue;
            }
            if (st.getClassName().equals(Logger.class.getName())) {
                continue;
            }
            tag = st.getClassName().substring(st.getClassName().lastIndexOf(".") + 1);
            break;
        }
        return tag;
    }

    /**
     * The Log Level:v
     *
     * @param tag
     * @param str
     */
    public static void v(String tag, String str) {
        if (logLevel <= Log.VERBOSE) {
            Log.v(tag, str);
        }
    }

    /**
     * The Log Level:w
     *
     * @param tag
     * @param str
     */
    public static void w(String tag, String str) {
        if (logLevel <= Log.WARN) {
            Log.w(tag, str);
        }
    }

    public static void w(String str) {
        if (logLevel <= Log.WARN) {
            Log.w(TAG, str);
        }
    }

    /**
     * The Log Level:e
     *
     * @param tag
     * @param str
     */
    public static void e(String tag, String str, Throwable e) {
        if (logLevel <= Log.ERROR) {
            Log.e(tag, str, e);
        }
    }

    public static void e(String str, Throwable e) {
        if (logLevel <= Log.ERROR) {
            Log.e(TAG, str, e);
        }
    }

    /**
     * The Log Level:e
     *
     * @param tag
     * @param str
     */
    public static void e(String tag, String str) {
        if (logLevel <= Log.ERROR) {
            Log.e(tag, str);
        }
    }

    public static void e(String str) {
        if (logLevel <= Log.ERROR) {
            Log.e(TAG, str);
        }
    }

    /**
     * The Log Level:d
     *
     * @param tag
     * @param str
     */
    public static void d(String tag, String str) {
        if (logLevel <= Log.DEBUG) {
            Log.d(tag, str);
        }
    }

    public static void d(String str) {
        if (logLevel <= Log.DEBUG) {
            Log.d(TAG, str);
        }
    }

    /**
     * The Log Level:i
     *
     * @param tag
     * @param str
     */
    public static void i(String tag, String str) {
        if (logLevel <= Log.INFO) {
            Log.i(tag, str);
        }
    }

    /**
     * 截断输出日志
     *
     * @param msg
     */
    public static void e_all(String tag, String msg) {
        if (tag == null || tag.length() == 0
                || msg == null || msg.length() == 0)
            return;

        int segmentSize = 3 * 1024;
        long length = msg.length();
        if (length <= segmentSize) {// 长度小于等于限制直接打印
            Log.e(tag, msg);
        } else {
            while (msg.length() > segmentSize) {// 循环分段打印日志
                String logContent = msg.substring(0, segmentSize);
                msg = msg.replace(logContent, "");
                Log.e(tag, logContent);
            }
            Log.e(tag, msg);// 打印剩余日志
        }
    }

}
