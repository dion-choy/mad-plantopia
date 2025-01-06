package com.sp.madproj;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFrag extends Fragment {
    private TextView temp;
    private TextView humid;
    private ImageView weatherImg;

    private SharedPreferences sharedPref;

    private SwipeRefreshLayout refreshHome;

    private RecyclerView notifs;
    private List<String> model = new ArrayList<>();
    private NotifsAdapter notifsAdapter;


    public HomeFrag() {
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
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        temp = view.findViewById(R.id.temp);
        humid = view.findViewById(R.id.humidity);

        weatherImg = view.findViewById(R.id.weatherImage);

        notifsAdapter = new NotifsAdapter(model);

        notifs = view.findViewById(R.id.notifsList);
        notifs.setHasFixedSize(true);
        notifs.setLayoutManager(new LinearLayoutManager(getContext()));
        notifs.setItemAnimator(new DefaultItemAnimator());

        loadNotifs();

        // Set refresh
        refreshHome =  view.findViewById(R.id.refreshHome);
        refreshHome.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ((MainActivity) getActivity()).updateLocation();
                getWeather();
                loadNotifs();
                model.add("1bruh");
                notifs.setAdapter(notifsAdapter);
                refreshHome.setRefreshing(false);
            }
        });

        // Load past data
        sharedPref = getActivity().getApplicationContext().getSharedPreferences("mySettings", MODE_PRIVATE);
        fillScreenText(
                sharedPref.getInt("temperature", 0),
                sharedPref.getInt("humidity", 0),
                sharedPref.getString("weatherIcon", "01d")
        );

        GPSTracker gpsTracker = ((MainActivity) getActivity()).gpsTracker;
        if (gpsTracker != null && gpsTracker.canGetLocation) {
            getWeather();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        getActivity().registerReceiver(gpsSwitchStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        notifs.setAdapter(notifsAdapter);
    }

    private class NotifsAdapter extends RecyclerView.Adapter<NotifsAdapter.NotifsHolder>{
        private List<String> notifs;

        NotifsAdapter(List<String> notifs) {
            this.notifs = notifs;
        }

        @Override
        public NotifsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.notifs_row, parent, false);
            return new NotifsHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotifsHolder holder, final int position) {
            final String notif = notifs.get(position);
            holder.notifText.setText(notif.substring(1));
            switch (notif.substring(0,1)) {
                case "1":
                    holder.notifType.setImageResource(R.drawable.notif_info);
                    break;
                case "2":
                    holder.notifType.setImageResource(R.drawable.notif_celebrate);
                    break;
                case "3":
                    holder.notifType.setImageResource(R.drawable.notif_important);
                    break;
                default:
                    Toast.makeText(getActivity().getApplicationContext(), notif.substring(0,1), Toast.LENGTH_SHORT).show();
                    holder.notifType.setImageResource(R.drawable.notif_info);
            }
        }

        @Override
        public int getItemCount() {
            return notifs.size();
        }

        class NotifsHolder extends RecyclerView.ViewHolder {
            private TextView notifText;
            private ImageView notifType;

            public NotifsHolder(View itemView) {
                super(itemView);
                notifType = itemView.findViewById(R.id.notifIcon);
                notifText = itemView.findViewById(R.id.notifText);
            }
        }
    }

    private final BroadcastReceiver gpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if ((isGpsEnabled || isNetworkEnabled)
                    && activeNetwork != null && (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI
                    || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
            ) {
                Log.i(this.getClass().getName(), "gpsSwitchStateReceiver.onReceive() location is enabled : isGpsEnabled = " + isGpsEnabled + " isNetworkEnabled = " + isNetworkEnabled);
                getWeather();
            } else {
                Log.w(this.getClass().getName(), "gpsSwitchStateReceiver.onReceive() location disabled ");
            }
        }
    };

    private void fillScreenText(int tempVal, int humidVal, String imageIcon) {
        temp.setText(String.format(Locale.ENGLISH, "%d", tempVal));
        humid.setText(String.format(Locale.ENGLISH, "%d%%", humidVal));

        switch (imageIcon) {
            case "01d":
                weatherImg.setImageResource(R.drawable.weather_clear_day);
                break;
            case "01n":
                weatherImg.setImageResource(R.drawable.weather_clear_night);
                break;
            case "02d":
                weatherImg.setImageResource(R.drawable.weather_partly_cloudy);
                break;
            case "02n":
                weatherImg.setImageResource(R.drawable.weather_partly_cloudy_night);
                break;
            case "03d": case "03n":
                weatherImg.setImageResource(R.drawable.weather_cloudy);
                break;
            case "04d": case "04n":
                weatherImg.setImageResource(R.drawable.weather_very_cloudy);
                break;
            case "09d": case "09n":
            case "10d": case "10n":
                weatherImg.setImageResource(R.drawable.weather_rain);
                break;
            case "11d": case "11n":
                weatherImg.setImageResource(R.drawable.weather_lightning);
                break;
            case "13d": case "13n":
                weatherImg.setImageResource(R.drawable.weather_snow);
                break;
            case "50d": case "50n":
                weatherImg.setImageResource(R.drawable.weather_mist);
                break;
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("temperature", tempVal);
        editor.putInt("humidity", humidVal);
        editor.putString("weatherIcon", imageIcon);
        editor.apply();
    }

    private void loadNotifs() {
        model.clear();
        model.add("1Test1");
        model.add("2Test2");
        model.add("3Test3");
    }

    private void getWeather() {
        String weatherUrl = "https://api.openweathermap.org/data/2.5/weather?units=metric" +
                "&lat=" + ((MainActivity) getActivity()).getLatitude() +
                "&lon=" + ((MainActivity) getActivity()).getLongitude() +
                "&appid=" + BuildConfig.WEATHER_KEY;

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, weatherUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    fillScreenText(
                            Math.round(response.getJSONObject("main").getLong("temp")),
                            Math.round(response.getJSONObject("main").getLong("humidity")),
                            response.getJSONArray("weather").getJSONObject(0).getString("icon")
                    );

                    Log.d("Weather Request", "Success: " + response.toString());
                } catch (JSONException e) {
                    Log.d("Weather Error", "Malformed Response: " + e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Weather Error", "Response Error: " + error.toString());
            }
        });

        queue.add(jsonObjectRequest);
    }
}