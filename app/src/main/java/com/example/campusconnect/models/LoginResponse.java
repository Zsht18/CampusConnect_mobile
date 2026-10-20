package com.example.campusconnect.models;

public class LoginResponse {
    @com.google.gson.annotations.SerializedName("success")
    private boolean success;
    @com.google.gson.annotations.SerializedName("message")
    private String message;
    @com.google.gson.annotations.SerializedName("user")
    private UserData user;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserData getUser() {
        return user;
    }

    public static class UserData {
        @com.google.gson.annotations.SerializedName("id")
        private String id;
        @com.google.gson.annotations.SerializedName("email")
        private String email;
        @com.google.gson.annotations.SerializedName("full_name")
        private String full_name;
        @com.google.gson.annotations.SerializedName("username")
        private String username;
        @com.google.gson.annotations.SerializedName("created_at")
        private String created_at;
        @com.google.gson.annotations.SerializedName("posts_count")
        private int posts_count;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return full_name; }
        public String getUsername() { return username; }
        public String getCreatedAt() { return created_at; }
        public int getPostsCount() { return posts_count; }
    }
}