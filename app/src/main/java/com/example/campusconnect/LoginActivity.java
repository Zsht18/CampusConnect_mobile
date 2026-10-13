package com.example.campusconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Binds the layout

        // Initialize Views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Handle Login button click
        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

                com.example.campusconnect.api.ApiService apiService =
                    com.example.campusconnect.api.ApiClient.getClient().create(com.example.campusconnect.api.ApiService.class);

                retrofit2.Call<com.example.campusconnect.models.LoginResponse> call = apiService.login(email, password);
                call.enqueue(new retrofit2.Callback<com.example.campusconnect.models.LoginResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.campusconnect.models.LoginResponse> call, retrofit2.Response<com.example.campusconnect.models.LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                android.content.Intent intent = new android.content.Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // This will print either "Wrong email" or "Wrong password" dynamically from the database
                                Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Server Error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.campusconnect.models.LoginResponse> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // Handle Sign Up button click
        signUpButton.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}