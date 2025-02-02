package com.sp.madproj;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.NoConnectionError;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class FeedSettingsActivity extends AppCompatActivity {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private FirebaseUser currentUser;

    private ConstraintLayout authContainer;
    private ActivityResultLauncher<Intent> getImage;

    private ImageView pfpIcon;

    private Animation fadeOutBg;
    private Animation fadeInBg;

    @Override
    public void onBackPressed() {
        if (authContainer.getVisibility() == View.VISIBLE) {
            authContainer.startAnimation(fadeOutBg);
            Log.d("FADE OUT BACK", "now");
            authContainer.setVisibility(View.GONE);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed_settings);

        fadeOutBg = AnimationUtils.loadAnimation(this, R.anim.fadeout_bg);
        fadeInBg = AnimationUtils.loadAnimation(this, R.anim.fadein_bg);

        findViewById(R.id.logOut).setOnClickListener(view -> {
            auth.signOut();
            finish();
        });

        findViewById(R.id.backBtn).setOnClickListener(view -> finish());

        authContainer = findViewById(R.id.reauth_container);
        authContainer.setOnClickListener(view -> {
            Log.d("FADE OUT CLICK", "now");
            authContainer.startAnimation(fadeOutBg);
            authContainer.setVisibility(View.GONE);
            new Handler().postDelayed(() -> authContainer.clearAnimation(), 300);
        });

        findViewById(R.id.delete).setOnClickListener(view -> {
            Log.d("FADE in", "now");
            authContainer.startAnimation(fadeInBg);
            authContainer.setVisibility(View.VISIBLE);
        });

        findViewById(R.id.authBtn).setOnClickListener(view -> {
            EditText passwordEdit = findViewById(R.id.passwordReauth);
            if (passwordEdit.getText().toString().isEmpty()) {
                ((TextInputLayout) findViewById(R.id.passwordContainer)).setError("Enter a password");
                return;
            }
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            AuthCredential credential = EmailAuthProvider
                    .getCredential(
                            user.getEmail(),
                            passwordEdit.getText().toString());

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("USER REAUTH SUCCESS", "User re-authenticated.");
                            deleteFromFirebase();
                        } else {
                            ((TextInputLayout) findViewById(R.id.passwordContainer)).setError("Wrong password");
                            Log.e("USER REAUTH ERROR", task.getException().toString());
//                                    Toast.makeText(getApplicationContext(), "Error reauthenticating account", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        findViewById(R.id.changePfp).setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            getImage.launch(intent);
        });

        getImage = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK || result.getResultCode() == CamActivity.IMAGE_URI
                            && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        Log.d("result", imageUri.toString());

                        String imageKey = Storage.uploadImgSupa(FeedSettingsActivity.this, imageUri, Storage.pfpStorage);

                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(Storage.pfpStorage + imageKey))
                                .build();

                        FirebaseUser user = auth.getCurrentUser();
                        if (!user.getPhotoUrl().toString().equals(Storage.pfpStorage + "default.png")) {
                            Storage.deleteObjSupa(FeedSettingsActivity.this, auth.getCurrentUser().getPhotoUrl().toString());
                        }

                        user.updateProfile(profileUpdates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Log.d("USER PROFILE", "User profile updated.");
                                        Toast.makeText(FeedSettingsActivity.this, "Profile picture updated", Toast.LENGTH_SHORT)
                                                        .show();

                                        updatePfp(currentUser.getPhotoUrl().toString());

                                        Picasso.get()
                                                .load(currentUser.getPhotoUrl())
                                                .placeholder(R.mipmap.default_pfp_foreground)
                                                .into(pfpIcon, new Callback() {
                                                    @Override
                                                    public void onSuccess() {
                                                        pfpIcon.setImageTintList(null);
                                                    }

                                                    @Override
                                                    public void onError(Exception e) {}
                                                });
                                    }
                                });

                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            finish();
        }

        pfpIcon = findViewById(R.id.pfpIcon);
        Picasso.get()
                .load(currentUser.getPhotoUrl())
                .placeholder(R.mipmap.default_pfp_foreground)
                .into(pfpIcon, new Callback() {
                    @Override
                    public void onSuccess() {
                        pfpIcon.setImageTintList(null);
                    }

                    @Override
                    public void onError(Exception e) {}
                });

        ((TextView) findViewById(R.id.username))
                .setText(currentUser.getDisplayName());

        ((TextView) findViewById(R.id.email))
                .setText(currentUser.getEmail());

    }

    private void updatePfp(String pfpUrl) {
        Database.queryAstra(this,
                "UPDATE plantopia.user_info SET pfp='" + pfpUrl + "' WHERE username='" + currentUser.getDisplayName() + "';",
                response -> Log.i("PFP UPDATE", "Success"),
                error -> {
                    Log.e("PFP UPDATE ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void deleteFromFirebase() {
        String username = currentUser.getDisplayName();
        Uri pfpUrl = currentUser.getPhotoUrl();
        currentUser.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                new Thread(() -> deleteFromAstra(username)).start();

                new Thread(() -> {
                    if (!pfpUrl.toString().equals(Storage.pfpStorage + "default.png")) {
                        Storage.deleteObjSupa(FeedSettingsActivity.this, pfpUrl.toString());
                    }
                }).start();

                auth.signOut();
                finish();
            } else {
                Log.e("USER DELETE ERROR", task.getException().toString());
                Toast.makeText(getApplicationContext(), "Error deleting account", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void deleteFromAstra(String username) {
        Database.queryAstra(this,
                "DELETE FROM plantopia.user_info WHERE username='" + username + "';",
                response -> Log.d("USERS", response),
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
}