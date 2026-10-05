package com.example.campusconnect;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
    }

    public void openPostDetails(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(ProfileActivity.this, PostDetailsActivity.class);
        startActivity(intent);
    }
}