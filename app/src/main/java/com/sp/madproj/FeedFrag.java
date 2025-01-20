package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class FeedFrag extends Fragment {
    private Animation fromBottomFabAnim;
    private Animation toBottomFabAnim;
    private Animation rotateClockWiseFabAnim;
    private Animation rotateAntiClockWiseFabAnim;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private ActivityResultLauncher<Intent> getImage;

    private FloatingActionButton addPost;
    private FloatingActionButton openCamBtn;
    private FloatingActionButton openGalleryBtn;
    private TextView shade;


    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;
    private String username = "";
    private String email = "";


    private RecyclerView chatRooms;
    private List<Chatroom> model = new ArrayList<Chatroom>();
    private ChatRoomAdapter chatRoomAdapter;


    public FeedFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        currentUser = auth.getCurrentUser();
        Log.d("RESUME", "FEED RESUMED");
        if (currentUser == null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.viewFrag, new LandingPageFrag())
                    .addToBackStack(null)
                    .commit();
        } else {
            username = currentUser.getDisplayName();
            email = currentUser.getEmail();

            Log.d("USER NAME: ", username);
            Log.d("USER EMAIL: ", email);
        }

        DatabaseReference chats = Database.get().getReference();
        chats.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateMenu(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateMenu(snapshot);
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                updateMenu(snapshot);
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateMenu(DataSnapshot snapshot) {
        model.clear();

        for (DataSnapshot child: snapshot.getChildren()) {
            Log.d("REALTIME", "onChildAdded: " + child.toString());
            DatabaseReference chatInfo = child.getRef().child("info");
            Log.d("REALTIME", "onChildAdded: " + chatInfo);
            chatInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    model.add(snapshot.getValue(Chatroom.class));
                    chatRoomAdapter.notifyItemInserted(model.size());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

        ((ImageButton) view.findViewById(R.id.settingsBtn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), FeedSettingsActivity.class);
                startActivity(intent);
            }
        });

        chatRoomAdapter = new ChatRoomAdapter(model);
        chatRoomAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                chatRooms.smoothScrollToPosition(chatRoomAdapter.getItemCount());
            }
        });

        chatRooms = view.findViewById(R.id.chatRooms);
        chatRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRooms.setItemAnimator(new DefaultItemAnimator());
        chatRooms.setAdapter(chatRoomAdapter);

        addPost = view.findViewById(R.id.addPost);
        addPost.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (addPost.getContentDescription().equals("Open options")) {
                            openOptions();
                        } else if (addPost.getContentDescription().equals("Close options")) {
                            closeOptions();
                        }
                    }
                }
        );

        openCamBtn = view.findViewById(R.id.openCam);
        openCamBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getImage.launch(new Intent(getActivity(), CamActivtity.class));
                    }
                }
        );

        openGalleryBtn = view.findViewById(R.id.openMenu);
        openGalleryBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getImage.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI));
                    }
                }
        );

        shade = view.findViewById(R.id.shade);
        shade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeOptions();
            }
        });

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            Toast.makeText(getActivity().getApplicationContext(), data.toUri(Intent.URI_ALLOW_UNSAFE), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        return view;
    }

    private class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomHolder> {
        private List<Chatroom> chatrooms = null;
        public ChatRoomAdapter(List<Chatroom> chatrooms) {
            this.chatrooms = chatrooms;
        }

        @NonNull
        @Override
        public ChatRoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.row_chatroom, parent, false);
            return new ChatRoomHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatRoomHolder holder, int position) {

            Chatroom chatroom = chatrooms.get(position);
            holder.chatName.setText(chatroom.name);

            Picasso.get()
                    .load(Storage.chatroomIconStorage + chatroom.iconKey)
                    .into(holder.chatIcon);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle roomName = new Bundle();
                    roomName.putString("roomName", chatroom.name);
                    ((MainActivity) getActivity()).chatFrag.setArguments(roomName);
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.chatFrag, ((MainActivity) getActivity()).chatFrag)
                            .commit();
                }
            });
        }

        @Override
        public int getItemCount() {
            return chatrooms.size();
        }

        class ChatRoomHolder extends RecyclerView.ViewHolder{
            private TextView chatName;
            private ImageView chatIcon;
            public ChatRoomHolder(View view) {
                super(view);
                this.chatName = view.findViewById(R.id.chatName);
                this.chatIcon = view.findViewById(R.id.chatIcon);
            }
        }
    }

    public void openOptions() {
        openCamBtn.setVisibility(View.VISIBLE);
        openGalleryBtn.setVisibility(View.VISIBLE);
        shade.setVisibility(View.VISIBLE);
        addPost.setContentDescription("Close options");

        shade.startAnimation(fadeInBg);
        addPost.startAnimation(rotateClockWiseFabAnim);
        openGalleryBtn.startAnimation(fromBottomFabAnim);
        openCamBtn.startAnimation(fromBottomFabAnim);

        shade.setClickable(true);

        addPost.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        openCamBtn.setVisibility(View.GONE);
        openGalleryBtn.setVisibility(View.GONE);
        shade.setVisibility(View.GONE);
        addPost.setContentDescription("Open options");

        shade.startAnimation(fadeOutBg);
        addPost.startAnimation(rotateAntiClockWiseFabAnim);
        openGalleryBtn.startAnimation(toBottomFabAnim);
        openCamBtn.startAnimation(toBottomFabAnim);

        shade.setClickable(false);

        addPost.setImageResource(R.drawable.icon_camera);
    }
}
