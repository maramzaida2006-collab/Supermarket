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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.List;

public class ArchivedCustomersFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArchivedCustomersAdapter adapter;
    private List<Customer> archivedCustomersList;
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

        archivedCustomersList = new ArrayList<>();

        // ربط الـ Adapter مع إضافة رسالة تأكيد الاستعادة ورسالة تأكيد الحذف النهائي
        adapter = new ArchivedCustomersAdapter(archivedCustomersList, new ArchivedCustomersAdapter.OnArchivedCustomerListener() {
            @Override
            public void onRestoreClick(Customer customer) {
                // إظهار نافذة تأكيد استعادة الزبون
                new AlertDialog.Builder(requireContext())
                        .setTitle("استعادة الزبون")
                        .setMessage("هل أنت متأكد من رغبتك في استعادة الزبون (" + (customer.getName() != null ? customer.getName() : "بدون اسم") + ") إلى القائمة النشطة؟")
                        .setPositiveButton("استعادة", (dialog, which) -> {
                            restoreCustomer(customer);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }

            @Override
            public void onDeleteLongClick(Customer customer) {
                // إظهار نافذة تأكيد الحذف النهائي عند الضغط المطول على الزبون
                new AlertDialog.Builder(requireContext())
                        .setTitle("حذف نهائي للزبون")
                        .setMessage("هل أنت متأكد من حذف الزبون (" + (customer.getName() != null ? customer.getName() : "بدون اسم") + ") نهائياً مع كافة ديونه؟ لا يمكن التراجع عن هذا الإجراء.")
                        .setPositiveButton("حذف نهائي", (dialog, which) -> {
                            deleteCustomerPermanently(customer);
                        })
                        .setNegativeButton("إلغاء", null)
                        .show();
            }
        });

        recyclerView.setAdapter(adapter);
        loadArchivedCustomers();

        return view;
    }

    private void loadArchivedCustomers() {
        if (currentUserId == null) return;

        db.collection("customers")
                .whereEqualTo("userId", currentUserId)
                .whereEqualTo("archived", true) // الزبائن المؤرشفون فقط
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    archivedCustomersList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Customer customer = doc.toObject(Customer.class);
                            customer.setId(doc.getId());
                            archivedCustomersList.add(customer);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void restoreCustomer(Customer customer) {
        if (customer.getId() == null) return;

        db.collection("customers").document(customer.getId())
                .update("archived", false, "deletedAt", 0L)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "تمت استعادة الزبون بنجاح", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "فشل الاستعادة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteCustomerPermanently(Customer customer) {
        if (customer.getId() == null) return;

        // حذف كافة الديون التابعة للزبون أولاً ثم حذف مستند الزبون نهائياً
        db.collection("customers").document(customer.getId())
                .collection("debts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (var doc : queryDocumentSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    // حذف الزبون نفسه من الـ Firestore
                    batch.delete(db.collection("customers").document(customer.getId()));

                    batch.commit().addOnSuccessListener(aVoid -> {
                        archivedCustomersList.remove(customer);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "تم حذف الزبون نهائياً", Toast.LENGTH_SHORT).show();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "فشل الحذف النهائي: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "خطأ أثناء جلب ديون الزبون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}