package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.CommentModel;
import com.example.campusconnect.models.GenericResponse;
import com.example.campusconnect.models.PostModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PostDetailsActivity displays detailed view of a selected post,
 * including full content, attached files/photos, comments list, and comment creation box.
 */
public class PostDetailsActivity extends AppCompatActivity {

    private int postId = 0;
    private int postUserId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details); // Bind layout

        postId = getIntent().getIntExtra("POST_ID", 0);
        postUserId = getIntent().getIntExtra("POST_USER_ID", 0);

        // Header profile button
        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(PostDetailsActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        AvatarHelper.loadHeaderAvatar(this); // Load user avatar into top header

        int currentUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
        int postIsAnonymous = getIntent().getIntExtra("POST_IS_ANONYMOUS", 0);

        // Load post author avatar
        File userLocalFile = new File(getFilesDir(), "profile_user_" + postUserId + ".jpg");
        String avatarTarget = "";
        if (postIsAnonymous == 0 && userLocalFile.exists()) {
            avatarTarget = "file://" + userLocalFile.getAbsolutePath();
        } else if (postIsAnonymous == 0) {
            avatarTarget = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("USER_PROFILE_PIC_URI_" + postUserId, "");
        }
        AvatarHelper.loadAvatar(this, avatarTarget, findViewById(R.id.detailAvatarImage), findViewById(R.id.detailAvatarText));

        // Load logged-in user avatar for comment input box
        String myPicUri = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("USER_PROFILE_PIC_URI_" + currentUserId, "");
        AvatarHelper.loadAvatar(this, myPicUri, findViewById(R.id.commentBoxAvatarImage), findViewById(R.id.commentBoxAvatarText));

        // Retrieve post details passed via Intent
        String author = getIntent().getStringExtra("POST_AUTHOR");
        String title = getIntent().getStringExtra("POST_TITLE");
        String content = getIntent().getStringExtra("POST_CONTENT");
        String date = getIntent().getStringExtra("POST_DATE");
        int approvals = getIntent().getIntExtra("POST_APPROVALS", 67);
        int shares = getIntent().getIntExtra("POST_SHARES", 69);
        String fileUrl = getIntent().getStringExtra("POST_FILE_URL");

        TextView authorNameText = findViewById(R.id.detailAuthorName);
        TextView timestampText = findViewById(R.id.detailTimestamp);
        TextView titleText = findViewById(R.id.detailTitle);
        TextView contentText = findViewById(R.id.detailContent);
        TextView approvalsText = findViewById(R.id.detailApprovals);
        TextView dislikesText = findViewById(R.id.detailDislikes);

        if (authorNameText != null && author != null && !author.isEmpty()) authorNameText.setText(author);
        if (timestampText != null && date != null && !date.isEmpty()) timestampText.setText(date);
        if (titleText != null && title != null && !title.isEmpty()) titleText.setText(title);
        if (contentText != null && content != null && !content.isEmpty()) contentText.setText(content);

        // Save post button toggle listener
        TextView savePostButton = findViewById(R.id.savePostButton);
        if (savePostButton != null) {
            if (SavedPostsStore.contains(this, postId, currentUserId)) {
                savePostButton.setText("🔖 Saved");
            }
            savePostButton.setOnClickListener(v -> {
                if (SavedPostsStore.contains(this, postId, currentUserId)) {
                    SavedPostsStore.remove(this, postId, currentUserId);
                    savePostButton.setText("🔖 Save");
                    Toast.makeText(this, "Post unsaved.", Toast.LENGTH_SHORT).show();
                } else {
                    PostModel post = new PostModel();
                    post.setId(postId);
                    post.setUserId(postUserId);
                    post.setTitle(title);
                    post.setContent(content);
                    post.setCreatedAt(date);
                    post.setApprovals(approvals);
                    post.setShares(shares);
                    post.setIsAnonymous(postIsAnonymous);
                    post.setAuthorName(author);
                    post.setFileUrl(fileUrl);
                    SavedPostsStore.add(this, post, currentUserId);
                    savePostButton.setText("🔖 Saved");
                    Toast.makeText(this, "Post saved.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Bind attached image/file if available
        TextView attachment = findViewById(R.id.detailAttachment);
        View imagePlaceholder = findViewById(R.id.detailImageContainer);
        if (imagePlaceholder != null) {
            imagePlaceholder.setVisibility(isImageAttachment(fileUrl) ? View.VISIBLE : View.GONE);
            if (isImageAttachment(fileUrl)) {
                PostAttachmentHelper.loadImage(this, fileUrl, findViewById(R.id.detailImagePlaceholder));
            }
        }
        if (attachment != null && fileUrl != null && !fileUrl.isEmpty()) {
            String fileName = android.net.Uri.parse(fileUrl).getLastPathSegment();
            attachment.setText("📎 Download attachment" + (fileName == null || fileName.isEmpty() ? "" : ": " + fileName));
            attachment.setVisibility(View.VISIBLE);
            attachment.setOnClickListener(v -> showAttachmentOptions(fileUrl, fileName));
        }

        // Like button toggle listener
        if (approvalsText != null) {
            approvalsText.setText("👍 " + approvals + " Likes");
            final boolean[] isApproved = {false};
            final int[] currentApprovals = {approvals};
            approvalsText.setOnClickListener(v -> {
                if (!isApproved[0]) {
                    isApproved[0] = true;
                    currentApprovals[0]++;
                    approvalsText.setText("👍 " + currentApprovals[0] + " Likes");
                    Toast.makeText(this, "Post approved!", Toast.LENGTH_SHORT).show();
                } else {
                    isApproved[0] = false;
                    currentApprovals[0]--;
                    approvalsText.setText("👍 " + currentApprovals[0] + " Likes");
                    Toast.makeText(this, "Approval removed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Dislike button toggle listener
        if (dislikesText != null) {
            dislikesText.setText("👎 " + shares + " Dislikes");
            final boolean[] isDisliked = {false};
            final int[] currentDislikes = {shares};
            dislikesText.setOnClickListener(v -> {
                if (!isDisliked[0]) {
                    isDisliked[0] = true;
                    currentDislikes[0]++;
                    dislikesText.setText("👎 " + currentDislikes[0] + " Dislikes");
                    Toast.makeText(this, "Post disliked", Toast.LENGTH_SHORT).show();
                } else {
                    isDisliked[0] = false;
                    currentDislikes[0]--;
                    dislikesText.setText("👎 " + currentDislikes[0] + " Dislikes");
                    Toast.makeText(this, "Dislike removed", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Send Comment button listener
        EditText commentInput = findViewById(R.id.commentInput);
        android.widget.ScrollView detailScrollView = findViewById(R.id.detailScrollView);
        if (commentInput != null && detailScrollView != null) {
            commentInput.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    detailScrollView.postDelayed(() -> detailScrollView.fullScroll(View.FOCUS_DOWN), 200);
                }
            });
            commentInput.setOnClickListener(v ->
                    detailScrollView.postDelayed(() -> detailScrollView.fullScroll(View.FOCUS_DOWN), 200));
        }

        View btnSend = findViewById(R.id.btnSendComment);
        if (btnSend != null && commentInput != null) {
            btnSend.setOnClickListener(v -> {
                String text = commentInput.getText().toString().trim();
                if (text.isEmpty()) {
                    Toast.makeText(this, "Please enter a comment.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (postId <= 0) {
                    Toast.makeText(this, "Error: Invalid post ID.", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                int userId = prefs.getInt("USER_ID", 1);

                // Call backend API to post comment
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.addComment(postId, userId, text);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            commentInput.setText("");
                            Toast.makeText(PostDetailsActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                            fetchComments(); // Refresh comments
                        } else {
                            Toast.makeText(PostDetailsActivity.this, "Failed to post comment.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(PostDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            });
        }

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (postId > 0) {
                    fetchComments();
                }
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    /**
     * Shows attachment options bottom sheet (View vs Download).
     */
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

    private boolean isImageAttachment(String fileUrl) {
        if (fileUrl == null) return false;
        String path = fileUrl.toLowerCase(java.util.Locale.US);
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) path = path.substring(0, queryIndex);
        return path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png");
    }

    /**
     * Opens in-app photo viewer or system document viewer for attachments.
     */
    private void viewAttachment(String fileUrl) {
        try {
            android.net.Uri uri = android.net.Uri.parse(fileUrl);
            String lower = fileUrl.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
                android.app.Dialog imgDialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
                ImageView imgView = new ImageView(this);
                imgView.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT));
                imgView.setScaleType(ImageView.ScaleType.FIT_CENTER);
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

    /**
     * Enqueues download request with Android DownloadManager.
     */
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

    @Override
    protected void onResume() {
        super.onResume();
        if (postId > 0) {
            fetchComments(); // Reload comments
        }
    }

    /**
     * Fetches post comments from backend database.
     */
    private void fetchComments() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<CommentModel>> call = apiService.getComments(postId);
        call.enqueue(new Callback<List<CommentModel>>() {
            @Override
            public void onResponse(Call<List<CommentModel>> call, Response<List<CommentModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateComments(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<CommentModel>> call, Throwable t) {}
        });
    }

    /**
     * Renders comment list into comments container.
     */
    private void populateComments(List<CommentModel> comments) {
        LinearLayout container = findViewById(R.id.commentsContainer);
        TextView titleText = findViewById(R.id.commentsTitleText);
        TextView commentsCountHeader = findViewById(R.id.detailCommentsCount);

        if (container == null) return;
        container.removeAllViews();

        int count = comments != null ? comments.size() : 0;
        if (titleText != null) titleText.setText("💬 Comments (" + count + ")");
        if (commentsCountHeader != null) commentsCountHeader.setText("💬 " + count + " comments");

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", 0);
        String currentFullName = prefs.getString("USER_FULL_NAME", "");

        if (comments == null || comments.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No comments yet. Be the first to comment!");
            emptyText.setTextColor(Color.parseColor("#888888"));
            emptyText.setTextSize(12f);
            emptyText.setGravity(android.view.Gravity.CENTER);
            emptyText.setPadding(32, 24, 32, 24);
            container.addView(emptyText);
        } else {
            for (CommentModel comment : comments) {
                View commentView = getLayoutInflater().inflate(R.layout.item_comment, container, false);
                TextView author = commentView.findViewById(R.id.commentAuthor);
                TextView timestamp = commentView.findViewById(R.id.commentTimestamp);
                TextView content = commentView.findViewById(R.id.commentContent);
                TextView optionsBtn = commentView.findViewById(R.id.commentOptionsBtn);
                ImageView commentAvatarImage = commentView.findViewById(R.id.commentAvatarImage);
                TextView commentAvatarText = commentView.findViewById(R.id.commentAvatarText);

                if (author != null) author.setText(comment.getAuthorName() != null && !comment.getAuthorName().isEmpty() ? comment.getAuthorName() : "Student");
                if (timestamp != null) timestamp.setText(comment.getCreatedAt() != null ? comment.getCreatedAt() : "");
                if (content != null) content.setText(comment.getContent());

                String avatarTarget = comment.getProfilePicUrl();
                File commentUserFile = new File(getFilesDir(), "profile_user_" + comment.getUserId() + ".jpg");
                if (commentUserFile.exists()) {
                    avatarTarget = "file://" + commentUserFile.getAbsolutePath();
                }
                if (avatarTarget == null || avatarTarget.isEmpty()) {
                    avatarTarget = prefs.getString("USER_PROFILE_PIC_URI_" + comment.getUserId(), "");
                }
                AvatarHelper.loadAvatar(this, avatarTarget, commentAvatarImage, commentAvatarText);

                boolean isCommentOwner = false;
                if (currentUserId > 0 && comment.getUserId() == currentUserId) {
                    isCommentOwner = true;
                } else if (currentUserId == 0 && comment.getUserId() == 1) {
                    isCommentOwner = true;
                } else if (comment.getAuthorName() != null && currentFullName != null &&
                           !currentFullName.isEmpty() && comment.getAuthorName().equalsIgnoreCase(currentFullName)) {
                    isCommentOwner = true;
                }

                if (optionsBtn != null) {
                    optionsBtn.setVisibility(View.VISIBLE);
                    optionsBtn.setOnClickListener(v -> showCommentOptions(comment, currentUserId));
                }

                container.addView(commentView);
            }
        }
    }

    /**
     * Shows comment options bottom sheet (Edit/Delete for author, Report for non-authors).
     */
    private void showCommentOptions(CommentModel comment, int currentUserId) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String currentFullName = prefs.getString("USER_FULL_NAME", "");

        boolean isCommentOwner = false;
        if (currentUserId > 0 && comment.getUserId() == currentUserId) {
            isCommentOwner = true;
        } else if (currentUserId == 0 && comment.getUserId() == 1) {
            isCommentOwner = true;
        } else if (comment.getAuthorName() != null && currentFullName != null &&
                   !currentFullName.isEmpty() && comment.getAuthorName().equalsIgnoreCase(currentFullName)) {
            isCommentOwner = true;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_comment_options, null);

        View optionReport = dialogView.findViewById(R.id.optionReportComment);
        View optionEdit = dialogView.findViewById(R.id.optionEditComment);
        View optionDelete = dialogView.findViewById(R.id.optionDeleteComment);

        if (isCommentOwner) {
            if (optionReport != null) optionReport.setVisibility(View.GONE);
            if (optionEdit != null) optionEdit.setVisibility(View.VISIBLE);
            if (optionDelete != null) optionDelete.setVisibility(View.VISIBLE);

            if (optionEdit != null) {
                optionEdit.setOnClickListener(v -> {
                    dialog.dismiss();
                    showEditCommentDialog(comment, currentUserId);
                });
            }

            if (optionDelete != null) {
                optionDelete.setOnClickListener(v -> {
                    dialog.dismiss();
                    deleteComment(comment.getId(), currentUserId);
                });
            }
        } else {
            if (optionReport != null) optionReport.setVisibility(View.VISIBLE);
            if (optionEdit != null) optionEdit.setVisibility(View.GONE);
            if (optionDelete != null) optionDelete.setVisibility(View.GONE);

            if (optionReport != null) {
                optionReport.setOnClickListener(v -> {
                    dialog.dismiss();
                    showReportCommentDialog(comment, currentUserId);
                });
            }
        }

        dialog.setContentView(dialogView);
        dialog.show();
    }

    private void showEditCommentDialog(CommentModel comment, int currentUserId) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_comment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText input = dialog.findViewById(R.id.dialogEditCommentInput);
        if (input != null && comment.getContent() != null) {
            input.setText(comment.getContent());
            input.setSelection(comment.getContent().length());
        }

        View btnCancel = dialog.findViewById(R.id.btnCancelEditComment);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSave = dialog.findViewById(R.id.btnSaveEditComment);
        if (btnSave != null && input != null) {
            btnSave.setOnClickListener(v -> {
                String newContent = input.getText().toString().trim();
                if (newContent.isEmpty()) {
                    Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(this, "Updating comment...", Toast.LENGTH_SHORT).show();
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.updateComment(comment.getId(), currentUserId, newContent);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            Toast.makeText(PostDetailsActivity.this, "Comment updated successfully!", Toast.LENGTH_SHORT).show();
                            fetchComments();
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Failed to update comment.";
                            Toast.makeText(PostDetailsActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(PostDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }

    private void showReportCommentDialog(CommentModel comment, int currentUserId) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_report_comment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.RadioGroup reasonGroup = dialog.findViewById(R.id.dialogReportReasonGroup);

        View btnCancel = dialog.findViewById(R.id.btnCancelReportComment);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        View btnSubmit = dialog.findViewById(R.id.btnSubmitReportComment);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String selectedReason = "Inappropriate / Offensive";
                if (reasonGroup != null) {
                    int checkedId = reasonGroup.getCheckedRadioButtonId();
                    android.widget.RadioButton radio = dialog.findViewById(checkedId);
                    if (radio != null) {
                        selectedReason = radio.getText().toString();
                    }
                }

                Toast.makeText(this, "Submitting comment report...", Toast.LENGTH_SHORT).show();
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.submitCommentReport(comment.getId(), currentUserId, selectedReason, "");
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            Toast.makeText(PostDetailsActivity.this, "Comment report submitted.", Toast.LENGTH_SHORT).show();
                        } else {
                            dialog.dismiss();
                            Toast.makeText(PostDetailsActivity.this, "Comment report submitted.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        dialog.dismiss();
                        Toast.makeText(PostDetailsActivity.this, "Comment report submitted.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }

    /**
     * Sends comment deletion request to backend API.
     */
    private void deleteComment(int commentId, int userId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.deleteComment(commentId, userId);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(PostDetailsActivity.this, "Comment deleted.", Toast.LENGTH_SHORT).show();
                    fetchComments();
                } else {
                    Toast.makeText(PostDetailsActivity.this, "Failed to delete comment.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(PostDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Shows 3-dots popup options for the post.
     */
    public void showPostOptions(View view) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", 0);
        String currentFullName = prefs.getString("USER_FULL_NAME", "");
        String author = getIntent().getStringExtra("POST_AUTHOR");

        boolean isOwner = false;
        if (postUserId > 0 && currentUserId > 0 && postUserId == currentUserId) {
            isOwner = true;
        } else if (currentUserId == 0 && postUserId == 1) {
            isOwner = true;
        } else if (author != null && currentFullName != null &&
                   !currentFullName.isEmpty() && author.equalsIgnoreCase(currentFullName)) {
            isOwner = true;
        }

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
                    Intent intent = new Intent(PostDetailsActivity.this, EditPostActivity.class);
                    intent.putExtra("POST_ID", postId);
                    intent.putExtra("POST_USER_ID", postUserId);
                    intent.putExtra("POST_TITLE", getIntent().getStringExtra("POST_TITLE"));
                    intent.putExtra("POST_CONTENT", getIntent().getStringExtra("POST_CONTENT"));
                    startActivity(intent);
                });
            }

            if (optionDelete != null) {
                optionDelete.setOnClickListener(v -> {
                    dialog.dismiss();
                    deletePost(postId, currentUserId);
                });
            }
        } else {
            if (optionReport != null) optionReport.setVisibility(View.VISIBLE);
            if (optionEdit != null) optionEdit.setVisibility(View.GONE);
            if (optionDelete != null) optionDelete.setVisibility(View.GONE);

            if (optionReport != null) {
                optionReport.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(PostDetailsActivity.this, ReportPostActivity.class);
                    intent.putExtra("POST_ID", postId);
                    startActivity(intent);
                });
            }
        }

        dialog.setContentView(dialogView);
        dialog.show();
    }

    /**
     * Sends post deletion request to backend API.
     */
    private void deletePost(int postId, int userId) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.deletePost(postId, userId);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(PostDetailsActivity.this, "Post deleted successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(PostDetailsActivity.this, "Failed to delete post.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(PostDetailsActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}