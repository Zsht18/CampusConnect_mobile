package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.LoginResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity handles user authentication.
 * It authenticates ID Number and Password against the backend database
 * and saves logged-in user details to SharedPreferences.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText idNumberEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // Bind layout

        // Initialize UI components
        idNumberEditText = findViewById(R.id.idNumberEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // Handle Login button click
        loginButton.setOnClickListener(v -> {
            String idNumber = idNumberEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validate that input fields are not empty
            if (idNumber.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please enter both ID Number and password.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(LoginActivity.this, "Logging in...", Toast.LENGTH_SHORT).show();

                // Send login request to backend API
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<LoginResponse> call = apiService.login(idNumber, password);

                call.enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (response.body().isSuccess()) {
                                Toast.makeText(LoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                                
                                // Save logged-in user profile data locally
                                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putString("USER_FULL_NAME", response.body().getUser().getFullName());
                                if (response.body().getUser().getUsername() != null) {
                                    editor.putString("USER_USERNAME", response.body().getUser().getUsername());
                                }
                                editor.putString("USER_CREATED_AT", response.body().getUser().getCreatedAt());
                                editor.putInt("USER_POSTS_COUNT", response.body().getUser().getPostsCount());
                                try {
                                    int userId = Integer.parseInt(response.body().getUser().getId());
                                    editor.putInt("USER_ID", userId);
                                    String profilePicUrl = response.body().getUser().getProfilePicUrl();
                                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                        editor.putString("USER_PROFILE_PIC_URI_" + userId, profilePicUrl);
                                    } else {
                                        editor.remove("USER_PROFILE_PIC_URI_" + userId);
                                    }
                                    editor.remove("USER_PROFILE_PIC_URL");
                                } catch (Exception e) {
                                    editor.putInt("USER_ID", 1);
                                }
                                editor.apply(); // Save changes

                                // Navigate to Home Feed screen
                                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                startActivity(intent);
                                finish(); // Close login screen
                            } else {
                                // Display error message returned by server (e.g. "Wrong password")
                                Toast.makeText(LoginActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Server Error (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable t) {
                        Toast.makeText(LoginActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        // Navigate to Sign Up screen when Sign Up button is tapped
        signUpButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }
}