package com.example.campusconnect;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;
import com.example.campusconnect.models.PostModel;
import com.example.campusconnect.models.SearchModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private String selectedCollege;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        View feedNew = findViewById(R.id.feedNew);
        View feedHot = findViewById(R.id.feedHot);
        View feedTop = findViewById(R.id.feedTop);

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        View btnAddPost = findViewById(R.id.btnAddPost);

        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnAddPost != null) {
            btnAddPost.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, CreatePostActivity.class);
                startActivity(intent);
            });
        }

        if (feedNew != null) feedNew.setVisibility(View.VISIBLE);
        if (feedHot != null) feedHot.setVisibility(View.GONE);
        if (feedTop != null) feedTop.setVisibility(View.GONE);

        Spinner collegeSpinner = findViewById(R.id.collegeFilterSpinner);
        if (collegeSpinner != null) {
            android.widget.ArrayAdapter<CharSequence> adapter =
                    android.widget.ArrayAdapter.createFromResource(this,
                            R.array.college_choices, R.layout.item_college_spinner);
            adapter.setDropDownViewResource(R.layout.item_college_spinner_dropdown);
            collegeSpinner.setAdapter(adapter);
            int userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 1);
            String preferredCollege = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                    .getString("PREFERRED_COLLEGE_" + userId, "");
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getItem(i).toString().equals(preferredCollege)) {
                    collegeSpinner.setSelection(i);
                    break;
                }
            }
            collegeSpinner.setPopupBackgroundDrawable(
                    getDrawable(R.drawable.college_spinner_popup_bg));
            collegeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedCollege = parent.getItemAtPosition(position).toString();
                    EditText search = findViewById(R.id.searchEditText);
                    fetchLivePosts(search == null ? null : search.getText().toString().trim());
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {
                }
            });
        }

        EditText searchEditText = findViewById(R.id.searchEditText);
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    fetchLivePosts(s.toString().trim());
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            searchEditText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == android.view.KeyEvent.KEYCODE_ENTER && event.getAction() == android.view.KeyEvent.ACTION_DOWN)) {
                    String query = searchEditText.getText().toString().trim();
                    if (!query.isEmpty()) {
                        recordSearchQuery(query);
                    }
                    fetchLivePosts(query);
                    return true;
                }
                return false;
            });
        }

        SwipeRefreshLayout swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setOnRefreshListener(() -> {
                String query = searchEditText != null ? searchEditText.getText().toString().trim() : null;
                fetchLivePosts(query);
                fetchTrendingSearches();
                swipeRefreshLayout.setRefreshing(false);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AvatarHelper.loadHeaderAvatar(this);
        EditText searchEditText = findViewById(R.id.searchEditText);
        String query = searchEditText != null ? searchEditText.getText().toString().trim() : null;
        fetchLivePosts(query);
        fetchTrendingSearches();
    }

    private void recordSearchQuery(String query) {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<GenericResponse> call = apiService.recordSearch(query);
        call.enqueue(new Callback<GenericResponse>() {
            @Override
            public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                fetchTrendingSearches();
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                // Ignore
            }
        });
    }

    private void fetchTrendingSearches() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<SearchModel>> call = apiService.getTrendingSearches();
        call.enqueue(new Callback<List<SearchModel>>() {
            @Override
            public void onResponse(Call<List<SearchModel>> call, Response<List<SearchModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateTrendingSearches(response.body());
                } else {
                    populateTrendingSearches(null);
                }
            }

            @Override
            public void onFailure(Call<List<SearchModel>> call, Throwable t) {
                populateTrendingSearches(null);
            }
        });
    }

    private void populateTrendingSearches(List<SearchModel> searches) {
        View card = findViewById(R.id.trendingSearchesCard);
        LinearLayout container = findViewById(R.id.trendingSearchesContainer);
        if (container == null || card == null) return;

        if (searches == null || searches.isEmpty()) {
            card.setVisibility(View.GONE);
        } else {
            card.setVisibility(View.VISIBLE);
            container.removeAllViews();
            for (SearchModel search : searches) {
                TextView itemText = new TextView(this);
                itemText.setText(search.getQuery() + "  >");
                itemText.setTextColor(Color.parseColor("#DDDDDD"));
                itemText.setTextSize(13f);
                itemText.setPadding(0, 10, 0, 10);
                itemText.setClickable(true);
                itemText.setFocusable(true);
                itemText.setOnClickListener(v -> {
                    EditText searchInput = findViewById(R.id.searchEditText);
                    if (searchInput != null) {
                        searchInput.setText(search.getQuery());
                        searchInput.setSelection(search.getQuery().length());
                        recordSearchQuery(search.getQuery());
                        fetchLivePosts(search.getQuery());
                    }
                });
                container.addView(itemText);
            }
        }
    }

    private void fetchLivePosts() {
        fetchLivePosts(null);
    }

    private void fetchLivePosts(String searchQuery) {
        String cleanQuery = (searchQuery != null && !searchQuery.isEmpty()) ? searchQuery : null;
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<List<PostModel>> call = apiService.getPosts(selectedCollege, null, cleanQuery);
        call.enqueue(new Callback<List<PostModel>>() {
            @Override
            public void onResponse(Call<List<PostModel>> call, Response<List<PostModel>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populatePosts(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<PostModel>> call, Throwable t) {
                // Ignore or handle network failure gracefully
            }
        });
    }

    private void populatePosts(List<PostModel> posts) {
        LinearLayout postsContainerNew = findViewById(R.id.postsContainerNew);
        LinearLayout postsContainerHot = findViewById(R.id.postsContainerHot);
        LinearLayout postsContainerTop = findViewById(R.id.postsContainerTop);

        LinearLayout[] containers = {postsContainerNew, postsContainerHot, postsContainerTop};
        for (LinearLayout container : containers) {
            if (container != null) {
                container.removeAllViews();
                if (posts == null || posts.isEmpty()) {
                    TextView emptyView = new TextView(this);
                    emptyView.setText("No posts found.");
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
                        TextView commentsCount = postView.findViewById(R.id.postCommentsCount);
                        TextView saveButton = postView.findViewById(R.id.postSaveButton);
                        TextView optionsBtn = postView.findViewById(R.id.postOptionsBtn);
                        android.widget.ImageView postAvatarImg = postView.findViewById(R.id.postAvatarImage);
                        TextView postAvatarTxt = postView.findViewById(R.id.postAvatarText);

                        java.io.File userLocalFile = new java.io.File(getFilesDir(), "profile_user_" + post.getUserId() + ".jpg");

                        String avatarTarget = "";
                        if (post.getIsAnonymous() == 0 && post.getProfilePicUrl() != null && !post.getProfilePicUrl().isEmpty()) {
                            avatarTarget = post.getProfilePicUrl();
                        } else if (post.getIsAnonymous() == 0 && userLocalFile.exists()) {
                            avatarTarget = "file://" + userLocalFile.getAbsolutePath();
                        }

                        AvatarHelper.loadAvatar(this, avatarTarget, postAvatarImg, postAvatarTxt);

                        TextView attachment = postView.findViewById(R.id.postAttachment);
                        bindAttachment(attachment, post.getFileUrl());
                        View imagePlaceholder = postView.findViewById(R.id.postImagePlaceholder);
                        if (imagePlaceholder != null) {
                            imagePlaceholder.setVisibility(isImageAttachment(post.getFileUrl()) ? View.VISIBLE : View.GONE);
                            if (isImageAttachment(post.getFileUrl())) {
                                PostAttachmentHelper.loadImage(this, post.getFileUrl(),
                                        (android.widget.ImageView) imagePlaceholder);
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

                        TextView dislikes = postView.findViewById(R.id.postDislikes);
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
                                Intent intent = new Intent(HomeActivity.this, PostDetailsActivity.class);
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
                            int savedUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                    .getInt("USER_ID", 1);
                            if (SavedPostsStore.contains(this, post.getId(), savedUserId)) {
                                saveButton.setText("🔖 Saved");
                            }
                            saveButton.setOnClickListener(v -> {
                                int currentUserId = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                                        .getInt("USER_ID", 1);
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
                            Intent intent = new Intent(HomeActivity.this, PostDetailsActivity.class);
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
        }
    }

    private void bindAttachment(TextView attachment, String fileUrl) {
        if (attachment == null || fileUrl == null || fileUrl.isEmpty()) return;
        String fileName = android.net.Uri.parse(fileUrl).getLastPathSegment();
        attachment.setText("📎 Download attachment" + (fileName.isEmpty() ? "" : ": " + fileName));
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
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
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
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fileUrl)));
        } catch (Exception e) {
            Toast.makeText(this, "No app can open this attachment.", Toast.LENGTH_SHORT).show();
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
        Intent intent = new Intent(HomeActivity.this, PostDetailsActivity.class);
        startActivity(intent);
    }

    public void showPostOptions(View view) {
        PostModel post = (PostModel) view.getTag();
        int postUserId = post != null ? post.getUserId() : 0;

        android.content.SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currentUserId = prefs.getInt("USER_ID", 0);
        String currentFullName = prefs.getString("USER_FULL_NAME", "");

        boolean isOwner = false;
        if (currentUserId > 0 && postUserId == currentUserId) {
            isOwner = true;
        } else if (currentUserId == 0 && postUserId == 1) {
            isOwner = true;
        } else if (post != null && post.getAuthorName() != null && currentFullName != null &&
                   !currentFullName.isEmpty() && post.getAuthorName().equalsIgnoreCase(currentFullName)) {
            isOwner = true;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
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
                        Intent intent = new Intent(HomeActivity.this, EditPostActivity.class);
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
                    Intent intent = new Intent(HomeActivity.this, ReportPostActivity.class);
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
                    Toast.makeText(HomeActivity.this, "Post deleted successfully.", Toast.LENGTH_SHORT).show();
                    fetchLivePosts();
                } else {
                    Toast.makeText(HomeActivity.this, "Failed to delete post.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GenericResponse> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}