package com.sp.madproj;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import uk.me.hardill.volley.multipart.MultipartRequest;

public class IdentificationHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "identification.db";
    private static final int SCHEMA_VERSION = 1;

    public IdentificationHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE identification_table (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "speciesName TEXT," +
                "commonName TEXT," +
                "date TEXT," +
                "accuracy REAL," +
                "jsonReply TEXT," +
                "plantImageKey TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Cursor getAll() {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM identification_table;", null));
    }

    public Cursor getIdentificationById(String id) {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM identification_table " +
                        "WHERE _id = ?", new String[]{id}));
    }

    public void insert(String species, String common, String imgKey, String date, double accuracy, String jsonReply, Context context) {
        ContentValues cv = new ContentValues();
        cv.put("speciesName", species);
        cv.put("commonName", common);
        cv.put("date", date);
        cv.put("accuracy", accuracy);
        cv.put("jsonReply", jsonReply);
        cv.put("plantImageKey", imgKey);

        getWritableDatabase().insert("identification_table", "speciesName", cv);
    }

    private void deleteImgSupa(String imgKey, Context context) {
        Log.d("Storage API", "Building Request...");

        RequestQueue queue = Volley.newRequestQueue(context);

        String apiUrl = "https://upevuilypqhjisraltzb.supabase.co/storage/v1/object/images/"
                + imgKey;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.DELETE, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response != null) {
                    Log.d("Storage API", "onResponse: " + response.toString());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Storage API Error", "Response Error: " + error.toString());
                Log.e("Storage API Error", "Response Error: " + error.getMessage());

                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);
                return params;
            }
        };


        Log.d("Storage API", "Sent request: " + apiUrl);
        queue.add(request);

    }

    public void delete(String id, String imgKey, Context context) {
        deleteImgSupa(imgKey, context);
        getWritableDatabase().delete("identification_table", "_id=?", new String[]{id});
    }

    public String getID(Cursor c) {
        return c.getString(0);
    }

    public String getSpecies(Cursor c) {
        return c.getString(1);
    }

    public String getCommon(Cursor c) {
        return c.getString(2);
    }

    public String getDate(Cursor c) {
        return c.getString(3);
    }

    public double getAccuracy(Cursor c) {
        return c.getDouble(4);
    }

    public String getJsonReply(Cursor c) {
        return c.getString(5);
    }

    public String getImage(Cursor c) {
        return c.getString(6);
    }
}