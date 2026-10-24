package com.example.campusconnect.api;

import com.example.campusconnect.models.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.POST;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

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
    @POST("send_otp.php")
    Call<com.example.campusconnect.models.GenericResponse> sendOtp(
            @Field("email") String email
    );

    @FormUrlEncoded
    @POST("verify_otp.php")
    Call<com.example.campusconnect.models.GenericResponse> verifyOtp(
            @Field("email") String email,
            @Field("otp_code") String otpCode
    );

    @Multipart
    @POST("create_post.php")
    Call<com.example.campusconnect.models.GenericResponse> createPost(
            @Part("user_id") RequestBody userId,
            @Part("title") RequestBody title,
            @Part("category") RequestBody category,
            @Part("content") RequestBody content,
            @Part("is_anonymous") RequestBody isAnonymous,
            @Part java.util.List<okhttp3.MultipartBody.Part> files
    );

    @retrofit2.http.GET("get_posts.php")
    Call<java.util.List<com.example.campusconnect.models.PostModel>> getPosts(
            @retrofit2.http.Query("category") String category,
            @retrofit2.http.Query("user_id") Integer userId,
            @retrofit2.http.Query("search") String search
    );

    @FormUrlEncoded
    @POST("record_search.php")
    Call<com.example.campusconnect.models.GenericResponse> recordSearch(
            @Field("query") String query
    );

    @retrofit2.http.GET("get_trending_searches.php")
    Call<java.util.List<com.example.campusconnect.models.SearchModel>> getTrendingSearches();

    @retrofit2.http.GET("get_notifications.php")
    Call<java.util.List<com.example.campusconnect.models.NotificationModel>> getNotifications(
            @retrofit2.http.Query("user_id") int userId
    );

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

    @FormUrlEncoded
    @POST("update_post.php")
    Call<com.example.campusconnect.models.GenericResponse> updatePost(
            @Field("post_id") int postId,
            @Field("user_id") int userId,
            @Field("title") String title,
            @Field("category") String category,
            @Field("content") String content,
            @Field("is_anonymous") int isAnonymous
    );

    @FormUrlEncoded
    @POST("update_profile.php")
    Call<com.example.campusconnect.models.GenericResponse> updateProfile(
            @Field("user_id") int userId,
            @Field("username") String username,
            @Field("old_password") String oldPassword,
            @Field("new_password") String newPassword
    );

    @FormUrlEncoded
    @POST("upload_profile_pic.php")
    Call<com.example.campusconnect.models.GenericResponse> uploadProfilePic(
            @Field("user_id") int userId,
            @Field("image") String base64Image
    );

    @FormUrlEncoded
    @POST("submit_report.php")
    Call<com.example.campusconnect.models.GenericResponse> submitReport(
            @Field("post_id") int postId,
            @Field("reporter_user_id") int reporterUserId,
            @Field("reason") String reason,
            @Field("additional_comments") String additionalComments
    );
}