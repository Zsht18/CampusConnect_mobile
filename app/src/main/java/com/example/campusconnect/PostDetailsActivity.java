package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.CommentModel;
import com.example.campusconnect.models.GenericResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailsActivity extends AppCompatActivity {

    private int postId = 0;
    private int postUserId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        postId = getIntent().getIntExtra("POST_ID", 0);
        postUserId = getIntent().getIntExtra("POST_USER_ID", 0);

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(PostDetailsActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        // Retrieve post details from Intent
        String author = getIntent().getStringExtra("POST_AUTHOR");
        String title = getIntent().getStringExtra("POST_TITLE");
        String content = getIntent().getStringExtra("POST_CONTENT");
        String date = getIntent().getStringExtra("POST_DATE");
        int approvals = getIntent().getIntExtra("POST_APPROVALS", 67);
        int shares = getIntent().getIntExtra("POST_SHARES", 69);

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
        if (approvalsText != null) {
            approvalsText.setText("👍 " + approvals + " Approvals");
            final boolean[] isApproved = {false};
            final int[] currentApprovals = {approvals};
            approvalsText.setOnClickListener(v -> {
                if (!isApproved[0]) {
                    isApproved[0] = true;
                    currentApprovals[0]++;
                    approvalsText.setText("👍 " + currentApprovals[0] + " Approvals");
                    Toast.makeText(this, "Post approved!", Toast.LENGTH_SHORT).show();
                } else {
                    isApproved[0] = false;
                    currentApprovals[0]--;
                    approvalsText.setText("👍 " + currentApprovals[0] + " Approvals");
                    Toast.makeText(this, "Approval removed", Toast.LENGTH_SHORT).show();
                }
            });
        }

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

        EditText commentInput = findViewById(R.id.commentInput);
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

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.addComment(postId, userId, text);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            commentInput.setText("");
                            Toast.makeText(PostDetailsActivity.this, "Comment posted!", Toast.LENGTH_SHORT).show();
                            fetchComments();
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (postId > 0) {
            fetchComments();
        }
    }

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
            public void onFailure(Call<List<CommentModel>> call, Throwable t) {
                // Ignore
            }
        });
    }

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
            emptyText.setTextColor(android.graphics.Color.parseColor("#888888"));
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

                if (author != null) author.setText(comment.getAuthorName() != null && !comment.getAuthorName().isEmpty() ? comment.getAuthorName() : "Student");
                if (timestamp != null) timestamp.setText(comment.getCreatedAt() != null ? comment.getCreatedAt() : "");
                if (content != null) content.setText(comment.getContent());

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
                    if (isCommentOwner) {
                        optionsBtn.setVisibility(View.VISIBLE);
                        optionsBtn.setOnClickListener(v -> showCommentOptions(comment, currentUserId));
                    } else {
                        optionsBtn.setVisibility(View.GONE);
                    }
                }

                container.addView(commentView);
            }
        }
    }

    private void showCommentOptions(CommentModel comment, int currentUserId) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_comment_options, null);

        View optionEdit = dialogView.findViewById(R.id.optionEditComment);
        View optionDelete = dialogView.findViewById(R.id.optionDeleteComment);

        if (optionEdit != null) {
            optionEdit.setOnClickListener(v -> {
                dialog.dismiss();
                Toast.makeText(this, "Edit Comment feature coming soon!", Toast.LENGTH_SHORT).show();
            });
        }

        if (optionDelete != null) {
            optionDelete.setOnClickListener(v -> {
                dialog.dismiss();
                deleteComment(comment.getId(), currentUserId);
            });
        }

        dialog.setContentView(dialogView);
        dialog.show();
    }

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