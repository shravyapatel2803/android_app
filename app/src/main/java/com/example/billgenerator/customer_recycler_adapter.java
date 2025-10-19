package com.example.billgenerator;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class customer_recycler_adapter extends RecyclerView.Adapter<customer_recycler_adapter.ViewHolder> {
    ArrayList<customer_recycler_model> arrayList;
    Context context;

    // Simplified constructor: You only need one context. The main activity's context is enough.
    public customer_recycler_adapter(Context context, ArrayList<customer_recycler_model> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your professional item layout
        // Make sure you have this layout file: res/layout/customer_layout_recyclerview.xml
        View view = LayoutInflater.from(context).inflate(R.layout.customer_layout_recyclerview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Get the customer model for the current position
        customer_recycler_model model = arrayList.get(position);

        // Set the data from the model to the TextViews
        holder.customerName.setText(model.name);
        holder.customerPhone.setText(model.phone);
        holder.customerVillage.setText(model.village);

        // Set a long-click listener to handle deletion
        holder.itemView.setOnLongClickListener(v -> {
            // Show a confirmation dialog before deleting
            new AlertDialog.Builder(context)
                    .setTitle("Delete Customer")
                    .setMessage("Are you sure you want to delete " + model.name + "?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Get the position in a safe way
                        int currentPosition = holder.getBindingAdapterPosition();

                        // Always check if the position is valid before using it
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            // 1. Delete the customer from the database
                            databaseSystem db = new databaseSystem(context);
                            // Use the model from the list at the specific position to get the ID
                            db.deleteCustomer(arrayList.get(currentPosition).id);

                            // 2. Remove the customer from the list in the adapter
                            arrayList.remove(currentPosition);

                            // 3. Notify the RecyclerView that an item has been removed
                            notifyItemRemoved(currentPosition);

                            Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("No", null) // "No" button does nothing
                    .show();

            return true; // Consume the long click event
        });
    }

    @Override
    public int getItemCount() {
        // Return the actual size of your list, not 0
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // Declare the views from your item layout
        TextView customerName;
        TextView customerPhone;
        TextView customerVillage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the views by their ID from customer_layout_recyclerview.xml
            // Make sure these IDs match your layout file
            customerName = itemView.findViewById(R.id.customer_name_textview);
            customerPhone = itemView.findViewById(R.id.customer_phone_textview);
            customerVillage = itemView.findViewById(R.id.customer_village_textview);
        }
    }
}
