package com.sp.madproj;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
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
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ChatFrag extends Fragment {
    private Animation disableSend;
    private Animation enableSend;

    private ConstraintLayout root;
    private DisplayMetrics displayMetrics;

    private EditText inputMsg;
    private ImageButton sendMsgBtn;
    private ActivityResultLauncher<Intent> getImage;

    private RecyclerView chatMessages;
    private List<Message> model = new ArrayList<Message>();
    private ChatAdapter chatAdapter;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference messages;

    private String username = "";
    private Uri pfp = null;

    public ChatFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chatroom, container, false);

        String roomName = getArguments().getString("roomName");
        if (roomName == null) {
            destroyFragment();
        }

        disableSend = AnimationUtils.loadAnimation(getContext(), R.anim.disable_send);
        enableSend = AnimationUtils.loadAnimation(getContext(), R.anim.enable_send);

        username = auth.getCurrentUser().getDisplayName();
        pfp = auth.getCurrentUser().getPhotoUrl();

        view.findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destroyFragment();
            }
        });

        ((TextView) view.findViewById(R.id.groupName)).setText(roomName);

        root = view.findViewById(R.id.root);
        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        chatAdapter = new ChatAdapter(model);
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                chatMessages.smoothScrollToPosition(chatAdapter.getItemCount());
            }
        });
        chatMessages = view.findViewById(R.id.chatMessages);
        chatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        chatMessages.setItemAnimator(new DefaultItemAnimator());
        chatMessages.setAdapter(chatAdapter);
        chatMessages.setOnTouchListener(swipeAway);

        messages = Database.get().getReference("rooms").child(roomName).child("messages");
        Log.d("Realtime DB", messages.toString());

        messages.keepSynced(true);
        messages.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                model.clear();
                for (DataSnapshot child: snapshot.getChildren()) {
                    model.add(child.getValue(Message.class));
                }
                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        inputMsg = view.findViewById(R.id.inputMsg);
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

        sendMsgBtn = view.findViewById(R.id.sendMessageBtn);
        sendMsgBtn.startAnimation(disableSend);
        sendMsgBtn.setEnabled(false);
        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pushKey = messages.push().getKey();
                Log.d("Firebase realtime db", pushKey);
                Message message = new Message(username, pfp, inputMsg.getText().toString().trim(), Timestamp.now());
                messages.child(pushKey).setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Log.d("REALTIME DB ADD", "onComplete: Notification added successfully!");
                    }
                });

                inputMsg.setText("");
            }
        });

        view.findViewById(R.id.sendImageBtn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                .setType("image/*");
                        getImage.launch(intent);
                    }
                }
        );

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            ClipData data = result.getData().getClipData();
                            if (data == null) {
                                Toast.makeText(getActivity().getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            UploadThread uploadThread = null;
                            for (int i = 0; i < data.getItemCount(); i++) {
                                Log.d("IMAGES", "onActivityResult: " + data.getItemAt(i).getUri().toString());

                                uploadThread = new UploadThread(data.getItemAt(i).getUri());
                                uploadThread.setPriority(i+1);
                                uploadThread.start();
                            }

                            try {
                                uploadThread.join();

                                for (int i = 0; i < imageKeyList.size(); i++) {
                                    Log.d("IMAGES KEY", "onActivityResult: " + imageKeyList.get(i));
                                }
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }

//                            Toast.makeText(getActivity().getApplicationContext(), data.getData().toString(), Toast.LENGTH_SHORT).show();
//                            Storage.uploadImgSupa(getContext(), data.getData(), Storage.chatroomImageStorage);
                        }
                    }
                });

        return view;
    }

    private List<String> imageKeyList = new ArrayList<>();
    private class UploadThread extends Thread {
        Uri imageUri;
        UploadThread(Uri imageUri) {
            this.imageUri = imageUri;
        }

        public void run() {
            imageKeyList.add(Storage.uploadImgSupa(getActivity(), imageUri, Storage.chatroomImageStorage));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.clear();
    }

    private void destroyFragment() {
        getParentFragmentManager()
                .beginTransaction()
                .remove(ChatFrag.this)
                .commit();
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

                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    destroyFragment();
                                                }
                                            }, 200);
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

                    if (Math.abs(motionEvent.getRawY() - initY) < (float) displayMetrics.heightPixels/16) {
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
        public ChatAdapter.ChatHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_chat, parent, false);
            return new ChatAdapter.ChatHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatAdapter.ChatHolder holder, int position) {
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
                holder.pfpIcon.setVisibility(View.VISIBLE);
                holder.username.setVisibility(View.VISIBLE);
            } else {
                holder.pfpIcon.setVisibility(View.GONE);
                holder.username.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(view -> {
                Toast.makeText(getActivity().getApplicationContext(), "message clicked", Toast.LENGTH_SHORT).show();
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