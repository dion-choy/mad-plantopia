package com.sp.madproj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class IdentifyFrag extends Fragment {
    private Animation fromBottomFabAnim;
    private Animation toBottomFabAnim;
    private Animation rotateClockWiseFabAnim;
    private Animation rotateAntiClockWiseFabAnim;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private ActivityResultLauncher<Intent> getImage;

    private FloatingActionButton idPlant;
    private FloatingActionButton openCamBtn;
    private FloatingActionButton openGalleryBtn;
    private TextView shade;


    private RecyclerView identifs;
    private Cursor model;
    private IdentifAdapter idAdapter;
    private IdentificationHelper idHelper;

    public IdentifyFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        idHelper = new IdentificationHelper(getContext());
        model = idHelper.getAll();
        idAdapter = new IdentifAdapter(getContext(), model);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_identify, container, false);

        identifs = view.findViewById(R.id.identifList);
        identifs.setHasFixedSize(true);
        identifs.setLayoutManager(new LinearLayoutManager(getContext()));
        identifs.setItemAnimator(new DefaultItemAnimator());
        identifs.setAdapter(idAdapter);

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

        idPlant = view.findViewById(R.id.idPlant);
        idPlant.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (idPlant.getContentDescription().equals("Open options")) {
                            openOptions();
                        } else if (idPlant.getContentDescription().equals("Close options")) {
                            closeOptions();
                        }
                    }
                }
        );

        openCamBtn = view.findViewById(R.id.openCam);
        openCamBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getImage.launch(new Intent(getActivity(), CamActivtity.class));
                    }
                }
        );

        openGalleryBtn = view.findViewById(R.id.openGallery);
        openGalleryBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        getImage.launch(intent);
                    }
                }
        );

        shade = view.findViewById(R.id.shade);

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivtity.IMAGE_URI
                        && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            Toast.makeText(getActivity().getApplicationContext(), imageUri.toString(), Toast.LENGTH_SHORT).show();
                            Log.d("result", imageUri.toString());

                            getPlantIdAPI(imageUri);

                        }
                    }
                });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (model != null) {
            model.close();
        }

        model = idHelper.getAll();
        idAdapter.swapCursor(model);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        idHelper.close();
    }

    public class IdentifAdapter extends RecyclerView.Adapter<IdentifAdapter.IdentifHolder> {
        private Context context;
        private IdentificationHelper helper = null;
        private Cursor cursor;

        IdentifAdapter(Context context, Cursor cursor) {
            this.context = context;
            this.cursor = cursor;
            helper = new IdentificationHelper(context);
        }

        public void swapCursor(Cursor newCursor) {
            Cursor oldCursor = this.cursor;
            this.cursor = newCursor;
            oldCursor.close();
        }

        @Override
        public IdentifHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.identif_row, parent, false);
            return new IdentifHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IdentifHolder holder, final int position) {
            if (!cursor.moveToPosition(position)) {
                return;
            }

            Log.d("Cursor", cursor.toString());
            holder.species.setText(helper.getSpecies(cursor));
            holder.common.setText(String.format("(%s, %s%%)", helper.getCommon(cursor), helper.getAccuracy(cursor)));
            holder.date.setText(helper.getDate(cursor));

            holder.image.setImageBitmap(helper.getImage(cursor, context));
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        class IdentifHolder extends RecyclerView.ViewHolder {
            private TextView species;
            private TextView common;
            private TextView date;
            private ImageView image;

            public IdentifHolder(View itemView) {
                super(itemView);
                species = itemView.findViewById(R.id.identifSpecies);
                common = itemView.findViewById(R.id.commonName);
                date = itemView.findViewById(R.id.date);
                image = itemView.findViewById(R.id.plantImage);
            }
        }
    }

    public void getPlantIdAPI(Uri imageUri) {
        byte[] bytes = null;
        try {
            bytes = getBytes(getContext().getContentResolver() .openInputStream(imageUri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        idHelper.insert("species", "name", imageUri, "1/2/3", 98.0, getContext());
        idAdapter.notifyItemInserted(idHelper.getAll().getCount()-1);

        String dataUri = "data:" + getContext().getContentResolver().getType(imageUri) +
                ";base64," + Base64.encodeToString(bytes, Base64.DEFAULT);

        String plantApi = "https://plant.id/api/v3/identification?details=common_names,url,description,taxonomy,rank,gbif_id,inaturalist_id,image,synonyms,edible_parts,watering,best_light_condition,best_soil_type,common_uses,cultural_significance,toxicity,best_watering&language=en";

        JSONObject body = new JSONObject();
        try {
            ((MainActivity) getActivity()).updateLocation();
            body.put("images", new JSONArray().put(dataUri))
                    .put("latitiude",  ((MainActivity) getActivity()).getLatitude())
                    .put("longitude", ((MainActivity) getActivity()).getLongitude())
                    .put("similar_images", true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, plantApi, body, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    response.getJSONObject("result");
                    Log.d("Plant API", "Success: " + response.toString());
                } catch (JSONException e) {
                    Log.d("Plant API Error", "Malformed Response: " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Plant API Error", "Response Error: " + error.toString());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json");
                params.put("Api-Key", BuildConfig.PLANT_KEY);
                return params;
            }
        };

        queue.add(jsonObjectRequest);
    }

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        inputStream.close();
        return byteBuffer.toByteArray();
    }

    public void openOptions() {
        openCamBtn.setVisibility(View.VISIBLE);
        openGalleryBtn.setVisibility(View.VISIBLE);
        shade.setVisibility(View.VISIBLE);
        idPlant.setContentDescription("Close options");

        shade.startAnimation(fadeInBg);
        idPlant.startAnimation(rotateClockWiseFabAnim);
        openGalleryBtn.startAnimation(fromBottomFabAnim);
        openCamBtn.startAnimation(fromBottomFabAnim);

        idPlant.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        openCamBtn.setVisibility(View.GONE);
        openGalleryBtn.setVisibility(View.GONE);
        shade.setVisibility(View.GONE);
        idPlant.setContentDescription("Open options");

        shade.startAnimation(fadeOutBg);
        idPlant.startAnimation(rotateAntiClockWiseFabAnim);
        openGalleryBtn.startAnimation(toBottomFabAnim);
        openCamBtn.startAnimation(toBottomFabAnim);

        idPlant.setImageResource(R.drawable.identify_icon);
    }
}