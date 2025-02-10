package com.sp.madproj.Chatroom;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
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
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.NoConnectionError;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.sp.madproj.Classes.Message;
import com.sp.madproj.Plant.CanvasView;
import com.sp.madproj.Utils.Database;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Storage;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatFrag extends Fragment {
    private Animation disableSend;
    private Animation enableSend;

    private ConstraintLayout root;
    private DisplayMetrics displayMetrics;

    private EditText inputMsg;
    private ImageButton sendMsgBtn;
    private ActivityResultLauncher<Intent> getImage;

    private RecyclerView chatMessages;
    private final List<Message> model = new ArrayList<>();
    private ChatAdapter chatAdapter;

    private final List<Uri> imagePreviewModel = new ArrayList<>();
    private ImagePreviewAdapter imagePreviewAdapter;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private DatabaseReference messages;

    private String username = "";
    private String uid = "";
    private Uri pfp = null;

    public ChatFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chatroom, container, false);

        getUsers();

        String roomKey = getArguments().getString("roomKey");
        String roomName = getArguments().getString("roomName");
        String iconKey = getArguments().getString("iconKey");
        if (roomKey == null) {
            destroyFragment();
        }
        Log.d("ROOM INFO", "RoomCode: "+ roomKey + "RoomName:" + roomName);

        disableSend = AnimationUtils.loadAnimation(getContext(), R.anim.disable_send);
        enableSend = AnimationUtils.loadAnimation(getContext(), R.anim.enable_send);

        username = auth.getCurrentUser().getDisplayName();
        uid = auth.getCurrentUser().getUid();
        pfp = auth.getCurrentUser().getPhotoUrl();

        view.findViewById(R.id.backBtn).setOnClickListener(view1 -> destroyFragment());

        ImageView groupIcon = view.findViewById(R.id.groupIcon);
        Picasso.get()
                .load(Storage.chatroomIconStorage + iconKey)
                .placeholder(R.mipmap.default_pfp_foreground)
                .into(groupIcon, new Callback() {
                    @Override
                    public void onSuccess() {
                        groupIcon.setImageTintList(null);
                    }

                    @Override
                    public void onError(Exception e) {}
                });

        ((TextView) view.findViewById(R.id.groupName)).setText(roomName);

        view.findViewById(R.id.chatTopBar).setOnClickListener(view2 -> {
            Intent intent = new Intent(getActivity(), ChatroomSettingsActivity.class);
            intent.putExtra("roomKey", roomKey);
            startActivity(intent);
        });


        root = view.findViewById(R.id.root);
        displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

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

        Database.get()
                .getReference("rooms")
                .child(roomKey)
                .child("members")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<>() {};
                        HashMap<String, String> info = snapshot.getValue(t);
                        if (!info.containsValue(uid)) {
                            new Handler().postDelayed(() -> destroyFragment(), 150);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        messages = Database.get().getReference("rooms").child(roomKey).child("messages");
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
        RecyclerView imagePreviews = view.findViewById(R.id.sendImagesList);
        imagePreviews.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        imagePreviews.setItemAnimator(new DefaultItemAnimator());
        imagePreviews.setAdapter(imagePreviewAdapter);

        view.findViewById(R.id.sendImageBtn).setOnClickListener(
                view3 -> {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            .setType("image/*");
                    getImage.launch(intent);
                }
        );

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        ClipData data = result.getData().getClipData();
                        if (data == null) {
                            if (Build.VERSION.SDK_INT < 29 && result.getData().getData() != null) {
                                imagePreviewModel.add(result.getData().getData());
                            } else {
                                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else if (data.getItemCount() > 10) {
                            Toast.makeText(getActivity(), "Too many images (Limit: 10)", Toast.LENGTH_SHORT).show();
                            return;
                        } else {
                            for (int i = 0; i < data.getItemCount(); i++) {
                                imagePreviewModel.add(data.getItemAt(i).getUri());
                                Log.d("IMAGES", "onActivityResult: " + data.getItemAt(i).getUri().toString());
                            }
                        }

                        sendMsgBtn.startAnimation(enableSend);
                        sendMsgBtn.setEnabled(true);
                        imagePreviewAdapter.notifyDataSetChanged();

                    }
                });

        return view;
    }

    private final View.OnClickListener sendMessage = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            String pushKey = messages.push().getKey();
            Log.d("Firebase realtime db", pushKey);
            Message message = new Message(username, uid, pfp, inputMsg.getText().toString().trim(), Timestamp.now());
            if (!imagePreviewModel.isEmpty()) {
                List<String> imageKeyList = new ArrayList<>();
                for (int i = 0; i < imagePreviewModel.size(); i++) {
                    imageKeyList.add(Storage.uploadImgSupa(getActivity(), imagePreviewModel.get(i), Storage.chatroomImageStorage));
                }

                message.imageKeys = imageKeyList;
                Log.d("IMAGE STORAGE API", "onClick: " + imageKeyList);

                imagePreviewModel.clear();
                imagePreviewAdapter.notifyDataSetChanged();
            }

            messages.child(pushKey).setValue(message).addOnCompleteListener(
                    task -> Log.d("REALTIME DB ADD", "onComplete: Message added successfully!")
            );

            inputMsg.setText("");
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        model.clear();
        requireActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
    }

    private void destroyFragment() {
        if (isAdded()) {
            getParentFragmentManager()
                    .beginTransaction()
                    .remove(ChatFrag.this)
                    .commit();
        }
    }

    private final View.OnTouchListener swipeAway = new View.OnTouchListener() {
        private boolean moving = false;
        private float offsetX, initY;

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch(motionEvent.getAction()) {
                case MotionEvent.ACTION_UP:
                    if (root.getX() > (float) displayMetrics.widthPixels /5) {
                        root.animate()
                                .x(displayMetrics.widthPixels )
                                .setDuration(200)
                                .start();

                        Handler handler = new Handler();
                        handler.postDelayed(() -> destroyFragment(), 200);
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
                case MotionEvent.ACTION_BUTTON_RELEASE:
                    view.performClick();
                    break;
            }
            return false;
        }
    };

    private class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewHolder> {
        private final List<Uri> imageUris;
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

            Matrix matrix = new Matrix();
            switch (Storage.getBitmapOriention(getActivity(), uri)) {
                case ExifInterface.ORIENTATION_NORMAL:
                    matrix.postRotate(0);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.postRotate(90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.postRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.postRotate(270);
                    break;
            }

            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            Bitmap.createScaledBitmap(bitmap, (int) CanvasView.pxFromDp(80, requireContext()),
                    (int) CanvasView.pxFromDp(80, requireContext()), false);
            holder.image.setImageBitmap(bitmap);

            holder.itemView.setOnClickListener(view -> {
                imagePreviewModel.remove(uri);
                imagePreviewAdapter.notifyItemRemoved(holder.getAdapterPosition());

                if (imagePreviewModel.isEmpty()) {
                    sendMsgBtn.startAnimation(disableSend);
                    sendMsgBtn.setEnabled(false);
                }
            });
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
        }

        class ImagePreviewHolder extends RecyclerView.ViewHolder {
            private final ImageView image;
            public ImagePreviewHolder(View view) {
                super(view);
                this.image = view.findViewById(R.id.newImagePreview);
            }
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatHolder> {
        private final List<Message> messages;
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
            if (message.messageTxt.isEmpty()) {
                holder.chatMessage.setVisibility(View.GONE);
            } else {
                holder.chatMessage.setVisibility(View.VISIBLE);
                holder.chatMessage.setText(message.messageTxt);
            }

            if (!message.username.equals(prevUsername)) {
                String usernameStr;
                String pfpStr;
                if (!users.isEmpty() && users.get(message.uid) != null){
                    Log.d("GET USERS", users.toString());
                    usernameStr  = users.get(message.uid).get("username");
                    pfpStr = users.get(message.uid).get("pfp");
                } else {
                    usernameStr  = message.username;
                    pfpStr = message.pfp;
                }

                holder.username.setText(usernameStr);
                Picasso.get()
                        .load(pfpStr)
                        .resize((int) CanvasView.pxFromDp(50, requireContext()),
                                (int) CanvasView.pxFromDp(50, requireContext()))
                        .centerCrop()
                        .placeholder(R.mipmap.default_pfp_foreground)
                        .into(holder.pfpIcon, new Callback() {
                            @Override
                            public void onSuccess() {
                                holder.pfpIcon.setImageTintList(null);
                            }

                            @Override
                            public void onError(Exception e) {
                            }
                        });
                holder.pfpIcon.setVisibility(View.VISIBLE);
                holder.username.setVisibility(View.VISIBLE);
            } else {
                holder.pfpIcon.setVisibility(View.GONE);
                holder.username.setVisibility(View.GONE);
            }

            Log.d("Images", message.imageKeys.toString());
            if (message.imageKeys == null || message.imageKeys.isEmpty()) {
                holder.imageContainer.setVisibility(View.GONE);
            } else {
                holder.imageContainer.setVisibility(View.VISIBLE);
            }

            if (message.imageKeys != null && !message.imageKeys.isEmpty()) {
                holder.imageTopLeft.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load(Storage.chatroomImageStorage + message.imageKeys.get(0))
                        .resize((int) CanvasView.pxFromDp(330, requireContext()),
                                (int) CanvasView.pxFromDp(330, requireContext()))
                        .centerCrop()
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
                        .resize((int) CanvasView.pxFromDp(165, requireContext()),
                                (int) CanvasView.pxFromDp(330, requireContext()))
                        .centerCrop()
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
                        .resize((int) CanvasView.pxFromDp(330, requireContext()),
                                (int) CanvasView.pxFromDp(165, requireContext()))
                        .centerCrop()
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
                        .resize((int) CanvasView.pxFromDp(165, requireContext()),
                                (int) CanvasView.pxFromDp(165, requireContext()))
                        .centerCrop()
                        .placeholder(R.drawable.gallery)
                        .into(holder.imageBottomRight);
                Log.d("CHAT IMAGE", "onBindViewHolder: 4 Viewable");
            } else {
                holder.imageBottomRight.setVisibility(View.GONE);
            }

            if (message.imageKeys != null && message.imageKeys.size() > 4) {
                holder.extraImages.setVisibility(View.VISIBLE);
                holder.extraImages.setText(String.format(Locale.ENGLISH, "+%d", message.imageKeys.size() - 3));
            } else {
                holder.extraImages.setVisibility(View.GONE);
            }

            holder.itemView.setOnTouchListener(swipeAway);

//            holder.itemView.setOnClickListener(view ->
//                Toast.makeText(getActivity(), "message clicked", Toast.LENGTH_SHORT).show()
//            );

            holder.imageContainer.setOnClickListener(view -> {
                Intent intent = new Intent(getContext(), ExpandImages.class);
                intent.putStringArrayListExtra("images", (ArrayList<String>) message.imageKeys);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        class ChatHolder extends RecyclerView.ViewHolder{
            private final TextView chatMessage;
            private final TextView username;
            private final ImageView pfpIcon;

            private final CardView imageContainer;
            private final TextView extraImages;
            private final ImageView imageTopLeft;
            private final ImageView imageTopRight;
            private final ImageView imageBottomLeft;
            private final ImageView imageBottomRight;
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

    private final Map<String, Map<String, String>> users = new HashMap<>();

    private void getUsers() {
        Database.queryAstra(getActivity(),
                "SELECT * FROM plantopia.user_info",
                response -> {
                    Log.d("GET USERS", response);

                    try {
                        JSONObject responseObj = new JSONObject(response);
                        JSONArray data = responseObj.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject row = data.getJSONObject(i);
                            Map<String, String> usernamePfp = new HashMap<>();
                            usernamePfp.put("username", row.getString("username"));
                            usernamePfp.put("pfp", row.getString("pfp"));
                            users.put(row.getString("uid"), usernamePfp);
                        }

                        Log.d("GET USERS", users.toString());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("GET USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}