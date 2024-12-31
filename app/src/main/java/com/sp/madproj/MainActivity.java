package com.sp.madproj;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navBar;
    private FragmentManager fragManager = getSupportFragmentManager();

    private HomeFrag homeFrag;
    private PlantFrag plantFrag;
    private IdentifyFrag identifyFrag;
    private FeedFrag feedFrag;
    private BadgeFrag badgeFrag;

    private GPSTracker gpsTracker;
    private double latitude = 0.0d;
    private double longitude = 0.0d;

    @Override
    public void onBackPressed() {
        if (fragManager.findFragmentById(R.id.viewFrag) == homeFrag) {
            return;
        }

        if (fragManager.findFragmentById(R.id.viewFrag) == identifyFrag && identifyFrag.getView().findViewById(R.id.idPlant).getContentDescription().equals("Close options")) {
            identifyFrag.closeOptions();
            return;
        }

        super.onBackPressed();

        if (fragManager.findFragmentById(R.id.viewFrag) == homeFrag) {
            navBar.setSelectedItemId(R.id.homeTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == identifyFrag) {
            navBar.setSelectedItemId(R.id.identifyTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == plantFrag) {
            navBar.setSelectedItemId(R.id.plantsTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == feedFrag) {
            navBar.setSelectedItemId(R.id.feedTab);
        } else if (fragManager.findFragmentById(R.id.viewFrag) == badgeFrag) {
            navBar.setSelectedItemId(R.id.badgesTab);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        navBar = findViewById(R.id.bottomNav);
        navBar.setOnItemSelectedListener(switchPage);

        homeFrag = new HomeFrag();
        plantFrag = new PlantFrag();
        identifyFrag = new IdentifyFrag();
        feedFrag = new FeedFrag();
        badgeFrag = new BadgeFrag();

        fragManager.beginTransaction()
                .replace(R.id.viewFrag, homeFrag)
                .setReorderingAllowed(false)
                .addToBackStack(null)
                .commit();

        gpsTracker = new GPSTracker(this);
        if (gpsTracker.canGetLocation()) {
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();

            Toast.makeText(getApplicationContext(), "Your location is - \nLat: " +
                    latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Cannot get loc", Toast.LENGTH_LONG).show();
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
                fragManager.beginTransaction()
                        .replace(R.id.viewFrag, feedFrag)
                        .setReorderingAllowed(true)
                        .addToBackStack(null)
                        .commit();
            }

            return true;
        }
    };
}