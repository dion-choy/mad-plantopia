package com.sp.madproj;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import com.google.firebase.database.DatabaseReference;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.Objects;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private BottomNavigationView navBar;
    private FragmentManager fragManager = getSupportFragmentManager();

    private HomeFrag homeFrag;
    private PlantFrag plantFrag;
    private IdentifyFrag identifyFrag;
    private FeedFrag feedFrag;
    private BadgeFrag badgeFrag;
    private LandingPageFrag landingPageFrag;
    public ChatFrag chatFrag;

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
        if (fragManager.findFragmentById(R.id.viewFrag) == identifyFrag && identifyFrag.getView().findViewById(R.id.idPlant).getContentDescription().equals("Close options")) {
            identifyFrag.closeOptions();
            return;
        } else if (fragManager.findFragmentById(R.id.viewFrag) == feedFrag && feedFrag.getView().findViewById(R.id.addRoom).getContentDescription().equals("Close options")) {
            feedFrag.closeOptions();
            return;
        } else if (fragManager.findFragmentById(R.id.viewFrag) == plantFrag && plantFrag.getView().findViewById(R.id.openMenu).getContentDescription().equals("Close options")) {
            plantFrag.closeOptions();
            return;
        } else if (Objects.equals(fragManager.findFragmentById(R.id.chatFrag), chatFrag)) {
            fragManager.beginTransaction()
                    .remove(chatFrag)
                    .commit();
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

        navBar = findViewById(R.id.bottomNav);
        navBar.setOnItemSelectedListener(switchPage);

        homeFrag = new HomeFrag();
        plantFrag = new PlantFrag();
        identifyFrag = new IdentifyFrag();
        feedFrag = new FeedFrag();
        badgeFrag = new BadgeFrag();
        landingPageFrag = new LandingPageFrag();
        chatFrag = new ChatFrag();

        fragManager.beginTransaction()
                .replace(R.id.viewFrag, homeFrag)
                .setReorderingAllowed(false)
                .disallowAddToBackStack()
                .commit();

        Picasso picasso = new Picasso
                .Builder(this)
                .downloader(new OkHttp3Downloader(okHttpClient))
                .build();

        try {
            Picasso.setSingletonInstance(picasso);
        } catch (IllegalStateException e) {
            Log.e("Picasso", e.toString());
            Log.e("Picasso", e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        DatabaseReference.goOnline();
        currentUser = auth.getCurrentUser();

        gpsTracker = new GPSTracker(this);

        sharedPref = getApplicationContext().getSharedPreferences("oldLocation", MODE_PRIVATE);
        updateLocation();

        Log.d("RESUME", "FEED RESUMED");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseReference.goOffline();
    }

    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request();
                    Response response = chain.proceed(request);
                    int tryCount = 0;
                    while (!response.isSuccessful() && tryCount < 5) {
                        tryCount++;
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        response = chain.proceed(request);
                        Log.i("Picasso Download Image", "Attempt: " + tryCount + ", " + 2*tryCount + "seconds");
                    }
                    if (response.isSuccessful()) {
                        Log.i("Picasso Download Image", "Success: " + request.url());
                    } else {
                        Log.e("Picasso Download Image", "Failed: " + request.url());
                    }
                    return response;
                }
            })
            .build();

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