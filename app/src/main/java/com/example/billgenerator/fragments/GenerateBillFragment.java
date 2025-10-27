package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.AddItemAdapter;
import com.example.billgenerator.models.Item;
import com.example.billgenerator.R;
import com.example.billgenerator.models.SelectedItem;
import com.example.billgenerator.adapters.SelectedItemAdapter;
import com.example.billgenerator.database.databaseSystem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenerateBillFragment extends Fragment {

    private EditText nameEditText, phoneEditText, villageEditText, goldRateEditText, silverRateEditText;
    private RecyclerView selectedItemsRecyclerView;
    private TextView totalBillTextView;
    private Button addItemButton, generateBillButton;
    private databaseSystem dbHelper;
    private ArrayList<SelectedItem> selectedItemsList = new ArrayList<>();
    private SelectedItemAdapter selectedItemAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_bill_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Use getContext() when initializing helpers in Fragments
        dbHelper = new databaseSystem(getContext());

        // Initialize all views using the fragment's 'view' object
        nameEditText = view.findViewById(R.id.name_editText);
        phoneEditText = view.findViewById(R.id.phone_editText);
        villageEditText = view.findViewById(R.id.village_editText);
        goldRateEditText = view.findViewById(R.id.rate_gold_editText);
        silverRateEditText = view.findViewById(R.id.rate_silver_editText);
        selectedItemsRecyclerView = view.findViewById(R.id.item_recycler_view);
        totalBillTextView = view.findViewById(R.id.total_bill);
        addItemButton = view.findViewById(R.id.add_button_new_item);
        generateBillButton = view.findViewById(R.id.generate_bill_button);

        setupRecyclerView(view);

        addItemButton.setOnClickListener(v -> showAddItemDialog());
        generateBillButton.setOnClickListener(v -> generateBill());
    }

    private void setupRecyclerView(View view) {
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pass a callback to the adapter to handle item removal and total updates
        selectedItemAdapter = new SelectedItemAdapter(selectedItemsList, this::calculateTotal);
        selectedItemsRecyclerView.setAdapter(selectedItemAdapter);
    }

    private void showAddItemDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.add_new_item);
        RecyclerView itemsRecyclerView = dialog.findViewById(R.id.add_item_recycler_view);
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        List<Item> allItems = dbHelper.fetchAllItems();
        AddItemAdapter addItemAdapter = new AddItemAdapter(allItems, item -> {
            selectedItemsList.add(new SelectedItem(item.getId(), item.getName(), item.getWeight(), item.getType()));
            selectedItemAdapter.notifyDataSetChanged();
            calculateTotal();
            dialog.dismiss();
        });
        itemsRecyclerView.setAdapter(addItemAdapter);
        dialog.show();
    }

    private void calculateTotal() {
        double total = 0;
        double goldRate = 0;
        double silverRate = 0;

        try {
            goldRate = Double.parseDouble(goldRateEditText.getText().toString()) / 10; // per gram
            silverRate = Double.parseDouble(silverRateEditText.getText().toString()) / 1000; // per gram
        } catch (NumberFormatException e) {
            // Rates are not valid numbers, so total is 0
        }

        for (SelectedItem item : selectedItemsList) {
            if (item.getType().equalsIgnoreCase("Gold")) {
                total += item.getWeight() * goldRate;
            } else if (item.getType().equalsIgnoreCase("Silver")) {
                total += item.getWeight() * silverRate;
            }
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        totalBillTextView.setText(currencyFormat.format(total));
    }

    private void generateBill() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();
        String goldRateStr = goldRateEditText.getText().toString().trim();
        String silverRateStr = silverRateEditText.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || village.isEmpty() || goldRateStr.isEmpty() || silverRateStr.isEmpty() || selectedItemsList.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields and add items", Toast.LENGTH_SHORT).show();
            return;
        }

        long customerId = dbHelper.insertOrGetCustomer(name, phone, village);
        double goldRate = Double.parseDouble(goldRateStr);
        double silverRate = Double.parseDouble(silverRateStr);

        // Recalculate final total to be sure
        double totalAmount = 0;
        for (SelectedItem item : selectedItemsList) {
            if (item.getType().equalsIgnoreCase("Gold")) {
                totalAmount += item.getWeight() * (goldRate / 10);
            } else if (item.getType().equalsIgnoreCase("Silver")) {
                totalAmount += item.getWeight() * (silverRate / 1000);
            }
        }

        long billId = dbHelper.insertBill(customerId, goldRate, silverRate, totalAmount);

        if (billId != -1) {
            Toast.makeText(getContext(), "Bill Generated Successfully with ID: " + billId, Toast.LENGTH_LONG).show();
            clearForm();
        } else {
            Toast.makeText(getContext(), "Failed to generate bill", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearForm() {
        nameEditText.setText("");
        phoneEditText.setText("");
        villageEditText.setText("");
        goldRateEditText.setText("");
        silverRateEditText.setText("");
        selectedItemsList.clear();
        selectedItemAdapter.notifyDataSetChanged();
        totalBillTextView.setText("₹ 0.00");
    }
}