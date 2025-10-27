package com.example.billgenerator.fragments;

import android.app.Dialog;
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
import android.text.TextUtils; // Import TextUtils
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText; // Ensure EditText is imported
import android.widget.TextView; // Ensure TextView is imported
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.adapters.customer_recycler_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.models.customer_recycler_model; // Ensure model is imported
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat; // For currency formatting
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


public class CustomerDetailsFragment extends Fragment {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private customer_recycler_adapter adapter;
    private ArrayList<customer_recycler_model> customerList = new ArrayList<>();
    private databaseSystem dbHelper;
    private static final String TAG = "CustomerDetailsFrag"; // For logging
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN")); // Currency formatter


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // For PDF menu
        Log.d(TAG, "onCreate called");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        // Inflate the correct layout that includes the FAB
        return inflater.inflate(R.layout.activity_customer_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated called");

        dbHelper = new databaseSystem(getContext());
        recyclerView = view.findViewById(R.id.customer_detail_recyclerView);
        fab = view.findViewById(R.id.fab_add_customer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // <-- Pass 'this' (the fragment) to the adapter constructor -->
        adapter = new customer_recycler_adapter(requireContext(), customerList, this);
        recyclerView.setAdapter(adapter);

        loadCustomersFromDB();

        fab.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked - showing Add Customer dialog");
            showAddCustomerDialog();
        });
        Log.d(TAG, "View setup complete");
    }

    // Loads customer data including debt from the database
    private void loadCustomersFromDB() {
        Log.d(TAG, "Loading customers from DB...");
        customerList.clear();
        Cursor cursor = dbHelper.fetchCustomers(); // fetchCustomers now includes debt
        if (cursor != null) {
            Log.d(TAG, "Cursor has " + cursor.getCount() + " rows.");
            try {
                int idIndex = cursor.getColumnIndexOrThrow("id");
                int nameIndex = cursor.getColumnIndexOrThrow("name");
                int phoneIndex = cursor.getColumnIndexOrThrow("phone");
                int villageIndex = cursor.getColumnIndexOrThrow("village");
                int debtIndex = cursor.getColumnIndexOrThrow("debt"); // Get debt index

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    String village = cursor.getString(villageIndex);
                    double debt = cursor.getDouble(debtIndex); // Read debt
                    // Use updated constructor
                    customerList.add(new customer_recycler_model(id, name, phone, village, debt));
                    Log.v(TAG, "Loaded: ID=" + id + ", Name=" + name + ", Debt=" + debt);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading customer data from cursor: " + e.getMessage());
            } finally {
                cursor.close();
            }
        } else {
            Log.w(TAG, "fetchCustomers returned a null cursor.");
        }
        // Update the adapter with the new list
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            Log.d(TAG, "Adapter notified of data change. Item count: " + adapter.getItemCount());
        }
    }

    // --- Add Customer Dialog ---
