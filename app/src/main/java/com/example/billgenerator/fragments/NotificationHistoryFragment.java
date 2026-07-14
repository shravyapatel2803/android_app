package com.example.billgenerator.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.NotificationHistoryAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.NotificationHistoryItem;
import com.example.billgenerator.ui.UiAnimationHelper;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NotificationHistoryFragment extends Fragment {

    private final ArrayList<NotificationHistoryItem> items = new ArrayList<>();
    private NotificationHistoryAdapter adapter;
    private databaseSystem dbHelper;
    private View emptyView;
    private RecyclerView recyclerView;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private static final String TAG = "NotificationHistoryFrag";
    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_AUTO_CLEAR_DAYS = "auto_clear_days";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new databaseSystem(requireContext());

        recyclerView = view.findViewById(R.id.notification_history_recycler);
        emptyView = view.findViewById(R.id.notification_history_empty);

        adapter = new NotificationHistoryAdapter(items);
        adapter.setOnItemClickListener(this::showBillDetailDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        UiAnimationHelper.setupRecyclerViewAnimations(recyclerView);
        UiAnimationHelper.configureEmptyState(
                emptyView,
                R.drawable.ic_empty_notifications,
                "No reminders yet",
                "Debt due-date notifications will appear here once reminders are sent.",
                null,
                null
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        performAutoClear();
        loadData();
    }

    private void performAutoClear() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int days = prefs.getInt(KEY_AUTO_CLEAR_DAYS, -1); // -1 means never
        if (days > 0) {
            dbHelper.clearOldNotifications(days);
        }
    }

    private void loadData() {
        items.clear();
        Cursor cursor = null;
        try {
            cursor = dbHelper.fetchNotificationHistory();
            if (cursor != null && cursor.moveToFirst()) {
                int billIdCol = cursor.getColumnIndexOrThrow("bill_id");
                int customerCol = cursor.getColumnIndexOrThrow("customer_name");
                int messageCol = cursor.getColumnIndexOrThrow("message");
                int typeCol = cursor.getColumnIndexOrThrow("notification_type");
                int dateCol = cursor.getColumnIndexOrThrow("notified_date");
                int createdCol = cursor.getColumnIndexOrThrow("created_at");
                do {
                    items.add(new NotificationHistoryItem(
                            cursor.getInt(billIdCol),
                            cursor.getString(customerCol),
                            cursor.getString(messageCol),
                            cursor.getString(typeCol),
                            cursor.getString(dateCol),
                            cursor.getString(createdCol)
                    ));
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        adapter.notifyDataSetChanged();
        if (!items.isEmpty() && recyclerView != null) {
            recyclerView.scheduleLayoutAnimation();
        }
        UiAnimationHelper.setVisible(emptyView, items.isEmpty());
    }

    private void showBillDetailDialog(NotificationHistoryItem item) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bill_detail_dialog);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Toolbar toolbar = dialog.findViewById(R.id.toolbar_bill_detail);
        TextView detailCustomerName = dialog.findViewById(R.id.detail_customer_name);
        TextView detailCustomerPhone = dialog.findViewById(R.id.detail_customer_phone);
        TextView detailBillId = dialog.findViewById(R.id.detail_bill_id);
        TextView detailBillDate = dialog.findViewById(R.id.detail_bill_date);
        TextView detailBilledAmount = dialog.findViewById(R.id.detail_billed_amount);
        TextView detailPaidAmount = dialog.findViewById(R.id.detail_paid_amount);
        TextView detailDebtStatus = dialog.findViewById(R.id.detail_debt_status);
        TextView detailTotalAmount = dialog.findViewById(R.id.detail_total_amount);
        LinearLayout itemsContainer = dialog.findViewById(R.id.container_bill_items);

        // Hide action buttons as this is a read-only view from notifications
        View actionButtons = dialog.findViewById(R.id.whatsapp_button);
        if (actionButtons != null && actionButtons.getParent() instanceof View) {
            ((View)actionButtons.getParent()).setVisibility(View.GONE);
        }

        if (toolbar != null) {
            toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
            toolbar.setNavigationOnClickListener(v -> dialog.dismiss());
            toolbar.setTitle("Bill Details");
        }

        Cursor billCursor = dbHelper.getBillDetails(item.billId);
        if (billCursor != null && billCursor.moveToFirst()) {
            try {
                if (detailCustomerName != null) detailCustomerName.setText(billCursor.getString(billCursor.getColumnIndexOrThrow("name")));
                if (detailCustomerPhone != null) detailCustomerPhone.setText(billCursor.getString(billCursor.getColumnIndexOrThrow("phone")));
                if (detailBillId != null) detailBillId.setText(String.format(Locale.getDefault(), "Bill #%d", item.billId));
                if (detailBillDate != null) detailBillDate.setText(formatDialogDateTime(billCursor.getString(billCursor.getColumnIndexOrThrow("bill_date"))));

                double totalAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("total_amount"));
                double debtAmount = billCursor.getDouble(billCursor.getColumnIndexOrThrow("debt_amount"));
                
                int billedAmountIndex = billCursor.getColumnIndex("billed_amount");
                int paidAmountIndex = billCursor.getColumnIndex("paid_amount");
                double billedAmount = billedAmountIndex != -1 ? billCursor.getDouble(billedAmountIndex) : totalAmount;
                double paidAmount = paidAmountIndex != -1 ? billCursor.getDouble(paidAmountIndex) : Math.max(0.0, totalAmount - debtAmount);

                if (detailBilledAmount != null) detailBilledAmount.setText(String.format(Locale.getDefault(), "Billed: %s", currencyFormat.format(billedAmount)));
                if (detailPaidAmount != null) detailPaidAmount.setText(String.format(Locale.getDefault(), "Paid: %s", currencyFormat.format(paidAmount)));
                if (detailDebtStatus != null) {
                    if (debtAmount > 0.001) {
                        detailDebtStatus.setText(String.format(Locale.getDefault(), "Pending Debt: %s", currencyFormat.format(debtAmount)));
                        detailDebtStatus.setTextColor(Color.parseColor("#D32F2F"));
                    } else {
                        detailDebtStatus.setText("Debt: Cleared");
                        detailDebtStatus.setTextColor(Color.parseColor("#4CAF50"));
                    }
                }
                if (detailTotalAmount != null) detailTotalAmount.setText(String.format("Total: %s", currencyFormat.format(totalAmount)));

                // Simple items list
                if (itemsContainer != null) {
                    itemsContainer.removeAllViews();
                    Cursor itemCursor = dbHelper.getItemsForBill(item.billId);
                    if (itemCursor != null && itemCursor.moveToFirst()) {
                        do {
                            String name = itemCursor.getString(itemCursor.getColumnIndexOrThrow("name"));
                            double weight = itemCursor.getDouble(itemCursor.getColumnIndexOrThrow("weight"));
                            TextView tv = new TextView(requireContext());
                            tv.setText(String.format(Locale.getDefault(), "• %s (%.3fg)", name, weight));
                            itemsContainer.addView(tv);
                        } while (itemCursor.moveToNext());
                        itemCursor.close();
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error showing bill details", e);
            } finally {
                billCursor.close();
            }
        }

        dialog.show();
    }

    private String formatDialogDateTime(String dateStr) {
        if (dateStr == null) return "N/A";
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            if (date != null) return outputFormat.format(date);
        } catch (ParseException e) {
            Log.w(TAG, "Could not parse date: " + dateStr);
        }
        return dateStr;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.add(0, 1, 0, "Auto-clear Settings").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0, 2, 1, "Clear All History").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            showAutoClearDialog();
            return true;
        } else if (item.getItemId() == 2) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to delete all notification history?")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        dbHelper.clearAllNotifications();
                        loadData();
                        Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAutoClearDialog() {
        String[] options = {"24 Hours", "7 Days", "15 Days", "90 Days", "Never (Keep Always)"};
        final int[] values = {1, 7, 15, 90, -1};

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int currentDays = prefs.getInt(KEY_AUTO_CLEAR_DAYS, -1);
        int selectedIndex = 4; // Default to Never
        for (int i = 0; i < values.length; i++) {
            if (values[i] == currentDays) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Auto-clear Notification History")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt(KEY_AUTO_CLEAR_DAYS, values[which]).apply();
                    Toast.makeText(requireContext(), "Setting updated", Toast.LENGTH_SHORT).show();
                    performAutoClear();
                    loadData();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
