package com.example.campusconnect;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText idNumberEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Binds the layout

        // Initialize Views
        idNumberEditText = findViewById(R.id.idNumberEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Handle Login button click
        loginButton.setOnClickListener(v -> {
            String idNumber = idNumberEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (idNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both ID Number and password.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

                com.example.campusconnect.api.ApiService apiService =
                    com.example.campusconnect.api.ApiClient.getClient().create(com.example.campusconnect.api.ApiService.class);

                retrofit2.Call<com.example.campusconnect.models.LoginResponse> call = apiService.login(idNumber, password);
                call.enqueue(new retrofit2.Callback<com.example.campusconnect.models.LoginResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.campusconnect.models.LoginResponse> call, retrofit2.Response<com.example.campusconnect.models.LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                
                                // Save user data globally using SharedPreferences
                                android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                android.content.SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("USER_FULL_NAME", response.body().getUser().getFullName());
                                if (response.body().getUser().getUsername() != null) {
                                    editor.putString("USER_USERNAME", response.body().getUser().getUsername());
                                }
                                editor.putString("USER_CREATED_AT", response.body().getUser().getCreatedAt());
                                editor.putInt("USER_POSTS_COUNT", response.body().getUser().getPostsCount());
                                try {
                                    int userId = Integer.parseInt(response.body().getUser().getId());
                                    editor.putInt("USER_ID", userId);
                                } catch (Exception e) {
                                    editor.putInt("USER_ID", 1);
                                }
                                editor.apply();

                                android.content.Intent intent = new android.content.Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // This will print either "Wrong ID Number" or "Wrong password" dynamically from the database
                                Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            try {
                                String errorMsg = "Server Error (" + response.code() + ")";
                                if (response.errorBody() != null) {
                                    String errorJson = response.errorBody().string();
                                    com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(errorJson).getAsJsonObject();
                                    if (obj.has("message")) {
                                        errorMsg = obj.get("message").getAsString();
                                    }
                                }
                                Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(LoginActivity.this, "Server Error (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
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