package com.sp.madproj.Identify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.sp.madproj.BuildConfig;
import com.sp.madproj.Main.MainActivity;
import com.sp.madproj.Plant.CanvasView;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Storage;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
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

    private ProgressBar loadingIcon;


    private Cursor model;
    private IdentifAdapter idAdapter;
    private IdentificationHelper idHelper;

    private static String imageKey = "";
    private UploadThread uploadThread = null;
    private class UploadThread extends Thread {
        private final Uri imageUri;
        UploadThread(Uri imageUri) {
            this.imageUri = imageUri;
        }

        public void run() {
            imageKey = Storage.uploadImgSupa(getActivity(), imageUri, Storage.identifStorage);
        }
    }

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

        RecyclerView identifs = view.findViewById(R.id.identifList);
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

        loadingIcon = view.findViewById(R.id.loadingIcon);

        idPlant = view.findViewById(R.id.idPlant);
        idPlant.setOnClickListener(
                view1 -> {
                    if (idPlant.getContentDescription().equals("Open options")) {
                        openOptions();
                    } else if (idPlant.getContentDescription().equals("Close options")) {
                        closeOptions();
                    }
                }
        );

        openCamBtn = view.findViewById(R.id.openCam);
        openCamBtn.setOnClickListener(
                view1 -> {
                    if (view1.getVisibility() == View.GONE) {
                        return;
                    }
                    getImage.launch(new Intent(getActivity(), CamActivity.class));
                }
        );

        openGalleryBtn = view.findViewById(R.id.openMenu);
        openGalleryBtn.setOnClickListener(
                view1 -> {
                    if (view1.getVisibility() == View.GONE) {
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    getImage.launch(intent);
                }
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(view2 -> closeOptions());

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivity.IMAGE_URI
                    && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
//                            Toast.makeText(getActivity().getApplicationContext(), imageUri.toString(), Toast.LENGTH_SHORT).show();
                        Log.d("result", imageUri.toString());

                        uploadThread = new UploadThread(imageUri);
                        uploadThread.start();

                        loadingIcon.setVisibility(View.VISIBLE);

                        // TEST CODE
//                            try {
//                                uploadThread.join();
//                            } catch (InterruptedException e) {
//                                throw new RuntimeException(e);
//                            }
//                            Intent intent = new Intent(getActivity(), IdResultActivity.class);
//                            intent.putExtra("response",
//                                    "{\n  \"access_token\": \"CtrjYkVtwJseMWs\",\n  \"model_version\": \"plant_id:4.0.2\",\n  \"custom_id\": null,\n  \"input\": {\n    \"latitude\": 49.207,\n    \"longitude\": 16.608,\n    \"similar_images\": true,\n    \"images\": [\n      \"https://plant.id/media/imgs/0f6ed1169d8442319fda1f9987e4210f.jpg\"\n    ],\n    \"datetime\": \"2024-08-05T08:16:45.899943+00:00\"\n  },\n  \"result\": {\n    \"is_plant\": {\n      \"probability\": 0.99096996,\n      \"threshold\": 0.5,\n      \"binary\": true\n    },\n    \"classification\": {\n      \"suggestions\": [\n        {\n          \"id\": \"872243f84209c0c2\",\n          \"name\": \"Buddleja davidii\",\n          \"probability\": 0.9892,\n          \"similar_images\": [\n            {\n              \"id\": \"909f07fbf17c7dab80a175a1649173b24ae6adb6\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/909/f07fbf17c7dab80a175a1649173b24ae6adb6.jpeg\",\n              \"license_name\": \"CC BY-NC-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-nc-sa/4.0/\",\n              \"citation\": \"FlowerChecker s.r.o.\",\n              \"similarity\": 0.758,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/909/f07fbf17c7dab80a175a1649173b24ae6adb6.small.jpeg\"\n            },\n            {\n              \"id\": \"808c7d58dabe9c3486549ea3e83de2fd9e86d581\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/808/c7d58dabe9c3486549ea3e83de2fd9e86d581.jpeg\",\n              \"license_name\": \"CC BY-NC-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-nc-sa/4.0/\",\n              \"citation\": \"FlowerChecker s.r.o.\",\n              \"similarity\": 0.741,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/808/c7d58dabe9c3486549ea3e83de2fd9e86d581.small.jpeg\"\n            }\n          ],\n          \"details\": {\n            \"common_names\": [\n              \"orange-eyed butterfly-bush\",\n              \"Butterfly bush\",\n              \"summer lilac\",\n              \"orange-eye butterfly bush\",\n              \"Chinese Sagewood\"\n            ],\n            \"taxonomy\": {\n              \"class\": \"Magnoliopsida\",\n              \"genus\": \"Buddleja\",\n              \"order\": \"Lamiales\",\n              \"family\": \"Scrophulariaceae\",\n              \"phylum\": \"Tracheophyta\",\n              \"kingdom\": \"Plantae\"\n            },\n            \"gbif_id\": 3173338,\n            \"inaturalist_id\": 75916,\n            \"rank\": \"species\",\n            \"edible_parts\": null,\n            \"best_light_condition\": \"This plant thrives in full sun, needing at least six hours of direct sunlight each day to perform its best. It can tolerate partial shade, but too much shade can result in fewer flowers and a leggy growth habit. Planting it in a sunny spot will encourage robust growth and abundant blooms, making it a standout in any garden.\",\n            \"best_soil_type\": \"For optimal growth, this plant prefers well-drained soil that is moderately fertile. It can tolerate a range of soil types, including sandy, loamy, and clay soils, as long as there is good drainage. Adding organic matter like compost can improve soil fertility and structure, helping the plant to establish and thrive.\",\n            \"best_watering\": \"Watering this plant requires a balanced approach. It prefers well-drained soil and does not like to sit in water. Water it deeply but infrequently, allowing the soil to dry out between waterings. During the growing season, typically spring and summer, it may need more frequent watering, especially in hot, dry conditions. In contrast, reduce watering in the fall and winter when the plant is not actively growing.\",\n            \"language\": \"en\",\n            \"entity_id\": \"872243f84209c0c2\"\n          }\n        },\n        {\n          \"id\": \"3514ca9d9bfbba10\",\n          \"name\": \"Buddleja japonica\",\n          \"probability\": 0.0108,\n          \"similar_images\": [\n            {\n              \"id\": \"23054bfca484d221f66f172d03242896f1ea9cdb\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/230/54bfca484d221f66f172d03242896f1ea9cdb.jpeg\",\n              \"license_name\": \"CC BY-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-sa/4.0/\",\n              \"citation\": \"Valentina Diakovasiliou\",\n              \"similarity\": 0.723,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/230/54bfca484d221f66f172d03242896f1ea9cdb.small.jpeg\"\n            },\n            {\n              \"id\": \"de6fb384640a6b498d452cd4da81f2cd52be41fe\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/de6/fb384640a6b498d452cd4da81f2cd52be41fe.jpeg\",\n              \"license_name\": \"CC BY 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by/4.0/\",\n              \"citation\": \"joffrey calvel\",\n              \"similarity\": 0.697,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/de6/fb384640a6b498d452cd4da81f2cd52be41fe.small.jpeg\"\n            }\n          ],\n          \"details\": {\n            \"common_names\": null,\n            \"taxonomy\": {\n              \"class\": \"Magnoliopsida\",\n              \"genus\": \"Buddleja\",\n              \"order\": \"Lamiales\",\n              \"family\": \"Scrophulariaceae\",\n              \"phylum\": \"Tracheophyta\",\n              \"kingdom\": \"Plantae\"\n            },\n            \"gbif_id\": 4055769,\n            \"inaturalist_id\": 509187,\n            \"rank\": \"species\",\n            \"best_light_condition\": \"This plant thrives in full sun to partial shade. It needs at least six hours of direct sunlight each day for optimal growth and flowering. If grown in partial shade, it may produce fewer flowers. However, it can tolerate some shade, especially in hotter climates where intense afternoon sun might be too harsh.\",\n            \"best_soil_type\": \"Well-draining soil is essential for healthy growth. A mix of loamy soil with some sand or perlite works well to ensure proper drainage. The soil should be rich in organic matter to provide necessary nutrients. Avoid heavy clay soils that retain too much moisture, as this can lead to root problems.\",\n            \"best_watering\": \"Watering should be done regularly but not excessively. The soil should be kept moist, especially during the growing season. It\'s important to let the top inch of soil dry out between waterings to prevent root rot. During the winter months, reduce the frequency of watering as the plant\'s growth slows down.\",\n            \"language\": \"en\",\n            \"entity_id\": \"3514ca9d9bfbba10\"\n          }\n        },\n        {\n          \"id\": \"872243f84209c0c2\",\n          \"name\": \"Buddleja davidii\",\n          \"probability\": 0.9892,\n          \"similar_images\": [\n            {\n              \"id\": \"909f07fbf17c7dab80a175a1649173b24ae6adb6\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/909/f07fbf17c7dab80a175a1649173b24ae6adb6.jpeg\",\n              \"license_name\": \"CC BY-NC-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-nc-sa/4.0/\",\n              \"citation\": \"FlowerChecker s.r.o.\",\n              \"similarity\": 0.758,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/909/f07fbf17c7dab80a175a1649173b24ae6adb6.small.jpeg\"\n            },\n            {\n              \"id\": \"808c7d58dabe9c3486549ea3e83de2fd9e86d581\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/808/c7d58dabe9c3486549ea3e83de2fd9e86d581.jpeg\",\n              \"license_name\": \"CC BY-NC-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-nc-sa/4.0/\",\n              \"citation\": \"FlowerChecker s.r.o.\",\n              \"similarity\": 0.741,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/808/c7d58dabe9c3486549ea3e83de2fd9e86d581.small.jpeg\"\n            }\n          ],\n          \"details\": {\n            \"common_names\": [\n              \"orange-eyed butterfly-bush\",\n              \"Butterfly bush\",\n              \"summer lilac\",\n              \"orange-eye butterfly bush\",\n              \"Chinese Sagewood\"\n            ],\n            \"taxonomy\": {\n              \"class\": \"Magnoliopsida\",\n              \"genus\": \"Buddleja\",\n              \"order\": \"Lamiales\",\n              \"family\": \"Scrophulariaceae\",\n              \"phylum\": \"Tracheophyta\",\n              \"kingdom\": \"Plantae\"\n            },\n            \"gbif_id\": 3173338,\n            \"inaturalist_id\": 75916,\n            \"rank\": \"species\",\n            \"edible_parts\": null,\n            \"best_light_condition\": \"This plant thrives in full sun, needing at least six hours of direct sunlight each day to perform its best. It can tolerate partial shade, but too much shade can result in fewer flowers and a leggy growth habit. Planting it in a sunny spot will encourage robust growth and abundant blooms, making it a standout in any garden.\",\n            \"best_soil_type\": \"For optimal growth, this plant prefers well-drained soil that is moderately fertile. It can tolerate a range of soil types, including sandy, loamy, and clay soils, as long as there is good drainage. Adding organic matter like compost can improve soil fertility and structure, helping the plant to establish and thrive.\",\n            \"best_watering\": \"Watering this plant requires a balanced approach. It prefers well-drained soil and does not like to sit in water. Water it deeply but infrequently, allowing the soil to dry out between waterings. During the growing season, typically spring and summer, it may need more frequent watering, especially in hot, dry conditions. In contrast, reduce watering in the fall and winter when the plant is not actively growing.\",\n            \"language\": \"en\",\n            \"entity_id\": \"872243f84209c0c2\"\n          }\n        },\n        {\n          \"id\": \"3514ca9d9bfbba10\",\n          \"name\": \"Buddleja japonica\",\n          \"probability\": 0.0108,\n          \"similar_images\": [\n            {\n              \"id\": \"23054bfca484d221f66f172d03242896f1ea9cdb\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/230/54bfca484d221f66f172d03242896f1ea9cdb.jpeg\",\n              \"license_name\": \"CC BY-SA 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by-sa/4.0/\",\n              \"citation\": \"Valentina Diakovasiliou\",\n              \"similarity\": 0.723,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/230/54bfca484d221f66f172d03242896f1ea9cdb.small.jpeg\"\n            },\n            {\n              \"id\": \"de6fb384640a6b498d452cd4da81f2cd52be41fe\",\n              \"url\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/de6/fb384640a6b498d452cd4da81f2cd52be41fe.jpeg\",\n              \"license_name\": \"CC BY 4.0\",\n              \"license_url\": \"https://creativecommons.org/licenses/by/4.0/\",\n              \"citation\": \"joffrey calvel\",\n              \"similarity\": 0.697,\n              \"url_small\": \"https://plant-id.ams3.cdn.digitaloceanspaces.com/similar_images/4/de6/fb384640a6b498d452cd4da81f2cd52be41fe.small.jpeg\"\n            }\n          ],\n          \"details\": {\n            \"common_names\": null,\n            \"taxonomy\": {\n              \"class\": \"Magnoliopsida\",\n              \"genus\": \"Buddleja\",\n              \"order\": \"Lamiales\",\n              \"family\": \"Scrophulariaceae\",\n              \"phylum\": \"Tracheophyta\",\n              \"kingdom\": \"Plantae\"\n            },\n            \"gbif_id\": 4055769,\n            \"inaturalist_id\": 509187,\n            \"rank\": \"species\",\n            \"best_light_condition\": \"This plant thrives in full sun to partial shade. It needs at least six hours of direct sunlight each day for optimal growth and flowering. If grown in partial shade, it may produce fewer flowers. However, it can tolerate some shade, especially in hotter climates where intense afternoon sun might be too harsh.\",\n            \"best_soil_type\": \"Well-draining soil is essential for healthy growth. A mix of loamy soil with some sand or perlite works well to ensure proper drainage. The soil should be rich in organic matter to provide necessary nutrients. Avoid heavy clay soils that retain too much moisture, as this can lead to root problems.\",\n            \"best_watering\": \"Watering should be done regularly but not excessively. The soil should be kept moist, especially during the growing season. It\'s important to let the top inch of soil dry out between waterings to prevent root rot. During the winter months, reduce the frequency of watering as the plant\'s growth slows down.\",\n            \"language\": \"en\",\n            \"entity_id\": \"3514ca9d9bfbba10\"\n          }\n        }\n      ]\n    }\n  },\n  \"status\": \"COMPLETED\",\n  \"sla_compliant_client\": false,\n  \"sla_compliant_system\": true,\n  \"created\": 1722845805.899943,\n  \"completed\": 1722845806.315829\n}"
//                            );
//                            intent.putExtra("inputUriStr", imageUri.toString());
//                            intent.putExtra("savedImg", imageKey);
//                            intent.putExtra("purpose", "identify");
//                            startActivity(intent);
//                            loadingIcon.setVisibility(View.GONE);

                        getPlantIdAPI(imageUri);

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
        idAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        idHelper.close();
    }

    public class IdentifAdapter extends RecyclerView.Adapter<IdentifAdapter.IdentifHolder> {
        private final IdentificationHelper helper;
        private Cursor cursor;

        IdentifAdapter(Context context, Cursor cursor) {
            this.cursor = cursor;
            helper = new IdentificationHelper(context);
        }

        public void swapCursor(Cursor newCursor) {
            Cursor oldCursor = this.cursor;
            this.cursor = newCursor;
            oldCursor.close();
        }

        @NonNull
        @Override
        public IdentifHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_identif, parent, false);
            return new IdentifHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull IdentifHolder holder, int position) {
            if (!cursor.moveToPosition(holder.getAdapterPosition())) {
                return;
            }

            Log.d("Cursor", cursor.toString());
            holder.species.setText(helper.getSpecies(cursor));
            holder.common.setText(String.format(Locale.ENGLISH, "(%s, %.2f%%)",
                    helper.getCommon(cursor).substring(0, 1).toUpperCase() +
                            helper.getCommon(cursor).substring(1),
                    100 * helper.getAccuracy(cursor)));
            holder.date.setText(helper.getDate(cursor).substring(0, 11));

            String imageUrl = Storage.identifStorage + helper.getImage(cursor);
            Picasso.get()
                    .load(imageUrl)
                    .resize((int) CanvasView.pxFromDp(100, requireContext()),
                            (int) CanvasView.pxFromDp(100, requireContext()))
                    .centerCrop()
                    .placeholder(R.drawable.plant_flower)
                    .into(holder.image);

            holder.itemView.setOnClickListener(view -> {
                if (!cursor.moveToPosition(holder.getAdapterPosition())) {
                    return;
                }

                Intent intent = new Intent(getActivity(), IdResultActivity.class);
                intent.putExtra("response", helper.getJsonReply(cursor));
                intent.putExtra("purpose", "check");
                intent.putExtra("savedImg", helper.getImage(cursor));
                intent.putExtra("recordId", helper.getID(cursor));
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return cursor.getCount();
        }

        public class IdentifHolder extends RecyclerView.ViewHolder {
            private final TextView species;
            private final TextView common;
            private final TextView date;
            private final ImageView image;

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
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int quality = bitmap.getByteCount()/1024 < 512 ? 100 : bitmap.getByteCount()/(1024*512);
        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bitmapStream);
        byte[] bytes = bitmapStream.toByteArray();
        bitmap.recycle();

        String base64Encoded = Base64.encodeToString(bytes, Base64.DEFAULT);

        String dataUri = "data:" + getContext().getContentResolver().getType(imageUri) +
                ";base64," + base64Encoded;

        String plantApi = "https://plant.id/api/v3/identification?details=common_names,gbif_id,inaturalist_id,best_light_condition,best_soil_type,best_watering&language=en";

        JSONObject body = new JSONObject();
        try {
            ((MainActivity) getActivity()).updateLocation();
            body.put("images", new JSONArray().put(dataUri))
                    .put("latitude",  ((MainActivity) getActivity()).getLatitude())
                    .put("longitude", ((MainActivity) getActivity()).getLongitude())
                    .put("similar_images", true);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        Log.d("test", body.toString());
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, plantApi, body,
                response -> {
                    try {
                        uploadThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    Intent intent = new Intent(getActivity(), IdResultActivity.class);
                    intent.putExtra("response", response.toString());
                    intent.putExtra("inputUriStr", imageUri.toString());
                    intent.putExtra("savedImg", imageKey);
                    intent.putExtra("purpose", "identify");
                    startActivity(intent);

                    loadingIcon.setVisibility(View.GONE);
                },
                error -> {
                    Log.d("Plant API Error", "Response Error: " + error.toString());
                    loadingIcon.setVisibility(View.GONE);
                    Storage.deleteObjSupa(getActivity(), Storage.identifStorage + imageKey);

                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    } else if (error.networkResponse != null && error.networkResponse.statusCode == 429) {
                        Toast.makeText(getActivity(), "Out of credits", Toast.LENGTH_SHORT).show();
                    }

                    if (error.networkResponse != null) {
                        String bodyStr = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        Log.d("Plant API Error", bodyStr);
                        if (bodyStr.equals("Invalid image data")) {
                            Toast.makeText(getActivity(), "Error with image", Toast.LENGTH_SHORT).show();
                        }
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

    public byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
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

        shade.setClickable(true);

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

        shade.setClickable(false);

        idPlant.setImageResource(R.drawable.identify_icon);
    }
}