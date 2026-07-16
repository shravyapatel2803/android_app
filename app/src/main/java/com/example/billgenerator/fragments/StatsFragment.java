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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.databinding.FragmentStatsBinding;
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

    private FragmentStatsBinding binding;
    private databaseSystem dbHelper;

    private ReportMode currentMode = ReportMode.MONTHLY;
    private final Calendar selectedDate = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
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

        setupControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void setupControls() {
        binding.statsPeriodToggle.check(R.id.stats_monthly_button);
        binding.statsPeriodToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
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

        binding.statsPickPeriodButton.setOnClickListener(v -> showPeriodPicker());
        binding.statsExportButton.setOnClickListener(v -> exportCurrentReportToCsv());
        if (binding.statsManageExpensesButton != null) {
            binding.statsManageExpensesButton.setOnClickListener(v -> showExpenseManager());
        }
    }

    private void showExpenseManager() {
        // Implementation of expense manager
    }

    private void loadStats() {
        if (binding == null) return;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String where = getWhereClauseForMode();
        String[] args = getWhereArgsForMode();

        binding.statsTotalBills.setText(querySingleInt(db, "SELECT COUNT(*) FROM bills WHERE " + where, args));
        binding.statsTotalRevenue.setText("₹ " + querySingleDouble(db, "SELECT SUM(total_amount) FROM bills WHERE " + where, args));
        binding.statsTotalCustomers.setText(querySingleInt(db, "SELECT COUNT(DISTINCT customer_id) FROM bills WHERE " + where, args));
        binding.statsTotalStock.setText(querySingleInt(db, "SELECT COUNT(*) FROM items WHERE is_sold = 0", null));
        
        binding.statsSelectedPeriodLabel.setText("Selected: " + getSelectedPeriodLabel());
        binding.statsPickPeriodButton.setText(getPickerButtonLabel());
        binding.statsChartTitle.setText(getChartTitle());

        loadAdvancedInsights(db, where, args);
        setupTrendChart(db);
    }

    private void loadAdvancedInsights(SQLiteDatabase db, String where, String[] args) {
        // Detailed metrics
    }

    private String querySingleInt(SQLiteDatabase db, String query, String[] args) {
        Cursor cursor = db.rawQuery(query, args);
        String result = "0";
        if (cursor.moveToFirst()) {
            result = String.valueOf(cursor.getInt(0));
        }
        cursor.close();
        return result;
    }

    private String querySingleDouble(SQLiteDatabase db, String query, String[] args) {
        Cursor cursor = db.rawQuery(query, args);
        String result = "0.00";
        if (cursor.moveToFirst()) {
            result = String.format(Locale.getDefault(), "%.2f", cursor.getDouble(0));
        }
        cursor.close();
        return result;
    }

    private void setupTrendChart(SQLiteDatabase db) {
        // Chart setup logic
    }

    private void showPeriodPicker() {
        // Period picker logic
    }

    private String getWhereClauseForMode() {
        switch (currentMode) {
            case DAILY: return "date(bill_date) = date(?)";
            case YEARLY: return "strftime('%Y', bill_date) = ?";
            case MONTHLY:
            default: return "strftime('%Y-%m', bill_date) = ?";
        }
    }

    private String[] getWhereArgsForMode() {
        // Args logic based on currentMode and selectedDate
        return new String[]{"2023-10"}; // Placeholder
    }

    private String getSelectedPeriodLabel() {
        return "October 2023"; // Placeholder
    }

    private String getPickerButtonLabel() {
        return "Change Month"; // Placeholder
    }

    private String getChartTitle() {
        return "Monthly Revenue Trend";
    }

    private void exportCurrentReportToCsv() {
        // CSV export logic
    }
}
