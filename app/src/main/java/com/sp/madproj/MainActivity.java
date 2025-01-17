package com.sp.madproj;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navBar;
    private FragmentManager fragManager = getSupportFragmentManager();

    private HomeFrag homeFrag;
    private PlantFrag plantFrag;
    private IdentifyFrag identifyFrag;
    private FeedFrag feedFrag;
    private BadgeFrag badgeFrag;
    private LandingPageFrag landingPageFrag;

    public GPSTracker gpsTracker;

    private SharedPreferences sharedPref;

    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private double latitude = 0.0d;
    private double longitude = 0.0d;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    private static Context context;
    public static Context getContext() {
        return context;
    }

    @Override
    public void onBackPressed() {
        if (fragManager.findFragmentById(R.id.viewFrag) == homeFrag) {
            return;
        }

        if (fragManager.findFragmentById(R.id.viewFrag) == identifyFrag && identifyFrag.getView().findViewById(R.id.idPlant).getContentDescription().equals("Close options")) {
            identifyFrag.closeOptions();
            return;
        } else if (fragManager.findFragmentById(R.id.viewFrag) == feedFrag && feedFrag.getView().findViewById(R.id.openMenu).getContentDescription().equals("Close options")) {
            feedFrag.closeOptions();
            return;
        } else if (fragManager.findFragmentById(R.id.viewFrag) == plantFrag && plantFrag.getView().findViewById(R.id.openMenu).getContentDescription().equals("Close options")) {
            plantFrag.closeOptions();
            return;
        }

        super.onBackPressed();

        if (fragManager.findFragmentById(R.id.viewFrag) == homeFrag) {
            navBar.setSelectedItemId(R.id.homeTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == identifyFrag) {
            navBar.setSelectedItemId(R.id.identifyTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == plantFrag) {
            navBar.setSelectedItemId(R.id.plantsTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == feedFrag || fragManager.findFragmentById(R.id.viewFrag) == landingPageFrag) {
            navBar.setSelectedItemId(R.id.feedTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == badgeFrag) {
            navBar.setSelectedItemId(R.id.badgesTab);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getContext();

        currentUser = auth.getCurrentUser();

        gpsTracker = new GPSTracker(this);

        navBar = findViewById(R.id.bottomNav);
        navBar.setOnItemSelectedListener(switchPage);

        homeFrag = new HomeFrag();
        plantFrag = new PlantFrag();
        identifyFrag = new IdentifyFrag();
        feedFrag = new FeedFrag();
        badgeFrag = new BadgeFrag();
        landingPageFrag = new LandingPageFrag();

        fragManager.beginTransaction()
                .replace(R.id.viewFrag, homeFrag)
                .setReorderingAllowed(false)
                .addToBackStack(null)
                .commit();

        sharedPref = getApplicationContext().getSharedPreferences("oldLocation", MODE_PRIVATE);
        updateLocation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (fragManager.findFragmentById(R.id.viewFrag) == feedFrag || fragManager.findFragmentById(R.id.viewFrag) == landingPageFrag) {
            currentUser = auth.getCurrentUser();
            if (currentUser != null){
                Log.i("USER", currentUser.getEmail());
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, feedFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            } else {
                Log.d("Not signed in", "onStart: Not Signed In");
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, landingPageFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    public void updateLocation() {
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putFloat("lat", (float) latitude);
            editor.putFloat("long", (float) longitude);
            editor.apply();
        } else {
            Toast.makeText(getApplicationContext(), "Please turn on location services", Toast.LENGTH_LONG).show();

            latitude = (double) sharedPref.getFloat("lat", 0);
            longitude = (double) sharedPref.getFloat("long", 0);
        }
    }

    BottomNavigationView.OnItemSelectedListener switchPage = new NavigationBarView.OnItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();

            if (id == R.id.homeTab && fragManager.findFragmentById(R.id.viewFrag) != homeFrag) {
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, homeFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            } else if (id == R.id.identifyTab && fragManager.findFragmentById(R.id.viewFrag) != identifyFrag) {
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, identifyFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            } else if (id == R.id.plantsTab && fragManager.findFragmentById(R.id.viewFrag) != plantFrag) {
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, plantFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            } else if (id == R.id.badgesTab && fragManager.findFragmentById(R.id.viewFrag) != badgeFrag) {
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, badgeFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            } else if (id == R.id.feedTab && fragManager.findFragmentById(R.id.viewFrag) != feedFrag) {
//                auth.signOut();
                currentUser = auth.getCurrentUser();

                if (currentUser != null){
                    Log.i("USER", currentUser.getEmail());
                    fragManager.beginTransaction()
                            .replace(R.id.viewFrag, feedFrag)
                            .setReorderingAllowed(true)
                            .addToBackStack(null)
                            .commit();
                } else {
                    Log.d("Not signed in", "onStart: Not Signed In");
                    fragManager.beginTransaction()
                            .replace(R.id.viewFrag, landingPageFrag)
                            .setReorderingAllowed(true)
                            .addToBackStack(null)
                            .commit();
                }
            }

            return true;
        }
    };
}