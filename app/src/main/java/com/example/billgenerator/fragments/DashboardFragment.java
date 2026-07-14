package com.example.billgenerator.fragments;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.billgenerator.MainActivity;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.ui.UiAnimationHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.card.MaterialCardView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DashboardFragment extends Fragment {

    private TextView tvTotalDebt;
    private TextView tvDueTodayCount;
    private TextView tvDueTodayAmount;
    private TextView tvOverdueCount;
    private TextView tvOverdueAmount;
    private ViewGroup topDebtorsContainer;
    private View dashboardDebtorsEmpty;
    private Button openDebtCustomersButton;
    private Button openNotificationsButton;
    private Button restoreFromPdfsButton;
    private databaseSystem dbHelper;
    private BarChart salesChart;
    private static final String TAG = "DashboardFragment";
    private ActivityResultLauncher<String[]> importPdfsLauncher;
    private final ExecutorService importExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private enum RestoreMode {
        FULL,
        CUSTOMERS_ONLY,
        STOCK_ONLY,
        BILLS_ONLY
    }

    private RestoreMode currentRestoreMode = RestoreMode.FULL;

    private enum ReportType {
        STOCK,
        CUSTOMER,
        DETAILED_BILL,
        SUMMARY_BILL,
        UNKNOWN
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PDFBoxResourceLoader.init(requireContext().getApplicationContext());
        importPdfsLauncher = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(), this::onPdfFilesSelected);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        tvTotalDebt = view.findViewById(R.id.tv_total_debt);
        tvDueTodayCount = view.findViewById(R.id.tv_due_today_count);
        tvDueTodayAmount = view.findViewById(R.id.tv_due_today_amount);
        tvOverdueCount = view.findViewById(R.id.tv_overdue_count);
        tvOverdueAmount = view.findViewById(R.id.tv_overdue_amount);
        topDebtorsContainer = view.findViewById(R.id.dashboard_top_debtors_container);
        dashboardDebtorsEmpty = view.findViewById(R.id.dashboard_debtors_empty);
        openDebtCustomersButton = view.findViewById(R.id.dashboard_open_debt_customers);
        openNotificationsButton = view.findViewById(R.id.dashboard_open_notifications);
        restoreFromPdfsButton = view.findViewById(R.id.restore_from_pdfs_button);
        salesChart = view.findViewById(R.id.dashboard_sales_chart);
        dbHelper = new databaseSystem(requireContext());

        if (openDebtCustomersButton != null) {
            openDebtCustomersButton.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToDestination(7);
                }
            });
        }

        if (openNotificationsButton != null) {
            openNotificationsButton.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToDestination(5);
                }
            });
        }

        if (restoreFromPdfsButton != null) {
            restoreFromPdfsButton.setOnClickListener(v -> showRestoreConfirmation());
        }

        UiAnimationHelper.configureEmptyState(
                dashboardDebtorsEmpty,
                R.drawable.ic_empty_debt,
                "All clear!",
                "No outstanding debts right now. Your customers are up to date.",
                "View Debt Customers",
                () -> {
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).navigateToDestination(7);
                    }
                }
        );

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data every time fragment becomes visible
        loadDashboardData();
    }

    private void loadDashboardData() {
        loadTotalDebt();
        loadDebtDueInsights();
        loadTopDebtors();
        loadSalesChart();
    }

    private void loadSalesChart() {
        if (salesChart == null) return;

        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Get sales data for the last 7 days
        Map<String, Float> salesData = new HashMap<>();
        String query = "SELECT date(bill_date) AS day_key, SUM(total_amount) AS day_total " +
                "FROM bills WHERE date(bill_date) BETWEEN date('now', '-6 day') AND date('now') " +
                "GROUP BY day_key";

        Cursor cursor = null;
        try {
            cursor = db.rawQuery(query, null);
            while (cursor.moveToNext()) {
                salesData.put(cursor.getString(0), cursor.getFloat(1));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading sales chart", e);
        } finally {
            if (cursor != null) cursor.close();
        }

        // Fill all 7 days even if no sales
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat labelFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            String dateKey = sdf.format(cal.getTime());
            String label = labelFormat.format(cal.getTime());
            float amount = salesData.getOrDefault(dateKey, 0f);

            labels.add(label);
            entries.add(new BarEntry(i, amount));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Sales");
        dataSet.setColor(0xFF2B7FFF);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return value > 0 ? String.format(Locale.getDefault(), "₹%.0f", value) : "";
            }
        });

        BarData data = new BarData(dataSet);
        salesChart.setData(data);
        salesChart.getDescription().setEnabled(false);
        salesChart.getAxisRight().setEnabled(false);
        salesChart.getLegend().setEnabled(false);
        
        XAxis xAxis = salesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        
        salesChart.getAxisLeft().setDrawGridLines(true);
        salesChart.getAxisLeft().setGridColor(0x1A000000);
        salesChart.setFitBars(true);
        salesChart.animateY(1000);
        salesChart.invalidate();
    }

    private void loadDebtDueInsights() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor dueTodayCursor = null;
        Cursor overdueCursor = null;

        String dueTodayQuery = "SELECT COUNT(*), IFNULL(SUM(debt_amount), 0) FROM bills WHERE debt_amount > 0 AND date(debt_due_date) = date('now', 'localtime')";
        String overdueQuery = "SELECT COUNT(*), IFNULL(SUM(debt_amount), 0) FROM bills WHERE debt_amount > 0 AND date(debt_due_date) < date('now', 'localtime')";

        try {
            dueTodayCursor = db.rawQuery(dueTodayQuery, null);
            if (dueTodayCursor.moveToFirst()) {
                int count = dueTodayCursor.getInt(0);
                double amount = dueTodayCursor.getDouble(1);
                if (tvDueTodayCount != null) {
                    tvDueTodayCount.setText(String.format(Locale.getDefault(), "%d bill%s", count, count == 1 ? "" : "s"));
                }
                if (tvDueTodayAmount != null) {
                    tvDueTodayAmount.setText(currencyFormat.format(amount));
                }
            }

            overdueCursor = db.rawQuery(overdueQuery, null);
            if (overdueCursor.moveToFirst()) {
                int count = overdueCursor.getInt(0);
                double amount = overdueCursor.getDouble(1);
                if (tvOverdueCount != null) {
                    tvOverdueCount.setText(String.format(Locale.getDefault(), "%d bill%s", count, count == 1 ? "" : "s"));
                }
                if (tvOverdueAmount != null) {
                    tvOverdueAmount.setText(currencyFormat.format(amount));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading due insights", e);
        } finally {
            if (dueTodayCursor != null) {
                dueTodayCursor.close();
            }
            if (overdueCursor != null) {
                overdueCursor.close();
            }
        }
    }

    private void loadTopDebtors() {
        if (topDebtorsContainer == null) {
            return;
        }

        topDebtorsContainer.removeAllViews();
        Cursor cursor = null;
        int shown = 0;

        try {
            cursor = dbHelper.fetchDebtCustomerDetails();
            if (cursor != null && cursor.moveToFirst()) {
                int nameCol = cursor.getColumnIndex("customer_name");
                int villageCol = cursor.getColumnIndex("customer_village");
                int phoneCol = cursor.getColumnIndex("customer_phone");
                int debtCol = cursor.getColumnIndex("customer_total_debt");
                int dueDateCol = cursor.getColumnIndex("nearest_due_date");

                do {
                    if (shown >= 3) {
                        break;
                    }

                    String name = nameCol != -1 ? cursor.getString(nameCol) : "Customer";
                    String village = villageCol != -1 ? cursor.getString(villageCol) : "";
                    String phone = phoneCol != -1 ? cursor.getString(phoneCol) : "";
                    double debt = debtCol != -1 ? cursor.getDouble(debtCol) : 0.0;
                    String dueDate = dueDateCol != -1 ? cursor.getString(dueDateCol) : null;

                    topDebtorsContainer.addView(createDebtorRow(name, village, phone, debt, dueDate));
                    shown++;
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading top debtors", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        UiAnimationHelper.setVisible(dashboardDebtorsEmpty, shown == 0);
    }

    private View createDebtorRow(String name, String village, String phone, double debt, String dueDate) {
        MaterialCardView rowCard = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        rowParams.bottomMargin = dp(6);
        rowCard.setLayoutParams(rowParams);
        rowCard.setCardElevation(0f);
        rowCard.setRadius(dp(12));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView nameView = new TextView(requireContext());
        nameView.setText(name == null || name.trim().isEmpty() ? "Customer" : name.trim());
        nameView.setTextSize(14f);
        nameView.setTypeface(nameView.getTypeface(), Typeface.BOLD);

        TextView detailView = new TextView(requireContext());
        StringBuilder detailBuilder = new StringBuilder();
        if (village != null && !village.trim().isEmpty()) {
            detailBuilder.append(village.trim());
        }
        if (phone != null && !phone.trim().isEmpty()) {
            if (detailBuilder.length() > 0) {
                detailBuilder.append(" • ");
            }
            detailBuilder.append(phone.trim());
        }
        if (dueDate != null && !dueDate.trim().isEmpty()) {
            if (detailBuilder.length() > 0) {
                detailBuilder.append(" • ");
            }
            detailBuilder.append("Due: ").append(dueDate.trim());
        }
        if (detailBuilder.length() == 0) {
            detailBuilder.append("No additional details");
        }
        detailView.setText(detailBuilder.toString());
        detailView.setTextSize(12f);

        TextView debtView = new TextView(requireContext());
        debtView.setText("Debt: " + currencyFormat.format(debt));
        debtView.setTextSize(13f);
        debtView.setTypeface(debtView.getTypeface(), Typeface.BOLD);

        content.addView(nameView);
        content.addView(detailView);
        content.addView(debtView);
        rowCard.addView(content);
        return rowCard;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private void showRestoreConfirmation() {
        String[] options = {"Full Database Restore (Wipe Current)", "Restore Customers Only", "Restore Stock Only", "Restore Bills Only"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Restore Option")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: currentRestoreMode = RestoreMode.FULL; break;
                        case 1: currentRestoreMode = RestoreMode.CUSTOMERS_ONLY; break;
                        case 2: currentRestoreMode = RestoreMode.STOCK_ONLY; break;
                        case 3: currentRestoreMode = RestoreMode.BILLS_ONLY; break;
                    }
                    proceedWithPdfSelection();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void proceedWithPdfSelection() {
        String message = currentRestoreMode == RestoreMode.FULL
                ? "This will replace current database data with records parsed from selected PDF reports. Continue?"
                : "This will add records from the selected PDF to your existing data. Continue?";

        new AlertDialog.Builder(requireContext())
                .setTitle("Restore From PDFs")
                .setMessage(message)
                .setPositiveButton("Continue", (dialog, which) -> {
                    if (importPdfsLauncher != null) {
                        importPdfsLauncher.launch(new String[]{"application/pdf"});
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onPdfFilesSelected(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) {
            Toast.makeText(getContext(), "No PDF selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Import started...", Toast.LENGTH_SHORT).show();
        importExecutor.execute(() -> {
            try {
                ImportBundle importBundle = parseSelectedReports(uris);
                ImportResult result = restoreIntoDatabase(importBundle);
                mainHandler.post(() -> {
                    loadDashboardData();
                    Toast.makeText(getContext(), buildImportResultMessage(result), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Bulk restore failed", e);
                mainHandler.post(() -> Toast.makeText(getContext(), "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private ImportBundle parseSelectedReports(List<Uri> uris) throws IOException {
        ImportBundle bundle = new ImportBundle();
        for (Uri uri : uris) {
            String text = extractPdfText(uri);
            ReportType type = detectReportType(text, uri);
            switch (type) {
                case STOCK:
                    bundle.stockItems.addAll(parseStockReport(text));
                    break;
                case CUSTOMER:
                    bundle.customers.addAll(parseCustomerReport(text));
                    break;
                case DETAILED_BILL:
                    addBills(bundle.billsById, parseDetailedBillReport(text));
                    break;
                case SUMMARY_BILL:
                    addBills(bundle.summaryBillsById, parseSummaryBillReport(text));
                    break;
                default:
                    bundle.ignoredFiles++;
                    break;
            }
        }

        if (bundle.billsById.isEmpty() && !bundle.summaryBillsById.isEmpty()) {
            bundle.billsById.putAll(bundle.summaryBillsById);
        }

        return bundle;
    }

    private ImportResult restoreIntoDatabase(ImportBundle bundle) {
        ImportResult result = new ImportResult();
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            if (currentRestoreMode == RestoreMode.FULL) {
                dbHelper.clearAllDataForImport(db);
            }

            Map<String, List<Integer>> stockKeyToIds = new HashMap<>();
            if (currentRestoreMode == RestoreMode.FULL || currentRestoreMode == RestoreMode.STOCK_ONLY) {
                for (StockRow row : bundle.stockItems) {
                    long insertedId = dbHelper.insertItemForImport(db, row.id, row.name, row.weight, row.type, row.isSold);
                    if (insertedId != -1) {
                        int finalId = (int) insertedId;
                        stockKeyToIds.computeIfAbsent(buildItemKey(row.name, row.type, row.weight), k -> new ArrayList<>()).add(finalId);
                        result.stockCount++;
                    }
                }
            }

            Map<String, CustomerRow> customerByPhone = new HashMap<>();
            if (currentRestoreMode == RestoreMode.FULL || currentRestoreMode == RestoreMode.CUSTOMERS_ONLY || currentRestoreMode == RestoreMode.BILLS_ONLY) {
                for (CustomerRow row : bundle.customers) {
                    if (!isUsablePhone(row.phone)) {
                        continue;
                    }
                    customerByPhone.put(normalizePhone(row.phone), row);
                    long customerId = dbHelper.upsertCustomerForImport(db, row.name, normalizePhone(row.phone), row.village, row.debt);
                    if (customerId != -1) {
                        result.customerCount++;
                    }
                }
            }

            if (currentRestoreMode == RestoreMode.FULL || currentRestoreMode == RestoreMode.BILLS_ONLY) {
                Set<Integer> usedItemIds = new HashSet<>();
                for (BillRow bill : bundle.billsById.values()) {
                    String normalizedPhone = normalizePhone(bill.customerPhone);
                    CustomerRow linkedCustomer = customerByPhone.get(normalizedPhone);
                    String customerName = linkedCustomer != null ? linkedCustomer.name : bill.customerName;
                    String village = linkedCustomer != null ? linkedCustomer.village : "";
                    double debt = linkedCustomer != null ? linkedCustomer.debt : 0.0;

                    long customerId = dbHelper.upsertCustomerForImport(db, customerName, normalizedPhone, village, debt);
                    if (customerId == -1) {
                        continue;
                    }

                    String dbDate = toDatabaseDate(bill.billDateRaw);
                    long billId = dbHelper.insertBillForImport(
                            db,
                            bill.billId,
                            customerId,
                            bill.totalAmount,
                            bill.gstPercent,
                            "Cash",
                            "",
                            dbDate
                    );
                    if (billId == -1) {
                        continue;
                    }
                    result.billCount++;

                    for (BillItemRow billItem : bill.items) {
                        Integer resolvedItemId = consumeMatchingItemId(stockKeyToIds, usedItemIds, billItem);
                        if (resolvedItemId == null) {
                            long newItemId = dbHelper.insertItemForImport(db, null, billItem.name, billItem.weight, billItem.type, true);
                            if (newItemId != -1) {
                                resolvedItemId = (int) newItemId;
                                result.stockCount++;
                                registerImportedStockItem(stockKeyToIds, resolvedItemId, billItem);
                            }
                        }

                        if (resolvedItemId != null) {
                            usedItemIds.add(resolvedItemId);
                            ContentValues soldValues = new ContentValues();
                            soldValues.put("is_sold", 1);
                            db.update("items", soldValues, "id = ?", new String[]{String.valueOf(resolvedItemId)});
                            long linkId = dbHelper.insertBillItemForImport(db, billId, resolvedItemId);
                            if (linkId != -1) {
                                result.billItemCount++;
                            }
                        }
                    }
                }
            }

            result.ignoredFiles = bundle.ignoredFiles;
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return result;
    }

    private void registerImportedStockItem(Map<String, List<Integer>> stockKeyToIds, int itemId, BillItemRow billItem) {
        String key = buildItemKey(billItem.name, billItem.type, billItem.weight);
        stockKeyToIds.computeIfAbsent(key, ignored -> new ArrayList<>()).add(itemId);
    }

    private void addBills(Map<Integer, BillRow> target, Map<Integer, BillRow> source) {
        for (Map.Entry<Integer, BillRow> entry : source.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    private String extractPdfText(Uri uri) throws IOException {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             PDDocument document = inputStream != null ? PDDocument.load(inputStream) : null) {
            if (document == null) {
                throw new IOException("Could not open PDF");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private ReportType detectReportType(String text, Uri uri) {
        String lowerText = text == null ? "" : text.toLowerCase(Locale.getDefault());
        String lowerUri = uri.toString().toLowerCase(Locale.getDefault());

        if (lowerText.contains("stock report") || lowerUri.contains("stock_report")) {
            return ReportType.STOCK;
        }
        if (lowerText.contains("customer report") || lowerUri.contains("customer_report")) {
            return ReportType.CUSTOMER;
        }
        if (lowerText.contains("detailed bill report") || lowerUri.contains("detailed_bill_report")) {
            return ReportType.DETAILED_BILL;
        }
        if (lowerText.contains("bill history summary") || lowerUri.contains("bill_history_summary") || lowerText.contains("bill history report")) {
            return ReportType.SUMMARY_BILL;
        }
        return ReportType.UNKNOWN;
    }

    private Map<Integer, BillRow> parseDetailedBillReport(String text) {
        Map<Integer, BillRow> bills = new HashMap<>();
        List<String> lines = nonEmptyLines(text);

        BillRow current = null;
        Pattern billHeaderPattern = Pattern.compile("^Bill #(\\d+)(?: \\(.+\\))?$");
        Pattern customerPattern = Pattern.compile("^Customer:\\s*(.*?)\\s*\\((.*?)\\)\\s*$", Pattern.CASE_INSENSITIVE);
        Pattern datePattern = Pattern.compile("^Date:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Pattern gstPattern = Pattern.compile("^GST Applied:\\s*([0-9]+(?:\\.[0-9]+)?)%$", Pattern.CASE_INSENSITIVE);
        Pattern totalPattern = Pattern.compile("^Total Amount:\\s*(.+)$", Pattern.CASE_INSENSITIVE);
        Pattern itemPattern = Pattern.compile("^[•\\-]?\\s*(.+?)\\s*\\((Gold|Silver)\\)\\s*[-–]\\s*([0-9.,]+)\\s*g\\s*$", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher billHeaderMatcher = billHeaderPattern.matcher(line);
            if (billHeaderMatcher.find()) {
                int billId = parseIntSafe(billHeaderMatcher.group(1), -1);
                if (billId > 0) {
                    current = bills.computeIfAbsent(billId, id -> new BillRow(id));
                }
                continue;
            }

            if (current == null) {
                continue;
            }

            Matcher customerMatcher = customerPattern.matcher(line);
            if (customerMatcher.find()) {
                current.customerName = customerMatcher.group(1).trim();
                current.customerPhone = normalizePhone(customerMatcher.group(2));
                continue;
            }

            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find()) {
                current.billDateRaw = dateMatcher.group(1).trim();
                continue;
            }

            Matcher gstMatcher = gstPattern.matcher(line);
            if (gstMatcher.find()) {
                current.gstPercent = parseDoubleSafe(gstMatcher.group(1), 0.0);
                continue;
            }

            Matcher totalMatcher = totalPattern.matcher(line);
            if (totalMatcher.find()) {
                current.totalAmount = parseMoney(totalMatcher.group(1));
                continue;
            }

            Matcher itemMatcher = itemPattern.matcher(line);
            if (itemMatcher.find()) {
                String itemName = itemMatcher.group(1).trim();
                String itemType = capitalize(itemMatcher.group(2).trim());
                double weight = parseDoubleSafe(itemMatcher.group(3), 0.0);
                if (!itemName.isEmpty() && weight > 0) {
                    current.items.add(new BillItemRow(itemName, itemType, weight));
                }
            }
        }

        bills.values().removeIf(bill -> !isUsablePhone(bill.customerPhone));
        return bills;
    }

    private Map<Integer, BillRow> parseSummaryBillReport(String text) {
        Map<Integer, BillRow> bills = new HashMap<>();
        List<String> lines = nonEmptyLines(text);
        Pattern summaryPattern = Pattern.compile("^(\\d+)\\s+(.+?)\\s+(\\d{2}\\s+[A-Za-z]{3}\\s+\\d{4})\\s+(.+)$");

        for (String line : lines) {
            Matcher matcher = summaryPattern.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            int billId = parseIntSafe(matcher.group(1), -1);
            if (billId <= 0) {
                continue;
            }

            BillRow bill = new BillRow(billId);
            bill.customerName = matcher.group(2).trim();
            bill.customerPhone = "";
            bill.billDateRaw = matcher.group(3).trim();
            bill.totalAmount = parseMoney(matcher.group(4));
            bills.put(billId, bill);
        }

        return bills;
    }

    private List<StockRow> parseStockReport(String text) {
        List<StockRow> rows = new ArrayList<>();
        List<String> lines = nonEmptyLines(text);
        Pattern rowPattern = Pattern.compile("^(\\d+)\\s+(.+?)\\s+([0-9]+(?:\\.[0-9]+)?)\\s+(Gold|Silver)\\s+(Sold|Available)$", Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher matcher = rowPattern.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            int id = parseIntSafe(matcher.group(1), -1);
            if (id <= 0) {
                continue;
            }

            String name = matcher.group(2).trim();
            double weight = parseDoubleSafe(matcher.group(3), 0.0);
            String type = capitalize(matcher.group(4).trim());
            boolean sold = matcher.group(5).equalsIgnoreCase("Sold");
            rows.add(new StockRow(id, name, weight, type, sold));
        }

        return rows;
    }

    private List<CustomerRow> parseCustomerReport(String text) {
        List<CustomerRow> rows = new ArrayList<>();
        List<String> lines = nonEmptyLines(text);
        Pattern rowPattern = Pattern.compile("^(.*?)\\s+(\\+?\\d[\\d\\s-]{7,}\\d)\\s+(.*?)\\s+(₹?\\s*[0-9,]+(?:\\.[0-9]+)?)\\s+(.*)$");

        for (String line : lines) {
            Matcher matcher = rowPattern.matcher(line);
            if (!matcher.find()) {
                continue;
            }

            String name = matcher.group(1).trim();
            String phone = normalizePhone(matcher.group(2));
            String village = matcher.group(3).trim();
            double debt = parseMoney(matcher.group(4));

            if (!isUsablePhone(phone)) {
                continue;
            }

            rows.add(new CustomerRow(name, phone, village, debt));
        }

        return rows;
    }

    private String buildImportResultMessage(ImportResult result) {
        return "Restore complete: "
                + result.stockCount + " stock, "
                + result.customerCount + " customers, "
                + result.billCount + " bills, "
                + result.billItemCount + " bill items"
                + (result.ignoredFiles > 0 ? ", ignored files: " + result.ignoredFiles : "");
    }

    private Integer consumeMatchingItemId(Map<String, List<Integer>> keyMap, Set<Integer> usedIds, BillItemRow item) {
        String key = buildItemKey(item.name, item.type, item.weight);
        List<Integer> candidateIds = keyMap.get(key);
        if (candidateIds == null || candidateIds.isEmpty()) {
            return null;
        }

        for (Integer id : candidateIds) {
            if (!usedIds.contains(id)) {
                usedIds.add(id);
                return id;
            }
        }
        return null;
    }

    private String buildItemKey(String name, String type, double weight) {
        String normalizedName = name == null ? "" : name.trim().toLowerCase(Locale.getDefault()).replaceAll("\\s+", " ");
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.getDefault());
        return normalizedName + "|" + normalizedType + "|" + String.format(Locale.US, "%.3f", weight);
    }

    private List<String> nonEmptyLines(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String normalized = text.replace('\r', '\n');
        for (String line : normalized.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[^0-9+]", "").trim();
    }

    private boolean isUsablePhone(String phone) {
        String normalized = normalizePhone(phone);
        return normalized.length() >= 10;
    }

    private String toDatabaseDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return null;
        }

        List<SimpleDateFormat> inputFormats = new ArrayList<>();
        inputFormats.add(new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()));
        inputFormats.add(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()));

        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (SimpleDateFormat inputFormat : inputFormats) {
            try {
                Date parsed = inputFormat.parse(rawDate.trim());
                if (parsed != null) {
                    return outputFormat.format(parsed);
                }
            } catch (ParseException ignored) {
            }
        }
        return null;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String lower = value.toLowerCase(Locale.getDefault());
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseDoubleSafe(String value, double fallback) {
        try {
            return Double.parseDouble(value.replace(",", "").trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private double parseMoney(String value) {
        if (value == null) {
            return 0.0;
        }
        String cleaned = value.replace("₹", "")
                .replace("Rs.", "")
                .replace("Rs", "")
                .replace(",", "")
                .trim();
        return parseDoubleSafe(cleaned, 0.0);
    }

    private void loadTotalDebt() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        try {
            // Query sum of debt from customer table
            cursor = db.rawQuery("SELECT SUM(debt) FROM customer", null);
            if (cursor.moveToFirst()) {
                double totalDebt = cursor.getDouble(0);
                tvTotalDebt.setText(currencyFormat.format(totalDebt));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching debt", e);
            tvTotalDebt.setText("Error");
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        importExecutor.shutdownNow();
    }

    private static class StockRow {
        final Integer id;
        final String name;
        final double weight;
        final String type;
        final boolean isSold;

        StockRow(Integer id, String name, double weight, String type, boolean isSold) {
            this.id = id;
            this.name = name;
            this.weight = weight;
            this.type = type;
            this.isSold = isSold;
        }
    }

    private static class CustomerRow {
        final String name;
        final String phone;
        final String village;
        final double debt;

        CustomerRow(String name, String phone, String village, double debt) {
            this.name = name;
            this.phone = phone;
            this.village = village;
            this.debt = debt;
        }
    }

    private static class BillItemRow {
        final String name;
        final String type;
        final double weight;

        BillItemRow(String name, String type, double weight) {
            this.name = name;
            this.type = type;
            this.weight = weight;
        }
    }

    private static class BillRow {
        final Integer billId;
        String customerName = "Unknown";
        String customerPhone = "";
        String billDateRaw = null;
        double totalAmount = 0.0;
        double gstPercent = 0.0;
        final List<BillItemRow> items = new ArrayList<>();

        BillRow(Integer billId) {
            this.billId = billId;
        }
    }

    private static class ImportBundle {
        final List<StockRow> stockItems = new ArrayList<>();
        final List<CustomerRow> customers = new ArrayList<>();
        final Map<Integer, BillRow> billsById = new HashMap<>();
        final Map<Integer, BillRow> summaryBillsById = new HashMap<>();
        int ignoredFiles = 0;
    }

    private static class ImportResult {
        int stockCount;
        int customerCount;
        int billCount;
        int billItemCount;
        int ignoredFiles;
    }
}