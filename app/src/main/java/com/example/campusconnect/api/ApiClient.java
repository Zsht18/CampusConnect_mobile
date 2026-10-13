package com.example.campusconnect.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    // IMPORTANT: Replace this with your actual Hostinger domain or IP!
    // Example: "https://your-domain.com/campusconnect_api/"
    // Note: If testing on emulator with a local PHP server, use "http://10.0.2.2/campusconnect_api/"
    private static final String BASE_URL = "https://campusconnect.uslsbsit.com/mobile/";
    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}