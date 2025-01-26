package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class ChatroomSettingsActivity extends AppCompatActivity {
    private String curImgKey = "default.png";
    private String roomKey = null;
    private ImageView groupIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom_settings);

        findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        roomKey = getIntent().getStringExtra("roomKey");
        if (roomKey == null) {
            finish();
        }

        DatabaseReference chatInfo = Database.get().getReference("rooms").child(roomKey).child("info");
        DatabaseReference chatMembers = Database.get().getReference("rooms").child(roomKey).child("members");

        chatInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("REALTIME", "onChildAdded: " + snapshot.toString());

                GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<HashMap<String, String>>() {};
                HashMap<String, String> info = snapshot.getValue(t);
                if (info != null) {
                    Log.d("REALTIME", "onChildAdded: " + info.toString());
                    ((TextView) findViewById(R.id.groupName)).setText(info.get("name"));

                    groupIcon = findViewById(R.id.groupIcon);
                    curImgKey = info.get("iconKey");
                    Picasso.get()
                            .load(Storage.chatroomIconStorage + curImgKey)
                            .placeholder(R.mipmap.default_pfp_foreground)
                            .into(groupIcon, new Callback() {
                                @Override
                                public void onSuccess() {
                                    groupIcon.setImageTintList(null);
                                }

                                @Override
                                public void onError(Exception e) {}
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        chatMembers.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("REALTIME", "onChildAdded: " + snapshot.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        findViewById(R.id.changePfp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                getImage.launch(intent);
            }
        });

        findViewById(R.id.inviteMembers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getCode(roomKey);
            }
        });
    }

    private void generateAndDisplayCode() {
        String code = "";
        for (int i = 0; i < 6; i++) {
            code += String.valueOf((int) Math.floor(Math.random()*10));
        }

        updateCodeDb(roomKey, code);
    }

    private void updateCodeDb(String id, String code) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Database.astraDbQueryUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ROOMS", code);
                        Toast.makeText(ChatroomSettingsActivity.this, "Code: " + code, Toast.LENGTH_LONG).show();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("USERS ERROR", error.toString());
                Log.e("USER ERROR", "UPDATE plantopia.rooms SET code='" + code + "', generated_time=toTimestamp(now()) WHERE id = '" + id + "';");
                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return ("UPDATE plantopia.rooms SET code='" + code + "', generated_time=toTimestamp(now()) WHERE id = '" + id + "';")
                        .getBytes(StandardCharsets.UTF_8);
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


    private void getCode(String id) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Database.astraDbQueryUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("ROOMS", response);
                        try {
                            JSONObject responseObj = new JSONObject(response);
                            Log.d("ROOMS", responseObj.toString());
                            if (responseObj.getJSONArray("data").getJSONObject(0).isNull("generated_time")) {
                                generateAndDisplayCode();
                                return;
                            }

                            OffsetDateTime generatedTime = OffsetDateTime.parse(responseObj.getJSONArray("data").getJSONObject(0).getString("generated_time"));
                            OffsetDateTime nowTime = OffsetDateTime.now();
                            long time = ChronoUnit.HOURS.between(generatedTime, nowTime);
                            if (time > 1) {
                                generateAndDisplayCode();
                            } else if (!responseObj.getJSONArray("data").getJSONObject(0).isNull("code")) {
                                String roomCode = responseObj.getJSONArray("data").getJSONObject(0).getString("code");
                                Log.d("ROOMS", roomCode);
                                Toast.makeText(ChatroomSettingsActivity.this, "Code: " + roomCode, Toast.LENGTH_LONG).show();
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("USERS ERROR", error.toString());
                Log.e("USER ERROR", "SELECT * FROM user_info WHERE plantopia.username = '" + id + "';");
                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return ("SELECT * FROM plantopia.rooms WHERE id = '" + id + "';").getBytes(StandardCharsets.UTF_8);
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

    private final ActivityResultLauncher<Intent> getImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivity.IMAGE_URI
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d("result", imageUri.toString());

                        if (!curImgKey.equals(Storage.pfpStorage + "default.png")) {
                            Storage.deleteObjSup(ChatroomSettingsActivity.this, Storage.chatroomIconStorage + curImgKey);
                        }

                        curImgKey = Storage.uploadImgSupa(ChatroomSettingsActivity.this, imageUri, Storage.chatroomIconStorage);
                        Database.get()
                                .getReference("rooms")
                                .child(roomKey)
                                .child("info")
                                .child("iconKey")
                                .setValue(curImgKey);

                        Picasso.get()
                                .load(Storage.chatroomIconStorage + curImgKey)
                                .placeholder(R.mipmap.default_pfp_foreground)
                                .into(groupIcon, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        groupIcon.setImageTintList(null);
                                    }

                                    @Override
                                    public void onError(Exception e) {}
                                });
                    }
                }
            });
}