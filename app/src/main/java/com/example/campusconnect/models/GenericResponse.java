package com.example.campusconnect.models;

import com.google.gson.annotations.SerializedName;

public class GenericResponse {
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("account_status")
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

    public String getFileUrl() {
        return fileUrl;
    }
}