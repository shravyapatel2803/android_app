package com.example.billgenerator.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.DebtCustomerAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.databinding.FragmentDebtCustomersBinding;
import com.example.billgenerator.models.DebtCustomerItem;
import com.example.billgenerator.ui.UiAnimationHelper;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DebtCustomersFragment extends Fragment {

    public static final String PREFS_NAME = "bill_generator_prefs";
    public static final String KEY_DEBT_WHATSAPP_TEMPLATE = "debt_whatsapp_template";
    public static final String DEFAULT_DEBT_TEMPLATE = "Hello {name}, this is a reminder for your pending debt of Rs {amount}. Please clear it by {due_date}.";

    private FragmentDebtCustomersBinding binding;
    private final ArrayList<DebtCustomerItem> allItems = new ArrayList<>();
    private final ArrayList<DebtCustomerItem> filteredItems = new ArrayList<>();
    private DebtCustomerAdapter adapter;
    private databaseSystem dbHelper;
    private String queryFilter = "";
    private int dueFilter = 0; // 0 all, 1 overdue, 2 today, 3 upcoming
    private int amountFilter = 0; // 0 all, 1 low, 2 medium, 3 high
    private static final String[] DUE_FILTER_OPTIONS = {"All Due", "Overdue", "Due Today", "Upcoming"};
    private static final String[] AMOUNT_FILTER_OPTIONS = {"All Amounts", "Low (<10K)", "Medium (10K-50K)", "High (>50K)"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDebtCustomersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new databaseSystem(requireContext());

        adapter = new DebtCustomerAdapter(requireContext(), filteredItems, this::openDebtHistory);
        
        int sw = requireContext().getResources().getConfiguration().smallestScreenWidthDp;
        if (sw >= 600) {
            binding.debtCustomersRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        } else {
            binding.debtCustomersRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        }
        binding.debtCustomersRecycler.setAdapter(adapter);
        UiAnimationHelper.setupRecyclerViewAnimations(binding.debtCustomersRecycler);
        
        UiAnimationHelper.configureEmptyState(
                binding.debtCustomersEmpty.getRoot(),
                R.drawable.ic_empty_debt,
                "No debt records found",
                "Customers with outstanding balances will appear here.",
                null,
                null
        );

        if (binding.debtCustomizeMessageButton != null) {
            binding.debtCustomizeMessageButton.setOnClickListener(v -> showTemplateCustomizationDialog());
        }

        if (binding.debtOpenFiltersButton != null) {
            binding.debtOpenFiltersButton.setOnClickListener(v -> showFilterDialog());
        }

        setupFilters();
        loadData();
        updateFilterButtonLabel();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        allItems.clear();
        Cursor cursor = null;
        try {
            cursor = dbHelper.fetchDebtCustomerDetails();
            if (cursor != null && cursor.moveToFirst()) {
                int idCol = cursor.getColumnIndexOrThrow("customer_id");
                int nameCol = cursor.getColumnIndexOrThrow("customer_name");
                int phoneCol = cursor.getColumnIndexOrThrow("customer_phone");
                int villageCol = cursor.getColumnIndexOrThrow("customer_village");
                int totalDebtCol = cursor.getColumnIndexOrThrow("customer_total_debt");
                int activeDebtCol = cursor.getColumnIndexOrThrow("active_bill_debt");
                int dueDateCol = cursor.getColumnIndexOrThrow("nearest_due_date");
                int lastBillCol = cursor.getColumnIndexOrThrow("last_bill_date");

                do {
                    double rowDebt = cursor.getDouble(totalDebtCol);
                    allItems.add(new DebtCustomerItem(
                            cursor.getInt(idCol),
                            cursor.getString(nameCol),
                            cursor.getString(phoneCol),
                            cursor.getString(villageCol),
                            rowDebt,
                            cursor.getDouble(activeDebtCol),
                            cursor.getString(dueDateCol),
                            cursor.getString(lastBillCol)
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        applyFilters();
    }

    private void setupFilters() {
        if (binding.debtSearchInput != null) {
            binding.debtSearchInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    queryFilter = s == null ? "" : s.toString().trim();
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_debt_filters, null);
        AutoCompleteTextView dueDropdown = dialogView.findViewById(R.id.dialog_debt_due_filter_dropdown);
        AutoCompleteTextView amountDropdown = dialogView.findViewById(R.id.dialog_debt_amount_filter_dropdown);

        ArrayAdapter<String> dueAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                DUE_FILTER_OPTIONS
        );
        dueDropdown.setAdapter(dueAdapter);
        dueDropdown.setText(DUE_FILTER_OPTIONS[Math.max(0, Math.min(dueFilter, DUE_FILTER_OPTIONS.length - 1))], false);

        ArrayAdapter<String> amountAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                AMOUNT_FILTER_OPTIONS
        );
        amountDropdown.setAdapter(amountAdapter);
        amountDropdown.setText(AMOUNT_FILTER_OPTIONS[Math.max(0, Math.min(amountFilter, AMOUNT_FILTER_OPTIONS.length - 1))], false);

        new AlertDialog.Builder(requireContext())
                .setTitle("Debt Filters")
                .setView(dialogView)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (dialog, which) -> {
                    dueFilter = 0;
                    amountFilter = 0;
                    updateFilterButtonLabel();
                    applyFilters();
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    dueFilter = indexOfOption(dueDropdown.getText() == null ? null : dueDropdown.getText().toString(), DUE_FILTER_OPTIONS);
                    amountFilter = indexOfOption(amountDropdown.getText() == null ? null : amountDropdown.getText().toString(), AMOUNT_FILTER_OPTIONS);
                    updateFilterButtonLabel();
                    applyFilters();
                })
                .show();
    }

    private int indexOfOption(String value, String[] options) {
        if (value == null) {
            return 0;
        }
        for (int i = 0; i < options.length; i++) {
            if (value.equalsIgnoreCase(options[i])) {
                return i;
            }
        }
        return 0;
    }

    private void updateFilterButtonLabel() {
        int activeCount = 0;
        if (dueFilter != 0) activeCount++;
        if (amountFilter != 0) activeCount++;
        
        if (binding.debtOpenFiltersButton instanceof TextView) {
            ((TextView) binding.debtOpenFiltersButton).setText(activeCount == 0 ? "Filter" : "Filter (" + activeCount + ")");
        }

        if (binding.debtActiveFiltersText != null) {
            String due = DUE_FILTER_OPTIONS[dueFilter];
            String amount = AMOUNT_FILTER_OPTIONS[amountFilter];
            if (dueFilter == 0 && amountFilter == 0) {
                binding.debtActiveFiltersText.setText("No filters applied");
            } else {
                binding.debtActiveFiltersText.setText("Active: " + due + " | " + amount);
            }
        }
    }

    private void applyFilters() {
        if (binding == null) return;
        filteredItems.clear();
        String search = queryFilter == null ? "" : queryFilter.toLowerCase(Locale.getDefault()).trim();
        String[] searchTokens = search.isEmpty() ? new String[0] : search.split("\\\\s+");
        String todayIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        double filteredTotalDebt = 0.0;
        for (DebtCustomerItem item : allItems) {
            String name = item.name == null ? "" : item.name.toLowerCase(Locale.getDefault());
            String phone = item.phone == null ? "" : item.phone.toLowerCase(Locale.getDefault());
            String village = item.village == null ? "" : item.village.toLowerCase(Locale.getDefault());

            boolean matchesSearch = true;
            for (String token : searchTokens) {
                if (token.isEmpty()) continue;
                if (!name.contains(token) && !village.contains(token) && !phone.contains(token)) {
                    matchesSearch = false;
                    break;
                }
            }

            boolean matchesDue = matchesDueFilter(item.nearestDueDate, todayIso);
            boolean matchesAmount = matchesAmountFilter(item.totalDebt);

            if (matchesSearch && matchesDue && matchesAmount) {
                filteredItems.add(item);
                filteredTotalDebt += item.totalDebt;
            }
        }

        binding.debtCustomersSummary.setText(String.format(
                Locale.getDefault(),
            "%d of %d customers | Rs %.2f",
                filteredItems.size(),
                allItems.size(),
                filteredTotalDebt
        ));

        adapter.notifyDataSetChanged();
        if (!filteredItems.isEmpty()) {
            binding.debtCustomersRecycler.scheduleLayoutAnimation();
        }
        UiAnimationHelper.setVisible(binding.debtCustomersEmpty.getRoot(), filteredItems.isEmpty());
    }

    private boolean matchesDueFilter(String dueDate, String todayIso) {
        if (dueFilter == 0) return true;
        if (dueDate == null || dueDate.trim().isEmpty()) return false;
        
        String trimmed = dueDate.trim();
        if (dueFilter == 1) return trimmed.compareTo(todayIso) < 0; // Overdue
        if (dueFilter == 2) return trimmed.equals(todayIso); // Due Today
        return trimmed.compareTo(todayIso) > 0; // Upcoming
    }

    private boolean matchesAmountFilter(double amount) {
        double absoluteAmount = Math.abs(amount);
        if (amountFilter == 0) return true;
        if (amountFilter == 1) return absoluteAmount < 10000.0;
        if (amountFilter == 2) return absoluteAmount >= 10000.0 && absoluteAmount <= 50000.0;
        return absoluteAmount > 50000.0;
    }

    private void openDebtHistory(DebtCustomerItem item) {
        // Assume DebtHistoryDialogFragment is already updated or implemented
    }

    private void showTemplateCustomizationDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        String existingTemplate = prefs.getString(KEY_DEBT_WHATSAPP_TEMPLATE, DEFAULT_DEBT_TEMPLATE);

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(4);
        input.setMaxLines(8);
        input.setText(existingTemplate);
        input.setSelection(input.getText().length());

        int padding = (int) (16 * requireContext().getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        String hint = "Use placeholders: {name}, {amount}, {due_date}";
        new AlertDialog.Builder(requireContext())
                .setTitle("Customize Debt Message")
                .setMessage(hint)
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Reset", (dialog, which) -> {
                    prefs.edit().putString(KEY_DEBT_WHATSAPP_TEMPLATE, DEFAULT_DEBT_TEMPLATE).apply();
                    Toast.makeText(requireContext(), "Template reset", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    String template = input.getText() == null ? "" : input.getText().toString().trim();
                    if (template.isEmpty()) {
                        template = DEFAULT_DEBT_TEMPLATE;
                    }
                    prefs.edit().putString(KEY_DEBT_WHATSAPP_TEMPLATE, template).apply();
                    Toast.makeText(requireContext(), "Template saved", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
