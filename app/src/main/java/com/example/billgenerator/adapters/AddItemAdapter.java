package com.example.billgenerator.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// <-- FIXED: Added import for databaseSystem
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.Item;

import java.util.List;
import java.util.Locale;

/**
 * Adapter for the dialog in GenerateBillFragment that shows all available items from stock.
 */
public class AddItemAdapter extends RecyclerView.Adapter<AddItemAdapter.ViewHolder> {

    // Listener interface for when an item is clicked
    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private List<Item> allItems;
    private OnItemClickListener listener;
    private Context context;
    private databaseSystem dbHelper; // <-- FIXED: Added dbHelper instance

    public AddItemAdapter(List<Item> allItems, OnItemClickListener listener) {
        this.allItems = allItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        // <-- FIXED: Initialize dbHelper here
        this.dbHelper = new databaseSystem(context);
        // Use the same layout as the stock management list
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = allItems.get(position);

        holder.itemName.setText(item.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f g", item.getWeight()));

        if ("Gold".equalsIgnoreCase(item.getType())) {
            holder.itemIcon.setImageResource(R.drawable.ic_gold_ingot);
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_silver_bar);
        }

        // Hide the "sold" overlay, as this list is only for adding available items
        holder.soldIconOverlay.setVisibility(View.GONE);

        // Set the click listener for the whole item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // <-- FIXED: Mark the item as sold in the database BEFORE calling the listener
                dbHelper.updateItemSoldStatus(item.getId(), true);
                listener.onItemClick(item);

                // <-- FIXED: Remove the item from the adapter's list immediately
                // This prevents adding the same item twice before the dialog closes
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    allItems.remove(currentPosition);
                    notifyItemRemoved(currentPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return allItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon, soldIconOverlay;
        TextView itemName, itemWeight;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            soldIconOverlay = itemView.findViewById(R.id.sold_icon_overlay);
        }
    }
}