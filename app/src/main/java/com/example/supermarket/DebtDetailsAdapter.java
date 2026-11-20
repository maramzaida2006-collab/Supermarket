package com.example.supermarket;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DebtDetailsAdapter extends RecyclerView.Adapter<DebtDetailsAdapter.DebtViewHolder> {

    private List<DebtItem> debtList;
    private OnDebtClickListener listener;

    public interface OnDebtClickListener {
        void onEditClick(DebtItem debtItem);
        void onDeleteClick(DebtItem debtItem);
    }

    public DebtDetailsAdapter(List<DebtItem> debtList, OnDebtClickListener listener) {
        this.debtList = debtList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DebtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt, parent, false);
        return new DebtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DebtViewHolder holder, int position) {
        DebtItem debt = debtList.get(position);
        holder.tvItemName.setText(debt.getItemName() != null ? debt.getItemName() : "بدون وصف");
        holder.tvItemAmount.setText(debt.getAmount() + " ₪");

        String formattedDate = new SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault()).format(new Date(debt.getTimestamp()));
        holder.tvItemDate.setText(formattedDate);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(debt);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(debt);
        });
    }

    @Override
    public int getItemCount() {
        return debtList.size();
    }

    public static class DebtViewHolder extends RecyclerView.ViewHolder {
        TextView tvItemName, tvItemAmount, tvItemDate;
        ImageView btnEdit, btnDelete;

        public DebtViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItemName = itemView.findViewById(R.id.tvDebtItemName);
            tvItemAmount = itemView.findViewById(R.id.tvDebtAmount);
            tvItemDate = itemView.findViewById(R.id.tvDebtDate);
            btnEdit = itemView.findViewById(R.id.btnEditDebt);
            btnDelete = itemView.findViewById(R.id.btnDeleteDebt);
        }
    }
}