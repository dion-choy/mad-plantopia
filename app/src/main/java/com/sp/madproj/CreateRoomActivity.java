package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class CreateRoomActivity extends AppCompatActivity {
    private TextInputEditText groupName;
    private ImageView groupIcon;
    private ActivityResultLauncher<Intent> getImage;
    private String curImgKey = "default.png";
    private DatabaseReference rooms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        groupName = findViewById(R.id.groupName);
        groupIcon = findViewById(R.id.groupIcon);

        rooms = Database.get().getReference("rooms");

        findViewById(R.id.doneBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean error = false;
                if (groupName.getText().toString().isEmpty()) {
                    groupName.setError("Enter a name!");
                    return;
                }


                String pushKey = rooms.push().getKey();
                Log.d("Firebase realtime db", pushKey);

                rooms.child(pushKey)
                        .child("info")
                        .setValue(new Chatroom(groupName.getText().toString(), curImgKey))
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d("REALTIME DB ADD", "onComplete: Chatroom added successfully!");
                            String memberKey = rooms.child(pushKey)
                                    .child("members")
                                    .push().getKey();
                            rooms.child(pushKey)
                                    .child("members")
                                    .child(pushKey)
                                    .setValue(
                                            FirebaseAuth.getInstance()
                                                    .getCurrentUser()
                                                    .getEmail()
                                    ). addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d("REALTIME DB ADD", "onComplete: Member added successfully!");
                                            }
                                        }
                                    });
                        }
                    }
                });

                Log.d("Output", groupName.getText().toString() + ", " + curImgKey);
                finish();
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

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivity.IMAGE_URI
                                && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            Log.d("result", imageUri.toString());

                            curImgKey = Storage.uploadImgSupa(CreateRoomActivity.this, imageUri, Storage.chatroomIconStorage);

                            if (!curImgKey.equals(Storage.pfpStorage + "default.png")) {
                                Storage.deleteObjSup(CreateRoomActivity.this, Storage.chatroomIconStorage + curImgKey);
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
                                        public void onError(Exception e) {}
                                    });
                        }
                    }
                });
    }
}