package com.example.billgenerator;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.generate_add_item_adapter;
import com.example.billgenerator.adapters.item_recycler_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.item_recycler_model;
import com.example.billgenerator.models.item_recycler_model_stocks;

import java.util.ArrayList;
import java.util.Locale;

public class BillGenerator extends AppCompatActivity {
    Toolbar toolbar;
    EditText name, phone, village, rate_gold, rate_silver;
    Button add_item, generate_Bill;
    RecyclerView item_recycler_view;
    TextView total_bill;
    databaseSystem dbHelper;

    ArrayList<item_recycler_model> billItemsList = new ArrayList<>();
    item_recycler_adapter billAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_generator);

        // Initialization
        dbHelper = new databaseSystem(this);
        name = findViewById(R.id.name_editText);
        phone = findViewById(R.id.phone_editText);
        village = findViewById(R.id.village_editText);
        rate_gold = findViewById(R.id.rate_gold_editText);
        rate_silver = findViewById(R.id.rate_silver_editText);
        total_bill = findViewById(R.id.total_bill);
        add_item = findViewById(R.id.add_button_new_item);
        generate_Bill = findViewById(R.id.generate_bill_button);
        item_recycler_view = findViewById(R.id.item_recycler_view);

        // Setup RecyclerView for the bill
        item_recycler_view.setLayoutManager(new LinearLayoutManager(this));
        billAdapter = new item_recycler_adapter(this, billItemsList);
        item_recycler_view.setAdapter(billAdapter);

        // Listeners
        add_item.setOnClickListener(v -> showAddItemDialog());
        generate_Bill.setOnClickListener(v -> calculateAndSaveBill()); // Changed the method name for clarity

        setupToolbar();
    }

    private void showAddItemDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_new_item);

        RecyclerView availableItemsRecyclerView = dialog.findViewById(R.id.add_item_recycler_view);
        availableItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // --- NEW: Find the search EditText ---
        EditText searchEditText = dialog.findViewById(R.id.item_search_editText);

        ArrayList<item_recycler_model_stocks> availableItems = fetchUnsoldItems();

        if (availableItems.isEmpty()) {
            Toast.makeText(this, "No available items in stock.", Toast.LENGTH_SHORT).show();
            return;
        }

        generate_add_item_adapter availableItemsAdapter = new generate_add_item_adapter(this, this, availableItems, billItemsList, dialog);
        availableItemsRecyclerView.setAdapter(availableItemsAdapter);

        // --- NEW: Add a TextWatcher to the search EditText ---
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call the adapter's filter method whenever the text changes
                availableItemsAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Not needed
            }
        });

        dialog.show();
    }


    private ArrayList<item_recycler_model_stocks> fetchUnsoldItems() {
        ArrayList<item_recycler_model_stocks> list = new ArrayList<>();
        Cursor cursor = dbHelper.fetchItems();

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex("id");
            int nameCol = cursor.getColumnIndex("name");
            int weightCol = cursor.getColumnIndex("weight");
            int typeCol = cursor.getColumnIndex("type");
            int soldCol = cursor.getColumnIndex("is_sold");

            do {
                boolean isSold = cursor.getInt(soldCol) == 1;
                if (!isSold) {
                    int id = cursor.getInt(idCol);
                    String itemName = cursor.getString(nameCol);
                    double weight = cursor.getDouble(weightCol);
                    String type = cursor.getString(typeCol);
                    list.add(new item_recycler_model_stocks(id, itemName, weight, type, false));
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    private void calculateAndSaveBill() {
        // --- Input Validation ---
        if (TextUtils.isEmpty(name.getText()) || TextUtils.isEmpty(phone.getText()) || TextUtils.isEmpty(village.getText()) || TextUtils.isEmpty(rate_gold.getText()) || TextUtils.isEmpty(rate_silver.getText())) {
            Toast.makeText(this, "Please fill all customer and rate fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (billItemsList.isEmpty()) {
            Toast.makeText(this, "Please add at least one item to the bill.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double goldRate = Double.parseDouble(rate_gold.getText().toString());
            double silverRate = Double.parseDouble(rate_silver.getText().toString());
            double total = 0;

            for (item_recycler_model item : billItemsList) {
                double weight = Double.parseDouble(item.weight);
                if (item.type.equalsIgnoreCase("Gold")) {
                    total += (weight * goldRate) / 10;
                } else if (item.type.equalsIgnoreCase("Silver")) {
                    total += (weight * silverRate) / 1000;
                }
            }
            total_bill.setText(String.format(Locale.getDefault(), "Total: %.2f", total));

            // --- SAVE EVERYTHING TO DATABASE ---
            saveBillAndCustomerData(goldRate, silverRate, total);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format in rates.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveBillAndCustomerData(double goldRate, double silverRate, double totalAmount) {
        String customerName = name.getText().toString().trim();
        String customerPhone = phone.getText().toString().trim();
        String customerVillage = village.getText().toString().trim();

        long customerId = -1;

        // Check if customer exists
        Cursor cursor = dbHelper.getCustomerByPhone(customerPhone);
        if (cursor != null && cursor.moveToFirst()) {
            // Customer exists, update their info
            customerId = cursor.getLong(cursor.getColumnIndexOrThrow("id"));
            dbHelper.updateCustomer(customerId, customerName, customerVillage);
        } else {
            // Customer is new, insert them
            customerId = dbHelper.insertCustomer(customerName, customerPhone, customerVillage);
        }
        if (cursor != null) {
            cursor.close();
        }

        if (customerId == -1) {
            Toast.makeText(this, "Error saving customer data.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save the main bill record
        long billId = dbHelper.insertBill(customerId, goldRate, silverRate, totalAmount);

        if (billId == -1) {
            Toast.makeText(this, "Error saving bill.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save each item associated with this bill
        for (item_recycler_model item : billItemsList) {
            dbHelper.insertBillItem(billId, item.id);
        }

        Toast.makeText(this, "Bill generated and saved successfully!", Toast.LENGTH_LONG).show();

        // Optional: Finish this activity or clear the fields for a new bill
        finish();
    }


    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Bill Generator");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bill_generator_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.view_bill) {
            // --- THIS IS THE CHANGE ---
            Intent intent = new Intent(BillGenerator.this, BillHistoryActivity.class);
            startActivity(intent);
        } else if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.customer_history) {
            Intent intent = new Intent(BillGenerator.this, customer_details_activity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
