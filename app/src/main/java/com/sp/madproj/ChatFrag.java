package com.sp.madproj;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
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
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    private RecyclerView imagePreviews;
    private List<Uri> imagePreviewModel = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;

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
                if ((!inputMsg.getText().toString().trim().isEmpty() || !imagePreviewModel.isEmpty()) && !sendMsgBtn.isEnabled())  {
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
        sendMsgBtn.setOnClickListener(sendMessage);

        imagePreviewAdapter = new ImagePreviewAdapter(imagePreviewModel);
        imagePreviews = view.findViewById(R.id.sendImagesList);
        imagePreviews.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagePreviews.setItemAnimator(new DefaultItemAnimator());
        imagePreviews.setAdapter(imagePreviewAdapter);

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
                            } else if (data.getItemCount() > 10) {
                                Toast.makeText(getActivity().getApplicationContext(), "Too many images (Limit: 10)", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            for (int i = 0; i < data.getItemCount(); i++) {
                                imagePreviewModel.add(data.getItemAt(i).getUri());
                                Log.d("IMAGES", "onActivityResult: " + data.getItemAt(i).getUri().toString());
                            }
                            sendMsgBtn.startAnimation(enableSend);
                            sendMsgBtn.setEnabled(true);
                            imagePreviewAdapter.notifyDataSetChanged();

                        }
                    }
                });

        return view;
    }

    private View.OnClickListener sendMessage = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String pushKey = messages.push().getKey();
            Log.d("Firebase realtime db", pushKey);
            Message message = new Message(username, pfp, inputMsg.getText().toString().trim(), Timestamp.now());
            if (!imagePreviewModel.isEmpty()) {
                List<String> imageKeyList = new ArrayList<>();
                for (int i = 0; i < imagePreviewModel.size(); i++) {
                    imageKeyList.add(Storage.uploadImgSupa(getActivity(), imagePreviewModel.get(i), Storage.chatroomImageStorage));
                }

                message.imageKeys = imageKeyList;
                Log.d("IMAGE STORAGE API", "onClick: " + imageKeyList.toString());

                imagePreviewModel.clear();
                imagePreviewAdapter.notifyDataSetChanged();
            }

            messages.child(pushKey).setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Log.d("REALTIME DB ADD", "onComplete: Message added successfully!");
                }
            });

            inputMsg.setText("");
        }
    };

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

    private View.OnTouchListener swipeAway = new View.OnTouchListener() {
        private boolean moving = false;
        private float offsetX, initY;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (root.getX() > (float) displayMetrics.widthPixels /4) {
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

    private class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewHolder> {
        private List<Uri> imageUris = null;
        public ImagePreviewAdapter(List<Uri> messages) {
            this.imageUris = messages;
        }

        @NonNull
        @Override
        public ImagePreviewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_chat_image_added, parent, false);
            return new ImagePreviewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImagePreviewHolder holder, int position) {
            Uri uri = imageUris.get(position);
            Picasso.get()
                    .load(uri)
                    .placeholder(R.drawable.gallery)
                    .into(holder.image);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    imagePreviewModel.remove(uri);
                    imagePreviewAdapter.notifyDataSetChanged();

                    if (imagePreviewModel.isEmpty()) {
                        sendMsgBtn.startAnimation(disableSend);
                        sendMsgBtn.setEnabled(false);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        class ImagePreviewHolder extends RecyclerView.ViewHolder{
            private ImageView image;
            public ImagePreviewHolder(View view) {
                super(view);
                this.image = view.findViewById(R.id.newImagePreview);
            }
        }
    }

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
            if (message.messageTxt.equals("")) {
                holder.chatMessage.setVisibility(View.GONE);
            } else {
                holder.chatMessage.setVisibility(View.VISIBLE);
                holder.chatMessage.setText(message.messageTxt);
            }

            if (!message.username.equals(prevUsername)) {
                holder.username.setText(message.username);
                Picasso.get()
                        .load(message.pfp)
                        .placeholder(R.mipmap.default_pfp_foreground)
                        .into(holder.pfpIcon);
                holder.pfpIcon.setVisibility(View.VISIBLE);
                holder.username.setVisibility(View.VISIBLE);
            } else {
                holder.pfpIcon.setVisibility(View.GONE);
                holder.username.setVisibility(View.GONE);
            }

            Log.d("Images", message.imageKeys.toString());
            if (message.imageKeys == null || message.imageKeys.size() == 0) {
                holder.imageContainer.setVisibility(View.GONE);
            } else {
                holder.imageContainer.setVisibility(View.VISIBLE);
            }

            if (message.imageKeys != null && !message.imageKeys.isEmpty()) {
                holder.imageTopLeft.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(Storage.chatroomImageStorage + message.imageKeys.get(0))
                        .placeholder(R.drawable.gallery)
                        .into(holder.imageTopLeft);
                Log.d("CHAT IMAGE", "onBindViewHolder: 1 Viewable");
            } else {
                holder.imageTopLeft.setVisibility(View.GONE);
            }

            if (message.imageKeys != null && message.imageKeys.size() >= 2) {
                holder.imageTopRight.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(Storage.chatroomImageStorage + message.imageKeys.get(1))
                        .placeholder(R.drawable.gallery)
                        .into(holder.imageTopRight);
                Log.d("CHAT IMAGE", "onBindViewHolder: 2 Viewable");
            } else {
                holder.imageTopRight.setVisibility(View.GONE);
            }

            if (message.imageKeys != null && message.imageKeys.size() >= 3) {
                holder.imageBottomLeft.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(Storage.chatroomImageStorage + message.imageKeys.get(2))
                        .placeholder(R.drawable.gallery)
                        .into(holder.imageBottomLeft);
                Log.d("CHAT IMAGE", "onBindViewHolder: 3 Viewable");
            } else {
                holder.imageBottomLeft.setVisibility(View.GONE);
            }

            if (message.imageKeys != null && message.imageKeys.size() >= 4) {
                holder.imageBottomRight.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(Storage.chatroomImageStorage + message.imageKeys.get(3))
                        .placeholder(R.drawable.gallery)
                        .into(holder.imageBottomRight);
                Log.d("CHAT IMAGE", "onBindViewHolder: 3 Viewable");
            } else {
                holder.imageBottomRight.setVisibility(View.GONE);
            }

            if (message.imageKeys != null && message.imageKeys.size() > 4) {
                holder.extraImages.setVisibility(View.VISIBLE);
                holder.extraImages.setText(String.format(Locale.ENGLISH, "+%d", message.imageKeys.size() - 3));
            } else {
                holder.imageBottomRight.setVisibility(View.GONE);
            }

            holder.itemView.setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), ExpandImages.class);
                intent.putStringArrayListExtra("images", (ArrayList<String>) message.imageKeys);
                startActivity(intent);
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

            private CardView imageContainer;
            private TextView extraImages;
            private ImageView imageTopLeft;
            private ImageView imageTopRight;
            private ImageView imageBottomLeft;
            private ImageView imageBottomRight;
            public ChatHolder(View view) {
                super(view);
                this.chatMessage = view.findViewById(R.id.chatMessage);
                this.username = view.findViewById(R.id.username);
                this.pfpIcon = view.findViewById(R.id.pfpIcon);

                this.imageContainer = view.findViewById(R.id.imageContainer);
                this.extraImages = view.findViewById(R.id.extraImages);
                this.imageTopLeft = view.findViewById(R.id.imageTopLeft);
                this.imageTopRight = view.findViewById(R.id.imageTopRight);
                this.imageBottomLeft = view.findViewById(R.id.imageBottomLeft);
                this.imageBottomRight = view.findViewById(R.id.imageBottomRight);
            }
        }
    }
}