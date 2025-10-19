package com.example.billgenerator;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillHistoryAdapter extends RecyclerView.Adapter<BillHistoryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<BillHistoryModel> billList;
    private final databaseSystem dbHelper; // Database helper to fetch details

    public BillHistoryAdapter(Context context, ArrayList<BillHistoryModel> billList) {
        this.context = context;
        this.billList = billList;
        this.dbHelper = new databaseSystem(context); // Initialize the database helper
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.bill_history_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BillHistoryModel model = billList.get(position);

        holder.customerName.setText(model.customerName);
        holder.billId.setText(String.format(Locale.getDefault(), "Bill #%d", model.billId));
        holder.billDate.setText(model.billDate);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        holder.totalAmount.setText(currencyFormat.format(model.totalAmount));

        // --- CORRECTED CLICK LISTENER ---
        // It now simply calls the detailed dialog method, passing the correct bill ID.
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(context, "this feature is upcoming version", Toast.LENGTH_SHORT).show();;
        });
    }

    // This method creates and shows the dialog with all the bill details.
    // Helper method to format the date string for better display
    private String formatDialogDate(String dateStr) {
        // Input format from SQLite: YYYY-MM-DD HH:MM:SS
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        // Desired output format: DD MMM YYYY, hh:mm a
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return dateStr; // Fallback to the raw date string if parsing fails
        }
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView customerName, billId, billDate, totalAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customer_name_textview);
            billId = itemView.findViewById(R.id.bill_id_textview);
            billDate = itemView.findViewById(R.id.bill_date_textview);
            totalAmount = itemView.findViewById(R.id.total_amount_textview);
        }
    }
}
