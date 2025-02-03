package com.sp.madproj.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.sp.madproj.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import uk.me.hardill.volley.multipart.MultipartRequest;

public class Storage {
    public final static String baseUrl = "https://upevuilypqhjisraltzb.supabase.co/storage/v1/object/images/";
    public final static String pfpStorage = baseUrl + "pfp/";
    public final static String identifStorage = baseUrl + "identification/";
    public final static String chatroomIconStorage = baseUrl + "chatroom/icons/";
    public final static String chatroomImageStorage = baseUrl + "chatroom/images/";

    public static void deleteObjSupa(Context context, String storagePath) {

        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest request = new StringRequest(Request.Method.DELETE, storagePath,
                response -> Log.d("Storage API", "onResponse: " + response),
                error -> {
                    Log.e("Storage API Error", "Response Error: " + error.toString());
                    Log.e("Storage API Error", "Response Error: " + error.getMessage());

                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> header = new HashMap<>();
                header.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);
                return header;
            }
        };

        queue.add(request);
    }

    public static String uploadImgSupa(Context context, Uri uri, String storagePath) {
        Matrix matrix = new Matrix();
        switch (getBitmapOriention(context, uri)) {
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

        Bitmap bitmap;
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

        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Bearer " + BuildConfig.SUPA_KEY);

        String rng = String.valueOf((int) Math.floor(Math.random()*Integer.MAX_VALUE));
        String apiUrl = storagePath +  rng + ".jpg";

        MultipartRequest request = new MultipartRequest(Request.Method.POST,
                apiUrl, header,
                response -> {
                    if (response != null) {
                        String responseData = new String(response.data, StandardCharsets.UTF_8);
                        Log.d("Storage API", "onResponse: " + responseData);
                    }
                },
                error -> {
                    Log.e("Storage API Error", "Response Error: " + error.toString());
                    Log.e("Storage API Error", "Response Error: " + error.getMessage());

                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(context.getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                });

        request.addPart(new MultipartRequest.FilePart("", "image/jpg", null, inputData));

        Log.d("Storage API", "Sent request: " + apiUrl);
        queue.add(request);

        return rng + ".jpg";
    }

    public static int getBitmapOriention(Context context, Uri uri){
        ExifInterface exif;
        int orientation = 0;
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            exif = new ExifInterface(input);
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);
//            Log.e("getBitmapOriention", "getBitmapOriention: " + orientation);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getBitmapOriention", "getBitmapOriention: " + e.getMessage());
        }
        return orientation;
    }

//    public static String getRealPathFromURI(Context context, Uri contentUri) {
//        Cursor cursor = null;
//        try {
//            String[] proj = { MediaStore.Images.Media.DATA };
//            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
//            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            cursor.moveToFirst();
//            return cursor.getString(column_index);
//        } finally {
//            if (cursor != null) {
//                cursor.close();
//            }
//        }
//    }
}
