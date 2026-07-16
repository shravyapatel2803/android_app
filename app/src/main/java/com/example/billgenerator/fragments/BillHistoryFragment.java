package com.example.billgenerator.fragments;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.ChipGroup;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.BillHistoryAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.BillHistoryModel;
import com.example.billgenerator.ui.UiAnimationHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillHistoryFragment extends Fragment {

    private enum BillFilterMode {
        ALL,
        TODAY,
        THIS_MONTH,
        PENDING_DEBT
    }

    private RecyclerView recyclerView;
    private View billHistoryEmpty;
    private TextView totalSalesText, totalCollectedText;
    private BillHistoryAdapter adapter;
    private ArrayList<BillHistoryModel> allBillsList = new ArrayList<>(); // Master list
    private ArrayList<BillHistoryModel> filteredList = new ArrayList<>(); // List shown in UI/PDF
    private databaseSystem dbHelper;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final String TAG = "BillHistoryFragment"; // For logging
    private BillFilterMode selectedFilterMode = BillFilterMode.ALL;
    private String searchQuery = "";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        return inflater.inflate(R.layout.fragment_bill_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.bill_history_menu, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setQueryHint("Search by name or bill ID...");
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override public boolean onQueryTextSubmit(String query) {
                            searchQuery = query == null ? "" : query;
                            applyFilters();
                            return false;
                        }
                        @Override public boolean onQueryTextChange(String newText) {
                            searchQuery = newText == null ? "" : newText;
                            applyFilters();
                            return true;
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == R.id.action_generate_simple_pdf) {
                    generateSimpleBillsPdf();
                    return true;
                } else if (itemId == R.id.action_generate_detailed_pdf) {
                    generateDetailedBillsPdf();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        dbHelper = new databaseSystem(getContext());
        recyclerView = view.findViewById(R.id.bill_history_recyclerView);
        billHistoryEmpty = view.findViewById(R.id.bill_history_empty);
        totalSalesText = view.findViewById(R.id.bill_history_total_sales);
        totalCollectedText = view.findViewById(R.id.bill_history_total_collected);
        setupFilterChips(view);
        setupRecyclerView();
        UiAnimationHelper.configureEmptyState(
                billHistoryEmpty,
                R.drawable.ic_empty_bills,
                "No bills found",
                "Generate a bill or adjust your filters to see records here.",
                null,
                null
        );
        loadBillHistory(); // Load data initially
    }

    private void setupFilterChips(View view) {
        ChipGroup filterGroup = view.findViewById(R.id.bill_history_filter_group);
        if (filterGroup == null) {
            return;
        }

        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                selectedFilterMode = BillFilterMode.ALL;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_bill_filter_today) {
                    selectedFilterMode = BillFilterMode.TODAY;
                } else if (checkedId == R.id.chip_bill_filter_month) {
                    selectedFilterMode = BillFilterMode.THIS_MONTH;
                } else if (checkedId == R.id.chip_bill_filter_debt) {
                    selectedFilterMode = BillFilterMode.PENDING_DEBT;
                } else {
                    selectedFilterMode = BillFilterMode.ALL;
                }
            }
            applyFilters();
        });
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");
        adapter = new BillHistoryAdapter(requireContext(), filteredList); // Adapter uses filtered list
        
        // Dynamic column count for tablets
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (isTablet) {
            recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        } else {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        recyclerView.setAdapter(adapter);
        UiAnimationHelper.setupRecyclerViewAnimations(recyclerView);
    }

    // Loads bill summary data into the master list (allBillsList)
    private void loadBillHistory() {
        Log.d(TAG, "Loading bill history summary...");
        allBillsList.clear();
        double totalSales = 0;
        double totalCollected = 0;

        Cursor cursor = dbHelper.fetchBillHistory(); // Fetches ID, Name, Date, TotalAmount
        if (cursor != null) {
            Log.d(TAG, "Bill history cursor has " + cursor.getCount() + " rows.");
            try {
                if (cursor.moveToFirst()) {
                    int billIdCol = cursor.getColumnIndexOrThrow("id");
                    int customerNameCol = cursor.getColumnIndexOrThrow("name");
                    int dateCol = cursor.getColumnIndexOrThrow("bill_date");
                    int totalCol = cursor.getColumnIndexOrThrow("total_amount");
                    int debtCol = cursor.getColumnIndexOrThrow("debt_amount");

                    do {
                        int billId = cursor.getInt(billIdCol);
                        String customerName = cursor.getString(customerNameCol);
                        String rawDate = cursor.getString(dateCol);
                        double totalAmount = cursor.getDouble(totalCol);
                        double debtAmount = cursor.getDouble(debtCol);
                        
                        totalSales += totalAmount;
                        totalCollected += (totalAmount - debtAmount);

                        String formattedListDate = formatListDate(rawDate); // Format date for list display
                        allBillsList.add(new BillHistoryModel(billId, customerName, formattedListDate, rawDate, totalAmount, debtAmount));
                        Log.v(TAG, "Loaded summary: BillID=" + billId + ", Name=" + customerName);
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading bill history cursor: " + e.getMessage());
            } finally {
                cursor.close();
            }
        } else {
            Log.w(TAG, "fetchBillHistory returned null cursor.");
        }

        if (totalSalesText != null) totalSalesText.setText(currencyFormat.format(totalSales));
        if (totalCollectedText != null) totalCollectedText.setText(currencyFormat.format(totalCollected));

        applyFilters();
        Log.d(TAG, "Finished loading bill history. Master list size: " + allBillsList.size());
    }

    // Formatter for list display (Date only)
    private String formatListDate(String dateStr) {
        if (dateStr == null) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) return outputFormat.format(date);
        } catch (ParseException e) {
            Log.w(TAG, "Could not parse list date: " + dateStr);
        }
        return dateStr; // Fallback
    }

    private String formatIndiaCurrency(double amount) {
        return currencyFormat.format(amount);
    }

    // Formatter for PDF/Dialog display (Date and Time)
    private String formatPdfDateTime(String dateStr) {
        if (dateStr == null) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) return outputFormat.format(date);
        } catch (ParseException e) {
            Log.w(TAG, "Could not parse PDF date/time: " + dateStr);
        }
        return dateStr; // Fallback
    }

    private String safeValue(String value) {
        return (value == null || value.trim().isEmpty()) ? "N/A" : value.trim();
    }



    // Filters the displayed list based on search text
    private void applyFilters() {
        Log.d(TAG, "Applying filters. mode=" + selectedFilterMode + ", search='" + searchQuery + "'");
        filteredList.clear(); // Start with an empty list to display
        String searchText = (searchQuery == null) ? "" : searchQuery.toLowerCase(Locale.getDefault());

        String todayPrefix = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String monthPrefix = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        for (BillHistoryModel bill : allBillsList) {
            boolean matchesSearch = searchText.isEmpty()
                    || bill.customerName.toLowerCase(Locale.getDefault()).contains(searchText)
                    || String.valueOf(bill.billId).contains(searchText);

            if (!matchesSearch) {
                continue;
            }

            boolean matchesMode;
            switch (selectedFilterMode) {
                case TODAY:
                    matchesMode = bill.rawBillDate != null && bill.rawBillDate.startsWith(todayPrefix);
                    break;
                case THIS_MONTH:
                    matchesMode = bill.rawBillDate != null && bill.rawBillDate.startsWith(monthPrefix);
                    break;
                case PENDING_DEBT:
                    matchesMode = bill.debtAmount > 0.001;
                    break;
                case ALL:
                default:
                    matchesMode = true;
                    break;
            }

            if (matchesMode) {
                filteredList.add(bill);
            }
        }

        Log.d(TAG, "Filters applied, showing " + filteredList.size() + " bills.");
        // Notify the adapter that the data *it uses* (filteredList) has changed
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (recyclerView != null) {
                recyclerView.scheduleLayoutAnimation();
            }
        }
        UiAnimationHelper.setVisible(billHistoryEmpty, filteredList.isEmpty());
    }

    // --- Generates a PDF summary of bills currently displayed ---
    private void generateSimpleBillsPdf() {
        Log.d(TAG, "Generating Simple Bills PDF...");
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No bills to generate PDF (check filter).", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595, pageHeight = 842; // A4 Portrait
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        int x = 30, y = 50;
        final int marginBottom = 50; // Margin at the bottom
        final int lineSpacing = 18;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Bill History Summary", x, y, titlePaint);
        y += 25;

        paint.setTextSize(9f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        // Draw Headers
        paint.setTextSize(11f);
        paint.setFakeBoldText(true);
        canvas.drawText("Bill ID", x, y, paint);
        canvas.drawText("Customer Name", x + 60, y, paint);
        canvas.drawText("Date", x + 300, y, paint);
        canvas.drawText("Total Amount", x + 400, y, paint); // Adjusted position for alignment
        y += 20;
        canvas.drawLine(x, y - 8, pageWidth - x, y - 8, paint); // Draw line below headers
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);

        for (BillHistoryModel bill : filteredList) { // Use the filtered list
            // Check for page break BEFORE drawing the current bill's data
            if (y > pageHeight - marginBottom) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50; // Reset Y for new page
                // Redraw headers on new page
                paint.setTextSize(11f); paint.setFakeBoldText(true);
                canvas.drawText("Bill ID", x, y, paint); canvas.drawText("Customer Name", x + 60, y, paint); canvas.drawText("Date", x + 300, y, paint); canvas.drawText("Total Amount", x + 400, y, paint);
                y += 20; canvas.drawLine(x, y - 8, pageWidth - x, y - 8, paint);
                paint.setFakeBoldText(false); paint.setTextSize(10f);
            }

            canvas.drawText(String.valueOf(bill.billId), x, y, paint);
            // Truncate long names if necessary
            String nameToDraw = bill.customerName.length() > 30 ? bill.customerName.substring(0, 27) + "..." : bill.customerName;
            canvas.drawText(nameToDraw, x + 60, y, paint);
            canvas.drawText(bill.billDate, x + 300, y, paint); // Use pre-formatted date from model
            canvas.drawText(currencyFormat.format(bill.totalAmount), x + 400, y, paint);
            y += lineSpacing; // Increment Y position for the next line
        }
        document.finishPage(page); // Finish the last page

        savePdfDocument(document, "Bill_History_Summary"); // Use helper method to save
        Log.i(TAG, "Simple Bill PDF generation complete.");
    }


    // --- Generates a detailed PDF including items for each bill currently displayed ---
    private void generateDetailedBillsPdf() {
        Log.d(TAG, "Generating Detailed Bills PDF...");
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No bills to generate detailed PDF (check filter).", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595, pageHeight = 842; // A4 Portrait
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Setup Paint objects
        Paint paint = new Paint(); // Normal text
        paint.setTextSize(10f);
        paint.setColor(Color.BLACK);

        Paint boldPaint = new Paint(paint); // Bold text
        boldPaint.setFakeBoldText(true);

        Paint headerPaint = new Paint(boldPaint); // Slightly larger bold for bill headers
        headerPaint.setTextSize(12f);

        Paint titlePaint = new Paint(headerPaint); // Largest bold for main title
        titlePaint.setTextSize(18f);

        Paint smallPaint = new Paint(paint); // Smaller text for generation date
        smallPaint.setTextSize(9f);


        int x = 30; // Left margin
        int y = 50; // Starting Y position
        final int lineSpacing = 16;
        final int itemIndent = x + 15; // Indentation for items
        final int sectionSpacing = 25; // Space after each bill section
        final int marginBottom = 50; // Bottom page margin

        // --- PDF Header ---
        canvas.drawText("Detailed Bill Report", x, y, titlePaint);
        y += 25;
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, smallPaint);
        y += sectionSpacing;

        // --- Loop through each bill in the filtered list ---
        for (BillHistoryModel billSummary : filteredList) {
            Log.d(TAG, "Processing Bill ID " + billSummary.billId + " for detailed PDF.");
            // --- Estimate space needed for the next bill entry (header + a few items) ---
            int estimatedHeight = 8 * lineSpacing; // Header lines + separator + total + items header + couple of items
            if (y > pageHeight - marginBottom - estimatedHeight) {
                Log.d(TAG, "Page break needed before Bill ID " + billSummary.billId);
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50; // Reset Y
            }

            // Fetch full details for this specific bill
            Cursor billCursor = dbHelper.getBillDetails(billSummary.billId);
            if (billCursor != null && billCursor.moveToFirst()) {
                Log.d(TAG, "Fetched details for Bill ID " + billSummary.billId);
                try {
                    String custName = billCursor.getString(billCursor.getColumnIndexOrThrow("name"));
                    String custPhone = billCursor.getString(billCursor.getColumnIndexOrThrow("phone"));
                    String billDateTime = formatPdfDateTime(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date")));
                    double goldRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_gold_rate"));
                    double silverRate = billCursor.getDouble(billCursor.getColumnIndexOrThrow("calc_silver_rate"));
                    double totalAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                    int billedAmountIndex = billCursor.getColumnIndex("billed_amount");
                    int paidAmountIndex = billCursor.getColumnIndex("paid_amount");
                    double billedAmount = billedAmountIndex != -1 ? billCursor.getDouble(billedAmountIndex) : totalAmount;
                    double paidAmount = paidAmountIndex != -1 ? billCursor.getDouble(paidAmountIndex) : totalAmount;
                    double gstPercent = billCursor.getDouble(billCursor.getColumnIndexOrThrow("gst_percent"));
                    String paymentMode = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_mode"));
                    String paymentDetails = billCursor.getString(billCursor.getColumnIndexOrThrow("payment_details"));
                    String returnItemType = billCursor.getString(billCursor.getColumnIndexOrThrow("return_item_type"));
                    double returnItemWeight = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_weight"));
                    double returnItemDeduction = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_deduct_amount"));
                    String debtDueDate = billCursor.getString(billCursor.getColumnIndexOrThrow("debt_due_date"));
                    double debtAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));

                    // --- Draw Bill Header ---
                    canvas.drawText("Bill #" + billSummary.billId, x, y, headerPaint);
                    y += lineSpacing;
                    canvas.drawText("Customer: " + safeValue(custName) + " (" + safeValue(custPhone) + ")", x, y, paint);
                    y += lineSpacing;
                    canvas.drawText("Date: " + billDateTime, x, y, paint);
                    y += lineSpacing;
                    canvas.drawText("Payment: " + safeValue(paymentMode) + " | " + safeValue(paymentDetails), x, y, paint);
                    y += lineSpacing;
                    canvas.drawText(String.format(Locale.getDefault(), "Billed: %s | Paid: %s | Debt: %s",
                            currencyFormat.format(billedAmount),
                            currencyFormat.format(paidAmount),
                            currencyFormat.format(debtAmount)), x, y, paint);
                    y += lineSpacing;
                    if (debtAmount > 0.001) {
                        canvas.drawText("Debt Due Date: " + safeValue(debtDueDate), x, y, paint);
                        y += lineSpacing;
                    }
                    // Display rates if available
                    if (goldRate > 0 || silverRate > 0) {
                        canvas.drawText(String.format(Locale.getDefault(), "Approx. Rates: Gold %s/10g, Silver %s/kg",
                                currencyFormat.format(goldRate), currencyFormat.format(silverRate)), x, y, paint);
                        y += lineSpacing;
                    }
                    // Display GST if applied
                    if (gstPercent > 0) {
                        canvas.drawText(String.format(Locale.getDefault(), "GST Applied: %.2f%%", gstPercent), x, y, paint);
                        y += lineSpacing;
                    }

                    // Fetch Return Items for this bill
                    Cursor returnCursor = dbHelper.getReturnItemsForBill(billSummary.billId);
                    boolean hasReturnItems = false;
                    if (returnCursor != null && returnCursor.moveToFirst()) {
                        hasReturnItems = true;
                        int rTypeCol = returnCursor.getColumnIndexOrThrow("return_type");
                        int rWeightCol = returnCursor.getColumnIndexOrThrow("return_weight");
                        int rDeductCol = returnCursor.getColumnIndexOrThrow("return_deduct_amount");

                        canvas.drawText("Return Items:", x, y, boldPaint);
                        y += lineSpacing;

                        do {
                            if (y > pageHeight - marginBottom) {
                                document.finishPage(page);
                                page = document.startPage(pageInfo);
                                canvas = page.getCanvas();
                                y = 50;
                                canvas.drawText("Bill #" + billSummary.billId + " (continued)", x, y, headerPaint); y += lineSpacing;
                                canvas.drawText("Return Items (continued):", x, y, boldPaint); y += lineSpacing;
                            }

                            String rType = returnCursor.getString(rTypeCol);
                            double rWeight = returnCursor.getDouble(rWeightCol);
                            double rDeduct = returnCursor.getDouble(rDeductCol);

                            canvas.drawText(String.format(Locale.getDefault(), "• %s: %.3f g | Deduction: %s",
                                    rType, rWeight, currencyFormat.format(rDeduct)), itemIndent, y, paint);
                            y += lineSpacing;
                        } while (returnCursor.moveToNext());
                        returnCursor.close();
                    }

                    if (!hasReturnItems) {
                        // Fallback to old single return column if no new return items found (for old bills)
                         returnItemType = billCursor.getString(billCursor.getColumnIndexOrThrow("return_item_type"));
                         returnItemWeight = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_weight"));
                         returnItemDeduction = billCursor.getDouble(billCursor.getColumnIndexOrThrow("return_item_deduct_amount"));

                        boolean hasOldReturn = (returnItemType != null && !returnItemType.trim().isEmpty())
                                || returnItemWeight > 0.001
                                || returnItemDeduction > 0.001;

                        if (hasOldReturn) {
                            canvas.drawText("Return Item:", x, y, boldPaint);
                            y += lineSpacing;
                            canvas.drawText(String.format(Locale.getDefault(), "• %s: %.3f g | Deduction: %s",
                                    safeValue(returnItemType), returnItemWeight, currencyFormat.format(returnItemDeduction)), itemIndent, y, paint);
                            y += lineSpacing;
                        }
                    }

                    y += 5; // Extra space before items header

                    // --- Draw Items Header ---
                    canvas.drawText("Items Purchased:", x, y, boldPaint);
                    y += lineSpacing;

                } catch (Exception e) {
                    Log.e(TAG, "Error reading bill details cursor for PDF: " + e.getMessage());
                    // Draw an error message for this bill in the PDF?
                    canvas.drawText("Error loading details for Bill #" + billSummary.billId, x, y, paint);
                    y += lineSpacing;
                } finally {
                    billCursor.close();
                }

                // --- Fetch and Draw Items for this bill ---
                Cursor itemCursor = dbHelper.getItemsForBill(billSummary.billId);
                boolean itemsFound = false;
                if (itemCursor != null) {
                    Log.d(TAG, "Fetched " + itemCursor.getCount() + " items for Bill ID " + billSummary.billId);
                    try {
                        if (itemCursor.moveToFirst()) {
                            itemsFound = true;
                            int nameCol = itemCursor.getColumnIndexOrThrow("name");
                            int weightCol = itemCursor.getColumnIndexOrThrow("weight");
                            int typeCol = itemCursor.getColumnIndexOrThrow("type");
                            int itemIndex = 1;
                            do {
                                // --- Check for page break before drawing EACH item ---
                                if (y > pageHeight - marginBottom) {
                                    Log.v(TAG, "Page break needed within items list for Bill ID " + billSummary.billId);
                                    document.finishPage(page);
                                    page = document.startPage(pageInfo);
                                    canvas = page.getCanvas();
                                    y = 50; // Reset Y
                                    // Optionally redraw bill header on new page if items span pages
                                    canvas.drawText("Bill #" + billSummary.billId + " (continued)", x, y, headerPaint); y += lineSpacing;
                                    canvas.drawText("Items Purchased (continued):", x, y, boldPaint); y += lineSpacing;
                                }

                                String iName = itemCursor.getString(nameCol);
                                double iWeight = itemCursor.getDouble(weightCol);
                                String iType = itemCursor.getString(typeCol);
                                canvas.drawText(String.format(Locale.getDefault(), "%d. %s (%s) - %.3f g",
                                        itemIndex,
                                        safeValue(iName),
                                        safeValue(iType),
                                        iWeight), itemIndent, y, paint);
                                y += lineSpacing;
                                itemIndex++;
                            } while (itemCursor.moveToNext());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading item cursor for PDF: " + e.getMessage());
                        canvas.drawText(" (Error loading items)", itemIndent, y, paint); y += lineSpacing;
                    } finally {
                        itemCursor.close();
                    }
                } else {
                    Log.w(TAG, "Item cursor was null for Bill ID " + billSummary.billId);
                }

                // If cursor was valid but empty, or loop didn't run
                if (!itemsFound) {
                    canvas.drawText(" (No items recorded)", itemIndent, y, paint);
                    y+= lineSpacing;
                }


                // --- Draw Bill Total ---
                if (y > pageHeight - marginBottom - lineSpacing) { // Check again before drawing total
                    Log.v(TAG, "Page break needed before total for Bill ID " + billSummary.billId);
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50; // Reset Y
                }
                // Use total amount from summary model as it's already available
                canvas.drawText("Total Amount: " + currencyFormat.format(billSummary.totalAmount), x, y, boldPaint);
                y += sectionSpacing; // Space before next bill's section

                // --- Draw a separator line (optional) ---
                if (y < pageHeight - marginBottom) { // Avoid drawing line at very bottom
                    canvas.drawLine(x, y - (sectionSpacing / 2), pageWidth - x, y - (sectionSpacing / 2), paint);
                }


            } else { // billCursor was null or empty initially
                Log.e(TAG, "Could not fetch details for Bill ID " + billSummary.billId + " for PDF.");
                // Draw placeholder/error in PDF
                canvas.drawText("Bill #" + billSummary.billId, x, y, headerPaint); y += lineSpacing;
                canvas.drawText("Customer: " + billSummary.customerName, x, y, paint); y += lineSpacing;
                canvas.drawText(" (Error fetching full details) ", x, y, paint); y += lineSpacing;
                canvas.drawText("Total Amount: " + currencyFormat.format(billSummary.totalAmount), x, y, boldPaint); y+= sectionSpacing;
                if (y < pageHeight - marginBottom) canvas.drawLine(x, y - (sectionSpacing / 2), pageWidth - x, y - (sectionSpacing / 2), paint);
                if(billCursor != null) billCursor.close(); // Ensure closed even if moveToFirst failed
            }
        } // End of loop through bills

        document.finishPage(page); // Finish the very last page
        savePdfDocument(document, "Detailed_Bill_Report"); // Use helper to save
        Log.i(TAG, "Detailed Bill PDF generation complete.");
    }

    // --- HELPER METHOD TO SAVE PDF using MediaStore ---
    private void savePdfDocument(PdfDocument document, String baseFileName) {
        Log.d(TAG, "Saving PDF: " + baseFileName);
        ContentValues values = new ContentValues();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = baseFileName + "_" + timestamp + ".pdf";

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = null;
        OutputStream outputStream = null;
        try {
            // Use ContentResolver which doesn't require direct storage permission on API 29+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = requireActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                // Handling for older APIs would require WRITE_EXTERNAL_STORAGE permission
                Log.w(TAG, "MediaStore API level lower than Q, saving PDF might require explicit permission.");
                Toast.makeText(getContext(), "Saving PDF requires storage permission on older Android versions.", Toast.LENGTH_LONG).show();
                document.close();
                return; // Stop if on older version and permission not handled
            }

            if (uri == null) { throw new IOException("Failed to create MediaStore entry for PDF."); }

            outputStream = requireActivity().getContentResolver().openOutputStream(uri);
            if (outputStream == null) { throw new IOException("Failed to open output stream for URI: " + uri); }

            document.writeTo(outputStream);
            Log.i(TAG, "PDF saved successfully to Downloads: " + fileName);
            Toast.makeText(getContext(), fileName + " saved to Downloads!", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF '" + fileName + "': " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Attempt to delete the partially created file if URI was obtained
            if (uri != null) {
                try { requireActivity().getContentResolver().delete(uri, null, null); } catch (Exception deleteEx) { Log.e(TAG, "Error deleting partial PDF URI: " + deleteEx.getMessage());}
            }
        } catch (IllegalStateException e) {
            // Catch IllegalStateException if requireActivity() or requireContext() is called when fragment not attached
            Log.e(TAG, "Error saving PDF - Fragment not attached: " + e.getMessage());
            Toast.makeText(getActivity(), "Error: Could not save PDF (Fragment not ready).", Toast.LENGTH_SHORT).show(); // Use getActivity() for context here
        } finally {
            // Ensure resources are closed
            if (outputStream != null) {
                try { outputStream.close(); } catch (IOException e) { Log.e(TAG, "Error closing PDF output stream: " + e.getMessage()); }
            }
            document.close(); // Always close the document
        }
    }


    // Refreshes data when the fragment becomes visible again
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Reload data from DB in case bills were added/deleted elsewhere
        if (dbHelper != null) {
            loadBillHistory();
        }
    }
}