package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class IdentifyFrag extends Fragment {
    private Animation fromBottomFabAnim;
    private Animation toBottomFabAnim;
    private Animation rotateClockWiseFabAnim;
    private Animation rotateAntiClockWiseFabAnim;
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private ActivityResultLauncher<Intent> getImage;

    private FloatingActionButton idPlant;
    private FloatingActionButton openCamBtn;
    private FloatingActionButton openGalleryBtn;
    private TextView shade;

    public IdentifyFrag() {
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
        View view = inflater.inflate(R.layout.fragment_identify, container, false);

        fromBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.from_bottom_fab);
        toBottomFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.to_bottom_fab);
        rotateClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_clock_wise);
        rotateAntiClockWiseFabAnim = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_anti_clock_wise);
        fadeOutBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(getContext(), R.anim.fadein_bg);

        idPlant = view.findViewById(R.id.idPlant);
        idPlant.setOnClickListener(camGallOption);

        openCamBtn = view.findViewById(R.id.openCam);
        openCamBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getImage.launch(new Intent(getActivity(), CamActivtity.class));
                    }
                }
        );

        openGalleryBtn = view.findViewById(R.id.openGallery);
        openGalleryBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        getImage.launch(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI));
                    }
                }
        );

        shade = view.findViewById(R.id.shade);

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
        idPlant.setContentDescription("Close options");

        shade.startAnimation(fadeInBg);
        idPlant.startAnimation(rotateClockWiseFabAnim);
        openGalleryBtn.startAnimation(fromBottomFabAnim);
        openCamBtn.startAnimation(fromBottomFabAnim);

        idPlant.setImageResource(R.drawable.cancel);
    }

    public void closeOptions() {
        openCamBtn.setVisibility(View.GONE);
        openGalleryBtn.setVisibility(View.GONE);
        shade.setVisibility(View.GONE);
        idPlant.setContentDescription("Open options");

        shade.startAnimation(fadeOutBg);
        idPlant.startAnimation(rotateAntiClockWiseFabAnim);
        openGalleryBtn.startAnimation(toBottomFabAnim);
        openCamBtn.startAnimation(toBottomFabAnim);

        idPlant.setImageResource(R.drawable.identify_icon);
    }

    View.OnClickListener camGallOption = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (idPlant.getContentDescription().equals("Open options")) {
                openOptions();
            } else if (idPlant.getContentDescription().equals("Close options")) {
                closeOptions();
            }
        }
    };
}