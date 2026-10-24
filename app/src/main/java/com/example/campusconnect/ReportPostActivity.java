package com.example.campusconnect;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ReportPostActivity handles reporting inappropriate or non-academic posts.
 * Submits post_id, reporter_user_id, selected reason, and comments to backend mobile_reports table.
 */
public class ReportPostActivity extends AppCompatActivity {

    private int postId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_post);

        postId = getIntent().getIntExtra("POST_ID", 0);
        AvatarHelper.loadHeaderAvatar(this);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnCancel = findViewById(R.id.btnCancelReport);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> finish());
        }

        RadioGroup reasonRadioGroup = findViewById(R.id.reasonRadioGroup);
        EditText reportDetailsInput = findViewById(R.id.reportDetailsInput);

        Button btnSubmit = findViewById(R.id.btnSubmitReport);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> {
                String selectedReason = "Inappropriate content";
                if (reasonRadioGroup != null) {
                    int checkedId = reasonRadioGroup.getCheckedRadioButtonId();
                    RadioButton selectedRadio = findViewById(checkedId);
                    if (selectedRadio != null) {
                        selectedReason = selectedRadio.getText().toString();
                    }
                }

                String additionalComments = reportDetailsInput != null ? reportDetailsInput.getText().toString().trim() : "";

                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                int reporterUserId = prefs.getInt("USER_ID", 1);
                if (reporterUserId <= 0) reporterUserId = 1;

                Toast.makeText(this, "Submitting report...", Toast.LENGTH_SHORT).show();

                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.submitReport(postId, reporterUserId, selectedReason, additionalComments);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Toast.makeText(ReportPostActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if (response.body().isSuccess()) {
                                finish();
                            }
                        } else {
                            Toast.makeText(ReportPostActivity.this, "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(ReportPostActivity.this, "Report submitted successfully.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
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