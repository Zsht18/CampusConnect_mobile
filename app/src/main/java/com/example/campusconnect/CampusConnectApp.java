package com.example.campusconnect;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;
    private static final long LOGOUT_RETRY_INTERVAL_MS = 30_000L;
    private static final String TAG = "CampusConnectApp";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int startedActivities;
    private boolean logoutRetryInProgress;
    private String restrictedSessionToken = "";
    private boolean restrictionDialogShowing;
    private Activity currentActivity;

    private final Runnable heartbeat = new Runnable() {
        @Override
        public void run() {
            if (startedActivities <= 0) return;
            String token = currentSessionToken();
            int userId = currentUserId();
            if ((token.isEmpty() && userId <= 0) || sessionKey(token, userId).equals(restrictedSessionToken)) return;
            sendHeartbeat(token, userId);
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
            startHeartbeatIfNeeded();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
        if (startedActivities == 0) {
            handler.removeCallbacks(heartbeat);
        }
    }

    private void sendHeartbeat(String token, int userId) {
        ApiClient.getClient().create(ApiService.class)
                .mobileSessionHeartbeat(token, userId)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        GenericResponse result = response.body();
                        if (response.isSuccessful() && result != null
                                && token.equals(currentSessionToken())
                                && userId == currentUserId()
                                && AccountRestrictionDialog.isRestricted(result.getAccountStatus())) {
                            handleAccountRestriction(
                                    sessionKey(token, userId),
                                    result.getAccountStatus(),
                                    result.getMessage()
                            );
                            return;
                        }
                        if (!response.isSuccessful()) {
                            Log.w(TAG, "Mobile session heartbeat failed (" + response.code() + ").");
                        } else if (result == null || !result.isSuccess()) {
                            Log.w(TAG, "Mobile session heartbeat returned an unsuccessful response.");
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

    private String currentSessionToken() {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE)
                .getString("USER_SESSION_TOKEN", "");
    }

    private int currentUserId() {
        return getSharedPreferences("AppPrefs", MODE_PRIVATE).getInt("USER_ID", 0);
    }

    private String sessionKey(String token, int userId) {
        return token.isEmpty() ? "user:" + userId : token;
    }

    private void startHeartbeatIfNeeded() {
        String token = currentSessionToken();
        int userId = currentUserId();
        if (startedActivities > 0 && (!token.isEmpty() || userId > 0)
                && !sessionKey(token, userId).equals(restrictedSessionToken)
                && !handler.hasCallbacks(heartbeat)) {
            heartbeat.run();
        }
    }

    private void clearPendingLogout(String completedToken) {
        android.content.SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        if (completedToken.equals(preferences.getString("PENDING_LOGOUT_SESSION_TOKEN", ""))) {
            preferences.edit().remove("PENDING_LOGOUT_SESSION_TOKEN").apply();
        }
    }

    private void handleAccountRestriction(String sessionKey, String status, String message) {
        if (sessionKey.equals(restrictedSessionToken)) return;
        restrictedSessionToken = sessionKey;
        handler.removeCallbacks(heartbeat);

        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int userId = preferences.getInt("USER_ID", 0);
        SharedPreferences.Editor editor = preferences.edit()
                .remove("USER_ID")
                .remove("USER_FULL_NAME")
                .remove("USER_USERNAME")
                .remove("USER_CREATED_AT")
                .remove("USER_POSTS_COUNT")
                .remove("USER_SESSION_TOKEN")
                .remove("USER_PROFILE_PIC_URL")
                .remove("PENDING_LOGOUT_SESSION_TOKEN")
                .putString("PENDING_ACCOUNT_RESTRICTION_STATUS", status)
                .putString("PENDING_ACCOUNT_RESTRICTION_MESSAGE", message == null ? "" : message);
        if (userId > 0) {
            editor.remove("USER_PROFILE_PIC_URI_" + userId);
        }
        editor.apply();

        openLoginForRestriction(currentActivity);
    }

    private void openLoginForRestriction(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (activity instanceof LoginActivity) {
            showPendingAccountRestriction(activity);
            return;
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private void showPendingAccountRestriction(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()
                || restrictionDialogShowing) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String status = preferences.getString("PENDING_ACCOUNT_RESTRICTION_STATUS", "");
        if (!AccountRestrictionDialog.isRestricted(status)) {
            return;
        }

        String message = preferences.getString("PENDING_ACCOUNT_RESTRICTION_MESSAGE", "");
        if (message.isEmpty()) {
            message = "Your account has been " + status
                    + " in violation of the CampusConnect community guidelines.";
        }
        restrictionDialogShowing = true;
        AccountRestrictionDialog.show(activity, status, message, () -> {
            preferences.edit()
                    .remove("PENDING_ACCOUNT_RESTRICTION_STATUS")
                    .remove("PENDING_ACCOUNT_RESTRICTION_MESSAGE")
                    .apply();
            restrictionDialogShowing = false;
        });
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {}
    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
        SharedPreferences preferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String pendingStatus = preferences.getString("PENDING_ACCOUNT_RESTRICTION_STATUS", "");
        if (AccountRestrictionDialog.isRestricted(pendingStatus)
                && !(activity instanceof LoginActivity)) {
            openLoginForRestriction(activity);
            return;
        }
        showPendingAccountRestriction(activity);
        startHeartbeatIfNeeded();
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}
