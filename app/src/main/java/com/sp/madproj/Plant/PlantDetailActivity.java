package com.sp.madproj.Plant;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.sp.madproj.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;

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
        Cursor plantCursor = plantHelper.getPlantById(id);
        plantCursor.moveToFirst();

        ((TextView) findViewById(R.id.plantName)).setText(plantHelper.getName(plantCursor));

        findViewById(R.id.backBtn).setOnClickListener((l) -> finish());

        Bitmap plantIcon;
        switch (plantHelper.getIcon(plantCursor)) {
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

        try {
            JSONObject details = new JSONObject(plantHelper.getDetail(plantCursor));
            ((TextView) findViewById(R.id.speciesName)).setText(details.getString("name"));
            ((TextView) findViewById(R.id.description)).setText(
                    details.getJSONObject("description")
                            .getString("value")
            );
            ((TextView) findViewById(R.id.light)).setText(details.getString("best_light_condition"));
            ((TextView) findViewById(R.id.watering)).setText(details.getString("best_watering"));
            ((TextView) findViewById(R.id.soil)).setText(details.getString("best_soil_type"));
            Picasso.get()
                    .load(Uri.parse(details.getJSONObject("image").getString("value")))
                    .into((ImageView) findViewById(R.id.exampleImg));
            String url = details.getString("url");
            findViewById(R.id.moreInfo).setOnClickListener(view -> {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)));
            });
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        ((TextView) findViewById(R.id.lastWatered)).setText("Last Watered: " + plantHelper.getTimestamp(plantCursor));
        findViewById(R.id.watered).setOnClickListener(view -> {
            plantHelper.update(plantHelper.getID(plantCursor), plantHelper.getPosition(plantCursor),
                    plantHelper.getDetail(plantCursor), plantHelper.getIcon(plantCursor),
                    plantHelper.getName(plantCursor), LocalDate.now().toString(), this, true
            );

            new Handler().postDelayed(() -> {
                ((TextView) findViewById(R.id.lastWatered)).setText("Last Watered: " + LocalDate.now().toString());
            }, 100);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        plantHelper.close();
    }
}