package com.example.billgenerator.fragments;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StatsFragment extends Fragment {

    private enum ReportMode {
        DAILY,
        MONTHLY,
        YEARLY
    }

    private databaseSystem dbHelper;
    private TextView totalBillsText;
    private TextView totalRevenueText;
    private TextView totalCustomersText;
    private TextView totalStockText;
    private TextView selectedPeriodText;
    private TextView chartTitleText;
    private TextView avgBillValueText;
    private TextView collectionRateText;
    private TextView pendingAmountText;
    private TextView topCustomerNameText;
    private TextView topCustomerValueText;
    private TextView metalMixText;
    private BarChart monthlyRevenueChart;
    private MaterialButtonToggleGroup periodToggle;
    private MaterialButton pickPeriodButton;
    private MaterialButton exportButton;
    private MaterialButton manageExpensesButton;

    private ReportMode currentMode = ReportMode.MONTHLY;
    private final Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new databaseSystem(requireContext());

        totalBillsText = view.findViewById(R.id.stats_total_bills);
        totalRevenueText = view.findViewById(R.id.stats_total_revenue);
        totalCustomersText = view.findViewById(R.id.stats_total_customers);
        totalStockText = view.findViewById(R.id.stats_total_stock);
        selectedPeriodText = view.findViewById(R.id.stats_selected_period_label);
        chartTitleText = view.findViewById(R.id.stats_chart_title);
        avgBillValueText = view.findViewById(R.id.stats_avg_bill_value);
        collectionRateText = view.findViewById(R.id.stats_collection_rate);
        pendingAmountText = view.findViewById(R.id.stats_pending_amount);
        topCustomerNameText = view.findViewById(R.id.stats_top_customer_name);
        topCustomerValueText = view.findViewById(R.id.stats_top_customer_value);
        metalMixText = view.findViewById(R.id.stats_metal_mix);
        monthlyRevenueChart = view.findViewById(R.id.stats_monthly_chart);
        periodToggle = view.findViewById(R.id.stats_period_toggle);
        pickPeriodButton = view.findViewById(R.id.stats_pick_period_button);
        exportButton = view.findViewById(R.id.stats_export_button);
        manageExpensesButton = view.findViewById(R.id.stats_manage_expenses_button);

        setupControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void setupControls() {
        periodToggle.check(R.id.stats_monthly_button);
        periodToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }

            if (checkedId == R.id.stats_daily_button) {
                currentMode = ReportMode.DAILY;
            } else if (checkedId == R.id.stats_yearly_button) {
                currentMode = ReportMode.YEARLY;
            } else {
                currentMode = ReportMode.MONTHLY;
            }
            loadStats();
        });

        pickPeriodButton.setOnClickListener(v -> showPeriodPicker());
        exportButton.setOnClickListener(v -> exportCurrentReportToCsv());
        if (manageExpensesButton != null) {
            manageExpensesButton.setOnClickListener(v -> showExpenseManager());
        }
    }

    private void showExpenseManager() {
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_expense_list);
        
        RecyclerView rv = dialog.findViewById(R.id.expense_recycler);
        MaterialButton addBtn = dialog.findViewById(R.id.btn_add_expense);
        
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadExpensesIntoList(rv);
        
        addBtn.setOnClickListener(v -> showAddExpenseDialog(() -> loadExpensesIntoList(rv)));
        
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.show();
    }

    private void loadExpensesIntoList(RecyclerView rv) {
        ArrayList<String> expenses = new ArrayList<>();
        Cursor c = dbHelper.fetchExpenses();
        if (c != null) {
            while (c.moveToNext()) {
                expenses.add(c.getString(1) + ": ₹" + c.getDouble(2) + " (" + c.getString(4) + ")");
            }
            c.close();
        }
        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                TextView tv = new TextView(p.getContext());
                tv.setPadding(16, 16, 16, 16);
                return new RecyclerView.ViewHolder(tv) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ((TextView)h.itemView).setText(expenses.get(pos));
            }
            @Override public int getItemCount() { return expenses.size(); }
        });
    }

    private void showAddExpenseDialog(Runnable onSaved) {
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_add_expense);
        
        com.google.android.material.textfield.TextInputEditText titleIn = dialog.findViewById(R.id.expense_title_input);
        com.google.android.material.textfield.TextInputEditText amtIn = dialog.findViewById(R.id.expense_amount_input);
        com.google.android.material.textfield.TextInputEditText catIn = dialog.findViewById(R.id.expense_category_input);
        MaterialButton saveBtn = dialog.findViewById(R.id.btn_save_expense);
        
        saveBtn.setOnClickListener(v -> {
            String title = titleIn.getText().toString();
            String amtStr = amtIn.getText().toString();
            if (title.isEmpty() || amtStr.isEmpty()) return;
            
            dbHelper.insertExpense(title, Double.parseDouble(amtStr), catIn.getText().toString(), 
                new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date()));
            dialog.dismiss();
            onSaved.run();
        });
        dialog.show();
    }

    private void loadStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String whereClause = getWhereClauseForMode();
        String[] whereArgs = getWhereArgsForMode();

        totalBillsText.setText(querySingleInt(db, "SELECT COUNT(*) FROM bills WHERE " + whereClause, whereArgs));
        totalCustomersText.setText(querySingleInt(db, "SELECT COUNT(DISTINCT customer_id) FROM bills WHERE " + whereClause, whereArgs));
        totalStockText.setText(querySingleInt(db, "SELECT COUNT(*) FROM items WHERE is_sold = 0", null));
        totalRevenueText.setText("Rs " + querySingleDouble(db, "SELECT IFNULL(SUM(total_amount), 0) FROM bills WHERE " + whereClause, whereArgs));

        selectedPeriodText.setText("Selected: " + getSelectedPeriodLabel());
        pickPeriodButton.setText(getPickerButtonLabel());
        chartTitleText.setText(getChartTitle());
        loadAdvancedInsights(db, whereClause, whereArgs);
        setupTrendChart(db);
    }

    private void loadAdvancedInsights(SQLiteDatabase db, String whereClause, String[] whereArgs) {
        String avgBill = querySingleDouble(db, "SELECT IFNULL(AVG(total_amount), 0) FROM bills WHERE " + whereClause, whereArgs);
        avgBillValueText.setText("Avg bill value: Rs " + avgBill);

        String pending = querySingleDouble(db, "SELECT IFNULL(SUM(debt_amount), 0) FROM bills WHERE " + whereClause, whereArgs);
        pendingAmountText.setText("Pending in period: Rs " + pending);

        Cursor collectionCursor = null;
        try {
            collectionCursor = db.rawQuery(
                    "SELECT IFNULL(SUM(paid_amount), 0), IFNULL(SUM(billed_amount), 0) FROM bills WHERE " + whereClause,
                    whereArgs
            );
            if (collectionCursor.moveToFirst()) {
                double paid = collectionCursor.getDouble(0);
                double billed = collectionCursor.getDouble(1);
                double collectionRate = billed <= 0.0 ? 0.0 : (paid * 100.0 / billed);
                collectionRateText.setText(String.format(Locale.getDefault(), "Collection efficiency: %.1f%%", collectionRate));
            } else {
                collectionRateText.setText("Collection efficiency: 0.0%");
            }
        } finally {
            if (collectionCursor != null) {
                collectionCursor.close();
            }
        }

        Cursor topCustomerCursor = null;
        try {
            String billWhereClause = getWhereClauseForMode("b.");
            topCustomerCursor = db.rawQuery(
                    "SELECT c.name, IFNULL(SUM(b.total_amount), 0) AS spend " +
                            "FROM bills b JOIN customer c ON b.customer_id = c.id " +
                            "WHERE " + billWhereClause + " " +
                            "GROUP BY b.customer_id " +
                            "ORDER BY spend DESC LIMIT 1",
                    whereArgs
            );
            if (topCustomerCursor.moveToFirst()) {
                String name = topCustomerCursor.getString(0);
                double spend = topCustomerCursor.getDouble(1);
                topCustomerNameText.setText("Top customer: " + (name == null || name.trim().isEmpty() ? "-" : name));
                topCustomerValueText.setText(String.format(Locale.getDefault(), "Top customer spend: Rs %.2f", spend));
            } else {
                topCustomerNameText.setText("Top customer: -");
                topCustomerValueText.setText("Top customer spend: Rs 0.00");
            }
        } finally {
            if (topCustomerCursor != null) {
                topCustomerCursor.close();
            }
        }

        Cursor mixCursor = null;
        int goldCount = 0;
        int silverCount = 0;
        int otherCount = 0;
        try {
            String billWhereClause = getWhereClauseForMode("b.");
            mixCursor = db.rawQuery(
                    "SELECT LOWER(IFNULL(i.type, 'other')) AS metal, COUNT(*) " +
                            "FROM bill_items bi " +
                            "JOIN items i ON bi.item_id = i.id " +
                            "JOIN bills b ON bi.bill_id = b.id " +
                            "WHERE " + billWhereClause + " " +
                            "GROUP BY LOWER(IFNULL(i.type, 'other'))",
                    whereArgs
            );
            while (mixCursor.moveToNext()) {
                String metal = mixCursor.getString(0);
                int count = mixCursor.getInt(1);
                if (metal != null && metal.contains("gold")) {
                    goldCount += count;
                } else if (metal != null && metal.contains("silver")) {
                    silverCount += count;
                } else {
                    otherCount += count;
                }
            }
        } finally {
            if (mixCursor != null) {
                mixCursor.close();
            }
        }
        metalMixText.setText(String.format(Locale.getDefault(), "Items sold mix: Gold %d | Silver %d | Other %d", goldCount, silverCount, otherCount));
    }

    private String querySingleInt(SQLiteDatabase db, String query, String[] args) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args);
            if (cursor.moveToFirst()) {
                return String.valueOf(cursor.getInt(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "0";
    }

    private String querySingleDouble(SQLiteDatabase db, String query, String[] args) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, args);
            if (cursor.moveToFirst()) {
                return String.format(Locale.getDefault(), "%.2f", cursor.getDouble(0));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "0.00";
    }

    private void setupTrendChart(SQLiteDatabase db) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        buildChartData(db, labels, entries);

        BarDataSet dataSet = new BarDataSet(entries, getChartDataSetLabel());
        dataSet.setColor(0xFF2B7FFF);
        dataSet.setValueTextSize(11f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        monthlyRevenueChart.setData(data);
        monthlyRevenueChart.getDescription().setEnabled(false);
        monthlyRevenueChart.getAxisRight().setEnabled(false);
        monthlyRevenueChart.getLegend().setEnabled(false);
        monthlyRevenueChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        monthlyRevenueChart.getXAxis().setGranularity(1f);
        monthlyRevenueChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        monthlyRevenueChart.getXAxis().setLabelRotationAngle(-25f);
        monthlyRevenueChart.setFitBars(true);
        monthlyRevenueChart.invalidate();
    }

    private void buildChartData(SQLiteDatabase db, ArrayList<String> labels, ArrayList<BarEntry> entries) {
        Cursor cursor = null;
        try {
            String query;
            if (currentMode == ReportMode.DAILY) {
                query = "SELECT strftime('%Y-%m-%d', bill_date) AS day_key, IFNULL(SUM(total_amount), 0) AS day_total " +
                        "FROM bills WHERE date(bill_date) BETWEEN date(?, '-6 day') AND date(?) " +
                        "GROUP BY day_key ORDER BY day_key ASC";
                String day = formatDate("yyyy-MM-dd");
                cursor = db.rawQuery(query, new String[]{day, day});
            } else if (currentMode == ReportMode.YEARLY) {
                query = "SELECT strftime('%Y', bill_date) AS year_key, IFNULL(SUM(total_amount), 0) AS year_total " +
                        "FROM bills WHERE CAST(strftime('%Y', bill_date) AS INTEGER) BETWEEN ? AND ? " +
                        "GROUP BY year_key ORDER BY year_key ASC";
                int selectedYear = selectedDate.get(Calendar.YEAR);
                int fromYear = selectedYear - 4;
                cursor = db.rawQuery(query, new String[]{String.valueOf(fromYear), String.valueOf(selectedYear)});
            } else {
                query = "SELECT strftime('%Y-%m', bill_date) AS month_key, IFNULL(SUM(total_amount), 0) AS month_total " +
                        "FROM bills WHERE strftime('%Y-%m', bill_date) <= ? " +
                        "GROUP BY month_key ORDER BY month_key DESC LIMIT 6";
                String selectedMonth = formatDate("yyyy-MM");
                cursor = db.rawQuery(query, new String[]{selectedMonth});
            }

            ArrayList<String> keys = new ArrayList<>();
            ArrayList<Double> values = new ArrayList<>();
            while (cursor.moveToNext()) {
                keys.add(cursor.getString(0));
                values.add(cursor.getDouble(1));
            }

            if (currentMode == ReportMode.MONTHLY) {
                for (int i = keys.size() - 1; i >= 0; i--) {
                    int index = keys.size() - 1 - i;
                    labels.add(keys.get(i));
                    entries.add(new BarEntry(index, values.get(i).floatValue()));
                }
            } else {
                for (int i = 0; i < keys.size(); i++) {
                    labels.add(keys.get(i));
                    entries.add(new BarEntry(i, values.get(i).floatValue()));
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void showPeriodPicker() {
        Calendar now = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            selectedDate.set(Calendar.YEAR, year);
            selectedDate.set(Calendar.MONTH, month);
            selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            loadStats();
        },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(now.getTimeInMillis());
        dialog.show();
    }

    private String getWhereClauseForMode() {
        return getWhereClauseForMode("");
    }

    private String getWhereClauseForMode(String alias) {
        String prefix = alias == null ? "" : alias;
        if (currentMode == ReportMode.DAILY) {
            return "date(" + prefix + "bill_date) = date(?)";
        }
        if (currentMode == ReportMode.YEARLY) {
            return "strftime('%Y', " + prefix + "bill_date) = ?";
        }
        return "strftime('%Y-%m', " + prefix + "bill_date) = ?";
    }

    private String[] getWhereArgsForMode() {
        if (currentMode == ReportMode.DAILY) {
            return new String[]{formatDate("yyyy-MM-dd")};
        }
        if (currentMode == ReportMode.YEARLY) {
            return new String[]{formatDate("yyyy")};
        }
        return new String[]{formatDate("yyyy-MM")};
    }

    private String formatDate(String pattern) {
        return new java.text.SimpleDateFormat(pattern, Locale.getDefault()).format(selectedDate.getTime());
    }

    private String getSelectedPeriodLabel() {
        if (currentMode == ReportMode.DAILY) {
            return formatDate("dd MMM yyyy");
        }
        if (currentMode == ReportMode.YEARLY) {
            return formatDate("yyyy");
        }
        return formatDate("MMM yyyy");
    }

    private String getPickerButtonLabel() {
        if (currentMode == ReportMode.DAILY) {
            return "Pick day";
        }
        if (currentMode == ReportMode.YEARLY) {
            return "Pick year";
        }
        return "Pick month";
    }

    private String getChartTitle() {
        if (currentMode == ReportMode.DAILY) {
            return "Revenue trend (last 7 days)";
        }
        if (currentMode == ReportMode.YEARLY) {
            return "Revenue trend (last 5 years)";
        }
        return "Revenue trend (last 6 months)";
    }

    private String getChartDataSetLabel() {
        if (currentMode == ReportMode.DAILY) {
            return "Daily Revenue";
        }
        if (currentMode == ReportMode.YEARLY) {
            return "Yearly Revenue";
        }
        return "Monthly Revenue";
    }

    private void exportCurrentReportToCsv() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(getContext(), "Export needs Android 10 or above", Toast.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String whereClause = getWhereClauseForMode();
        String[] whereArgs = getWhereArgsForMode();
        Cursor cursor = null;

        try {
                String query = "SELECT b.id, b.bill_date, c.name, c.phone, b.total_amount, b.billed_amount, b.paid_amount, b.debt_amount, b.payment_mode " +
                    "FROM bills b JOIN customer c ON b.customer_id = c.id " +
                    "WHERE " + getWhereClauseForMode("b.") + " ORDER BY b.bill_date DESC";
            cursor = db.rawQuery(query, whereArgs);

            ContentValues values = new ContentValues();
            String fileName = "stats_" + currentMode.name().toLowerCase(Locale.getDefault()) + "_" + formatDate("yyyyMMdd") + ".csv";
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = requireActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(getContext(), "Failed to create export file", Toast.LENGTH_LONG).show();
                return;
            }

            try (OutputStream outputStream = requireActivity().getContentResolver().openOutputStream(uri)) {
                if (outputStream == null) {
                    Toast.makeText(getContext(), "Failed to open export file", Toast.LENGTH_LONG).show();
                    return;
                }

                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("Mode,").append(currentMode.name()).append("\n");
                csvBuilder.append("Period,").append(getSelectedPeriodLabel()).append("\n\n");
                csvBuilder.append("Bill ID,Bill Date,Customer Name,Phone,Total Amount,Billed Amount,Paid Amount,Debt Amount,Payment Mode\n");

                while (cursor.moveToNext()) {
                    csvBuilder.append(cursor.getInt(0)).append(',')
                            .append(escapeCsv(cursor.getString(1))).append(',')
                            .append(escapeCsv(cursor.getString(2))).append(',')
                            .append(escapeCsv(cursor.getString(3))).append(',')
                            .append(String.format(Locale.getDefault(), "%.2f", cursor.getDouble(4))).append(',')
                            .append(String.format(Locale.getDefault(), "%.2f", cursor.getDouble(5))).append(',')
                            .append(String.format(Locale.getDefault(), "%.2f", cursor.getDouble(6))).append(',')
                            .append(String.format(Locale.getDefault(), "%.2f", cursor.getDouble(7))).append(',')
                            .append(escapeCsv(cursor.getString(8)))
                            .append("\n");
                }

                outputStream.write(csvBuilder.toString().getBytes());
                outputStream.flush();
                Toast.makeText(getContext(), "Report exported to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
