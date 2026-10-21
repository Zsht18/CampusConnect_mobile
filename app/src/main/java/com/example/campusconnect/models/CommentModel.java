package com.example.campusconnect.models;

import com.google.gson.annotations.SerializedName;

public class CommentModel {
    @SerializedName("id")
    private int id;

    @SerializedName("post_id")
    private int postId;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("content")
    private String content;

    @SerializedName("likes")
    private int likes;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("profile_pic_url")
    private String profilePicUrl;

    public int getId() { return id; }
    public int getPostId() { return postId; }
    public int getUserId() { return userId; }
    public String getContent() { return content; }
    public int getLikes() { return likes; }
    public String getCreatedAt() { return createdAt; }
    public String getAuthorName() { return authorName; }
    public String getProfilePicUrl() { return profilePicUrl; }
}