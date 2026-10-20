package com.example.campusconnect.models;

import com.google.gson.annotations.SerializedName;

public class SearchModel {
    @SerializedName("query")
    private String query;

    @SerializedName("search_count")
    private int searchCount;

    public String getQuery() {
        return query;
    }

    public int getSearchCount() {
        return searchCount;
    }
}