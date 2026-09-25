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
            Toast.makeText(SignUpActivity.this, "Sign Up Clicked", Toast.LENGTH_SHORT).show();
            // TODO: Add Sign Up logic here
        });

        returnToLoginText.setOnClickListener(v -> {
            // Simply finish this activity to return to the previous one (LoginActivity)
            finish();
        });
    }
}