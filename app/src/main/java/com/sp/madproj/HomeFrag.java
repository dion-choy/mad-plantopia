package com.sp.madproj;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HomeFrag extends Fragment {
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

        String url = "https://api-open.data.gov.sg/v2/real-time/api/air-temperature";

        OkHttpHandler okHttpHandler= new OkHttpHandler();
        okHttpHandler.execute(url);

        return view;
    }

    public class OkHttpHandler extends AsyncTask<String, Void, String> {
        OkHttpClient client = new OkHttpClient();

        @Override
        protected String doInBackground(String ...params) {

            Request request = new Request.Builder()
                    .url(params[0])
                    .get()
                    .build();

            try {
                Response response = client.newCall(request).execute();
                return response.body().string();
            }catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(getActivity().getApplicationContext(), s, Toast.LENGTH_SHORT).show();
        }
    }
}