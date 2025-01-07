package com.sp.madproj;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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
                "plantImage BLOB," +
                "date TEXT," +
                "accuracy REAL);");
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

    public void update(String id, String species, String common, Uri uri, String date, double accuracy, Context context) {
        ContentValues cv = new ContentValues();

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            int width = 100;
            int height = 100;
            if (bitmap.getWidth() < bitmap.getHeight()) {
                height = Math.round(bitmap.getHeight() / (bitmap.getWidth() / 100));
            } else if (bitmap.getHeight() < bitmap.getWidth()) {
                width = Math.round(bitmap.getWidth() / (bitmap.getHeight() / 100));
            }
            Bitmap resizedBitmap;
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);


            Bitmap cropBitmap;
            if (width > height) {
                cropBitmap = Bitmap.createBitmap(resizedBitmap, Math.round((width - 100) / 2), 0,
                        Math.round(((width - 100) / 2) + 100), 100);
            } else if (height > width) {
                cropBitmap = Bitmap.createBitmap(resizedBitmap, 0, Math.round((height - 100) / 2),
                        100, Math.round(((width - 100) / 2) + 100));
            } else {
                cropBitmap = resizedBitmap;
            }

            cropBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] inputData = stream.toByteArray();
            cropBitmap.recycle();

            cv.put("speciesName", species);
            cv.put("commonName", common);
            cv.put("plantImage", inputData);
            cv.put("date", date);
            cv.put("accuracy", accuracy);

            getWritableDatabase().update("identification_table", cv, "_id = ?",
                    new String[]{id});
        } catch (IOException e) {
            Toast.makeText(MainActivity.getContext().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
            Log.e("Database error", e.toString());
        }
    }

    public void insert(String species, String common, Uri uri, String date, double accuracy, Context context) {
        ContentValues cv = new ContentValues();
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            int width = 100;
            int height = 100;
            if (bitmap.getWidth() < bitmap.getHeight()) {
                height = Math.round(bitmap.getHeight() / (bitmap.getWidth() / 100));
            } else if (bitmap.getHeight() < bitmap.getWidth()) {
                width = Math.round(bitmap.getWidth() / (bitmap.getHeight() / 100));
            }
            Bitmap resizedBitmap;
            resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);


            Bitmap cropBitmap;
            if (width > height) {
                cropBitmap = Bitmap.createBitmap(resizedBitmap, Math.round((width - 100) / 2), 0,
                        Math.round(((width - 100) / 2) + 100), 100);
            } else if (height > width) {
                cropBitmap = Bitmap.createBitmap(resizedBitmap, 0, Math.round((height - 100) / 2),
                        100, Math.round(((width - 100) / 2) + 100));
            } else {
                cropBitmap = resizedBitmap;
            }

            cropBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] inputData = stream.toByteArray();
            cropBitmap.recycle();

            cv.put("speciesName", species);
            cv.put("commonName", common);
            cv.put("plantImage", inputData);
            cv.put("date", date);
            cv.put("accuracy", accuracy);

            getWritableDatabase().insert("identification_table", "speciesName", cv);
        } catch (IOException e) {
            Toast.makeText(MainActivity.getContext().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
            Log.e("Database error", e.toString());
        }
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

    public Bitmap getImage(Cursor c, Context context) {
        byte[] image = c.getBlob(3);
        if (image == null) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.notif_important);
        }

        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public String getDate(Cursor c) {
        return c.getString(4);
    }

    public String getAccuracy(Cursor c) {
        return c.getString(5);
    }
}