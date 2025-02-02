package com.sp.madproj.Feed;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.NoConnectionError;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.sp.madproj.R;
import com.sp.madproj.Utils.Database;
import com.sp.madproj.Utils.Storage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private TextInputEditText username;
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText confirmPassword;
    private TextInputLayout usernameContainer;
    private TextInputLayout emailContainer;
    private TextInputLayout passwordContainer;
    private TextInputLayout confirmPasswordContainer;
    Button signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        username = findViewById(R.id.chatName);
        usernameContainer = findViewById(R.id.usernameContainer);
        email = findViewById(R.id.email);
        emailContainer = findViewById(R.id.emailContainer);
        password = findViewById(R.id.password);
        passwordContainer = findViewById(R.id.passwordContainer);
        confirmPassword = findViewById(R.id.confirmPassword);
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        signUp = findViewById(R.id.signUp);
        signUp.setOnClickListener(signUpUser);

        findViewById(R.id.resendEmail).setOnClickListener(view -> {
            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                sendVerificationEmail(user);
            }
        });

        findViewById(R.id.backBtn).setOnClickListener(view -> finish());
    }

    View.OnClickListener signUpUser = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (username.getText() == null) {
                return;
            }
            String usernameStr = username.getText().toString();
            if (usernameStr.isEmpty()) {
                usernameContainer.setError("Enter your username");
            } else if (usernameStr.contains(" ")) {
                usernameContainer.setError("Username cannot contain space");
            } else {
                if (usernameContainer.isErrorEnabled() &&
                        (Objects.equals(usernameContainer.getError(), "Enter your username") ||
                                Objects.equals(usernameContainer.getError(), "Username cannot contain space"))
                ) {
                    usernameContainer.setErrorEnabled(false);
                }

                checkUsernames(username.getText().toString());
            }

        }
    };

    private void continuteValidation(boolean error, String username) {
        if (email.getText() == null) {
            return;
        }
        String emailStr = email.getText().toString();
        if (emailStr.isEmpty()) {
            emailContainer.setError("Enter your email");
            error = true;
        } else {
            if (emailContainer.isErrorEnabled() && Objects.equals(emailContainer.getError(), "Enter your email")) {
                emailContainer.setErrorEnabled(false);
            }
        }

        if (password.getText() == null || confirmPassword.getText() == null) {
            return;
        }
        String passwordStr = password.getText().toString();
        String confirmPasswordStr = confirmPassword.getText().toString();
        if (passwordStr.length() < 6){
            passwordContainer.setError("Password must be more than 6 characters");
            error = true;
        } else {
            passwordContainer.setErrorEnabled(false);
        }

        if (!passwordStr.equals(confirmPasswordStr)) {
            passwordContainer.setError("Password must be the same");
            confirmPasswordContainer.setError("Password must be the same");
            Log.d("ERROR", password.getText().toString() + confirmPassword.getText().toString());
            error = true;
        } else if (!error) {
            passwordContainer.setErrorEnabled(false);
            confirmPasswordContainer.setErrorEnabled(false);
        }

        if (error) {
            return;
        }

        auth.createUserWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                .addOnCompleteListener(SignUpActivity.this, task -> {
                    if (task.isSuccessful()) {
                        addUsername(username, email.getText().toString());
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("SIGN UP", "createUserWithEmail:success");
                        FirebaseUser user = auth.getCurrentUser();

                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .setPhotoUri(Uri.parse(Storage.pfpStorage + "default.png"))
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            Log.d("USER PROFILE", "User profile updated.");
                                        }
                                    });

                            findViewById(R.id.resendEmail).setVisibility(View.VISIBLE);
                            sendVerificationEmail(user);
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("SIGN UP", "createUserWithEmail:failure", task.getException());
                        if (task.getException().getClass().equals(FirebaseNetworkException.class)) {
                            Toast.makeText(getApplicationContext(), "Please connect to internet",
                                    Toast.LENGTH_SHORT).show();
                        } else if (Objects.equals(task.getException().getMessage(), "The email address is badly formatted.")) {
                            emailContainer.setError("Please enter a proper email");
                        } else if(Objects.equals(task.getException().getMessage(), "The email address is already in use by another account.")) {
                            emailContainer.setError("Email is in use already");
                        } else {
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void addUsername(String username, String email) {
        Database.queryAstra(this,
                "INSERT INTO plantopia.user_info (username, email, pfp, uid) VALUES('" + username +
                        "', '" + email + "', '" + Storage.pfpStorage + "default.png', '" + FirebaseAuth.getInstance().getCurrentUser().getUid() + "');",
                response -> Log.d("USERS", response),
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkUsernames(String username) {
        Database.queryAstra(this,
                "SELECT * FROM plantopia.user_info WHERE username = '" + username + "';",
                response -> {
                    Log.d("USERS", response);
                    try {
                        JSONObject responseObj = new JSONObject(response);
                        if (responseObj.getInt("count") > 0) {
                            usernameContainer.setError("Username in use already");
                            continuteValidation(true, username);
                        } else {
                            if (usernameContainer.isErrorEnabled() && Objects.equals(usernameContainer.getError(), "Username in use already")) {
                                usernameContainer.setErrorEnabled(false);
                            }
                            continuteValidation(false, username);
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                },
                error -> {
                    Log.e("USERS ERROR", error.toString());
                    if (error.getClass() == NoConnectionError.class) {
                        Toast.makeText(getApplicationContext(), "Please connect to internet", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(SignUpActivity.this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Verification email sent!",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Log.e("SEND EMAIL", task.getException().getMessage());
                        Toast.makeText(getApplicationContext(), "Error sending verification email",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null){
            Log.i("USER", currentUser.getEmail());
        } else {
            Log.d("Not signed in", "onStart: Not Signed In");
        }

    }
}