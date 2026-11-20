package com.example.supermarket;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {

    private TextInputEditText etPinCode;
    private MaterialButton btnLogin, btnBiometric;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "AppLockPrefs";
    private static final String KEY_PIN = "user_pin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        etPinCode = findViewById(R.id.etPinCode);
        btnLogin = findViewById(R.id.btnLogin);
        btnBiometric = findViewById(R.id.btnBiometric);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedPin = sharedPreferences.getString(KEY_PIN, null);

        if (savedPin == null) {
            btnLogin.setText("إنشاء كلمة السر والحفظ");
            etPinCode.setHint("أنشئ كلمة سر جديدة (4-6 أرقام)");
        } else {
            showBiometricPrompt();
        }

        btnLogin.setOnClickListener(v -> {
            String inputPin = etPinCode.getText().toString().trim();
            if (inputPin.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال كلمة السر", Toast.LENGTH_SHORT).show();
                return;
            }

            if (savedPin == null) {
                sharedPreferences.edit().putString(KEY_PIN, inputPin).apply();
                Toast.makeText(this, "تم حفظ كلمة السر بنجاح", Toast.LENGTH_SHORT).show();
                openMainActivity();
            } else {
                if (inputPin.equals(savedPin)) {
                    openMainActivity();
                } else {
                    Toast.makeText(this, "كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnBiometric.setOnClickListener(v -> showBiometricPrompt());
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(LockActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                openMainActivity();
            }

            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "لم يتم التعرف على البصمة", Toast.LENGTH_SHORT).show();
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("تسجيل الدخول بالبصمة")
                .setSubtitle("ضع إصبعك على المستشعر لفتح دفتر الديون")
                .setNegativeButtonText("إدخال كلمة السر")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private void openMainActivity() {
        Intent intent = new Intent(LockActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}