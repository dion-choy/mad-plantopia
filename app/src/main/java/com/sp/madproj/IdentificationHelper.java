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
                "accuracy REAL," +
                "jsonReply TEXT);");
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

    public void update(String id, String species, String common, Uri uri, String date, double accuracy, String jsonReply, Context context) {
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
            cv.put("jsonReply", jsonReply);

            getWritableDatabase().update("identification_table", cv, "_id = ?",
                    new String[]{id});
        } catch (IOException e) {
            Toast.makeText(MainActivity.getContext().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
            Log.e("Database error", e.toString());
        }
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
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);

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

            ByteArrayOutputStream stream = new ByteArrayOutputStream();

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
            cropBitmap = Bitmap.createBitmap(cropBitmap, 0, 0, cropBitmap.getWidth(), cropBitmap.getHeight(), matrix, true);

            cropBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] inputData = stream.toByteArray();
            cropBitmap.recycle();

            cv.put("speciesName", species);
            cv.put("commonName", common);
            cv.put("plantImage", inputData);
            cv.put("date", date);
            cv.put("accuracy", accuracy);
            cv.put("jsonReply", jsonReply);

            getWritableDatabase().insert("identification_table", "speciesName", cv);
        } catch (IOException e) {
            Toast.makeText(context.getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
            Log.e("Database error", e.toString());
        }
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

    public Bitmap getImage(Cursor c, Context context) {
        byte[] image = c.getBlob(3);
        if (image == null) {
            return BitmapFactory.decodeResource(context.getResources(), R.drawable.notif_important);
        }

        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    public byte[] getImageByteArr(Cursor c) {
        return c.getBlob(3);
    }

    public String getDate(Cursor c) {
        return c.getString(4);
    }

    public double getAccuracy(Cursor c) {
        return c.getDouble(5);
    }

    public String getJsonReply(Cursor c) {
        return c.getString(6);
    }
}