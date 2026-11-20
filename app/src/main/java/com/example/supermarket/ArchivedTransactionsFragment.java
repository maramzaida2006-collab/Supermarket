package com.example.supermarket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class ArchivedTransactionsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArchivedTransactionsAdapter adapter;
    private List<DebtItem> archivedDebtsList;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_archived_list, container, false);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        recyclerView = view.findViewById(R.id.recyclerViewArchived);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        archivedDebtsList = new ArrayList<>();

        adapter = new ArchivedTransactionsAdapter(archivedDebtsList, new ArchivedTransactionsAdapter.OnArchivedDebtListener() {
            @Override
            public void onRestoreClick(DebtItem debt) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("استعادة الحركة")
                        .setMessage("هل أنت متأكد من رغبتك في استعادة حركة الدين (" + (debt.getItemName() != null ? debt.getItemName() : "دين") + ")؟")
                        .setPositiveButton("استعادة", (dialog, which) -> {
                            restoreDebt(debt);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }

            @Override
            public void onDeleteLongClick(DebtItem debt) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("حذف نهائي للحركة")
                        .setMessage("هل أنت متأكد من حذف حركة (" + (debt.getItemName() != null ? debt.getItemName() : "دين") + ") نهائياً؟")
                        .setPositiveButton("حذف نهائي", (dialog, which) -> {
                            deleteDebtPermanently(debt);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);
        loadArchivedDebts();

        return view;
    }

    private void loadArchivedDebts() {
        if (currentUserId == null) return;

        db.collection("customers")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(customerSnapshots -> {
                    archivedDebtsList.clear();

                    if (customerSnapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (var customerDoc : customerSnapshots) {
                        String customerId = customerDoc.getId();

                        customerDoc.getReference().collection("debts")
                                .whereEqualTo("archived", true)
                                .addSnapshotListener((debtSnapshots, error) -> {
                                    if (error != null || debtSnapshots == null) return;

                                    archivedDebtsList.removeIf(item -> customerId.equals(item.getCustomerId()));

                                    for (QueryDocumentSnapshot debtDoc : debtSnapshots) {
                                        DebtItem debt = debtDoc.toObject(DebtItem.class);
                                        debt.setId(debtDoc.getId());
                                        debt.setCustomerId(customerId);
                                        archivedDebtsList.add(debt);
                                    }
                                    adapter.notifyDataSetChanged();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل جلب الأرشيف محلياً: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void restoreDebt(DebtItem debt) {
        if (debt.getId() == null || debt.getCustomerId() == null) {
            Toast.makeText(getContext(), "خطأ: معرف الزبون أو الحركة مفقود", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference customerRef = db.collection("customers").document(debt.getCustomerId());
        DocumentReference debtRef = customerRef.collection("debts").document(debt.getId());

        // جلب بيانات الزبون من الكاش المحلي لمعرفة إجمالي الدين الحالي وإضافة قيمة الحركة المستعادة إليه فوراً
        customerRef.get().addOnSuccessListener(customerDoc -> {
            double currentTotal = 0.0;
            if (customerDoc.exists() && customerDoc.getDouble("totalDebt") != null) {
                currentTotal = customerDoc.getDouble("totalDebt");
            }

            // حساب الإجمالي الجديد بعد إضافة قيمة الحركة المستعادة
            double newTotalDebt = Math.round((currentTotal + debt.getAmount()) * 100.0) / 100.0;

            WriteBatch batch = db.batch();
            // 1. إعادة حالة الحركة إلى غير مؤرشفة
            batch.update(debtRef, "archived", false);
            // 2. تحديث إجمالي الدين للزبون في مستند الزبون لكي يتحدث في الشاشة الرئيسية فوراً
            batch.update(customerRef, "totalDebt", newTotalDebt);

            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        archivedDebtsList.remove(debt);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "تمت استعادة الحركة وتحديث رصيد الزبون بنجاح", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "فشل الاستعادة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            // في حال فشل القراءة، نقوم بعمل استعادة عادية للحركة على الأقل
            debtRef.update("archived", false).addOnSuccessListener(aVoid -> {
                archivedDebtsList.remove(debt);
                adapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "تمت الاستعادة بنجاح", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteDebtPermanently(DebtItem debt) {
        if (debt.getId() == null || debt.getCustomerId() == null) {
            Toast.makeText(getContext(), "خطأ: معرف الزبون أو الحركة مفقود", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("customers").document(debt.getCustomerId())
                .collection("debts").document(debt.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    archivedDebtsList.remove(debt);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "تم حذف الحركة نهائياً", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل الحذف النهائي: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}