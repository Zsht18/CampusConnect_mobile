package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText postTitleInput;
    private EditText postCategoryInput;
    private EditText postContentInput;
    private Switch switchAnonymous;
    private Button btnPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        postTitleInput = findViewById(R.id.postTitleInput);
        postCategoryInput = findViewById(R.id.postCategoryInput);
        postContentInput = findViewById(R.id.postContentInput);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        btnPost = findViewById(R.id.btnPost);

        Button btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(CreatePostActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> submitPost());
        }
    }

    private void submitPost() {
        String title = postTitleInput.getText().toString().trim();
        String category = postCategoryInput.getText().toString().trim();
        String content = postContentInput.getText().toString().trim();
        int isAnonymous = switchAnonymous.isChecked() ? 1 : 0;

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please enter both Title and Question/Discussion.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.createPost(userId, title, category, content, isAnonymous);

        Toast.makeText(this, "Publishing post...", Toast.LENGTH_SHORT).show();

        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CreatePostActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if (response.body().isSuccess()) {
                        finish();
                    }
                } else {
                    Toast.makeText(CreatePostActivity.this, "Failed to create post.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(CreatePostActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}