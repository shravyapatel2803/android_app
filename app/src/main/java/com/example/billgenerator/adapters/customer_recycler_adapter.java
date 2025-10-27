package com.example.billgenerator.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // For colors
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.fragments.CustomerDetailsFragment; // Import fragment
import com.example.billgenerator.models.customer_recycler_model;

import java.text.NumberFormat; // For currency formatting
import java.util.ArrayList;
import java.util.Locale; // For locale

public class customer_recycler_adapter extends RecyclerView.Adapter<customer_recycler_adapter.ViewHolder> {
    ArrayList<customer_recycler_model> arrayList;
    Context context;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN")); // Currency formatter
    private static final String TAG = "CustomerAdapter"; // For logging

    // Modify constructor to accept the Fragment reference for calling edit dialog
    private CustomerDetailsFragment fragment;

    // Constructor requires the fragment to call back to for editing
    public customer_recycler_adapter(Context context, ArrayList<customer_recycler_model> arrayList, CustomerDetailsFragment fragment) {
        this.context = context;
        this.arrayList = arrayList;
        this.fragment = fragment; // Store fragment reference
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

        // --- Display Debt ---
        Log.d(TAG, "Customer " + model.name + " Debt: " + model.debt);
        // Use a small tolerance for comparing floating point numbers to zero
        if (model.debt > 0.001) {
            holder.customerDebt.setText(String.format("Debt: %s", currencyFormat.format(model.debt)));
            holder.customerDebt.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.customerDebt.setVisibility(View.VISIBLE);
        } else if (model.debt < -0.001) {
            // Optionally handle credit/negative debt differently
            holder.customerDebt.setText(String.format("Credit: %s", currencyFormat.format(Math.abs(model.debt))));
            holder.customerDebt.setTextColor(ContextCompat.getColor(context, R.color.teal_primary)); // Use a theme color for credit
            holder.customerDebt.setVisibility(View.VISIBLE);
        }
        else {
            holder.customerDebt.setVisibility(View.GONE); // Hide if debt is zero or negligible
        }

        // --- Edit on Short Click ---
        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked for editing: " + model.name);
            if (fragment != null) {
                fragment.showEditCustomerDialog(model); // Call fragment's edit method
            } else {
                Log.e(TAG, "Fragment reference is null, cannot show edit dialog.");
            }
        });


        // --- Delete on Long Click (Existing) ---
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
                            // Consider adding checks here: Does customer have outstanding debt? Are there associated bills?
                            // For now, we proceed with deletion.
                            db.deleteCustomer(arrayList.get(currentPosition).id);
                            arrayList.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                            // Optional: Notify item range changed if positions shift significantly
                            // notifyItemRangeChanged(currentPosition, arrayList.size());
                            Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Could not get adapter position for deletion.");
                        }
                    })
                    .setNegativeButton("No", (dialog, which) -> Log.d(TAG, "Deletion cancelled for: " + model.name))
                    .show();
            return true; // Consume the long click
        });
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    // ViewHolder remains the same, just ensure customer_debt_textview ID is correct
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerName;
        TextView customerPhone;
        TextView customerVillage;
        TextView customerDebt; // <-- Added Debt TextView

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customer_name_textview);
            customerPhone = itemView.findViewById(R.id.customer_phone_textview);
            customerVillage = itemView.findViewById(R.id.customer_village_textview);
            customerDebt = itemView.findViewById(R.id.customer_debt_textview); // <-- Find Debt TextView
        }
    }

    // Helper method to update the list, used after editing or adding
    public void updateList(ArrayList<customer_recycler_model> newList) {
        Log.d(TAG, "Updating adapter list with " + newList.size() + " items.");
        arrayList.clear();
        arrayList.addAll(newList);
        notifyDataSetChanged(); // Simple way to refresh the whole list
    }
}