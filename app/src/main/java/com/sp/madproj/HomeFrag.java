package com.sp.madproj;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HomeFrag extends Fragment {
    private GPSTracker gpsTracker;

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
        TextView homeFrag = view.findViewById(R.id.homeFrag);
        gpsTracker = ((MainActivity) getActivity()).gpsTracker;

        String weatherUrl = "";
        if (gpsTracker != null && gpsTracker.canGetLocation) {
            weatherUrl = "https://api.openweathermap.org/data/2.5/weather?units=metric" +
                    "&lat=" + ((MainActivity) getActivity()).getLatitude() +
                    "&lon=" + ((MainActivity) getActivity()).getLongitude() +
                    "&appid=" + BuildConfig.WEATHER_KEY;
        }

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, weatherUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        homeFrag.setText(response.getJSONObject("main").toString());
                    } catch (JSONException e) {
                        Log.d("Weather Error", "Malformed Response: " + e.toString());
                    }
                    Toast.makeText(getActivity().getApplicationContext(), "new", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Weather Error", "Response Error: " + error.toString());
            }
        });

        queue.add(jsonObjectRequest);

        return view;
    }
}