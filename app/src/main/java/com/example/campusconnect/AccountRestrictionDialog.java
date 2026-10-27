package com.example.campusconnect;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

/** Popup shown when a suspended or banned account tries to log in or register. */
public final class AccountRestrictionDialog {

    private AccountRestrictionDialog() {}

    public static boolean isRestricted(String accountStatus) {
        return "suspended".equals(accountStatus) || "banned".equals(accountStatus);
    }

    public static void show(Context context, String accountStatus) {
        show(context, accountStatus,
                "Your account has been " + accountStatus
                        + " in violation of the CampusConnect community guidelines.",
                null);
    }

    public static void show(Context context, String accountStatus, String message, Runnable onDismiss) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle("Account " + accountStatus)
                .setMessage(message)
                .setCancelable(onDismiss == null)
                .setPositiveButton("OK", (dialog, which) -> {
                    if (onDismiss != null) {
                        onDismiss.run();
                    }
                });
        builder.show();
    }
}
