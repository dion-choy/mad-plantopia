package com.sp.madproj.Main;

import android.content.Intent;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sp.madproj.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.splash_music);
        mediaPlayer.setVolume(1f, 1f);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SplashScreen.installSplashScreen(this);

        ImageView splashScreenImg = findViewById(R.id.splashScreen);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ((AnimatedVectorDrawable) splashScreenImg.getDrawable()).start();
            new Handler().postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 1800);
        } else {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}