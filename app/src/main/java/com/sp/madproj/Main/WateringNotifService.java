package com.sp.madproj.Main;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.sp.madproj.Plant.PlantHelper;
import com.sp.madproj.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class WateringNotifService extends Service {
    public WateringNotifService() {
    }

    private static final String TAG = WateringNotifService.class.getSimpleName();
    private static final int INTERVAL = 10000;

    private static Handler handler;
    private final Runnable callback = new Runnable() {
        @SuppressLint("MissingPermission")
        @Override
        public void run() {
            Log.d(TAG, "Service ran " + ++num + " times");

            plants.moveToFirst();
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

            handler.postDelayed(this, INTERVAL);
        }
    };
    private int num = 0;

    private final IBinder serviceBinder = new RunServiceBinder();
    public class RunServiceBinder extends Binder {
        public WateringNotifService getService() {
            return WateringNotifService.this;
        }
    }

    private PlantHelper plantHelper;
    private Cursor plants;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "Creating service");
        plantHelper = new PlantHelper(this);
        plants = plantHelper.getAll();
        handler = new Handler();
        handler.postDelayed(callback, INTERVAL);
        Log.v(TAG, "Run");
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
    @SuppressLint("MissingPermission")
    private Notification createNotification(String content) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "MyChannel", NotificationManager.IMPORTANCE_LOW);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Timer Active")
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