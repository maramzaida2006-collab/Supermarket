package com.example.supermarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ArchivedCustomersAdapter extends RecyclerView.Adapter<ArchivedCustomersAdapter.ViewHolder> {

    private List<Customer> customerList;
    private OnArchivedCustomerListener listener;

    // تحديث الواجهة لتشمل النقر العادي (استعادة) والنقر المطول (حذف نهائي للزبون)
    public interface OnArchivedCustomerListener {
        void onRestoreClick(Customer customer);
        void onDeleteLongClick(Customer customer);
    }

    public ArchivedCustomersAdapter(List<Customer> customerList, OnArchivedCustomerListener listener) {
        this.customerList = customerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customerList.get(position);

        // ربط بيانات الزبون مع النصوص
        holder.tvCustomerName.setText(customer.getName() != null ? customer.getName() : "بدون اسم");
        holder.tvCustomerDebt.setText("إجمالي الدين: " + customer.getTotalDebt() + " شيكل");

        // 1. تفعيل زر الاستعادة الأخضر (النقر العادي)
        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreClick(customer);
            }
        });

        // 2. تفعيل الضغط المطول على كارد الزبون للحذف النهائي
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteLongClick(customer);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return customerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCustomerName, tvCustomerDebt;
        MaterialButton btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvArchivedCustomerName);
            tvCustomerDebt = itemView.findViewById(R.id.tvArchivedCustomerDebt);
            btnRestore = itemView.findViewById(R.id.btnRestoreCustomer);
        }
    }
}