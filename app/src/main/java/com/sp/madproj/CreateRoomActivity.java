package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.NoConnectionError;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class CreateRoomActivity extends AppCompatActivity {
    private TextInputEditText groupName;
    private ImageView groupIcon;
    private String curImgKey = "default.png";
    private DatabaseReference rooms;
    private boolean success = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        groupName = findViewById(R.id.groupName);
        groupIcon = findViewById(R.id.groupIcon);

        rooms = Database.get().getReference("rooms");

        findViewById(R.id.doneBtn).setOnClickListener(view -> {
            if (groupName.getText().toString().isEmpty()) {
                groupName.setError("Enter a name!");
                return;
            }


            String pushKey = rooms.push().getKey();
            Log.d("Firebase realtime db", pushKey);

            Map<String, Object> childUpdate = new HashMap<>();
            childUpdate.put("/" + pushKey + "/info/", new Chatroom(groupName.getText().toString(), curImgKey));

            childUpdate.put("/" + pushKey + "/members/" + pushKey + "/",
                    FirebaseAuth.getInstance()
                            .getCurrentUser()
                            .getUid()
                    );

            rooms.updateChildren(childUpdate)
                    .addOnSuccessListener(unused -> {
                        Log.d("REALTIME DB ADD", "onComplete: Member added successfully!");
                        success = true;
                        insertRoom(pushKey);
                    }
                    );

            Log.d("Output", groupName.getText().toString() + ", " + curImgKey);
            finish();
        });

        findViewById(R.id.changePfp).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            getImage.launch(intent);
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!success) {
            Storage.deleteObjSupa(CreateRoomActivity.this, Storage.chatroomIconStorage + curImgKey);
        }
    }

    private final ActivityResultLauncher<Intent> getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivity.IMAGE_URI
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d("result", imageUri.toString());

                        curImgKey = Storage.uploadImgSupa(CreateRoomActivity.this, imageUri, Storage.chatroomIconStorage);

                        if (!curImgKey.equals(Storage.pfpStorage + "default.png")) {
                            Storage.deleteObjSupa(CreateRoomActivity.this, Storage.chatroomIconStorage + curImgKey);
                        }

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

    private void insertRoom(String id) {
        Database.queryAstra(this,
                "INSERT INTO plantopia.rooms (id) VALUES('" + id + "');",
                response -> Log.d("CREATE", "Room inserted successfully"),
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    Log.e("USER ERROR", "INSERT INTO plantopia.rooms (id) VALUES('" + id + "');");
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}