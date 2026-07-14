package com.example.billgenerator.fragments;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.ManualDebtUpdateActivity;
import com.example.billgenerator.R;
import com.example.billgenerator.adapters.DebtUpdateAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.DebtUpdateItem;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;

public class DebtHistoryDialogFragment extends DialogFragment {

    private static final String ARG_CUSTOMER_ID = "customer_id";
    private static final String ARG_CUSTOMER_NAME = "customer_name";

    private databaseSystem dbHelper;
    private final ArrayList<DebtUpdateItem> updates = new ArrayList<>();
    private DebtUpdateAdapter adapter;
    private long customerId;
    private String customerName;
    private double currentBalance;
    private String customerPhone = "";
    private TextView balanceText;
    private TextView emptyText;

    private final ActivityResultLauncher<Intent> addEntryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadDebtUpdates();
                    updateHeaderViews();
                }
            });

    public static DebtHistoryDialogFragment newInstance(int customerId, String name) {
        DebtHistoryDialogFragment fragment = new DebtHistoryDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CUSTOMER_ID, customerId);
        args.putString(ARG_CUSTOMER_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            customerId = getArguments().getLong(ARG_CUSTOMER_ID, -1);
            customerName = getArguments().getString(ARG_CUSTOMER_NAME);
        }
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_BillGenerator);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_debt_history, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new databaseSystem(requireContext());

        Toolbar toolbar = view.findViewById(R.id.debt_history_toolbar);
        if (toolbar != null) {
            toolbar.setTitle(customerName);
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(v -> dismiss());
        }

        TextView nameText = view.findViewById(R.id.debt_history_customer_name);
        balanceText = view.findViewById(R.id.debt_history_balance);
        RecyclerView recyclerView = view.findViewById(R.id.debt_history_recycler);
        emptyText = view.findViewById(R.id.debt_history_empty);
        MaterialButton downloadButton = view.findViewById(R.id.debt_history_download_button);
        MaterialButton shareButton = view.findViewById(R.id.debt_history_share_button);
        MaterialButton printButton = view.findViewById(R.id.debt_history_print_button);
        MaterialButton addEntryButton = view.findViewById(R.id.debt_history_add_entry_button);
        MaterialButton whatsappButton = view.findViewById(R.id.debt_history_whatsapp_button);

        if (customerId <= 0) {
            Toast.makeText(requireContext(), "Invalid customer", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }

        adapter = new DebtUpdateAdapter(updates);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        if (customerName == null || customerName.trim().isEmpty()) {
            customerName = "Customer #" + customerId;
        }
        nameText.setText(customerName);
        customerPhone = dbHelper.getCustomerPhoneById(customerId);

        loadDebtUpdates();
        updateHeaderViews();

        downloadButton.setOnClickListener(v -> saveDebtHistoryPdfToDownloads());
        shareButton.setOnClickListener(v -> shareDebtHistoryPdf());
        printButton.setOnClickListener(v -> openDebtHistoryPdfForPrint());
        addEntryButton.setOnClickListener(v -> openManualEntryScreen());
        whatsappButton.setOnClickListener(v -> shareDebtSummaryOnWhatsApp());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            Window window = getDialog().getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (int) (getResources().getDisplayMetrics().heightPixels * 0.9));
            }
        }
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

    private void updateHeaderViews() {
        if (balanceText != null) {
            if (currentBalance >= 0) {
                balanceText.setText(String.format(Locale.getDefault(), "Current debt: Rs %.2f", currentBalance));
            } else {
                balanceText.setText(String.format(Locale.getDefault(), "Current credit: Rs %.2f", Math.abs(currentBalance)));
            }
        }
        if (emptyText != null) {
            emptyText.setVisibility(updates.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    private void openManualEntryScreen() {
        Intent intent = new Intent(requireContext(), ManualDebtUpdateActivity.class);
        intent.putExtra(ManualDebtUpdateActivity.EXTRA_CUSTOMER_ID, customerId);
        intent.putExtra(ManualDebtUpdateActivity.EXTRA_CUSTOMER_NAME, customerName);
        addEntryLauncher.launch(intent);
    }

    private void shareDebtSummaryOnWhatsApp() {
        String normalizedPhone = normalizeWhatsAppNumber(customerPhone);
        if (normalizedPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Customer phone not available", Toast.LENGTH_SHORT).show();
            return;
        }
        String message = buildDebtSummaryMessage();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/" + normalizedPhone + "?text=" + Uri.encode(message)));
        try {
            startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Unable to open WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildDebtSummaryMessage() {
        String balanceLine = currentBalance >= 0
                ? String.format(Locale.getDefault(), "Current debt: Rs %.2f", currentBalance)
                : String.format(Locale.getDefault(), "Current credit: Rs %.2f", Math.abs(currentBalance));
        return "Hello " + safe(customerName) + ",\n"
                + balanceLine + "\n"
                + "Total updates: " + updates.size() + "\n"
                + "Please contact us for full details.";
    }

    private String normalizeWhatsAppNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return "";
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;
        }
        return digits;
    }

    private void saveDebtHistoryPdfToDownloads() {
        String fileName = buildFileName();
        try {
            OutputStream outputStream;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                android.content.ContentValues values = new android.content.ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BillGenerator");
                Uri uri = requireContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) {
                    Toast.makeText(requireContext(), "Unable to create PDF file", Toast.LENGTH_SHORT).show();
                    return;
                }
                outputStream = requireContext().getContentResolver().openOutputStream(uri);
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BillGenerator");
                if (!dir.exists() && !dir.mkdirs()) {
                    Toast.makeText(requireContext(), "Unable to create folder", Toast.LENGTH_SHORT).show();
                    return;
                }
                File file = new File(dir, fileName);
                outputStream = new FileOutputStream(file);
            }

            if (outputStream == null) {
                Toast.makeText(requireContext(), "Unable to open output stream", Toast.LENGTH_SHORT).show();
                return;
            }

            writeDebtHistoryPdf(outputStream);
            outputStream.close();
            Toast.makeText(requireContext(), "Debt history PDF downloaded", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Failed to save PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void shareDebtHistoryPdf() {
        try {
            File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), buildFileName());
            FileOutputStream outputStream = new FileOutputStream(file);
            writeDebtHistoryPdf(outputStream);
            outputStream.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), "com.example.billgenerator.provider", file);
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(shareIntent, "Send debt history PDF"));
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Failed to send PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openDebtHistoryPdfForPrint() {
        try {
            File file = new File(requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), buildFileName());
            FileOutputStream outputStream = new FileOutputStream(file);
            writeDebtHistoryPdf(outputStream);
            outputStream.close();

            Uri uri = FileProvider.getUriForFile(requireContext(), "com.example.billgenerator.provider", file);
            android.content.Intent openIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            openIntent.setDataAndType(uri, "application/pdf");
            openIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(android.content.Intent.createChooser(openIntent, "Open PDF to print"));
        } catch (Exception ex) {
            Toast.makeText(requireContext(), "Failed to open PDF: " + ex.getMessage(), Toast.LENGTH_LONG).show();
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