// --- Add Customer Dialog ---
    private void showAddCustomerDialog() {
        Log.d(TAG, "Showing Add Customer dialog.");
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog);

        // Find views in the dialog layout
        TextView titleTextView = dialog.findViewById(R.id.dialog_title); // Make sure this ID exists
        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        EditText editDebt = dialog.findViewById(R.id.edit_customer_debt); // Find the debt field
        Button saveButton = dialog.findViewById(R.id.save_button);

        // Set title for adding
        if (titleTextView != null) titleTextView.setText("Add New Customer");
        else dialog.setTitle("Add New Customer");

        // Debt field might be optional for adding new customer, clear it
        editDebt.setText("0.0"); // Default to 0 debt for new customer
        editPhone.setEnabled(true); // Ensure phone is editable when adding
        editPhone.setFocusableInTouchMode(true); // Make it focusable
        editPhone.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.primary_text_light)); // Reset color

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String village = editVillage.getText().toString().trim();
            String debtStr = editDebt.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || village.isEmpty()) {
                Toast.makeText(getContext(), "Name, Phone, and Village are required", Toast.LENGTH_SHORT).show();
                return;
            }

            // <-- FIXED: Check if phone number already exists BEFORE inserting -->
            Cursor existing = null;
            try {
                existing = dbHelper.getCustomerByPhone(phone);
                if (existing != null && existing.getCount() > 0) {
                    Log.w(TAG, "Attempted to add customer with existing phone: " + phone);
                    Toast.makeText(getContext(), "Phone number already exists. Cannot add duplicate.", Toast.LENGTH_LONG).show();
                    // Optionally, you could offer to edit the existing customer here
                    return; // Stop the save process
                }
            } finally {
                if (existing != null) {
                    existing.close();
                }
            }
            // <-- End of Fixed Check -->


            // Phone number is unique, proceed with insertion
            double initialDebt = 0.0;
            if (!debtStr.isEmpty()) {
                try {
                    initialDebt = Double.parseDouble(debtStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid initial debt format entered: " + debtStr + ". Defaulting to 0.");
                }
            }

            Log.d(TAG, "Attempting to insert new customer: " + name);
            // Insert using the method that initializes debt to 0 by default
            long result = dbHelper.insertCustomer(name, phone, village); // insertCustomer now sets debt to 0

            if (result != -1) { // Check if insert was successful (didn't return -1)
                // If initial debt was provided and is not 0, update it separately
                if (Math.abs(initialDebt) > 0.001) {
                    dbHelper.updateCustomerDebt(result, initialDebt);
                    Log.d(TAG, "Set initial debt for new customer ID " + result + " to " + initialDebt);
                }
                Toast.makeText(getContext(), "Customer Saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB(); // Refresh list
            } else {
                // This might happen if there's another unexpected constraint violation or DB error
                Log.e(TAG, "Error saving new customer (insert returned -1 despite check): " + name);
                Toast.makeText(getContext(), "Error saving customer. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }
    // --- Edit Customer Dialog (Called by Adapter) ---
    public void showEditCustomerDialog(final customer_recycler_model customerToEdit) {
        Log.d(TAG, "Showing Edit Customer dialog for: " + customerToEdit.name);
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog); // Reuse the same layout

        // Find views
        TextView titleTextView = dialog.findViewById(R.id.dialog_title); // Assuming ID exists
        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        EditText editDebt = dialog.findViewById(R.id.edit_customer_debt); // Find debt field
        Button saveButton = dialog.findViewById(R.id.save_button);

        // Set title for editing
        if (titleTextView != null) titleTextView.setText("Edit Customer");
        else dialog.setTitle("Edit Customer");

        // Pre-fill fields
        editName.setText(customerToEdit.name);
        editPhone.setText(customerToEdit.phone);
        editPhone.setEnabled(false); // Do not allow editing phone number (Primary Key/Unique)
        editPhone.setFocusable(false); // Prevent focus
        editPhone.setTextColor(Color.GRAY); // Indicate it's not editable
        editVillage.setText(customerToEdit.village);
        // Display current debt, formatted
        editDebt.setText(String.format(Locale.US, "%.2f", customerToEdit.debt)); // Use Locale.US for decimal point

        saveButton.setText("Update Customer");
        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            // Phone is not edited
            String village = editVillage.getText().toString().trim();
            String debtStr = editDebt.getText().toString().trim();

            if (name.isEmpty() || village.isEmpty() || debtStr.isEmpty()) {
                Toast.makeText(getContext(), "Name, Village, and Debt cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double debt;
            try {
                // Use Locale.US when parsing to ensure correct decimal separator
                debt = Double.parseDouble(debtStr.replace(',', '.')); // Replace comma if necessary, parse as US format
                // Optional: Check if debt makes sense (e.g., not excessively large)
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid debt format entered during edit: " + debtStr);
                Toast.makeText(getContext(), "Invalid Debt amount format", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Attempting to update customer ID " + customerToEdit.id);
            // Use the updateCustomer method that includes debt
            int rowsAffected = dbHelper.updateCustomer(customerToEdit.id, name, village, debt);

            if (rowsAffected > 0) {
                Log.i(TAG, "Customer ID " + customerToEdit.id + " updated successfully.");
                Toast.makeText(getContext(), "Customer Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB(); // Refresh the list to show changes
            } else {
                Log.e(TAG, "Error updating customer ID " + customerToEdit.id);
                Toast.makeText(getContext(), "Error updating customer", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }


    // --- Menu Handling for PDF ---
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.customer_details_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "onCreateOptionsMenu called");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: " + item.getTitle());
        if (item.getItemId() == R.id.action_generate_pdf) {
            generateCustomersPdf(); // Call the PDF generation method
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- PDF Generation Logic ---
    private void generateCustomersPdf() {
        Log.d(TAG, "generateCustomersPdf called");
        if (customerList.isEmpty()) {
            Toast.makeText(getContext(), "No customers to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 842; // A4 landscape
        int pageHeight = 595;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint boldPaint = new Paint();
        boldPaint.setFakeBoldText(true);
        boldPaint.setTextSize(10f);

        int x = 30, y = 50;
        final int lineSpacing = 16;
        final int marginBottom = 40;

        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Customer Report", x, y, titlePaint);
        y += 25;

        paint.setTextSize(9f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        // Draw Headers
        paint.setTextSize(11f); paint.setFakeBoldText(true);
        canvas.drawText("Name", x, y, paint);
        canvas.drawText("Phone", x + 180, y, paint); // Adjusted spacing
        canvas.drawText("Village", x + 310, y, paint);
        canvas.drawText("Debt (₹)", x + 480, y, paint); // Added Debt Header
        canvas.drawText("Bill IDs", x + 600, y, paint); // Shifted Bill IDs
        y += 20;
        canvas.drawLine(x, y-8, pageWidth - x, y-8, paint); // Line below headers
        paint.setFakeBoldText(false); paint.setTextSize(10f);


        for (customer_recycler_model customer : customerList) {
            if (y > pageHeight - marginBottom) { // Check for page break
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50; // Reset Y
                // Redraw headers
                paint.setTextSize(11f); paint.setFakeBoldText(true);
                canvas.drawText("Name", x, y, paint); canvas.drawText("Phone", x + 180, y, paint); canvas.drawText("Village", x + 310, y, paint);
                canvas.drawText("Debt (₹)", x + 480, y, paint); canvas.drawText("Bill IDs", x + 600, y, paint);
                y += 20; canvas.drawLine(x, y-8, pageWidth - x, y-8, paint);
                paint.setFakeBoldText(false); paint.setTextSize(10f);
            }

            // Fetch the bill IDs for this customer
            String billIds = dbHelper.getBillIdsForCustomer(customer.id);

            // Draw customer data
            String nameToDraw = customer.name.length() > 25 ? customer.name.substring(0, 22)+"..." : customer.name; // Truncate name slightly more
            canvas.drawText(nameToDraw, x, y, paint);
            canvas.drawText(customer.phone, x + 180, y, paint);
            String villageToDraw = customer.village.length() > 25 ? customer.village.substring(0, 22)+"..." : customer.village; // Truncate village
            canvas.drawText(villageToDraw, x + 310, y, paint);

            // Format and draw debt (color based on value)
            if (customer.debt > 0.001) {
                paint.setColor(Color.RED);
            } else {
                paint.setColor(Color.BLACK); // Or Green for credit
            }
            canvas.drawText(currencyFormat.format(customer.debt), x + 480, y, paint);
            paint.setColor(Color.BLACK); // Reset color

            // Handle potentially long list of bill IDs
            String billIdsToDraw = billIds.isEmpty() ? "N/A" : (billIds.length() > 25 ? billIds.substring(0, 22)+"..." : billIds); // Shorter truncation needed for landscape
            canvas.drawText(billIdsToDraw, x + 600, y, paint);
            y += lineSpacing;
        }
        document.finishPage(page);

        // Save the File using MediaStore
        savePdfDocument(document, "Customer_Report");
        Log.i(TAG, "Customer PDF generation complete.");
    }

    // --- HELPER METHOD TO SAVE PDF using MediaStore ---
    private void savePdfDocument(PdfDocument document, String baseFileName) {
        Log.d(TAG, "Saving PDF: " + baseFileName);
        // Needs context, use requireActivity().getContentResolver()
        ContentValues values = new ContentValues();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = baseFileName + "_" + timestamp + ".pdf";

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = null;
        OutputStream outputStream = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = requireActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                Log.w(TAG, "MediaStore API level lower than Q, saving PDF might require explicit permission.");
                Toast.makeText(getContext(), "Saving PDF might require storage permission on older Android versions.", Toast.LENGTH_LONG).show();
                // Add logic here to request WRITE_EXTERNAL_STORAGE if needed for API < 29
                // For now, we'll just stop if below Q
                document.close();
                return;
            }

            if (uri == null) { throw new IOException("Failed to create new MediaStore record."); }

            outputStream = requireActivity().getContentResolver().openOutputStream(uri);
            if (outputStream == null) { throw new IOException("Failed to get output stream for URI: " + uri); }

            document.writeTo(outputStream);
            Log.i(TAG, "PDF saved successfully: " + fileName);
            Toast.makeText(getContext(), fileName + " saved to Downloads!", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Clean up partially created file if possible
            if (uri != null) {
                try { requireActivity().getContentResolver().delete(uri, null, null); } catch (Exception deleteEx) { Log.e(TAG, "Error deleting partial PDF: " + deleteEx.getMessage());}
            }
        } finally {
            // Always close the output stream and the document
            if (outputStream != null) {
                try { outputStream.close();} catch (IOException e) { Log.e(TAG, "Error closing output stream: " + e.getMessage());}
            }
            document.close();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        // Reload data to reflect any changes made elsewhere (e.g., debt added from bill generation)
        if (dbHelper != null) {
            loadCustomersFromDB();
        }
    }
}