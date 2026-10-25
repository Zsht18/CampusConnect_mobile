package com.example.campusconnect;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CampusConnectApp extends Application implements Application.ActivityLifecycleCallbacks {
    private static final long HEARTBEAT_INTERVAL_MS = 60_000L;
    private static final long LOGOUT_RETRY_INTERVAL_MS = 30_000L;
    private static final String TAG = "CampusConnectApp";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int startedActivities;
    private boolean logoutRetryInProgress;

    private final Runnable heartbeat = new Runnable() {
        @Override
        public void run() {
            if (startedActivities <= 0) return;
            sendHeartbeat();
            handler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        startedActivities++;
        retryPendingLogout();
        if (startedActivities == 1) {
            handler.removeCallbacks(heartbeat);
            heartbeat.run();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) {
            handler.removeCallbacks(heartbeat);
        }
    }

    private void sendHeartbeat() {
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("USER_SESSION_TOKEN", "");
        if (token.isEmpty()) return;

        ApiClient.getClient().create(ApiService.class)
                .mobileSessionHeartbeat(token)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "Mobile session heartbeat failed (" + response.code() + ").");
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable error) {
                        Log.w(TAG, "Unable to send mobile session heartbeat.", error);
                    }
                });
    }

    private void retryPendingLogout() {
        if (logoutRetryInProgress) return;
        String token = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("PENDING_LOGOUT_SESSION_TOKEN", "");
        if (token.isEmpty()) return;

        logoutRetryInProgress = true;
        ApiClient.getClient().create(ApiService.class)
                .mobileSessionLogout(token)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (!response.isSuccessful() || response.body() == null || !response.body().isSuccess()) {
                            Log.w(TAG, "Pending mobile logout was not confirmed (" + response.code() + ").");
                            logoutRetryInProgress = false;
                            schedulePendingLogoutRetry();
                            return;
                        }
                        clearPendingLogout(token);
                        logoutRetryInProgress = false;
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable error) {
                        Log.w(TAG, "Unable to retry pending mobile logout.", error);
                        logoutRetryInProgress = false;
                        schedulePendingLogoutRetry();
                    }
                });
    }

    private void schedulePendingLogoutRetry() {
        if (startedActivities > 0) {
            handler.postDelayed(this::retryPendingLogout, LOGOUT_RETRY_INTERVAL_MS);
        }
    }

    private void clearPendingLogout(String completedToken) {
        android.content.SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (completedToken.equals(preferences.getString("PENDING_LOGOUT_SESSION_TOKEN", ""))) {
            preferences.edit().remove("PENDING_LOGOUT_SESSION_TOKEN").apply();
        }
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
