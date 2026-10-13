package com.example.campusconnect.models;

public class LoginResponse {
    private boolean success;
    private String message;
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
        private String id;
        private String email;
        private String full_name;

        public String getId() { return id; }
        public String getEmail() { return email; }
        public String getFullName() { return full_name; }
    }
}