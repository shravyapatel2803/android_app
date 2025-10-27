package com.example.billgenerator.fragments;

// <-- FIXED: NOTE: This file will NOT compile.
// It requires "Item.java" and "ItemAdapter.java" which are missing from your project.

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

import com.example.billgenerator.models.Item;
import com.example.billgenerator.adapters.ItemAdapter;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class StockManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList = new ArrayList<>();
    private databaseSystem dbHelper;
    private FloatingActionButton fab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_maintain, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new databaseSystem(getContext());
        // <-- FIXED: The ID in your layout is "stock_item_recyclerView", not "stock_recycler_view"
        recyclerView = view.findViewById(R.id.stock_item_recyclerView);
        // <-- FIXED: The ID in your layout is "fab_add_stock_item", not "fab_add_item"
        fab = view.findViewById(R.id.fab_add_stock_item);

        setupRecyclerView();
        loadItemsFromDB();

        fab.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(itemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadItemsFromDB() {
        itemList.clear();
        Cursor cursor = dbHelper.fetchAllItemsCursor();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                String type = cursor.getString(cursor.getColumnIndexOrThrow("type"));
                itemList.add(new Item(id, name, weight, type));
            }
            cursor.close();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

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
                RadioButton selectedType = dialog.findViewById(selectedTypeId);
                String type = selectedType.getText().toString();

                dbHelper.insertItem(name, weight, type, false);
                Toast.makeText(getContext(), "Item saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadItemsFromDB(); // Refresh the list
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid weight", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadItemsFromDB();
        }
    }
}