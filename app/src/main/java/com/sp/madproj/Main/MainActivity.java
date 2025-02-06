package com.sp.madproj.Main;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.sp.madproj.BadgeFrag;
import com.sp.madproj.Chatroom.ChatFrag;
import com.sp.madproj.Feed.FeedFrag;
import com.sp.madproj.Feed.LandingPageFrag;
import com.sp.madproj.HomeFrag;
import com.sp.madproj.Identify.IdentifyFrag;
import com.sp.madproj.Plant.PlantFrag;
import com.sp.madproj.R;
import com.squareup.picasso.OkHttp3Downloader;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public BottomNavigationView navBar;
    private final FragmentManager fragManager = getSupportFragmentManager();

    private HomeFrag homeFrag;
    private PlantFrag plantFrag;
    private IdentifyFrag identifyFrag;
    private FeedFrag feedFrag;
    private BadgeFrag badgeFrag;
    private LandingPageFrag landingPageFrag;
    public ChatFrag chatFrag;

    public GPSTracker gpsTracker;

    private SharedPreferences sharedPref;

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private double latitude = 0.0d;
    private double longitude = 0.0d;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public PlantFrag getPlantFrag() {
        return this.plantFrag;
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
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        int permissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS);
        if (permissionState == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
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

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
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

            latitude = sharedPref.getFloat("lat", 0);
            longitude = sharedPref.getFloat("long", 0);
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

    private WateringNotifService notifService;
    private boolean serviceBound;

    private final static int MSG_UPDATE_TIME = 0;

    private final Handler serviceHandler = new UIUpdateHandler(this);

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this, WateringNotifService.class);
        stopService(i);
        startService(i);
        bindService(i, mConnection, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (serviceBound) {
            unbindService(mConnection);
            serviceBound = false;
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            WateringNotifService.RunServiceBinder binder = (WateringNotifService.RunServiceBinder) service;
            notifService = binder.getService();
            serviceBound = true;
            notifService.startBackground();
            notifService.setSyncListener(() -> {
                serviceHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    static class UIUpdateHandler extends Handler {

        private final static int UPDATE_RATE_MS = 1000;
        private final WeakReference<MainActivity> activity;

        public UIUpdateHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message message) {
            if (MSG_UPDATE_TIME == message.what) {
                activity.get().getPlantFrag().loadPlants();
//                sendEmptyMessageDelayed(MSG_UPDATE_TIME, UPDATE_RATE_MS);
            }
        }
    }
}