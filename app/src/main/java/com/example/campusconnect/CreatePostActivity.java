package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * CreatePostActivity allows students to compose and publish new academic posts,
 * select target college, attach multiple files/resources, and choose anonymous or public posting.
 */
public class CreatePostActivity extends AppCompatActivity {

    private EditText postTitleInput;
    private Spinner postCategoryInput;
    private EditText postContentInput;
    private Switch switchAnonymous;
    private Button btnPost;
    private static final int PICK_FILE_REQUEST = 102;
    private final List<Uri> selectedFileUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post); // Bind layout
        AvatarHelper.loadHeaderAvatar(this); // Load user avatar into top header

        postTitleInput = findViewById(R.id.postTitleInput);
        postCategoryInput = findViewById(R.id.postCategoryInput);

        // Configure College selection spinner
        ArrayAdapter<CharSequence> collegeAdapter =
                ArrayAdapter.createFromResource(this, R.array.college_choices, R.layout.item_college_spinner);
        collegeAdapter.setDropDownViewResource(R.layout.item_college_spinner_dropdown);
        postCategoryInput.setAdapter(collegeAdapter);
        postCategoryInput.setPopupBackgroundDrawable(getDrawable(R.drawable.college_spinner_popup_bg));

        // Pre-select user's preferred college if saved
        int userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
        String preferredCollege = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("PREFERRED_COLLEGE_" + userId, "");
        selectCollege(collegeAdapter, postCategoryInput, preferredCollege);

        postContentInput = findViewById(R.id.postContentInput);
        switchAnonymous = findViewById(R.id.switchAnonymous);
        btnPost = findViewById(R.id.btnPost);

        // Update posting option avatar preview
        updatePostingOptionAvatar(false);
        switchAnonymous.setOnCheckedChangeListener((buttonView, isChecked) -> updatePostingOption(isChecked));

        // Cancel button listener -> closes activity
        Button btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        // Top header profile button
        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(CreatePostActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Attach File button listener -> opens system file picker
        View uploadBoxContainer = findViewById(R.id.uploadBoxContainer);
        if (uploadBoxContainer != null) {
            uploadBoxContainer.setOnClickListener(v -> openFilePicker());
        }

        // Post button listener -> submits post to backend
        if (btnPost != null) {
            btnPost.setOnClickListener(v -> submitPost());
        }
    }

    /**
     * Updates label, description, and avatar preview for public vs. anonymous posting.
     */
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

    /**
     * Renders avatar image in posting option preview box based on anonymous setting.
     */
    private void updatePostingOptionAvatar(boolean anonymous) {
        ImageView imageView = findViewById(R.id.postingOptionAvatarImage);
        TextView textView = findViewById(R.id.postingOptionAvatarText);
        if (anonymous) {
            AvatarHelper.loadAvatar(this, "", imageView, textView); // Show default anonymous avatar
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

    /**
     * Opens system document/file picker allowing multi-select for PDF, DOC, or Image attachments.
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        String[] mimetypes = {"application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(Intent.createChooser(intent, "Select File(s) or Resource"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUris.clear();
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    selectedFileUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedFileUris.add(data.getData());
            }

            TextView uploadText = findViewById(R.id.uploadBoxText);
            if (uploadText != null) {
                if (selectedFileUris.size() == 1) {
                    uploadText.setText("📎 Selected: " + getFileName(selectedFileUris.get(0)));
                } else if (selectedFileUris.size() > 1) {
                    uploadText.setText("📎 Selected " + selectedFileUris.size() + " files");
                }
            }
            Toast.makeText(this, selectedFileUris.size() + " file(s) attached successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resolves display file name from Uri.
     */
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

    /**
     * Validates input fields and submits post via Retrofit multipart request.
     */
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

        List<MultipartBody.Part> fileParts = new ArrayList<>();
        for (Uri uri : selectedFileUris) {
            MultipartBody.Part part = createAttachmentPart(uri);
            if (part != null) {
                fileParts.add(part);
            }
        }

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.createPost(
                textPart(String.valueOf(userId)),
                textPart(title),
                textPart(category),
                textPart(content),
                textPart(String.valueOf(isAnonymous)),
                fileParts);

        Toast.makeText(this, "Publishing post...", Toast.LENGTH_SHORT).show();

        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(CreatePostActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    if (response.body().isSuccess()) {
                        finish(); // Close activity and return to Home
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

    private void selectCollege(ArrayAdapter<CharSequence> adapter, Spinner spinner, String college) {
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

    /**
     * Converts selected attachment Uri into a Retrofit MultipartBody.Part for file upload.
     */
    private MultipartBody.Part createAttachmentPart(Uri uri) {
        try {
            String fileName = getFileName(uri);
            if (fileName == null || fileName.isEmpty()) fileName = "attachment";
            File cachedFile = new File(getCacheDir(), "post_attachment_" + System.currentTimeMillis());
            try (InputStream input = getContentResolver().openInputStream(uri);
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
            return MultipartBody.Part.createFormData("files[]", fileName, body);
        } catch (Exception e) {
            return null;
        }
    }
}