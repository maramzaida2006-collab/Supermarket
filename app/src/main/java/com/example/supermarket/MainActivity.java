package com.example.supermarket;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private RecyclerView recyclerView;
    private MaterialButton btnAddCustomer, btnLogout;
    private MaterialCardView btnMenuArchive;

    private CustomersAdapter adapter;
    private List<Customer> customerList;
    private List<Customer> filteredList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private final Handler securityHandler = new Handler(Looper.getMainLooper());
    private Runnable securityRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);

        mAuth = FirebaseAuth.getInstance();

        etSearch = findViewById(R.id.etSearch);
        recyclerView = findViewById(R.id.recyclerViewCustomers);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        btnMenuArchive = findViewById(R.id.btnMenuArchive);
        btnLogout = findViewById(R.id.btnLogout);

        customerList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new CustomersAdapter(filteredList, customer -> {
            Intent intent = new Intent(MainActivity.this, CustomerDetailActivity.class);
            intent.putExtra("CUSTOMER_ID", customer.getId());
            intent.putExtra("CUSTOMER_NAME", customer.getName());
            intent.putExtra("CUSTOMER_PHONE", customer.getPhone());
            intent.putExtra("CUSTOMER_TOTAL_DEBT", customer.getTotalDebt());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            navigateToLogin();
        });

        btnMenuArchive.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ArchiveActivity.class);
            startActivity(intent);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCustomers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnAddCustomer.setOnClickListener(v -> showAddCustomerDialog());

        securityRunnable = new Runnable() {
            @Override
            public void run() {
                checkAccountActiveStatusSilently();
                securityHandler.postDelayed(this, 10000);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthAndLoad();
        if (securityHandler != null && securityRunnable != null) {
            securityHandler.postDelayed(securityRunnable, 10000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (securityHandler != null && securityRunnable != null) {
            securityHandler.removeCallbacks(securityRunnable);
        }
    }

    private void checkAccountActiveStatusSilently() {
        if (mAuth.getCurrentUser() != null) {
            mAuth.getCurrentUser().getIdToken(true).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Exception exception = task.getException();
                    if (exception instanceof FirebaseAuthInvalidUserException) {
                        String errorCode = ((FirebaseAuthInvalidUserException) exception).getErrorCode();
                        if (errorCode != null && errorCode.equals("ERROR_USER_DISABLED")) {
                            Toast.makeText(MainActivity.this, "تم تعطيل هذا الحساب من الإدارة!", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            navigateToLogin();
                        }
                    }
                }
            });
        }
    }

    private void checkUserAuthAndLoad() {
        if (mAuth.getCurrentUser() == null) {
            navigateToLogin();
        } else {
            currentUserId = mAuth.getCurrentUser().getUid();

            mAuth.getCurrentUser().getIdToken(false).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    if (mAuth.getCurrentUser() != null && !mAuth.getCurrentUser().isEmailVerified()) {
                        Toast.makeText(this, "هذا البريد غير مفعل! يرجى تفعيل الحساب.", Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        navigateToLogin();
                        return;
                    }
                }
                loadCustomers();
            });
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadCustomers() {
        if (currentUserId == null || mAuth.getCurrentUser() == null) return;

        db.collection("customers")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        // إذا كان المستخدم قد قام بتسجيل الخروج بالفعل، نتجاهل الخطأ ولا نُظهر رسالة
                        if (mAuth.getCurrentUser() == null) {
                            return;
                        }
                        Toast.makeText(this, "خطأ في التحميل: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    customerList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Customer customer = doc.toObject(Customer.class);
                            customer.setId(doc.getId());

                            Boolean archived = doc.getBoolean("archived");
                            if (archived != null && archived) {
                                continue;
                            }

                            customerList.add(customer);
                        }
                    }

                    String currentQuery = etSearch.getText() != null ? etSearch.getText().toString() : "";
                    filterCustomers(currentQuery);
                });
    }
    private void filterCustomers(String query) {
        filteredList.clear();
        for (Customer customer : customerList) {
            if (customer.getName() != null && customer.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(customer);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddCustomerDialog() {
        if (currentUserId == null) {
            Toast.makeText(this, "جاري تهيئة الحساب، يرجى الانتظار قليلاً...", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_customer, null);
        builder.setView(dialogView);

        TextInputEditText etName = dialogView.findViewById(R.id.etDialogName);
        TextInputEditText etPhone = dialogView.findViewById(R.id.etDialogPhone);
        TextInputEditText etItemName = dialogView.findViewById(R.id.etDialogItemName);
        TextInputEditText etAmount = dialogView.findViewById(R.id.etDialogAmount);
        TextInputEditText etDays = dialogView.findViewById(R.id.etDialogDays);

        builder.setPositiveButton("حفظ وإضافة", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String itemName = etItemName.getText().toString().trim();
            String amountStr = etAmount.getText().toString().trim();
            String daysStr = etDays.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال اسم الزبون كحقل إجباري!", Toast.LENGTH_LONG).show();
                return;
            }

            for (Customer existingCustomer : customerList) {
                if (existingCustomer.getName() != null && existingCustomer.getName().trim().equalsIgnoreCase(name)) {
                    Toast.makeText(this, "هذا الزبون موجود سابقاً!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال المبلغ كحقل إجباري!", Toast.LENGTH_LONG).show();
                return;
            }

            double initialAmount;
            try {
                initialAmount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "الرجاء إدخال مبلغ صحيح!", Toast.LENGTH_SHORT).show();
                return;
            }

            int daysToAdd = daysStr.isEmpty() ? 0 : Integer.parseInt(daysStr);
            long dueDateMillis = (daysToAdd > 0) ? System.currentTimeMillis() + ((long) daysToAdd * 24 * 60 * 60 * 1000) : 0;
            String finalPhone = phone.isEmpty() ? "" : phone;
            String finalItemName = itemName.isEmpty() ? "دين" : itemName;

            proceedToSaveCustomer(name, finalPhone, finalItemName, initialAmount, dueDateMillis);
        });

        builder.setNegativeButton("إلغاء", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void proceedToSaveCustomer(String name, String phone, String itemName, double initialAmount, long dueDateMillis) {
        String customerId = db.collection("customers").document().getId();
        String debtId = db.collection("customers").document(customerId)
                .collection("debts").document().getId();

        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", name);
        customerMap.put("phone", phone);
        customerMap.put("totalDebt", initialAmount);
        customerMap.put("dueDate", dueDateMillis);
        customerMap.put("userId", currentUserId);
        customerMap.put("archived", false);
        customerMap.put("deletedAt", 0L);

        // إنشاء عنصر الدين مع ربطه بمعرف الزبون
        DebtItem debtItem = new DebtItem(debtId, itemName, initialAmount, System.currentTimeMillis(), dueDateMillis);
        debtItem.setCustomerId(customerId); // <--- ربط معرف الزبون بالحركة

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtId);
        debtMap.put("customerId", customerId); // <--- إضافة معرف الزبون هنا لضمان الاستعادة بدون نت
        debtMap.put("itemName", itemName);
        debtMap.put("amount", initialAmount);
        debtMap.put("timestamp", System.currentTimeMillis());
        debtMap.put("dueDate", dueDateMillis);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(debtId);

        batch.set(customerRef, customerMap);
        batch.set(debtRef, debtMap); // استخدام debtMap بدلاً من الكائن لضمان حفظ كافة الحقول في السيرفر

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تمت إضافة الزبون وسجل الدين بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "خطأ في حفظ البيانات: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}