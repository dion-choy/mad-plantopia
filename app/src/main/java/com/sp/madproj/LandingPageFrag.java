package com.sp.madproj;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LandingPageFrag extends Fragment {

    private FirebaseAuth auth = FirebaseAuth.getInstance();

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

        ((Button) view.findViewById(R.id.logIn)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), LogInActivity.class);
                        startActivity(intent);
                    }
                }
        );

        ((Button) view.findViewById(R.id.signUp)).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), SignUpActivity.class);
                        startActivity(intent);
                    }
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
