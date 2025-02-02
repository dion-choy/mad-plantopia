package com.sp.madproj;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogInActivity extends AppCompatActivity {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputLayout emailContainer;
    private TextInputLayout passwordContainer;
    Button logIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        email = findViewById(R.id.email);
        emailContainer = findViewById(R.id.emailContainer);
        password = findViewById(R.id.password);
        passwordContainer = findViewById(R.id.passwordContainer);
        logIn = findViewById(R.id.logIn);
        logIn.setOnClickListener(view -> {
            boolean error = false;
            if (email.getText() == null || email.getText().toString().isEmpty()) {
                emailContainer.setError("Enter your email");
                error = true;
            } else {
                emailContainer.setErrorEnabled(false);
            }

            if (password.getText() == null || password.getText().toString().isEmpty()){
                passwordContainer.setError("Enter your password");
                error = true;
            } else {
                passwordContainer.setErrorEnabled(false);
            }

            if (error) {
                return;
            }

            auth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(LogInActivity.this, task -> {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("LOG IN", "signInWithEmail:success");
                            finish();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("LOG IN", "signInWithEmail:failure", task.getException());
                            if (task.getException().getClass().equals(FirebaseNetworkException.class)) {
                                Toast.makeText(getApplicationContext(), "Please connect to internet",
                                        Toast.LENGTH_SHORT).show();
                            } else if (task.getException().getMessage()
                                    .equals("The email address is badly formatted.")) {
                                emailContainer.setError("Please enter a proper email");
                            } else if (task.getException().getMessage()
                                    .equals("The supplied auth credential is incorrect, malformed or has expired.")) {
                                emailContainer.setError("Invalid email or password");
                                passwordContainer.setError("Invalid email or password");
                            } else {
                                Toast.makeText(LogInActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        findViewById(R.id.backBtn).setOnClickListener(view -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null){
            Log.d("Not signed in", "onStart: Not Signed In");
        }

    }
}