package com.example.billgenerator.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.ui.UiAnimationHelper;

import java.util.ArrayList;
import java.util.Locale;

public class WorkerLedgerFragment extends Fragment {

    private RecyclerView recyclerView;
    private databaseSystem dbHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ArrayList<WorkerModel> workerList = new ArrayList<>();
    private WorkerAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_worker_ledger, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dbHelper = new databaseSystem(requireContext());
        recyclerView = view.findViewById(R.id.worker_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new WorkerAdapter();
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.fab_add_worker).setOnClickListener(v -> showAddWorkerDialog());
        loadWorkers();
    }

    private void loadWorkers() {
        databaseSystem.databaseExecutor.execute(() -> {
            Cursor cursor = dbHelper.fetchWorkers();
            ArrayList<WorkerModel> temp = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    temp.add(new WorkerModel(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("gold_balance")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("silver_balance"))
                    ));
                }
                cursor.close();
            }
            mainHandler.post(() -> {
                workerList.clear();
                workerList.addAll(temp);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showAddWorkerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = getLayoutInflater().inflate(R.layout.add_customer_dialog, null); // Reuse customer dialog layout
        ((TextView)v.findViewById(R.id.dialog_title)).setText("Add New Karigar");
        EditText name = v.findViewById(R.id.edit_customer_name);
        EditText phone = v.findViewById(R.id.edit_customer_phone);
        v.findViewById(R.id.edit_customer_village).setVisibility(View.GONE);
        
        builder.setView(v);
        AlertDialog dialog = builder.create();
        v.findViewById(R.id.save_button).setOnClickListener(view -> {
            String n = name.getText().toString().trim();
            String p = phone.getText().toString().trim();
            if (n.isEmpty()) return;
            databaseSystem.databaseExecutor.execute(() -> {
                dbHelper.insertWorker(n, p);
                mainHandler.post(() -> {
                    loadWorkers();
                    dialog.dismiss();
                });
            });
        });
        dialog.show();
    }

    private class WorkerAdapter extends RecyclerView.Adapter<WorkerAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(getContext()).inflate(R.layout.item_debt_customer, p, false));
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            WorkerModel m = workerList.get(pos);
            h.name.setText(m.name);
            h.phone.setText(m.phone);
            h.balance.setText(String.format(Locale.getDefault(), "Gold: %.3f g | Silver: %.3f g", m.gold, m.silver));
            h.itemView.setOnClickListener(v -> showTransactionDialog(m));
        }

        @Override public int getItemCount() { return workerList.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView name, phone, balance;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.debt_item_name);
                phone = v.findViewById(R.id.debt_item_contact);
                balance = v.findViewById(R.id.debt_item_total_debt);
                v.findViewById(R.id.debt_item_active_debt).setVisibility(View.GONE);
                v.findViewById(R.id.debt_item_due_date).setVisibility(View.GONE);
                v.findViewById(R.id.debt_item_last_bill).setVisibility(View.GONE);
                v.findViewById(R.id.debt_item_call_button).setVisibility(View.GONE);
                v.findViewById(R.id.debt_item_whatsapp_button).setVisibility(View.GONE);
            }
        }
    }

    private void showTransactionDialog(WorkerModel worker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View v = getLayoutInflater().inflate(R.layout.dialog_add_expense, null);
        EditText weight = v.findViewById(R.id.expense_amount_input);
        weight.setHint("Weight (grams)");
        EditText note = v.findViewById(R.id.expense_title_input);
        note.setHint("Note (e.g. Chain order)");
        v.findViewById(R.id.expense_category_input).setVisibility(View.GONE);
        
        builder.setView(v);
        AlertDialog dialog = builder.create();
        v.findViewById(R.id.btn_save_expense).setOnClickListener(view -> {
            String wStr = weight.getText().toString().trim();
            String nStr = note.getText().toString().trim();
            if (wStr.isEmpty()) return;
            
            dialog.dismiss();
            new AlertDialog.Builder(requireContext())
                .setTitle("Transaction Type")
                .setMessage("Are you giving metal to worker or receiving finished jewelry?")
                .setPositiveButton("Metal Given", (d, i) -> handleWorkerLog(worker, "GIVEN", wStr, nStr))
                .setNegativeButton("Finished Received", (d, i) -> handleWorkerLog(worker, "RECEIVED", wStr, nStr))
                .show();
        });
        dialog.show();
    }

    private void handleWorkerLog(WorkerModel w, String type, String weightStr, String note) {
        try {
            double weight = Double.parseDouble(weightStr);
            databaseSystem.databaseExecutor.execute(() -> {
                dbHelper.logWorkerTransaction(w.id, type, "GOLD", weight, note); // Defaulting to gold for now
                mainHandler.post(this::loadWorkers);
            });
        } catch (Exception ignored) {}
    }

    private static class WorkerModel {
        int id; String name, phone; double gold, silver;
        WorkerModel(int id, String name, String phone, double gold, double silver) {
            this.id = id; this.name = name; this.phone = phone; this.gold = gold; this.silver = silver;
        }
    }
}