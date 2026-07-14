package com.example.billgenerator;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.billgenerator.database.databaseSystem;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ManualDebtUpdateActivity extends AppCompatActivity {

    public static final String EXTRA_CUSTOMER_ID = "extra_customer_id";
    public static final String EXTRA_CUSTOMER_NAME = "extra_customer_name";

    private long customerId;
    private String customerName;
    private databaseSystem dbHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_debt_update);

        customerId = getIntent().getLongExtra(EXTRA_CUSTOMER_ID, -1);
        customerName = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        dbHelper = new databaseSystem(this);

        Toolbar toolbar = findViewById(R.id.manual_debt_toolbar);
        toolbar.setTitle("Add Old Debt Entry");
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView customerNameText = findViewById(R.id.manual_debt_customer_name);
        customerNameText.setText((customerName == null || customerName.trim().isEmpty())
                ? "Customer #" + customerId
                : customerName);

        TextInputEditText entryDateEditText = findViewById(R.id.manual_debt_entry_date);
        TextInputEditText amountEditText = findViewById(R.id.manual_debt_amount);
        TextInputEditText dueDateEditText = findViewById(R.id.manual_debt_due_date);
        TextInputEditText noteEditText = findViewById(R.id.manual_debt_note);
        RadioGroup changeTypeGroup = findViewById(R.id.manual_debt_change_type);
        MaterialButton saveButton = findViewById(R.id.manual_debt_save_button);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        entryDateEditText.setText(today);
        entryDateEditText.setOnClickListener(v -> showDatePicker(entryDateEditText));

        saveButton.setOnClickListener(v -> {
            if (customerId <= 0) {
                Toast.makeText(this, "Invalid customer", Toast.LENGTH_SHORT).show();
                return;
            }

            String amountText = valueOf(amountEditText);
            if (TextUtils.isEmpty(amountText)) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double rawAmount;
            try {
                rawAmount = Double.parseDouble(amountText);
            } catch (NumberFormatException ex) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }
            if (rawAmount <= 0) {
                Toast.makeText(this, "Amount should be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            String entryDate = valueOf(entryDateEditText);
            if (!isIsoDate(entryDate)) {
                Toast.makeText(this, "Entry date must be yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                return;
            }

            String dueDate = valueOf(dueDateEditText);
            if (!TextUtils.isEmpty(dueDate) && !isIsoDate(dueDate)) {
                Toast.makeText(this, "Due date must be yyyy-MM-dd", Toast.LENGTH_SHORT).show();
                return;
            }

            double signedChange = changeTypeGroup.getCheckedRadioButtonId() == R.id.manual_debt_reduce_radio
                    ? -rawAmount
                    : rawAmount;

            int debtUpdated = dbHelper.updateCustomerDebt(customerId, signedChange);
            if (debtUpdated <= 0) {
                Toast.makeText(this, "Failed to update customer debt", Toast.LENGTH_SHORT).show();
                return;
            }

            double newBalance = dbHelper.getCustomerDebt(customerId);
            String userNote = valueOf(noteEditText);
            String finalNote = TextUtils.isEmpty(userNote)
                    ? "Manual entry imported from old hardcopy"
                    : "Manual entry imported from old hardcopy: " + userNote;

            String createdAt = entryDate + " 12:00:00";
            long rowId = dbHelper.insertDebtUpdateWithDate(
                    customerId,
                    -1,
                    signedChange,
                    newBalance,
                    0.0,
                    0.0,
                    TextUtils.isEmpty(dueDate) ? null : dueDate,
                    finalNote,
                    createdAt
            );

            if (rowId <= 0) {
                dbHelper.updateCustomerDebt(customerId, -signedChange);
                Toast.makeText(this, "Failed to save manual debt entry", Toast.LENGTH_SHORT).show();
                return;
            }

            setResult(RESULT_OK);
            Toast.makeText(this, "Manual debt entry saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showDatePicker(TextInputEditText targetView) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selected = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    targetView.setText(selected);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private String valueOf(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private boolean isIsoDate(String value) {
        return !TextUtils.isEmpty(value) && value.matches("\\d{4}-\\d{2}-\\d{2}");
    }
}
