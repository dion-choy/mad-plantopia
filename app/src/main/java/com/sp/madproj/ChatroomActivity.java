package com.sp.madproj;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChatroomActivity extends AppCompatActivity {

    private Animation disableSend;
    private Animation enableSend;

    private ConstraintLayout root;
    private DisplayMetrics displayMetrics;

    private EditText inputMsg;
    private ImageButton sendMsgBtn;

    private RecyclerView chatMessages;
    private List<Message> model = new ArrayList<Message>();
    private ChatAdapter chatAdapter;

    private final String databaseUrl = " https://plantopia-backend-ecce9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private final FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference realtimeDB;

    private String username = "";
    private Uri pfp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);

        disableSend = AnimationUtils.loadAnimation(this, R.anim.disable_send);
        enableSend = AnimationUtils.loadAnimation(this, R.anim.enable_send);

        username = auth.getCurrentUser().getDisplayName();
        pfp = auth.getCurrentUser().getPhotoUrl();

        root = findViewById(R.id.root);
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        chatAdapter = new ChatAdapter(model);
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                chatMessages.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        });

        chatMessages = findViewById(R.id.chatMessages);
        chatMessages.setLayoutManager(new LinearLayoutManager(this));
        chatMessages.setItemAnimator(new DefaultItemAnimator());
        chatMessages.setAdapter(chatAdapter);
        chatMessages.setOnTouchListener(swipeAway);

        realtimeDB = database.getReference("rooms");
        Log.d("Realtime DB", realtimeDB.toString());

        realtimeDB.keepSynced(true);

        realtimeDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                model.add(snapshot.getValue(Message.class));
                chatAdapter.notifyItemInserted(chatAdapter.getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        inputMsg = findViewById(R.id.inputMsg);
        inputMsg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (!inputMsg.getText().toString().trim().isEmpty() && !sendMsgBtn.isEnabled()) {
                    sendMsgBtn.startAnimation(enableSend);
                    sendMsgBtn.setEnabled(true);
                } else if (inputMsg.getText().toString().trim().isEmpty() && sendMsgBtn.isEnabled()) {
                    sendMsgBtn.startAnimation(disableSend);
                    sendMsgBtn.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        sendMsgBtn = findViewById(R.id.sendMessageBtn);
        sendMsgBtn.startAnimation(disableSend);
        sendMsgBtn.setEnabled(false);
        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pushKey = realtimeDB.push().getKey();
                Log.d("Firebase realtime db", pushKey);
                Message message = new Message(username, pfp, inputMsg.getText().toString().trim(), Timestamp.now());
                realtimeDB.child(pushKey).setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("REALTIME DB ADD", "onComplete: Notification added successfully!");
                    }
                });

                inputMsg.setText("");
            }
        });
    }


    private boolean moving = false;
    private float offsetX, initY;

    View.OnTouchListener swipeAway = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (root.getX() > (float) displayMetrics.widthPixels /2) {
                        root.animate()
                                .x(displayMetrics.widthPixels )
                                .setDuration(200)
                                .start();

                        finish();
                    } else {
                        root.animate()
                                .x(0)
                                .setDuration(200)
                                .start();
                    }
                    moving = false;

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (!moving) {
                        offsetX = root.getX() - motionEvent.getRawX();
                        initY = motionEvent.getRawY();
                        moving = true;
                        break;
                    }

                    if (Math.abs(motionEvent.getRawY() - initY) < (float) displayMetrics.heightPixels/8) {
                        if (motionEvent.getRawX() > -offsetX || root.getX() > 0) {
                            root.animate()
                                    .x(motionEvent.getRawX() + offsetX)
                                    .setDuration(0)
                                    .start();
                        } else {
                            root.animate()
                                    .x(50 * (motionEvent.getRawX() + offsetX) / displayMetrics.widthPixels)
                                    .setDuration(0)
                                    .start();
                        }
                    } else {
                        offsetX = root.getX() - motionEvent.getRawX();
                        root.animate()
                                .x(0)
                                .setDuration(0)
                                .start();
                    }
                    break;
            }
            view.performClick();
            return false;
        }
    };

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatHolder> {
        private List<Message> messages = null;
        public ChatAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @NonNull
        @Override
        public ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_chat, parent, false);
            return new ChatHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatHolder holder, int position) {
            String prevUsername = "";
            if (position > 0) {
                prevUsername = messages.get(position-1).username;
            }

            Message message = messages.get(position);
            holder.chatMessage.setText(message.messageTxt);

            if (!message.username.equals(prevUsername)) {
                holder.username.setText(message.username);
                Picasso.get()
                        .load(message.pfp)
                        .into(holder.pfpIcon);
            } else {
                holder.pfpIcon.setVisibility(View.GONE);
                holder.username.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(view -> {
                Toast.makeText(ChatroomActivity.this.getApplicationContext(), "message clicked", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatHolder extends RecyclerView.ViewHolder{
            private TextView chatMessage;
            private TextView username;
            private ImageView pfpIcon;
            public ChatHolder(View view) {
                super(view);
                this.chatMessage = view.findViewById(R.id.chatMessage);
                this.username = view.findViewById(R.id.username);
                this.pfpIcon = view.findViewById(R.id.pfpIcon);
            }
        }
    }
}