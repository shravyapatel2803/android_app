package com.example.billgenerator;

import android.app.Dialog;
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
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.customer_recycler_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.fragments.CustomerDetailsFragment;
import com.example.billgenerator.models.customer_recycler_model;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class customer_details_activity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton fab;
    customer_recycler_adapter adapter;
    ArrayList<customer_recycler_model> customerList = new ArrayList<>();
    databaseSystem dbHelper;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_details);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Customer Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new databaseSystem(this);
        recyclerView = findViewById(R.id.customer_detail_recyclerView);
        fab = findViewById(R.id.fab_add_customer);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Pass the activity context and the list to the adapter
        adapter = new customer_recycler_adapter(this, customerList, new CustomerDetailsFragment());
        recyclerView.setAdapter(adapter);

        loadCustomersFromDB();

        fab.setOnClickListener(v -> showAddCustomerDialog());
    }

    private void loadCustomersFromDB() {
        customerList.clear();
        Cursor cursor = dbHelper.fetchCustomers();
        if (cursor != null && cursor.getCount() > 0) {
            int idIndex = cursor.getColumnIndex("id");
            int nameIndex = cursor.getColumnIndex("name");
            int phoneIndex = cursor.getColumnIndex("phone");
            int villageIndex = cursor.getColumnIndex("village");
            int debtIndex = cursor.getColumnIndex("debt");


            while (cursor.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1 && phoneIndex != -1 && villageIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    String village = cursor.getString(villageIndex);
                    float debt = cursor.getFloat(debtIndex);
                    customerList.add(new customer_recycler_model(id, name, phone, village,debt));
                }
            }
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    // --- Inflate Menu and Handle Clicks ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.customer_details_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_generate_pdf) {
            generateCustomersPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- PDF Generation Logic ---
    private void generateCustomersPdf() {
        if (customerList.isEmpty()) {
            Toast.makeText(this, "No customers to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 842; // A4 landscape
        int pageHeight = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        int x = 30, y = 50;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(20f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Customer Report", x, y, titlePaint);
        y += 30;

        paint.setTextSize(10f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        paint.setTextSize(12f);
        paint.setFakeBoldText(true);
        canvas.drawText("Name", x, y, paint);
        canvas.drawText("Phone", x + 150, y, paint);
        canvas.drawText("Village", x + 280, y, paint);
        canvas.drawText("Associated Bill IDs", x + 450, y, paint);
        y += 20;
        canvas.drawLine(x, y-10, pageWidth - x, y-10, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);

        for (customer_recycler_model customer : customerList) {
            // Fetch the bill IDs for this customer
            String billIds = dbHelper.getBillIdsForCustomer(customer.id);

            canvas.drawText(customer.name, x, y, paint);
            canvas.drawText(customer.phone, x + 150, y, paint);
            canvas.drawText(customer.village, x + 280, y, paint);
            canvas.drawText(billIds.isEmpty() ? "N/A" : billIds, x + 450, y, paint); // Show N/A if no bills
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
        String fileName = "Customer_Report_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
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

    private void showAddCustomerDialog() {
        // This method remains unchanged
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog);

        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        Button saveButton = dialog.findViewById(R.id.save_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String village = editVillage.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || village.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertCustomer(name, phone, village);
                Toast.makeText(this, "Customer Saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB();
            }
        });
        dialog.show();
    }
}
