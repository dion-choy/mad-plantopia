package com.sp.madproj;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
    public static final int DP = 0;
    public static final int PX = 1;

    private final Context context;
    private Bitmap bg = null;

    private Bitmap buffer;
    private final Canvas bufferCanvas = new Canvas();

    public CanvasView(Context context) {
        super(context);
        this.context = context;
    }
    public CanvasView(Context context, int backgroundDrawableResource) {
        super(context);
        this.context = context;
        bg = getBitmapFromVectorDrawable(context, backgroundDrawableResource);

        Log.d("Canvas", "New canvas");
    }

    private float bgScale = 1;
    private float scrollY = 0;
    private float prevY = 0;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.i("Canvas View", "Layout Changed: " + left + ", " + top + ", " + right + ", " + bottom);
        bgScale = getWidth() / (float) bg.getWidth();

        buffer = Bitmap.createBitmap(getWidth(), (int) (bg.getHeight()* bgScale), Bitmap.Config.ARGB_8888);
        bufferCanvas.setBitmap(buffer);

        setOnTouchListener(new View.OnTouchListener() {
            private float initY = 0;
            private float initPos = 0;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
//                Log.d("Canvas", "onClick:" + motionEvent + motionEvent.getX() + ", " + motionEvent.getY());
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    initY = motionEvent.getY();
                    initPos = scrollY;
                    return true;
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    if (initY - motionEvent.getY() != 0) {
                        scrollY = initPos + initY - motionEvent.getY();

                        if (scrollY < 0) {
                            scrollY = 0;
                        } else if (scrollY >= bg.getHeight()* bgScale - bottom) {
                            scrollY = bg.getHeight() * bgScale - bottom;
                        }

                        if (scrollY != prevY){
                            invalidate();
                            prevY = scrollY;
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

        //Clear canvas
        bufferCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        //Draw background
        bufferCanvas.save();
        bufferCanvas.scale(bgScale, bgScale);

        bufferCanvas.drawBitmap(bg, 0, 0, null);

        bufferCanvas.restore();

        //Draw sprites
        for (Sprite sprite: allSprites) {
            sprite.draw();
        }

        //Draw from buffer scroll pos
        canvas.drawBitmap(buffer, 0, -scrollY, null);
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

    public static float dpFromPx(final float px, Context context) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public boolean remove(Sprite sprite) {
        return allSprites.remove(sprite);
    }

    public void add(Sprite sprite) {
        allSprites.add(sprite);
    }


    private final List<Sprite> allSprites = new ArrayList<>();
    private final List<Sprite> clickableSprites = new ArrayList<>();
    public class Sprite {
        private Bitmap bm;
        private RectF position;
        private float scale;

        public Sprite(int units, Bitmap bm, float left, float top, float scale) {
            this.bm = bm;
            this.scale = scale;
            if (units == CanvasView.DP) {
                left = pxFromDp(left, context);
                top = pxFromDp(top, context);
            }
            position = new RectF(left, top, left + bm.getWidth(), top + bm.getHeight());

            clickableSprites.add(this);
        }

        public Sprite(int units, Bitmap bm, float left, float top, float scale, boolean clickable) {
            this(units, bm, left, top, scale);
            if (clickable) {
                clickableSprites.remove(this);
            }
        }

        private void draw() {
            bufferCanvas.save();
            bufferCanvas.scale(CanvasView.this.bgScale, CanvasView.this.bgScale);
            bufferCanvas.scale(scale, scale, position.left, position.top);

            bufferCanvas.drawBitmap(bm, position.left, position.top, null);

            bufferCanvas.restore();
        }

        private boolean contains(float x, float y) {
            return position.contains(x/scale, (y+ scrollY)/scale);
        }

        public Bitmap getBitmap() {
            return this.bm;
        }
    }
}
