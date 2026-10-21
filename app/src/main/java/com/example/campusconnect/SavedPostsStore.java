package com.example.campusconnect;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.campusconnect.models.PostModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class SavedPostsStore {
    private static final String PREFS = "SavedPosts";

    private SavedPostsStore() {
    }

    public static boolean add(Context context, PostModel post, int userId) {
        List<PostModel> posts = get(context, userId);
        for (PostModel saved : posts) {
            if (saved.getId() == post.getId()) return false;
        }
        posts.add(0, post);
        save(context, userId, posts);
        return true;
    }

    public static boolean contains(Context context, int postId, int userId) {
        for (PostModel saved : get(context, userId)) {
            if (saved.getId() == postId) return true;
        }
        return false;
    }

    public static boolean remove(Context context, int postId, int userId) {
        List<PostModel> posts = get(context, userId);
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getId() == postId) {
                posts.remove(i);
                save(context, userId, posts);
                return true;
            }
        }
        return false;
    }

    public static List<PostModel> get(Context context, int userId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = prefs.getString("USER_" + userId, "[]");
        Type type = new TypeToken<List<PostModel>>() { }.getType();
        List<PostModel> posts = new Gson().fromJson(json, type);
        return posts != null ? posts : new ArrayList<>();
    }

    private static void save(Context context, int userId, List<PostModel> posts) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("USER_" + userId, new Gson().toJson(posts))
                .apply();
    }
}
