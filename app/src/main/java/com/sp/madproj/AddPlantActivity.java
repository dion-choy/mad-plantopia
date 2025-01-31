package com.sp.madproj;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPlantActivity extends AppCompatActivity {
    Animation scaleUp;
    Animation scaleDown;

    ImageButton selectFlower;
    ImageButton selectUpright;
    ImageButton selectVine;
    ImageButton selectCactus;

    TextInputEditText nameInput;
    AutoCompleteTextView speciesInput;
    String selectAccessToken = "";
    String species = "";

    ImageView flowerTick;
    ImageView uprightTick;
    ImageView vineTick;
    ImageView cactusTick;

    List<String> model = new ArrayList<>();
    List<String> queries = new ArrayList<>();
    ArrayAdapter<String> adapter = null;

    boolean queueFilled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_plant);

        scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        selectFlower = findViewById(R.id.selectFlower);
        selectUpright = findViewById(R.id.selectUpright);
        selectVine = findViewById(R.id.selectVine);
        selectCactus = findViewById(R.id.selectCactus);

        flowerTick = findViewById(R.id.selectedFlower);
        uprightTick = findViewById(R.id.selectedUpright);
        vineTick = findViewById(R.id.selectedVine);
        cactusTick = findViewById(R.id.selectedCactus);

        selectFlower.setOnClickListener(plantSelection);
        selectUpright.setOnClickListener(plantSelection);
        selectVine.setOnClickListener(plantSelection);
        selectCactus.setOnClickListener(plantSelection);

        nameInput = findViewById(R.id.nameInput);

        adapter = new ArrayAdapter<>(this, R.layout.row_autocomplete_dropdown, model);
        speciesInput = findViewById(R.id.speciesInput);
        speciesInput.setAdapter(adapter);
        speciesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                getAutoComplete(speciesInput.getText().toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        speciesInput.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectAccessToken = queries.get(i);
                species = speciesInput.getText().toString();
            }
        });

        findViewById(R.id.returnBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean error = false;
                if (nameInput.getText().toString().isEmpty()) {
                    nameInput.setError("Enter a name!");
                    error = true;
                }

                if (speciesInput.getText().toString().isEmpty()) {
                    speciesInput.setError("Enter the species!");
                    error = true;
                }

                if (selectAccessToken.isEmpty() || species.isEmpty()) {
                    speciesInput.setError("Please pick an item from the dropdown list!");
                    error = true;
                }

                String selectedIcon = "";
                if (flowerTick.getVisibility() == View.VISIBLE) {
                    selectedIcon = "flower";
                } else if (uprightTick.getVisibility() == View.VISIBLE) {
                    selectedIcon = "upright";
                } else if (vineTick.getVisibility() == View.VISIBLE) {
                    selectedIcon = "vine";
                } else if (cactusTick.getVisibility() == View.VISIBLE) {
                    selectedIcon = "cactus";
                } else {
                    error = true;
                }

                if (error) {
                    return;
                }

                Log.d("Output", nameInput.getText().toString() + ", " + selectAccessToken + ", " + selectedIcon);
                setResult(1,
                        new Intent()
                                .putExtra("name", nameInput.getText().toString())
                                .putExtra("accessToken", selectAccessToken)
                                .putExtra("species", species)
                                .putExtra("icon", selectedIcon)
                );
                finish();
            }
        });
    }

    String prevQuery = "";
    void getAutoComplete(String query) {
        RequestQueue queue = Volley.newRequestQueue(this);

        String plantApi = "https://plant.id/api/v3/kb/plants/name_search?q=" + query;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, plantApi, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                queueFilled = false;
                adapter.clear();
                try {
                    JSONArray matches = response.getJSONArray("entities");
                    for (int i = 0; i < matches.length(); i++) {
                        adapter.add(matches.getJSONObject(i).getString("matched_in"));
                        queries.add(matches.getJSONObject(i).getString("access_token"));
                    }
                    adapter.notifyDataSetChanged();

                    if (!query.equals(prevQuery)) {
                        int selectionStart = speciesInput.getSelectionStart();
                        int selectionEnd = speciesInput.getSelectionEnd();
                        speciesInput.setText(speciesInput.getText());
                        speciesInput.setSelection(selectionStart, selectionEnd);
                    }
                    prevQuery = query;
//                    Toast.makeText(getApplicationContext(), "Response", Toast.LENGTH_SHORT).show();
                    Log.d("Plant API Success", matches.getJSONObject(0).getString("entity_name"));
                } catch (JSONException e) {
                    Log.e("Plant API Error", "Malformed Response: " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Plant API Error", "Response Error: " + error.toString());
                queueFilled = false;

                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                } else if (error.networkResponse != null && error.networkResponse.statusCode == 429) {
                    Toast.makeText(getApplicationContext(), "Out of credits", Toast.LENGTH_SHORT).show();
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

        if (!queueFilled) {
            queue.add(jsonObjectRequest);
            queueFilled = true;
        }
    }

    View.OnClickListener plantSelection = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
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