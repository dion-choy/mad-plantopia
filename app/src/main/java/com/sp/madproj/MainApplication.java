package com.sp.madproj;

import android.app.Application;
import android.media.MediaPlayer;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.splash_music);
        mediaPlayer.setVolume(1f, 1f);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
    }
}
