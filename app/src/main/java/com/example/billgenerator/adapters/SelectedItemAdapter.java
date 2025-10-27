package com.example.billgenerator.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.SelectedItem;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Adapter for the list of items *added* to the bill in GenerateBillFragment.
 * Allows items to be removed.
 */
public class SelectedItemAdapter extends RecyclerView.Adapter<SelectedItemAdapter.ViewHolder> {

    private ArrayList<SelectedItem> selectedItems;
    private Runnable onTotalUpdate; // Callback to trigger total calculation
    private Context context;
    private databaseSystem dbHelper;

    public SelectedItemAdapter(ArrayList<SelectedItem> selectedItems, Runnable onTotalUpdate) {
        this.selectedItems = selectedItems;
        this.onTotalUpdate = onTotalUpdate;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        this.dbHelper = new databaseSystem(context);
        View view = LayoutInflater.from(context).inflate(R.layout.selected_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectedItem item = selectedItems.get(position);

        holder.itemName.setText(item.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f g", item.getWeight()));

        if ("Gold".equalsIgnoreCase(item.getType())) {
            holder.itemIcon.setImageResource(R.drawable.ic_gold_ingot);
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_silver_bar);
        }

        // Set listener for the remove button
        holder.removeButton.setOnClickListener(v -> {
            int currentPosition = holder.getBindingAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                // Get the item before removing it
                SelectedItem removedItem = selectedItems.get(currentPosition);

                // IMPORTANT: Mark the item as "not sold" in the database
                // so it can be added to stock again.
                dbHelper.updateItemSoldStatus(removedItem.getId(), false);

                // Remove item from the list
                selectedItems.remove(currentPosition);

                // Notify adapter
                notifyItemRemoved(currentPosition);

                // Trigger the callback to update the total
                if(onTotalUpdate != null) {
                    onTotalUpdate.run();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return selectedItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon;
        TextView itemName, itemWeight;
        ImageButton removeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            removeButton = itemView.findViewById(R.id.remove_item_button);
        }
    }
}