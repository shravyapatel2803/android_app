package com.example.billgenerator.adapters;

import android.app.Dialog;
import android.content.Context;
import android.util.Log; // Added for logging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment; // <-- Import Fragment
import androidx.recyclerview.widget.RecyclerView;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model_stocks;
// Needs the correct model for the bill list
import com.example.billgenerator.models.SelectedItem; // <-- Changed from item_recycler_model


import java.util.ArrayList;
import java.util.Locale;

public class generate_add_item_adapter extends RecyclerView.Adapter<generate_add_item_adapter.ViewHolder> {
    // --- Context and Fragment Reference ---
    Context context;
    Fragment parentFragment; // <-- Changed from Context mainClass to Fragment parentFragment

    // --- Data Lists ---
    private final ArrayList<item_recycler_model_stocks> allAvailableItemsList; // Source list (unsold items)
    private final ArrayList<item_recycler_model_stocks> filteredAvailableItemsList; // List shown in dialog
    ArrayList<SelectedItem> billItemsList; // <-- Changed to use SelectedItem model (list IN the bill)

    // --- Helpers ---
    databaseSystem dbHelper;
    Dialog parentDialog; // The dialog this adapter lives in
    private static final String TAG = "GenAddItemAdapter"; // For logging


    // --- Updated Constructor ---
    public generate_add_item_adapter(Context context, Fragment fragment, // <-- Takes Fragment now
                                     ArrayList<item_recycler_model_stocks> availableItemsList,
                                     ArrayList<SelectedItem> billItemsList, // <-- Expects SelectedItem list
                                     Dialog parentDialog) {
        this.context = context;
        this.parentFragment = fragment; // <-- Store Fragment reference
        // Use copies to avoid modifying original lists directly if passed by reference elsewhere
        this.allAvailableItemsList = new ArrayList<>(availableItemsList); // Use a copy for source
        this.filteredAvailableItemsList = new ArrayList<>(availableItemsList); // Initialize filtered list
        this.billItemsList = billItemsList; // <-- Store reference to bill's item list
        this.dbHelper = new databaseSystem(context);
        this.parentDialog = parentDialog;
        Log.d(TAG, "Adapter created. Available items source: " + this.allAvailableItemsList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout_recyclerview, parent, false);
        // Ensure the layout used here (item_layout_recyclerview) has the correct IDs:
        // item_name_textview, item_weight_textview, item_icon, sold_icon_overlay
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind data from the filtered list (items available to be added)
        item_recycler_model_stocks model = filteredAvailableItemsList.get(position);

        holder.itemName.setText(model.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f g", model.getWeight()));
        // TODO: Add logic for itemIcon and soldIconOverlay if they exist in item_layout_recyclerview
        // e.g., holder.itemIcon.setImageResource(model.getType().equalsIgnoreCase("Gold") ? R.drawable.ic_gold_ingot : R.drawable.ic_silver_bar);
        // holder.soldIconOverlay.setVisibility(View.GONE); // Should always be hidden here

        // --- Click Listener to Add Item ---
        holder.itemView.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                Log.w(TAG, "Clicked item at NO_POSITION");
                return;
            }

            // Get the selected item *from the filtered list*
            // Use try-catch for safety in case list changes unexpectedly
            item_recycler_model_stocks selectedItem;
            try {
                selectedItem = filteredAvailableItemsList.get(currentPosition);
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "IndexOutOfBoundsException getting item at position: " + currentPosition);
                return; // Cannot proceed
            }

            Log.d(TAG, "Item clicked: ID=" + selectedItem.getId() + ", Name=" + selectedItem.getName());

