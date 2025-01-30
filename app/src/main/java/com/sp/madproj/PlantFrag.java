package com.sp.madproj;

import static com.sp.madproj.CanvasView.getBitmapFromVectorDrawable;
import static com.sp.madproj.CanvasView.pxFromDp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class PlantFrag extends Fragment {
    private Animation fromBottomFabAnim;
    private Animation toBottomFabAnim;
    private Animation rotateClockWiseFabAnim;
    private Animation rotateAntiClockWiseFabAnim;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private FloatingActionButton openMenu;
    private FloatingActionButton addPlantBtn;
    private FloatingActionButton addPersonBtn;
    private TextView shade;

    public PlantFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_plant, container, false);

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

        openMenu = view.findViewById(R.id.openMenu);
        openMenu.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (openMenu.getContentDescription().equals("Open options")) {
                            openOptions();
                        } else if (openMenu.getContentDescription().equals("Close options")) {
                            closeOptions();
                        }
                    }
                }
        );

        addPlantBtn = view.findViewById(R.id.addPlant);
        addPlantBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), AddPlantActivity.class);
                        addPlantRes.launch(intent);
                        Toast.makeText(getActivity().getApplicationContext(), "add plant", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        addPersonBtn = view.findViewById(R.id.addPerson);
        addPersonBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(getActivity().getApplicationContext(), "add person", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeOptions();
            }
        });

        canvasHolder = view.findViewById(R.id.canvasHolder);
        return view;
    }

    private RelativeLayout canvasHolder;
    @Override
    public void onResume() {
        super.onResume();
        canvasHolder.removeAllViews();
        CanvasView canvas = new CanvasView(getActivity(), R.drawable.greenhouse);
        canvasHolder.addView(canvas);

        canvas.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
                Context context = getContext();
                Bitmap pot = getBitmapFromVectorDrawable(getActivity(), R.drawable.pot);

                CanvasView.Sprite potSprite3 = new CanvasView.Sprite(pot, pxFromDp(104f, context), pxFromDp(406f, context), pxFromDp(61, context)/pot.getWidth(), false);
                CanvasView.Sprite potSprite2 = new CanvasView.Sprite(pot, pxFromDp(55f, context), pxFromDp(402f, context), pxFromDp(72, context)/pot.getWidth(), false);
                CanvasView.Sprite potSprite1 = new CanvasView.Sprite(pot, pxFromDp(-5.73f, context), pxFromDp(399f, context), 1, false);

                CanvasView.Sprite potSprite4 = new CanvasView.Sprite(pot, pxFromDp(246f, context), pxFromDp(406f, context), pxFromDp(61, context)/pot.getWidth(), false);
                CanvasView.Sprite potSprite5 = new CanvasView.Sprite(pot, pxFromDp(285.28f, context), pxFromDp(402f, context), pxFromDp(72, context)/pot.getWidth(), false);
                CanvasView.Sprite potSprite6 = new CanvasView.Sprite(pot, pxFromDp(335.09f, context), pxFromDp(399f, context), 1, false);

                potSprite3.draw();
                potSprite2.draw();
                potSprite1.draw();

                potSprite4.draw();
                potSprite5.draw();
                potSprite6.draw();
            }
        });
    }


    private final ActivityResultLauncher<Intent> addPlantRes = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == 1
                    && result.getData() != null) {
                String plantName = result.getData().getStringExtra("name");
                String accessToken = result.getData().getStringExtra("accessToken");
                String icon = result.getData().getStringExtra("icon");
//                Toast.makeText(getActivity().getApplicationContext(), imageUri.toString(), Toast.LENGTH_SHORT).show();
                Log.d("result", plantName);
                Log.d("result", accessToken);
                Log.d("result", icon);
            }
        }
    });

    public void openOptions() {
        addPlantBtn.setVisibility(View.VISIBLE);
        addPersonBtn.setVisibility(View.VISIBLE);
        shade.setVisibility(View.VISIBLE);
        openMenu.setContentDescription("Close options");

        shade.startAnimation(fadeInBg);
        openMenu.startAnimation(rotateClockWiseFabAnim);
        addPersonBtn.startAnimation(fromBottomFabAnim);
        addPlantBtn.startAnimation(fromBottomFabAnim);

        shade.setClickable(true);

        openMenu.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        addPlantBtn.setVisibility(View.GONE);
        addPersonBtn.setVisibility(View.GONE);
        shade.setVisibility(View.GONE);
        openMenu.setContentDescription("Open options");

        shade.startAnimation(fadeOutBg);
        openMenu.startAnimation(rotateAntiClockWiseFabAnim);
        addPersonBtn.startAnimation(toBottomFabAnim);
        addPlantBtn.startAnimation(toBottomFabAnim);

        shade.setClickable(false);

        openMenu.setImageResource(R.drawable.icon_add);
    }
}