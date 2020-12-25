package com.jack.player.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

/**
 * 原生资源文件,一般assets目录下存放
 */
public class RawDataSourceProvider implements IMediaDataSource {
    /**
     * 在AssetManager中一项的文件描述符
     */
    private AssetFileDescriptor mDescriptor;

    private byte[] mMediaBytes;

    public RawDataSourceProvider(AssetFileDescriptor descriptor) {
        this.mDescriptor = descriptor;
    }

    public static RawDataSourceProvider create(Context context, Uri uri) {
        try {
            AssetFileDescriptor fileDescriptor = context.getApplicationContext().getContentResolver().openAssetFileDescriptor(uri, "r");
            return new RawDataSourceProvider(fileDescriptor);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) {
        if (position + 1 >= mMediaBytes.length) {
            return -1;
        }
        int length;
        if (position + size < mMediaBytes.length) {
            length = size;
        } else {
            length = (int) (mMediaBytes.length - position);
            if (length > buffer.length)
                length = buffer.length;
            length--;
        }
        System.arraycopy(mMediaBytes, (int) position, buffer, offset, length);
        return length;
    }

    /**
     * 总字节数
     */
    @Override
    public long getSize() throws IOException {
        //总字节数
        long length = mDescriptor.getLength();
        if (mMediaBytes == null) {
            //为asset创建并返回一个自动关闭的输入流。
            InputStream inputStream = mDescriptor.createInputStream();
            mMediaBytes = readBytes(inputStream);
        }
        return length;
    }

    @Override
    public void close() throws IOException {
        if (mDescriptor != null)
            mDescriptor.close();
        mDescriptor = null;
        mMediaBytes = null;
    }

    @Override
    public void setMediaDataSource(String s) {

    }

    /**
     * 读取字节
     */
    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }
}