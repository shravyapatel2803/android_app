package com.example.billgenerator;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.DebtUpdateAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.DebtUpdateItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class DebtHistoryActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "extra_customer_id";
    public static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";

    private databaseSystem dbHelper;
    private final ArrayList<DebtUpdateItem> updates = new ArrayList<>();
    private DebtUpdateAdapter adapter;
    private long customerId;
    private String customerName;
    private double currentBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debt_history);

        dbHelper = new databaseSystem(this);
        Intent launchIntent = getIntent();
        customerId = launchIntent.getLongExtra(EXTRA_CUSTOMER_ID, -1L);
        if (customerId <= 0 && launchIntent.hasExtra(EXTRA_CUSTOMER_ID)) {
            customerId = launchIntent.getIntExtra(EXTRA_CUSTOMER_ID, -1);
        }
        customerName = launchIntent.getStringExtra(EXTRA_CUSTOMER_NAME);

        TextView nameText = findViewById(R.id.debt_history_customer_name);
        TextView balanceText = findViewById(R.id.debt_history_balance);
        RecyclerView recyclerView = findViewById(R.id.debt_history_recycler);
        TextView emptyText = findViewById(R.id.debt_history_empty);
        MaterialButton downloadButton = findViewById(R.id.debt_history_download_button);
        MaterialButton shareButton = findViewById(R.id.debt_history_share_button);
        MaterialButton printButton = findViewById(R.id.debt_history_print_button);

        adapter = new DebtUpdateAdapter(updates);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        if (customerId <= 0) {
            Toast.makeText(this, "Invalid customer", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (customerName == null || customerName.trim().isEmpty()) {
            customerName = "Customer #" + customerId;
        }
        nameText.setText(customerName);

        loadDebtUpdates();

        if (currentBalance >= 0) {
            balanceText.setText(String.format(Locale.getDefault(), "Current debt: Rs %.2f", currentBalance));
        } else {
            balanceText.setText(String.format(Locale.getDefault(), "Current credit: Rs %.2f", Math.abs(currentBalance)));
        }

        emptyText.setVisibility(updates.isEmpty() ? TextView.VISIBLE : TextView.GONE);

        downloadButton.setOnClickListener(v -> saveDebtHistoryPdfToDownloads());
        shareButton.setOnClickListener(v -> shareDebtHistoryPdf());
        printButton.setOnClickListener(v -> openDebtHistoryPdfForPrint());
    }

    private void loadDebtUpdates() {
        currentBalance = dbHelper.getCustomerDebt(customerId);
        updates.clear();
        Cursor cursor = null;
        try {
            cursor = dbHelper.fetchDebtUpdatesForCustomer(customerId);
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int billCol = cursor.getColumnIndexOrThrow("bill_id");
                int changeCol = cursor.getColumnIndexOrThrow("change_amount");
                int balanceCol = cursor.getColumnIndexOrThrow("resulting_balance");
                int billedCol = cursor.getColumnIndexOrThrow("billed_amount");
                int paidCol = cursor.getColumnIndexOrThrow("paid_amount");
                int dueCol = cursor.getColumnIndexOrThrow("due_date");
                int noteCol = cursor.getColumnIndexOrThrow("note");
                int createdCol = cursor.getColumnIndexOrThrow("created_at");
                do {
                    updates.add(new DebtUpdateItem(
                            cursor.getLong(idCol),
                            cursor.getLong(billCol),
                            cursor.getDouble(changeCol),
                            cursor.getDouble(balanceCol),
                            cursor.getDouble(billedCol),
                            cursor.getDouble(paidCol),
                            cursor.getString(dueCol),
                            cursor.getString(noteCol),
                            cursor.getString(createdCol)
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void saveDebtHistoryPdfToDownloads() {
        String fileName = buildFileName();
        try {
            OutputStream outputStream;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BillGenerator");
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Toast.makeText(this, "Unable to create PDF file", Toast.LENGTH_SHORT).show();
                    return;
                }
                outputStream = getContentResolver().openOutputStream(uri);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BillGenerator");
                if (!dir.exists() && !dir.mkdirs()) {
                    Toast.makeText(this, "Unable to create folder", Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(dir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream == null) {
                Toast.makeText(this, "Unable to open output stream", Toast.LENGTH_SHORT).show();
                return;
            }

            writeDebtHistoryPdf(outputStream);
            outputStream.close();
            Toast.makeText(this, "Debt history PDF downloaded", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to save PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareDebtHistoryPdf() {
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), buildFileName());
            FileOutputStream outputStream = new FileOutputStream(file);
            writeDebtHistoryPdf(outputStream);
            outputStream.close();

            Uri uri = FileProvider.getUriForFile(this, "com.example.billgenerator.provider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Send debt history PDF"));
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to send PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openDebtHistoryPdfForPrint() {
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), buildFileName());
            FileOutputStream outputStream = new FileOutputStream(file);
            writeDebtHistoryPdf(outputStream);
            outputStream.close();

            Uri uri = FileProvider.getUriForFile(this, "com.example.billgenerator.provider", file);
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, "application/pdf");
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(openIntent, "Open PDF to print"));
        } catch (Exception ex) {
            Toast.makeText(this, "Failed to open PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String buildFileName() {
        return "debt_history_" + customerId + ".pdf";
    }

    private void writeDebtHistoryPdf(OutputStream outputStream) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);

        Paint textPaint = new Paint();
        textPaint.setTextSize(12f);

        int pageNumber = 1;
        int y = 50;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        page.getCanvas().drawText("Debt Update History", 40, y, titlePaint);
        y += 24;
        page.getCanvas().drawText("Customer: " + customerName, 40, y, textPaint);
        y += 20;
        page.getCanvas().drawText(String.format(Locale.getDefault(), "Current balance: Rs %.2f", currentBalance), 40, y, textPaint);
        y += 24;

        for (DebtUpdateItem update : updates) {
            if (y > 780) {
                document.finishPage(page);
                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                page = document.startPage(pageInfo);
                y = 50;
            }

            String changeText = update.debtChange >= 0
                    ? String.format(Locale.getDefault(), "Debt +Rs %.2f", update.debtChange)
                    : String.format(Locale.getDefault(), "Credit Rs %.2f", Math.abs(update.debtChange));

            page.getCanvas().drawText("Date: " + safe(update.createdAt), 40, y, textPaint);
            y += 16;
            page.getCanvas().drawText("Change: " + changeText + " | Balance: Rs " + String.format(Locale.getDefault(), "%.2f", update.resultingBalance), 40, y, textPaint);
            y += 16;
            page.getCanvas().drawText("Bill #" + update.billId + " | Billed: Rs " + String.format(Locale.getDefault(), "%.2f", update.billedAmount) + " | Paid: Rs " + String.format(Locale.getDefault(), "%.2f", update.paidAmount), 40, y, textPaint);
            y += 16;
            page.getCanvas().drawText("Due: " + safe(update.dueDate) + " | Note: " + safe(update.note), 40, y, textPaint);
            y += 20;
        }

        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }
}
