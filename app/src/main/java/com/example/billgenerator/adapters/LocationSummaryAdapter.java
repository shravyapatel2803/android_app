package com.example.billgenerator.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.LocationSummaryItem;

import java.util.ArrayList;
import java.util.Locale;

public class LocationSummaryAdapter extends RecyclerView.Adapter<LocationSummaryAdapter.ViewHolder> {

    public interface OnLocationClickListener {
        void onLocationClick(LocationSummaryItem item);
    }

    private final ArrayList<LocationSummaryItem> items;
    private final OnLocationClickListener listener;

    public LocationSummaryAdapter(ArrayList<LocationSummaryItem> items, OnLocationClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location_summary, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationSummaryItem item = items.get(position);
        holder.villageName.setText(item.village);
        holder.customerCount.setText(String.format(Locale.getDefault(), "%d customer(s)", item.customerCount));
        holder.totalDebt.setText(String.format(Locale.getDefault(), "Total Debt: Rs %.2f", item.totalDebt));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLocationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView villageName;
        TextView customerCount;
        TextView totalDebt;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            villageName = itemView.findViewById(R.id.location_village_name);
            customerCount = itemView.findViewById(R.id.location_customer_count);
            totalDebt = itemView.findViewById(R.id.location_total_debt);
        }
    }
}
