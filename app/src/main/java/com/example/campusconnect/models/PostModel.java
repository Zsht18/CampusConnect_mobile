package com.example.campusconnect.models;

import com.google.gson.annotations.SerializedName;

public class PostModel {
    @SerializedName("id")
    private int id;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("title")
    private String title;

    @SerializedName("category")
    private String category;

    @SerializedName("content")
    private String content;

    @SerializedName("is_anonymous")
    private int isAnonymous;

    @SerializedName("approvals")
    private int approvals;

    @SerializedName("shares")
    private int shares;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("author_name")
    private String authorName;

    @SerializedName("comments_count")
    private int commentsCount;

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getContent() { return content; }
    public int getIsAnonymous() { return isAnonymous; }
    public int getApprovals() { return approvals; }
    public int getShares() { return shares; }
    public String getCreatedAt() { return createdAt; }
    public String getAuthorName() { return authorName; }
    public int getCommentsCount() { return commentsCount; }
}