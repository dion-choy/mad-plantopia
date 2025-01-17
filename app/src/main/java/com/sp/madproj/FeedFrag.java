package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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

    public final static String pfpStorage = "https://upevuilypqhjisraltzb.supabase.co/storage/v1/object/images/pfp/";
    private final String databaseUrl = " https://plantopia-backend-ecce9-default-rtdb.asia-southeast1.firebasedatabase.app";
    private final FirebaseDatabase database = FirebaseDatabase.getInstance(databaseUrl);

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private String username = "";
    private String email = "";


    public FeedFrag() {
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
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

        username = auth.getCurrentUser().getDisplayName();
        email = auth.getCurrentUser().getEmail();

        Log.d("USER NAME: ", username);
        Log.d("USER EMAIL: ", email);

//        DatabaseReference realtimeDB = database.getReference("message");
//        Log.d("Realtime DB", realtimeDB.toString());
//        String pushKey = realtimeDB.push().getKey();
//        realtimeDB.child(pushKey).setValue(message);

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
