package com.sp.madproj;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IdResult extends AppCompatActivity {
    private ImageView inputImage;
    private Toolbar toolbar;

    private RecyclerView results;
    private List<JSONObject> model = new ArrayList<>();
    private ResultsAdapter resultsAdapter;

    private IdentificationHelper idHelper;
    private Cursor modelSQLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_id_result);

        idHelper = new IdentificationHelper(this);
        modelSQLite = idHelper.getAll();

        resultsAdapter = new ResultsAdapter(model);

        results = findViewById(R.id.resultsList);
        results.setHasFixedSize(true);
        results.setLayoutManager(new LinearLayoutManager(this));
        results.setItemAnimator(new DefaultItemAnimator());

        inputImage = findViewById(R.id.inputImg);
        toolbar = findViewById(R.id.toolbar);

        findViewById(R.id.returnBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        String inputUriStr = getIntent().getStringExtra("inputUriStr" );
        String apiReply = getIntent().getStringExtra("response" );
        String purpose = getIntent().getStringExtra("purpose" );
        String recordId = getIntent().getStringExtra("recordId" );
        byte[] savedImgByteArr = getIntent().getByteArrayExtra("savedImg" );
        if (purpose != null && apiReply != null) {
            if (purpose.equals("identify") && inputUriStr != null) {
                loadResult(inputUriStr, apiReply);
            } else if (purpose.equals("check") && savedImgByteArr != null && recordId != null) {
                toolbar.setVisibility(View.VISIBLE);
                toolbar.inflateMenu(R.menu.id_menu);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.delete) {
                            idHelper.delete(recordId);
                            finish();
                            return true;
                        }
                        return false;
                    }
                });

                loadFromDB(savedImgByteArr, apiReply);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        results.setAdapter(resultsAdapter);

        if (modelSQLite != null) {
            modelSQLite.close();
        }

        modelSQLite = idHelper.getAll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        idHelper.close();
    }

    void loadFromDB(byte[] savedImgByteArr, String apiReply) {
        try {
            JSONObject apiReplyObj = new JSONObject(apiReply);

            inputImage.setImageBitmap(
                    BitmapFactory.decodeByteArray(savedImgByteArr, 0, savedImgByteArr.length)
            );

            boolean isPlant = apiReplyObj.getJSONObject("result")
                    .getJSONObject("is_plant").getDouble("probability") >= 0.4;

            if (!isPlant) {
                inputImage.setImageResource(R.drawable.notif_important);
                ((TextView) findViewById(R.id.notPlant)).setText("Might not be a plant");
                (findViewById(R.id.notPlant)).setVisibility(View.VISIBLE);
            }

            JSONArray suggestions = apiReplyObj.getJSONObject("result")
                    .getJSONObject("classification")
                    .getJSONArray("suggestions");

            ((TextView) findViewById(R.id.test)).setText(apiReply);

            for (int i = 0; i < suggestions.length(); i++) {
                model.add(suggestions.getJSONObject(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    void loadResult(String inputUriStr, String apiReply) {
        inputImage.setImageURI(Uri.parse(inputUriStr));

        try {
            ((TextView) findViewById(R.id.test)).setText(apiReply);

            JSONObject apiReplyObj = new JSONObject(apiReply);

            JSONArray suggestions = apiReplyObj.getJSONObject("result")
                    .getJSONObject("classification")
                    .getJSONArray("suggestions");

            for (int i = 0; i < suggestions.length(); i++) {
                model.add(suggestions.getJSONObject(i));
            }

            boolean isPlant = apiReplyObj.getJSONObject("result")
                    .getJSONObject("is_plant").getDouble("probability") >= 0.4;

            if (isPlant) {
                idHelper.insert(suggestions.getJSONObject(0).getString("name"),
                        suggestions.getJSONObject(0).getJSONObject("details")
                                .getJSONArray("common_names").getString(0),
                        Uri.parse(inputUriStr),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                        suggestions.getJSONObject(0).getDouble("probability"),
                        apiReply,
                        this);
            } else {
                inputImage.setImageResource(R.drawable.notif_important);
                ((TextView) findViewById(R.id.notPlant)).setText("Might not be a plant");
                (findViewById(R.id.notPlant)).setVisibility(View.VISIBLE);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultsHolder>{
        private List<JSONObject> results;

        ResultsAdapter(List<JSONObject> results) {
            this.results = results;
        }

        @Override
        public ResultsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.results_row, parent, false);
            return new ResultsAdapter.ResultsHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultsHolder holder, final int position) {
            final JSONObject result = results.get(position);
            try {
                holder.species.setText(result.getString("name" ));
                holder.accuracy.setText(
                        String.format(Locale.ENGLISH, "%.2f%%",
                                result.getDouble("probability") * 100)
                );

                if (!result.getJSONObject("details" ).isNull("common_names")) {
                    String commonNameOut = "";
                    JSONArray commonNames = result.getJSONObject("details" )
                            .getJSONArray("common_names" );

                    for (int i = 0; i < Math.min(commonNames.length(), 3); i++) {
                        commonNameOut += commonNames.getString(i).substring(0, 1).toUpperCase() +
                                commonNames.getString(i).substring(1);
                        if (i < Math.min(commonNames.length(), 3)-1) {
                            commonNameOut += ",\n";
                        }
                    }

                    holder.common.setText(commonNameOut);
                } else {
                    holder.commonLabel.setText("");
                }

                if (!result.isNull("similar_images" )) {
                    Picasso.get()
                            .load(result.getJSONArray("similar_images" ).getJSONObject(0)
                                            .getString("url")
                            ).into(holder.similarImage);
                }
            } catch (Exception e) {
                Log.e("Malformed Response", e.toString());
            }
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class ResultsHolder extends RecyclerView.ViewHolder {
            private TextView species;
            private TextView common;
            private TextView commonLabel;
            private TextView accuracy;
            private ImageView similarImage;

            public ResultsHolder(View itemView) {
                super(itemView);
                species = itemView.findViewById(R.id.speciesName);
                common = itemView.findViewById(R.id.commonName);
                commonLabel = itemView.findViewById(R.id.commonLabel);
                accuracy = itemView.findViewById(R.id.accuracy);
                similarImage = itemView.findViewById(R.id.similarImage);
            }
        }
    }
}