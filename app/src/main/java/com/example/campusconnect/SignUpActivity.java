package com.example.campusconnect;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText idNumberEditText;
    private EditText fullNameEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText retypePasswordEditText;
    private Button uploadPhotoButton;
    private Button signUpButton;
    private TextView returnToLoginText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // Binds the layout

        // Initialize Views
        emailEditText = findViewById(R.id.emailEditText);
        idNumberEditText = findViewById(R.id.idNumberEditText);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        retypePasswordEditText = findViewById(R.id.retypePasswordEditText);
        uploadPhotoButton = findViewById(R.id.uploadPhotoButton);
        signUpButton = findViewById(R.id.signUpButton);
        returnToLoginText = findViewById(R.id.returnToLoginText);

        uploadPhotoButton.setOnClickListener(v -> {
            Toast.makeText(SignUpActivity.this, "Upload Photo Clicked", Toast.LENGTH_SHORT).show();
            // TODO: Add logic to pick an image from the gallery
        });

        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String idNumber = idNumberEditText.getText().toString().trim();
            String fullName = fullNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String retypePassword = retypePasswordEditText.getText().toString().trim();

            if (email.isEmpty() || idNumber.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(retypePassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(SignUpActivity.this, "Signing up...", Toast.LENGTH_SHORT).show();

            // Once your Hostinger API is live, uncomment the code below to connect to the database!
            com.example.campusconnect.api.ApiService apiService =
                com.example.campusconnect.api.ApiClient.getClient().create(com.example.campusconnect.api.ApiService.class);
            
            retrofit2.Call<com.example.campusconnect.models.SignupResponse> call = apiService.signup(email, idNumber, fullName, username, password);
            call.enqueue(new retrofit2.Callback<com.example.campusconnect.models.SignupResponse>() {
                @Override
                public void onResponse(retrofit2.Call<com.example.campusconnect.models.SignupResponse> call, retrofit2.Response<com.example.campusconnect.models.SignupResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(SignUpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        if (response.body().isSuccess()) {
                            finish(); // Return to Login
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Signup failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<com.example.campusconnect.models.SignupResponse> call, Throwable t) {
                    Toast.makeText(SignUpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        });
    }
}