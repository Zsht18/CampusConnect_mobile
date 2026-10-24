package com.example.campusconnect.models;

import com.google.gson.annotations.SerializedName;

public class NotificationModel {
    @SerializedName("id")
    private int id;
    @SerializedName("title")
    private String title;
    @SerializedName("message")
    private String message;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("is_read")
    private int read;

    public String getTitle() {
        return title == null ? "" : title;
    }

    public String getMessage() {
        return message == null ? "" : message;
    }

    public String getCreatedAt() {
        return createdAt == null ? "" : createdAt;
    }

    public boolean isRead() {
        return read != 0;
    }
}
