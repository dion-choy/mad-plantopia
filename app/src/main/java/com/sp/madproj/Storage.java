package com.sp.madproj;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import uk.me.hardill.volley.multipart.MultipartRequest;

public static class Storage {
    public final String baseUrl = "https://upevuilypqhjisraltzb.supabase.co/storage/v1/object/images/";
    public final String pfpStorage = baseUrl + "pfp/";
    public final String identifStorage = baseUrl + "identification/";
    public final String chatroomIconStorage = baseUrl + "chatroom/icons/";
    public final String chatroomImageStorage = baseUrl + "chatroom/images/";

    public void deleteObjSup(Context context, String storagePath) {

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.DELETE, storagePath,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("Storage API", "onResponse: " + response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Storage API Error", "Response Error: " + error.toString());
                Log.e("Storage API Error", "Response Error: " + error.getMessage());

                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> header = new HashMap<String, String>();
                header.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);
                return header;
            }
        };

        queue.add(request);
    }

    public String uploadImgSupa(Context context, Uri uri, String storagePath) {
        Matrix matrix = new Matrix();
        switch (getBitmapOriention(getRealPathFromURI(context, uri))) {
            case ExifInterface.ORIENTATION_NORMAL:
                matrix.postRotate(0);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
        }

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bitmapStream);
        byte[] inputData = bitmapStream.toByteArray();
        bitmap.recycle();

        Log.d("Storage API", "Building Request...");

        RequestQueue queue = Volley.newRequestQueue(context);

        Map<String, String> header = new HashMap<String, String>();
        header.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);

        String rng = String.valueOf((int) Math.floor(Math.random()*Integer.MAX_VALUE));
        String apiUrl = storagePath +  rng + ".jpg";

        MultipartRequest request = new MultipartRequest(Request.Method.POST,
                apiUrl, header,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        if (response != null) {
                            String responseData = new String(response.data, StandardCharsets.UTF_8);
                            Log.d("Storage API", "onResponse: " + responseData);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Storage API Error", "Response Error: " + error.toString());
                Log.e("Storage API Error", "Response Error: " + error.getMessage());

                if (error.getClass() == NoConnectionError.class) {
                    Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                }
            }
        });

        request.addPart(new MultipartRequest.FilePart("", "image/jpg", null, inputData));

        Log.d("Storage API", "Sent request: " + apiUrl);
        queue.add(request);

        return rng + ".jpg";
    }

    public int getBitmapOriention(String path){
        ExifInterface exif = null;
        int orientation = 0;
        try {
            exif = new ExifInterface(path);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
            //Log.e("getBitmapOriention", "getBitmapOriention: "+orientation );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orientation;
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
