package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditPostActivity extends AppCompatActivity {

    private EditText editPostTitleInput;
    private EditText editPostCategoryInput;
    private EditText editPostContentInput;
    private Switch editSwitchAnonymous;
    private Button btnUpdatePost;
    private int postId = 0;
    private int postUserId = 0;
    private static final int PICK_FILE_REQUEST = 102;
    private Uri selectedFileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        postId = getIntent().getIntExtra("POST_ID", 0);
        postUserId = getIntent().getIntExtra("POST_USER_ID", 0);
        String initialTitle = getIntent().getStringExtra("POST_TITLE");
        String initialCategory = getIntent().getStringExtra("POST_CATEGORY");
        String initialContent = getIntent().getStringExtra("POST_CONTENT");
        int initialAnonymous = getIntent().getIntExtra("POST_IS_ANONYMOUS", 0);

        editPostTitleInput = findViewById(R.id.editPostTitleInput);
        editPostCategoryInput = findViewById(R.id.editPostCategoryInput);
        editPostContentInput = findViewById(R.id.editPostContentInput);
        editSwitchAnonymous = findViewById(R.id.editSwitchAnonymous);
        btnUpdatePost = findViewById(R.id.btnUpdatePost);

        if (initialTitle != null) editPostTitleInput.setText(initialTitle);
        if (initialCategory != null) editPostCategoryInput.setText(initialCategory);
        if (initialContent != null) editPostContentInput.setText(initialContent);
        if (editSwitchAnonymous != null) editSwitchAnonymous.setChecked(initialAnonymous == 1);

        Button btnCancel = findViewById(R.id.btnCancelEdit);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(EditPostActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        View uploadBoxContainer = findViewById(R.id.uploadBoxContainer);
        if (uploadBoxContainer != null) {
            uploadBoxContainer.setOnClickListener(v -> openFilePicker());
        }

        if (btnUpdatePost != null) {
            btnUpdatePost.setOnClickListener(v -> updatePost());
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(Intent.createChooser(intent, "Select File or Resource"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            String fileName = getFileName(selectedFileUri);
            TextView uploadText = findViewById(R.id.uploadBoxText);
            if (uploadText != null) {
                uploadText.setText("📎 Selected: " + fileName);
            }
            Toast.makeText(this, "File attached successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void updatePost() {
        String title = editPostTitleInput.getText().toString().trim();
        String category = editPostCategoryInput.getText().toString().trim();
        String content = editPostContentInput.getText().toString().trim();
        int isAnonymous = editSwitchAnonymous.isChecked() ? 1 : 0;

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please enter both Title and Question/Discussion.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", postUserId > 0 ? postUserId : 1);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.updatePost(postId, userId, title, category, content, isAnonymous);

        Toast.makeText(this, "Updating post...", Toast.LENGTH_SHORT).show();

        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(EditPostActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if (response.body().isSuccess()) {
                        setResult(RESULT_OK);
                        finish();
                    }
                } else {
                    Toast.makeText(EditPostActivity.this, "Failed to update post.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(EditPostActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}