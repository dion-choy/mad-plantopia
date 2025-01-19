package com.sp.madproj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FeedSettingsActivity extends AppCompatActivity {
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private final String astraDbUrl = "https://60fa55e9-e981-4ca4-8e90-d1dacc1dac57-eu-west-1.apps.astra.datastax.com/api/rest/v2/cql?keyspaceQP=users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_settings);

        findViewById(R.id.logOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();
                finish();
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        deleteUser(currentUser.getDisplayName());
                    }
                });
                thread.start();

                currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            try {
                                thread.join();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                            auth.signOut();
                            finish();
                        } else {
                            Log.e("USER DELETE ERROR", task.getException().toString());
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
        }

        Picasso.get()
                .load(currentUser.getPhotoUrl())
                .into((ImageView) findViewById(R.id.pfpIcon));

        ((TextView) findViewById(R.id.username))
                .setText(currentUser.getDisplayName());

        ((TextView) findViewById(R.id.email))
                .setText(currentUser.getEmail());

    }

    private void deleteUser(String username) {
        RequestQueue queue = Volley.newRequestQueue(FeedSettingsActivity.this);

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                astraDbUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("USERS", response.toString());
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("USERS ERROR", error.toString());
                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return (" DELETE FROM users.user_info WHERE username='" + username + "';").getBytes(StandardCharsets.UTF_8);
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
}