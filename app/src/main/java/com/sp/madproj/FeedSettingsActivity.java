package com.sp.madproj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FeedSettingsActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private final String astraDbUrl = "https://60fa55e9-e981-4ca4-8e90-d1dacc1dac57-eu-west-1.apps.astra.datastax.com/api/rest/v2/cql?keyspaceQP=plantopia";

    private ConstraintLayout authContainer;

    @Override
    public void onBackPressed() {
        if (authContainer.getVisibility() == View.VISIBLE) {
            authContainer.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

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

        findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        authContainer = findViewById(R.id.reauth_container);
        authContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authContainer.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                authContainer.setVisibility(View.VISIBLE);
            }
        });

        findViewById(R.id.authBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                AuthCredential credential = EmailAuthProvider
                        .getCredential(
                                user.getEmail(),
                                ((EditText) findViewById(R.id.passwordReauth)).getText().toString());

                user.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("USER REAUTH SUCCESS", "User re-authenticated.");

                                    String username = currentUser.getDisplayName();
                                    currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        deleteUser(username);
                                                    }
                                                }).start();

                                                auth.signOut();
                                                finish();
                                            } else {
                                                Log.e("USER DELETE ERROR", task.getException().toString());
                                                Toast.makeText(getApplicationContext(), "Error deleting account", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                } else {
                                    Log.e("USER REAUTH ERROR", task.getException().toString());
                                    Toast.makeText(getApplicationContext(), "Error reauthenticating account", Toast.LENGTH_SHORT).show();
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
                .placeholder(R.mipmap.default_pfp_foreground)
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
                        Log.d("USERS", response);
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
                return ("DELETE FROM plantopia.user_info WHERE username='" + username + "';").getBytes(StandardCharsets.UTF_8);
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