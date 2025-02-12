package com.sp.madproj.Plant;

import static android.content.Context.MODE_PRIVATE;
import static com.sp.madproj.Plant.CanvasView.getBitmapFromVectorDrawable;
import static com.sp.madproj.Plant.CanvasView.pxFromDp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.NoConnectionError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.sp.madproj.BuildConfig;
import com.sp.madproj.Classes.User;
import com.sp.madproj.Main.MainActivity;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Database;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

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
    private FloatingActionButton shareBtn;
    private TextView shade;
    private CardView addPersonContainer;
    private ProgressBar loadingIcon;

    private final List<User> caretakersModel = new ArrayList<>();
    private CaretakersAdapter caretakersAdapter;

    private PlantHelper plantHelper;

    private CanvasView canvas = null;

    private SharedPreferences sharedPref;

    private static final String inviteMemberTxt = "Invite caretaker";
    private static final String loginTxt = "Log in first";
    private static final String syncGreenhouseTxt = "Sync my greenhouse";
    private static final String leaveGreenhouseTxt = "Leave greenhouse";

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

        int permissionState = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR);
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_CALENDAR}, 1);
        }

        permissionState = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR);
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.WRITE_CALENDAR}, 1);
        }
    }

    @SuppressLint("ApplySharedPref")
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

        TextInputLayout joinGreenhouseContainer = view.findViewById(R.id.joinGreenhouseContainer);
        Button addCaretakerBtn = view.findViewById(R.id.inviteCaretaker);
        Button joinGreenhouse = view.findViewById(R.id.joinGreenhouse);
        TextInputEditText joinCode = view.findViewById(R.id.joinGreenhouseText);
        joinGreenhouse.setVisibility(View.GONE);
        joinGreenhouse.setOnClickListener(view1 -> {
            if (joinCode.getText() == null || joinCode.getText().toString().isEmpty()) {
                joinCode.setError("Enter a code");
                return;
            }
            checkDbForCode(joinCode.getText().toString());
        });

        sharedPref = requireActivity().getSharedPreferences("greenhouse", MODE_PRIVATE);
        addCaretakerBtn.setOnClickListener(view1 -> {
            switch (addCaretakerBtn.getText().toString()) {
                case loginTxt:
                    ((MainActivity) requireActivity()).navBar.setSelectedItemId(R.id.feedTab);
                    break;
                case syncGreenhouseTxt:
                    String hashStr = String.valueOf(FirebaseAuth.getInstance().getCurrentUser().getUid().hashCode());

                    updateUsersGreenhouse(FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), hashStr);
                    joinGreenhouseContainer.setVisibility(View.GONE);

                    caretakersList.setVisibility(View.VISIBLE);
                    addCaretakerBtn.setText(inviteMemberTxt);

                    sharedPref.edit()
                            .putBoolean("clientChanged", true)
                            .putString("greenhouseId", hashStr)
                            .commit();

                    joinGreenhouse.setVisibility(View.GONE);
                    break;
                case inviteMemberTxt:
                    generateAndDisplayCode();
                    sharedPref.edit()
                            .putBoolean("clientChanged", true)
                            .commit();
                    break;
                case leaveGreenhouseTxt:
                    sharedPref.edit()
                            .remove("greenhouseId")
                            .commit();

                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        Database.queryAstra(getActivity(),
                                "UPDATE plantopia.user_info SET greenhouse_id=NULL WHERE username='"+FirebaseAuth.getInstance().getCurrentUser().getDisplayName()+"'",
                                response -> {},
                                error -> {}
                        );
                    }

                    addCaretakerBtn.setText(syncGreenhouseTxt);
                default:
                    joinGreenhouseContainer.setVisibility(View.VISIBLE);
            }
        });

        addPersonContainer = view.findViewById(R.id.addPersonContainer);

        shareBtn = view.findViewById(R.id.shareBtn);
        shareBtn.setOnClickListener(
                view1 -> {
                    String fname;
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(requireActivity(),
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    1);
                        }

                        fname = "/Pictures/Image-" + LocalDateTime.now().toString().replaceAll(":", ".").replaceAll("\\.", "-") + ".jpg";
                    } else {
                        fname = "/Pictures/Plantopia/Image-" + LocalDateTime.now().toString().replaceAll(":", ".").replaceAll("\\.", "-") + ".jpg";
                    }

                    String root = Environment.getExternalStorageDirectory().toString();
                    File myDir = new File(root);
                    myDir.mkdirs();
                    File file = new File(myDir, fname);
                    if (file.exists()) file.delete();
                    Log.i("SHARE", root + fname);
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        canvas.getBuffer().compress(Bitmap.CompressFormat.JPEG, 100, out);
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    MediaScannerConnection.scanFile(requireContext(), new String[]{file.toString()}, new String[] {"image/jpeg"},
                            new MediaScannerConnection.OnScanCompletedListener() {
                                public void onScanCompleted(String path, Uri uri) {
                                    Log.i("SHARE", uri.toString());

                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out my Greenhouse!");
                                    sendIntent.setType("image/jpeg");

                                    startActivity(Intent.createChooser(sendIntent, null));
                                }
                            });

                }
        );

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
                    getUsers();

                    joinGreenhouseContainer.setVisibility(View.GONE);
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                        addCaretakerBtn.setText(loginTxt);
                        caretakersList.setVisibility(View.GONE);
                    } else if (sharedPref.getString("greenhouseId", null) == null) {
                        addCaretakerBtn.setText(syncGreenhouseTxt);
                        caretakersList.setVisibility(View.GONE);
                        joinGreenhouse.setVisibility(View.VISIBLE);
                        joinGreenhouseContainer.setVisibility(View.VISIBLE);
                    } else {
                        caretakersList.setVisibility(View.VISIBLE);
                        if (String.valueOf(FirebaseAuth.getInstance()
                                .getCurrentUser()
                                .getUid().hashCode())
                                .equals(sharedPref.getString("greenhouseId", null))) {
                            addCaretakerBtn.setText(inviteMemberTxt);
                        } else {
                            addCaretakerBtn.setText(leaveGreenhouseTxt);
                        }
                    }

                    addPersonContainer.setVisibility(View.VISIBLE);
                    addPersonContainer.startAnimation(
                            AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg)
                    );


                    shareBtn.setVisibility(View.GONE);
                    shareBtn.startAnimation(toBottomFabAnim);
                    new Handler().postDelayed(() -> shareBtn.clearAnimation(), 300);

                    addPlantBtn.setVisibility(View.GONE);
                    addPlantBtn.startAnimation(toBottomFabAnim);
                    new Handler().postDelayed(() -> addPlantBtn.clearAnimation(), 300);

                    addPersonBtn.setVisibility(View.GONE);
                    addPersonBtn.startAnimation(toBottomFabAnim);
                    new Handler().postDelayed(() -> addPersonBtn.clearAnimation(), 300);
                    openMenu.setImageResource(R.drawable.icon_done);
                }
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(view1 -> closeOptions());

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        plantHelper.close();
    }

    private void checkDbForCode(String code) {
        Database.queryAstra(getActivity(),
                "SELECT id, detail FROM plantopia.greenhouses WHERE position=-100;",
                response -> {
                    Log.d("join greenhouse", response);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        if (jsonObject.getInt("count") == 0) {
                            Toast.makeText(getActivity(), "Invalid code", Toast.LENGTH_LONG).show();
                            return;
                        }

                        JSONArray rows = jsonObject.getJSONArray("data");
                        for (int i = 0; i<rows.length(); i++) {
                            String greenhouseId = rows.getJSONObject(i).getString("id");
                            if (code.equals(rows.getJSONObject(i).getString("detail"))) {
                                Toast.makeText(getActivity(), "Greenhouse added", Toast.LENGTH_LONG).show();
                                closeOptions();

                                sharedPref.edit()
                                        .putString("greenhouseId", greenhouseId)
                                        .putBoolean("clientChanged", false)
                                        .commit();

                                Database.queryAstra(getActivity(),
                                        "UPDATE plantopia.user_info SET greenhouse_id="+greenhouseId+" WHERE username='"+FirebaseAuth.getInstance().getCurrentUser().getDisplayName()+"'",
                                        response1 -> {},
                                        error -> {}
                                        );
                                return;
                            }
                        }
                        Toast.makeText(getActivity(), "Invalid code", Toast.LENGTH_LONG).show();

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("UPDATE CODE ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void generateAndDisplayCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int) Math.floor(Math.random() * 10));
        }

        Database.queryAstra(getActivity(),
                "UPDATE plantopia.greenhouses SET detail='"+ code +"' WHERE id=" + sharedPref.getString("greenhouseId", null) + " AND position=-100;",
                response -> {
                    Log.d("ROOMS", code.toString());
                    Toast.makeText(getActivity(), "Code: " + code, Toast.LENGTH_LONG).show();
                },
                error -> {
                    Log.e("UPDATE CODE ERROR", error.toString());
                    Log.e("UPDATE CODE ERROR",
                            "UPDATE plantopia.greenhouses SET detail='"+ code +"' WHERE id=" + sharedPref.getString("greenhouseId", null) + " AND position=-100;"
                        );
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
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


            if (users.get(position).uid.hashCode() == Integer.parseInt(sharedPref.getString("greenhouseId", "0"))) {
                holder.removeMember.setVisibility(View.GONE);
            } else {
                holder.removeMember.setVisibility(View.VISIBLE);
            }

            if (!String.valueOf(FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid().hashCode())
                    .equals(sharedPref.getString("greenhouseId", null))) {
                holder.removeMember.setVisibility(View.GONE);
            }

            holder.removeMember.setOnClickListener(view ->
                    Database.queryAstra(getActivity(),
                        "UPDATE plantopia.user_info SET greenhouse_id=NULL WHERE username='"+users.get(position).username+"'",
                        response -> {},
                        error -> {}
                    )
            );
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

    private void getUsers() {
        if (getActivity() == null) {
            return;
        }
        SharedPreferences sharedPref = getActivity().getSharedPreferences("greenhouse", MODE_PRIVATE);
        String greenhouseId = sharedPref.getString("greenhouseId", null);
        if (greenhouseId == null) {
            return;
        }

        Database.queryAstra(getActivity(),
                "SELECT * FROM plantopia.user_info WHERE greenhouse_id=" + greenhouseId + ";",
                response -> {
                    caretakersModel.clear();
                    Log.d("Get users", "Success");
                    Log.d("Get users", response);
                    try {
                        JSONArray rows = new JSONObject(response).getJSONArray("data");
                        for (int i = 0; i<rows.length(); i++) {
                            JSONObject row = rows.getJSONObject(i);
                            caretakersModel.add(new User(row.getString("username"), row.getString("uid"), row.getString("pfp")));
                        }
                        caretakersAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("Get users", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
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
                String icon = result.getData().getStringExtra("icon");
                Log.d("Result access token", accessToken);

                int position;
                do {
                    position = (int) (Math.random() * MAX_SIZE);
                } while (plantHelper.getFilledPos(position).getCount() > 0);

                getDetail(accessToken, plantName, icon, position);
            }
        }
    });

    private void updateUsersGreenhouse(String username, String hashStr) {
        Database.queryAstra(getActivity(),
                "UPDATE plantopia.user_info SET greenhouse_id=" + hashStr + " WHERE username = '" + username + "';",
                response -> Log.d("Update greenhouse", "Success"),
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void getDetail(String accessToken, String plantName, String icon, int position) {

        String plantApi = "https://plant.id/api/v3/kb/plants/" + accessToken +
                "?details=common_names,url,description,gbif_id,inaturalist_id,image,synonyms,watering,propagation_methods,best_light_condition,best_soil_type,best_watering&language=en";

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(plantApi,
                response -> {
                    loadingIcon.setVisibility(View.GONE);

                    plantHelper.insert(position, response.toString(), icon, plantName, LocalDate.now().toString(), getActivity(), true);
                    loadPlants();
                },
                error -> {
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
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json");
                params.put("Api-Key", BuildConfig.PLANT_KEY);
                return params;
            }
        };

        queue.add(jsonObjectRequest);
    }

    private void openOptions() {
        shareBtn.setVisibility(View.VISIBLE);
        addPlantBtn.setVisibility(View.VISIBLE);
        addPersonBtn.setVisibility(View.VISIBLE);
        shade.setVisibility(View.VISIBLE);
        openMenu.setContentDescription("Close options");

        shade.startAnimation(fadeInBg);
        openMenu.startAnimation(rotateClockWiseFabAnim);
        shareBtn.startAnimation(fromBottomFabAnim);
        addPersonBtn.startAnimation(fromBottomFabAnim);
        addPlantBtn.startAnimation(fromBottomFabAnim);

        shade.setClickable(true);

        openMenu.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        if (addPersonContainer.getVisibility() != View.GONE) {
            addPersonContainer.setVisibility(View.GONE);
            addPersonContainer.startAnimation(fadeOutBg);
            new Handler().postDelayed(() -> addPersonContainer.clearAnimation(), 300);
        }

        if (getView() != null && getActivity() != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }

        shareBtn.setVisibility(View.GONE);
        shareBtn.startAnimation(toBottomFabAnim);
        new Handler().postDelayed(() -> shareBtn.clearAnimation(), 300);

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

    public void loadPlants() {
        Context context = getContext();
        if (context == null) {
            return;
        }

        canvas.clear();

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
            intent.putExtra("id", plantHelper.getID(allPlants));

            sprite.setOnClickListener(() -> startActivity(intent));

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