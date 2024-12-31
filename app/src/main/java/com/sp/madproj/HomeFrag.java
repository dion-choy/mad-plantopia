package com.sp.madproj;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

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

        gpsTracker = ((MainActivity) getActivity()).gpsTracker;
        String weatherUrl = "";
        if (((MainActivity) getActivity()).gpsTracker != null && gpsTracker.canGetLocation) {
            weatherUrl = "https://api.openweathermap.org/data/2.5/weather?units=metric" +
                    "&lat=" + ((MainActivity) getActivity()).getLatitude() +
                    "&lon=" + ((MainActivity) getActivity()).getLongitude() +
                    "&appid=" + BuildConfig.WEATHER_KEY;
        }

        RequestQueue queue = Volley.newRequestQueue(getActivity());

        TextView homeFrag = view.findViewById(R.id.homeFrag);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, weatherUrl, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    homeFrag.setText(response.toString());
                }
            }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getActivity().getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });

        queue.add(jsonObjectRequest);

        return view;
    }
}