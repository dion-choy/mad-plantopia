package com.sp.madproj.Feed;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.sp.madproj.R;

public class LandingPageFrag extends Fragment {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public LandingPageFrag() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_landing_page, container, false);

        view.findViewById(R.id.logIn).setOnClickListener(
                view1 -> {
                    Intent intent = new Intent(getActivity(), LogInActivity.class);
                    startActivity(intent);
                }
        );

        view.findViewById(R.id.signUp).setOnClickListener(
                view1 -> {
                    Intent intent = new Intent(getActivity(), SignUpActivity.class);
                    startActivity(intent);
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null){
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.viewFrag, new FeedFrag())
                    .setReorderingAllowed(true)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
