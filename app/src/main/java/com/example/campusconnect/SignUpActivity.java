package com.example.campusconnect;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campusconnect.api.ApiClient;
import com.example.campusconnect.api.ApiService;
import com.example.campusconnect.models.GenericResponse;
import com.example.campusconnect.models.SignupResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * SignUpActivity handles new student account registration.
 * It enforces institutional email domain validation (.edu / .edu.ph),
 * OTP verification via email, and submits account creation requests to backend.
 */
public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText idNumberEditText;
    private EditText fullNameEditText;
    private EditText usernameEditText;
    private EditText passwordEditText;
    private EditText retypePasswordEditText;
    private Button signUpButton;
    private Button returnToLoginBtn;
    private Button btnGetOtp;

    // Flag indicating whether the institutional email has been verified with OTP
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup); // Bind layout

        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        idNumberEditText = findViewById(R.id.idNumberEditText);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        retypePasswordEditText = findViewById(R.id.retypePasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        returnToLoginBtn = findViewById(R.id.returnToLoginBtn);
        btnGetOtp = findViewById(R.id.btnGetOtp);

        // Return to Login Activity when "Return to Login" button is tapped
        if (returnToLoginBtn != null) {
            returnToLoginBtn.setOnClickListener(v -> finish());
        }

        // Handle "Get OTP Code" button click
        if (btnGetOtp != null) {
            btnGetOtp.setOnClickListener(v -> {
                String email = emailEditText.getText().toString().trim();
                if (email.isEmpty()) {
                    Toast.makeText(this, "Please enter your institutional email first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isValidInstitutionalEmail(email)) {
                    Toast.makeText(this, "Institutional/School Emails Only", Toast.LENGTH_LONG).show();
                    return;
                }

                // Call backend API to generate and send OTP code to email
                Toast.makeText(this, "Sending OTP code...", Toast.LENGTH_SHORT).show();
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.sendOtp(email);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            if (AccountRestrictionDialog.isRestricted(response.body().getAccountStatus())) {
                                AccountRestrictionDialog.show(SignUpActivity.this, response.body().getAccountStatus());
                                return;
                            }
                            Toast.makeText(SignUpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                            if (response.body().isSuccess()) {
                                showEnterOtpDialog(email); // Show OTP verification popup
                                btnGetOtp.setText("Resend OTP");
                            }
                        } else {
                            Toast.makeText(SignUpActivity.this, "Failed to send OTP.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(SignUpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        // Handle Sign Up button submission
        signUpButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String idNumber = idNumberEditText.getText().toString().trim();
            String fullName = fullNameEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String retypePassword = retypePasswordEditText.getText().toString().trim();

            // Input field validation
            if (email.isEmpty() || idNumber.isEmpty() || fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidInstitutionalEmail(email)) {
                Toast.makeText(SignUpActivity.this, "Institutional/School Emails Only", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isEmailVerified) {
                Toast.makeText(SignUpActivity.this, "Please verify your institutional email with OTP first.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!isValidStrongPassword(password)) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 8 characters long and contain both letters and numbers.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(retypePassword)) {
                Toast.makeText(SignUpActivity.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(SignUpActivity.this, "Signing up...", Toast.LENGTH_SHORT).show();

            // Call backend API to create user account
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            Call<SignupResponse> call = apiService.signup(email, idNumber, fullName, username, password);
            call.enqueue(new Callback<SignupResponse>() {
                @Override
                public void onResponse(Call<SignupResponse> call, Response<SignupResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        if (AccountRestrictionDialog.isRestricted(response.body().getAccountStatus())) {
                            AccountRestrictionDialog.show(SignUpActivity.this, response.body().getAccountStatus());
                            return;
                        }
                        Toast.makeText(SignUpActivity.this, response.body().getMessage(), Toast.LENGTH_SHORT).show();
                        if (response.body().isSuccess()) {
                            finish(); // Account created, return to Login
                        }
                    } else {
                        Toast.makeText(SignUpActivity.this, "Signup failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<SignupResponse> call, Throwable t) {
                    Toast.makeText(SignUpActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        });
    }

    /**
     * Displays a popup dialog for entering the 6-digit OTP code sent to email.
     */
    private void showEnterOtpDialog(String email) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_enter_otp);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        EditText dialogOtpInput = dialog.findViewById(R.id.dialogOtpInput);
        View btnCancel = dialog.findViewById(R.id.btnCancelOtp);
        View btnVerify = dialog.findViewById(R.id.btnVerifyOtpModal);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnVerify != null && dialogOtpInput != null) {
            btnVerify.setOnClickListener(v -> {
                String otpCode = dialogOtpInput.getText().toString().trim();
                if (otpCode.length() != 6) {
                    Toast.makeText(this, "Please enter the 6-digit OTP code.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Call backend API to verify entered OTP code
                Toast.makeText(this, "Verifying OTP...", Toast.LENGTH_SHORT).show();
                ApiService apiService = ApiClient.getClient().create(ApiService.class);
                Call<GenericResponse> call = apiService.verifyOtp(email, otpCode);
                call.enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            dialog.dismiss();
                            isEmailVerified = true;
                            Toast.makeText(SignUpActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                            emailEditText.setEnabled(false);
                            if (btnGetOtp != null) {
                                btnGetOtp.setEnabled(false);
                                btnGetOtp.setText("✓ Verified");
                                btnGetOtp.setBackgroundResource(R.drawable.verified_btn_bg);
                            }
                        } else {
                            String msg = response.body() != null ? response.body().getMessage() : "Invalid OTP.";
                            Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Toast.makeText(SignUpActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }

        dialog.show();
    }

    /**
     * Validates that the provided email belongs to an institutional/educational domain (.edu / .edu.ph)
     * and rejects personal domains (Gmail, Yahoo, Outlook, etc.).
     */
    private boolean isValidInstitutionalEmail(String email) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }
        String lowerEmail = email.toLowerCase().trim();
        String[] blockedDomains = {
            "gmail.com", "yahoo.com", "outlook.com", "hotmail.com", 
            "live.com", "icloud.com", "me.com", "aol.com", 
            "mail.com", "zoho.com", "proton.me", "protonmail.com"
        };

        for (String domain : blockedDomains) {
            if (lowerEmail.endsWith("@" + domain) || lowerEmail.contains("@" + domain)) {
                return false;
            }
        }

        return lowerEmail.contains(".edu") || lowerEmail.contains(".ac.");
    }

    private boolean isValidStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        return hasLetter && hasDigit;
    }
}