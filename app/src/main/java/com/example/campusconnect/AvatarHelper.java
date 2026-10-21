package com.example.campusconnect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.net.URL;

public class AvatarHelper {
    public static void loadHeaderAvatar(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int userId = prefs.getInt("USER_ID", 1);

        File userLocalFile = new File(activity.getFilesDir(), "profile_user_" + userId + ".jpg");
        String target = userLocalFile.exists() ? "file://" + userLocalFile.getAbsolutePath() : prefs.getString("USER_PROFILE_PIC_URI_" + userId, "");

        loadAvatar(activity, target, activity.findViewById(R.id.headerAvatarImage), activity.findViewById(R.id.headerAvatarText));
    }

    public static void loadAvatar(Context context, String urlOrUri, ImageView imageView, TextView textView) {
        if (urlOrUri == null || urlOrUri.isEmpty()) {
            if (imageView != null) imageView.setVisibility(View.GONE);
            if (textView != null) textView.setVisibility(View.VISIBLE);
            return;
        }

        if (urlOrUri.startsWith("file://")) {
            try {
                String filePath = Uri.parse(urlOrUri).getPath();
                if (filePath != null) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        Bitmap bmp = BitmapFactory.decodeFile(filePath);
                        if (bmp != null && imageView != null && textView != null) {
                            imageView.setImageBitmap(bmp);
                            imageView.setVisibility(View.VISIBLE);
                            textView.setVisibility(View.GONE);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore and fall through
            }
        }

        if (urlOrUri.startsWith("content://")) {
            try {
                if (imageView != null && textView != null) {
                    imageView.setImageURI(Uri.parse(urlOrUri));
                    imageView.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.GONE);
                    return;
                }
            } catch (Exception e) {
                // Ignore and fall through
            }
        }

        if (urlOrUri.startsWith("http://") || urlOrUri.startsWith("https://")) {
            new Thread(() -> {
                try {
                    URL url = new URL(urlOrUri);
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    if (bmp != null && context instanceof Activity) {
                        ((Activity) context).runOnUiThread(() -> {
                            if (imageView != null && textView != null) {
                                imageView.setImageBitmap(bmp);
                                imageView.setVisibility(View.VISIBLE);
                                textView.setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (Exception e) {
                    if (context instanceof Activity) {
                        ((Activity) context).runOnUiThread(() -> {
                            if (imageView != null) imageView.setVisibility(View.GONE);
                            if (textView != null) textView.setVisibility(View.VISIBLE);
                        });
                    }
                }
            }).start();
            return;
        }

        if (imageView != null) imageView.setVisibility(View.GONE);
        if (textView != null) textView.setVisibility(View.VISIBLE);
    }
}