package com.star.videodemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.jack.player.VideoManager;
import com.jack.player.player.IjkPlayerManager;
import com.jack.player.player.PlayerFactory;
import com.jack.player.player.SystemPlayerManager;

public class MainActivity2 extends AppCompatActivity {
    int a = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
    }

    public void click(View view) {
        a++;
        if (a % 2 == 0) {
            PlayerFactory.setPlayManager(SystemPlayerManager.class);
        } else {
            PlayerFactory.setPlayManager(IjkPlayerManager.class);
        }
    }

    public void click1(View view) {
        startActivity(new Intent(this, MainActivity.class));
    }

    public void click2(View view) {
        VideoManager.instance().clearAllDefaultCache(this);
    }
}