package com.sp.madproj;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class PlantHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "plant.db";
    private static final int SCHEMA_VERSION = 1;

    public PlantHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE plant_table (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "position INTEGER," +
                "accessKey TEXT," +
                "icon TEXT," +
                "name TEXT," +
                "species TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Cursor getAll() {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table;", null));
    }

    public Cursor getIdentificationById(String id) {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table " +
                        "WHERE _id = ?", new String[]{id}));
    }

    public void insert(int position, String accessKey, String icon, String name, String species, Context context) {
        ContentValues cv = new ContentValues();

        cv.put("position", position);
        cv.put("accessKey", accessKey);
        cv.put("icon", icon);
        cv.put("name", name);
        cv.put("species", species);

        getWritableDatabase().insert("plant_table", "position", cv);
    }


    public void delete(String id, String imgKey, Context context) {
        Storage.deleteObjSupa(context, Storage.identifStorage + imgKey);
        getWritableDatabase().delete("plant_table", "_id=?", new String[]{id});
    }


    public String getID(Cursor c) {
        return c.getString(0);
    }

    public int getPosition(Cursor c) {
        return c.getInt(1);
    }

    public String getAccessKey(Cursor c) {
        return c.getString(2);
    }

    public String getIcon(Cursor c) {
        return c.getString(3);
    }

    public String getName(Cursor c) {
        return c.getString(4);
    }

    public String getSpecies(Cursor c) {
        return c.getString(5);
    }
}