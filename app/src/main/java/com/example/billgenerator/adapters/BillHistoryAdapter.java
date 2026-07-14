package com.example.billgenerator.adapters;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.BillHistoryModel;
import com.example.billgenerator.utils.PdfUtils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;

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
            showBillDetailDialog(model, holder.getAdapterPosition());
        });
    }

    private void showBillDetailDialog(final BillHistoryModel model, final int position) {
        Log.d(TAG, "Showing detail dialog for Bill ID: " + model.billId);
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bill_detail_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Toolbar toolbar = dialog.findViewById(R.id.toolbar_bill_detail);
        TextView detailCustomerName = dialog.findViewById(R.id.detail_customer_name);
        TextView detailCustomerPhone = dialog.findViewById(R.id.detail_customer_phone);
        TextView detailBillId = dialog.findViewById(R.id.detail_bill_id);
        TextView detailBillDate = dialog.findViewById(R.id.detail_bill_date);
        TextView detailGoldRate = dialog.findViewById(R.id.detail_gold_rate);
        TextView detailSilverRate = dialog.findViewById(R.id.detail_silver_rate);
        TextView detailReturnItemInfo = dialog.findViewById(R.id.detail_return_item_info);
        TextView detailBilledAmount = dialog.findViewById(R.id.detail_billed_amount);
        TextView detailPaidAmount = dialog.findViewById(R.id.detail_paid_amount);
        TextView detailDebtStatus = dialog.findViewById(R.id.detail_debt_status);
        TextView detailTotalAmount = dialog.findViewById(R.id.detail_total_amount);
        LinearLayout itemsContainer = dialog.findViewById(R.id.container_bill_items);
        ImageButton whatsappButton = dialog.findViewById(R.id.whatsapp_button);
        ImageButton shareButton = dialog.findViewById(R.id.share_button);
        ImageButton printButton = dialog.findViewById(R.id.print_button);
        Button editButton = dialog.findViewById(R.id.edit_bill_button);
        Button deleteButton = dialog.findViewById(R.id.delete_bill_button);

        View totalsSectionView = dialog.findViewById(R.id.detail_total_amount);
        ViewGroup totalsSection = (totalsSectionView != null && totalsSectionView.getParent() instanceof ViewGroup)
                ? (ViewGroup) totalsSectionView.getParent() : null;

        if (toolbar == null || detailCustomerName == null || detailCustomerPhone == null || detailBillId == null ||
                detailBillDate == null || detailGoldRate == null || detailSilverRate == null ||
            detailBilledAmount == null || detailPaidAmount == null || detailDebtStatus == null ||
            detailTotalAmount == null || detailReturnItemInfo == null || itemsContainer == null || totalsSection == null) {
            Log.e(TAG, "Error: One or more views not found in bill_detail_dialog.xml");
            Toast.makeText(context, "Error displaying bill details.", Toast.LENGTH_SHORT).show();
            return;
        }

        TextView detailGstInfo = new TextView(context);
        LinearLayout.LayoutParams gstParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gstParams.setMargins(0, 4, 0, 0);
        detailGstInfo.setLayoutParams(gstParams);
        detailGstInfo.setGravity(android.view.Gravity.END);
        detailGstInfo.setTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Body2);
        detailGstInfo.setVisibility(View.GONE);
        int totalIndex = totalsSection.indexOfChild(detailTotalAmount);
        totalsSection.addView(detailGstInfo, totalIndex);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
        toolbar.setTitle("Bill #" + model.billId);

        final String[] customerPhone = new String[1];
        Cursor billCursor = dbHelper.getBillDetails(model.billId);
        if (billCursor != null && billCursor.moveToFirst()) {
            Log.d(TAG, "Bill details found for ID: " + model.billId);
            try {
                detailCustomerName.setText(billCursor.getString(billCursor.getColumnIndexOrThrow("name")));
                customerPhone[0] = billCursor.getString(billCursor.getColumnIndexOrThrow("phone"));
                detailCustomerPhone.setText(customerPhone[0]);
                detailBillId.setText(String.format(Locale.getDefault(), "Bill #%d", model.billId));
                detailBillDate.setText(formatDialogDateTime(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date"))));

                double goldRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_gold_rate"));
                double silverRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_silver_rate"));
                double totalAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                int billedAmountIndex = billCursor.getColumnIndex("billed_amount");
                int paidAmountIndex = billCursor.getColumnIndex("paid_amount");
                double gstPercent = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));
                double debtAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));
                String returnItemType = billCursor.getString(billCursor.getColumnIndexOrThrow("return_item_type"));
                double returnItemWeight = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_weight"));
                double returnItemDeduction = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_deduct_amount"));

                double billedAmount = billedAmountIndex != -1 ? billCursor.getDouble(billedAmountIndex) : totalAmount;
                double paidAmount = paidAmountIndex != -1 ? billCursor.getDouble(paidAmountIndex) : Math.max(0.0, totalAmount - debtAmount);

                if (billedAmount <= 0.001) {
                    billedAmount = totalAmount;
                }
                if (paidAmount <= 0.001 && debtAmount <= 0.001) {
                    paidAmount = totalAmount;
                }

                detailGoldRate.setText(goldRate > 0 ? String.format(Locale.getDefault(), "Approx. Gold Rate: %s / 10g", currencyFormat.format(goldRate)) : "Gold Rate: N/A");
                detailSilverRate.setText(silverRate > 0 ? String.format(Locale.getDefault(), "Approx. Silver Rate: %s / kg", currencyFormat.format(silverRate)) : "Silver Rate: N/A");

                if (returnItemType != null && !returnItemType.trim().isEmpty()) {
                    StringBuilder returnInfoBuilder = new StringBuilder();
                    Cursor returnCursor = dbHelper.getReturnItemsForBill(model.billId);
                    if (returnCursor != null && returnCursor.moveToFirst()) {
                        int typeCol = returnCursor.getColumnIndexOrThrow("return_type");
                        int weightCol = returnCursor.getColumnIndexOrThrow("return_weight");
                        int deductCol = returnCursor.getColumnIndexOrThrow("return_deduct_amount");
                        do {
                            String type = returnCursor.getString(typeCol);
                            double weight = returnCursor.getDouble(weightCol);
                            double deduction = returnCursor.getDouble(deductCol);
                            
                            if (returnInfoBuilder.length() > 0) returnInfoBuilder.append("\n");
                            returnInfoBuilder.append(String.format(Locale.getDefault(),
                                    "Return Item: %s %.3f g | Deduction: %s",
                                    (type == null || type.isEmpty()) ? "N/A" : type,
                                    weight,
                                    currencyFormat.format(deduction)));
                        } while (returnCursor.moveToNext());
                        returnCursor.close();
                    }
                    
                    if (returnInfoBuilder.length() > 0) {
                        detailReturnItemInfo.setText(returnInfoBuilder.toString());
                        detailReturnItemInfo.setVisibility(View.VISIBLE);
                    } else {
                        // Fallback to single column if table is empty but column isn't
                        detailReturnItemInfo.setText(String.format(Locale.getDefault(),
                                "Return Item: %s %.3f g | Deduction: %s",
                                returnItemType,
                                returnItemWeight,
                                currencyFormat.format(returnItemDeduction)));
                        detailReturnItemInfo.setVisibility(View.VISIBLE);
                    }
                } else {
                    detailReturnItemInfo.setVisibility(View.GONE);
                }

                if (gstPercent > 0.001) {
                    detailGstInfo.setText(String.format(Locale.getDefault(), "GST Applied: %.2f%%", gstPercent));
                    detailGstInfo.setVisibility(View.VISIBLE);
                } else {
                    detailGstInfo.setVisibility(View.GONE);
                }

                detailBilledAmount.setText(String.format(Locale.getDefault(), "Billed Amount: %s", currencyFormat.format(billedAmount)));
                detailPaidAmount.setText(String.format(Locale.getDefault(), "Amount Paid: %s", currencyFormat.format(paidAmount)));
                if (debtAmount > 0.001) {
                    detailDebtStatus.setText(String.format(Locale.getDefault(), "Debt: Yes (%s)", currencyFormat.format(debtAmount)));
                    detailDebtStatus.setTextColor(Color.parseColor("#D32F2F"));
                } else {
                    detailDebtStatus.setText("Debt: No");
                    detailDebtStatus.setTextColor(Color.parseColor("#4CAF50"));
                }

                detailTotalAmount.setText(String.format("Total: %s", currencyFormat.format(totalAmount)));

            } catch (Exception e) {
                Log.e(TAG, "Error reading bill details cursor: " + e.getMessage());
                Toast.makeText(context, "Error reading bill details.", Toast.LENGTH_SHORT).show();
            } finally {
                billCursor.close();
            }
        } else {
            Log.e(TAG, "Bill details cursor is null or empty for ID: " + model.billId);
            Toast.makeText(context, "Could not find bill details.", Toast.LENGTH_SHORT).show();
            if(billCursor != null) billCursor.close();
            dialog.dismiss();
            return;
        }

        itemsContainer.removeAllViews();
        Cursor itemCursor = dbHelper.getItemsForBill(model.billId);
        boolean itemsFound = false;
        if (itemCursor != null) {
            Log.d(TAG, "Found " + itemCursor.getCount() + " items for bill ID: " + model.billId);
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
                itemsFound = false;
            } finally {
                itemCursor.close();
            }
        } else {
            Log.w(TAG, "getItemsForBill cursor is null for bill ID: " + model.billId);
        }

        if (!itemsFound) {
            Log.d(TAG, "No items found or error reading items for bill ID: " + model.billId);
            TextView noItemsTextView = new TextView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            noItemsTextView.setLayoutParams(params);
            noItemsTextView.setText("(No items recorded for this bill)");
            noItemsTextView.setTextAppearance(context, androidx.appcompat.R.style.TextAppearance_AppCompat_Caption);
            itemsContainer.addView(noItemsTextView);
        }

        whatsappButton.setOnClickListener(v -> {
            sharePdfBill(model.billId, customerPhone[0]);
            dialog.dismiss();
        });

        shareButton.setOnClickListener(v -> {
            View billView = dialog.findViewById(R.id.bill_details_layout);
            Bitmap bitmap = viewToBitmap(billView);
            shareBillImage(bitmap);
        });

        if (printButton != null) {
            printButton.setOnClickListener(v -> {
                showBluetoothPrinterDialog(model.billId);
            });
        }

        editButton.setOnClickListener(v -> {
            if (context instanceof com.example.billgenerator.MainActivity) {
                ((com.example.billgenerator.MainActivity) context).navigateToEditBill(model.billId);
                dialog.dismiss();
            } else {
                Toast.makeText(context, "Edit feature is only available from Main Dashboard.", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Bill")
                    .setMessage("Are you sure you want to delete this bill? This action cannot be undone.")
                    .setPositiveButton("Yes, Delete", (dialogInterface, i) -> {
                        dbHelper.deleteBill(model.billId);
                        billList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, billList.size());
                        Toast.makeText(context, "Bill #" + model.billId + " deleted", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.show();
    }

    private void sharePdfBill(int billId, String phoneNumber) {
        try {
            File pdfFile = PdfUtils.generateBillPdf(context, billId);
            SharedPreferences shopPrefs = context.getSharedPreferences("shop_profile_prefs", Context.MODE_PRIVATE);
            String message = shopPrefs.getString("whatsapp_note", "Thank you for shopping with us! Your bill is attached.");
            sharePdf(pdfFile, phoneNumber, message);
        } catch (IOException e) {
            Log.e(TAG, "Error generating or sharing PDF", e);
            Toast.makeText(context, "Failed to share PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showBluetoothPrinterDialog(int billId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Select Bluetooth Printer");
        
        // Mock list of printers
        String[] printers = {"Inner Printer (Built-in)", "BT-Printer-80", "Thermal-P58", "Add New Printer..."};
        
        builder.setItems(printers, (dialog, which) -> {
            if (which == printers.length - 1) {
                Toast.makeText(context, "Bluetooth discovery started...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Printing Bill #" + billId + " to " + printers[which], Toast.LENGTH_LONG).show();
                // In a real app, you would connect to the MAC address and send ESC/POS commands
                Log.i(TAG, "Bluetooth Print Command Sent for Bill #" + billId);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void generateAndSharePdfBill(int billId, String phoneNumber) {
        LayoutInflater inflater = LayoutInflater.from(context);
        Cursor billCursor = dbHelper.getBillDetails(billId);
        if (billCursor == null || !billCursor.moveToFirst()) {
            Toast.makeText(context, "Bill details not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        View billView;
        try {
            double totalAmt = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
            double gstPercent = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));
            String paymentMode = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_mode"));

            boolean isOnline = paymentMode != null && paymentMode.contains("Online");
            boolean isGstBill = isOnline || gstPercent > 0;

            int layoutResId = isGstBill ? R.layout.professional_bill_layout : R.layout.estimate_bill_layout;
            billView = inflater.inflate(layoutResId, null);

            TextView billTitle = billView.findViewById(R.id.bill_title_textview);
            TextView shopName = billView.findViewById(R.id.shop_name_textview);
            TextView customerName = billView.findViewById(R.id.customer_name_textview);
            TextView billDate = billView.findViewById(R.id.bill_date_textview);
            TextView customerPhone = billView.findViewById(R.id.customer_phone_textview);
            TextView shopMetaLine1 = billView.findViewById(R.id.shop_meta_line1_textview);
            TextView shopMetaLine2 = billView.findViewById(R.id.shop_meta_line2_textview);
            TextView shopTagline = billView.findViewById(R.id.shop_tagline_textview);
            TextView invoiceNo = billView.findViewById(R.id.invoice_no_value_textview);
            LinearLayout itemsContainer = billView.findViewById(R.id.items_container);
            TextView subtotal = billView.findViewById(R.id.subtotal_textview);
            LinearLayout gstLayout = billView.findViewById(R.id.gst_layout);
            TextView gstLabel = billView.findViewById(R.id.gst_label_textview);
            TextView gstAmount = billView.findViewById(R.id.gst_amount_textview);
            TextView totalAmount = billView.findViewById(R.id.total_amount_textview);
            TextView paymentSummary = billView.findViewById(R.id.payment_summary_textview);
            TextView debtDueDate = billView.findViewById(R.id.debt_due_date_textview);
            TextView returnItemInfo = billView.findViewById(R.id.return_item_textview);

            int pdfTextColor = Color.parseColor("#111111");
            billTitle.setTextColor(pdfTextColor);
            shopName.setTextColor(pdfTextColor);
            customerName.setTextColor(pdfTextColor);
            billDate.setTextColor(pdfTextColor);
            customerPhone.setTextColor(pdfTextColor);
            subtotal.setTextColor(pdfTextColor);
            gstLabel.setTextColor(pdfTextColor);
            gstAmount.setTextColor(pdfTextColor);
            totalAmount.setTextColor(pdfTextColor);
            shopMetaLine1.setTextColor(pdfTextColor);
            shopMetaLine2.setTextColor(pdfTextColor);
            shopTagline.setTextColor(pdfTextColor);

            applyShopProfileToBillHeader(shopName, shopMetaLine1, shopMetaLine2, shopTagline);

            if (isGstBill) {
                billTitle.setText(isOnline ? "GST Bill" : "Tax Invoice");
                gstLayout.setVisibility(View.VISIBLE);

                double subTotalAmt = totalAmt / (1 + (gstPercent / 100));
                double gstAmt = totalAmt - subTotalAmt;

                subtotal.setText(currencyFormat.format(subTotalAmt));
                gstLabel.setText(String.format(Locale.getDefault(), "GST (%.2f%%)", gstPercent));
                gstAmount.setText(currencyFormat.format(gstAmt));
                if (invoiceNo != null) {
                    invoiceNo.setText(String.format(Locale.getDefault(), "INV-%05d", billId));
                }
            } else {
                billTitle.setText("Estimate Bill");
                gstLayout.setVisibility(View.GONE);
                subtotal.setText(currencyFormat.format(totalAmt));
                if (invoiceNo != null) {
                    invoiceNo.setText(String.format(Locale.getDefault(), "EST-%05d", billId));
                }
            }

            String customerNameValue = safeText(billCursor.getString(billCursor.getColumnIndexOrThrow("name")));
            String customerPhoneValue = safeText(billCursor.getString(billCursor.getColumnIndexOrThrow("phone")));
            double debtAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));
            String debtDueDateValue = safeText(billCursor.getString(billCursor.getColumnIndexOrThrow("debt_due_date")));

            int billedAmountIndex = billCursor.getColumnIndex("billed_amount");
            int paidAmountIndex = billCursor.getColumnIndex("paid_amount");
            double billedAmount = billedAmountIndex != -1 ? billCursor.getDouble(billedAmountIndex) : totalAmt;
            double paidAmount = paidAmountIndex != -1 ? billCursor.getDouble(paidAmountIndex) : Math.max(0.0, totalAmt - debtAmount);

            if (billedAmount <= 0.001) {
                billedAmount = totalAmt;
            }
            if (paidAmount < 0.0) {
                paidAmount = 0.0;
            }
            customerName.setText(customerNameValue.isEmpty() ? "Customer" : customerNameValue);
            billDate.setText(formatPdfInvoiceDate(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date"))));
            customerPhone.setText(customerPhoneValue.isEmpty() ? "Phone: NA" : "Phone: " + customerPhoneValue);
            totalAmount.setText(currencyFormat.format(totalAmt));

            if (paymentSummary != null) {
                paymentSummary.setText(String.format(
                        Locale.getDefault(),
                        "Billed: %s | Paid: %s | Debt: %s",
                        currencyFormat.format(billedAmount),
                        currencyFormat.format(paidAmount),
                        currencyFormat.format(debtAmount)
                ));
            }

            if (debtDueDate != null) {
                if (debtAmount > 0.001 && !debtDueDateValue.isEmpty()) {
                    debtDueDate.setVisibility(View.VISIBLE);
                    debtDueDate.setText("Debt Due: " + debtDueDateValue);
                } else {
                    debtDueDate.setVisibility(View.GONE);
                }
            }

            if (returnItemInfo != null) {
                StringBuilder returnInfoBuilder = new StringBuilder();
                Cursor returnCursor = dbHelper.getReturnItemsForBill(billId);
                if (returnCursor != null && returnCursor.moveToFirst()) {
                    int typeCol = returnCursor.getColumnIndexOrThrow("return_type");
                    int weightCol = returnCursor.getColumnIndexOrThrow("return_weight");
                    int deductCol = returnCursor.getColumnIndexOrThrow("return_deduct_amount");
                    do {
                        String type = safeText(returnCursor.getString(typeCol));
                        double weight = returnCursor.getDouble(weightCol);
                        double deduction = returnCursor.getDouble(deductCol);
                        
                        if (returnInfoBuilder.length() > 0) returnInfoBuilder.append("\n");
                        returnInfoBuilder.append(String.format(Locale.getDefault(),
                                "Return Item: %s %.3f g | Deduction: %s",
                                type.isEmpty() ? "N/A" : type,
                                weight,
                                currencyFormat.format(deduction)));
                    } while (returnCursor.moveToNext());
                    returnCursor.close();
                }

                if (returnInfoBuilder.length() > 0) {
                    returnItemInfo.setText(returnInfoBuilder.toString());
                    returnItemInfo.setVisibility(View.VISIBLE);
                } else {
                    returnItemInfo.setText("Return Item: No return item");
                    // Optionally hide if no return items
                    // returnItemInfo.setVisibility(View.GONE);
                }
            }

            itemsContainer.removeAllViews();
            Cursor itemCursor = dbHelper.getItemsForBill(billId);
            if (itemCursor != null && itemCursor.moveToFirst()) {
                int serialNo = 1;
                do {
                    String name = itemCursor.getString(itemCursor.getColumnIndexOrThrow("name"));
                    double weight = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow("weight"));
                    String type = itemCursor.getString(itemCursor.getColumnIndexOrThrow("type"));
                    String safeType = (type == null || type.trim().isEmpty()) ? "Item" : type;

                    View itemRow = inflater.inflate(R.layout.pdf_bill_item_row, itemsContainer, false);
                    TextView rowNo = itemRow.findViewById(R.id.row_no_textview);
                    TextView rowName = itemRow.findViewById(R.id.row_name_textview);
                    TextView rowQty = itemRow.findViewById(R.id.row_qty_textview);
                    TextView rowWeight = itemRow.findViewById(R.id.row_weight_textview);

                    rowNo.setText(String.valueOf(serialNo));
                    rowName.setText(String.format(Locale.getDefault(), "%s (%s)", name, safeType));
                    rowQty.setText("1");
                    rowWeight.setText(String.format(Locale.getDefault(), "%.3f g", weight));

                    itemsContainer.addView(itemRow);
                    serialNo++;
                } while (itemCursor.moveToNext());
            } else {
                TextView emptyView = new TextView(context);
                emptyView.setText("No items available");
                emptyView.setTextSize(11f);
                emptyView.setPadding(12, 12, 12, 12);
                emptyView.setTextColor(pdfTextColor);
                itemsContainer.addView(emptyView);
            }
            if (itemCursor != null) {
                itemCursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error populating PDF bill layout: " + e.getMessage());
            Toast.makeText(context, "Error generating bill PDF.", Toast.LENGTH_SHORT).show();
            billCursor.close();
            return;
        }
        billCursor.close();

        // Generate PDF from the view
        try {
            File pdfFile = createPdfFromView(billView, "bill_" + billId);
            SharedPreferences shopPrefs = context.getSharedPreferences("shop_profile_prefs", Context.MODE_PRIVATE);
            String message = shopPrefs.getString("whatsapp_note", "Thank you for shopping with us! Your bill is attached.");
            sharePdf(pdfFile, phoneNumber, message);
        } catch (IOException e) {
            Log.e(TAG, "Error creating or sharing PDF: " + e.getMessage());
            Toast.makeText(context, "Could not create or share bill PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyShopProfileToBillHeader(TextView shopName, TextView metaLine1, TextView metaLine2, TextView taglineView) {
        SharedPreferences prefs = context.getSharedPreferences("shop_profile_prefs", Context.MODE_PRIVATE);

        String profileShopName = safeText(prefs.getString("shop_name", ""));
        String owner = safeText(prefs.getString("owner_name", ""));
        String phone = safeText(prefs.getString("phone", ""));
        String whatsapp = safeText(prefs.getString("whatsapp", ""));
        String address = safeText(prefs.getString("address", ""));
        String gstin = safeText(prefs.getString("gstin", ""));
        String tagline = safeText(prefs.getString("tagline", ""));

        if (!profileShopName.isEmpty()) {
            shopName.setText(profileShopName);
        }

        ArrayList<String> partsLine1 = new ArrayList<>();
        if (!owner.isEmpty()) {
            partsLine1.add("Owner: " + owner);
        }
        if (!phone.isEmpty()) {
            partsLine1.add("Phone: " + phone);
        } else if (!whatsapp.isEmpty()) {
            partsLine1.add("WhatsApp: " + whatsapp);
        }

        ArrayList<String> partsLine2 = new ArrayList<>();
        if (!address.isEmpty()) {
            partsLine2.add(address);
        }
        if (!gstin.isEmpty()) {
            partsLine2.add("GSTIN: " + gstin);
        }

        bindOptionalLine(metaLine1, joinParts(partsLine1));
        bindOptionalLine(metaLine2, joinParts(partsLine2));
        bindOptionalLine(taglineView, tagline);
    }

    private void bindOptionalLine(TextView view, String value) {
        if (value.isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(value);
        }
    }

    private String joinParts(ArrayList<String> parts) {
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                builder.append(" | ");
            }
            builder.append(parts.get(i));
        }
        return builder.toString();
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private File createPdfFromView(View view, String filename) throws IOException {
        // Measure and layout the view
        view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        // Create a bitmap from the view
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        // Create a PDF file
        File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename + ".pdf");
        FileOutputStream outputStream = new FileOutputStream(pdfFile);

        // Write the bitmap to the PDF
        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();

        return pdfFile;
    }

    private void sharePdf(File pdfFile, String phoneNumber, String message) {
        Uri pdfUri = FileProvider.getUriForFile(context, "com.example.billgenerator.provider", pdfFile);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        String normalizedPhone = normalizeWhatsAppNumber(phoneNumber);
        if (!normalizedPhone.isEmpty()) {
            shareIntent.setPackage("com.whatsapp");
            shareIntent.putExtra("jid", normalizedPhone + "@s.whatsapp.net");
        }

        try {
            if ("com.whatsapp".equals(shareIntent.getPackage())) {
                context.startActivity(shareIntent);
            } else {
                context.startActivity(Intent.createChooser(shareIntent, "Share Bill PDF"));
            }
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, "No app to handle this action.", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            if (!normalizedPhone.isEmpty()) {
                openWhatsAppChat(normalizedPhone);
            }
            Toast.makeText(context, "Could not attach PDF directly to that chat. Opened WhatsApp contact.", Toast.LENGTH_SHORT).show();
        }
    }

    private String normalizeWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }

        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }

        // For common India local numbers, prepend country code so WhatsApp can resolve chat.
        if (digits.length() == 10) {
            return "91" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return "91" + digits.substring(1);
        }

        return digits;
    }

    private void openWhatsAppChat(String normalizedPhone) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + normalizedPhone));
            context.startActivity(intent);
        } catch (Exception ignored) {
            // Keep silent here; caller already handles user feedback.
        }
    }

    private Bitmap viewToBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void shareBillImage(Bitmap bitmap) {
        try {
            File cachePath = new File(context.getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/bill_image.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            File imagePath = new File(context.getCacheDir(), "images");
            File newFile = new File(imagePath, "bill_image.png");
            Uri contentUri = FileProvider.getUriForFile(context, "com.example.billgenerator.provider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, context.getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                context.startActivity(Intent.createChooser(shareIntent, "Share bill via"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatDialogDateTime(String dateStr) {
        if (dateStr == null) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) { return outputFormat.format(date); }
        } catch (ParseException e) { Log.w(TAG, "Could not parse date for dialog: " + dateStr); }
        return dateStr;
    }

    private String formatPdfInvoiceDate(String dateStr) {
        if (dateStr == null) return "NA";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) {
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            Log.w(TAG, "Could not parse date for PDF: " + dateStr);
        }
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
