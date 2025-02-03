package com.sp.madproj.Main;

import android.app.Application;
import android.media.MediaPlayer;

import com.sp.madproj.R;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.splash_music);
        mediaPlayer.setVolume(1f, 1f);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
    }
}
