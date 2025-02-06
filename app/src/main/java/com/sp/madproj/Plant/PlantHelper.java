package com.sp.madproj.Plant;

import static android.content.Context.MODE_PRIVATE;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.volley.NoConnectionError;
import com.sp.madproj.Utils.Database;
import com.sp.madproj.Utils.Storage;

import java.util.Locale;

public class PlantHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "plant.db";
    private static final int SCHEMA_VERSION = 1;

    private final String greenhouseId;
    private final SharedPreferences sharedPref;
    public PlantHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);

        sharedPref = context.getSharedPreferences("greenhouse", MODE_PRIVATE);
        greenhouseId = sharedPref.getString("greenhouseId", null);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE plant_table (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "position INTEGER," +
                "detail TEXT," +
                "icon TEXT," +
                "name TEXT," +
                "last_watered TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Cursor getAll() {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table ORDER BY position;", null));
    }

    public Cursor getPlantById(String id) {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table " +
                        "WHERE _id = ?", new String[]{id}));
    }

    public Cursor getFilledPos(int position) {
        return (getReadableDatabase().rawQuery(
                "SELECT position FROM plant_table " +
                        "WHERE position = ?", new String[]{String.valueOf(position)}));
    }

    public void deleteAll() {
        getWritableDatabase().delete("plant_table", null, null);
    }

    public void insert(int position, String detail, String icon, String name, String last_watered, Context context, boolean forSync) {
        ContentValues cv = new ContentValues();

        cv.put("position", position);
        cv.put("detail", detail);
        cv.put("icon", icon);
        cv.put("name", name);
        cv.put("last_watered", last_watered);

        getWritableDatabase().insert("plant_table", "position", cv);
        if (greenhouseId != null && forSync) {
            Log.d("Database", "Client Changes: True");
            sharedPref.edit()
                    .putBoolean("clientChanged", true)
                    .commit();
        }
    }

    public void insert(int position, String detail, String icon, String name, String last_watered, Context context) {
        insert(position, detail, icon, name, last_watered, context, false);
    }

    public void updateByPos(int position, String detail, String icon, String name, String last_watered, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("position", position);
        cv.put("detail", detail);
        cv.put("icon", icon);
        cv.put("name", name);
        cv.put("last_watered", last_watered);

        getWritableDatabase().update("plant_table", cv, "position = ?",
                new String[]{String.valueOf(position)});
    }

    public void update(String id, int position, String detail, String icon, String name, String last_watered, Context context, boolean forSync) {
        ContentValues cv = new ContentValues();

        cv.put("position", position);
        cv.put("detail", detail);
        cv.put("icon", icon);
        cv.put("name", name);
        cv.put("last_watered", last_watered);

        getWritableDatabase().update("plant_table", cv, "_id = ?",
                new String[]{id});

        if (greenhouseId != null && forSync) {
            sharedPref.edit()
                    .putBoolean("clientChanged", true)
                    .commit();
        }
    }

    public void update(String id, int position, String detail, String icon, String name, String last_watered, Context context) {
        update(id, position, detail, icon, name, last_watered, context, false);
    }

    public void delete(String id, String imgKey, Context context) {
        getWritableDatabase().delete("plant_table", "_id=?", new String[]{id});
    }


    public String getID(Cursor c) {
        return c.getString(0);
    }

    public int getPosition(Cursor c) {
        return c.getInt(1);
    }

    public String getDetail(Cursor c) {
        return c.getString(2);
    }

    public String getIcon(Cursor c) {
        return c.getString(3);
    }

    public String getName(Cursor c) {
        return c.getString(4);
    }

    public String getTimestamp(Cursor c) {
        return c.getString(5);
    }
}