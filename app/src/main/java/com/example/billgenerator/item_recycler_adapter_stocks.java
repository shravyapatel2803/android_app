package com.example.billgenerator;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Locale;

public class item_recycler_adapter_stocks extends RecyclerView.Adapter<item_recycler_adapter_stocks.ViewHolder> {
    Context context;
    ArrayList<item_recycler_model_stocks> itemList;
    databaseSystem dbHelper; // Add a database helper instance

    public item_recycler_adapter_stocks(Context context, ArrayList<item_recycler_model_stocks> itemList) {
        this.context = context;
        this.itemList = itemList;
        this.dbHelper = new databaseSystem(context); // Initialize the helper
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

        // Set icon based on item type
        if ("Gold".equalsIgnoreCase(model.getType())) {
            holder.itemIcon.setImageResource(R.drawable.ic_gold_ingot);
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_silver_bar);
        }

        // --- NEW: LOGIC FOR SOLD ITEMS ---
        if (model.isSold()) {
            // Item IS sold: Gray it out and show the overlay
            holder.layoutRoot.setAlpha(0.5f); // Make the whole item semi-transparent
            holder.soldIconOverlay.setVisibility(View.VISIBLE);
        } else {
            // Item IS NOT sold: Make sure it's fully visible and hide the overlay
            holder.layoutRoot.setAlpha(1.0f); // Full opacity
            holder.soldIconOverlay.setVisibility(View.GONE);
        }

        // --- NEW: Long-click listener to toggle sold status ---
        holder.itemView.setOnLongClickListener(v -> {
            // Determine the new status (the opposite of the current one)
            boolean newSoldStatus = !model.isSold();
            String message = newSoldStatus ? "Mark this item as sold?" : "Mark this item as available?";

            new AlertDialog.Builder(context)
                    .setTitle("Update Status")
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // 1. Update the database
                        dbHelper.updateItemSoldStatus(model.getId(), newSoldStatus);

                        // 2. Update the model in the list
                        model.isSold = newSoldStatus;

                        // 3. Notify the adapter that this specific item has changed
                        notifyItemChanged(holder.getBindingAdapterPosition());

                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true; // Indicate the long click was handled
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // --- UPDATED VIEWS ---
        ImageView itemIcon, soldIconOverlay;
        TextView itemName, itemWeight;
        ConstraintLayout layoutRoot; // Get the root layout to change its alpha

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            // --- NEW ---
            soldIconOverlay = itemView.findViewById(R.id.sold_icon_overlay);
            layoutRoot = itemView.findViewById(R.id.constraint_layout_root);
        }
    }
}
