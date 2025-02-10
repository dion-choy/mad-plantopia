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
import com.google.firebase.auth.FirebaseAuth;
import com.sp.madproj.Plant.PlantHelper;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Database;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class WateringNotifService extends Service {
    public WateringNotifService() {
    }

    private static final String TAG = WateringNotifService.class.getSimpleName();
    private static final int INTERVAL = 10000;

    private static final Handler handler = new Handler();
    private final TimerTask callback = new TimerTask() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            Log.d(TAG, "Service ran " + ++num + " times");

            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                checkUsernames(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            }

            // Test
            NotificationManagerCompat.from(WateringNotifService.this)
                    .notify(10000, createNotification(
                            "Run " + num + "times"
                    ));
            if (!plantHelper.getReadableDatabase().isOpen()) {
                plants = plantHelper.getAll();
                return;
            }

            String greenhouseId = sharedPref.getString("greenhouseId", null);
            if (greenhouseId != null) {
                Database.queryAstra(WateringNotifService.this,
                        "SELECT * FROM plantopia.greenhouses WHERE id=" + greenhouseId + ";",
                        (response) -> {
                            plants = plantHelper.getAll();
                            Log.d("update", response);

                            try {
                                JSONArray rows = new JSONObject(response).getJSONArray("data");

                                Log.d("update sync needed", String.valueOf(sharedPref.getBoolean("clientChanged", false)));
                                if (sharedPref.getBoolean("clientChanged", false)) {
                                    push(greenhouseId, rows);
                                    return;
                                }

                                if (rows.length() == 0) {
                                    return;
                                }

                                StringBuilder existingPos = new StringBuilder();
                                for (int i = 0; i < rows.length(); i++) {
                                    JSONObject jsonObject = rows.getJSONObject(i);
                                    if (jsonObject.getInt("position") == -100) {
                                        continue;
                                    }

                                    if (plantHelper.getFilledPos(jsonObject.getInt("position")).getCount() > 0) {
                                        plantHelper.updateByPos(jsonObject.getInt("position"),
                                                jsonObject.getString("detail").replace("htmlSpecial", "\\/").replace("ApOsTrOpHe", "'"), jsonObject.getString("icon"),
                                                jsonObject.getString("name"), jsonObject.getString("last_watered"),
                                                WateringNotifService.this
                                        );
                                        Log.d("update client", "Updated");
                                    } else {
                                        plantHelper.insert(jsonObject.getInt("position"),
                                                jsonObject.getString("detail").replace("htmlSpecial", "\\/").replace("ApOsTrOpHe", "'"), jsonObject.getString("icon"),
                                                jsonObject.getString("name"), jsonObject.getString("last_watered"),
                                                WateringNotifService.this
                                        );
                                        Log.d("update client", "Inserted");
                                        if (syncListener != null) {
                                            syncListener.run();
                                        }
                                    }
                                    existingPos.append(jsonObject.getInt("position"))
                                            .append(", ");
                                }
                                existingPos.setLength(existingPos.length() - 2);
                                Log.d("TEST", existingPos.toString());
                                if (plantHelper.getWritableDatabase()
                                        .delete("plant_table", "position NOT IN (" + existingPos +
                                                ")", null) > 0) {
                                    syncListener.run();
                                }

                                Log.d("update", "Updated");
                            } catch (JSONException e) {
                                Log.d("update error", e.toString());
                                throw new RuntimeException(e);
                            }
                        },
                        (error) -> {
                            Log.e("USERS ERROR", error.toString());
                        }
                );
            } else {
                Log.d("update", "sync not turned on");
            }

            loadNotifications();
            plantHelper.close();
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

    @SuppressLint("ApplySharedPref")
    private void push(String greenhouseId, JSONArray rows) {
        plants.moveToFirst();

        ArrayList<Integer> inCloud = new ArrayList<>();
        int i;
        for (i = 0; i<rows.length(); i++) {
            try {
                inCloud.add(rows.getJSONObject(i).getInt("position"));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        inCloud.remove(Integer.valueOf(-100));
        for (i = 0; i < plants.getCount(); i++) {
            plants.moveToPosition(i);
            inCloud.remove(Integer.valueOf(plantHelper.getPosition(plants)));

            int finalI = i;
            Database.queryAstra(this,
                    String.format(Locale.ENGLISH,
                            "UPDATE plantopia.greenhouses " +
                                    "SET detail='%s', icon='%s', name='%s', last_watered='%s' " +
                                    "WHERE id=%s AND position=%s;",
                            plantHelper.getDetail(plants).replace("\\/", "htmlSpecial").replace("'", "ApOsTrOpHe"),
                            plantHelper.getIcon(plants), plantHelper.getName(plants),
                            plantHelper.getTimestamp(plants), greenhouseId, plantHelper.getPosition(plants)),
                    response -> {
                        Log.i("update sync push", response);
                        if (finalI == plants.getCount() - 1) {
                            deleteFromCloud(greenhouseId, inCloud);
                        }
                    },
                    error -> {
                        sharedPref.edit()
                                .putBoolean("clientChanged", true)
                                .commit();
                        Log.e("update error", error.toString());
                    }
            );

        }

        if (plants.getCount() == 0) {
            Log.e("update sync push", "NOthing in db");
            deleteFromCloud(greenhouseId, inCloud);
        }
    }

    @SuppressLint("ApplySharedPref")
    private void deleteFromCloud(String greenhouseId, ArrayList<Integer> inCloud) {
        if (inCloud.isEmpty()) {
            sharedPref.edit()
                    .putBoolean("clientChanged", false)
                    .commit();
        }

        for (int i = 0; i<inCloud.size(); i++) {
            Cursor plant = plantHelper.getFilledPos(inCloud.get(i));
            plant.moveToFirst();

            int finalI = i;
            Database.queryAstra(this,
                    String.format(Locale.ENGLISH,
                            "DELETE FROM plantopia.greenhouses " +
                                    "WHERE id=%s AND position=%s;",
                            greenhouseId, inCloud.get(i)),
                    response -> {
                        Log.e("update sync delete", response);
                        if (finalI == inCloud.size() - 1) {
                            sharedPref.edit()
                                    .putBoolean("clientChanged", false)
                                    .commit();
                        }
                    },
                    error -> {
                        sharedPref.edit()
                                .putBoolean("clientChanged", true)
                                .commit();
                        Log.e("update error", error.toString());
                    }
            );
        }
    }

    public void setSyncListener(Runnable listener) {
        syncListener = listener;
    }

    private Runnable syncListener = null;

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
        new Timer().schedule(callback, 0, INTERVAL);
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
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
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

    private void checkUsernames(String username) {
        Database.queryAstra(this,
                "SELECT * FROM plantopia.user_info WHERE username = '" + username + "';",
                response -> {
                    Log.d("USERS", response);
                    try {
                        JSONObject responseObj = new JSONObject(response);
                        if (responseObj.getInt("count") > 0) {
                            if (responseObj.getJSONArray("data")
                                        .getJSONObject(0)
                                        .isNull("greenhouse_id")
                            ) {
                                sharedPref.edit()
                                        .remove("greenhouseId")
                                        .apply();
                                Log.d("Greenhouse ID", "Greenhouse id removed");
                                return;
                            }

                            if (!responseObj.getJSONArray("data")
                                    .getJSONObject(0)
                                    .getString("greenhouse_id").equals(sharedPref.getString("greenhouseId", ""))
                            ) {
                                sharedPref.edit()
                                        .remove("greenhouseId")
                                        .apply();
                                Log.d("Greenhouse ID", "Greenhouse id removed");
                            }
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}