package com.example.supermarket;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoToRegister, btnForgotPassword;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "SupermarketPrefs";
    private static final String KEY_REMEMBER = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);


        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (mAuth.getCurrentUser() != null) {
            if (isRemembered) {

                startActivity(new Intent(LoginActivity.this, LockActivity.class));
                finish();
            } else {

                mAuth.signOut();
            }
        }

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnGoToRegister);
        btnForgotPassword = findViewById(R.id.btnForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        cbRememberMe.setChecked(isRemembered);

        btnLogin.setOnClickListener(v -> {
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

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(KEY_REMEMBER, cbRememberMe.isChecked());
                                editor.apply();

                                Toast.makeText(this, "تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "الرجاء تفعيل البريد الإلكتروني أولاً من الرابط المرسل إليك", Toast.LENGTH_LONG).show();
                                mAuth.signOut();
                            }
                        } else {
                            String errorMessage = "فشل تسجيل الدخول";
                            try {
                                throw task.getException();
                            } catch (FirebaseAuthInvalidUserException e) {
                                String errorCode = e.getErrorCode();
                                if (errorCode != null && errorCode.equals("ERROR_USER_DISABLED")) {
                                    errorMessage = "تم تعطيل حسابك من قبل الإدارة!";
                                } else {
                                    errorMessage = "الحساب غير موجود أو تم تعطيله!";
                                }
                            } catch (FirebaseAuthInvalidCredentialsException e) {
                                errorMessage = "كلمة المرور غير صحيحة أو البريد غير صحيح!";
                            } catch (Exception e) {
                                errorMessage = "فشل الدخول: " + e.getMessage();
                            }

                            Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}