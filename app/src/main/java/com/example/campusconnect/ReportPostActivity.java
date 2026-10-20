package com.example.campusconnect;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ReportPostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_post);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnCancel = findViewById(R.id.btnCancelReport);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        Button btnSubmit = findViewById(R.id.btnSubmitReport);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                Toast.makeText(ReportPostActivity.this, "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(ReportPostActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }
    }
}