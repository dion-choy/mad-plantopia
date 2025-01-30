package com.sp.madproj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class CanvasView extends View {
    private final Context context;
    private final Bitmap bg;
    private final Bitmap pot;
    private final Bitmap cactus;
    private final Bitmap flower;
    private final Bitmap upright;
    private final Bitmap vine;

    public static List<Sprite> pots = new ArrayList<>();
    public static List<Sprite> plants = new ArrayList<>();

    private Bitmap buffer;
    private static final Canvas bufferCanvas = new Canvas();

    public CanvasView(Context context, int backgroundDrawableResource) {
        super(context);
        this.context = context;
        bg = getBitmapFromVectorDrawable(context, backgroundDrawableResource);
        pot = getBitmapFromVectorDrawable(context, R.drawable.pot);
        cactus = getBitmapFromVectorDrawable(context, R.drawable.plant_cactus);
        flower = getBitmapFromVectorDrawable(context, R.drawable.plant_flower);
        upright = getBitmapFromVectorDrawable(context, R.drawable.plant_upright);
        vine = getBitmapFromVectorDrawable(context, R.drawable.plant_vine);

        Log.d("Canvas", "New canvas");
    }

    private static float bgScale = 1;
    private static float posY = 0;
    private float prevY = 0;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.i("Canvas View", "Layout Changed: " + left + ", " + top + ", " + right + ", " + bottom);
        bgScale = getWidth() / (float) bg.getWidth();

        buffer = Bitmap.createBitmap(getWidth(), (int) (bg.getHeight()* bgScale), Bitmap.Config.ARGB_8888);
        bufferCanvas.setBitmap(buffer);

        bufferCanvas.save();
        bufferCanvas.scale(bgScale, bgScale);

        bufferCanvas.drawBitmap(bg, 0, 0, null);

        bufferCanvas.restore();

        setOnTouchListener(new View.OnTouchListener() {
            private float initY = 0;
            private float initPos = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                Log.d("Canvas", "onClick:" + motionEvent + motionEvent.getX() + ", " + motionEvent.getY());
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    initY = motionEvent.getY();
                    initPos = posY;
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    if (initY - motionEvent.getY() != 0) {
                        posY = initPos + initY - motionEvent.getY();

                        if (posY < 0) {
                            posY = 0;
                        } else if (posY >= bg.getHeight()* bgScale - bottom) {
                            posY = bg.getHeight() * bgScale - bottom;
                        }

                        if (posY != prevY){
                            invalidate();
                            prevY = posY;
                        }
                    }

                    return true;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if (initY == motionEvent.getY()) {
                        clickAt(motionEvent.getX(), motionEvent.getY());
                        performClick();
                    }
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Log.d("Canvas", "onDraw");

        canvas.drawBitmap(buffer, 0, -posY, null);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void clickAt(float x, float y) {
        Log.d("Sprite", clickableSprites.toString());
        for (Sprite sprite: clickableSprites) {
            if (sprite.contains(x, y)) {
                Log.d("Sprite", "Clicked");
                Toast.makeText(getContext(), "Clicked", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static float pxFromDp(final float dp, Context context) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private static List<Sprite> clickableSprites = new ArrayList<>();
    public static class Sprite {
        private Bitmap bm;
        private RectF position;
        private float scale;

        public Sprite(Bitmap bm, float left, float top, float scale) {
            this.bm = bm;
            this.scale = scale;
            position = new RectF(left, top, left + bm.getWidth(), top + bm.getHeight());

            clickableSprites.add(this);
        }

        public Sprite(Bitmap bm, float left, float top, float scale, boolean clickable) {
            this(bm, left, top, scale);
            if (clickable) {
                clickableSprites.remove(this);
            }
        }

        public void draw() {
            bufferCanvas.save();
            bufferCanvas.scale(CanvasView.bgScale, CanvasView.bgScale);
            bufferCanvas.scale(scale, scale, position.left, position.top);

            bufferCanvas.drawBitmap(bm, position.left, position.top, null);

            bufferCanvas.restore();
        }

        public boolean contains(float x, float y) {
            return position.contains(x/scale, (y+posY)/scale);
        }

        public Bitmap getBitmap() {
            return this.bm;
        }
    }
}
