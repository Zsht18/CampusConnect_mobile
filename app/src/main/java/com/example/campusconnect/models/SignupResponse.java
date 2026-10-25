package com.example.campusconnect.models;

public class SignupResponse {
    private boolean success;
    private String message;
    @com.google.gson.annotations.SerializedName("account_status")
    private String accountStatus;

    public String getAccountStatus() {
        return accountStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}