package com.example.billgenerator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.ReturnItem;

import java.util.ArrayList;
import java.util.Locale;

public class ReturnItemAdapter extends RecyclerView.Adapter<ReturnItemAdapter.ViewHolder> {

    private final ArrayList<ReturnItem> items;
    private final OnItemRemoveListener listener;

    public interface OnItemRemoveListener {
        void onRemove(int position);
    }

    public ReturnItemAdapter(ArrayList<ReturnItem> items, OnItemRemoveListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReturnItem item = items.get(position);
        holder.nameText.setText(String.format(Locale.getDefault(), "Return: %s", item.getType()));
        holder.weightText.setText(String.format(Locale.getDefault(), "%.3f g | Deduct: Rs %.2f", item.getWeight(), item.getDeductAmount()));
        holder.removeButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemove(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameText;
        TextView weightText;
        ImageButton removeButton;

        ViewHolder(View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.item_name_textview);
            weightText = itemView.findViewById(R.id.item_weight_textview);
            removeButton = itemView.findViewById(R.id.remove_item_button);
        }
    }
}
