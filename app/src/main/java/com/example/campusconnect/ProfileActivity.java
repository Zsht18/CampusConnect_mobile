package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;
import com.example.campusconnect.models.PostModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileActivity displays the logged-in student's profile details,
 * account age, post contributions, saved posts, and options to edit profile or log out.
 */
public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Bind layout

        TextView profileUsernameText = findViewById(R.id.profileUsernameText);
        TextView profileHandleText = findViewById(R.id.profileHandleText);
        TextView accountAgeText = findViewById(R.id.accountAgeText);
        TextView postsCountText = findViewById(R.id.postsCountText);

        // Retrieve the saved user session data from LoginActivity
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        // Load saved profile picture if present
        String picUriStr = prefs.getString("USER_PROFILE_PIC_URI_" + userId, "");
        AvatarHelper.loadAvatar(this, picUriStr, findViewById(R.id.profileAvatarImage), findViewById(R.id.profileAvatarText));

        String fullName = prefs.getString("USER_FULL_NAME", "");
        if (fullName != null && !fullName.isEmpty() && profileUsernameText != null) {
            profileUsernameText.setText(fullName);
        }

        String username = prefs.getString("USER_USERNAME", "");
        if (username != null && !username.isEmpty() && profileHandleText != null) {
            profileHandleText.setText(username.startsWith("@") ? username : "@" + username);
        } else if (profileHandleText != null && fullName != null && !fullName.isEmpty()) {
            profileHandleText.setText("@" + fullName.toLowerCase().replaceAll("\\s+", ""));
        }

        int postsCount = prefs.getInt("USER_POSTS_COUNT", -1);
        if (postsCount != -1 && postsCountText != null) {
            postsCountText.setText(postsCount + " Posts");
        }

        // Calculate and display relative account age from registration timestamp
        String createdAtStr = prefs.getString("USER_CREATED_AT", "");
        if (createdAtStr != null && !createdAtStr.isEmpty() && accountAgeText != null) {
            java.util.Date createdAtDate = null;
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                createdAtDate = inputFormat.parse(createdAtStr);
            } catch (Exception e) {
                try {
                    java.text.SimpleDateFormat inputFormatDateOnly = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    createdAtDate = inputFormatDateOnly.parse(createdAtStr);
                } catch (Exception ex) {
                    // Ignore
                }
            }

            if (createdAtDate != null) {
                accountAgeText.setText(calculateAccountAge(createdAtDate));
            } else {
                accountAgeText.setText("1 Day");
            }
        }

        // Setup Edit Profile button listener
        TextView btnEditProfile = findViewById(R.id.btnEditProfile);
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        }

        // Setup Saved Posts button listener
        View savedPostsButton = findViewById(R.id.savedPostsButton);
        if (savedPostsButton != null) {
            savedPostsButton.setOnClickListener(v ->
                    startActivity(new Intent(ProfileActivity.this, SavedPostsActivity.class)));
        }

        // Setup Log Out button listener
        androidx.appcompat.widget.AppCompatButton logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog(prefs));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AvatarHelper.loadHeaderAvatar(this); // Load user avatar into top header
        fetchUserPosts(); // Refresh user posts
    }

    /**
     * Fetches user's own created posts from backend database.
     */
    private void fetchUserPosts() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<PostModel>> call = apiService.getPosts(null, userId, null);

        call.enqueue(new Callback<List<PostModel>>() {
            @Override
            public void onResponse(Call<List<PostModel>> call, Response<List<PostModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateProfilePosts(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<PostModel>> call, Throwable t) {}
        });
    }

    /**
     * Renders post item views into profile posts container.
     */
    private void populateProfilePosts(List<PostModel> posts) {
        LinearLayout container = findViewById(R.id.profilePostsContainer);
        if (container == null) return;
        container.removeAllViews();

        if (posts == null || posts.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("You haven't created any posts yet.");
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(32, 48, 32, 48);
            emptyView.setTextColor(Color.parseColor("#888888"));
            container.addView(emptyView);
        } else {
            for (PostModel post : posts) {
                View postView = getLayoutInflater().inflate(R.layout.item_post, container, false);
                TextView author = postView.findViewById(R.id.postAuthorName);
                TextView content = postView.findViewById(R.id.postContentText);
                TextView approvals = postView.findViewById(R.id.postApprovals);
                TextView dislikes = postView.findViewById(R.id.postDislikes);
                TextView commentsCount = postView.findViewById(R.id.postCommentsCount);
                TextView saveButton = postView.findViewById(R.id.postSaveButton);
                TextView optionsBtn = postView.findViewById(R.id.postOptionsBtn);
                android.widget.ImageView postAvatarImg = postView.findViewById(R.id.postAvatarImage);
                TextView postAvatarTxt = postView.findViewById(R.id.postAvatarText);

                java.io.File userLocalFile = new java.io.File(getFilesDir(), "profile_user_" + post.getUserId() + ".jpg");

                String avatarTarget = "";
                if (post.getProfilePicUrl() != null && !post.getProfilePicUrl().isEmpty()) {
                    avatarTarget = post.getProfilePicUrl();
                } else if (userLocalFile.exists()) {
                    avatarTarget = "file://" + userLocalFile.getAbsolutePath();
                }

                AvatarHelper.loadAvatar(this, avatarTarget, postAvatarImg, postAvatarTxt);

                TextView attachment = postView.findViewById(R.id.postAttachment);
                bindAttachment(attachment, post.getFileUrl());
                View imagePlaceholder = postView.findViewById(R.id.postImagePlaceholder);
                if (imagePlaceholder != null) {
                    imagePlaceholder.setVisibility(isImageAttachment(post.getFileUrl()) ? View.VISIBLE : View.GONE);
                    if (isImageAttachment(post.getFileUrl())) {
                        PostAttachmentHelper.loadImage(this, post.getFileUrl(), (android.widget.ImageView) imagePlaceholder);
                    }
                }

                if (optionsBtn != null) {
                    optionsBtn.setTag(post);
                }

                if (author != null) {
                    author.setText(getDisplayedAuthor(post));
                }
                if (content != null) {
                    String fullText = post.getTitle() != null ? post.getTitle() : "";
                    if (post.getContent() != null && !post.getContent().isEmpty()) {
                        if (!fullText.isEmpty()) fullText += "\n\n";
                        fullText += post.getContent();
                    }
                    content.setText(fullText);
                }

                if (approvals != null) {
                    approvals.setText("👍 " + post.getApprovals() + " Likes");
                    final boolean[] isApproved = {false};
                    final int[] currentApprovals = {post.getApprovals()};
                    approvals.setOnClickListener(v -> {
                        if (!isApproved[0]) {
                            isApproved[0] = true;
                            currentApprovals[0]++;
                            approvals.setText("👍 " + currentApprovals[0] + " Likes");
                            Toast.makeText(this, "Post approved!", Toast.LENGTH_SHORT).show();
                        } else {
                            isApproved[0] = false;
                            currentApprovals[0]--;
                            approvals.setText("👍 " + currentApprovals[0] + " Likes");
                            Toast.makeText(this, "Approval removed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                if (dislikes != null) {
                    dislikes.setText("👎 " + post.getShares() + " Dislikes");
                    final boolean[] isDisliked = {false};
                    final int[] currentDislikes = {post.getShares()};
                    dislikes.setOnClickListener(v -> {
                        if (!isDisliked[0]) {
                            isDisliked[0] = true;
                            currentDislikes[0]++;
                            dislikes.setText("👎 " + currentDislikes[0] + " Dislikes");
                            Toast.makeText(this, "Post disliked", Toast.LENGTH_SHORT).show();
                        } else {
                            isDisliked[0] = false;
                            currentDislikes[0]--;
                            dislikes.setText("👎 " + currentDislikes[0] + " Dislikes");
                            Toast.makeText(this, "Dislike removed", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                if (commentsCount != null) {
                    commentsCount.setText("💬 " + post.getCommentsCount());
                    commentsCount.setOnClickListener(v -> {
                        Intent intent = new Intent(ProfileActivity.this, PostDetailsActivity.class);
                        intent.putExtra("POST_ID", post.getId());
                        intent.putExtra("POST_USER_ID", post.getUserId());
                        intent.putExtra("POST_AUTHOR", getDisplayedAuthor(post));
                        intent.putExtra("POST_IS_ANONYMOUS", post.getIsAnonymous());
                        intent.putExtra("POST_TITLE", post.getTitle() != null ? post.getTitle() : "");
                        intent.putExtra("POST_CONTENT", post.getContent() != null ? post.getContent() : "");
                        intent.putExtra("POST_DATE", post.getCreatedAt() != null ? post.getCreatedAt() : "");
                        intent.putExtra("POST_APPROVALS", post.getApprovals());
                        intent.putExtra("POST_SHARES", post.getShares());
                        intent.putExtra("POST_FILE_URL", post.getFileUrl());
                        startActivity(intent);
                    });
                }
                if (saveButton != null) {
                    int savedUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
                    if (SavedPostsStore.contains(this, post.getId(), savedUserId)) {
                        saveButton.setText("🔖 Saved");
                    }
                    saveButton.setOnClickListener(v -> {
                        int currentUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
                        boolean saved = SavedPostsStore.contains(this, post.getId(), currentUserId);
                        if (saved) {
                            SavedPostsStore.remove(this, post.getId(), currentUserId);
                            saveButton.setText("🔖 Save");
                            Toast.makeText(this, "Post unsaved.", Toast.LENGTH_SHORT).show();
                        } else {
                            SavedPostsStore.add(this, post, currentUserId);
                            saveButton.setText("🔖 Saved");
                            Toast.makeText(this, "Post saved.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                postView.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, PostDetailsActivity.class);
                    intent.putExtra("POST_ID", post.getId());
                    intent.putExtra("POST_USER_ID", post.getUserId());
                    intent.putExtra("POST_AUTHOR", getDisplayedAuthor(post));
                    intent.putExtra("POST_IS_ANONYMOUS", post.getIsAnonymous());
                    intent.putExtra("POST_TITLE", post.getTitle() != null ? post.getTitle() : "");
                    intent.putExtra("POST_CONTENT", post.getContent() != null ? post.getContent() : "");
                    intent.putExtra("POST_DATE", post.getCreatedAt() != null ? post.getCreatedAt() : "");
                    intent.putExtra("POST_APPROVALS", post.getApprovals());
                    intent.putExtra("POST_SHARES", post.getShares());
                    intent.putExtra("POST_FILE_URL", post.getFileUrl());
                    startActivity(intent);
                });

                container.addView(postView);
            }
        }

    }

    private void bindAttachment(TextView attachment, String fileUrl) {
        if (attachment == null || fileUrl == null || fileUrl.isEmpty()) return;
        String fileName = android.net.Uri.parse(fileUrl).getLastPathSegment();
        attachment.setText("📎 Download attachment" + (fileName == null || fileName.isEmpty() ? "" : ": " + fileName));
        attachment.setVisibility(View.VISIBLE);
        attachment.setOnClickListener(v -> showAttachmentOptions(fileUrl, fileName));
    }

    private boolean isImageAttachment(String fileUrl) {
        if (fileUrl == null) return false;
        String path = fileUrl.toLowerCase(java.util.Locale.US);
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) path = path.substring(0, queryIndex);
        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png");
    }

    private String getDisplayedAuthor(PostModel post) {
        return post.getAuthorName() != null && !post.getAuthorName().isEmpty()
                ? post.getAuthorName() : "Student";
    }

    private void showAttachmentOptions(String fileUrl, String fileName) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_attachment_options, null);
        dialogView.findViewById(R.id.optionViewAttachment).setOnClickListener(v -> {
            dialog.dismiss();
            viewAttachment(fileUrl);
        });
        dialogView.findViewById(R.id.optionDownloadAttachment).setOnClickListener(v -> {
            dialog.dismiss();
            downloadAttachment(fileUrl, fileName);
        });
        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void viewAttachment(String fileUrl) {
        try {
            android.net.Uri uri = android.net.Uri.parse(fileUrl);
            String lower = fileUrl.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
                android.app.Dialog imgDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                android.widget.ImageView imgView = new android.widget.ImageView(this);
                imgView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                imgView.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                imgView.setBackgroundColor(Color.BLACK);

                new Thread(() -> {
                    try {
                        java.net.URL url = new java.net.URL(fileUrl);
                        android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(url.openConnection().getInputStream());
                        if (bmp != null) {
                            runOnUiThread(() -> imgView.setImageBitmap(bmp));
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                }).start();

                imgView.setOnClickListener(v -> imgDialog.dismiss());
                imgDialog.setContentView(imgView);
                imgDialog.show();
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to view attachment.", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadAttachment(String fileUrl, String fileName) {
        try {
            android.app.DownloadManager.Request request =
                    new android.app.DownloadManager.Request(android.net.Uri.parse(fileUrl));
            request.setTitle(fileName);
            request.setDescription("Downloading attachment");
            request.setNotificationVisibility(
                    android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
            android.app.DownloadManager manager =
                    (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(this, "Download started.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to download attachment.", Toast.LENGTH_SHORT).show();
        }
    }

    public void openPostDetails(View view) {
        Intent intent = new Intent(ProfileActivity.this, PostDetailsActivity.class);
        startActivity(intent);
    }

    public void showPostOptions(View view) {
        PostModel post = (PostModel) view.getTag();
        int postUserId = post != null ? post.getUserId() : 0;

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", 0);

        boolean isOwner = true;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_options, null);

        View optionReport = dialogView.findViewById(R.id.optionReportPost);
        View optionEdit = dialogView.findViewById(R.id.optionEditPost);
        View optionDelete = dialogView.findViewById(R.id.optionDeletePost);

        if (isOwner) {
            if (optionReport != null) optionReport.setVisibility(View.GONE);
            if (optionEdit != null) optionEdit.setVisibility(View.VISIBLE);
            if (optionDelete != null) optionDelete.setVisibility(View.VISIBLE);

            if (optionEdit != null) {
                optionEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (post != null) {
                        Intent intent = new Intent(ProfileActivity.this, EditPostActivity.class);
                        intent.putExtra("POST_ID", post.getId());
                        intent.putExtra("POST_USER_ID", post.getUserId());
                        intent.putExtra("POST_TITLE", post.getTitle());
                        intent.putExtra("POST_CATEGORY", post.getCategory());
                        intent.putExtra("POST_CONTENT", post.getContent());
                        intent.putExtra("POST_IS_ANONYMOUS", post.getIsAnonymous());
                        startActivity(intent);
                    }
                });
            }

            if (optionDelete != null) {
                optionDelete.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (post != null) {
                        deletePost(post.getId(), currentUserId);
                    }
                });
            }
        } else {
            if (optionReport != null) optionReport.setVisibility(View.VISIBLE);
            if (optionEdit != null) optionEdit.setVisibility(View.GONE);
            if (optionDelete != null) optionDelete.setVisibility(View.GONE);

            if (optionReport != null) {
                optionReport.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(ProfileActivity.this, ReportPostActivity.class);
                    if (post != null) {
                        intent.putExtra("POST_ID", post.getId());
                    }
                    startActivity(intent);
                });
            }
        }

        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void deletePost(int postId, int userId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.deletePost(postId, userId);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(ProfileActivity.this, "Post deleted successfully.", Toast.LENGTH_SHORT).show();
                    fetchUserPosts();
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to delete post.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showEditProfileDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_profile_options, null);

        View optionUsername = dialogView.findViewById(R.id.optionChangeUsername);
        View optionPassword = dialogView.findViewById(R.id.optionChangePassword);
        View optionPicture = dialogView.findViewById(R.id.optionChangePicture);
        View optionPreferredCollege = dialogView.findViewById(R.id.optionPreferredCollege);

        if (optionUsername != null) {
            optionUsername.setOnClickListener(v -> {
                dialog.dismiss();
                showChangeUsernameDialog();
            });
        }

        if (optionPassword != null) {
            optionPassword.setOnClickListener(v -> {
                dialog.dismiss();
                showChangePasswordDialog();
            });
        }

        if (optionPicture != null) {
            optionPicture.setOnClickListener(v -> {
                dialog.dismiss();
                openImagePicker();
            });
        }

        if (optionPreferredCollege != null) {
            optionPreferredCollege.setOnClickListener(v -> {
                dialog.dismiss();
                showPreferredCollegeDialog();
            });
        }

        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void showPreferredCollegeDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_preferred_college, null);
        dialog.setContentView(dialogView);

        Spinner spinner = dialogView.findViewById(R.id.preferredCollegeSpinner);
        android.widget.ArrayAdapter<CharSequence> adapter =
                android.widget.ArrayAdapter.createFromResource(this, R.array.college_choices,
                        R.layout.item_college_spinner);
        adapter.setDropDownViewResource(R.layout.item_college_spinner_dropdown);
        spinner.setAdapter(adapter);
        spinner.setPopupBackgroundDrawable(
                getDrawable(R.drawable.college_spinner_popup_bg));

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);
        String preferred = prefs.getString("PREFERRED_COLLEGE_" + userId, "");
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString().equals(preferred)) {
                spinner.setSelection(i);
                break;
            }
        }

        View save = dialogView.findViewById(R.id.savePreferredCollegeButton);
        save.setOnClickListener(v -> {
            String college = spinner.getSelectedItem().toString();
            prefs.edit().putString("PREFERRED_COLLEGE_" + userId, college).apply();
            Toast.makeText(this, "Preferred college saved.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private static final int PICK_IMAGE_REQUEST = 101;

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String[] mimetypes = {"image/jpeg", "image/png", "image/jpg", "image/heif"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            android.net.Uri uri = data.getData();
            String path = uri.toString();
            String mimeType = getContentResolver().getType(uri);

            boolean isValid = false;
            if (path.toLowerCase().matches(".*\\.(jpg|jpeg|png|heif)$")) {
                isValid = true;
            } else if (mimeType != null && (mimeType.contains("jpeg") || mimeType.contains("png") || mimeType.contains("jpg") || mimeType.contains("heif"))) {
                isValid = true;
            }

            if (isValid) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                int userId = prefs.getInt("USER_ID", 1);
                String localSavedPath = uri.toString();

                try {
                    java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
                    java.io.File localFile = new java.io.File(getFilesDir(), "profile_user_" + userId + ".jpg");
                    java.io.FileOutputStream outputStream = new java.io.FileOutputStream(localFile);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    outputStream.close();
                    inputStream.close();
                    localSavedPath = "file://" + localFile.getAbsolutePath();
                } catch (Exception e) {
                    try {
                        getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (Exception ex) {
                        // ignore
                    }
                }

                prefs.edit().putString("USER_PROFILE_PIC_URI_" + userId, localSavedPath).apply();

                android.widget.ImageView avatarImg = findViewById(R.id.profileAvatarImage);
                TextView avatarTxt = findViewById(R.id.profileAvatarText);
                AvatarHelper.loadAvatar(this, localSavedPath, avatarImg, avatarTxt);

                new Thread(() -> {
                    try {
                        android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        android.graphics.Bitmap scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 300, 300, true);
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos);
                        byte[] imageBytes = baos.toByteArray();
                        String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

                        ApiService apiService = ApiClient.getClient().create(ApiService.class);
                        Call<GenericResponse> call = apiService.uploadProfilePic(userId, base64Image);
                        call.enqueue(new Callback<GenericResponse>() {
                            @Override
                            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Profile picture saved to server!", Toast.LENGTH_SHORT).show());
                                }
                            }

                            @Override
                            public void onFailure(Call<GenericResponse> call, Throwable t) {
                                // Ignore network error
                            }
                        });
                    } catch (Exception e) {
                        // Ignore image compression error
                    }
                }).start();

                Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please select a valid image (.jpg, .png, .jpeg, .heif)", Toast.LENGTH_LONG).show();
            }
        }
    }

    private String calculateAccountAge(java.util.Date createdDate) {
        if (createdDate == null) return "1 Day";

        long now = System.currentTimeMillis();
        long diffInMillis = Math.max(0, now - createdDate.getTime());
        long diffInDays = diffInMillis / (1000L * 60 * 60 * 24);

        if (diffInDays < 1) {
            long diffInHours = diffInMillis / (1000L * 60 * 60);
            if (diffInHours <= 1) return "1 Hour";
            return diffInHours + " Hours";
        } else if (diffInDays < 30) {
            if (diffInDays == 1) return "1 Day";
            return diffInDays + " Days";
        } else if (diffInDays < 365) {
            long months = diffInDays / 30;
            if (months <= 1) return "1 Month";
            return months + " Months";
        } else {
            long years = diffInDays / 365;
            if (years <= 1) return "1 Year";
            return years + " Years";
        }
    }

    private void showChangeUsernameDialog() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);
        String currentUsername = prefs.getString("USER_USERNAME", "");

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_change_username);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText input = dialog.findViewById(R.id.dialogUsernameInput);
        if (input != null && currentUsername != null && !currentUsername.isEmpty()) {
            input.setText(currentUsername);
            input.setSelection(currentUsername.length());
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelUsername);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSave = dialog.findViewById(R.id.btnSaveUsername);
        if (btnSave != null && input != null) {
            btnSave.setOnClickListener(v -> {
                String newUsername = input.getText().toString().trim();
                if (newUsername.isEmpty()) {
                    Toast.makeText(this, "Username cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.updateProfile(userId, newUsername, null, null);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Username updated successfully!", Toast.LENGTH_SHORT).show();
                            prefs.edit().putString("USER_USERNAME", newUsername).apply();
                            TextView profileHandleText = findViewById(R.id.profileHandleText);
                            if (profileHandleText != null) {
                                profileHandleText.setText(newUsername.startsWith("@") ? newUsername : "@" + newUsername);
                            }
                            fetchUserPosts();
                        } else {
                            String msg = (response.body() != null) ? response.body().getMessage() : "Failed to update username.";
                            Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void showChangePasswordDialog() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_change_password);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText oldInput = dialog.findViewById(R.id.dialogOldPasswordInput);
        EditText newInput = dialog.findViewById(R.id.dialogNewPasswordInput);
        EditText confirmInput = dialog.findViewById(R.id.dialogConfirmPasswordInput);
        android.widget.CheckBox chkShowPassword = dialog.findViewById(R.id.chkShowPassword);

        if (chkShowPassword != null) {
            chkShowPassword.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int inputType = isChecked ?
                    (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) :
                    (android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

                if (oldInput != null) {
                    oldInput.setInputType(inputType);
                    oldInput.setSelection(oldInput.getText().length());
                }
                if (newInput != null) {
                    newInput.setInputType(inputType);
                    newInput.setSelection(newInput.getText().length());
                }
                if (confirmInput != null) {
                    confirmInput.setInputType(inputType);
                    confirmInput.setSelection(confirmInput.getText().length());
                }
            });
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelPassword);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSave = dialog.findViewById(R.id.btnSavePassword);
        if (btnSave != null && oldInput != null && newInput != null && confirmInput != null) {
            btnSave.setOnClickListener(v -> {
                String oldPass = oldInput.getText().toString().trim();
                String newPass = newInput.getText().toString().trim();
                String confirmPass = confirmInput.getText().toString().trim();

                if (oldPass.isEmpty()) {
                    Toast.makeText(this, "Please enter your current password.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPass.isEmpty()) {
                    Toast.makeText(this, "Please enter a new password.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidStrongPassword(newPass)) {
                    Toast.makeText(this, "Password must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!newPass.equals(confirmPass)) {
                    Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                    return;
                }

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.updateProfile(userId, null, oldPass, newPass);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            Toast.makeText(ProfileActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            String msg = (response.body() != null) ? response.body().getMessage() : "Failed to update password.";
                            Toast.makeText(ProfileActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(ProfileActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void showLogoutConfirmationDialog(SharedPreferences prefs) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_logout_confirmation);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelLogout);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnConfirm = dialog.findViewById(R.id.btnConfirmLogout);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                String sessionToken = prefs.getString("USER_SESSION_TOKEN", "");
                if (!sessionToken.isEmpty()) {
                    prefs.edit().putString("PENDING_LOGOUT_SESSION_TOKEN", sessionToken).apply();
                    ApiClient.getClient().create(ApiService.class)
                            .mobileSessionLogout(sessionToken)
                            .enqueue(new Callback<GenericResponse>() {
                                @Override
                                public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                                    if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                                        android.util.Log.e("ProfileActivity", "Unable to record mobile logout (" + response.code() + ").");
                                    } else {
                                        prefs.edit().remove("PENDING_LOGOUT_SESSION_TOKEN").apply();
                                    }
                                    finishLogout(prefs);
                                }

                                @Override
                                public void onFailure(Call<GenericResponse> call, Throwable error) {
                                    android.util.Log.e("ProfileActivity", "Unable to record mobile logout.", error);
                                    finishLogout(prefs);
                                }
                            });
                    return;
                }
                finishLogout(prefs);
            });
        }

        dialog.show();
    }

    private void finishLogout(SharedPreferences prefs) {
        String pendingLogoutToken = prefs.getString("PENDING_LOGOUT_SESSION_TOKEN", "");
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        if (!pendingLogoutToken.isEmpty()) {
            editor.putString("PENDING_LOGOUT_SESSION_TOKEN", pendingLogoutToken);
        }
        editor.apply();

        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private boolean isValidStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }
}