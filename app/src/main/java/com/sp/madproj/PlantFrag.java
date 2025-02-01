package com.sp.madproj;

import static com.sp.madproj.CanvasView.getBitmapFromVectorDrawable;
import static com.sp.madproj.CanvasView.pxFromDp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import java.util.Arrays;
import java.util.List;

public class PlantFrag extends Fragment {
    private static final int MAX_SIZE = 11;

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

    private PlantHelper plantHelper;

    private CanvasView canvas = null;

    private Bitmap flower;
    private Bitmap upright;
    private Bitmap vine;
    private Bitmap cactus;
    private List<Sprite> pots = null;
    public PlantFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        plantHelper = new PlantHelper(context);

        Bitmap pot = getBitmapFromVectorDrawable(getActivity(), R.drawable.pot);
        flower = getBitmapFromVectorDrawable(getActivity(), R.drawable.plant_flower);
        upright = getBitmapFromVectorDrawable(getActivity(), R.drawable.plant_upright);
        vine = getBitmapFromVectorDrawable(getActivity(), R.drawable.plant_vine);
        cactus = getBitmapFromVectorDrawable(getActivity(), R.drawable.plant_cactus);

        pots = Arrays.asList(
                new Sprite(context, CanvasView.DP, pot, 104f, 406f, pxFromDp(61, context)/ pot.getWidth(), pxFromDp(61, context)/ pot.getWidth(), false),
                new Sprite(context, CanvasView.DP, pot, 246f, 406f, pxFromDp(61, context)/ pot.getWidth(), pxFromDp(61, context)/ pot.getWidth(), false),
                new Sprite(context, CanvasView.DP, pot, 55f, 402f, pxFromDp(72, context)/ pot.getWidth(), pxFromDp(72, context)/ pot.getWidth(), false),
                new Sprite(context, CanvasView.DP, pot, 285.28f, 402f, pxFromDp(72, context)/ pot.getWidth(), pxFromDp(72, context)/ pot.getWidth(), false),
                new Sprite(context, CanvasView.DP, pot, 335.09f, 399f, 1, 1, false),
                new Sprite(context, CanvasView.DP, pot, -5.73f, 399f, 1, 1, false)
        );
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
                view1 -> {
                    if (openMenu.getContentDescription().equals("Open options")) {
                        openOptions();
                    } else if (openMenu.getContentDescription().equals("Close options")) {
                        closeOptions();
                    }
                }
        );

        addPlantBtn = view.findViewById(R.id.addPlant);
        addPlantBtn.setOnClickListener(
                view1 -> {
                    if (plantHelper.getAll().getCount() >= MAX_SIZE) {
                        Toast.makeText(getActivity(), "Greenhouse full", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), AddPlantActivity.class);
                    addPlantRes.launch(intent);
                    Toast.makeText(getActivity(), "add plant", Toast.LENGTH_SHORT).show();
                }
        );

        addPersonBtn = view.findViewById(R.id.addPerson);
        addPersonBtn.setOnClickListener(
                view2 -> Toast.makeText(getActivity(), "add person", Toast.LENGTH_SHORT).show()
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(view3 -> closeOptions());

        RelativeLayout canvasHolder = view.findViewById(R.id.canvasHolder);
        canvasHolder.removeAllViews();
        canvas = new CanvasView(getActivity(), R.drawable.greenhouse);
        canvasHolder.addView(canvas);
        return view;
    }

    private final float[][] positions = {
            {106, 365, -0.8f},
            {242, 363, 0.8f},
            {56, 355, -0.95f},
            {281, 352, 0.95f},
            {340, 345, 1},
            {-1, 346, -1},
            {152, 591, -0.8f},
            {72, 607, -0.95f},
            {280, 612, 0.95f},
            {343, 661, 1},
            {3, 656, -1},
    };

    public float[] uprightCoords(float[] pos) {
        return new float[]{pos[0]+5, pos[1]-35, pos[2]};
    }
    public float[] flowerCoords(float[] pos) {
        return new float[]{pos[0]+21, pos[1]-4, (float) (pos[2]*0.7)};
    }
    public float[] cactusCoords(float[] pos) {
        return new float[]{pos[0], pos[1], pos[2]};
    }
    public float[] vineCoords(float[] pos) {
        return new float[]{pos[0]-7, pos[1]+10, pos[2]};
    }

    @Override
    public void onResume() {
        super.onResume();
        Context context = getContext();
        if (context == null) {
            return;
        }

        Cursor allPlants = plantHelper.getAll();
        allPlants.moveToFirst();

        float[] pos = {0, 0, 0};
        for (int i = 0; i < allPlants.getCount(); i++) {
            allPlants.moveToPosition(i);
            Log.d("Plants", plantHelper.getName(allPlants));
            Log.d("Plants", ""+plantHelper.getPosition(allPlants));
            Log.d("Plants", plantHelper.getAccessToken(allPlants));
            int position = plantHelper.getPosition(allPlants);
            Bitmap plant = null;
            String icon = plantHelper.getIcon(allPlants);
            switch (icon) {
                case "cactus":
                    plant = cactus;
                    pos = cactusCoords(positions[position]);
                    break;
                case "upright":
                    plant = upright;
                    pos = uprightCoords(positions[position]);
                    break;
                case "flower":
                    plant = flower;
                    pos = flowerCoords(positions[position]);
                    break;
                case "vine":
                    plant = vine;
                    pos = vineCoords(positions[position]);
                    break;
            }

            Sprite sprite = new Sprite(context, CanvasView.DP, plant, pos[0], pos[1], pos[2], Math.abs(pos[2]));

            Intent intent = new Intent(getActivity(), PlantDetailActivity.class);
            intent.putExtra("name", plantHelper.getName(allPlants));
            intent.putExtra("icon", icon);
            intent.putExtra("species", plantHelper.getSpecies(allPlants));

            sprite.setOnClickListener(() -> {
                Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show();
                startActivity(intent);
            });

            canvas.add(sprite);
            if (position < 6) {
                canvas.add(pots.get(position));
            }
            if (icon.equals("vine")) {
                canvas.remove(sprite); //move sprite to top layer
                canvas.add(sprite);
            }
        }

        canvas.invalidate();
    }

    private final ActivityResultLauncher<Intent> addPlantRes = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == 1
                    && result.getData() != null) {
                String plantName = result.getData().getStringExtra("name");
                String accessToken = result.getData().getStringExtra("accessToken");
                String species = result.getData().getStringExtra("species");
                String icon = result.getData().getStringExtra("icon");
//                Toast.makeText(getActivity().getApplicationContext(), imageUri.toString(), Toast.LENGTH_SHORT).show();

                int position;
                do {
                    position = (int) (Math.random() * MAX_SIZE);
                } while (plantHelper.getFilledPos(position).getCount() > 0);

                Log.d("result name", plantName);
                Log.d("result accessToken", accessToken);
                Log.d("result species", species);
                Log.d("result icon", icon);
                Log.d("result position", ""+position);
                plantHelper.insert(position, accessToken, icon, plantName, species);
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