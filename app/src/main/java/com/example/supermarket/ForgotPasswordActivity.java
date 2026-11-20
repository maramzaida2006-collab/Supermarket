package com.example.supermarket;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etResetEmail;
    private MaterialButton btnSendResetLink, btnBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etResetEmail = findViewById(R.id.etResetEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnSendResetLink.setOnClickListener(v -> resetPassword());

        btnBackToLogin.setOnClickListener(v -> finish());
    }

    private void resetPassword() {
        String email = etResetEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etResetEmail.setError("الرجاء إدخال البريد الإلكتروني");
            etResetEmail.requestFocus();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني بنجاح", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "فشل الإرسال: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}