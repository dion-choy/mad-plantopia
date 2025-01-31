package com.sp.madproj;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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
                "accessToken TEXT," +
                "icon TEXT," +
                "name TEXT," +
                "species TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public Cursor getAll() {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table ORDER BY position;", null));
    }

    public Cursor getIdentificationById(String id) {
        return (getReadableDatabase().rawQuery(
                "SELECT * FROM plant_table " +
                        "WHERE _id = ?", new String[]{id}));
    }

    public Cursor getFilledPos(int position) {
        return (getReadableDatabase().rawQuery(
                "SELECT position FROM plant_table " +
                        "WHERE position = ?", new String[]{String.valueOf(position)}));
    }

    public void insert(int position, String accessToken, String icon, String name, String species) {
        ContentValues cv = new ContentValues();

        cv.put("position", position);
        cv.put("accessToken", accessToken);
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

    public String getAccessToken(Cursor c) {
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