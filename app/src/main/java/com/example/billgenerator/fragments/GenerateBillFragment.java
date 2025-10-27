package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
// <-- Import CompoundButton for Switch listener
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.adapters.generate_add_item_adapter;
import com.example.billgenerator.models.item_recycler_model_stocks;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;


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

    // Views
    private TextInputEditText nameEditText, phoneEditText, villageEditText;
    private RecyclerView selectedItemsRecyclerView;
    private SwitchMaterial gstSwitch;
    private TextInputLayout gstPercentageLayout;
    private TextInputEditText gstPercentageEditText;
    private TextInputEditText manualTotalEditText;

    // Helpers
    private databaseSystem dbHelper;
    private ArrayList<SelectedItem> selectedItemsList = new ArrayList<>();
    private SelectedItemAdapter selectedItemAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final String TAG = "GenerateBillFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.activity_bill_generator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        dbHelper = new databaseSystem(getContext());

        // Initialize views
        nameEditText = view.findViewById(R.id.name_editText);
        phoneEditText = view.findViewById(R.id.phone_editText);
        villageEditText = view.findViewById(R.id.village_editText);
        selectedItemsRecyclerView = view.findViewById(R.id.item_recycler_view);
        Button addItemButton = view.findViewById(R.id.add_button_new_item);
        Button generateBillButton = view.findViewById(R.id.generate_bill_button);
        gstSwitch = view.findViewById(R.id.gst_switch);
        gstPercentageLayout = view.findViewById(R.id.gst_percentage_layout);
        gstPercentageEditText = view.findViewById(R.id.gst_percentage_editText);
        manualTotalEditText = view.findViewById(R.id.manual_total_editText);

        if (nameEditText == null || /* ... other null checks ... */ manualTotalEditText == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found. Check layout IDs.");
            Toast.makeText(getContext(), "Layout Error!", Toast.LENGTH_LONG).show();
            return;
        }

        setupRecyclerView();
        setupGstSwitch();
        addItemButton.setOnClickListener(v -> showAddItemDialog());
        generateBillButton.setOnClickListener(v -> validateAndShowFinalizeDialog());
        Log.d(TAG, "View setup complete");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedItemAdapter = new SelectedItemAdapter(selectedItemsList, null);
        selectedItemsRecyclerView.setAdapter(selectedItemAdapter);
    }

    private void setupGstSwitch() {
        Log.d(TAG, "Setting up GST Switch listener");
        gstSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "GST Switch toggled: " + isChecked);
            gstPercentageLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                gstPercentageEditText.setText("");
            }
        });
    }

    private void showAddItemDialog() {
        // ... (Implementation remains the same as previous response) ...
        Log.d(TAG, "Showing Add Item dialog");
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_new_item);
        dialog.setTitle("Select Item to Add");
        Window window = dialog.getWindow(); if (window != null) { window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
        RecyclerView itemsRecyclerView = dialog.findViewById(R.id.add_item_recycler_view);
        EditText searchEditText = dialog.findViewById(R.id.item_search_editText);
        if (itemsRecyclerView == null) { Log.e(TAG, "RecyclerView R.id.add_item_recycler_view not found"); Toast.makeText(getContext(), "Error: Dialog layout invalid.", Toast.LENGTH_SHORT).show(); return; }
        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<Item> availableItemsList = dbHelper.fetchAllItems();
        ArrayList<item_recycler_model_stocks> availableStockItems = new ArrayList<>();
        for(Item item : availableItemsList){ availableStockItems.add(new item_recycler_model_stocks(item.getId(), item.getName(), item.getWeight(), item.getType(), false)); }
        if (availableStockItems.isEmpty()){ Log.d(TAG, "No available items."); Toast.makeText(getContext(), "No available items in stock.", Toast.LENGTH_SHORT).show(); TextView tv = dialog.findViewById(R.id.name_editText); if (tv != null) tv.setText("No items available"); return; }
        Log.d(TAG, "Found " + availableStockItems.size() + " available items.");
        generate_add_item_adapter addItemAdapter = new generate_add_item_adapter(requireContext(), this, availableStockItems, selectedItemsList, dialog);
        itemsRecyclerView.setAdapter(addItemAdapter);
        if (searchEditText != null) { Log.d(TAG, "Setting up search listener"); searchEditText.addTextChangedListener(new TextWatcher() { /*...*/ @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){} @Override public void onTextChanged(CharSequence s,int start,int before,int count){ addItemAdapter.filter(s.toString()); } @Override public void afterTextChanged(Editable s){} }); } else { Log.w(TAG, "Search EditText R.id.item_search_editText not found"); }
        dialog.show();
    }

    private void validateAndShowFinalizeDialog() {
        // ... (Validation logic remains the same as previous response) ...
        Log.d(TAG, "Validating bill details..."); String name = nameEditText.getText().toString().trim(); String phone = phoneEditText.getText().toString().trim(); String village = villageEditText.getText().toString().trim(); String manualTotalStr = manualTotalEditText.getText().toString().trim(); boolean isGstApplied = gstSwitch.isChecked(); String gstPercentStr = gstPercentageEditText.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty() || village.isEmpty() || manualTotalStr.isEmpty()) { /*...*/ Log.w(TAG,"Validation failed: Missing info"); Toast.makeText(getContext(),"Fill customer details and final amount", Toast.LENGTH_SHORT).show(); return; }
        if (selectedItemsList.isEmpty()) { /*...*/ Log.w(TAG,"Validation failed: No items"); Toast.makeText(getContext(), "Add at least one item", Toast.LENGTH_SHORT).show(); return; }
        double manualTotal; try { manualTotal = Double.parseDouble(manualTotalStr); if(manualTotal<=0){ /*...*/ Log.w(TAG,"Validation failed: Amount <= 0"); Toast.makeText(getContext(),"Amount must be positive",Toast.LENGTH_SHORT).show(); return; } } catch (NumberFormatException e) { /*...*/ Log.w(TAG,"Validation failed: Invalid amount format"); Toast.makeText(getContext(),"Invalid Final Bill Amount", Toast.LENGTH_SHORT).show(); return; }
        double gstPercent = 0.0; if (isGstApplied) { if (gstPercentStr.isEmpty()) { /*...*/ Log.w(TAG,"Validation failed: GST% empty"); Toast.makeText(getContext(), "Enter GST Percentage", Toast.LENGTH_SHORT).show(); return; } try { gstPercent = Double.parseDouble(gstPercentStr); if(gstPercent<0){ /*...*/ Log.w(TAG,"Validation failed: Negative GST%"); Toast.makeText(getContext(),"GST cannot be negative",Toast.LENGTH_SHORT).show(); return; } } catch (NumberFormatException e) { /*...*/ Log.w(TAG,"Validation failed: Invalid GST% format"); Toast.makeText(getContext(), "Invalid GST Percentage", Toast.LENGTH_SHORT).show(); return; } }

        Log.d(TAG, "Validation successful. Showing finalize dialog.");
        showFinalizeBillDialog(manualTotal, gstPercent); // Pass original manual total
    }


    // --- Shows the Dialog for Payment and Debt Calculation ---
    private void showFinalizeBillDialog(final double originalManualTotal, double gstPercent) { // Renamed parameter
        Log.d(TAG, "Showing Finalize Bill dialog. Original Amount: " + originalManualTotal + ", GST: " + gstPercent + "%");

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_finalize_bill);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) { window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }

        // Find views
        TextView totalTextView = dialog.findViewById(R.id.dialog_bill_total_textview);
        EditText amountPaidEditText = dialog.findViewById(R.id.dialog_amount_paid_editText);
        SwitchMaterial updateTotalSwitch = dialog.findViewById(R.id.dialog_update_total_switch); // Find the switch
        TextView debtTextView = dialog.findViewById(R.id.dialog_debt_textview);
        TextView finalTotalTextView = dialog.findViewById(R.id.dialog_final_total_textview); // Find new TextView
        Button cancelButton = dialog.findViewById(R.id.dialog_cancel_button);
        Button saveButton = dialog.findViewById(R.id.dialog_save_button);

        // Check views
        if (totalTextView == null || amountPaidEditText == null || updateTotalSwitch == null ||
                debtTextView == null || finalTotalTextView == null || cancelButton == null || saveButton == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found in dialog_finalize_bill.xml.");
            Toast.makeText(getContext(), "Error: Finalize dialog layout invalid.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        // Display the original total amount from the main screen
        totalTextView.setText(String.format("Original Total: %s", currencyFormat.format(originalManualTotal)));

        // --- Listener for TextWatcher and Switch ---
        TextWatcher amountWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateDialogDisplay(s.toString(), originalManualTotal, updateTotalSwitch.isChecked(), debtTextView, finalTotalTextView);
            }
        };
        amountPaidEditText.addTextChangedListener(amountWatcher);

        updateTotalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Update Total Switch toggled: " + isChecked);
            // Trigger display update when switch changes
            updateDialogDisplay(amountPaidEditText.getText().toString(), originalManualTotal, isChecked, debtTextView, finalTotalTextView);
        });

        // Initial display update
        updateDialogDisplay("", originalManualTotal, false, debtTextView, finalTotalTextView);


        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Finalize dialog cancelled.");
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked in finalize dialog.");
            String amountPaidStr = amountPaidEditText.getText().toString().trim();
            boolean updateBillTotal = updateTotalSwitch.isChecked();

            if (amountPaidStr.isEmpty()) { Log.w(TAG, "Save failed: Amount received empty."); Toast.makeText(getContext(), "Enter amount received", Toast.LENGTH_SHORT).show(); return; }

            double amountPaid;
            try { amountPaid = Double.parseDouble(amountPaidStr); if (amountPaid < 0) { Log.w(TAG, "Save failed: Negative amount."); Toast.makeText(getContext(), "Amount received cannot be negative", Toast.LENGTH_SHORT).show(); return; } }
            catch (NumberFormatException e) { Log.w(TAG, "Save failed: Invalid amount format."); Toast.makeText(getContext(), "Invalid amount received format", Toast.LENGTH_SHORT).show(); return; }

            double finalAmountToSave;
            double debtToAdd = 0.0;

            if (updateBillTotal) {
                // Update the bill total TO the amount paid
                finalAmountToSave = amountPaid;
                debtToAdd = 0.0; // No debt is added in this case
                Log.d(TAG, "Saving bill with updated total: " + finalAmountToSave + ", No debt added.");
            } else {
                // Keep the original bill total
                finalAmountToSave = originalManualTotal;
                // Calculate debt based on the ORIGINAL total
                if (originalManualTotal > amountPaid) {
                    debtToAdd = originalManualTotal - amountPaid;
                }
                Log.d(TAG, "Saving bill with original total: " + finalAmountToSave + ", Debt to add: " + debtToAdd);
            }

            // Proceed to save bill using finalAmountToSave and debtToAdd
            saveBillToDatabase(finalAmountToSave, gstPercent, debtToAdd);
            dialog.dismiss();
        });

        dialog.show();
        Log.d(TAG, "Finalize Bill dialog shown.");
    }

    // --- NEW HELPER: Updates the Debt and Final Total TextViews in the Dialog ---
    private void updateDialogDisplay(String amountPaidStr, double originalTotal, boolean updateTotal,
                                     TextView debtTextView, TextView finalTotalTextView) {
        try {
            String input = amountPaidStr.trim();
            double amountPaid = input.isEmpty() ? 0.0 : Double.parseDouble(input);

            if (updateTotal) {
                // If updating total, show what the new total will be, hide debt
                debtTextView.setVisibility(View.GONE);
                finalTotalTextView.setText(String.format(Locale.getDefault(),
                        "Final Bill Total will be: %s", currencyFormat.format(amountPaid)));
                finalTotalTextView.setVisibility(View.VISIBLE);
            } else {
                // If NOT updating total, calculate and show potential debt, hide final total hint
                finalTotalTextView.setVisibility(View.GONE);
                double remainingDebt = originalTotal - amountPaid;
                if (remainingDebt > 0.001) { // Use tolerance
                    debtTextView.setText(String.format(Locale.getDefault(),
                            "Remaining Debt: %s will be added.", currencyFormat.format(remainingDebt)));
                    debtTextView.setVisibility(View.VISIBLE);
                } else {
                    debtTextView.setVisibility(View.GONE); // Hide if fully paid or overpaid
                }
            }
        } catch (NumberFormatException e) {
            // If amount paid is invalid, hide both debt and final total hints
            debtTextView.setVisibility(View.GONE);
            finalTotalTextView.setVisibility(View.GONE);
        }
    }


    // --- Handles saving the bill and updating customer debt ---
    private void saveBillToDatabase(double finalTotalAmount, double gstPercent, double debtToAdd) {
        // ... (Implementation remains the same as previous response) ...
        Log.i(TAG, "Attempting to save bill. Final Amount: " + finalTotalAmount + ", GST: " + gstPercent + "%, DebtToAdd: " + debtToAdd); String name = nameEditText.getText().toString().trim(); String phone = phoneEditText.getText().toString().trim(); String village = villageEditText.getText().toString().trim();
        long customerId = dbHelper.insertOrGetCustomer(name, phone, village);
        if (customerId == -1) { Log.e(TAG, "CRITICAL: Failed get/insert customer: " + phone); Toast.makeText(getContext(), "Error saving customer data.", Toast.LENGTH_LONG).show(); return; }
        Log.d(TAG, "Obtained customer ID: " + customerId);
        if (debtToAdd > 0.001) { Log.d(TAG, "Attempting update debt for customer ID: " + customerId); int updatedRows = dbHelper.updateCustomerDebt(customerId, debtToAdd); if (updatedRows <= 0) { Log.e(TAG, "Failed update debt for ID: " + customerId); Toast.makeText(getContext(), "Warning: Could not update debt.", Toast.LENGTH_SHORT).show(); } else { Log.i(TAG, "Successfully added debt " + debtToAdd + " to ID: " + customerId); } }
        long billId = dbHelper.insertBill(customerId, 0.0, 0.0, finalTotalAmount, gstPercent, selectedItemsList);
        if (billId != -1) { Log.i(TAG, "Bill #" + billId + " saved!"); Toast.makeText(getContext(), "Bill #" + billId + " Generated!", Toast.LENGTH_LONG).show(); clearForm(); }
        else { Log.e(TAG, "CRITICAL: Failed save bill for ID: " + customerId); Toast.makeText(getContext(), "Failed save bill details.", Toast.LENGTH_LONG).show(); if (debtToAdd > 0.001) { Log.w(TAG, "Attempting revert debt for ID: " + customerId); dbHelper.updateCustomerDebt(customerId, -debtToAdd); } Log.e(TAG, "Bill save failed. Debt reversal attempted. Item 'sold' inconsistent!"); }
    }


    // Clears the input form
    private void clearForm() {
        // ... (Implementation remains the same as previous response) ...
        Log.d(TAG, "Clearing form."); nameEditText.setText(""); phoneEditText.setText(""); villageEditText.setText(""); manualTotalEditText.setText(""); gstSwitch.setChecked(false); gstPercentageEditText.setText(""); selectedItemsList.clear(); selectedItemAdapter.notifyDataSetChanged(); View currentFocus = getActivity()!=null?getActivity().getCurrentFocus():null; if(currentFocus!=null){currentFocus.clearFocus();}
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }
}