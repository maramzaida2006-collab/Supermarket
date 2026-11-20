package com.example.supermarket;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnRegister, btnGoToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etRegisterEmail);
        etPassword = findViewById(R.id.etRegisterPassword);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("الرجاء إدخال البريد الإلكتروني");
                etEmail.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                etPassword.setError("الرجاء إدخال كلمة المرور");
                etPassword.requestFocus();
                return;
            }


            if (!email.endsWith("@gmail.com")) {
                Toast.makeText(RegisterActivity.this, "هذا البريد غير صالح", Toast.LENGTH_LONG).show();
                etEmail.requestFocus();
                return;
            }

            if (password.length() < 6) {
                etPassword.setError("كلمة المرور يجب أن تكون 6 أحرف على الأقل");
                etPassword.requestFocus();
                return;
            }


            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {

                            if (mAuth.getCurrentUser() != null) {
                                mAuth.getCurrentUser().sendEmailVerification()
                                        .addOnCompleteListener(verificationTask -> {
                                            Toast.makeText(RegisterActivity.this, "تم إنشاء الحساب بنجاح! تم إرسال رابط التحقق إلى بريدك.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            finish();
                                        });
                            }
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "";

                            if (errorMessage.contains("badly formatted") || errorMessage.contains("address is invalid") || errorMessage.contains("no user record")) {
                                Toast.makeText(RegisterActivity.this, "البريد الإلكتروني المدخل غير موجود على جوجل أو غير صالح!", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(RegisterActivity.this, "فشل إنشاء الحساب: " + errorMessage, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        btnGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }
}