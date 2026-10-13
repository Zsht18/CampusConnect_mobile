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
            @Field("email") String email,
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
}