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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import java.io.File;
import java.io.FileOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private EditText postTitleInput;
    private Spinner postCategoryInput;
    private EditText postContentInput;
    private Switch switchAnonymous;
    private Button btnPost;
    private static final int PICK_FILE_REQUEST = 102;
    private Uri selectedFileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);
        AvatarHelper.loadHeaderAvatar(this);

        postTitleInput = findViewById(R.id.postTitleInput);
        postCategoryInput = findViewById(R.id.postCategoryInput);
        android.widget.ArrayAdapter<CharSequence> collegeAdapter =
                android.widget.ArrayAdapter.createFromResource(this, R.array.college_choices,
                        R.layout.item_college_spinner);
        collegeAdapter.setDropDownViewResource(R.layout.item_college_spinner_dropdown);
        postCategoryInput.setAdapter(collegeAdapter);
        postCategoryInput.setPopupBackgroundDrawable(
                getDrawable(R.drawable.college_spinner_popup_bg));
        int userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
        String preferredCollege = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("PREFERRED_COLLEGE_" + userId, "");
        selectCollege(collegeAdapter, postCategoryInput, preferredCollege);
        postContentInput = findViewById(R.id.postContentInput);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        btnPost = findViewById(R.id.btnPost);
        updatePostingOptionAvatar(false);
        switchAnonymous.setOnCheckedChangeListener((buttonView, isChecked) ->
                updatePostingOption(isChecked));

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

        View uploadBoxContainer = findViewById(R.id.uploadBoxContainer);
        if (uploadBoxContainer != null) {
            uploadBoxContainer.setOnClickListener(v -> openFilePicker());
        }

        if (btnPost != null) {
            btnPost.setOnClickListener(v -> submitPost());
        }
    }

    private void updatePostingOption(boolean anonymous) {
        updatePostingOptionAvatar(anonymous);
        TextView label = findViewById(R.id.postingOptionLabel);
        if (label != null) {
            label.setText(anonymous ? "Post Anonymously" : "Post Publicly");
        }
        TextView description = findViewById(R.id.postingOptionDescription);
        if (description != null) {
            description.setText(anonymous
                    ? "Your name will NOT be visible to other students."
                    : "Your name will be visible to other students.");
        }
    }

    private void updatePostingOptionAvatar(boolean anonymous) {
        android.widget.ImageView imageView = findViewById(R.id.postingOptionAvatarImage);
        TextView textView = findViewById(R.id.postingOptionAvatarText);
        if (anonymous) {
            AvatarHelper.loadAvatar(this, "", imageView, textView);
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);
        File localFile = new File(getFilesDir(), "profile_user_" + userId + ".jpg");
        String target = localFile.exists()
                ? "file://" + localFile.getAbsolutePath()
                : prefs.getString("USER_PROFILE_PIC_URI_" + userId, "");
        AvatarHelper.loadAvatar(this, target, imageView, textView);
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
        if (uri.getScheme().equals("content")) {
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
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void submitPost() {
        String title = postTitleInput.getText().toString().trim();
        String category = postCategoryInput.getSelectedItem().toString().trim();
        String content = postContentInput.getText().toString().trim();
        int isAnonymous = switchAnonymous.isChecked() ? 1 : 0;

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please enter both Title and Question/Discussion.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        MultipartBody.Part filePart = null;
        if (selectedFileUri != null) {
            filePart = createAttachmentPart(selectedFileUri);
            if (filePart == null) {
                Toast.makeText(this, "Unable to read the selected attachment.", Toast.LENGTH_SHORT).show();
                return;
            }

        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.createPost(
                textPart(String.valueOf(userId)),
                textPart(title),
                textPart(category),
                textPart(content),
                textPart(String.valueOf(isAnonymous)),
                filePart);

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

    private void selectCollege(android.widget.ArrayAdapter<CharSequence> adapter,
                               Spinner spinner, String college) {
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(college)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private RequestBody textPart(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    private MultipartBody.Part createAttachmentPart(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || fileName.isEmpty()) fileName = "attachment";
            File cachedFile = new File(getCacheDir(), "post_attachment_" + System.currentTimeMillis());
            try (java.io.InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(cachedFile)) {
                if (input == null) return null;
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
            String mimeType = getContentResolver().getType(uri);
            RequestBody body = RequestBody.create(MediaType.parse(mimeType != null ? mimeType : "application/octet-stream"), cachedFile);
            return MultipartBody.Part.createFormData("file", fileName, body);
        } catch (Exception e) {
            return null;
        }
    }
}