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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.example.billgenerator.adapters.item_recycler_adapter_stocks;
import com.example.billgenerator.models.item_recycler_model_stocks;

// <-- Added necessary imports -->
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.content.ContentValues;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.appcompat.widget.SearchView;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class StockManagementFragment extends Fragment {

    private RecyclerView recyclerView;
    private item_recycler_adapter_stocks adapter;
    // Master list of all items
    private ArrayList<item_recycler_model_stocks> itemList = new ArrayList<>();
    // List displayed by the adapter (filtered)
    private ArrayList<item_recycler_model_stocks> filteredItemList = new ArrayList<>();
    private databaseSystem dbHelper;
    private FloatingActionButton fab;


    // <-- Added: Tell the fragment it has menu items -->
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the correct layout for stock management
        return inflater.inflate(R.layout.activity_maintain, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new databaseSystem(getContext());
        // IDs were corrected previously, ensure they match activity_maintain.xml
        recyclerView = view.findViewById(R.id.stock_item_recyclerView);
        fab = view.findViewById(R.id.fab_add_stock_item);

        setupRecyclerView();
        loadItemsFromDB(); // Load initial data

        fab.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupRecyclerView() {
        // <-- IMPORTANT: Use the filteredList for the adapter -->
        adapter = new item_recycler_adapter_stocks(requireContext(), filteredItemList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void loadItemsFromDB() {
        itemList.clear(); // Clear the master list before loading
        Cursor cursor = dbHelper.fetchItems();
        if (cursor != null) {
            // Get column indices once
            int idCol = cursor.getColumnIndexOrThrow("id");
            int nameCol = cursor.getColumnIndexOrThrow("name");
            int weightCol = cursor.getColumnIndexOrThrow("weight");
            int typeCol = cursor.getColumnIndexOrThrow("type");
            int soldCol = cursor.getColumnIndexOrThrow("is_sold");

            while (cursor.moveToNext()) {
                int id = cursor.getInt(idCol);
                String name = cursor.getString(nameCol);
                double weight = cursor.getDouble(weightCol);
                String type = cursor.getString(typeCol);
                boolean isSold = cursor.getInt(soldCol) == 1;

                itemList.add(new item_recycler_model_stocks(id, name, weight, type, isSold));
            }
            cursor.close();
        }
        // <-- Apply filter after loading master list -->
        filter(""); // Initially show all items by applying an empty filter
    }

    // <-- Added: Filter logic -->
    private void filter(String text) {
        filteredItemList.clear(); // Clear the list displayed by the adapter
        String searchText = text.toLowerCase(Locale.getDefault());
        if (text.isEmpty()) {
            filteredItemList.addAll(itemList); // Add all items from master list if search is empty
        } else {
            for (item_recycler_model_stocks item : itemList) { // Iterate master list
                // Check if name or type contains the search text
                if (item.getName().toLowerCase(Locale.getDefault()).contains(searchText) ||
                        item.getType().toLowerCase(Locale.getDefault()).contains(searchText)) {
                    filteredItemList.add(item); // Add matches to the filtered list
                }
            }
        }
        // Notify the adapter that the data (filteredItemList) has changed
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddItemDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_item_dialog);

        final EditText editName = dialog.findViewById(R.id.edit_item_name);
        final EditText editWeight = dialog.findViewById(R.id.edit_item_weight);
        final RadioGroup typeGroup = dialog.findViewById(R.id.radio_group_type);
        Button saveButton = dialog.findViewById(R.id.save_item_button);

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            if (name.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                int selectedTypeId = typeGroup.getCheckedRadioButtonId();
                if (selectedTypeId == -1) { // No radio button selected
                    Toast.makeText(getContext(), "Please select item type (Gold/Silver)", Toast.LENGTH_SHORT).show();
                    return;
                }
                RadioButton selectedType = dialog.findViewById(selectedTypeId);
                String type = selectedType.getText().toString();

                // Insert with isSold = false by default
                dbHelper.insertItem(name, weight, type, false);
                Toast.makeText(getContext(), "Item saved!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                loadItemsFromDB(); // Refresh the master list and re-apply filter
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // <-- Added: Inflate the menu -->
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.maintain_menu, menu); // Use the correct menu for stock

        // --- Setup Search ---
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint("Search by name or type...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    filter(query); // Optionally filter on submit too
                    return false;
                }
                @Override public boolean onQueryTextChange(String newText) {
                    filter(newText); // Filter as user types
                    return true;
                }
            });
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    // <-- Added: Handle menu item clicks -->
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_generate_pdf) {
            generateStockPdf(); // Call the PDF generation method
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // <-- Added: PDF Generation Logic (moved/adapted from Maintain activity) -->
    private void generateStockPdf() {
        // --- IMPORTANT: Use the filteredItemList for the PDF ---
        if (filteredItemList.isEmpty()) {
            Toast.makeText(getContext(), "No items to generate PDF based on current filter.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        int pageWidth = 595;
        int pageHeight = 842; // A4 Portrait
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
        canvas.drawText("Stock Report", x, y, titlePaint);
        y += 25;

        paint.setTextSize(9f);
        String date = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText("Generated on: " + date, x, y, paint);
        y += 30;

        // Draw Headers
        paint.setTextSize(11f); paint.setFakeBoldText(true);
        canvas.drawText("ID", x, y, paint);
        canvas.drawText("Name", x + 40, y, paint);
        canvas.drawText("Weight (g)", x + 250, y, paint); // Adjusted spacing
        canvas.drawText("Type", x + 350, y, paint);
        canvas.drawText("Status", x + 450, y, paint);
        y += 20;
        canvas.drawLine(x, y - 8, pageWidth - x, y - 8, paint); // Line below headers
        paint.setFakeBoldText(false); paint.setTextSize(10f);


        for (item_recycler_model_stocks item : filteredItemList) { // Use filtered list
            if (y > pageHeight - marginBottom) { // Check for page break
                document.finishPage(page);
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50; // Reset Y
                // Redraw headers
                paint.setTextSize(11f); paint.setFakeBoldText(true);
                canvas.drawText("ID", x, y, paint); canvas.drawText("Name", x + 40, y, paint); canvas.drawText("Weight (g)", x + 250, y, paint); canvas.drawText("Type", x + 350, y, paint); canvas.drawText("Status", x + 450, y, paint);
                y += 20; canvas.drawLine(x, y - 8, pageWidth - x, y - 8, paint);
                paint.setFakeBoldText(false); paint.setTextSize(10f);
            }

            canvas.drawText(String.valueOf(item.getId()), x, y, paint);
            // Truncate name if too long
            String nameToDraw = item.getName().length() > 30 ? item.getName().substring(0, 27)+"..." : item.getName();
            canvas.drawText(nameToDraw, x + 40, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.3f", item.getWeight()), x + 250, y, paint);
            canvas.drawText(item.getType(), x + 350, y, paint);
            canvas.drawText(item.isSold() ? "Sold" : "Available", x + 450, y, paint);
            y += lineSpacing;
        }
        document.finishPage(page);

        // Save the File using MediaStore (Using helper method)
        savePdfDocument(document, "Stock_Report");
    }

    // --- HELPER METHOD TO SAVE PDF using MediaStore (Copied from BillHistoryFragment) ---
    private void savePdfDocument(PdfDocument document, String baseFileName) {
        ContentValues values = new ContentValues();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = baseFileName + "_" + timestamp + ".pdf";

        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                uri = requireActivity().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                Toast.makeText(getContext(), "Saving PDF might require storage permission on older Android versions.", Toast.LENGTH_LONG).show();
                document.close();
                return;
            }

            if (uri != null) {
                OutputStream outputStream = requireActivity().getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    document.writeTo(outputStream);
                    outputStream.close();
                    Toast.makeText(getContext(), fileName + " saved to Downloads!", Toast.LENGTH_LONG).show();
                } else { throw new IOException("Failed to get output stream."); }
            } else { throw new IOException("Failed to create new MediaStore record."); }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (dbHelper != null) {
            loadItemsFromDB(); // Reload data and reapply filter
        }
    }
}