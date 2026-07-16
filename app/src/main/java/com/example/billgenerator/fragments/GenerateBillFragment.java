package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListAdapter;
import android.widget.Filterable;
import android.widget.Filter;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;

import com.example.billgenerator.adapters.ReturnItemAdapter;
import com.example.billgenerator.adapters.DraftAdapter;
import com.example.billgenerator.models.DraftItem;
import com.example.billgenerator.models.Item;
import com.example.billgenerator.models.ReturnItem;
import com.example.billgenerator.models.item_recycler_model_stocks;
import com.example.billgenerator.R;
import com.example.billgenerator.models.SelectedItem;
import com.example.billgenerator.adapters.SelectedItemAdapter;
import com.example.billgenerator.adapters.generate_add_item_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.ui.UiAnimationHelper;
import com.example.billgenerator.utils.CurrencyUtils;
import com.example.billgenerator.utils.QrCodeGenerator;
import com.example.billgenerator.utils.PdfUtils;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class GenerateBillFragment extends Fragment {

    private AutoCompleteTextView nameEditText, phoneEditText;
    private TextInputEditText villageEditText;
    private RecyclerView selectedItemsRecyclerView;
    private Button addItemButton, generateBillButton, scanBarcodeButton;
    private SwitchMaterial gstSwitch, returnItemSwitch;
    private TextInputLayout gstPercentageLayout;
    private TextInputEditText gstPercentageEditText;
    private TextInputEditText manualTotalEditText;
    private RadioGroup paymentModeRadioGroup, returnItemTypeRadioGroup;
    private TextInputEditText paymentDetailsEditText, returnItemWeightEditText, returnItemDeductAmountEditText;
    private LinearLayout returnItemDetailsLayout;
    private View generateBillItemsEmpty;
    private RecyclerView returnItemsRecyclerView;
    private Button addReturnItemButton;
    private ImageView kycImageView;
    private View kycContainer;
    private String currentKycPhotoPath = null;

    private databaseSystem dbHelper;
    private ArrayList<SelectedItem> selectedItemsList = new ArrayList<>();
    private ArrayList<ReturnItem> returnItemsList = new ArrayList<>();
    private SelectedItemAdapter selectedItemAdapter;
    private ReturnItemAdapter returnItemAdapter;
    private GmsBarcodeScanner scanner;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final String TAG = "GenerateBillFragment";
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView customerLookupHint;
    private LinearLayout recentCustomersContainer;
    private boolean applyingSuggestion = false;
    private final ArrayList<CustomerSuggestion> nameSuggestions = new ArrayList<>();
    private final ArrayList<CustomerSuggestion> phoneSuggestions = new ArrayList<>();
    
    private static ArrayList<item_recycler_model_stocks> cachedStockItems = null;
    private static final Object CACHE_LOCK = new Object();
    private Dialog reusableAddItemDialog;
    private generate_add_item_adapter addItemAdapter;
    private ArrayList<item_recycler_model_stocks> dialogStockItems = new ArrayList<>();
    
    private int editingBillId = -1;
    private int pendingEditBillId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build();
        scanner = GmsBarcodeScanning.getClient(requireContext(), options);
    }

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

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                if (editingBillId != -1) {
                    MenuItem cancelEdit = menu.add(0, 100, 0, "Cancel Edit");
                    cancelEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                MenuItem parkBill = menu.add(0, 101, 0, "Park Bill");
                parkBill.setIcon(R.drawable.ic_inventory); // Reusing an icon
                parkBill.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                
                MenuItem resumeDraft = menu.add(0, 102, 0, "Saved Drafts");
                resumeDraft.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

                MenuItem clearForm = menu.add(0, 103, 0, "Clear Form");
                clearForm.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                clearForm.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == 100) {
                    clearForm();
                    requireActivity().invalidateOptionsMenu();
                    Toast.makeText(getContext(), "Editing cancelled", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (menuItem.getItemId() == 101) {
                    parkCurrentBill();
                    return true;
                } else if (menuItem.getItemId() == 102) {
                    showDraftsDialog();
                    return true;
                } else if (menuItem.getItemId() == 103) {
                    confirmClearForm();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        dbHelper = new databaseSystem(requireContext());

        nameEditText = view.findViewById(R.id.name_editText);
        phoneEditText = view.findViewById(R.id.phone_editText);
        villageEditText = view.findViewById(R.id.village_editText);
        customerLookupHint = view.findViewById(R.id.customer_lookup_hint);
        recentCustomersContainer = view.findViewById(R.id.recent_customers_container);
        selectedItemsRecyclerView = view.findViewById(R.id.item_recycler_view);
        addItemButton = view.findViewById(R.id.add_button_new_item);
        scanBarcodeButton = view.findViewById(R.id.btn_scan_barcode);
        generateBillButton = view.findViewById(R.id.generate_bill_button);
        gstSwitch = view.findViewById(R.id.gst_switch);
        gstPercentageLayout = view.findViewById(R.id.gst_percentage_layout);
        gstPercentageEditText = view.findViewById(R.id.gst_percentage_editText);
        manualTotalEditText = view.findViewById(R.id.manual_total_editText);
        paymentModeRadioGroup = view.findViewById(R.id.payment_mode_radio_group);
        paymentDetailsEditText = view.findViewById(R.id.payment_details_editText);
        returnItemSwitch = view.findViewById(R.id.return_item_switch);
        returnItemDetailsLayout = view.findViewById(R.id.return_item_details_layout);
        returnItemTypeRadioGroup = view.findViewById(R.id.return_item_type_radio_group);
        returnItemWeightEditText = view.findViewById(R.id.return_item_weight_editText);
        returnItemDeductAmountEditText = view.findViewById(R.id.return_item_deduct_amount_editText);
        generateBillItemsEmpty = view.findViewById(R.id.generate_bill_items_empty);
        returnItemsRecyclerView = view.findViewById(R.id.return_items_recycler_view);
        addReturnItemButton = view.findViewById(R.id.add_return_item_button);
        kycImageView = view.findViewById(R.id.img_customer_kyc);
        kycContainer = view.findViewById(R.id.kyc_photo_container);

        loadSettingsAndApplyVisibility();

        if (kycImageView != null) {
            kycImageView.setOnClickListener(v -> takeKycPhoto());
        }


        if (nameEditText == null || phoneEditText == null || villageEditText == null ||
                selectedItemsRecyclerView == null || addItemButton == null || generateBillButton == null ||
                gstSwitch == null || gstPercentageLayout == null || gstPercentageEditText == null ||
            manualTotalEditText == null || paymentModeRadioGroup == null || paymentDetailsEditText == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found. Check layout IDs in activity_bill_generator.xml.");
            Toast.makeText(getContext(), "Layout Error!", Toast.LENGTH_LONG).show();
            if (generateBillButton != null) generateBillButton.setEnabled(false);
            if (addItemButton != null) addItemButton.setEnabled(false);
            return;
        }

        setupRecyclerView();
        setupReturnItemsRecyclerView();
        setupGstSwitch();
        setupPaymentMode();
        setupReturnItemSwitch();
        setupCustomerAutoFill();
        loadRecentCustomerChips();
        addItemButton.setOnClickListener(v -> showAddItemDialog());
        if (scanBarcodeButton != null) {
            scanBarcodeButton.setOnClickListener(v -> startBarcodeScanning());
        }
        
        // Setup Indian currency formatting for manual total
        CurrencyUtils.setupAmountFormatter(manualTotalEditText);
        CurrencyUtils.setupAmountFormatter(returnItemDeductAmountEditText);

        generateBillButton.setOnClickListener(v -> validateAndShowFinalizeDialog());
        
        // Pre-load stock items if not already cached
        preloadStockItems();

        if (pendingEditBillId != -1) {
            int idToLoad = pendingEditBillId;
            pendingEditBillId = -1;
            mainHandler.postDelayed(() -> loadBillForEditing(idToLoad), 200);
        }

        Log.d(TAG, "View setup complete");
    }

    private void parkCurrentBill() {
        String name = nameEditText.getText().toString().trim();
        if (selectedItemsList.isEmpty() && name.isEmpty()) {
            Toast.makeText(getContext(), "Nothing to park", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject draft = new JSONObject();
            draft.put("name", name);
            draft.put("phone", phoneEditText.getText().toString().trim());
            draft.put("village", villageEditText.getText().toString().trim());
            draft.put("total", manualTotalEditText.getText().toString().trim());
            
            JSONArray itemsArr = new JSONArray();
            for (SelectedItem item : selectedItemsList) {
                JSONObject itemObj = new JSONObject();
                itemObj.put("id", item.getId());
                itemObj.put("name", item.getName());
                itemObj.put("weight", item.getWeight());
                itemObj.put("type", item.getType());
                itemsArr.put(itemObj);
            }
            draft.put("items", itemsArr);

            String customerDisplayName = name.isEmpty() ? "Unnamed Customer" : name;
            databaseSystem.databaseExecutor.execute(() -> {
                dbHelper.insertDraft(customerDisplayName, draft.toString());
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Bill Parked Successfully", Toast.LENGTH_SHORT).show();
                    
                    // IMPORTANT: When parking, items were already marked SOLD in DB 
                    // (done in generate_add_item_adapter). We KEEP them SOLD.
                    // This is consistent because we want to RESERVE them for this draft.
                    
                    clearForm();
                });
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parking bill", e);
        }
    }

    private void showDraftsDialog() {
        databaseSystem.databaseExecutor.execute(() -> {
            Cursor cursor = dbHelper.fetchDrafts();
            ArrayList<DraftItem> drafts = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    drafts.add(new DraftItem(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("customer_name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("draft_data")),
                        cursor.getString(cursor.getColumnIndexOrThrow("created_at"))
                    ));
                }
                cursor.close();
            }

            mainHandler.post(() -> {
                if (drafts.isEmpty()) {
                    Toast.makeText(getContext(), "No saved drafts found", Toast.LENGTH_SHORT).show();
                    return;
                }

                Dialog dialog = new Dialog(requireContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_saved_drafts);
                UiAnimationHelper.applyDialogAnimations(dialog);
                
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                RecyclerView recyclerView = dialog.findViewById(R.id.drafts_recycler_view);
                Button closeButton = dialog.findViewById(R.id.btn_close_drafts);

                DraftAdapter adapter = new DraftAdapter(drafts, new DraftAdapter.OnDraftInteractionListener() {
                    @Override
                    public void onResume(DraftItem draft) {
                        resumeDraft(draft);
                        dialog.dismiss();
                    }

                    @Override
                    public void onDelete(DraftItem draft) {
                        new AlertDialog.Builder(requireContext())
                            .setTitle("Delete Draft")
                            .setMessage("Deleting this draft will also return its items to stock. Are you sure?")
                            .setPositiveButton("Delete", (d, which) -> {
                                databaseSystem.databaseExecutor.execute(() -> {
                                    // REVERT items to UNSOLD before deleting draft
                                    try {
                                        JSONObject obj = new JSONObject(draft.jsonData);
                                        JSONArray itemsArr = obj.optJSONArray("items");
                                        if (itemsArr != null) {
                                            for (int i = 0; i < itemsArr.length(); i++) {
                                                int itemId = itemsArr.getJSONObject(i).getInt("id");
                                                dbHelper.updateItemSoldStatus(itemId, false);
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error reverting items on draft delete", e);
                                    }

                                    dbHelper.deleteDraft(draft.id);
                                    mainHandler.post(() -> {
                                        drafts.remove(draft);
                                        if (drafts.isEmpty()) {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Drafts cleared", Toast.LENGTH_SHORT).show();
                                        } else {
                                            recyclerView.getAdapter().notifyDataSetChanged();
                                        }
                                    });
                                });
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                    }
                });

                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                
                closeButton.setOnClickListener(v -> dialog.dismiss());
                dialog.show();
            });
        });
    }

    private void resumeDraft(DraftItem draftItem) {
        try {
            clearForm();
            JSONObject draft = new JSONObject(draftItem.jsonData);
            nameEditText.setText(draft.optString("name"), false);
            phoneEditText.setText(draft.optString("phone"), false);
            villageEditText.setText(draft.optString("village"));
            manualTotalEditText.setText(draft.optString("total"));

            JSONArray itemsArr = draft.getJSONArray("items");
            for (int i = 0; i < itemsArr.length(); i++) {
                JSONObject itemObj = itemsArr.getJSONObject(i);
                selectedItemsList.add(new SelectedItem(
                    itemObj.getInt("id"),
                    itemObj.getString("name"),
                    itemObj.getDouble("weight"),
                    itemObj.getString("type")
                ));
            }
            selectedItemAdapter.notifyDataSetChanged();
            updateSelectedItemsEmptyState();
            
            databaseSystem.databaseExecutor.execute(() -> dbHelper.deleteDraft(draftItem.id));
            Toast.makeText(getContext(), "Draft Resumed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error resuming draft", e);
        }
    }

    private void preloadStockItems() {
        synchronized (CACHE_LOCK) {
            if (cachedStockItems != null) return;
        }
        
        databaseSystem.databaseExecutor.execute(() -> {
            try {
                List<Item> availableItemsList = dbHelper.fetchAllItems();
                ArrayList<item_recycler_model_stocks> loadedItems = new ArrayList<>();
                for(Item item : availableItemsList){
                    loadedItems.add(new item_recycler_model_stocks(
                            item.getId(), item.getName(), item.getWeight(), item.getType(), false
                    ));
                }
                synchronized (CACHE_LOCK) {
                    cachedStockItems = loadedItems;
                }
                Log.d(TAG, "Stock items pre-loaded into cache: " + loadedItems.size());
            } catch (Exception e) {
                Log.e(TAG, "Error pre-loading stock items", e);
            }
        });
    }

    private void setupReturnItemsRecyclerView() {
        if (returnItemsRecyclerView != null) {
            returnItemsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            returnItemAdapter = new ReturnItemAdapter(returnItemsList, position -> {
                returnItemsList.remove(position);
                returnItemAdapter.notifyItemRemoved(position);
            });
            returnItemsRecyclerView.setAdapter(returnItemAdapter);
        }

        if (addReturnItemButton != null) {
            addReturnItemButton.setOnClickListener(v -> {
                int selectedId = returnItemTypeRadioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = returnItemTypeRadioGroup.findViewById(selectedId);
                String type = radioButton != null ? radioButton.getText().toString() : "Unknown";
                String weightStr = returnItemWeightEditText.getText().toString();
                String deductStr = returnItemDeductAmountEditText.getText().toString();

                if (TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(deductStr)) {
                    Toast.makeText(requireContext(), "Please enter weight and amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String cleanWeight = weightStr.replace(",", "");
                    String cleanDeduct = deductStr.replace(",", "");
                    double weight = Double.parseDouble(cleanWeight);
                    double deduct = Double.parseDouble(cleanDeduct);
                    returnItemsList.add(new ReturnItem(type, weight, deduct));
                    returnItemAdapter.notifyItemInserted(returnItemsList.size() - 1);

                    // Clear inputs
                    returnItemWeightEditText.setText("");
                    returnItemDeductAmountEditText.setText("");
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupCustomerAutoFill() {
        if (phoneEditText == null || nameEditText == null) {
            return;
        }

        nameEditText.setOnItemClickListener((parent, view1, position, id) -> {
            CustomerSuggestion suggestion = (CustomerSuggestion) parent.getItemAtPosition(position);
            if (suggestion != null) {
                applyCustomerSuggestion(suggestion);
                // Explicitly fix the text to ensure no phone number is appended
                nameEditText.post(() -> {
                    applyingSuggestion = true;
                    nameEditText.setText(suggestion.name, false);
                    nameEditText.setSelection(suggestion.name.length());
                    applyingSuggestion = false;
                });
            }
        });

        phoneEditText.setOnItemClickListener((parent, view1, position, id) -> {
            CustomerSuggestion suggestion = (CustomerSuggestion) parent.getItemAtPosition(position);
            if (suggestion != null) {
                applyCustomerSuggestion(suggestion);
                // Explicitly fix the text
                phoneEditText.post(() -> {
                    applyingSuggestion = true;
                    phoneEditText.setText(suggestion.phone, false);
                    phoneEditText.setSelection(suggestion.phone.length());
                    applyingSuggestion = false;
                });
            }
        });

        phoneEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                lookupCustomerByPhone();
            }
        });

        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingSuggestion) {
                    return;
                }
                String nameText = s == null ? "" : s.toString().trim();
                loadNameSuggestions(nameText);
            }
        });

        phoneEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (applyingSuggestion) {
                    return;
                }
                String phone = s == null ? "" : s.toString().trim();
                loadPhoneSuggestions(phone);
                if (phone.length() < 10) {
                    if (customerLookupHint != null) {
                        customerLookupHint.setVisibility(View.GONE);
                        customerLookupHint.setText("");
                    }
                    return;
                }
                lookupCustomerByPhone();
            }
        });
    }

    private void loadNameSuggestions(String queryText) {
        nameSuggestions.clear();
        if (TextUtils.isEmpty(queryText) || queryText.length() < 1) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.searchCustomersByName(queryText, 8);
            while (cursor != null && cursor.moveToNext()) {
                nameSuggestions.add(readSuggestionFromCursor(cursor));
            }
            if (!nameSuggestions.isEmpty()) {
                MultiCustomerAdapter adapter = new MultiCustomerAdapter(requireContext(), new ArrayList<>(nameSuggestions), true);
                nameEditText.setAdapter(adapter);
                nameEditText.showDropDown();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading name suggestions", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void loadPhoneSuggestions(String queryText) {
        phoneSuggestions.clear();
        if (TextUtils.isEmpty(queryText) || queryText.length() < 3) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.searchCustomersByPhone(queryText, 8);
            while (cursor != null && cursor.moveToNext()) {
                phoneSuggestions.add(readSuggestionFromCursor(cursor));
            }
            if (!phoneSuggestions.isEmpty()) {
                MultiCustomerAdapter adapter = new MultiCustomerAdapter(requireContext(), new ArrayList<>(phoneSuggestions), false);
                phoneEditText.setAdapter(adapter);
                phoneEditText.showDropDown();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading phone suggestions", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static class MultiCustomerAdapter extends ArrayAdapter<CustomerSuggestion> implements Filterable {
        private final ArrayList<CustomerSuggestion> customers;
        private final boolean isNameQuery;

        public MultiCustomerAdapter(android.content.Context context, ArrayList<CustomerSuggestion> customers, boolean isNameQuery) {
            super(context, android.R.layout.simple_dropdown_item_1line, customers);
            this.customers = customers;
            this.isNameQuery = isNameQuery;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            }
            TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
            CustomerSuggestion suggestion = getItem(position);
            if (suggestion != null) {
                textView.setText(isNameQuery ? suggestion.name + " • " + suggestion.phone : suggestion.phone + " • " + suggestion.name);
            }
            return convertView;
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    results.values = customers;
                    results.count = customers.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    if (resultValue instanceof CustomerSuggestion) {
                        return isNameQuery ? ((CustomerSuggestion) resultValue).name : ((CustomerSuggestion) resultValue).phone;
                    }
                    return super.convertResultToString(resultValue);
                }
            };
        }
    }

    private CustomerSuggestion readSuggestionFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
        String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
        String village = cursor.getString(cursor.getColumnIndexOrThrow("village"));
        double debt = cursor.getDouble(cursor.getColumnIndexOrThrow("debt"));
        return new CustomerSuggestion(id, name, phone, village, debt);
    }

    private void applyCustomerSuggestion(CustomerSuggestion suggestion) {
        applyingSuggestion = true;
        nameEditText.setText(suggestion.name, false);
        phoneEditText.setText(suggestion.phone, false);
        villageEditText.setText(suggestion.village);
        applyingSuggestion = false;
        updateCustomerLookupHint(suggestion.debt, true);
    }

    private void loadRecentCustomerChips() {
        if (recentCustomersContainer == null) {
            return;
        }
        recentCustomersContainer.removeAllViews();

        Cursor cursor = null;
        try {
            cursor = dbHelper.fetchRecentCustomers(8);
            int count = 0;
            while (cursor != null && cursor.moveToNext()) {
                CustomerSuggestion suggestion = new CustomerSuggestion(
                        cursor.getInt(cursor.getColumnIndexOrThrow("customer_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        cursor.getString(cursor.getColumnIndexOrThrow("village")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("debt"))
                );
                Chip chip = new Chip(requireContext());
                // Display both name and phone in the chip for clarity
                chip.setText(suggestion.name + " • " + suggestion.phone);
                chip.setClickable(true);
                chip.setCheckable(false);
                chip.setChipMinHeight(44f);
                chip.setTextSize(12f);
                chip.setEnsureMinTouchTargetSize(true);
                chip.setChipStartPadding(12f);
                chip.setChipEndPadding(12f);
                chip.setChipBackgroundColorResource(com.google.android.material.R.color.m3_chip_background_color);
                LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                chipParams.setMargins(0, 0, 12, 8);
                chip.setLayoutParams(chipParams);
                chip.setOnClickListener(v -> applyCustomerSuggestion(suggestion));
                recentCustomersContainer.addView(chip);
                count++;
            }
            if (count == 0) {
                recentCustomersContainer.setVisibility(View.GONE);
            } else {
                recentCustomersContainer.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recent customer chips", e);
            recentCustomersContainer.setVisibility(View.GONE);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void lookupCustomerByPhone() {
        if (phoneEditText == null) {
            return;
        }

        String phone = phoneEditText.getText() == null ? "" : phoneEditText.getText().toString().trim();
        if (phone.length() < 10) {
            return;
        }

        Cursor cursor = null;
        try {
            cursor = dbHelper.getCustomerByPhone(phone);
            if (cursor != null && cursor.moveToFirst()) {
                String existingName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String existingVillage = cursor.getString(cursor.getColumnIndexOrThrow("village"));
                double existingDebt = cursor.getDouble(cursor.getColumnIndexOrThrow("debt"));

                if (nameEditText != null && TextUtils.isEmpty(nameEditText.getText())) {
                    nameEditText.setText(existingName);
                }
                if (villageEditText != null && TextUtils.isEmpty(villageEditText.getText())) {
                    villageEditText.setText(existingVillage);
                }

                if (customerLookupHint != null) {
                    updateCustomerLookupHint(existingDebt, true);
                }
            } else if (customerLookupHint != null) {
                updateCustomerLookupHint(0.0, false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while looking up customer by phone", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void updateCustomerLookupHint(double debtAmount, boolean existingCustomer) {
        if (customerLookupHint == null) {
            return;
        }
        if (!existingCustomer) {
            customerLookupHint.setText("New customer. Details will be saved on bill generation.");
            customerLookupHint.setTextColor(Color.WHITE);
            customerLookupHint.setVisibility(View.VISIBLE);
            return;
        }

        if (debtAmount > 0.001) {
            customerLookupHint.setText(String.format(Locale.getDefault(), "Existing customer found. Outstanding debt: %s", currencyFormat.format(debtAmount)));
            customerLookupHint.setTextColor(Color.WHITE);
        } else {
            customerLookupHint.setText("Existing customer found. No outstanding debt.");
            customerLookupHint.setTextColor(Color.WHITE);
        }
        customerLookupHint.setVisibility(View.VISIBLE);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView for selected items");
        selectedItemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedItemAdapter = new SelectedItemAdapter(selectedItemsList, null);
        selectedItemsRecyclerView.setAdapter(selectedItemAdapter);
        UiAnimationHelper.setupRecyclerViewAnimations(selectedItemsRecyclerView);
        UiAnimationHelper.configureEmptyState(
                generateBillItemsEmpty,
                R.drawable.ic_empty_items,
                "No items added yet",
                "Add items from stock to build the invoice.",
                "Add Item",
                () -> {
                    if (addItemButton != null) {
                        addItemButton.performClick();
                    }
                }
        );
        updateSelectedItemsEmptyState();
    }

    public void updateSelectedItemsEmptyState() {
        UiAnimationHelper.setVisible(generateBillItemsEmpty, selectedItemsList.isEmpty());
        if (selectedItemsRecyclerView != null && !selectedItemsList.isEmpty()) {
            selectedItemsRecyclerView.scheduleLayoutAnimation();
        }
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

    private void setupPaymentMode() {
        paymentModeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.online_radio_button) {
                paymentDetailsEditText.setVisibility(View.VISIBLE);
            } else {
                paymentDetailsEditText.setVisibility(View.GONE);
            }
        });
    }

    private void setupReturnItemSwitch() {
        returnItemSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            returnItemDetailsLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) {
                returnItemWeightEditText.setText("");
                returnItemDeductAmountEditText.setText("");
            }
        });
    }

    private void showAddItemDialog() {
        if (reusableAddItemDialog == null) {
            reusableAddItemDialog = new Dialog(requireContext());
            reusableAddItemDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            reusableAddItemDialog.setContentView(R.layout.add_new_item);
            UiAnimationHelper.applyDialogAnimations(reusableAddItemDialog);
            
            Window window = reusableAddItemDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            RecyclerView itemsRecyclerView = reusableAddItemDialog.findViewById(R.id.add_item_recycler_view);
            EditText searchEditText = reusableAddItemDialog.findViewById(R.id.item_search_editText);
            View closeButton = reusableAddItemDialog.findViewById(R.id.add_item_dialog_close_button);

            if (closeButton != null) {
                closeButton.setOnClickListener(v -> reusableAddItemDialog.dismiss());
            }

            itemsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            itemsRecyclerView.setHasFixedSize(true);
            itemsRecyclerView.setItemViewCacheSize(20);
            itemsRecyclerView.setDrawingCacheEnabled(true);
            itemsRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            
            // Initialize with cached items if available
            synchronized (CACHE_LOCK) {
                if (cachedStockItems != null) {
                    dialogStockItems.clear();
                    dialogStockItems.addAll(cachedStockItems);
                }
            }

            addItemAdapter = new generate_add_item_adapter(
                    requireContext(),
                    this,
                    dialogStockItems,
                    selectedItemsList,
                    reusableAddItemDialog
            );
            itemsRecyclerView.setAdapter(addItemAdapter);

            if (searchEditText != null) {
                searchEditText.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                        addItemAdapter.filter(s.toString());
                    }
                    @Override public void afterTextChanged(Editable s) {}
                });
            }
        }

        // Show dialog immediately
        reusableAddItemDialog.show();

        // Refresh data silently in background
        databaseSystem.databaseExecutor.execute(() -> {
            try {
                List<Item> items = dbHelper.fetchAllItems();
                ArrayList<item_recycler_model_stocks> loaded = new ArrayList<>();
                for (Item item : items) {
                    loaded.add(new item_recycler_model_stocks(item.getId(), item.getName(), item.getWeight(), item.getType(), false));
                }
                
                synchronized (CACHE_LOCK) {
                    cachedStockItems = loaded;
                }

                mainHandler.post(() -> {
                    if (reusableAddItemDialog != null && reusableAddItemDialog.isShowing() && addItemAdapter != null) {
                        addItemAdapter.updateData(loaded);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing stock data", e);
            }
        });
    }

    private void loadSettingsAndApplyVisibility() {
        SharedPreferences sp = requireContext().getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE);
        boolean showKyc = sp.getBoolean("enable_customer_photo", true);
        if (kycContainer != null) {
            kycContainer.setVisibility(showKyc ? View.VISIBLE : View.GONE);
        }
    }

    private void takeKycPhoto() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, 1001);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == android.app.Activity.RESULT_OK && data != null) {
            android.graphics.Bitmap imageBitmap = (android.graphics.Bitmap) data.getExtras().get("data");
            if (kycImageView != null) {
                kycImageView.setImageBitmap(imageBitmap);
                kycImageView.setPadding(0, 0, 0, 0);
            }
            saveKycPhotoLocally(imageBitmap);
        }
    }

    private void saveKycPhotoLocally(android.graphics.Bitmap bitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "KYC_" + timeStamp + ".jpg";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = new File(storageDir, fileName);
        try (FileOutputStream out = new FileOutputStream(imageFile)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
            currentKycPhotoPath = imageFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Error saving KYC photo", e);
        }
    }

    private void validateAndShowFinalizeDialog() {
        Log.d(TAG, "Validating bill details...");
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();
        String manualTotalStr = manualTotalEditText.getText().toString().trim();
        boolean isGstApplied = gstSwitch.isChecked();
        String gstPercentStr = gstPercentageEditText.getText().toString().trim();

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
            String cleanTotal = manualTotalStr.replace(",", "");
            manualTotal = Double.parseDouble(cleanTotal);
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

        Log.d(TAG, "Validation successful. Showing finalize dialog.");
        showFinalizeBillDialog(manualTotal, gstPercent);
    }

    private void showFinalizeBillDialog(final double originalManualTotal, double gstPercent) {
        Log.d(TAG, "Showing Finalize Bill dialog. Original Amount: " + originalManualTotal + ", GST: " + gstPercent + "%");

        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_finalize_bill);
        UiAnimationHelper.applyDialogAnimations(dialog);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) { window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }

        TextView totalTextView = dialog.findViewById(R.id.dialog_bill_total_textview);
        EditText amountPaidEditText = dialog.findViewById(R.id.dialog_amount_paid_editText);
        SwitchMaterial updateTotalSwitch = dialog.findViewById(R.id.dialog_update_total_switch);
        TextView debtTextView = dialog.findViewById(R.id.dialog_debt_textview);
        LinearLayout dueDateContainer = dialog.findViewById(R.id.dialog_due_date_container);
        TextView dueDateTextView = dialog.findViewById(R.id.dialog_due_date_text);
        com.google.android.material.button.MaterialButton pickDueDateButton = dialog.findViewById(R.id.dialog_pick_due_date_button);
        TextView pdcDateTextView = dialog.findViewById(R.id.dialog_pdc_date_text);
        com.google.android.material.button.MaterialButton pickPdcButton = dialog.findViewById(R.id.dialog_pick_pdc_button);
        View pdcContainer = dialog.findViewById(R.id.dialog_pdc_container);
        TextView finalTotalTextView = dialog.findViewById(R.id.dialog_final_total_textview);
        View upiQrContainer = dialog.findViewById(R.id.upi_qr_container);
        android.widget.ImageView upiQrImage = dialog.findViewById(R.id.upi_qr_image);
        Button cancelButton = dialog.findViewById(R.id.dialog_cancel_button);
        Button saveButton = dialog.findViewById(R.id.dialog_save_button);

        SharedPreferences sp = requireContext().getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE);
        boolean showPdc = sp.getBoolean("enable_pdc", true);
        if (pdcContainer != null) pdcContainer.setVisibility(showPdc ? View.VISIBLE : View.GONE);

        if (totalTextView == null || amountPaidEditText == null || updateTotalSwitch == null ||
                debtTextView == null || dueDateContainer == null || dueDateTextView == null || pickDueDateButton == null ||
                finalTotalTextView == null || cancelButton == null || saveButton == null) {
            Log.e(TAG, "CRITICAL ERROR: One or more views not found in dialog_finalize_bill.xml. Check IDs.");
            Toast.makeText(getContext(), "Error: Finalize dialog layout invalid.", Toast.LENGTH_LONG).show();
            dialog.dismiss();
            return;
        }

        // Load Shop UPI Details
        android.content.SharedPreferences shopPrefs = requireContext().getSharedPreferences("shop_profile_prefs", android.content.Context.MODE_PRIVATE);
        String upiId = shopPrefs.getString("upi_id", "");
        String shopName = shopPrefs.getString("shop_name", "Shop");

        double deduction = 0;
        if (returnItemSwitch.isChecked()) {
            for (ReturnItem item : returnItemsList) {
                deduction += item.getDeductAmount();
            }
        }
        double finalTotal = originalManualTotal - deduction;

        totalTextView.setText(String.format("Original Total: %s", currencyFormat.format(finalTotal)));

        // Setup Indian currency formatting for amount paid in dialog
        CurrencyUtils.setupAmountFormatter(amountPaidEditText);

        final String[] selectedDueDateIso = {null};
        pickDueDateButton.setOnClickListener(v -> showDueDatePicker(selectedDueDateIso, dueDateTextView));

        final String[] selectedPdcDateIso = {null};
        if (pickPdcButton != null) {
            pickPdcButton.setOnClickListener(v -> showDueDatePicker(selectedPdcDateIso, pdcDateTextView));
        }

        TextWatcher amountWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateDialogDisplay(s.toString(), finalTotal, updateTotalSwitch.isChecked(), debtTextView, dueDateContainer, finalTotalTextView, upiId, shopName, upiQrContainer, upiQrImage);
            }
        };
        amountPaidEditText.addTextChangedListener(amountWatcher);

        updateTotalSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Update Total Switch toggled: " + isChecked);
            updateDialogDisplay(amountPaidEditText.getText().toString(), finalTotal, isChecked, debtTextView, dueDateContainer, finalTotalTextView, upiId, shopName, upiQrContainer, upiQrImage);
        });

        updateDialogDisplay("", finalTotal, false, debtTextView, dueDateContainer, finalTotalTextView, upiId, shopName, upiQrContainer, upiQrImage);


        cancelButton.setOnClickListener(v -> {
            Log.d(TAG, "Finalize dialog cancelled.");
            dialog.dismiss();
        });

        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Save button clicked in finalize dialog.");
            String amountPaidStr = amountPaidEditText.getText().toString().trim();
            boolean updateBillTotal = updateTotalSwitch.isChecked();

            if (amountPaidStr.isEmpty()) {
                Log.w(TAG, "Save failed: Amount received is empty.");
                Toast.makeText(getContext(), "Please enter amount received", Toast.LENGTH_SHORT).show();
                return;
            }

            double amountPaid;
            try {
                String cleanAmountPaid = amountPaidStr.replace(",", "");
                amountPaid = Double.parseDouble(cleanAmountPaid);
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

            double finalAmountToSave;
            double debtToAdd = 0.0;

            if (updateBillTotal) {
                finalAmountToSave = amountPaid;
                debtToAdd = 0.0;
                Log.d(TAG, "Saving bill with UPDATED total matching amount paid: " + finalAmountToSave + ", No debt added for this transaction.");
            } else {
                finalAmountToSave = finalTotal;
                if (finalTotal - amountPaid > 0.001) { 
                    debtToAdd = finalTotal - amountPaid;
                }
                Log.d(TAG, "Saving bill with ORIGINAL total: " + finalAmountToSave + ", Debt to add: " + debtToAdd);
            }

            if (debtToAdd > 0.001 && TextUtils.isEmpty(selectedDueDateIso[0])) {
                Toast.makeText(getContext(), "Please select debt due date for reminder.", Toast.LENGTH_SHORT).show();
                return;
            }

            String phone = phoneEditText.getText() == null ? "" : phoneEditText.getText().toString().trim();
            boolean looksDuplicate = !TextUtils.isEmpty(phone)
                    && dbHelper.hasRecentSimilarBillByPhone(phone, finalTotal, 10);

            final double saveFinalAmountToSave = finalAmountToSave;
            final double saveGstPercent = gstPercent;
            final double saveDebtToAdd = debtToAdd;
            final String saveDebtDueDate = selectedDueDateIso[0];
            final String savePdcDate = selectedPdcDateIso[0];
            final double saveBilledAmount = updateBillTotal ? amountPaid : finalTotal;
            final double saveAmountPaid = amountPaid;

            if (looksDuplicate) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Possible Duplicate Bill")
                        .setMessage("A bill with the same customer phone and similar billed amount was saved in the last 10 minutes. Save anyway?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save Anyway", (d, which) -> {
                            saveBillToDatabase(saveFinalAmountToSave, saveGstPercent, saveDebtToAdd, saveDebtDueDate, saveBilledAmount, saveAmountPaid, savePdcDate);
                            dialog.dismiss();
                        })
                        .show();
                return;
            }

            saveBillToDatabase(saveFinalAmountToSave, saveGstPercent, saveDebtToAdd, saveDebtDueDate, saveBilledAmount, saveAmountPaid, savePdcDate);
            dialog.dismiss();
        });

        dialog.show();
        Log.d(TAG, "Finalize Bill dialog shown.");
    }

    private void updateDialogDisplay(String amountPaidStr, double originalTotal, boolean updateTotal,
                                     TextView debtTextView, LinearLayout dueDateContainer, TextView finalTotalTextView,
                                     String upiId, String shopName, View upiQrContainer, android.widget.ImageView upiQrImage) {
        double qrAmount = originalTotal;
        try {
            String input = amountPaidStr.trim().replace(",", "");
            double amountPaid = input.isEmpty() ? 0.0 : Double.parseDouble(input);

            if (amountPaid > 0) {
                qrAmount = amountPaid;
            }

            if (updateTotal) {
                debtTextView.setVisibility(View.GONE);
                dueDateContainer.setVisibility(View.GONE);
                finalTotalTextView.setText(String.format(Locale.getDefault(),
                        "Final Bill Total will be: %s", currencyFormat.format(amountPaid)));
                finalTotalTextView.setVisibility(View.VISIBLE);
            } else {
                finalTotalTextView.setVisibility(View.GONE);
                double remainingDebt = originalTotal - amountPaid;
                if (remainingDebt > 0.001) {
                    debtTextView.setText(String.format(Locale.getDefault(),
                            "Remaining Debt: %s will be added.", currencyFormat.format(remainingDebt)));
                    debtTextView.setVisibility(View.VISIBLE);
                    dueDateContainer.setVisibility(View.VISIBLE);
                } else if (remainingDebt < -0.001) {
                    debtTextView.setText(String.format(Locale.getDefault(),
                            "Extra payment (credit): %s will reduce old debt.", currencyFormat.format(Math.abs(remainingDebt))));
                    debtTextView.setVisibility(View.VISIBLE);
                    dueDateContainer.setVisibility(View.GONE);
                } else {
                    debtTextView.setVisibility(View.GONE);
                    dueDateContainer.setVisibility(View.GONE);
                }
            }
        } catch (NumberFormatException e) {
            debtTextView.setVisibility(View.GONE);
            dueDateContainer.setVisibility(View.GONE);
            finalTotalTextView.setVisibility(View.GONE);
        }

        // Show/Update QR if UPI ID exists
        if (upiId != null && !upiId.isEmpty() && upiQrContainer != null && upiQrImage != null) {
            upiQrContainer.setVisibility(View.VISIBLE);
            String qrAmountStr = String.format(Locale.US, "%.2f", qrAmount);
            String lastQrAmount = (String) upiQrImage.getTag();
            if (lastQrAmount == null || !lastQrAmount.equals(qrAmountStr)) {
                android.graphics.Bitmap qr = QrCodeGenerator.generateUpiQrCode(upiId, shopName, qrAmountStr, "Jewelry Purchase");
                if (qr != null) {
                    upiQrImage.setImageBitmap(qr);
                    upiQrImage.setTag(qrAmountStr);
                }
            }
        } else if (upiQrContainer != null) {
            upiQrContainer.setVisibility(View.GONE);
        }
    }

    private void showDueDatePicker(String[] selectedDueDateIso, TextView dueDateTextView) {
        final Calendar now = Calendar.getInstance();
        DatePickerDialog pickerDialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            selectedDueDateIso[0] = isoFormat.format(selected.getTime());
            dueDateTextView.setText("Due date: " + displayFormat.format(selected.getTime()));
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        pickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        pickerDialog.show();
    }

    private void revertBillEffects(int billId) {
        Log.d(TAG, "Reverting effects of Bill #" + billId + " for update.");
        Cursor billCursor = dbHelper.getBillDetails(billId);
        if (billCursor != null && billCursor.moveToFirst()) {
            try {
                long custId = billCursor.getLong(billCursor.getColumnIndexOrThrow("customer_id"));
                double total = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                double debt = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));
                int billedIdx = billCursor.getColumnIndex("billed_amount");
                int paidIdx = billCursor.getColumnIndex("paid_amount");
                double billed = billedIdx != -1 ? billCursor.getDouble(billedIdx) : total;
                double paid = paidIdx != -1 ? billCursor.getDouble(paidIdx) : (total - debt);

                // Revert debt: If debt was added, subtract it. If credit was added, add it back.
                double netChange = billed - paid;
                if (Math.abs(netChange) > 0.001) {
                    dbHelper.updateCustomerDebt(custId, -netChange);
                }

                // Revert inventory: Mark items as unsold
                Cursor itemCursor = dbHelper.getItemsForBill(billId);
                if (itemCursor != null) {
                    while (itemCursor.moveToNext()) {
                        int itemId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow("id"));
                        dbHelper.updateItemSoldStatus(itemId, false);
                    }
                    itemCursor.close();
                }

                // Delete the old bill record and its mappings (items, returns)
                dbHelper.deleteBill(billId);

            } catch (Exception e) {
                Log.e(TAG, "Error reverting bill effects", e);
            } finally {
                billCursor.close();
            }
        }
    }

    private void saveBillToDatabase(double finalTotalAmount, double gstPercent, double debtToAdd, String debtDueDateIso, double billedAmount, double paidAmount, String pdcDate) {
        Log.i(TAG, "Attempting to save bill. Final Amount: " + finalTotalAmount + ", GST: " + gstPercent + "%, DebtToAdd: " + debtToAdd);
        
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String village = villageEditText.getText().toString().trim();
        String paymentDetails = paymentDetailsEditText.getText().toString().trim();

        int selectedPaymentModeId = paymentModeRadioGroup.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = paymentModeRadioGroup.findViewById(selectedPaymentModeId);
        String paymentMode = selectedRadioButton != null ? selectedRadioButton.getText().toString() : "Cash";

        // Snapshot the items to save, to avoid concurrent modification if list is cleared
        ArrayList<SelectedItem> itemsToSave = new ArrayList<>(selectedItemsList);
        ArrayList<ReturnItem> returnsToSave = new ArrayList<>(returnItemsList);
        boolean isReturnApplied = returnItemSwitch.isChecked();
        String kycPath = currentKycPhotoPath;

        databaseSystem.databaseExecutor.execute(() -> {
            try {
                if (editingBillId != -1) {
                    revertBillEffects(editingBillId);
                }

                long customerId = dbHelper.insertOrGetCustomer(name, phone, village);
                if (customerId == -1) {
                    mainHandler.post(() -> Toast.makeText(getContext(), "Error saving customer. Bill aborted.", Toast.LENGTH_LONG).show());
                    return;
                }

                // Update photo if captured
                if (kycPath != null) {
                    dbHelper.updateCustomer(customerId, name, village, dbHelper.getCustomerDebt(customerId), kycPath);
                }

                double netDebtChange = billedAmount - paidAmount;
                boolean debtWasUpdated = false;
                if (Math.abs(netDebtChange) > 0.001) {
                    int updatedRows = dbHelper.updateCustomerDebt(customerId, netDebtChange);
                    debtWasUpdated = updatedRows > 0;
                }

                List<ReturnItem> finalReturnItems = isReturnApplied ? returnsToSave : new ArrayList<>();
                double billDebtAmount = Math.max(0.0, billedAmount - paidAmount);
                String billDebtDueDate = billDebtAmount > 0.001 ? debtDueDateIso : null;
                Integer explicitId = (editingBillId != -1) ? editingBillId : null;

                long billId = dbHelper.insertBill(explicitId, customerId, 0.0, 0.0, finalTotalAmount, gstPercent, paymentMode, paymentDetails, itemsToSave, finalReturnItems, billDebtDueDate, billDebtAmount, billedAmount, paidAmount, pdcDate);

                if (billId != -1) {
                    for (SelectedItem item : itemsToSave) {
                        dbHelper.updateItemSoldStatus(item.getId(), true);
                    }

                    if (debtWasUpdated && Math.abs(netDebtChange) > 0.001) {
                        double resultingBalance = dbHelper.getCustomerDebt(customerId);
                        String note = netDebtChange > 0 ? "Debt added from bill" : "Credit adjustment";
                        dbHelper.insertDebtUpdate(customerId, billId, netDebtChange, resultingBalance, billedAmount, paidAmount, billDebtDueDate, note);
                    }

                    mainHandler.post(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Bill #" + billId + " Generated!", Toast.LENGTH_SHORT).show();
                            autoShareBillViaWhatsApp((int) billId, phone);
                            clearForm();
                        }
                    });
                } else {
                    mainHandler.post(() -> Toast.makeText(getContext(), "Failed to save bill details.", Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e(TAG, "Critical error saving bill", e);
                mainHandler.post(() -> Toast.makeText(getContext(), "Database Error while saving.", Toast.LENGTH_LONG).show());
            }
        });
    }

    private void autoShareBillViaWhatsApp(int billId, String phoneNumber) {
        try {
            File pdfFile = PdfUtils.generateBillPdf(requireContext(), billId);
            android.content.SharedPreferences shopPrefs = requireContext().getSharedPreferences("shop_profile_prefs", android.content.Context.MODE_PRIVATE);
            String message = shopPrefs.getString("whatsapp_note", "Thank you for shopping with us! Your bill is attached.");
            
            Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "com.example.billgenerator.provider", pdfFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String digits = phoneNumber.replaceAll("[^0-9]", "");
            String normalizedPhone = digits;
            if (digits.length() == 10) normalizedPhone = "91" + digits;
            else if (digits.length() == 11 && digits.startsWith("0")) normalizedPhone = "91" + digits.substring(1);

            if (!normalizedPhone.isEmpty()) {
                shareIntent.setPackage("com.whatsapp");
                shareIntent.putExtra("jid", normalizedPhone + "@s.whatsapp.net");
            }

            startActivity(Intent.createChooser(shareIntent, "Send Bill via WhatsApp"));
        } catch (Exception e) {
            Log.e(TAG, "Error in auto-sharing bill", e);
            Toast.makeText(getContext(), "Bill generated but auto-share failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmClearForm() {
        if (selectedItemsList.isEmpty() && nameEditText.getText().toString().isEmpty()) {
            clearForm();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Clear All Details?")
                .setMessage("This will remove all items and customer details. Are you sure?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    // IMPORTANT: We need to return items to stock if they were marked sold
                    for (SelectedItem item : selectedItemsList) {
                        dbHelper.updateItemSoldStatus(item.getId(), false);
                    }
                    clearForm();
                    Toast.makeText(getContext(), "Form cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearForm() {
        Log.d(TAG, "Clearing the form.");
        
        // IMPORTANT: If we were editing a bill or a draft was loaded, 
        // we should NOT mark items as unsold here automatically,
        // UNLESS the user explicitly cancelled.
        // For simple clear, if items were marked sold in DB, they stay sold.

        editingBillId = -1;
        if (generateBillButton != null) generateBillButton.setText("Review & Finalize Bill");

        if(nameEditText != null) nameEditText.setText("");
        if(phoneEditText != null) phoneEditText.setText("");
        if(villageEditText != null) villageEditText.setText("");
        if(manualTotalEditText != null) manualTotalEditText.setText("");
        if(gstSwitch != null) gstSwitch.setChecked(false);
        if(gstPercentageEditText != null) gstPercentageEditText.setText("");
        if(paymentModeRadioGroup != null) paymentModeRadioGroup.check(R.id.cash_radio_button);
        if(paymentDetailsEditText != null) paymentDetailsEditText.setText("");
        if(returnItemSwitch != null) returnItemSwitch.setChecked(false);
        currentKycPhotoPath = null;
        if (kycImageView != null) {
            kycImageView.setImageResource(android.R.drawable.ic_menu_camera);
            kycImageView.setPadding(16, 16, 16, 16);
        }
        selectedItemsList.clear();
        returnItemsList.clear();
        if(selectedItemAdapter != null) selectedItemAdapter.notifyDataSetChanged();
        if(returnItemAdapter != null) returnItemAdapter.notifyDataSetChanged();
        updateSelectedItemsEmptyState();

        View currentFocus = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (currentFocus != null) {
            currentFocus.clearFocus();
        }

        if (customerLookupHint != null) {
            customerLookupHint.setVisibility(View.GONE);
            customerLookupHint.setText("");
        }
        loadRecentCustomerChips();
    }

    private static class CustomerSuggestion {
        final int id;
        final String name;
        final String phone;
        final String village;
        final double debt;

        CustomerSuggestion(int id, String name, String phone, String village, double debt) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.village = village;
            this.debt = debt;
        }
    }

    private void startBarcodeScanning() {
        if (scanner == null) {
            Log.e(TAG, "Barcode scanner not initialized");
            return;
        }

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null) {
                        processScannedBarcode(rawValue);
                    }
                })
                .addOnCanceledListener(() -> Log.d(TAG, "Barcode scanning canceled"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Barcode scanning failed", e);
                    Toast.makeText(getContext(), "Scanning failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processScannedBarcode(String barcode) {
        if (barcode == null) return;
        String cleanBarcode = barcode.trim();
        if (cleanBarcode.isEmpty()) return;

        Log.d(TAG, "Processing barcode: " + cleanBarcode);
        Item item = dbHelper.findItemByBarcode(cleanBarcode);
        
        if (item != null) {
            // Check if item already in the list
            boolean alreadyAdded = false;
            for (SelectedItem si : selectedItemsList) {
                if (si.getId() == item.getId()) {
                    alreadyAdded = true;
                    break;
                }
            }

            if (alreadyAdded) {
                Toast.makeText(getContext(), "Item already added to bill", Toast.LENGTH_SHORT).show();
            } else {
                selectedItemsList.add(new SelectedItem(item.getId(), item.getName(), item.getWeight(), item.getType()));
                if (selectedItemAdapter != null) {
                    selectedItemAdapter.notifyItemInserted(selectedItemsList.size() - 1);
                    selectedItemsRecyclerView.smoothScrollToPosition(selectedItemsList.size() - 1);
                }
                updateSelectedItemsEmptyState();
                Toast.makeText(getContext(), "Added: " + item.getName(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "No unsold item found in database for barcode: " + cleanBarcode);
            Toast.makeText(getContext(), "Item not found in stock. Make sure it's not already sold. Scanned: " + cleanBarcode, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        if (reusableAddItemDialog != null && reusableAddItemDialog.isShowing()) {
            reusableAddItemDialog.dismiss();
        }
        reusableAddItemDialog = null;
        addItemAdapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (getActivity() != null) getActivity().invalidateOptionsMenu();
    }



    public void loadBillForEditing(int billId) {
        if (nameEditText == null) {
            Log.d(TAG, "Views not ready, queuing bill load: " + billId);
            this.pendingEditBillId = billId;
            return;
        }

        this.editingBillId = billId;
        Log.d(TAG, "Loading bill for editing: " + billId);
        
        clearForm(); // Reset everything first
        this.editingBillId = billId; // Set it back after clear

        Cursor billCursor = dbHelper.getBillDetails(billId);
        if (billCursor != null && billCursor.moveToFirst()) {
            try {
                String name = billCursor.getString(billCursor.getColumnIndexOrThrow("name"));
                String phone = billCursor.getString(billCursor.getColumnIndexOrThrow("phone"));
                String village = billCursor.getString(billCursor.getColumnIndexOrThrow("village"));
                double total = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                double gst = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));
                String payMode = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_mode"));
                String payDetails = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_details"));

                nameEditText.setText(name, false);
                phoneEditText.setText(phone, false);
                villageEditText.setText(village);
                manualTotalEditText.setText(String.format(Locale.US, "%.0f", total));
                
                if (gst > 0) {
                    gstSwitch.setChecked(true);
                    gstPercentageEditText.setText(String.format(Locale.US, "%.2f", gst));
                }

                if ("Online / UPI".equalsIgnoreCase(payMode)) {
                    paymentModeRadioGroup.check(R.id.online_radio_button);
                    paymentDetailsEditText.setText(payDetails);
                    paymentDetailsEditText.setVisibility(View.VISIBLE);
                } else {
                    paymentModeRadioGroup.check(R.id.cash_radio_button);
                }

                // Load items
                Cursor itemCursor = dbHelper.getItemsForBill(billId);
                if (itemCursor != null) {
                    selectedItemsList.clear();
                    while (itemCursor.moveToNext()) {
                        int itemId = itemCursor.getInt(itemCursor.getColumnIndexOrThrow("id"));
                        String itemName = itemCursor.getString(itemCursor.getColumnIndexOrThrow("name"));
                        double itemWeight = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow("weight"));
                        String itemType = itemCursor.getString(itemCursor.getColumnIndexOrThrow("type"));
                        selectedItemsList.add(new SelectedItem(itemId, itemName, itemWeight, itemType));
                    }
                    itemCursor.close();
                    selectedItemAdapter.notifyDataSetChanged();
                    updateSelectedItemsEmptyState();
                }

                // Load return items
                Cursor returnCursor = dbHelper.getReturnItemsForBill(billId);
                if (returnCursor != null) {
                    returnItemsList.clear();
                    while (returnCursor.moveToNext()) {
                        String rType = returnCursor.getString(returnCursor.getColumnIndexOrThrow("return_type"));
                        double rWeight = returnCursor.getDouble(returnCursor.getColumnIndexOrThrow("return_weight"));
                        double rDeduct = returnCursor.getDouble(returnCursor.getColumnIndexOrThrow("return_deduct_amount"));
                        returnItemsList.add(new ReturnItem(rType, rWeight, rDeduct));
                    }
                    returnCursor.close();
                    if (!returnItemsList.isEmpty()) {
                        returnItemSwitch.setChecked(true);
                        returnItemDetailsLayout.setVisibility(View.VISIBLE);
                        returnItemAdapter.notifyDataSetChanged();
                    }
                }

                generateBillButton.setText("Update & Re-generate Bill #" + billId);
                if (getActivity() != null) getActivity().invalidateOptionsMenu();
                Toast.makeText(getContext(), "Editing Bill #" + billId, Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Error loading bill for edit", e);
            } finally {
                billCursor.close();
            }
        }
    }
}
