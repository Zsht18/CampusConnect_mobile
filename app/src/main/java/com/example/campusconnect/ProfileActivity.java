package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
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

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView profileUsernameText = findViewById(R.id.profileUsernameText);
        TextView profileHandleText = findViewById(R.id.profileHandleText);
        TextView accountAgeText = findViewById(R.id.accountAgeText);
        TextView postsCountText = findViewById(R.id.postsCountText);
        
        // Retrieve the data saved from the LoginActivity
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
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

        String createdAtStr = prefs.getString("USER_CREATED_AT", "");
        if (createdAtStr != null && !createdAtStr.isEmpty() && accountAgeText != null) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date createdAtDate = inputFormat.parse(createdAtStr);
                if (createdAtDate != null) {
                    java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                    accountAgeText.setText(outputFormat.format(createdAtDate));
                } else {
                    accountAgeText.setText(createdAtStr);
                }
            } catch (Exception e) {
                try {
                    java.text.SimpleDateFormat inputFormatDateOnly = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    java.util.Date createdAtDate = inputFormatDateOnly.parse(createdAtStr);
                    if (createdAtDate != null) {
                        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
                        accountAgeText.setText(outputFormat.format(createdAtDate));
                    } else {
                        accountAgeText.setText(createdAtStr);
                    }
                } catch (Exception ex) {
                    accountAgeText.setText(createdAtStr);
                }
            }
        }

        // Setup Log Out button
        androidx.appcompat.widget.AppCompatButton logoutButton = findViewById(R.id.logoutButton);
        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> {
                // Clear saved SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.clear();
                editor.apply();

                // Redirect to LoginActivity and clear activity stack
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchUserPosts();
    }

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
            public void onFailure(Call<List<PostModel>> call, Throwable t) {
                // Ignore
            }
        });
    }

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
                TextView optionsBtn = postView.findViewById(R.id.postOptionsBtn);

                if (optionsBtn != null) {
                    optionsBtn.setTag(post);
                }

                if (author != null) {
                    author.setText(post.getAuthorName() != null && !post.getAuthorName().isEmpty() ? post.getAuthorName() : "Student");
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
                    approvals.setText("👍 " + post.getApprovals() + " Approvals");
                    final boolean[] isApproved = {false};
                    final int[] currentApprovals = {post.getApprovals()};
                    approvals.setOnClickListener(v -> {
                        if (!isApproved[0]) {
                            isApproved[0] = true;
                            currentApprovals[0]++;
                            approvals.setText("👍 " + currentApprovals[0] + " Approvals");
                            Toast.makeText(this, "Post approved!", Toast.LENGTH_SHORT).show();
                        } else {
                            isApproved[0] = false;
                            currentApprovals[0]--;
                            approvals.setText("👍 " + currentApprovals[0] + " Approvals");
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
                        intent.putExtra("POST_AUTHOR", post.getAuthorName() != null && !post.getAuthorName().isEmpty() ? post.getAuthorName() : "Student");
                        intent.putExtra("POST_TITLE", post.getTitle() != null ? post.getTitle() : "");
                        intent.putExtra("POST_CONTENT", post.getContent() != null ? post.getContent() : "");
                        intent.putExtra("POST_DATE", post.getCreatedAt() != null ? post.getCreatedAt() : "");
                        intent.putExtra("POST_APPROVALS", post.getApprovals());
                        intent.putExtra("POST_SHARES", post.getShares());
                        startActivity(intent);
                    });
                }

                postView.setOnClickListener(v -> {
                    Intent intent = new Intent(ProfileActivity.this, PostDetailsActivity.class);
                    intent.putExtra("POST_ID", post.getId());
                    intent.putExtra("POST_USER_ID", post.getUserId());
                    intent.putExtra("POST_AUTHOR", post.getAuthorName() != null && !post.getAuthorName().isEmpty() ? post.getAuthorName() : "Student");
                    intent.putExtra("POST_TITLE", post.getTitle() != null ? post.getTitle() : "");
                    intent.putExtra("POST_CONTENT", post.getContent() != null ? post.getContent() : "");
                    intent.putExtra("POST_DATE", post.getCreatedAt() != null ? post.getCreatedAt() : "");
                    intent.putExtra("POST_APPROVALS", post.getApprovals());
                    intent.putExtra("POST_SHARES", post.getShares());
                    startActivity(intent);
                });

                container.addView(postView);
            }
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
}