            // --- Prevent Adding Duplicates to the Current Bill ---
            boolean alreadyAdded = false;
            for (SelectedItem billItem : billItemsList) {
                if (billItem.getId() == selectedItem.getId()) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (alreadyAdded) {
                Log.w(TAG, "Item ID " + selectedItem.getId() + " is already in the bill list.");
                Toast.makeText(context, selectedItem.getName() + " already added.", Toast.LENGTH_SHORT).show();
                return; // Stop processing if already added
            }

            // 1. Add item to the bill's list (using SelectedItem model)
            billItemsList.add(new SelectedItem(selectedItem.getId(), selectedItem.getName(), selectedItem.getWeight(), selectedItem.getType()));
            Log.d(TAG, "Added item ID " + selectedItem.getId() + " to billItemsList. New size: " + billItemsList.size());


            // 2. Update the BillFragment's RecyclerView
            if (parentFragment != null && parentFragment.getView() != null) {
                RecyclerView billRecyclerView = parentFragment.getView().findViewById(R.id.item_recycler_view);
                if (billRecyclerView != null && billRecyclerView.getAdapter() != null) {
                    billRecyclerView.getAdapter().notifyItemInserted(billItemsList.size() - 1);
                    Log.d(TAG, "Notified bill's RecyclerView adapter (item inserted).");
                } else {
                    Log.w(TAG, "Could not find bill's RecyclerView (R.id.item_recycler_view) or its adapter.");
                }
            } else {
                Log.e(TAG, "Parent fragment or its view is null. Cannot update bill RecyclerView.");
            }


            // 3. Mark item as "sold" in the database
            dbHelper.updateItemSoldStatus(selectedItem.getId(), true);
            Log.d(TAG, "Marked item ID " + selectedItem.getId() + " as sold in DB.");
            Toast.makeText(context, selectedItem.getName() + " added to bill.", Toast.LENGTH_SHORT).show();

            // 4. IMPORTANT: Remove the item from *this adapter's lists* (filtered and source)
            // It's critical to remove the correct object from the source list.
            boolean removedFromSource = false;
            // Iterate source list to find the matching object by ID (safer than relying on object equality if objects were recreated)
            item_recycler_model_stocks itemToRemoveFromSource = null;
            for(item_recycler_model_stocks sourceItem : allAvailableItemsList) {
                if(sourceItem.getId() == selectedItem.getId()) {
                    itemToRemoveFromSource = sourceItem;
                    break;
                }
            }
            if (itemToRemoveFromSource != null) {
                removedFromSource = allAvailableItemsList.remove(itemToRemoveFromSource);
            }

            // Also remove from the currently displayed list by position
            filteredAvailableItemsList.remove(currentPosition);

            // Notify this adapter (generate_add_item_adapter) that the item was removed
            notifyItemRemoved(currentPosition);
            // Optional: If positions shift, might need notifyItemRangeChanged
            // notifyItemRangeChanged(currentPosition, filteredAvailableItemsList.size());

            Log.d(TAG, "Removed item ID " + selectedItem.getId() + " from adapter lists. Removed from source: " + removedFromSource);


            // 5. Check if the dialog should be dismissed
            if (allAvailableItemsList.isEmpty()) {
                Log.d(TAG, "All available items added, dismissing dialog.");
                if (parentDialog != null && parentDialog.isShowing()) {
                    parentDialog.dismiss();
                }
            } else if (filteredAvailableItemsList.isEmpty()){
                Log.d(TAG, "Filtered list is empty, source list size: " + allAvailableItemsList.size());
                Toast.makeText(context, "No more items match search.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        // Count items in the currently displayed (filtered) list
        return filteredAvailableItemsList.size();
    }

    // --- Filter Method ---
    public void filter(String text) {
        Log.d(TAG, "Filtering list with text: '" + text + "'");
        filteredAvailableItemsList.clear(); // Clear the displayed list
        String searchText = (text == null) ? "" : text.toLowerCase(Locale.getDefault());

        if (searchText.isEmpty()) {
            // If search is empty, show all items from the source list
            filteredAvailableItemsList.addAll(allAvailableItemsList);
            Log.d(TAG, "Filter empty, showing all " + allAvailableItemsList.size() + " available items.");
        } else {
            // Otherwise, iterate through the source list and add matches
            for (item_recycler_model_stocks item : allAvailableItemsList) {
                if (item.getName().toLowerCase(Locale.getDefault()).contains(searchText) ||
                        item.getType().toLowerCase(Locale.getDefault()).contains(searchText)) {
                    filteredAvailableItemsList.add(item);
                }
            }
            Log.d(TAG, "Filter applied, showing " + filteredAvailableItemsList.size() + " matching items.");
        }
        notifyDataSetChanged();
        // Notify the adapter that the displayed list has changed
    }

    // --- ViewHolder ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemWeight;
        // Add ImageView references if they exist in item_layout_recyclerview.xml
        // ImageView itemIcon, soldIconOverlay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs exist in R.layout.item_layout_recyclerview
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            // itemIcon = itemView.findViewById(R.id.item_icon);
            // soldIconOverlay = itemView.findViewById(R.id.sold_icon_overlay);
        }
    }
}