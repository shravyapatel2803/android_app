package com.example.billgenerator.adapters;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.models.DebtCustomerItem;
import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Locale;

public class DebtCustomerAdapter extends RecyclerView.Adapter<DebtCustomerAdapter.ViewHolder> {

    public interface OnDebtCustomerActionListener {
        void onViewHistory(DebtCustomerItem item);
    }

    private final Context context;
    private final ArrayList<DebtCustomerItem> items;
    private final OnDebtCustomerActionListener listener;

    public DebtCustomerAdapter(Context context, ArrayList<DebtCustomerItem> items, OnDebtCustomerActionListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_debt_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DebtCustomerItem item = items.get(position);
        holder.name.setText(safe(item.name));
        holder.contact.setText(buildContactLine(item.phone, item.village));
        if (item.totalDebt >= 0) {
            holder.totalDebt.setText(String.format(Locale.getDefault(), "Total debt: Rs %.2f", item.totalDebt));
            holder.totalDebt.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
        } else {
            holder.totalDebt.setText(String.format(Locale.getDefault(), "Available credit: Rs %.2f", Math.abs(item.totalDebt)));
            holder.totalDebt.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
        }
        holder.activeDebt.setText(String.format(Locale.getDefault(), "Open bill debt: Rs %.2f", item.activeBillDebt));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            holder.dueDate.setText(buildDueDateLabel(item.nearestDueDate));
        }
        holder.lastBill.setText("Last bill: " + safe(item.lastBillDate));

        holder.callButton.setOnClickListener(v -> openDialer(item.phone));
        holder.whatsAppButton.setOnClickListener(v -> openWhatsApp(item));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewHistory(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String buildContactLine(String phone, String village) {
        String safePhone = safe(phone);
        String safeVillage = safe(village);
        if ("-".equals(safePhone) && "-".equals(safeVillage)) {
            return "Contact not available";
        }
        if ("-".equals(safeVillage)) {
            return safePhone;
        }
        if ("-".equals(safePhone)) {
            return safeVillage;
        }
        return safePhone + " | " + safeVillage;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String buildDueDateLabel(String dueDate) {
        String safeDueDate = safe(dueDate);
        if ("-".equals(safeDueDate)) {
            return "Due date: -";
        }

        try {
            LocalDate due = LocalDate.parse(safeDueDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate today = LocalDate.now();
            if (due.isBefore(today)) {
                return "Due: " + safeDueDate + " (Overdue)";
            }
            if (due.isEqual(today)) {
                return "Due: " + safeDueDate + " (Today)";
            }
            return "Due: " + safeDueDate;
        } catch (DateTimeParseException ex) {
            return "Due: " + safeDueDate;
        }
    }

    private void openDialer(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        context.startActivity(intent);
    }

    private void openWhatsApp(DebtCustomerItem item) {
        String phone = item.phone == null ? "" : item.phone.replaceAll("[^0-9]", "");
        if (phone.isEmpty()) {
            Toast.makeText(context, "Phone number not available", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences("bill_generator_prefs", Context.MODE_PRIVATE);
        String template = prefs.getString("debt_whatsapp_template", "Hello {name}, this is a reminder for your pending debt of Rs {amount}. Please clear it by {due_date}.");
        String dueDate = safe(item.nearestDueDate);
        String message = template
                .replace("{name}", safe(item.name))
                .replace("{amount}", String.format(Locale.getDefault(), "%.2f", item.totalDebt))
                .replace("{due_date}", dueDate);
        String url = "https://wa.me/" + phone + "?text=" + Uri.encode(message);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            context.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(context, "Unable to open WhatsApp", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView contact;
        TextView totalDebt;
        TextView activeDebt;
        TextView dueDate;
        TextView lastBill;
        MaterialButton callButton;
        MaterialButton whatsAppButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.debt_item_name);
            contact = itemView.findViewById(R.id.debt_item_contact);
            totalDebt = itemView.findViewById(R.id.debt_item_total_debt);
            activeDebt = itemView.findViewById(R.id.debt_item_active_debt);
            dueDate = itemView.findViewById(R.id.debt_item_due_date);
            lastBill = itemView.findViewById(R.id.debt_item_last_bill);
            callButton = itemView.findViewById(R.id.debt_item_call_button);
            whatsAppButton = itemView.findViewById(R.id.debt_item_whatsapp_button);
        }
    }
}
