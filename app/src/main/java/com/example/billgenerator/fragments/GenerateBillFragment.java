package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton; // Import CompoundButton for Switch listener
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;


import com.example.billgenerator.adapters.AddItemAdapter;
// Models should ideally be in a sub-package like com.example.billgenerator.models
import com.example.billgenerator.models.Item;
import com.example.billgenerator.models.item_recycler_model_stocks; // Import for adapter creation
import com.example.billgenerator.R;
import com.example.billgenerator.models.SelectedItem;
import com.example.billgenerator.adapters.SelectedItemAdapter;
// Import the corrected generate_add_item_adapter
import com.example.billgenerator.adapters.generate_add_item_adapter;
import com.example.billgenerator.database.databaseSystem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GenerateBillFragment extends Fragment {

    // Views
    private TextInputEditText nameEditText, phoneEditText, villageEditText;
    private RecyclerView selectedItemsRecyclerView;
    private Button addItemButton, generateBillButton;
    private SwitchMaterial gstSwitch;
    private TextInputLayout gstPercentageLayout;
    private TextInputEditText gstPercentageEditText;
    private TextInputEditText manualTotalEditText;

    // Helpers
    private databaseSystem dbHelper;
    private ArrayList<SelectedItem> selectedItemsList = new ArrayList<>(); // Items currently in the bill
    private SelectedItemAdapter selectedItemAdapter; // Adapter for selectedItemsRecyclerView
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

        // Use getContext() or requireContext() safely after onViewCreated
        dbHelper = new databaseSystem(requireContext());

        // Initialize views
        nameEditText = view.findViewById(R.id.name_editText);
        phoneEditText = view.findViewById(R.id.phone_editText);
        villageEditText = view.findViewById(R.id.village_editText);
        selectedItemsRecyclerView = view.findViewById(R.id.item_recycler_view);
        addItemButton = view.findViewById(R.id.add_button_new_item);
        generateBillButton = view.findViewById(R.id.generate_bill_button);
        gstSwitch = view.findViewById(R.id.gst_switch);
        gstPercentageLayout = view.findViewById(R.id.gst_percentage_layout);
        gstPercentageEditText = view.findViewById(R.id.gst_percentage_editText);
        manualTotalEditText = view.findViewById(R.id.manual_total_editText);

        // Check view initialization
        if (nameEditText == null || phoneEditText == null || villageEditText == null ||
                selectedItemsRecyclerView == null || addItemButton == null || generateBillButton == null ||
                gstSwitch == null || gstPercentageLayout == null || gstPercentageEditText == null ||
                manualTotalEditText == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found. Check layout IDs in activity_bill_generator.xml.");
            Toast.makeText(getContext(), "Layout Error!", Toast.LENGTH_LONG).show();
            // Disable functionality if layout is broken
            if (generateBillButton != null) generateBillButton.setEnabled(false);
            if (addItemButton != null) addItemButton.setEnabled(false);
            return;
        }

        setupRecyclerView();
        setupGstSwitch();
        addItemButton.setOnClickListener(v -> showAddItemDialog());
        generateBillButton.setOnClickListener(v -> validateAndShowFinalizeDialog());
        Log.d(TAG, "View setup complete");
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView for selected items");
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Adapter for items *in* the bill
        selectedItemAdapter = new SelectedItemAdapter(selectedItemsList, null); // Callback removed
        selectedItemsRecyclerView.setAdapter(selectedItemAdapter);
    }

    private void setupGstSwitch() {
        Log.d(TAG, "Setting up GST Switch listener");
        gstSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "GST Switch toggled: " + isChecked);
            gstPercentageLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                gstPercentageEditText.setText(""); // Clear percentage if GST is turned off
            }
        });
    }

    // --- Dialog to show available stock items ---
    private void showAddItemDialog() {
        Log.d(TAG, "Showing Add Item dialog");
        final Dialog dialog = new Dialog(requireContext()); // Use requireContext() in fragments
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_new_item);
        dialog.setTitle("Select Item to Add");

        Window window = dialog.getWindow();
        if (window != null) { window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }

        RecyclerView itemsRecyclerView = dialog.findViewById(R.id.add_item_recycler_view);
        EditText searchEditText = dialog.findViewById(R.id.item_search_editText); // ID from add_new_item.xml

        if (itemsRecyclerView == null) {
            Log.e(TAG, "RecyclerView R.id.add_item_recycler_view not found in add_new_item.xml");
            Toast.makeText(getContext(), "Error: Dialog layout invalid.", Toast.LENGTH_SHORT).show();
            return;
        }

        itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Fetch ONLY available (unsold) items from DB
        List<Item> availableItemsList = dbHelper.fetchAllItems(); // Returns List<Item> of unsold items

        // The generate_add_item_adapter expects ArrayList<item_recycler_model_stocks>
        // We need to convert List<Item> to the required type.
        ArrayList<item_recycler_model_stocks> availableStockItems = new ArrayList<>();
        for(Item item : availableItemsList){
            availableStockItems.add(new item_recycler_model_stocks(
                    item.getId(), item.getName(), item.getWeight(), item.getType(), false // isSold is always false here
            ));
        }


        if (availableStockItems.isEmpty()){
            Log.d(TAG, "No available items in stock to add.");
            Toast.makeText(getContext(), "No available items in stock.", Toast.LENGTH_SHORT).show();
            // Display message in dialog instead of just dismissing
            TextView titleOrMsgView = dialog.findViewById(R.id.name_editText); // Reuse title TextView?
            if(titleOrMsgView != null) titleOrMsgView.setText("No Stock Available");
            if(searchEditText != null) searchEditText.setVisibility(View.GONE); // Hide search if no items
            // Don't return here, let the dialog show the message
        } else {
            Log.d(TAG, "Found " + availableStockItems.size() + " available items.");
        }

        // --- Create the adapter, passing 'this' fragment reference ---
        generate_add_item_adapter addItemAdapter = new generate_add_item_adapter(
                requireContext(),
                this, // Pass the GenerateBillFragment instance
                availableStockItems, // Pass the converted list
                selectedItemsList, // Pass the list of items currently in the bill (SelectedItem type)
                dialog
        );
        itemsRecyclerView.setAdapter(addItemAdapter);

        // Setup search listener only if the search EditText exists
        if (searchEditText != null) {
            Log.d(TAG, "Setting up search listener for AddItemDialog");
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Call the filter method in generate_add_item_adapter
                    addItemAdapter.filter(s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        } else {
            Log.w(TAG, "Search EditText R.id.item_search_editText not found in add_new_item.xml");
        }

        dialog.show();
    }


    // --- Validates inputs before showing the finalize dialog ---
    private void validateAndShowFinalizeDialog() {
        Log.d(TAG, "Validating bill details...");
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();
        String manualTotalStr = manualTotalEditText.getText().toString().trim();
        boolean isGstApplied = gstSwitch.isChecked();
        String gstPercentStr = gstPercentageEditText.getText().toString().trim();

        // --- Validation Logic ---
        if (name.isEmpty() || phone.isEmpty() || village.isEmpty() || manualTotalStr.isEmpty()) {
            Log.w(TAG,"Validation failed: Missing customer details or final amount.");
            Toast.makeText(getContext(),"Please fill customer details and final amount", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedItemsList.isEmpty()) {
            Log.w(TAG,"Validation failed: No items selected.");
            Toast.makeText(getContext(), "Please add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }
        double manualTotal;
        try {
            manualTotal = Double.parseDouble(manualTotalStr);
            if(manualTotal <= 0){
                Log.w(TAG,"Validation failed: Final amount <= 0.");
                Toast.makeText(getContext(),"Final Bill Amount must be positive",Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Log.w(TAG,"Validation failed: Invalid final amount format.");
            Toast.makeText(getContext(),"Invalid Final Bill Amount format", Toast.LENGTH_SHORT).show();
            return;
        }
        double gstPercent = 0.0;
        if (isGstApplied) {
            if (gstPercentStr.isEmpty()) {
                Log.w(TAG,"Validation failed: GST applied but percentage missing.");
                Toast.makeText(getContext(), "Please enter GST Percentage", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                gstPercent = Double.parseDouble(gstPercentStr);
                if(gstPercent < 0){
                    Log.w(TAG,"Validation failed: Negative GST percentage.");
                    Toast.makeText(getContext(),"GST Percentage cannot be negative",Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG,"Validation failed: Invalid GST percentage format.");
                Toast.makeText(getContext(), "Invalid GST Percentage format", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        // --- End Validation ---

        Log.d(TAG, "Validation successful. Showing finalize dialog.");
        showFinalizeBillDialog(manualTotal, gstPercent); // Pass original manual total
    }


    // --- Shows the Dialog for Payment, Debt, and Final Total Confirmation ---
    private void showFinalizeBillDialog(final double originalManualTotal, double gstPercent) {
        Log.d(TAG, "Showing Finalize Bill dialog. Original Amount: " + originalManualTotal + ", GST: " + gstPercent + "%");

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_finalize_bill);
        dialog.setCancelable(false); // Prevent accidental dismissal

        Window window = dialog.getWindow();
        if (window != null) { window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }

        // Find views within the finalize dialog layout
        TextView totalTextView = dialog.findViewById(R.id.dialog_bill_total_textview);
        EditText amountPaidEditText = dialog.findViewById(R.id.dialog_amount_paid_editText);
        SwitchMaterial updateTotalSwitch = dialog.findViewById(R.id.dialog_update_total_switch); // Find the switch
        TextView debtTextView = dialog.findViewById(R.id.dialog_debt_textview);
        TextView finalTotalTextView = dialog.findViewById(R.id.dialog_final_total_textview); // Find new TextView
        Button cancelButton = dialog.findViewById(R.id.dialog_cancel_button);
        Button saveButton = dialog.findViewById(R.id.dialog_save_button);

        // Check if all views were found
        if (totalTextView == null || amountPaidEditText == null || updateTotalSwitch == null ||
                debtTextView == null || finalTotalTextView == null || cancelButton == null || saveButton == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found in dialog_finalize_bill.xml. Check IDs.");
            Toast.makeText(getContext(), "Error: Finalize dialog layout invalid.", Toast.LENGTH_LONG).show();
            dialog.dismiss(); // Don't show a broken dialog
            return;
        }

        // Display the original total amount from the main screen
        totalTextView.setText(String.format("Original Total: %s", currencyFormat.format(originalManualTotal)));

        // --- Listener for Amount Paid EditText and Update Total Switch ---
        TextWatcher amountWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                // Update the display based on amount paid AND switch state
                updateDialogDisplay(s.toString(), originalManualTotal, updateTotalSwitch.isChecked(), debtTextView, finalTotalTextView);
            }
        };
        amountPaidEditText.addTextChangedListener(amountWatcher);

        updateTotalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Update Total Switch toggled: " + isChecked);
            // Trigger display update when switch changes, using current amount paid
            updateDialogDisplay(amountPaidEditText.getText().toString(), originalManualTotal, isChecked, debtTextView, finalTotalTextView);
        });

        // Initial display update based on default values (empty amount paid, switch off)
        updateDialogDisplay("", originalManualTotal, false, debtTextView, finalTotalTextView);


        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Finalize dialog cancelled.");
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked in finalize dialog.");
            String amountPaidStr = amountPaidEditText.getText().toString().trim();
            boolean updateBillTotal = updateTotalSwitch.isChecked(); // Get switch state

            if (amountPaidStr.isEmpty()) {
                Log.w(TAG, "Save failed: Amount received is empty.");
                Toast.makeText(getContext(), "Please enter amount received", Toast.LENGTH_SHORT).show();
                return;
            }

            double amountPaid;
            try {
                amountPaid = Double.parseDouble(amountPaidStr);
                if (amountPaid < 0) {
                    Log.w(TAG, "Save failed: Negative amount received.");
                    Toast.makeText(getContext(), "Amount received cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "Save failed: Invalid amount received format.");
                Toast.makeText(getContext(), "Invalid amount received format", Toast.LENGTH_SHORT).show();
                return;
            }

            double finalAmountToSave; // The total amount to be stored in the Bill record
            double debtToAdd = 0.0;   // The amount to add to the customer's debt record

            if (updateBillTotal) {
                // If switch is ON: Final bill amount = Amount Paid. No debt added for *this* bill.
                finalAmountToSave = amountPaid;
                debtToAdd = 0.0;
                Log.d(TAG, "Saving bill with UPDATED total matching amount paid: " + finalAmountToSave + ", No debt added for this transaction.");
            } else {
                // If switch is OFF: Final bill amount = Original Total. Calculate debt if needed.
                finalAmountToSave = originalManualTotal;
                // Calculate debt based on the ORIGINAL total vs amount paid
                if (originalManualTotal - amountPaid > 0.001) { // Use tolerance
                    debtToAdd = originalManualTotal - amountPaid;
                }
                Log.d(TAG, "Saving bill with ORIGINAL total: " + finalAmountToSave + ", Debt to add: " + debtToAdd);
            }

            // --- Proceed to save bill ---
            // Pass the determined finalAmountToSave and debtToAdd
            saveBillToDatabase(finalAmountToSave, gstPercent, debtToAdd);
            dialog.dismiss(); // Close dialog after initiating save
        });

        dialog.show();
        Log.d(TAG, "Finalize Bill dialog shown.");
    }

    // --- Helper to Update Debt/Final Total Text in Dialog ---
    private void updateDialogDisplay(String amountPaidStr, double originalTotal, boolean updateTotal,
                                     TextView debtTextView, TextView finalTotalTextView) {
        try {
            String input = amountPaidStr.trim();
            double amountPaid = input.isEmpty() ? 0.0 : Double.parseDouble(input);

            if (updateTotal) {
                // Switch is ON: Hide debt, show what the final total will become
                debtTextView.setVisibility(View.GONE);
                finalTotalTextView.setText(String.format(Locale.getDefault(),
                        "Final Bill Total will be: %s", currencyFormat.format(amountPaid)));
                finalTotalTextView.setVisibility(View.VISIBLE);
            } else {
                // Switch is OFF: Hide final total hint, calculate and show potential debt
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
            // If amount paid is invalid, hide both indicators
            debtTextView.setVisibility(View.GONE);
            finalTotalTextView.setVisibility(View.GONE);
        }
    }


    // --- Handles saving the bill and updating customer debt ---
    private void saveBillToDatabase(double finalTotalAmount, double gstPercent, double debtToAdd) {
        Log.i(TAG, "Attempting to save bill. Final Amount: " + finalTotalAmount + ", GST: " + gstPercent + "%, DebtToAdd: " + debtToAdd);
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();

        // 1. Get or create customer ID
        long customerId = dbHelper.insertOrGetCustomer(name, phone, village);
        if (customerId == -1) {
            Log.e(TAG, "CRITICAL: Failed to get or insert customer data for phone: " + phone);
            Toast.makeText(getContext(), "Error saving customer data. Bill not saved.", Toast.LENGTH_LONG).show();
            // TODO: Consider reverting item 'sold' status if customer save fails. This is complex.
            // Maybe mark items sold only *after* successful bill save?
            return;
        }
        Log.d(TAG, "Obtained customer ID: " + customerId);

        // 2. Update Customer Debt (only if debtToAdd is positive)
        if (debtToAdd > 0.001) { // Use tolerance
            Log.d(TAG, "Attempting to add debt " + debtToAdd + " to customer ID: " + customerId);
            int updatedRows = dbHelper.updateCustomerDebt(customerId, debtToAdd);
            if (updatedRows <= 0) {
                // Log error but continue saving the bill. Debt might be inconsistent.
                Log.e(TAG, "Failed to update debt for customer ID: " + customerId + ". Bill will still be saved.");
                Toast.makeText(getContext(), "Warning: Could not update customer debt record.", Toast.LENGTH_SHORT).show();
            } else {
                Log.i(TAG, "Successfully added debt " + debtToAdd + " to customer ID: " + customerId);
            }
        }

        // 3. Save the Bill record
        // Passing 0.0 for calculated rates as they are not determined here.
        long billId = dbHelper.insertBill(customerId, 0.0, 0.0, finalTotalAmount, gstPercent, selectedItemsList);

        if (billId != -1) {
            Log.i(TAG, "Bill #" + billId + " saved successfully!");
            Toast.makeText(getContext(), "Bill #" + billId + " Generated Successfully!", Toast.LENGTH_LONG).show();
            // Bill saved. Items were marked sold by generate_add_item_adapter when added.
            clearForm(); // Clear the form for the next bill
        } else {
            Log.e(TAG, "CRITICAL: Failed to save bill details to database for customer ID: " + customerId);
            Toast.makeText(getContext(), "Failed to save bill details.", Toast.LENGTH_LONG).show();
            // --- Attempt to Rollback Changes ---
            // Revert the debt update if it was applied
            if (debtToAdd > 0.001) {
                Log.w(TAG, "Bill save failed. Attempting to revert debt update for customer ID: " + customerId);
                dbHelper.updateCustomerDebt(customerId, -debtToAdd); // Subtract the debt we added
            }
            // Revert item 'sold' status (This is important but complex if items were marked sold too early)
            Log.w(TAG, "Attempting to mark items unsold again due to bill save failure...");
            for (SelectedItem item : selectedItemsList) {
                dbHelper.updateItemSoldStatus(item.getId(), false); // Mark back as not sold
            }
            Log.e(TAG, "Bill save failed. Debt reversal attempted. Item 'sold' status reverted. Items are still in the list on screen.");
            // Inform user items were not actually billed
            Toast.makeText(getContext(), "Items added back to stock due to save error.", Toast.LENGTH_LONG).show();
            // Do NOT clear the form here, let the user retry or modify.
            // clearForm();
        }
    }


    // Clears the input form
    private void clearForm() {
        Log.d(TAG, "Clearing the form.");
        if(nameEditText != null) nameEditText.setText("");
        if(phoneEditText != null) phoneEditText.setText("");
        if(villageEditText != null) villageEditText.setText("");
        if(manualTotalEditText != null) manualTotalEditText.setText("");
        if(gstSwitch != null) gstSwitch.setChecked(false); // Resets GST layout visibility via listener
        if(gstPercentageEditText != null) gstPercentageEditText.setText("");
        selectedItemsList.clear();
        if(selectedItemAdapter != null) selectedItemAdapter.notifyDataSetChanged();

        // Clear focus from the last edited field
        View currentFocus = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (currentFocus != null) {
            currentFocus.clearFocus();
        }
        // Optionally, request focus on the first field
        // if(nameEditText != null) nameEditText.requestFocus();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }
}