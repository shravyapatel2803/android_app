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
import com.example.billgenerator.fragments.GenerateBillFragment;
import com.example.billgenerator.models.item_recycler_model_stocks;
// Needs the correct model for the bill list
import com.example.billgenerator.models.SelectedItem; // <-- Changed from item_recycler_model


import java.util.ArrayList;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import androidx.recyclerview.widget.DiffUtil;
import java.util.List;

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


    private final Handler mainHandler = new Handler(Looper.getMainLooper());


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

    public void updateData(ArrayList<item_recycler_model_stocks> newData) {
        ArrayList<item_recycler_model_stocks> oldList = new ArrayList<>(filteredAvailableItemsList);
        
        databaseSystem.databaseExecutor.execute(() -> {
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() { return oldList.size(); }
                @Override
                public int getNewListSize() { return newData.size(); }
                @Override
                public boolean areItemsTheSame(int oldPos, int newPos) {
                    return oldList.get(oldPos).getId() == newData.get(newPos).getId();
                }
                @Override
                public boolean areContentsTheSame(int oldPos, int newPos) {
                    item_recycler_model_stocks oldItem = oldList.get(oldPos);
                    item_recycler_model_stocks newItem = newData.get(newPos);
                    return oldItem.getName().equals(newItem.getName()) && 
                           oldItem.getWeight() == newItem.getWeight() &&
                           oldItem.getType().equals(newItem.getType());
                }
            });

            mainHandler.post(() -> {
                allAvailableItemsList.clear();
                allAvailableItemsList.addAll(newData);
                filteredAvailableItemsList.clear();
                filteredAvailableItemsList.addAll(newData);
                diffResult.dispatchUpdatesTo(this);
            });
        });
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
        item_recycler_model_stocks model = filteredAvailableItemsList.get(position);

        holder.itemName.setText(model.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f g", model.getWeight()));
        
        if (holder.itemIcon != null) {
            holder.itemIcon.setImageResource("Gold".equalsIgnoreCase(model.getType()) ? 
                    R.drawable.ic_gold_ingot : R.drawable.ic_silver_bar);
        }

        if (holder.deleteButton != null) {
            holder.deleteButton.setVisibility(View.GONE);
        }

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
                    if (parentFragment instanceof GenerateBillFragment) {
                        ((GenerateBillFragment) parentFragment).updateSelectedItemsEmptyState();
                    }
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
        String searchText = (text == null) ? "" : text.toLowerCase(Locale.getDefault()).trim();
        ArrayList<item_recycler_model_stocks> oldList = new ArrayList<>(filteredAvailableItemsList);
        
        databaseSystem.databaseExecutor.execute(() -> {
            ArrayList<item_recycler_model_stocks> newList = new ArrayList<>();

            if (searchText.isEmpty()) {
                newList.addAll(allAvailableItemsList);
            } else {
                for (item_recycler_model_stocks item : allAvailableItemsList) {
                    if (item.getName().toLowerCase(Locale.getDefault()).contains(searchText) ||
                            item.getType().toLowerCase(Locale.getDefault()).contains(searchText)) {
                        newList.add(item);
                    }
                }
            }

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override public int getOldListSize() { return oldList.size(); }
                @Override public int getNewListSize() { return newList.size(); }
                @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
                }
                @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
                }
            });

            mainHandler.post(() -> {
                filteredAvailableItemsList.clear();
                filteredAvailableItemsList.addAll(newList);
                diffResult.dispatchUpdatesTo(this);
            });
        });
    }

    // --- ViewHolder ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemWeight;
        ImageView itemIcon;
        View deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            itemIcon = itemView.findViewById(R.id.item_icon);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}