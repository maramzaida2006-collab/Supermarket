package com.example.supermarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class ArchivedTransactionsAdapter extends RecyclerView.Adapter<ArchivedTransactionsAdapter.ViewHolder> {

    private List<DebtItem> debtList;
    private OnArchivedDebtListener listener;

    // تحديث الواجهة لتشمل النقر العادي (استعادة) والنقر المطول (حذف نهائي)
    public interface OnArchivedDebtListener {
        void onRestoreClick(DebtItem debt);
        void onDeleteLongClick(DebtItem debt);
    }

    public ArchivedTransactionsAdapter(List<DebtItem> debtList, OnArchivedDebtListener listener) {
        this.debtList = debtList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_archived, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DebtItem debt = debtList.get(position);

        // ربط البيانات مع النصوص في التصميم
        holder.tvItemName.setText(debt.getItemName() != null ? debt.getItemName() : "بدون صنف");
        holder.tvCustomerRef.setText("حركة دين مؤرشفة");
        holder.tvAmount.setText("المبلغ: " + debt.getAmount() + " شيكل");

        // 1. تفعيل زر الاستعادة الأخضر (الضغط العادي)
        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRestoreClick(debt);
            }
        });

        // 2. تفعيل الضغط المطول على العنصر بالكامل للحذف النهائي
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteLongClick(debt);
            }
            return true; // إرجاع true يعني أنه تم التعامل مع الضغطة المطولة بنجاح
        });
    }

    @Override
    public int getItemCount() {
        return debtList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvCustomerRef, tvAmount;
        MaterialButton btnRestore;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvArchivedItemName);
            tvCustomerRef = itemView.findViewById(R.id.tvArchivedCustomerRef);
            tvAmount = itemView.findViewById(R.id.tvArchivedAmount);
            btnRestore = itemView.findViewById(R.id.btnRestoreTransaction);
        }
    }
}