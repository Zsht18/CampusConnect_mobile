package com.example.campusconnect;

import android.os.Bundle;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.models.PostModel;

import java.io.File;
import java.util.List;

public class SavedPostsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_posts);

        View backButton = findViewById(R.id.savedPostsBackButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        int userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
        LinearLayout container = findViewById(R.id.savedPostsContainer);
        TextView emptyText = findViewById(R.id.savedPostsEmptyText);
        List<PostModel> posts = SavedPostsStore.get(this, userId);
        if (posts.isEmpty()) {
            return;
        }
        emptyText.setVisibility(View.GONE);
        for (PostModel post : posts) {
            View postView = getLayoutInflater().inflate(R.layout.item_post, container, false);
            TextView author = postView.findViewById(R.id.postAuthorName);
            TextView content = postView.findViewById(R.id.postContentText);
            TextView approvals = postView.findViewById(R.id.postApprovals);
            TextView dislikes = postView.findViewById(R.id.postDislikes);
            TextView comments = postView.findViewById(R.id.postCommentsCount);
            TextView saveButton = postView.findViewById(R.id.postSaveButton);
            TextView attachment = postView.findViewById(R.id.postAttachment);
            android.widget.ImageView avatarImage = postView.findViewById(R.id.postAvatarImage);
            TextView avatarText = postView.findViewById(R.id.postAvatarText);

            author.setText(post.getAuthorName() == null ? "Student" : post.getAuthorName());
            String postText = post.getTitle() == null ? "" : post.getTitle();
            if (post.getContent() != null && !post.getContent().isEmpty()) {
                if (!postText.isEmpty()) postText += "\n\n";
                postText += post.getContent();
            }
            content.setText(postText);
            approvals.setText("👍 " + post.getApprovals() + " Approvals");
            dislikes.setText("👎 " + post.getShares() + " Dislikes");
            comments.setText("💬 " + post.getCommentsCount());
            saveButton.setText("🔖 Saved");
            saveButton.setOnClickListener(v -> {
                int currentUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        .getInt("USER_ID", 1);
                if (SavedPostsStore.remove(this, post.getId(), currentUserId)) {
                    container.removeView(postView);
                    if (container.getChildCount() == 0) {
                        emptyText.setVisibility(View.VISIBLE);
                    }
                }
            });

            String avatarTarget = post.getIsAnonymous() == 0 ? post.getProfilePicUrl() : "";
            File localAvatar = new File(getFilesDir(), "profile_user_" + post.getUserId() + ".jpg");
            if (post.getIsAnonymous() == 0
                    && (avatarTarget == null || avatarTarget.isEmpty()) && localAvatar.exists()) {
                avatarTarget = "file://" + localAvatar.getAbsolutePath();
            }
            AvatarHelper.loadAvatar(this, avatarTarget, avatarImage, avatarText);

            if (post.getFileUrl() != null && !post.getFileUrl().isEmpty()) {
                String fileName = post.getFileUrl().substring(post.getFileUrl().lastIndexOf('/') + 1);
                attachment.setText("📎 Download attachment: " + fileName);
                attachment.setVisibility(View.VISIBLE);
            }

            postView.setTag(post);
            postView.setOnClickListener(v -> openPostDetails(v));
            container.addView(postView);
        }
    }

    public void openPostDetails(View view) {
        PostModel post = (PostModel) view.getTag();
        if (post == null) return;
        Intent intent = new Intent(this, PostDetailsActivity.class);
        intent.putExtra("POST_ID", post.getId());
        intent.putExtra("POST_USER_ID", post.getUserId());
        intent.putExtra("POST_AUTHOR", post.getAuthorName());
        intent.putExtra("POST_IS_ANONYMOUS", post.getIsAnonymous());
        intent.putExtra("POST_TITLE", post.getTitle());
        intent.putExtra("POST_CONTENT", post.getContent());
        intent.putExtra("POST_DATE", post.getCreatedAt());
        intent.putExtra("POST_APPROVALS", post.getApprovals());
        intent.putExtra("POST_SHARES", post.getShares());
        intent.putExtra("POST_FILE_URL", post.getFileUrl());
        startActivity(intent);
    }
}
