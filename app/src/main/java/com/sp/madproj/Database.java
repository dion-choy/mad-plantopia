package com.sp.madproj;

import com.google.firebase.database.FirebaseDatabase;

public class Database {
    private static final String databaseUrl = " https://plantopia-backend-ecce9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private static FirebaseDatabase database;
    private Database() {
        this.database = FirebaseDatabase.getInstance(databaseUrl);

        database.setPersistenceEnabled(true);
    }

    public static FirebaseDatabase get() {
        if (database == null) {
            return new Database().get();
        }
        return database;
    }
}
