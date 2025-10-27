package com.example.billgenerator.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model_stocks;

import java.util.ArrayList;
import java.util.Locale;

public class item_recycler_adapter_stocks extends RecyclerView.Adapter<item_recycler_adapter_stocks.ViewHolder> {
    Context context;
    ArrayList<item_recycler_model_stocks> itemList;
    databaseSystem dbHelper;

    // --- NEW: Listener for state changes ---
    private final OnItemStatusChangedListener statusChangedListener;

    // --- NEW: Interface for the listener ---
    public interface OnItemStatusChangedListener {
        void onItemStatusChanged();
    }

    public item_recycler_adapter_stocks(Context context, ArrayList<item_recycler_model_stocks> itemList, OnItemStatusChangedListener listener) {
        this.context = context;
        this.itemList = itemList;
        this.dbHelper = new databaseSystem(context);
        this.statusChangedListener = listener; // Initialize the listener
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        item_recycler_model_stocks model = itemList.get(position);

        holder.itemName.setText(model.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f grams", model.getWeight()));

        if ("Gold".equalsIgnoreCase(model.getType())) {
            holder.itemIcon.setImageResource(R.drawable.ic_gold_ingot);
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_silver_bar);
        }

        if (model.isSold()) {
            holder.layoutRoot.setAlpha(0.5f);
            holder.soldIconOverlay.setVisibility(View.VISIBLE);
        } else {
            holder.layoutRoot.setAlpha(1.0f);
            holder.soldIconOverlay.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            boolean newSoldStatus = !model.isSold();
            String message = newSoldStatus ? "Mark this item as sold?" : "Mark this item as available?";

            new AlertDialog.Builder(context)
                    .setTitle("Update Status")
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // 1. Update the database
                        dbHelper.updateItemSoldStatus(model.getId(), newSoldStatus);
                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show();

                        // 2. --- MODIFIED: Notify the activity to reload the data ---
                        if (statusChangedListener != null) {
                            statusChangedListener.onItemStatusChanged();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon, soldIconOverlay;
        TextView itemName, itemWeight;
        ConstraintLayout layoutRoot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            soldIconOverlay = itemView.findViewById(R.id.sold_icon_overlay);
            layoutRoot = itemView.findViewById(R.id.constraint_layout_root);
        }
    }
}