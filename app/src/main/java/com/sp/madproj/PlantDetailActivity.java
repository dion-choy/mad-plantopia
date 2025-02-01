package com.sp.madproj;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class PlantDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        Intent intent = getIntent();
        String icon = intent.getStringExtra("icon");
        String name = intent.getStringExtra("name");
        if (icon == null || name == null) {
            return;
        }

        ((TextView) findViewById(R.id.plantName)).setText(name);

        findViewById(R.id.backBtn).setOnClickListener((l) -> finish());

        Bitmap plant;
        switch (icon) {
            case "cactus":
                plant = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_cactus);
                break;
            case "upright":
                plant = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_upright);
                break;
            case "flower":
                plant = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_flower);
                break;
            case "vine":
            default:
                plant = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_vine);
        }

        ((ImageView) findViewById(R.id.plantIcon)).setImageBitmap(plant);
    }
}