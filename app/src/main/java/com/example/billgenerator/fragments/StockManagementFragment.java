package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// <-- FIXED: Removed unused Item and ItemAdapter imports
// import com.example.billgenerator.Item;
// import com.example.billgenerator.ItemAdapter;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// <-- FIXED: Import the correct adapter and model
import com.example.billgenerator.adapters.item_recycler_adapter_stocks;
import com.example.billgenerator.models.item_recycler_model_stocks;


import java.util.ArrayList;
import java.util.List; // Keep List if needed, but ArrayList is used

public class StockManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    // <-- FIXED: Changed adapter type
    private item_recycler_adapter_stocks adapter;
    // <-- FIXED: Changed list type to use the model that includes 'isSold'
    private ArrayList<item_recycler_model_stocks> itemList = new ArrayList<>();
    private databaseSystem dbHelper;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the correct layout for stock management
        return inflater.inflate(R.layout.activity_maintain, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new databaseSystem(getContext());
        // IDs were corrected previously, ensure they match activity_maintain.xml
        recyclerView = view.findViewById(R.id.stock_item_recyclerView);
        fab = view.findViewById(R.id.fab_add_stock_item);

        setupRecyclerView();
        loadItemsFromDB(); // Load initial data

        fab.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupRecyclerView() {
        // <-- FIXED: Initialize with the correct adapter, passing the itemList
        adapter = new item_recycler_adapter_stocks(requireContext(), itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadItemsFromDB() {
        itemList.clear(); // Clear the list before loading
        // <-- FIXED: Use fetchItems cursor, which includes is_sold
        Cursor cursor = dbHelper.fetchItems();
        if (cursor != null) {
            // Get column indices once
            int idCol = cursor.getColumnIndexOrThrow("id");
            int nameCol = cursor.getColumnIndexOrThrow("name");
            int weightCol = cursor.getColumnIndexOrThrow("weight");
            int typeCol = cursor.getColumnIndexOrThrow("type");
            int soldCol = cursor.getColumnIndexOrThrow("is_sold"); // Make sure this column exists

            while (cursor.moveToNext()) {
                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                double weight = cursor.getDouble(weightCol);
                String type = cursor.getString(typeCol);
                // <-- FIXED: Read the is_sold status correctly
                boolean isSold = cursor.getInt(soldCol) == 1;

                // <-- FIXED: Add the correct model type to the list
                itemList.add(new item_recycler_model_stocks(id, name, weight, type, isSold));
            }
            cursor.close();
        }
        // Notify the adapter that data has changed
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // showAddItemDialog remains the same as it correctly inserts into the DB
    private void showAddItemDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_item_dialog);

        final EditText editName = dialog.findViewById(R.id.edit_item_name);
        final EditText editWeight = dialog.findViewById(R.id.edit_item_weight);
        final RadioGroup typeGroup = dialog.findViewById(R.id.radio_group_type);
        Button saveButton = dialog.findViewById(R.id.save_item_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            if (name.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                int selectedTypeId = typeGroup.getCheckedRadioButtonId();
                if (selectedTypeId == -1) { // No radio button selected
                    Toast.makeText(getContext(), "Please select item type (Gold/Silver)", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton selectedType = dialog.findViewById(selectedTypeId);
                String type = selectedType.getText().toString();

                // Insert with isSold = false by default
                dbHelper.insertItem(name, weight, type, false);
                Toast.makeText(getContext(), "Item saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadItemsFromDB(); // Refresh the list in the RecyclerView
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Refresh data when the fragment becomes visible again
    @Override
    public void onResume() {
        super.onResume();
        // Reload data from DB in case changes were made elsewhere
        if (dbHelper != null) {
            loadItemsFromDB();
        }
    }
}