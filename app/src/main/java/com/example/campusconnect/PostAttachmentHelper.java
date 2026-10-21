package com.example.campusconnect;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import java.net.URL;

public final class PostAttachmentHelper {

    private PostAttachmentHelper() {
    }

    public static void loadImage(Activity activity, String fileUrl, ImageView imageView) {
        if (imageView == null || fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        imageView.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(new URL(fileUrl).openStream());
                activity.runOnUiThread(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                activity.runOnUiThread(() -> imageView.setVisibility(View.GONE));
            }
        }).start();
    }
}
