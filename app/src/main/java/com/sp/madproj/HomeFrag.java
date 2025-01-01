package com.sp.madproj;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.Locale;

public class HomeFrag extends Fragment {
    private TextView test;
    private TextView temp;
    private TextView humid;

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
        test = view.findViewById(R.id.test);
        humid = view.findViewById(R.id.humidity);

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
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        getActivity().registerReceiver(gpsSwitchStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private final BroadcastReceiver gpsSwitchStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
                // Make an action or refresh an already managed state.

                LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
                boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (isGpsEnabled || isNetworkEnabled) {
                    Log.i(this.getClass().getName(), "gpsSwitchStateReceiver.onReceive() location is enabled : isGpsEnabled = " + isGpsEnabled + " isNetworkEnabled = " + isNetworkEnabled);
                    getWeather();
                } else {
                    Log.w(this.getClass().getName(), "gpsSwitchStateReceiver.onReceive() location disabled ");
                }
            }
        }
    };

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
                    test.setText(response.toString());
                    temp.setText(String.format(Locale.ENGLISH, "%.1f",
                            response.getJSONObject("main").getDouble("temp")
                    ));
                    humid.setText(String.format(Locale.ENGLISH, "%.0f%%",
                            response.getJSONObject("main").getDouble("humidity")
                    ));
                    Toast.makeText(getActivity().getApplicationContext(), "new", Toast.LENGTH_SHORT).show();
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