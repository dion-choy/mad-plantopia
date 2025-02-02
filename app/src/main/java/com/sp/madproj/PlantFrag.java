package com.sp.madproj;

import static com.sp.madproj.CanvasView.getBitmapFromVectorDrawable;
import static com.sp.madproj.CanvasView.pxFromDp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private CardView addPersonContainer;
    private ProgressBar loadingIcon;

    private CaretakersAdapter caretakersAdapter;
    private final List<User> caretakersModel = new ArrayList<>();

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

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

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

        caretakersAdapter = new CaretakersAdapter(caretakersModel);

        RecyclerView caretakersList = view.findViewById(R.id.caretakerList);
        caretakersList.setLayoutManager(new LinearLayoutManager(getActivity()));
        caretakersList.setItemAnimator(new DefaultItemAnimator());
        caretakersList.setAdapter(caretakersAdapter);

        loadingIcon = view.findViewById(R.id.loadingIcon);

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

        addPersonContainer = view.findViewById(R.id.addPersonContainer);

        addPlantBtn = view.findViewById(R.id.addPlant);
        addPlantBtn.setOnClickListener(
                view1 -> {
                    if (view1.getVisibility() == View.GONE) {
                        return;
                    }
                    if (plantHelper.getAll().getCount() >= MAX_SIZE) {
                        Toast.makeText(getActivity(), "Greenhouse full", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), AddPlantActivity.class);
                    addPlantRes.launch(intent);
                }
        );

        addPersonBtn = view.findViewById(R.id.addPerson);
        addPersonBtn.setOnClickListener(
                view1 -> {
                    if (view1.getVisibility() == View.GONE) {
                        return;
                    }
                    addPersonContainer.setVisibility(View.VISIBLE);
                    addPersonContainer.startAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg)
                    );


                    addPersonBtn.startAnimation(toBottomFabAnim);
                    addPlantBtn.startAnimation(toBottomFabAnim);

                    addPlantBtn.setVisibility(View.GONE);
                    addPersonBtn.setVisibility(View.GONE);
                    openMenu.setImageResource(R.drawable.icon_done);
                }
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(view3 -> closeOptions());

        RelativeLayout canvasHolder = view.findViewById(R.id.canvasHolder);
        canvasHolder.removeAllViews();
        canvas = new CanvasView(getActivity(), R.drawable.greenhouse);
        canvasHolder.addView(canvas);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlants();
    }

    private class CaretakersAdapter extends RecyclerView.Adapter<CaretakersAdapter.MembersHolder>{
        List<User> users;
        public CaretakersAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public CaretakersAdapter.MembersHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_add_del_user, parent, false);
            return new CaretakersAdapter.MembersHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CaretakersAdapter.MembersHolder holder, int position) {
            holder.name.setText(users.get(position).username);
            Picasso.get()
                    .load(Uri.parse(users.get(position).pfp))
                    .placeholder(R.mipmap.default_pfp_foreground)
                    .into(holder.pfpIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.pfpIcon.setImageTintList(null);
                        }

                        @Override
                        public void onError(Exception e) {}
                    });


//            String userKey = "";
//            String uid = users.get(holder.getAdapterPosition()).uid;
//            for (Map.Entry<String, String> entry : members.entrySet()) {
//                if (uid.equals(entry.getValue())) {
//                    userKey = entry.getKey();
//                    break;
//                }
//            }

            holder.removeMember.setOnClickListener(view -> {
                Toast.makeText(getActivity(), "Click", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class MembersHolder extends RecyclerView.ViewHolder {
            private final TextView name;
            private final ImageView pfpIcon;
            private final ImageButton removeMember;
            public MembersHolder(View view) {
                super(view);
                name = view.findViewById(R.id.username);
                pfpIcon = view.findViewById(R.id.pfpIcon);
                removeMember = view.findViewById(R.id.removeMember);
            }
        }
    }

    private final ActivityResultLauncher<Intent> addPlantRes = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == 1
                    && result.getData() != null) {
                loadingIcon.setVisibility(View.VISIBLE);

                String plantName = result.getData().getStringExtra("name");
                String accessToken = result.getData().getStringExtra("accessToken");
                String species = result.getData().getStringExtra("species");
                String icon = result.getData().getStringExtra("icon");
                Log.d("Result access token", accessToken);

                int position;
                do {
                    position = (int) (Math.random() * MAX_SIZE);
                } while (plantHelper.getFilledPos(position).getCount() > 0);

                getDetail(accessToken, plantName, icon, species, position);
            }
        }
    });

    public void getDetail(String accessToken, String plantName, String icon, String species, int position) {

        String plantApi = "https://plant.id/api/v3/kb/plants/" + accessToken +
                "?details=common_names,url,description,gbif_id,inaturalist_id,image,synonyms,watering,propagation_methods,best_light_condition,best_soil_type,best_watering&language=en";

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(plantApi, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                loadingIcon.setVisibility(View.GONE);

                plantHelper.insert(position, response.toString(), icon, plantName, species);
                loadPlants();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Plant API Error", "Response Error: " + error.toString());
                loadingIcon.setVisibility(View.GONE);

                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                } else if (error.networkResponse != null && error.networkResponse.statusCode == 429) {
                    Toast.makeText(getActivity(), "Out of credits", Toast.LENGTH_SHORT).show();
                }

                if (error.networkResponse != null) {
                    String bodyStr = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                    Log.d("Plant API Error", bodyStr);
                }

//                Log.d("Plant API Error", "Response Error: " + error.networkResponse.statusCode);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Api-Key", BuildConfig.PLANT_KEY);
                return params;
            }
        };

        queue.add(jsonObjectRequest);
    }

    private void openOptions() {
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
        Log.d("Visibility", addPersonContainer.getVisibility()+"");
        addPersonContainer.setVisibility(View.GONE);
        addPersonContainer.startAnimation(fadeOutBg);
        new Handler().postDelayed(() -> addPersonContainer.clearAnimation(), 300);

        addPlantBtn.setVisibility(View.GONE);
        addPlantBtn.startAnimation(toBottomFabAnim);
        new Handler().postDelayed(() -> addPlantBtn.clearAnimation(), 300);

        addPersonBtn.setVisibility(View.GONE);
        addPersonBtn.startAnimation(toBottomFabAnim);
        new Handler().postDelayed(() -> addPersonBtn.clearAnimation(), 300);

        shade.setVisibility(View.GONE);
        openMenu.setContentDescription("Open options");

        shade.startAnimation(fadeOutBg);
        openMenu.startAnimation(rotateAntiClockWiseFabAnim);

        shade.setClickable(false);

        openMenu.setImageResource(R.drawable.icon_add);
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

    private float[] uprightCoords(float[] pos) {
        return new float[]{pos[0]+5, pos[1]-35, pos[2]};
    }
    private float[] flowerCoords(float[] pos) {
        return new float[]{pos[0]+21, pos[1]-4, (float) (pos[2]*0.7)};
    }
    private float[] cactusCoords(float[] pos) {
        return new float[]{pos[0], pos[1], pos[2]};
    }
    private float[] vineCoords(float[] pos) {
        return new float[]{pos[0]-7, pos[1]+10, pos[2]};
    }

    private void loadPlants() {
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
            Log.d("Plants", plantHelper.getDetail(allPlants));
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
}