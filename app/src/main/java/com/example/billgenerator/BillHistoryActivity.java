package com.example.billgenerator;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private BillHistoryAdapter adapter;
    private ArrayList<BillHistoryModel> allBillsList = new ArrayList<>();
    private ArrayList<BillHistoryModel> filteredList = new ArrayList<>();
    private databaseSystem dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_history);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Bill History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new databaseSystem(this);
        recyclerView = findViewById(R.id.bill_history_recyclerView);
        setupRecyclerView();
        loadBillHistory();
    }

    private void setupRecyclerView() {
        adapter = new BillHistoryAdapter(this, filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadBillHistory() {
        allBillsList.clear();
        Cursor cursor = dbHelper.fetchBillHistory();
        if (cursor != null && cursor.moveToFirst()) {
            int billIdCol = cursor.getColumnIndex("id");
            int customerNameCol = cursor.getColumnIndex("name");
            int dateCol = cursor.getColumnIndex("bill_date");
            int totalCol = cursor.getColumnIndex("total_amount");

            do {
                int billId = cursor.getInt(billIdCol);
                String customerName = cursor.getString(customerNameCol);
                String rawDate = cursor.getString(dateCol);
                double totalAmount = cursor.getDouble(totalCol);
                String formattedDate = formatDate(rawDate);
                allBillsList.add(new BillHistoryModel(billId, customerName, formattedDate, totalAmount));
            } while (cursor.moveToNext());
            cursor.close();
        }
        filter(""); // Apply empty filter to load all data initially
    }

    private String formatDate(String dateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bill_history_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        // Add the save icon to this menu
        menu.add(Menu.NONE, R.id.action_generate_pdf, Menu.NONE, "Save as PDF")
                .setIcon(android.R.drawable.ic_menu_save)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search by name or bill ID...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_generate_pdf) {
            generateBillsPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allBillsList);
        } else {
            String searchText = text.toLowerCase(Locale.getDefault());
            for (BillHistoryModel bill : allBillsList) {
                if (bill.customerName.toLowerCase(Locale.getDefault()).contains(searchText) ||
                        String.valueOf(bill.billId).contains(searchText)) {
                    filteredList.add(bill);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void generateBillsPdf() {
        // Use the filteredList to export exactly what the user is seeing
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No bills to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595, pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        int x = 30, y = 50;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Bill History Report", x, y, titlePaint);
        y += 30;

        paint.setTextSize(10f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Bill ID", x, y, paint);
        canvas.drawText("Customer Name", x + 100, y, paint);
        canvas.drawText("Date", x + 300, y, paint);
        canvas.drawText("Total Amount", x + 450, y, paint);
        y += 20;
        canvas.drawLine(x, y - 10, pageWidth - x, y - 10, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);

        for (BillHistoryModel bill : filteredList) {
            canvas.drawText(String.valueOf(bill.billId), x, y, paint);
            canvas.drawText(bill.customerName, x + 100, y, paint);
            canvas.drawText(bill.billDate, x + 300, y, paint);

            // Format currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            canvas.drawText(currencyFormat.format(bill.totalAmount), x + 450, y, paint);
            y += 15;

            if (y > pageHeight - 50) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
        }
        document.finishPage(page);

        // Save the File using MediaStore
        ContentValues values = new ContentValues();
        String fileName = "Bill_History_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        if (uri != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    document.writeTo(outputStream);
                    outputStream.close();
                    Toast.makeText(this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        document.close();
    }
}
