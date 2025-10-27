package com.example.billgenerator.adapters;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView; // Import NestedScrollView
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.BillHistoryModel;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillHistoryAdapter extends RecyclerView.Adapter<BillHistoryAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<BillHistoryModel> billList;
    private final databaseSystem dbHelper;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final String TAG = "BillHistoryAdapter";

    public BillHistoryAdapter(Context context, ArrayList<BillHistoryModel> billList) {
        this.context = context;
        this.billList = billList;
        this.dbHelper = new databaseSystem(context);
        Log.d(TAG, "Adapter created with " + billList.size() + " bills.");
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
        holder.totalAmount.setText(currencyFormat.format(model.totalAmount));

        holder.itemView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked: Bill ID " + model.billId);
            showBillDetailDialog(model.billId);
        });
    }

    // --- Method to Show the Bill Detail Dialog ---
    private void showBillDetailDialog(int billId) {
        Log.d(TAG, "Showing detail dialog for Bill ID: " + billId);
        // <-- FIXED: Use standard Dialog constructor -->
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE); // Still remove default title bar
        dialog.setContentView(R.layout.bill_detail_dialog); // Use your custom layout

        // Make dialog width match parent and height wrap content
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            // Optional: Add background dimming
            // window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            // window.setDimAmount(0.6f);
        }


        // Find views inside the dialog layout
        Toolbar toolbar = dialog.findViewById(R.id.toolbar_bill_detail);
        TextView detailCustomerName = dialog.findViewById(R.id.detail_customer_name);
        TextView detailCustomerPhone = dialog.findViewById(R.id.detail_customer_phone);
        TextView detailBillId = dialog.findViewById(R.id.detail_bill_id);
        TextView detailBillDate = dialog.findViewById(R.id.detail_bill_date);
        TextView detailGoldRate = dialog.findViewById(R.id.detail_gold_rate);
        TextView detailSilverRate = dialog.findViewById(R.id.detail_silver_rate);
        TextView detailTotalAmount = dialog.findViewById(R.id.detail_total_amount);
        LinearLayout itemsContainer = dialog.findViewById(R.id.container_bill_items);
        // Find the parent of the rates/total section to add GST info
        View totalsSectionView = dialog.findViewById(R.id.detail_total_amount); // Get the total text view
        ViewGroup totalsSection = (totalsSectionView != null && totalsSectionView.getParent() instanceof ViewGroup)
                ? (ViewGroup) totalsSectionView.getParent() : null; // Get its parent


        // Check if all essential views were found
        if (toolbar == null || detailCustomerName == null || detailCustomerPhone == null || detailBillId == null ||
                detailBillDate == null || detailGoldRate == null || detailSilverRate == null ||
                detailTotalAmount == null || itemsContainer == null || totalsSection == null) {
            Log.e(TAG, "Error: One or more views not found in bill_detail_dialog.xml");
            Toast.makeText(context, "Error displaying bill details.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create GST TextView dynamically (better control than assuming parent structure)
        TextView detailGstInfo = new TextView(context);
        LinearLayout.LayoutParams gstParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gstParams.setMargins(0, 4, 0, 0); // Add some top margin
        detailGstInfo.setLayoutParams(gstParams);
        detailGstInfo.setGravity(android.view.Gravity.END);
        detailGstInfo.setTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2);
        detailGstInfo.setVisibility(View.GONE); // Initially hidden
        // Insert GST TextView before the Total Amount TextView
        int totalIndex = totalsSection.indexOfChild(detailTotalAmount);
        totalsSection.addView(detailGstInfo, totalIndex); // Add before total


        // Setup Toolbar
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        toolbar.setTitle("Bill #" + billId);


        // Fetch Bill Master Details
        Cursor billCursor = dbHelper.getBillDetails(billId);
        if (billCursor != null && billCursor.moveToFirst()) {
            Log.d(TAG, "Bill details found for ID: " + billId);
            try {
                detailCustomerName.setText(billCursor.getString(billCursor.getColumnIndexOrThrow("name")));
                detailCustomerPhone.setText(billCursor.getString(billCursor.getColumnIndexOrThrow("phone")));
                detailBillId.setText(String.format(Locale.getDefault(), "Bill #%d", billId));
                detailBillDate.setText(formatDialogDateTime(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date"))));

                double goldRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_gold_rate"));
                double silverRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_silver_rate"));
                double totalAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                double gstPercent = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));

                // Display rates
                detailGoldRate.setText(goldRate > 0 ? String.format(Locale.getDefault(), "Approx. Gold Rate: %s / 10g", currencyFormat.format(goldRate)) : "Gold Rate: N/A");
                detailSilverRate.setText(silverRate > 0 ? String.format(Locale.getDefault(), "Approx. Silver Rate: %s / kg", currencyFormat.format(silverRate)) : "Silver Rate: N/A");

                // Display GST info if applied
                if (gstPercent > 0.001) { // Use tolerance
                    detailGstInfo.setText(String.format(Locale.getDefault(), "GST Applied: %.2f%%", gstPercent));
                    detailGstInfo.setVisibility(View.VISIBLE);
                } else {
                    detailGstInfo.setVisibility(View.GONE);
                }

                detailTotalAmount.setText(String.format("Total: %s", currencyFormat.format(totalAmount)));

            } catch (Exception e) {
                Log.e(TAG, "Error reading bill details cursor: " + e.getMessage());
                Toast.makeText(context, "Error reading bill details.", Toast.LENGTH_SHORT).show();
            } finally {
                billCursor.close();
            }
        } else {
            Log.e(TAG, "Bill details cursor is null or empty for ID: " + billId);
            Toast.makeText(context, "Could not find bill details.", Toast.LENGTH_SHORT).show();
            if(billCursor != null) billCursor.close();
            dialog.dismiss();
            return;
        }

        // --- Fetch and Add Bill Items Dynamically ---
        itemsContainer.removeAllViews(); // Clear previous items
        Cursor itemCursor = dbHelper.getItemsForBill(billId);
        boolean itemsFound = false;
        if (itemCursor != null) {
            Log.d(TAG, "Found " + itemCursor.getCount() + " items for bill ID: " + billId);
            try {
                if (itemCursor.moveToFirst()) {
                    itemsFound = true;
                    int nameCol = itemCursor.getColumnIndexOrThrow("name");
                    int weightCol = itemCursor.getColumnIndexOrThrow("weight");
                    int typeCol = itemCursor.getColumnIndexOrThrow("type");

                    do {
                        String name = itemCursor.getString(nameCol);
                        double weight = itemCursor.getDouble(weightCol);
                        String type = itemCursor.getString(typeCol);

                        TextView itemTextView = new TextView(context);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 4, 0, 4);
                        itemTextView.setLayoutParams(params);
                        itemTextView.setText(String.format(Locale.getDefault(), "• %s (%s) - %.3f g", name, type, weight));
                        itemTextView.setTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Body1);
                        itemsContainer.addView(itemTextView);
                        Log.v(TAG, "Added item to dialog view: " + name);
                    } while (itemCursor.moveToNext());
                }
            } catch(Exception e) {
                Log.e(TAG, "Error reading item cursor: " + e.getMessage());
                itemsFound = false; // Mark as not found if error occurred during iteration
            } finally {
                itemCursor.close();
            }
        } else {
            Log.w(TAG, "getItemsForBill cursor is null for bill ID: " + billId);
        }

        // Add placeholder if no items were successfully added
        if (!itemsFound) {
            Log.d(TAG, "No items found or error reading items for bill ID: " + billId);
            TextView noItemsTextView = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            noItemsTextView.setLayoutParams(params);
            noItemsTextView.setText("(No items recorded for this bill)");
            noItemsTextView.setTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Caption);
            itemsContainer.addView(noItemsTextView);
        }

        dialog.show(); // Show the prepared dialog
    }

    // Helper method to format date and time
    private String formatDialogDateTime(String dateStr) {
        // ... (implementation remains the same as previous) ...
        if (dateStr == null) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) { return outputFormat.format(date); }
        } catch (ParseException e) { Log.w(TAG, "Could not parse date for dialog: " + dateStr); }
        return dateStr;
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