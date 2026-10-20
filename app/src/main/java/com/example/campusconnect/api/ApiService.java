package com.example.campusconnect.api;

import com.example.campusconnect.models.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {

    @FormUrlEncoded
    @POST("login.php")
    Call<LoginResponse> login(
            @Field("id_number") String idNumber,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("signup.php")
    Call<com.example.campusconnect.models.SignupResponse> signup(
            @Field("email") String email,
            @Field("id_number") String idNumber,
            @Field("full_name") String fullName,
            @Field("username") String username,
            @Field("password") String password
    );

    @FormUrlEncoded
    @POST("create_post.php")
    Call<com.example.campusconnect.models.GenericResponse> createPost(
            @Field("user_id") int userId,
            @Field("title") String title,
            @Field("category") String category,
            @Field("content") String content,
            @Field("is_anonymous") int isAnonymous
    );

    @retrofit2.http.GET("get_posts.php")
    Call<java.util.List<com.example.campusconnect.models.PostModel>> getPosts(
            @retrofit2.http.Query("category") String category,
            @retrofit2.http.Query("user_id") Integer userId
    );

    @FormUrlEncoded
    @POST("record_search.php")
    Call<com.example.campusconnect.models.GenericResponse> recordSearch(
            @Field("query") String query
    );

    @retrofit2.http.GET("get_trending_searches.php")
    Call<java.util.List<com.example.campusconnect.models.SearchModel>> getTrendingSearches();

    @FormUrlEncoded
    @POST("add_comment.php")
    Call<com.example.campusconnect.models.GenericResponse> addComment(
            @Field("post_id") int postId,
            @Field("user_id") int userId,
            @Field("content") String content
    );

    @retrofit2.http.GET("get_comments.php")
    Call<java.util.List<com.example.campusconnect.models.CommentModel>> getComments(
            @retrofit2.http.Query("post_id") int postId
    );

    @FormUrlEncoded
    @POST("delete_post.php")
    Call<com.example.campusconnect.models.GenericResponse> deletePost(
            @Field("post_id") int postId,
            @Field("user_id") int userId
    );

    @FormUrlEncoded
    @POST("delete_comment.php")
    Call<com.example.campusconnect.models.GenericResponse> deleteComment(
            @Field("comment_id") int commentId,
            @Field("user_id") int userId
    );
}