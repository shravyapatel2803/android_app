package com.example.billgenerator.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
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
    Fragment parentFragment;

    public item_recycler_adapter_stocks(Context context, ArrayList<item_recycler_model_stocks> itemList, Fragment fragment) {
        this.context = context;
        this.itemList = itemList;
        this.dbHelper = new databaseSystem(context);
        this.parentFragment = fragment;
    }

    public item_recycler_adapter_stocks(Context context, ArrayList<item_recycler_model_stocks> itemList) {
        this(context, itemList, null);
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
        holder.itemType.setText(model.getType().toUpperCase());
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

        holder.itemView.setOnClickListener(v -> {
            if (parentFragment instanceof com.example.billgenerator.fragments.StockManagementFragment) {
                ((com.example.billgenerator.fragments.StockManagementFragment) parentFragment).showEditItemDialog(model);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            boolean newSoldStatus = !model.isSold();
            String message = newSoldStatus ? "Mark this item as sold?" : "Mark this item as available?";

            new AlertDialog.Builder(context)
                    .setTitle("Update Status")
                    .setMessage(message)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.updateItemSoldStatus(model.getId(), newSoldStatus);
                        model.isSold = newSoldStatus;
                        notifyItemChanged(holder.getBindingAdapterPosition());
                        Toast.makeText(context, "Status Updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });

        holder.deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete this item?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteItem(model.getId());
                        itemList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, itemList.size());
                        Toast.makeText(context, "Item Deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public interface OnItemStatusChangedListener {

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemIcon, soldIconOverlay;
        TextView itemName, itemType, itemWeight;
        ConstraintLayout layoutRoot;
        ImageButton deleteButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemIcon = itemView.findViewById(R.id.item_icon);
            itemName = itemView.findViewById(R.id.item_name_textview);
            itemType = itemView.findViewById(R.id.item_type_textview);
            itemWeight = itemView.findViewById(R.id.item_weight_textview);
            soldIconOverlay = itemView.findViewById(R.id.sold_icon_overlay);
            layoutRoot = itemView.findViewById(R.id.constraint_layout_root);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
