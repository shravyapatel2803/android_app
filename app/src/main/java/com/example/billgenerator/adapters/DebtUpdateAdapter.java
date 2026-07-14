package com.example.billgenerator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.DebtUpdateItem;

import java.util.ArrayList;
import java.util.Locale;

public class DebtUpdateAdapter extends RecyclerView.Adapter<DebtUpdateAdapter.ViewHolder> {

    private final ArrayList<DebtUpdateItem> items;

    public DebtUpdateAdapter(ArrayList<DebtUpdateItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_update, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DebtUpdateItem item = items.get(position);
        String changeLabel = item.debtChange >= 0
                ? String.format(Locale.getDefault(), "Debt +Rs %.2f", item.debtChange)
                : String.format(Locale.getDefault(), "Credit Rs %.2f", Math.abs(item.debtChange));
        holder.changeText.setText(changeLabel);
        holder.balanceText.setText(String.format(Locale.getDefault(), "Balance: Rs %.2f", item.resultingBalance));
        holder.billText.setText(item.billId > 0
                ? String.format(Locale.getDefault(), "Bill #%d | Billed %.2f | Paid %.2f", item.billId, item.billedAmount, item.paidAmount)
                : item.note);
        holder.dateText.setText(item.createdAt == null ? "-" : item.createdAt);
        holder.noteText.setText(item.note == null || item.note.trim().isEmpty() ? "-" : item.note);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView changeText;
        TextView balanceText;
        TextView billText;
        TextView dateText;
        TextView noteText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            changeText = itemView.findViewById(R.id.debt_update_change);
            balanceText = itemView.findViewById(R.id.debt_update_balance);
            billText = itemView.findViewById(R.id.debt_update_bill_info);
            dateText = itemView.findViewById(R.id.debt_update_date);
            noteText = itemView.findViewById(R.id.debt_update_note);
        }
    }
}
