package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.customer_recycler_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.customer_recycler_model;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class CustomerDetailsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private customer_recycler_adapter adapter;
    private ArrayList<customer_recycler_model> customerList = new ArrayList<>();
    private databaseSystem dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_customer_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new databaseSystem(getContext());
        recyclerView = view.findViewById(R.id.customer_detail_recyclerView);
        fab = view.findViewById(R.id.fab_add_customer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new customer_recycler_adapter(requireContext(), customerList);
        recyclerView.setAdapter(adapter);

        loadCustomersFromDB();

        fab.setOnClickListener(v -> showAddCustomerDialog());
    }

    private void loadCustomersFromDB() {
        customerList.clear();
        Cursor cursor = dbHelper.fetchCustomers();
        if (cursor != null && cursor.getCount() > 0) {
            int idIndex = cursor.getColumnIndex("id");
            int nameIndex = cursor.getColumnIndex("name");
            int phoneIndex = cursor.getColumnIndex("phone");
            int villageIndex = cursor.getColumnIndex("village");

            while (cursor.moveToNext()) {
                if (idIndex != -1 && nameIndex != -1 && phoneIndex != -1 && villageIndex != -1) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    String village = cursor.getString(villageIndex);
                    customerList.add(new customer_recycler_model(id, name, phone, village));
                }
            }
            cursor.close();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddCustomerDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog);

        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        Button saveButton = dialog.findViewById(R.id.save_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String village = editVillage.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || village.isEmpty()) {
                Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.insertCustomer(name, phone, village);
                Toast.makeText(getContext(), "Customer Saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB();
            }
        });
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadCustomersFromDB();
        }
    }
}