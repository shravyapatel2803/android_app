package com.example.billgenerator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.NotificationHistoryItem;

import java.util.ArrayList;

public class NotificationHistoryAdapter extends RecyclerView.Adapter<NotificationHistoryAdapter.ViewHolder> {

    private final ArrayList<NotificationHistoryItem> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(NotificationHistoryItem item);
    }

    public NotificationHistoryAdapter(ArrayList<NotificationHistoryItem> items) {
        this.items = items;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationHistoryItem item = items.get(position);
        holder.customerText.setText(item.customerName + " (Bill #" + item.billId + ")");
        holder.messageText.setText(item.message);
        holder.metaText.setText("Due: " + safe(item.notifiedDate) + " • Logged: " + safe(item.createdAt));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerText;
        TextView messageText;
        TextView metaText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerText = itemView.findViewById(R.id.item_notification_customer);
            messageText = itemView.findViewById(R.id.item_notification_message);
            metaText = itemView.findViewById(R.id.item_notification_meta);
        }
    }
}
