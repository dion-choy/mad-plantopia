package com.sp.madproj.Main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.NoConnectionError;
import com.sp.madproj.Plant.PlantHelper;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class WateringNotifService extends Service {
    public WateringNotifService() {
    }

    private static final String TAG = WateringNotifService.class.getSimpleName();
    private static final int INTERVAL = 10000;

    private static final Handler handler = new Handler();
    private final Runnable callback = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            Log.d(TAG, "Service ran " + ++num + " times");

            // Test
            NotificationManagerCompat.from(WateringNotifService.this)
                    .notify(10000, createNotification(
                            "Run " + num + "times"
                    ));

            String greenhouseId = sharedPref.getString("greenhouseId", null);
            if (greenhouseId != null && !sharedPref.getBoolean("clientChanged", false)) {
                Database.queryAstra(WateringNotifService.this,
                        "SELECT * FROM plantopia.greenhouses WHERE id=" + greenhouseId + ";",
                        (response) -> {
                            Log.d("update", response);
                            try {
                                JSONArray rows = new JSONObject(response).getJSONArray("data");
                                if (rows.length() == 0) {
                                    return;
                                }

                                plantHelper.deleteAll();
                                for (int i = 0; i < rows.length(); i++) {
                                    JSONObject jsonObject = rows.getJSONObject(i);
                                    if (plantHelper.getPlantById(String.valueOf(jsonObject.getInt("plant_id"))).getCount() > 0) {
                                        plantHelper.update(String.valueOf(jsonObject.getInt("plant_id")), jsonObject.getInt("position"),
                                                jsonObject.getString("detail").replace("htmlSpecial", "\\/").replace("ApOsTrOpHe", "'"), jsonObject.getString("icon"),
                                                jsonObject.getString("name"), jsonObject.getString("last_watered"),
                                                WateringNotifService.this, true
                                        );
                                    } else {
                                        plantHelper.insert(jsonObject.getInt("position"),
                                                jsonObject.getString("detail").replace("htmlSpecial", "\\/").replace("ApOsTrOpHe", "'"), jsonObject.getString("icon"),
                                                jsonObject.getString("name"), jsonObject.getString("last_watered"),
                                                WateringNotifService.this, true
                                        );

                                    }
                                }

                                Log.d("update", "Updated");
                            } catch (JSONException e) {
                                Log.d("update error", e.toString());
                                throw new RuntimeException(e);
                            }
                        },
                        (error) -> {
//                            Log.e("USERS ERROR", error.toString());
                            Log.e("users error", "SELECT * FROM plantopia.greenhouses WHERE id=" + greenhouseId + ";");
                        }
                );
            } else if (greenhouseId != null) {
                push(greenhouseId);
            } else {
                Log.d("update", "sync not turned on");
            }

            loadNotifications();

            handler.postDelayed(this, INTERVAL);
        }
    };
    private int num = 0;

    private void loadNotifications() {
        plants.moveToFirst();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        for (int i=0; i<plants.getCount(); i++) {
            plants.moveToPosition(i);
            LocalDate lastWatered = LocalDate.parse(plantHelper.getTimestamp(plants));
            int timeDelta = (int) ChronoUnit.DAYS.between(lastWatered, LocalDate.now());

            int min = 0;
            try {
                JSONObject jsonObject = new JSONObject(plantHelper.getDetail(plants));
                if (!jsonObject.isNull("watering")) {
                    min = jsonObject.getJSONObject("watering").getInt("min");
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }

            if (min == 1) {
                if (timeDelta >= 7) {
                    NotificationManagerCompat.from(WateringNotifService.this)
                            .notify(plantHelper.getPosition(plants), createNotification(
                                    plantHelper.getName(plants) + " requires watering!"
                            ));
                }
            } else if (min == 2) {
                if (timeDelta >= 3) {
                    NotificationManagerCompat.from(WateringNotifService.this)
                            .notify(plantHelper.getPosition(plants), createNotification(
                                    plantHelper.getName(plants) + " requires watering!"
                            ));
                }
            } else if (min == 3) {
                if (timeDelta >= 1) {
                    NotificationManagerCompat.from(WateringNotifService.this)
                            .notify(plantHelper.getPosition(plants), createNotification(
                                    plantHelper.getName(plants) + " requires watering!"
                            ));
                }
            }
        }
    }

    private void push(String greenhouseId) {
        plants.moveToFirst();
        for (int i = 0; i < plants.getCount(); i++) {
            plants.moveToPosition(i);

            Database.queryAstra(this,
                    String.format(Locale.ENGLISH,
                            "UPDATE plantopia.greenhouses " +
                                    "SET position=%d, detail='%s', icon='%s', name='%s', last_watered='%s' " +
                                    "WHERE id=%s AND plant_id=%s;",
                            plantHelper.getPosition(plants), plantHelper.getDetail(plants).replace("\\/", "htmlSpecial").replace("'", "ApOsTrOpHe"),
                            plantHelper.getIcon(plants), plantHelper.getName(plants),
                            plantHelper.getTimestamp(plants), greenhouseId, plantHelper.getID(plants)),
                    response -> {},
                    error -> {
                        sharedPref.edit()
                                .putBoolean("clientChanged", true)
                                .commit();
                        Log.e("update error", error.toString());
                    }
            );
        }

        sharedPref.edit()
                .putBoolean("clientChanged", false)
                .commit();
    }

    private final IBinder serviceBinder = new RunServiceBinder();
    public class RunServiceBinder extends Binder {
        public WateringNotifService getService() {
            return WateringNotifService.this;
        }
    }

    private SharedPreferences sharedPref;
    private PlantHelper plantHelper;
    private Cursor plants;
    @Override
    public void onCreate() {
        super.onCreate();

        sharedPref = getSharedPreferences("greenhouse", MODE_PRIVATE);

        Log.v(TAG, "Creating service");
        plantHelper = new PlantHelper(this);
        plants = plantHelper.getAll();

        handler.removeCallbacksAndMessages(null);
        callback.run();
        handler.postDelayed(callback, INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "Binding service");
        return serviceBinder;
    }


    @Override
    public void onDestroy() {
        Log.v(TAG, "Destroying service");
        handler.removeCallbacks(callback);
        plantHelper.close();
        super.onDestroy();

        onCreate();
    }

    public void startBackground() {
        stopForeground(true);
    }

    private static final String CHANNEL_ID = "MyChannel";
    private Notification createNotification(String content) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "MyChannel", NotificationManager.IMPORTANCE_LOW);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Thirsty Plant!")
                .setContentText(content)
                .setColor(Color.parseColor("#FFAFE2C2"))
//                .setLargeIcon(CanvasView.getBitmapFromVectorDrawable(this, R.drawable.plant_vine))
                .setSmallIcon(R.drawable.plant_vine);

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(resultPendingIntent);

        return builder.build();
    }
}