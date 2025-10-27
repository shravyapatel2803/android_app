package com.example.billgenerator.adapters;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.BillGenerator;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model_stocks;
import com.example.billgenerator.models.item_recycler_model;

import java.util.ArrayList;
import java.util.Locale;

public class generate_add_item_adapter extends RecyclerView.Adapter<generate_add_item_adapter.ViewHolder> {
    Context context, mainClass;
    // Two lists are now needed for filtering
    private final ArrayList<item_recycler_model_stocks> allAvailableItemsList;
    private final ArrayList<item_recycler_model_stocks> filteredAvailableItemsList;
    ArrayList<item_recycler_model> billItemsList; // Items added to the bill
    databaseSystem dbHelper;
    Dialog parentDialog;

    // Updated Constructor
    // <-- FIXED: Added 'public' to the constructor
    public generate_add_item_adapter(Context context, Context mainClass, ArrayList<item_recycler_model_stocks> availableItemsList, ArrayList<item_recycler_model> billItemsList, Dialog parentDialog) {
        this.context = context;
        this.allAvailableItemsList = availableItemsList;
        // Initialize the filtered list with all items
        this.filteredAvailableItemsList = new ArrayList<>(availableItemsList);
        this.billItemsList = billItemsList;
        this.mainClass = mainClass;
        this.dbHelper = new databaseSystem(context);
        this.parentDialog = parentDialog;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // We now bind from the filtered list
        item_recycler_model_stocks model = filteredAvailableItemsList.get(position);

        holder.itemName.setText(model.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f g", model.getWeight()));
        // You can add a textview for type if you want, but for now it's not in the layout
        // holder.itemType.setText(model.getType());

        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;

            item_recycler_model_stocks selectedItem = filteredAvailableItemsList.get(currentPosition);

            // 1. Add item to the bill's list
            billItemsList.add(new item_recycler_model(selectedItem.getId(), selectedItem.getName(), String.valueOf(selectedItem.getWeight()), selectedItem.getType()));

            // 2. Update the BillGenerator's RecyclerView
            RecyclerView billRecyclerView = ((BillGenerator) mainClass).findViewById(R.id.item_recycler_view);
            if (billRecyclerView.getAdapter() != null) {
                billRecyclerView.getAdapter().notifyItemInserted(billItemsList.size() - 1);
            }

            // 3. Mark item as "sold" in the database
            dbHelper.updateItemSoldStatus(selectedItem.getId(), true);
            Toast.makeText(context, selectedItem.getName() + " added to bill.", Toast.LENGTH_SHORT).show();

            // 4. IMPORTANT: Remove the item from BOTH lists to prevent it from reappearing
            allAvailableItemsList.remove(selectedItem);
            filteredAvailableItemsList.remove(currentPosition);
            notifyItemRemoved(currentPosition);

            if (filteredAvailableItemsList.isEmpty() && allAvailableItemsList.isEmpty()) {
                parentDialog.dismiss();
            }
        });
    }

    @Override
    public int getItemCount() {
        // The adapter should only count the items in the filtered list
        return filteredAvailableItemsList.size();
    }

    // --- NEW: FILTER METHOD ---
    public void filter(String text) {
        filteredAvailableItemsList.clear();
        if (text.isEmpty()) {
            filteredAvailableItemsList.addAll(allAvailableItemsList);
        } else {
            text = text.toLowerCase(Locale.getDefault());
            for (item_recycler_model_stocks item : allAvailableItemsList) {
                // Search by item name or type
                if (item.getName().toLowerCase(Locale.getDefault()).contains(text) ||
                        item.getType().toLowerCase(Locale.getDefault()).contains(text)) {
                    filteredAvailableItemsList.add(item);
                }
            }
        }
        // Notify the adapter that the dataset has changed
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemWeight;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
        }
    }
}