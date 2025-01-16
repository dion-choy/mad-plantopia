package com.sp.madproj;

import android.content.ContentValues;
import android.content.Context;
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

import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private String uploadImgSupa(Context context, Uri uri) {
        Matrix matrix = new Matrix();
        switch (getBitmapOriention(getRealPathFromURI(context, uri))) {
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
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bitmapStream);
        byte[] inputData = bitmapStream.toByteArray();
        bitmap.recycle();

        Log.d("Storage API", "Building Request...");

        RequestQueue queue = Volley.newRequestQueue(context);

        Map<String, String> header = new HashMap<String, String>();
        header.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);

        String rng = String.valueOf((int) Math.floor(Math.random()*100000));
        String apiUrl = "https://upevuilypqhjisraltzb.supabase.co/storage/v1/object/images/"
                +  rng + ".jpg";

        MultipartRequest request = new MultipartRequest(Request.Method.POST,
                apiUrl, header,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        if (response != null) {
                            String responseData = new String(response.data, StandardCharsets.UTF_8);
                            Log.d("Storage API", "onResponse: " + responseData);
                        }
                        Log.d("Storage API", "run ");
                        Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
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
        });

        request.addPart(new MultipartRequest.FilePart("", "image/jpg", null, inputData));

        Log.d("Storage API", "Sent request: " + apiUrl);
        queue.add(request);

        return rng + ".jpg";
    }

    public static int getBitmapOriention(String path){
        ExifInterface exif = null;
        int orientation = 0;
        try {
            exif = new ExifInterface(path);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            //Log.e("getBitmapOriention", "getBitmapOriention: "+orientation );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orientation;
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public void insert(String species, String common, Uri uri, String date, double accuracy, String jsonReply, Context context) {
        ContentValues cv = new ContentValues();
        cv.put("speciesName", species);
        cv.put("commonName", common);
        cv.put("date", date);
        cv.put("accuracy", accuracy);
        cv.put("jsonReply", jsonReply);
        cv.put("plantImageKey", uploadImgSupa(context, uri));

        getWritableDatabase().insert("identification_table", "speciesName", cv);
        Toast.makeText(context.getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
    }

    public void delete(String id) {
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