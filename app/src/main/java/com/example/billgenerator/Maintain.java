package com.example.billgenerator;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.content.ContentValues;
import android.net.Uri;
import java.io.OutputStream;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.item_recycler_adapter_stocks;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model_stocks;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Maintain extends AppCompatActivity {

    // --- UI Components ---
    Toolbar toolbar;
    RecyclerView recyclerView;
    FloatingActionButton fab;

    // --- Database & Data ---
    databaseSystem dbHelper;
    ArrayList<item_recycler_model_stocks> allItemsList = new ArrayList<>();
    ArrayList<item_recycler_model_stocks> filteredList = new ArrayList<>();
    item_recycler_adapter_stocks adapter;

    // --- NEW: For PDF Permissions ---
    private static final int PERMISSION_REQUEST_CODE = 100;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintain);

        // --- Toolbar Setup ---
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // --- Initialization ---
        dbHelper = new databaseSystem(this);
        recyclerView = findViewById(R.id.stock_item_recyclerView);
        fab = findViewById(R.id.fab_add_stock_item);

        // --- Setup and Load Data ---
        setupRecyclerView();
        loadItemsFromDB();

        // --- Click Listeners ---
        fab.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupRecyclerView() {
        adapter = new item_recycler_adapter_stocks(this, filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadItemsFromDB() {
        allItemsList.clear();
        Cursor cursor = dbHelper.fetchItems();

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex("id");
            int nameCol = cursor.getColumnIndex("name");
            int weightCol = cursor.getColumnIndex("weight");
            int typeCol = cursor.getColumnIndex("type");
            int soldCol = cursor.getColumnIndex("is_sold");

            do {
                if (idCol != -1 && nameCol != -1 && weightCol != -1 && typeCol != -1 && soldCol != -1) {
                    int id = cursor.getInt(idCol);
                    String name = cursor.getString(nameCol);
                    double weight = cursor.getDouble(weightCol);
                    String type = cursor.getString(typeCol);
                    boolean isSold = cursor.getInt(soldCol) == 1;
                    allItemsList.add(new item_recycler_model_stocks(id, name, weight, type, isSold));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        filteredList.clear();
        filteredList.addAll(allItemsList);
        adapter.notifyDataSetChanged();
    }


    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allItemsList);
        } else {
            text = text.toLowerCase(Locale.getDefault());
            for (item_recycler_model_stocks item : allItemsList) {
                if (item.getName().toLowerCase(Locale.getDefault()).contains(text) ||
                        item.getType().toLowerCase(Locale.getDefault()).contains(text)) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maintain_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search by name or type...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    // --- NEW: Handle clicks on menu items (Search and PDF) ---

    // --- NEW: Check for permissions before generating the PDF ---
    private void checkPermissionAndGeneratePdf() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        } else {
            // Permission has already been granted
            generateStockPdf();
        }
    }

    // --- NEW: Handle the result of the permission request ---
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted
                generateStockPdf();
            } else {
                // Permission was denied
                Toast.makeText(this, "Permission Denied. Cannot save PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- UPDATED: Handle clicks on menu items ---
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_generate_pdf) {
            // Directly call the PDF generation method. No permission check is needed.
            generateStockPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- NEW & MODERN: The core PDF generation logic using MediaStore ---
    private void generateStockPdf() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No items to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- All the drawing logic remains the same ---
        PdfDocument document = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        int x = 30, y = 50;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Stock Report", x, y, titlePaint);
        y += 30;

        paint.setTextSize(10f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("ID", x, y, paint);
        canvas.drawText("Name", x + 50, y, paint);
        canvas.drawText("Weight (g)", x + 250, y, paint);
        canvas.drawText("Type", x + 350, y, paint);
        canvas.drawText("Status", x + 450, y, paint);
        y += 20;
        canvas.drawLine(x, y - 10, pageWidth - x, y - 10, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);

        for (item_recycler_model_stocks item : filteredList) {
            canvas.drawText(String.valueOf(item.getId()), x, y, paint);
            canvas.drawText(item.getName(), x + 50, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.3f", item.getWeight()), x + 250, y, paint);
            canvas.drawText(item.getType(), x + 350, y, paint);
            canvas.drawText(item.isSold() ? "Sold" : "Available", x + 450, y, paint);
            y += 15;
            if (y > pageHeight - 50) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
        }
        document.finishPage(page);

        // --- NEW & MODERN: Save the File using MediaStore ---

        // 1. Create the metadata for the new file
        ContentValues values = new ContentValues();
        String fileName = "Stock_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        // 2. Get a URI (a path-like handle) from MediaStore
        // The 'resolver' is what communicates with the MediaStore
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        if (uri != null) {
            try {
                // 3. Open an output stream using the URI
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    // 4. Write the PDF to the output stream
                    document.writeTo(outputStream);
                    outputStream.close();
                    Toast.makeText(this, "PDF saved to Downloads folder!", Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }

        // 5. Always close the document
        document.close();
    }

    private void showAddItemDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_item_dialog);

        EditText editName = dialog.findViewById(R.id.edit_item_name);
        EditText editWeight = dialog.findViewById(R.id.edit_item_weight);
        RadioGroup typeGroup = dialog.findViewById(R.id.radio_group_type);
        Button saveButton = dialog.findViewById(R.id.save_item_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();

            if (name.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                int selectedTypeId = typeGroup.getCheckedRadioButtonId();
                RadioButton selectedType = dialog.findViewById(selectedTypeId);
                String type = selectedType.getText().toString();

                dbHelper.insertItem(name, weight, type, false);
                Toast.makeText(this, "Item saved successfully!", Toast.LENGTH_SHORT).show();

                dialog.dismiss();
                loadItemsFromDB();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid weight", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }
}
