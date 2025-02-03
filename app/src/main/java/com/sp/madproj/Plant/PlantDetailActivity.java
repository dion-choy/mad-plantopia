package com.sp.madproj.Plant;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.sp.madproj.R;

public class PlantDetailActivity extends AppCompatActivity {
    private PlantHelper plantHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        if (id == null) {
            finish();
            return;
        }

        plantHelper = new PlantHelper(this);
        Cursor plant = plantHelper.getPlantById(id);
        plant.moveToFirst();

        ((TextView) findViewById(R.id.plantName)).setText(plantHelper.getName(plant));

        findViewById(R.id.backBtn).setOnClickListener((l) -> finish());

        Bitmap plantIcon;
        switch (plantHelper.getIcon(plant)) {
            case "cactus":
                plantIcon = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_cactus);
                break;
            case "upright":
                plantIcon = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_upright);
                break;
            case "flower":
                plantIcon = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_flower);
                break;
            case "vine":
            default:
                plantIcon = CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_vine);
        }

        ((ImageView) findViewById(R.id.plantIcon)).setImageBitmap(plantIcon);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        plantHelper.close();
    }
}