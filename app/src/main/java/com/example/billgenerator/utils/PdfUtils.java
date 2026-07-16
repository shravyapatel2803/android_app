package com.example.billgenerator.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class PdfUtils {

    private static final String TAG = "PdfUtils";
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static File generateBillPdf(Context context, int billId) throws IOException {
        databaseSystem dbHelper = new databaseSystem(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        Cursor billCursor = dbHelper.getBillDetails(billId);
        
        if (billCursor == null || !billCursor.moveToFirst()) {
            throw new IOException("Bill details not found for ID: " + billId);
        }

        View billView;
        try {
            double totalAmt = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
            double gstPercent = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));
            String paymentMode = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_mode"));

            // Load font size and PDF language from settings
            SharedPreferences settingsPrefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE);
            String pdfLang = settingsPrefs.getString("pdf_language", "English");
            
            // Set locale for formatting and strings based on selection
            Locale pdfLocale = new Locale("en");
            if ("Hindi".equals(pdfLang)) pdfLocale = new Locale("hi");
            else if ("Gujarati".equals(pdfLang)) pdfLocale = new Locale("gu");
            
            // Update context configuration for resource retrieval
            android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
            config.setLocale(pdfLocale);
            Context pdfContext = context.createConfigurationContext(config);

            // NEW LOGIC: Use Professional/GST layout if Online/UPI is selected OR GST is applied
            boolean isGstBill = (gstPercent > 0.001) || "Online / UPI".equalsIgnoreCase(paymentMode);

            int layoutResId = isGstBill ? R.layout.professional_bill_layout : R.layout.estimate_bill_layout;
            billView = LayoutInflater.from(pdfContext).inflate(layoutResId, null);

            TextView billTitle = billView.findViewById(R.id.bill_title_textview);
            TextView shopNameView = billView.findViewById(R.id.shop_name_textview);
            TextView customerName = billView.findViewById(R.id.customer_name_textview);
            TextView billDate = billView.findViewById(R.id.bill_date_textview);
            TextView customerPhone = billView.findViewById(R.id.customer_phone_textview);
            TextView shopMetaLine1 = billView.findViewById(R.id.shop_meta_line1_textview);
            TextView shopMetaLine2 = billView.findViewById(R.id.shop_meta_line2_textview);
            TextView shopTagline = billView.findViewById(R.id.shop_tagline_textview);
            TextView shopGstin = billView.findViewById(R.id.shop_gstin_textview);
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
            android.widget.ImageView logoView = billView.findViewById(R.id.shop_logo_imageview);

            applyShopProfileToBillHeader(context, shopNameView, shopMetaLine1, shopMetaLine2, shopTagline, shopGstin, logoView);

            if (isGstBill) {
                billTitle.setText("Tax Invoice");
                gstLayout.setVisibility(View.VISIBLE);
                double subTotalAmt = totalAmt / (1 + (gstPercent / 100));
                double gstAmt = totalAmt - subTotalAmt;
                subtotal.setText(currencyFormat.format(subTotalAmt));
                gstLabel.setText(String.format(Locale.getDefault(), "GST (%.2f%%)", gstPercent));
                gstAmount.setText(currencyFormat.format(gstAmt));
                if (invoiceNo != null) invoiceNo.setText(String.format(Locale.getDefault(), "INV-%05d", billId));
            } else {
                billTitle.setText("Estimate Bill");
                gstLayout.setVisibility(View.GONE);
                subtotal.setText(currencyFormat.format(totalAmt));
                if (invoiceNo != null) invoiceNo.setText(String.format(Locale.getDefault(), "EST-%05d", billId));
            }

            customerName.setText(safeText(billCursor.getString(billCursor.getColumnIndexOrThrow("name"))));
            billDate.setText(formatPdfInvoiceDate(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date"))));
            customerPhone.setText("Phone: " + safeText(billCursor.getString(billCursor.getColumnIndexOrThrow("phone"))));
            totalAmount.setText(currencyFormat.format(totalAmt));

            double debtAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));
            int billedAmountIndex = billCursor.getColumnIndex("billed_amount");
            int paidAmountIndex = billCursor.getColumnIndex("paid_amount");
            double billedAmount = billedAmountIndex != -1 ? billCursor.getDouble(billedAmountIndex) : totalAmt;
            double paidAmount = paidAmountIndex != -1 ? billCursor.getDouble(paidAmountIndex) : Math.max(0.0, totalAmt - debtAmount);

            if (paymentSummary != null) {
                paymentSummary.setText(String.format(Locale.getDefault(), "Billed: %s | Paid: %s | Debt: %s",
                        currencyFormat.format(billedAmount), currencyFormat.format(paidAmount), currencyFormat.format(debtAmount)));
            }

            itemsContainer.removeAllViews();
            Cursor itemCursor = dbHelper.getItemsForBill(billId);
            if (itemCursor != null && itemCursor.moveToFirst()) {
                int serialNo = 1;
                do {
                    View itemRow = inflater.inflate(R.layout.pdf_bill_item_row, itemsContainer, false);
                    ((TextView)itemRow.findViewById(R.id.row_no_textview)).setText(String.valueOf(serialNo++));
                    ((TextView)itemRow.findViewById(R.id.row_name_textview)).setText(itemCursor.getString(itemCursor.getColumnIndexOrThrow("name")));
                    ((TextView)itemRow.findViewById(R.id.row_qty_textview)).setText("1");
                    ((TextView)itemRow.findViewById(R.id.row_weight_textview)).setText(String.format(Locale.getDefault(), "%.3f g", itemCursor.getDouble(itemCursor.getColumnIndexOrThrow("weight"))));
                    itemsContainer.addView(itemRow);
                } while (itemCursor.moveToNext());
                itemCursor.close();
            }

        } finally {
            billCursor.close();
        }

        return createPdfFromView(context, billView, "bill_" + billId);
    }

    private static void applyShopProfileToBillHeader(Context context, TextView shopName, TextView metaLine1, TextView metaLine2, TextView taglineView, TextView gstinView, android.widget.ImageView logoView) {
        SharedPreferences prefs = context.getSharedPreferences("shop_profile_prefs", Context.MODE_PRIVATE);
        shopName.setText(prefs.getString("shop_name", "Shop"));
        
        String owner = prefs.getString("owner_name", "");
        String phone = prefs.getString("phone", "");
        String address = prefs.getString("address", "");
        String gstin = prefs.getString("gstin", "");
        String tagline = prefs.getString("tagline", "");
        String logoUriStr = prefs.getString("shop_logo_uri", null);

        if (logoView != null && logoUriStr != null) {
            try {
                Uri uri = Uri.parse(logoUriStr);
                Bitmap bitmap;
                if ("file".equals(uri.getScheme())) {
                    bitmap = BitmapFactory.decodeFile(uri.getPath());
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                }

                if (bitmap != null) {
                    logoView.setImageBitmap(bitmap);
                    logoView.setVisibility(View.VISIBLE);
                } else {
                    logoView.setVisibility(View.GONE);
                }
            } catch (SecurityException se) {
                Log.e(TAG, "Permission denied for shop logo URI: " + logoUriStr, se);
                logoView.setVisibility(View.GONE);
                // Clear the invalid URI from prefs to avoid repeated crashes if it wasn't caught
                prefs.edit().remove("shop_logo_uri").apply();
            } catch (Exception e) {
                Log.e(TAG, "Error loading shop logo for PDF", e);
                logoView.setVisibility(View.GONE);
            }
        }

        if (metaLine1 != null) {
            metaLine1.setText("Owner: " + owner + " | Phone: " + phone);
            metaLine1.setVisibility(View.VISIBLE);
        }
        
        if (metaLine2 != null) {
            metaLine2.setText(address);
            metaLine2.setVisibility(View.VISIBLE);
        }

        if (taglineView != null && !tagline.isEmpty()) {
            taglineView.setText(tagline);
            taglineView.setVisibility(View.VISIBLE);
        } else if (taglineView != null) {
            taglineView.setVisibility(View.GONE);
        }

        if (gstinView != null) {
            if (!gstin.isEmpty()) {
                gstinView.setText("GST NO: " + gstin);
                gstinView.setVisibility(View.VISIBLE);
            } else {
                gstinView.setText("GST NO: NA");
                gstinView.setVisibility(View.VISIBLE);
            }
        }
    }

    private static File createPdfFromView(Context context, View view, String filename) throws IOException {
        view.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));

        File pdfFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), filename + ".pdf");
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
        document.finishPage(page);
        try (FileOutputStream os = new FileOutputStream(pdfFile)) {
            document.writeTo(os);
        }
        document.close();
        return pdfFile;
    }

    private static String formatPdfInvoiceDate(String dateStr) {
        if (dateStr == null) return "NA";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) return outputFormat.format(date);
        } catch (ParseException ignored) {}
        return dateStr;
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
