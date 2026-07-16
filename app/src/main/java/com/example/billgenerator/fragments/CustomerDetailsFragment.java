package com.example.billgenerator.fragments;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.MainActivity;
import com.example.billgenerator.adapters.customer_recycler_adapter;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.databinding.FragmentCustomerDetailsBinding;
import com.example.billgenerator.models.customer_recycler_model; 
import com.example.billgenerator.ui.UiAnimationHelper;

import java.io.IOException;
import java.io.OutputStream;
import java.text.NumberFormat; 
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class CustomerDetailsFragment extends Fragment {

    private enum CustomerBalanceFilter {
        ALL,
        DEBT,
        CREDIT,
        CLEAR
    }

    private FragmentCustomerDetailsBinding binding;
    private customer_recycler_adapter adapter;
    private ArrayList<customer_recycler_model> customerList = new ArrayList<>();
    private databaseSystem dbHelper;
    private static final String TAG = "CustomerDetailsFrag"; 
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private CustomerBalanceFilter selectedBalanceFilter = CustomerBalanceFilter.ALL;
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
        binding = FragmentCustomerDetailsBinding.inflate(inflater, container, false);
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
        Log.d(TAG, "onViewCreated called");

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.customer_details_menu, menu);
                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            searchQuery = query == null ? "" : query;
                            applyCustomerFilters();
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            searchQuery = newText == null ? "" : newText;
                            applyCustomerFilters();
                            return false;
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_generate_pdf) {
                    generateCustomersPdf();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        dbHelper = new databaseSystem(getContext());

        // Customize empty state using helper
        UiAnimationHelper.configureEmptyState(
                binding.emptyViewInclude.getRoot(),
                R.drawable.ic_empty_bills, // Using existing drawable
                "No Customers Found",
                "Try adjusting your search or filters to find what you're looking for.",
                "Add Customer",
                this::showAddCustomerDialog
        );

        setupFilterChips(view);

        // Dynamic column count for tablets
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (isTablet) {
            binding.customerDetailRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        } else {
            binding.customerDetailRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        adapter = new customer_recycler_adapter(requireContext(), customerList, this);
        binding.customerDetailRecyclerView.setAdapter(adapter);

        loadCustomersFromDB();
        openCustomerFromNotificationIfRequested();

        binding.fabAddCustomer.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked - showing Add Customer dialog");
            showAddCustomerDialog();
        });
        Log.d(TAG, "View setup complete");
    }

    private void setupFilterChips(View view) {
        binding.customerFilterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                selectedBalanceFilter = CustomerBalanceFilter.ALL;
            } else {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chip_customer_debt) {
                    selectedBalanceFilter = CustomerBalanceFilter.DEBT;
                } else if (checkedId == R.id.chip_customer_credit) {
                    selectedBalanceFilter = CustomerBalanceFilter.CREDIT;
                } else if (checkedId == R.id.chip_customer_clear) {
                    selectedBalanceFilter = CustomerBalanceFilter.CLEAR;
                } else {
                    selectedBalanceFilter = CustomerBalanceFilter.ALL;
                }
            }
            applyCustomerFilters();
        });
    }

    private void loadCustomersFromDB() {
        Log.d(TAG, "Loading customers from DB...");
        customerList.clear();
        double totalDebt = 0;
        Cursor cursor = dbHelper.fetchCustomers();
        if (cursor != null) {
            Log.d(TAG, "Cursor has " + cursor.getCount() + " rows.");
            try {
                int idIndex = cursor.getColumnIndexOrThrow("id");
                int nameIndex = cursor.getColumnIndexOrThrow("name");
                int phoneIndex = cursor.getColumnIndexOrThrow("phone");
                int villageIndex = cursor.getColumnIndexOrThrow("village");
                int debtIndex = cursor.getColumnIndexOrThrow("debt");

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    String village = cursor.getString(villageIndex);
                    double debt = cursor.getDouble(debtIndex);
                    
                    totalDebt += debt;
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

        if (binding != null) {
            binding.customerTotalCount.setText(String.valueOf(customerList.size()));
            binding.customerTotalDebt.setText(currencyFormat.format(totalDebt));
        }

        applyCustomerFilters();
    }

    private void applyCustomerFilters() {
        if (binding == null) return;

        ArrayList<customer_recycler_model> filtered = new ArrayList<>();
        String search = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.getDefault());
        double totalDebt = 0;

        for (customer_recycler_model customer : customerList) {
            boolean matchesSearch = search.isEmpty()
                    || customer.name.toLowerCase(Locale.getDefault()).contains(search)
                    || customer.phone.toLowerCase(Locale.getDefault()).contains(search)
                    || customer.village.toLowerCase(Locale.getDefault()).contains(search);

            if (!matchesSearch) {
                continue;
            }

            boolean matchesBalance;
            switch (selectedBalanceFilter) {
                case DEBT:
                    matchesBalance = customer.debt > 0.001;
                    break;
                case CREDIT:
                    matchesBalance = customer.debt < -0.001;
                    break;
                case CLEAR:
                    matchesBalance = Math.abs(customer.debt) <= 0.001;
                    break;
                case ALL:
                default:
                    matchesBalance = true;
                    break;
            }

            if (matchesBalance) {
                filtered.add(customer);
                if (customer.debt > 0) totalDebt += customer.debt;
            }
        }

        binding.customerTotalCount.setText(String.valueOf(filtered.size()));
        binding.customerTotalDebt.setText(currencyFormat.format(totalDebt));

        if (filtered.isEmpty()) {
            binding.customerDetailRecyclerView.setVisibility(View.GONE);
            binding.emptyViewInclude.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.customerDetailRecyclerView.setVisibility(View.VISIBLE);
            binding.emptyViewInclude.getRoot().setVisibility(View.GONE);
        }

        if (adapter != null) {
            adapter.updateList(filtered);
            Log.d(TAG, "Adapter notified of data change. Filtered count: " + filtered.size());
        }
    }

    private void openCustomerFromNotificationIfRequested() {
        Intent intent = requireActivity().getIntent();
        if (intent == null) {
            return;
        }

        int requestedCustomerId = intent.getIntExtra(MainActivity.EXTRA_OPEN_CUSTOMER_ID, -1);
        if (requestedCustomerId <= 0) {
            return;
        }

        intent.removeExtra(MainActivity.EXTRA_OPEN_CUSTOMER_ID);
        intent.removeExtra(MainActivity.EXTRA_OPEN_BILL_ID);
        intent.removeExtra(MainActivity.EXTRA_OPEN_DESTINATION);

        for (customer_recycler_model customer : customerList) {
            if (customer.id == requestedCustomerId) {
                showEditCustomerDialog(customer);
                return;
            }
        }

        Toast.makeText(getContext(), "Customer record not found", Toast.LENGTH_SHORT).show();
    }

    public void showTransactionTimeline(customer_recycler_model model) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        TextView title = new TextView(requireContext());
        title.setText("Timeline: " + model.name);
        title.setTextSize(20f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        RecyclerView rv = new RecyclerView(requireContext());
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        ArrayList<String> timelineData = new ArrayList<>();
        Cursor cursor = dbHelper.fetchAllTransactionsForCustomer(model.id);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String type = cursor.getString(0);
                String date = cursor.getString(2);
                double amt = cursor.getDouble(3);
                String note = cursor.getString(6);
                timelineData.add(String.format("[%s] %s: ₹%.2f %s", date, type.toUpperCase(), amt, note));
            }
            cursor.close();
        }

        rv.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
                TextView tv = new TextView(p.getContext());
                tv.setPadding(16, 16, 16, 16);
                return new RecyclerView.ViewHolder(tv) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ((TextView)h.itemView).setText(timelineData.get(pos));
            }
            @Override public int getItemCount() { return timelineData.size(); }
        });

        layout.addView(rv);
        dialog.setContentView(layout);
        dialog.show();
    }

    private void showAddCustomerDialog() {
        Log.d(TAG, "Showing Add Customer dialog.");
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog);

        TextView titleTextView = dialog.findViewById(R.id.dialog_title);
        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        EditText editDebt = dialog.findViewById(R.id.edit_customer_debt);
        Button saveButton = dialog.findViewById(R.id.save_button);

        if (titleTextView != null) titleTextView.setText("Add New Customer");
        else dialog.setTitle("Add New Customer");

        editDebt.setText("0.0");
        editPhone.setEnabled(true);
        editPhone.setFocusableInTouchMode(true);
        editPhone.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.primary_text_light));

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();
            String village = editVillage.getText().toString().trim();
            String debtStr = editDebt.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || village.isEmpty()) {
                Toast.makeText(getContext(), "Name, Phone, and Village are required", Toast.LENGTH_SHORT).show();
                return;
            }

            Cursor existing = null;
            try {
                existing = dbHelper.getCustomerByPhone(phone);
                if (existing != null && existing.getCount() > 0) {
                    Log.w(TAG, "Attempted to add customer with existing phone: " + phone);
                    Toast.makeText(getContext(), "Phone number already. Cannot add duplicate.", Toast.LENGTH_LONG).show();
                    return;
                }
            } finally {
                if (existing != null) {
                    existing.close();
                }
            }

            double initialDebt = 0.0;
            if (!debtStr.isEmpty()) {
                try {
                    initialDebt = Double.parseDouble(debtStr);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid initial debt format entered: " + debtStr + ". Defaulting to 0.");
                }
            }

            Log.d(TAG, "Attempting to insert new customer: " + name);
            long result = dbHelper.insertCustomer(name, phone, village);

            if (result != -1) {
                if (Math.abs(initialDebt) > 0.001) {
                    dbHelper.updateCustomerDebt(result, initialDebt);
                    Log.d(TAG, "Set initial debt for new customer ID " + result + " to " + initialDebt);
                }
                Toast.makeText(getContext(), "Customer Saved", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB();
            } else {
                Log.e(TAG, "Error saving new customer (insert returned -1 despite check): " + name);
                Toast.makeText(getContext(), "Error saving customer. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    public void showEditCustomerDialog(final customer_recycler_model customerToEdit) {
        Log.d(TAG, "Showing Edit Customer dialog for: " + customerToEdit.name);
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_customer_dialog);

        TextView titleTextView = dialog.findViewById(R.id.dialog_title);
        EditText editName = dialog.findViewById(R.id.edit_customer_name);
        EditText editPhone = dialog.findViewById(R.id.edit_customer_phone);
        EditText editVillage = dialog.findViewById(R.id.edit_customer_village);
        EditText editDebt = dialog.findViewById(R.id.edit_customer_debt);
        Button saveButton = dialog.findViewById(R.id.save_button);

        if (titleTextView != null) titleTextView.setText("Edit Customer");
        else dialog.setTitle("Edit Customer");

        editName.setText(customerToEdit.name);
        editPhone.setText(customerToEdit.phone);
        editPhone.setEnabled(false);
        editPhone.setFocusable(false);
        editPhone.setTextColor(Color.GRAY);
        editVillage.setText(customerToEdit.village);
        editDebt.setText(String.format(Locale.US, "%.2f", customerToEdit.debt));

        saveButton.setText("Update Customer");
        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String village = editVillage.getText().toString().trim();
            String debtStr = editDebt.getText().toString().trim();

            if (name.isEmpty() || village.isEmpty() || debtStr.isEmpty()) {
                Toast.makeText(getContext(), "Name, Village, and Debt cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double debt;
            try {
                debt = Double.parseDouble(debtStr.replace(',', '.'));
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid debt format entered during edit: " + debtStr);
                Toast.makeText(getContext(), "Invalid Debt amount format", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Attempting to update customer ID " + customerToEdit.id);
            int rowsAffected = dbHelper.updateCustomer(customerToEdit.id, name, village, debt);

            if (rowsAffected > 0) {
                Log.i(TAG, "Customer ID " + customerToEdit.id + " updated successfully.");
                Toast.makeText(getContext(), "Customer Updated", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadCustomersFromDB();
            } else {
                Log.e(TAG, "Error updating customer ID " + customerToEdit.id);
                Toast.makeText(getContext(), "Error updating customer", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }



    private void generateCustomersPdf() {
        Log.d(TAG, "generateCustomersPdf called");
        if (customerList.isEmpty()) {
            Toast.makeText(getContext(), "No customers to generate PDF.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 842;
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

        paint.setTextSize(11f); paint.setFakeBoldText(true);
        canvas.drawText("Name", x, y, paint);
        canvas.drawText("Phone", x + 180, y, paint);
        canvas.drawText("Village", x + 310, y, paint);
        canvas.drawText("Debt (₹)", x + 480, y, paint);
        canvas.drawText("Bill IDs", x + 600, y, paint);
        y += 20;
        canvas.drawLine(x, y-8, pageWidth - x, y-8, paint);
        paint.setFakeBoldText(false); paint.setTextSize(10f);


        for (customer_recycler_model customer : customerList) {
            if (y > pageHeight - marginBottom) {
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
                paint.setTextSize(11f); paint.setFakeBoldText(true);
                canvas.drawText("Name", x, y, paint); canvas.drawText("Phone", x + 180, y, paint); canvas.drawText("Village", x + 310, y, paint);
                canvas.drawText("Debt (₹)", x + 480, y, paint); canvas.drawText("Bill IDs", x + 600, y, paint);
                y += 20; canvas.drawLine(x, y-8, pageWidth - x, y-8, paint);
                paint.setFakeBoldText(false); paint.setTextSize(10f);
            }

            String billIds = dbHelper.getBillIdsForCustomer(customer.id);

            String nameToDraw = customer.name.length() > 25 ? customer.name.substring(0, 22)+"..." : customer.name;
            canvas.drawText(nameToDraw, x, y, paint);
            canvas.drawText(customer.phone, x + 180, y, paint);
            String villageToDraw = customer.village.length() > 25 ? customer.village.substring(0, 22)+"..." : customer.village;
            canvas.drawText(villageToDraw, x + 310, y, paint);

            if (customer.debt > 0.001) {
                paint.setColor(Color.RED);
            } else {
                paint.setColor(Color.BLACK);
            }
            canvas.drawText(currencyFormat.format(customer.debt), x + 480, y, paint);
            paint.setColor(Color.BLACK);

            String billIdsToDraw = billIds.isEmpty() ? "N/A" : (billIds.length() > 25 ? billIds.substring(0, 22)+"..." : billIds);
            canvas.drawText(billIdsToDraw, x + 600, y, paint);
            y += lineSpacing;
        }
        document.finishPage(page);

        savePdfDocument(document, "Customer_Report");
        Log.i(TAG, "Customer PDF generation complete.");
    }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = requireActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                Log.w(TAG, "MediaStore API level lower than Q, saving PDF might require explicit permission.");
                Toast.makeText(getContext(), "Saving PDF might require storage permission on older Android versions.", Toast.LENGTH_LONG).show();
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
            if (uri != null) {
                try { requireActivity().getContentResolver().delete(uri, null, null); } catch (Exception deleteEx) { Log.e(TAG, "Error deleting partial PDF: " + deleteEx.getMessage());}
            }
        } finally {
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
        if (dbHelper != null) {
            loadCustomersFromDB();
        }
    }
}