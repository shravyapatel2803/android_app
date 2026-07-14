package com.example.billgenerator.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model;

import java.util.ArrayList;

public class item_recycler_adapter extends RecyclerView.Adapter<item_recycler_adapter.ViewHolder> {
    Context context;
    ArrayList<item_recycler_model> billItemsList;
    databaseSystem dbHelper; // Add database helper

    // <-- FIXED: Added 'public' to the constructor
    public item_recycler_adapter(Context context, ArrayList<item_recycler_model> arrayList) {
        this.context = context;
        this.billItemsList = arrayList;
        this.dbHelper = new databaseSystem(context); // Initialize
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // You are using 'add_item_recyclerview.xml' for the bill items
        View view = LayoutInflater.from(context).inflate(R.layout.add_item_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        item_recycler_model model = billItemsList.get(position);
        holder.itemName.setText(model.name);
        holder.itemWeight.setText(model.weight);
        holder.itemType.setText(model.type);

        holder.llm.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Remove Item")
                    .setMessage("Are you sure you want to remove this item from the bill?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        int pos = holder.getBindingAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            // Get the item before removing it
                            item_recycler_model removedItem = billItemsList.get(pos);

                            // 1. UPDATE THE DATABASE: Mark the item as NOT SOLD
                            dbHelper.updateItemSoldStatus(removedItem.id, false);

                            // 2. Remove from the bill list
                            billItemsList.remove(pos);
                            notifyItemRemoved(pos);
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return billItemsList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemWeight, itemType;
        LinearLayout llm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.itemName);
            itemWeight = itemView.findViewById(R.id.itemWeight);
            itemType = itemView.findViewById(R.id.itemType);
            llm = itemView.findViewById(R.id.llm);
        }
    }
}