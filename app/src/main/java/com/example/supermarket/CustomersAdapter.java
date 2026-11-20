package com.example.supermarket;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class CustomersAdapter extends RecyclerView.Adapter<CustomersAdapter.CustomerViewHolder> {

    private List<Customer> customerList;
    private OnCustomerClickListener listener;

    public interface OnCustomerClickListener {
        void onCustomerClick(Customer customer);
    }

    public CustomersAdapter(List<Customer> customerList, OnCustomerClickListener listener) {
        this.customerList = customerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CustomerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_customer, parent, false);
        return new CustomerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CustomerViewHolder holder, int position) {
        Customer customer = customerList.get(position);
        holder.tvName.setText(customer.getName());
        holder.tvPhone.setText(customer.getPhone() != null && !customer.getPhone().isEmpty() ? customer.getPhone() : "بدون رقم");
        holder.tvTotalDebt.setText(customer.getTotalDebt() + " شيكل");

        long currentTime = System.currentTimeMillis();
        long dueDate = customer.getDueDate();
        double debt = customer.getTotalDebt();

        if (debt <= 0) {
            ViewCompat.setBackgroundTintList(holder.viewRiskIndicator, ColorStateList.valueOf(Color.parseColor("#10B981")));
        } else if (dueDate == 0 || dueDate > currentTime) {
            ViewCompat.setBackgroundTintList(holder.viewRiskIndicator, ColorStateList.valueOf(Color.parseColor("#22C55E")));
        } else {
            long diffDays = (currentTime - dueDate) / (1000 * 60 * 60 * 24);
            if (diffDays <= 7) {
                ViewCompat.setBackgroundTintList(holder.viewRiskIndicator, ColorStateList.valueOf(Color.parseColor("#EAB308")));
            } else {
                ViewCompat.setBackgroundTintList(holder.viewRiskIndicator, ColorStateList.valueOf(Color.parseColor("#EF4444")));
            }
        }

        holder.itemView.setOnClickListener(v -> listener.onCustomerClick(customer));

        // تعديل الضغط المطول ليصبح "أرشفة" بدلاً من "الحذف النهائي"
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("نقل إلى الأرشيف")
                    .setMessage("هل أنت متأكد من نقل الزبون (" + customer.getName() + ") إلى الأرشيف؟ لن يتم حذفه نهائياً وستتمكن من استرجاعه لاحقاً.")
                    .setPositiveButton("أرشفة", (dialog, which) -> {
                        if (customer.getId() == null) return;

                        // تحديث حالة الأرشفة بدلاً من المسح نهائياً
                        FirebaseFirestore.getInstance().collection("customers")
                                .document(customer.getId())
                                .update("archived", true, "deletedAt", System.currentTimeMillis())
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(v.getContext(), "تم نقل الزبون إلى الأرشيف بنجاح", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(v.getContext(), "فشل الأرشفة: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("إلغاء", null)
                    .show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public static class CustomerViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvTotalDebt;
        View viewRiskIndicator;

        public CustomerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCustomerPhone);
            tvTotalDebt = itemView.findViewById(R.id.tvTotalDebt);
            viewRiskIndicator = itemView.findViewById(R.id.viewRiskIndicator);
        }
    }
}