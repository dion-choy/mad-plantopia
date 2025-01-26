package com.sp.madproj;

import com.google.firebase.database.FirebaseDatabase;

public class Database {
    public static final String astraDbQueryUrl = "https://60fa55e9-e981-4ca4-8e90-d1dacc1dac57-eu-west-1.apps.astra.datastax.com/api/rest/v2/cql?keyspaceQP=plantopia";
    private static final String databaseUrl = "https://plantopia-backend-ecce9-default-rtdb.asia-southeast1.firebasedatabase.app";
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
