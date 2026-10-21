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

    @SerializedName("profile_pic_url")
    private String profilePicUrl;

    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("owner_username")
    private String ownerUsername;

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
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getFileUrl() { return fileUrl; }
    public String getOwnerUsername() { return ownerUsername; }
    public int getCommentsCount() { return commentsCount; }
    public void setId(int id) { this.id = id; }
    public void setUserId(int userId) { this.userId = userId; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setApprovals(int approvals) { this.approvals = approvals; }
    public void setShares(int shares) { this.shares = shares; }
    public void setIsAnonymous(int isAnonymous) { this.isAnonymous = isAnonymous; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}