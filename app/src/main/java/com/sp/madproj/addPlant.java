package com.sp.madproj;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class addPlant extends AppCompatActivity {
    Animation scaleUp;
    Animation scaleDown;

    ImageButton selectFlower;
    ImageButton selectUpright;
    ImageButton selectVine;
    ImageButton selectCactus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        selectFlower = findViewById(R.id.selectFlower);
        selectUpright = findViewById(R.id.selectUpright);
        selectVine = findViewById(R.id.selectVine);
        selectCactus = findViewById(R.id.selectCactus);

        selectFlower.setOnClickListener(plantSelection);
        selectUpright.setOnClickListener(plantSelection);
        selectVine.setOnClickListener(plantSelection);
        selectCactus.setOnClickListener(plantSelection);

        findViewById(R.id.returnBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    View.OnClickListener plantSelection = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ImageView flowerTick = findViewById(R.id.selectedFlower);
            ImageView uprightTick = findViewById(R.id.selectedUpright);
            ImageView vineTick = findViewById(R.id.selectedVine);
            ImageView cactusTick = findViewById(R.id.selectedCactus);

            flowerTick.clearAnimation();
            uprightTick.clearAnimation();
            vineTick.clearAnimation();
            cactusTick.clearAnimation();
            if (flowerTick.getVisibility() == View.VISIBLE) {
                flowerTick.setVisibility(View.GONE);
                flowerTick.startAnimation(scaleDown);
            }
            if (uprightTick.getVisibility() == View.VISIBLE) {
                uprightTick.setVisibility(View.GONE);
                uprightTick.startAnimation(scaleDown);
            }
            if (vineTick.getVisibility() == View.VISIBLE) {
                vineTick.setVisibility(View.GONE);
                vineTick.startAnimation(scaleDown);
            }
            if (cactusTick.getVisibility() == View.VISIBLE) {
                cactusTick.setVisibility(View.GONE);
                cactusTick.startAnimation(scaleDown);
            }

            int clickId = view.getId();
            if (clickId == R.id.selectFlower) {
                flowerTick.setVisibility(View.VISIBLE);
                flowerTick.startAnimation(scaleUp);
            } else if (clickId == R.id.selectUpright) {
                uprightTick.setVisibility(View.VISIBLE);
                uprightTick.startAnimation(scaleUp);
            } else if (clickId == R.id.selectVine) {
                vineTick.setVisibility(View.VISIBLE);
                vineTick.startAnimation(scaleUp);
            } else if (clickId == R.id.selectCactus) {
                cactusTick.setVisibility(View.VISIBLE);
                cactusTick.startAnimation(scaleUp);
            }
        }
    };
}