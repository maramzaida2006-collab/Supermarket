package com.example.supermarket;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerDetailActivity extends AppCompatActivity {

    private static final String TAG = "CustomerDetailActivity";

    private ImageView btnBack;
    private TextView tvDetailCustomerName, tvDetailCustomerPhone, tvDetailTotalDebt;
    private TextInputEditText etAmount, etItemName;
    private MaterialButton btnAddDebtItem, btnPayDebt, btnDownloadPdf;
    private RecyclerView recyclerViewDebts;

    private FirebaseFirestore db;
    private String customerId, customerName, customerPhone;
    private double currentTotalDebt = 0.0;
    private List<DebtItem> debtItemList;
    private DebtDetailsAdapter debtsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        db = FirebaseFirestore.getInstance();

        customerId = getIntent().getStringExtra("CUSTOMER_ID");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");
        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        currentTotalDebt = getIntent().getDoubleExtra("CUSTOMER_TOTAL_DEBT", 0.0);

        btnBack = findViewById(R.id.btnBack);
        tvDetailCustomerName = findViewById(R.id.tvDetailCustomerName);
        tvDetailCustomerPhone = findViewById(R.id.tvDetailCustomerPhone);
        tvDetailTotalDebt = findViewById(R.id.tvDetailTotalDebt);
        etAmount = findViewById(R.id.etAmount);
        etItemName = findViewById(R.id.etItemName);
        btnAddDebtItem = findViewById(R.id.btnAddDebtItem);
        btnPayDebt = findViewById(R.id.btnPayDebt);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        recyclerViewDebts = findViewById(R.id.recyclerViewDebts);

        tvDetailCustomerName.setText(customerName != null ? customerName : "اسم الزبون");
        tvDetailCustomerPhone.setText(customerPhone != null ? customerPhone : "0590000000");

        // عرض الرصيد القادم من الشاشة الرئيسية فوراً لمنع ظهور الصفر تماماً
        tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

        btnBack.setOnClickListener(v -> finish());

        debtItemList = new ArrayList<>();

        debtsAdapter = new DebtDetailsAdapter(debtItemList, new DebtDetailsAdapter.OnDebtClickListener() {
            @Override
            public void onEditClick(DebtItem debtItem) {
                showEditDebtDialog(debtItem);
            }

            @Override
            public void onDeleteClick(DebtItem debtItem) {
                showDeleteConfirmationDialog(debtItem);
            }
        });

        recyclerViewDebts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDebts.setAdapter(debtsAdapter);

        btnAddDebtItem.setOnClickListener(v -> addNewDebtItem());
        btnPayDebt.setOnClickListener(v -> showPaymentDialog());
        btnDownloadPdf.setOnClickListener(v -> generateAndSharePdf());

        // جلب الحركات (تعمل الآن بسلاسة حتى بدون إنترنت بفضل الكاش والترتيب البرمجي)
        loadCustomerDetailsAndDebts();
    }

    private void loadCustomerDetailsAndDebts() {
        if (customerId == null) return;

        db.collection("customers").document(customerId)
                .collection("debts")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "خطأ في جلب الحركات: " + error.getMessage());
                        return;
                    }

                    if (value == null || value.isEmpty()) {
                        return;
                    }

                    debtItemList.clear();
                    double calculatedTotal = 0.0;

                    for (QueryDocumentSnapshot doc : value) {
                        Boolean archived = doc.getBoolean("archived");
                        if (archived != null && archived) {
                            continue;
                        }

                        DebtItem item = doc.toObject(DebtItem.class);
                        item.setId(doc.getId());
                        item.setCustomerId(customerId);
                        debtItemList.add(item);

                        calculatedTotal += item.getAmount();
                    }

                    // ترتيب العناصر برمجياً من الأحدث للأقدم لضمان ظهورها بشكل صحيح بدون إنترنت
                    debtItemList.sort((item1, item2) -> Long.compare(item2.getTimestamp(), item1.getTimestamp()));

                    currentTotalDebt = Math.round(calculatedTotal * 100.0) / 100.0;
                    tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

                    debtsAdapter.notifyDataSetChanged();

                    // تحديث إجمالي الدين للمستند محلياً
                    db.collection("customers").document(customerId)
                            .update("totalDebt", currentTotalDebt)
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "فشل التحديث المحلي: " + e.getMessage());
                            });
                });
    }

    private void addNewDebtItem() {
        String amountStr = etAmount.getText().toString().trim();
        String itemName = etItemName.getText().toString().trim();

        if (amountStr.isEmpty() || itemName.isEmpty()) {
            Toast.makeText(this, "الرجاء إدخال المبلغ واسم الصنف", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        long currentTime = System.currentTimeMillis();

        currentTotalDebt = Math.round((currentTotalDebt + amount) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

        String debtId = db.collection("customers").document(customerId)
                .collection("debts").document().getId();

        DebtItem debtItem = new DebtItem(debtId, itemName, amount, currentTime, currentTime);
        debtItem.setCustomerId(customerId);

        debtItemList.add(0, debtItem);
        debtsAdapter.notifyDataSetChanged();

        etAmount.setText("");
        etItemName.setText("");
        etAmount.requestFocus();

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtId);
        debtMap.put("customerId", customerId);
        debtMap.put("itemName", itemName);
        debtMap.put("amount", amount);
        debtMap.put("timestamp", currentTime);
        debtMap.put("dueDate", currentTime);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(debtId);

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.set(debtRef, debtMap);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تمت إضافة الحركة وتحديث السجل بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "تم الحفظ محلياً", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تسجيل دفعة سداد جديدة");

        final TextInputEditText inputAmount = new TextInputEditText(this);
        inputAmount.setHint("أدخل مبلغ السداد (مثلاً: 10)");
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(inputAmount);

        builder.setView(layout);

        builder.setPositiveButton("تثبيت السداد", (dialog, which) -> {
            String amountStr = inputAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "الرجاء إدخال مبلغ السداد", Toast.LENGTH_SHORT).show();
                return;
            }

            double paymentAmount = Double.parseDouble(amountStr);
            if (paymentAmount <= 0) {
                Toast.makeText(this, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show();
                return;
            }

            processPaymentTransaction(paymentAmount);
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void processPaymentTransaction(double paymentAmount) {
        currentTotalDebt = Math.round((currentTotalDebt - paymentAmount) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

        String debtId = db.collection("customers").document(customerId)
                .collection("debts").document().getId();

        long currentTime = System.currentTimeMillis();
        DebtItem paymentItem = new DebtItem(debtId, "سداد دفعة نقدية", -paymentAmount, currentTime, currentTime);
        paymentItem.setCustomerId(customerId);

        debtItemList.add(0, paymentItem);
        debtsAdapter.notifyDataSetChanged();

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtId);
        debtMap.put("customerId", customerId);
        debtMap.put("itemName", "سداد دفعة نقدية");
        debtMap.put("amount", -paymentAmount);
        debtMap.put("timestamp", currentTime);
        debtMap.put("dueDate", currentTime);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(debtId);

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.set(debtRef, debtMap);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم تسجيل السداد بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "تم الحفظ محلياً", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditDebtDialog(DebtItem debtItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعديل الحركة");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final TextInputEditText inputName = new TextInputEditText(this);
        inputName.setHint("اسم الصنف أو الملاحظة");
        inputName.setText(debtItem.getItemName());
        layout.addView(inputName);

        final TextInputEditText inputAmount = new TextInputEditText(this);
        inputAmount.setHint("المبلغ");
        inputAmount.setText(String.valueOf(Math.abs(debtItem.getAmount())));
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(inputAmount);

        builder.setView(layout);

        builder.setPositiveButton("تعديل", (dialog, which) -> {
            String newName = inputName.getText().toString().trim();
            String newAmountStr = inputAmount.getText().toString().trim();

            if (newName.isEmpty() || newAmountStr.isEmpty()) {
                Toast.makeText(this, "الرجاء تعبئة الحقول", Toast.LENGTH_SHORT).show();
                return;
            }

            double newAmount = Double.parseDouble(newAmountStr);
            if (debtItem.getAmount() < 0) {
                newAmount = -newAmount;
            }

            double oldAmount = debtItem.getAmount();
            double diff = newAmount - oldAmount;

            currentTotalDebt = Math.round((currentTotalDebt + diff) * 100.0) / 100.0;
            tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

            debtItem.setItemName(newName);
            debtItem.setAmount(newAmount);
            debtsAdapter.notifyDataSetChanged();

            WriteBatch batch = db.batch();
            DocumentReference customerRef = db.collection("customers").document(customerId);
            DocumentReference debtRef = customerRef.collection("debts").document(debtItem.getId());

            batch.update(customerRef, "totalDebt", currentTotalDebt);
            batch.update(debtRef, "itemName", newName, "amount", newAmount);

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "تم تعديل الحركة بنجاح", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "تم التعديل محلياً", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void showDeleteConfirmationDialog(DebtItem debtItem) {
        new AlertDialog.Builder(this)
                .setTitle("نقل للأرشيف")
                .setMessage("هل أنت متأكد من نقل حركة (" + debtItem.getItemName() + ") إلى الأرشيف؟")
                .setPositiveButton("نقل للأرشيف", (dialog, which) -> {
                    executeDeleteDebtItem(debtItem);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void executeDeleteDebtItem(DebtItem item) {
        if (item.getId() == null) {
            Toast.makeText(this, "خطأ: معرف الحركة غير موجود", Toast.LENGTH_SHORT).show();
            return;
        }

        currentTotalDebt = Math.round((currentTotalDebt - item.getAmount()) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + currentTotalDebt);

        debtItemList.remove(item);
        debtsAdapter.notifyDataSetChanged();

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(item.getId());

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.update(debtRef, "archived", true, "deletedAt", System.currentTimeMillis());

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "تم نقل الحركة إلى الأرشيف بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "فشل نقل الحركة للأرشيف: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void generateAndSharePdf() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        int pageNumber = 1;

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(30, 30, 44));
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);

        Paint subTitlePaint = new Paint();
        subTitlePaint.setColor(Color.rgb(100, 100, 100));
        subTitlePaint.setTextSize(14);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(50, 50, 50));
        textPaint.setTextSize(14);

        Paint headerPaint = new Paint();
        headerPaint.setColor(Color.rgb(30, 30, 44));
        headerPaint.setTextSize(14);
        headerPaint.setFakeBoldText(true);

        int y = 50;

        canvas.drawText("كشف حساب زبون - متجر", 50, y, titlePaint);
        y += 30;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        canvas.drawText("تاريخ الاصدار: " + currentDate, 50, y, subTitlePaint);
        y += 40;

        canvas.drawText("اسم الزبون: " + (customerName != null ? customerName : ""), 50, y, textPaint);
        y += 25;
        canvas.drawText("رقم الهاتف: " + (customerPhone != null ? customerPhone : ""), 50, y, textPaint);
        y += 25;

        Paint totalPaint = new Paint();
        totalPaint.setColor(Color.rgb(229, 57, 53));
        totalPaint.setTextSize(18);
        totalPaint.setFakeBoldText(true);
        canvas.drawText("إجمالي المديونية الحالية: " + tvDetailTotalDebt.getText().toString(), 50, y, totalPaint);
        y += 40;

        Paint linePaint = new Paint();
        linePaint.setColor(Color.rgb(200, 200, 200));
        linePaint.setStrokeWidth(2);
        canvas.drawLine(50, y, 545, y, linePaint);
        y += 30;

        canvas.drawText("التاريخ والوقت", 50, y, headerPaint);
        canvas.drawText("الحركة / الصنف", 250, y, headerPaint);
        canvas.drawText("المبلغ (₪)", 450, y, headerPaint);
        y += 10;
        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        for (DebtItem item : debtItemList) {
            if (y > 780) {
                pdfDocument.finishPage(page);
                pageNumber++;

                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();

                y = 50;

                canvas.drawText("التاريخ والوقت", 50, y, headerPaint);
                canvas.drawText("الحركة / الصنف", 250, y, headerPaint);
                canvas.drawText("المبلغ (₪)", 450, y, headerPaint);
                y += 10;
                canvas.drawLine(50, y, 545, y, linePaint);
                y += 25;
            }

            String dateStr = sdf.format(new Date(item.getTimestamp()));
            canvas.drawText(dateStr, 50, y, textPaint);
            canvas.drawText(item.getItemName(), 250, y, textPaint);
            canvas.drawText(String.valueOf(item.getAmount()), 450, y, textPaint);
            y += 30;
        }

        pdfDocument.finishPage(page);

        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Customer_Statement_" + System.currentTimeMillis() + ".pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            pdfDocument.close();
            sharePdfFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "خطأ في إنشاء ملف PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            pdfDocument.close();
        }
    }

    private void sharePdfFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "مشاركة كشف الحساب عبر"));
    }
}