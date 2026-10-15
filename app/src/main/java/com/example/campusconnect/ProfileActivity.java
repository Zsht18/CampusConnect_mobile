package com.example.campusconnect;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        android.widget.TextView profileUsernameText = findViewById(R.id.profileUsernameText);
        android.widget.TextView accountAgeText = findViewById(R.id.accountAgeText);
        android.widget.TextView postsCountText = findViewById(R.id.postsCountText);
        
        // Retrieve the data saved from the LoginActivity
        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String fullName = prefs.getString("USER_FULL_NAME", "");
        if (fullName != null && !fullName.isEmpty() && profileUsernameText != null) {
            profileUsernameText.setText(fullName);
        }

        int postsCount = prefs.getInt("USER_POSTS_COUNT", -1);
        if (postsCount != -1 && postsCountText != null) {
            postsCountText.setText(postsCount + " Posts");
        }

        String createdAtStr = prefs.getString("USER_CREATED_AT", "");
        if (createdAtStr != null && !createdAtStr.isEmpty() && accountAgeText != null) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date createdAtDate = sdf.parse(createdAtStr);
                long diffInMillis = System.currentTimeMillis() - createdAtDate.getTime();
                long months = diffInMillis / (1000L * 60 * 60 * 24 * 30L);
                if (months == 0) {
                    accountAgeText.setText("New");
                } else {
                    accountAgeText.setText(months + " Months");
                }
            } catch (Exception e) {
                accountAgeText.setText("Active");
            }
        }
    }

    public void openPostDetails(android.view.View view) {
        android.content.Intent intent = new android.content.Intent(ProfileActivity.this, PostDetailsActivity.class);
        startActivity(intent);
    }
}