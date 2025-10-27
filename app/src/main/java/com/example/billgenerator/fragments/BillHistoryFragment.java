package com.example.billgenerator.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.BillHistoryAdapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.BillHistoryModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class BillHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private BillHistoryAdapter adapter;
    private ArrayList<BillHistoryModel> allBillsList = new ArrayList<>();
    private ArrayList<BillHistoryModel> filteredList = new ArrayList<>();
    private databaseSystem dbHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // Important for fragments to handle menu items
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_bill_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new databaseSystem(getContext());
        recyclerView = view.findViewById(R.id.bill_history_recyclerView);
        setupRecyclerView();
        loadBillHistory();
    }

    private void setupRecyclerView() {
        adapter = new BillHistoryAdapter(requireContext(), filteredList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadBillHistory() {
        allBillsList.clear();
        Cursor cursor = dbHelper.fetchBillHistory();
        if (cursor != null && cursor.moveToFirst()) {
            int billIdCol = cursor.getColumnIndex("id");
            int customerNameCol = cursor.getColumnIndex("name");
            int dateCol = cursor.getColumnIndex("bill_date");
            int totalCol = cursor.getColumnIndex("total_amount");

            do {
                int billId = cursor.getInt(billIdCol);
                String customerName = cursor.getString(customerNameCol);
                String rawDate = cursor.getString(dateCol);
                double totalAmount = cursor.getDouble(totalCol);
                String formattedDate = formatDate(rawDate);
                allBillsList.add(new BillHistoryModel(billId, customerName, formattedDate, totalAmount));
            } while (cursor.moveToNext());
            cursor.close();
        }
        filter(""); // Load all data initially
    }

    private String formatDate(String dateStr) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        try {
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateStr;
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.bill_history_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search by name or bill ID...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(allBillsList);
        } else {
            String searchText = text.toLowerCase(Locale.getDefault());
            for (BillHistoryModel bill : allBillsList) {
                if (bill.customerName.toLowerCase(Locale.getDefault()).contains(searchText) ||
                        String.valueOf(bill.billId).contains(searchText)) {
                    filteredList.add(bill);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    // Refresh data when user returns to this tab
    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadBillHistory();
        }
    }
}