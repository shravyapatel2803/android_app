package com.example.billgenerator.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.fragments.CustomerDetailsFragment;
import com.example.billgenerator.models.customer_recycler_model;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class customer_recycler_adapter extends RecyclerView.Adapter<customer_recycler_adapter.ViewHolder> implements Filterable {
    ArrayList<customer_recycler_model> arrayList;
    ArrayList<customer_recycler_model> arrayListFull;
    Context context;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN")); // Currency formatter
    private static final String TAG = "CustomerAdapter"; // For logging

    private CustomerDetailsFragment fragment;

    public customer_recycler_adapter(Context context, ArrayList<customer_recycler_model> arrayList, CustomerDetailsFragment fragment) {
        this.context = context;
        this.arrayList = arrayList;
        this.fragment = fragment;
        this.arrayListFull = new ArrayList<>(arrayList);
        Log.d(TAG, "Adapter created with " + arrayList.size() + " items.");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(context).inflate(R.layout.customer_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        customer_recycler_model model = arrayList.get(position);
        Log.d(TAG, "Binding view holder for position " + position + ", Customer: " + model.name);

        holder.customerName.setText(model.name);
        holder.customerPhone.setText(model.phone);
        holder.customerVillage.setText(model.village);

        holder.itemView.setOnClickListener(v -> {
            if (fragment != null) {
                fragment.showTransactionTimeline(model);
            }
        });

        if (model.debt > 0.001) {
            holder.customerDebt.setText(String.format("Debt: %s", currencyFormat.format(model.debt)));
            holder.customerDebt.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.customerDebt.setVisibility(View.VISIBLE);
        } else if (model.debt < -0.001) {
            holder.customerDebt.setText(String.format("Credit: %s", currencyFormat.format(Math.abs(model.debt))));
            holder.customerDebt.setTextColor(ContextCompat.getColor(context, R.color.teal_primary));
            holder.customerDebt.setVisibility(View.VISIBLE);
        }
        else {
            holder.customerDebt.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked for editing: " + model.name);
            if (fragment != null) {
                fragment.showEditCustomerDialog(model);
            } else {
                Log.e(TAG, "Fragment reference is null, cannot show edit dialog.");
            }
        });


        holder.itemView.setOnLongClickListener(v -> {
    Log.d(TAG, "Item long-clicked for deletion: " + model.name);
    new AlertDialog.Builder(context)
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete " + model.name + "?\n(This action cannot be undone)")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes", (dialog, which) -> {
                int currentPosition = holder.getBindingAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.w(TAG, "Deleting customer: " + arrayList.get(currentPosition).name);
                    databaseSystem db = new databaseSystem(context);
                    boolean isDeleted = db.deleteCustomer(arrayList.get(currentPosition).id);

                    if (isDeleted) {
                        arrayListFull.remove(arrayList.get(currentPosition));
                        arrayList.remove(currentPosition);
                        notifyItemRemoved(currentPosition);
                        Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Cannot delete customer with existing bills.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w(TAG, "Could not get adapter position for deletion.");
                }
            })
            .setNegativeButton("No", (dialog, which) -> Log.d(TAG, "Deletion cancelled for: " + model.name))
            .show();
    return true;
});
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    @Override
    public Filter getFilter() {
        return customerFilter;
    }

    private final Filter customerFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<customer_recycler_model> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(arrayListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (customer_recycler_model item : arrayListFull) {
                    if (item.name.toLowerCase().contains(filterPattern) ||
                        item.phone.toLowerCase().contains(filterPattern) ||
                        item.village.toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            arrayList.clear();
            arrayList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerName;
        TextView customerPhone;
        TextView customerVillage;
        TextView customerDebt; 

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customer_name_textview);
            customerPhone = itemView.findViewById(R.id.customer_phone_textview);
            customerVillage = itemView.findViewById(R.id.customer_village_textview);
            customerDebt = itemView.findViewById(R.id.customer_debt_textview);
        }
    }

    public void updateList(ArrayList<customer_recycler_model> newList) {
        Log.d(TAG, "Updating adapter list with " + newList.size() + " items.");
        this.arrayList = newList;
        this.arrayListFull = new ArrayList<>(newList);
        notifyDataSetChanged();
    }
}
