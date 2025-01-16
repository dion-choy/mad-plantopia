package com.sp.madproj;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth auth = FirebaseAuth.getInstance();

    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText confirmPassword;
    private TextInputLayout emailContainer;
    private TextInputLayout passwordContainer;
    private TextInputLayout confirmPasswordContainer;
    Button signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        email = findViewById(R.id.email);
        emailContainer = findViewById(R.id.emailContainer);
        password = findViewById(R.id.password);
        passwordContainer = findViewById(R.id.passwordContainer);
        confirmPassword = findViewById(R.id.confirmPassword);
        confirmPasswordContainer = findViewById(R.id.confirmPasswordContainer);
        signUp = findViewById(R.id.signUp);
        signUp.setOnClickListener(signUpUser);

        findViewById(R.id.resendEmail).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseUser user = auth.getCurrentUser();

                if (user != null) {
                    sendVerificationEmail(user);
                }
            }
        });
    }

    View.OnClickListener signUpUser = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            boolean error = false;
            if (email.getText().toString().isEmpty()) {
                emailContainer.setError("Enter your email");
                error = true;
            } else {
                if (!emailContainer.isErrorEnabled()) {
                    emailContainer.setErrorEnabled(false);
                }
            }

            if (password.getText().toString().length() < 6){
                passwordContainer.setError("Password must be more than 6 characters");
                error = true;
            }

            if (!password.getText().toString().equals(confirmPassword.getText().toString())) {
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
                    .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("SIGN UP", "createUserWithEmail:success");
                                FirebaseUser user = auth.getCurrentUser();

                                if (user != null) {
                                    sendVerificationEmail(user);
                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("SIGN UP", "createUserWithEmail:failure", task.getException());
                                if (task.getException().getClass().equals(FirebaseNetworkException.class)) {
                                    Toast.makeText(getApplicationContext(), "Please connect to internet",
                                            Toast.LENGTH_SHORT).show();
                                } else if (task.getException().getMessage()
                                        .equals("The email address is badly formatted.")) {
                                    emailContainer.setError("Please enter a proper email");
                                } else if(task.getException().getMessage()
                                        .equals("The email address is already in use by another account.")) {
                                    emailContainer.setError("Email is in use already");
                                } else {
                                    Toast.makeText(getApplicationContext(), "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
    //                                    updateUI(null);
                            }
                        }
                    });
        }
    };

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(getApplicationContext(), "Verification email sent!",
                                    Toast.LENGTH_SHORT).show();
//                            finish();
                        } else {
                            findViewById(R.id.resendEmail).setVisibility(View.VISIBLE);

                            Log.e("SEND EMAIL", task.getException().getMessage());
                            Toast.makeText(getApplicationContext(), "Error sending verification email",
                                    Toast.LENGTH_SHORT).show();
                        }
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