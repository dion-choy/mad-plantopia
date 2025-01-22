package com.sp.madproj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class ExpandImages extends AppCompatActivity {
    private Animation fadeOutBg;
    private Animation fadeInBg;

    private ViewPager2 expandedImage;
    private ImageAdapter adapter;
    private List<String> modelUris = new ArrayList<>();
    private LinearLayout backBtnContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expand_images);

        fadeOutBg = AnimationUtils.loadAnimation(this, R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(this, R.anim.fadein_bg);

        modelUris = getIntent().getStringArrayListExtra("images");
        Log.d("IMAGES", modelUris.toString());

        if (modelUris.isEmpty()) {
            finish();
        }

        adapter = new ImageAdapter(modelUris);

        expandedImage = findViewById(R.id.expandedImage);
        expandedImage.setAdapter(adapter);

        backBtnContainer = findViewById(R.id.backBtnContainer);
        findViewById(R.id.backBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageHolder> {
        private List<String> imageUriList = null;
        public ImageAdapter(List<String> imageUriList) {
            this.imageUriList = imageUriList;
        }

        @NonNull
        @Override
        public ImageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.image_expanded, parent, false);
            return new ImageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageHolder holder, int position) {
            Picasso.get()
                    .load(Storage.chatroomImageStorage + imageUriList.get(position))
                    .placeholder(R.drawable.gallery)
                    .into(holder.expandedImage);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (backBtnContainer.getVisibility() == View.VISIBLE) {
                        backBtnContainer.startAnimation(fadeOutBg);
                        backBtnContainer.setVisibility(View.GONE);
                    } else {
                        backBtnContainer.startAnimation(fadeInBg);
                        backBtnContainer.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return imageUriList.size();
        }

        class ImageHolder extends RecyclerView.ViewHolder{
            ImageView expandedImage;

            public ImageHolder(View view) {
                super(view);
                this.expandedImage = view.findViewById(R.id.fullImage);
            }
        }
    }
}