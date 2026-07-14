package com.example.billgenerator.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.Item;

import java.util.List;
import java.util.Locale;

/**
 * This adapter is used by StockManagementFragment to display the list of all items.
 * Note: This is a simple version. Your "item_recycler_adapter_stocks" is more
 * advanced as it handles "isSold" status.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> itemList;
    private Context context;

    public ItemAdapter(List<Item> itemList) {
        this.itemList = itemList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item model = itemList.get(position);

        holder.itemName.setText(model.getName());
        holder.itemWeight.setText(String.format(Locale.getDefault(), "%.3f grams", model.getWeight()));

        // Set icon based on item type
        if ("Gold".equalsIgnoreCase(model.getType())) {
            holder.itemIcon.setImageResource(R.drawable.ic_gold_ingot);
        } else {
            holder.itemIcon.setImageResource(R.drawable.ic_silver_bar);
        }

        // This basic adapter doesn't show sold status, so hide the overlay
        holder.layoutRoot.setAlpha(1.0f);
        holder.soldIconOverlay.setVisibility(View.GONE);
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