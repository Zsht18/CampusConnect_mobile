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
        new AlertDialog.Builder(context)
                .setTitle("Account " + accountStatus)
                .setMessage("Your account has been " + accountStatus
                        + " in violation of the CampusConnect community guidelines.")
                .setPositiveButton("OK", null)
                .show();
    }
}
