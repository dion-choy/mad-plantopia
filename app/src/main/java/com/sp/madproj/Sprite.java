package com.sp.madproj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class Sprite {
    private Bitmap bm;
    private RectF position;
    private float scaleX;
    private float scaleY;
    private boolean clickable;

    public Map<String, String> attrs = new HashMap<>();


    public Sprite(Context context, int units, Bitmap bm, float left, float top, float scaleX, float scaleY) {
        // Clickable by default
        this(context, units, bm, left, top, scaleX, scaleY, true);
    }

    public Sprite(Context context, int units, Bitmap bm, float left, float top, float scaleX, float scaleY, boolean clickable) {
        this.bm = bm;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        if (units == CanvasView.DP) {
            left = CanvasView.pxFromDp(left, context);
            top = CanvasView.pxFromDp(top, context);
        }
        position = new RectF(left, top, left + bm.getWidth(), top + bm.getHeight());
        this.clickable = clickable;
    }

    public void drawOn(Canvas canvas, CanvasView canvasView) {
        canvas.save();
        canvas.scale(canvasView.getBgScale(), canvasView.getBgScale());
        canvas.scale(scaleX, scaleY, position.left, position.top);
        if (scaleX < 0) {
            canvas.translate(-bm.getWidth(), 0);
        }
        if (scaleY < 0) {
            canvas.translate(0, -bm.getHeight());
        }

        canvas.drawBitmap(bm, position.left, position.top, null);

        canvas.restore();
    }

    public boolean contains(float x, float y) {
        return position.contains(x, y);
    }
    
    public boolean isClickable() {
        return clickable;
    }

    public void click(Context context) {
        Log.d("Sprite", "Clicked");
        Toast.makeText(context, "Clicked", Toast.LENGTH_SHORT).show();
    }

    public Bitmap getBitmap() {
        return this.bm;
    }
}
