package com.sp.madproj.Chatroom;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.NoConnectionError;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.sp.madproj.Classes.User;
import com.sp.madproj.Utils.Database;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Storage;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatroomSettingsActivity extends AppCompatActivity {
    private String curImgKey = "default.png";
    private String roomKey = null;
    private ImageView groupIcon;

    private final List<User> membersModel = new ArrayList<>();
    private MembersAdapter membersAdapter;

    private TextView selectedUserKey;
    private ConstraintLayout confirmRemove;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private HashMap<String, String> members;
    private DatabaseReference membersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom_settings);

        fadeOutBg = AnimationUtils.loadAnimation(this, R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(this, R.anim.fadein_bg);

        selectedUserKey = findViewById(R.id.selectedEmail);
        confirmRemove = findViewById(R.id.confirmRemove);
        confirmRemove.setOnClickListener(view -> {
            Log.d("FADE OUT CLICK", "now");
            confirmRemove.startAnimation(fadeOutBg);
            confirmRemove.setVisibility(View.GONE);
            new Handler().postDelayed(() -> confirmRemove.clearAnimation(), 300);
        });
        findViewById(R.id.removeBtn).setOnClickListener(view -> {
            membersRef.child(selectedUserKey.getText().toString()).removeValue();
            confirmRemove.performClick();
        });

        membersAdapter = new MembersAdapter(membersModel);

        RecyclerView membersList = findViewById(R.id.membersList);
        membersList.setHasFixedSize(true);
        membersList.setLayoutManager(new LinearLayoutManager(this));
        membersList.setItemAnimator(new DefaultItemAnimator());
        membersList.setAdapter(membersAdapter);
        
        getUsers();

        findViewById(R.id.backBtn).setOnClickListener(view -> finish());

        roomKey = getIntent().getStringExtra("roomKey");
        if (roomKey == null) {
            finish();
        }

        DatabaseReference chatInfo = Database.get().getReference("rooms").child(roomKey).child("info");

        membersRef = Database.get().getReference("rooms").child(roomKey).child("members");

        chatInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("REALTIME", "onChildAdded: " + snapshot);

                GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<>() {
                };
                HashMap<String, String> info = snapshot.getValue(t);
                if (info == null) {
                    return;
                }

                Log.d("REALTIME", "onChildAdded: " + info);
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        membersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                membersModel.clear();
                Log.d("REALTIME", "onChildAdded: " + snapshot);
                GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<>() {
                };
                members = snapshot.getValue(t);
                Log.d("GET USERS", "Call updateModel()");
                updateModel();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        findViewById(R.id.changePfp).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            getImage.launch(intent);
        });

        findViewById(R.id.inviteMembers).setOnClickListener(view -> getCode(roomKey));
    }

    private class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MembersHolder>{
        List<User> users;
        public MembersAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public MembersHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_add_del_user, parent, false);
            return new MembersHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MembersHolder holder, int position) {
            holder.name.setText(users.get(position).username);
            Picasso.get()
                    .load(Uri.parse(users.get(position).pfp))
                    .placeholder(R.mipmap.default_pfp_foreground)
                    .into(holder.pfpIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.pfpIcon.setImageTintList(null);
                        }

                        @Override
                        public void onError(Exception e) {}
                    });


            String userKey = "";
            String uid = users.get(holder.getAdapterPosition()).uid;
            for (Map.Entry<String, String> entry : members.entrySet()) {
                if (uid.equals(entry.getValue())) {
                    userKey = entry.getKey();
                    break;
                }
            }

            Log.d("TEST LOAD", userKey + roomKey);
            if (userKey.equals(roomKey)) {
                holder.removeMember.setVisibility(View.GONE);
            } else {
                holder.removeMember.setVisibility(View.VISIBLE);
            }

            final String finalUserKey = userKey;
            holder.removeMember.setOnClickListener(view -> {
                confirmRemove.startAnimation(fadeInBg);
                confirmRemove.setVisibility(View.VISIBLE);
                selectedUserKey.setText(finalUserKey);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class MembersHolder extends RecyclerView.ViewHolder{
            private final TextView name;
            private final ImageView pfpIcon;
            private final ImageButton removeMember;
            public MembersHolder(View view) {
                super(view);
                name = view.findViewById(R.id.username);
                pfpIcon = view.findViewById(R.id.pfpIcon);
                removeMember = view.findViewById(R.id.removeMember);
            }
        }
    }

    JSONArray allUsers;
    private void updateModel() {
        if (allUsers == null || members == null) {
            return;
        }

        Log.d("GET USERS", members + ", " + allUsers);
        boolean thisIn = false;
        for (int i = 0; i < allUsers.length(); i++) {
            try {
                JSONObject row = allUsers.getJSONObject(i);
                if (members.containsValue(row.getString("uid"))) {
                    if (row.getString("uid").equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                        thisIn = true;
                    }
                    membersModel.add(new User(row.getString("username"), row.getString("uid"), row.getString("pfp")));
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            membersAdapter.notifyDataSetChanged();
        }
        if (!thisIn) {
            finish();
        }
    }
    private void getUsers() {
        Database.queryAstra(this,
                "SELECT * FROM plantopia.user_info",
                response -> {
                    Log.d("GET USERS", response);

                    try {
                        JSONObject responseObj = new JSONObject(response);
                        allUsers = responseObj.getJSONArray("data");
                        updateModel();
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("GET USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void generateAndDisplayCode() {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int) Math.floor(Math.random() * 10));
        }

        updateCodeDb(roomKey, code.toString());
    }

    private void updateCodeDb(String id, String code) {
        Database.queryAstra(this,
                "UPDATE plantopia.rooms SET code='" + code + "', generated_time=toTimestamp(now()) WHERE id = '" + id + "';",
                response -> {
                    Log.d("ROOMS", code);
                    Toast.makeText(ChatroomSettingsActivity.this, "Code: " + code, Toast.LENGTH_LONG).show();
                },
                error -> {
                    Log.e("UPDATE CODE ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void getCode(String id) {
        Database.queryAstra(this,
                "SELECT * FROM plantopia.rooms WHERE id = '" + id + "';",
                response -> {
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
                            return;
                        }

                        if (responseObj.getJSONArray("data").length() != 1) {
                            getCode(id);
                        } else {
                            String roomCode = responseObj.getJSONArray("data").getJSONObject(0).getString("code");
                            Log.d("ROOMS", roomCode);
                            Toast.makeText(ChatroomSettingsActivity.this, "Code: " + roomCode, Toast.LENGTH_LONG).show();
                        }

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("GET CODE ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private final ActivityResultLauncher<Intent> getImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d("result", imageUri.toString());

                        if (!curImgKey.equals(Storage.pfpStorage + "default.png")) {
                            Storage.deleteObjSupa(ChatroomSettingsActivity.this, Storage.chatroomIconStorage + curImgKey);
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
                                    public void onError(Exception e) {
                                    }
                                });
                    }
                }
            });
}