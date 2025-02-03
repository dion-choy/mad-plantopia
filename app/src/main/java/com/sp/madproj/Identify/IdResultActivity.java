package com.sp.madproj.Identify;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sp.madproj.R;
import com.sp.madproj.Utils.Storage;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IdResultActivity extends AppCompatActivity {
    private ImageView inputImage;

    private RecyclerView results;
    private List<JSONObject> model = new ArrayList<>();
    private ResultsAdapter resultsAdapter;

    private IdentificationHelper idHelper;
    private Cursor modelSQLite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

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
        Toolbar toolbar = findViewById(R.id.toolbar);

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
        String imgKey= getIntent().getStringExtra("savedImg" );
        if (purpose != null && apiReply != null) {
            if (purpose.equals("identify") && inputUriStr != null && imgKey != null) {
                loadResult(inputUriStr, apiReply, imgKey);
            } else if (purpose.equals("check") && imgKey != null && recordId != null) {
                toolbar.setVisibility(View.VISIBLE);
                toolbar.inflateMenu(R.menu.id_menu);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        if (menuItem.getItemId() == R.id.delete) {
                            idHelper.delete(recordId, imgKey, IdResultActivity.this);
                            Storage.deleteObjSupa(IdResultActivity.this, Storage.identifStorage + imgKey);
                            finish();
                            return true;
                        }
                        return false;
                    }
                });

                loadFromDB(imgKey, apiReply);
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

    void loadFromDB(String imgKey, String apiReply) {
        try {
            JSONObject apiReplyObj = new JSONObject(apiReply);

            Picasso.get()
                    .load(Storage.identifStorage + imgKey)
                    .placeholder(R.drawable.plant_flower)
                    .into(inputImage);

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

            for (int i = 0; i < suggestions.length(); i++) {
                model.add(suggestions.getJSONObject(i));
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    void loadResult(String inputUriStr, String apiReply, String imgKey) {
        Matrix matrix = new Matrix();
        switch (Storage.getBitmapOriention(this, Uri.parse(inputUriStr))) {
            case ExifInterface.ORIENTATION_NORMAL:
                matrix.postRotate(0);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Uri.parse(inputUriStr));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        inputImage.setImageBitmap(bitmap);

        try {
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
                        imgKey,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                        suggestions.getJSONObject(0).getDouble("probability"),
                        apiReply);
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
                    .inflate(R.layout.row_results, parent, false);
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
                            )
                            .placeholder(R.drawable.plant_flower)
                            .into(holder.similarImage);
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