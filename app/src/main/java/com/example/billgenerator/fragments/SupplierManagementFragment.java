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
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.databinding.FragmentSupplierManagementBinding;
import com.example.billgenerator.models.Supplier;

import java.util.ArrayList;

public class SupplierManagementFragment extends Fragment {

    private FragmentSupplierManagementBinding binding;
    private databaseSystem dbHelper;
    private ArrayList<Supplier> supplierList = new ArrayList<>();
    // private SupplierAdapter adapter; // To be implemented

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSupplierManagementBinding.inflate(inflater, container, false);
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

        binding.supplierRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        // adapter = new SupplierAdapter(supplierList);
        // binding.supplierRecyclerView.setAdapter(adapter);

        binding.fabAddSupplier.setOnClickListener(v -> showAddSupplierDialog());
        loadSuppliers();
    }

    private void loadSuppliers() {
        supplierList.clear();
        Cursor cursor = dbHelper.fetchSuppliers();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                supplierList.add(new Supplier(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        cursor.getString(cursor.getColumnIndexOrThrow("address"))
                ));
            } while (cursor.moveToNext());
            cursor.close();
        }
        // adapter.notifyDataSetChanged();
    }

    private void showAddSupplierDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_supplier); // To be created

        final EditText editName = dialog.findViewById(R.id.edit_supplier_name);
        final EditText editPhone = dialog.findViewById(R.id.edit_supplier_phone);
        final EditText editAddress = dialog.findViewById(R.id.edit_supplier_address);
        Button saveButton = dialog.findViewById(R.id.save_supplier_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String address = editAddress.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                return;
            }

            dbHelper.insertSupplier(name, phone, address);
            Toast.makeText(getContext(), "Supplier added", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            loadSuppliers();
        });

        dialog.show();
    }
}
