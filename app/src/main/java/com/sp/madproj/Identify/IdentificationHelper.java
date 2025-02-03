package com.sp.madproj.Identify;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sp.madproj.Utils.Storage;

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

    public void insert(String species, String common, String imgKey, String date, double accuracy, String jsonReply) {
        ContentValues cv = new ContentValues();
        cv.put("speciesName", species);
        cv.put("commonName", common);
        cv.put("date", date);
        cv.put("accuracy", accuracy);
        cv.put("jsonReply", jsonReply);
        cv.put("plantImageKey", imgKey);

        getWritableDatabase().insert("identification_table", "speciesName", cv);
    }

    public void delete(String id, String imgKey, Context context) {
        Storage.deleteObjSupa(context, Storage.identifStorage + imgKey);
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