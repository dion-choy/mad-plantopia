package com.sp.madproj;

import android.content.Context;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class Database {
    // AstrDB
    public static final String astraDbQueryUrl = "https://60fa55e9-e981-4ca4-8e90-d1dacc1dac57-eu-west-1.apps.astra.datastax.com/api/rest/v2/cql?keyspaceQP=plantopia";
    private static RequestQueue queue = null;

    public static void queryAstra(Context context, String query, Response.Listener<String> onResponse, Response.ErrorListener onError) {
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
        }

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                astraDbQueryUrl,
                onResponse,
                onError
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return query.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("x-cassandra-token", BuildConfig.ASTRA_DB_TOKEN);
                headers.put("Content-Type", "text/plain");
                return headers;
            }
        };

        queue.add(stringRequest);
    }

    // FIRESTORE REALTIME DB
    private static FirebaseDatabase database;
    private static final String databaseUrl = "https://plantopia-backend-ecce9-default-rtdb.asia-southeast1.firebasedatabase.app";
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
