package com.sp.madproj;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedFrag extends Fragment {
    private Animation fromBottomFabAnim;
    private Animation toBottomFabAnim;
    private Animation rotateClockWiseFabAnim;
    private Animation rotateAntiClockWiseFabAnim;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private ActivityResultLauncher<Intent> getImage;

    private FloatingActionButton addServer;
    private ConstraintLayout addServerContainer;


    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;
    private String username = "";
    private String email = "";


    private RecyclerView chatRooms;
    private List<Chatroom> model = new ArrayList<Chatroom>();
    private ChatRoomAdapter chatRoomAdapter;

    private EditText codeInput;


    public FeedFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        model.clear();
        DatabaseReference chats = Database.get().getReference().child("rooms");
        chats.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                model.clear();
                updateMenu(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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
    }

    private void updateMenu(DataSnapshot snapshot) {
        for (DataSnapshot child: snapshot.getChildren()) {
            Log.d("REALTIME", "onChildAdded: " + child.toString());
            DatabaseReference chatMembers = child.getRef().child("members");

            chatMembers.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<HashMap<String, String>>() {};
                    HashMap<String, String> members = snapshot.getValue(t);
                    if (members != null) {
                        Log.d("REALTIME", "onChildAdded: " + members.toString());
                        if (members.containsValue(currentUser.getEmail())) {
                            addChatroomToModel(child);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

        }
    }

    private void addChatroomToModel(DataSnapshot child) {
        DatabaseReference chatInfo = child.getRef().child("info");
        Log.d("REALTIME", "onChildAdded: " + chatInfo);
        chatInfo.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                model.add((Chatroom) snapshot.getValue(Chatroom.class)
                        .setKey(child.getKey()));
                if (chatRoomAdapter != null) {
                    chatRoomAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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

        chatRooms = view.findViewById(R.id.chatRooms);
        chatRooms.setLayoutManager(new LinearLayoutManager(getContext()));
        chatRooms.setItemAnimator(new DefaultItemAnimator());
        chatRooms.setAdapter(chatRoomAdapter);

        addServer = view.findViewById(R.id.addRoom);
        addServer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (addServer.getContentDescription().equals("Open options")) {
                            openOptions();
                        } else if (addServer.getContentDescription().equals("Close options")) {
                            closeOptions();
                        }
                    }
                }
        );

        addServerContainer = view.findViewById(R.id.add_room_container);
        addServerContainer.setOnClickListener(new View.OnClickListener() {
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

        codeInput = view.findViewById(R.id.codeInput);

        view.findViewById(R.id.joinBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (codeInput.getText().length() != 6) {
                    codeInput.setError("Code must be 6 digits");
                    return;
                }

                String inputCode = codeInput.getText().toString();
                codeInput.setText("");
                getCode(inputCode);
            }
        });

        view.findViewById(R.id.createRoom).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CreateRoomActivity.class);
                getActivity().startActivity(intent);
            }
        });
        return view;
    }

    private void getCode(String code) {
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Database.astraDbQueryUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("CODE", response);
                        try {
                            JSONObject responseObj = new JSONObject(response);
                            Log.d("CODE", responseObj.toString());
                            if (responseObj.getJSONArray("data").getJSONObject(0).isNull("generated_time")) {
                                Toast.makeText(getActivity(), "Code expired or not generated", Toast.LENGTH_LONG).show();
                                return;
                            }

                            OffsetDateTime generatedTime = OffsetDateTime.parse(responseObj.getJSONArray("data").getJSONObject(0).getString("generated_time"));
                            OffsetDateTime nowTime = OffsetDateTime.now();
                            long time = ChronoUnit.HOURS.between(generatedTime, nowTime);
                            if (time > 1) {
                                Toast.makeText(getActivity(), "Code expired or not generated", Toast.LENGTH_LONG).show();
                                String id = responseObj.getJSONArray("data").getJSONObject(0).getString("id");
                                removeExpiredCode(id);
                            } else {
                                String roomKey = responseObj.getJSONArray("data").getJSONObject(0).getString("id");
                                Log.d("CODE", roomKey);
                                addUser(roomKey);
                                closeOptions();
                            }

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("USERS ERROR", error.toString());
                Log.e("USER ERROR", "SELECT * FROM user_info WHERE plantopia.username = '" + code + "';");
                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return ("SELECT * FROM plantopia.rooms WHERE code = '" + code + "';").getBytes(StandardCharsets.UTF_8);
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

    private void addUser(String roomKey) {
        DatabaseReference chatMembers = Database.get()
                .getReference()
                .child("rooms")
                .child(roomKey)
                .child("members");

        chatMembers.get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DataSnapshot> task) {
                if (!task.isSuccessful()) {
                    return;
                }

                GenericTypeIndicator<HashMap<String, String>> t = new GenericTypeIndicator<HashMap<String, String>>() {};
                HashMap<String, String> members = task.getResult().getValue(t);

                if (members.containsValue(currentUser.getEmail())) {
                    Toast.makeText(getActivity(), "Already a member", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newMemberKey = chatMembers.push().getKey();

                chatMembers.child(newMemberKey)
                        .setValue(currentUser.getEmail())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d("REALTIME DB ADD", "onComplete: Member added successfully!");
                                }
                            }
                        });
            }
        });

    }

    private void removeExpiredCode(String id) {
        RequestQueue queue = Volley.newRequestQueue(getActivity());

        StringRequest stringRequest = new StringRequest(
                Request.Method.POST,
                Database.astraDbQueryUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("CODE", "Expired code removed successfully");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("USERS ERROR", error.toString());
                Log.e("USER ERROR", "UPDATE plantopia.rooms SET code=NULL, generated_time=NULL WHERE id= '" + id + "';");
                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(getActivity(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }
        ) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                return ("UPDATE plantopia.rooms SET code=NULL, generated_time=NULL WHERE id= '" + id + "';")
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
            if (chatroom == null) {
                return;
            }

            holder.chatName.setText(chatroom.name);
            Picasso.get()
                    .load(Storage.chatroomIconStorage + chatroom.iconKey)
                    .placeholder(R.mipmap.default_pfp_foreground)
                    .into(holder.chatIcon, new Callback() {
                        @Override
                        public void onSuccess() {
                            holder.chatIcon.setImageTintList(null);
                        }

                        @Override
                        public void onError(Exception e) {}
                    });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle roomName = new Bundle();
                    roomName.putString("roomKey", chatroom.key);
                    roomName.putString("roomName", chatroom.name);
                    roomName.putString("iconKey", chatroom.iconKey);
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
        addServerContainer.setVisibility(View.VISIBLE);
        addServer.setContentDescription("Close options");

        addServerContainer.startAnimation(fadeInBg);
        addServer.startAnimation(rotateClockWiseFabAnim);

        addServerContainer.setClickable(true);

        addServer.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        addServerContainer.setVisibility(View.GONE);
        addServer.setContentDescription("Open options");

        addServerContainer.startAnimation(fadeOutBg);
        addServer.startAnimation(rotateAntiClockWiseFabAnim);

        addServerContainer.setClickable(false);
        codeInput.setText("");

        InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);

        addServer.setImageResource(R.drawable.icon_add);
    }
}
