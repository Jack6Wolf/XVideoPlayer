package com.star.videodemo;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.jack.player.VideoManager;
import com.jack.player.builder.VideoOptionBuilder;
import com.jack.player.listener.LockClickListener;
import com.jack.player.utils.OrientationUtils;
import com.jack.player.video.StandardVideoPlayer;


public class MainActivity extends AppCompatActivity {
    private StandardVideoPlayer videoPlayer;
    private OrientationUtils orientationUtils;
    private boolean isPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        VideoManager.instance().setDebug(Log.VERBOSE);
        init();
    }

    private void init() {
        videoPlayer = findViewById(R.id.video_player);
        //外部辅助的旋转，帮助全屏
        orientationUtils = new OrientationUtils(this, videoPlayer);
        //初始化不打开外部的旋转
        orientationUtils.setEnable(false);
        //竖屏隐藏
        videoPlayer.getBackButton().setVisibility(View.GONE);
        videoPlayer.getTitleTextView().setVisibility(View.GONE);
        String url = "http://9890.vod.myqcloud.com/9890_4e292f9a3dd011e6b4078980237cc3d3.f20.mp4";
        //增加封面
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);
        VideoOptionBuilder videoOption = new VideoOptionBuilder();
        videoOption.setThumbImageView(imageView)
                .setLockLand(false)
                .setAutoFullWithSize(false)
                .setShowFullAnimation(false)
                .setNeedLockFull(true)
                .setFullHideActionBar(true)
                .setFullHideStatusBar(true)
                .setUrl(url)
                .setCacheWithPlay(true)
                .setVideoTitle("测试视频")
                .setVideoAllCallBack(new SampleCallBack() {
                    @Override
                    public void onPrepared(String url, Object... objects) {
                        super.onPrepared(url, objects);
                        //开始播放了才能旋转和全屏
                        orientationUtils.setEnable(true);
                        isPlay = true;
                    }
                })
                .setLockClickListener(new LockClickListener() {
                    @Override
                    public void onClick(View view, boolean lock) {
                        if (orientationUtils != null) {
                            //配合下方的onConfigurationChanged
                            orientationUtils.setEnable(!lock);
                        }
                    }
                })
                .build(videoPlayer);
        //设置完自动播放
        videoPlayer.startPlayLogic();
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoManager.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VideoManager.releaseAllVideos();
    }

    @Override
    public void onBackPressed() {
        if (orientationUtils != null) {
            orientationUtils.backToPortraitVideo();
        }
        if (VideoManager.backFromWindowFull(this)) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isPlay) {
            videoPlayer.onConfigurationChanged(this, newConfig, orientationUtils);
        }
    }

}