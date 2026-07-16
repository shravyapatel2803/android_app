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
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.Lifecycle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;
import com.example.billgenerator.databinding.FragmentStockManagementBinding;
import com.example.billgenerator.ui.UiAnimationHelper;
import com.google.android.material.textfield.TextInputLayout;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

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
import java.util.List;
import java.util.Date;
import java.util.Locale;

import android.os.Handler;
import android.os.Looper;

public class StockManagementFragment extends Fragment {

    private FragmentStockManagementBinding binding;
    private item_recycler_adapter_stocks adapter;
    // Master list of all items
    private ArrayList<item_recycler_model_stocks> itemList = new ArrayList<>();
    // List displayed by the adapter (filtered)
    private ArrayList<item_recycler_model_stocks> filteredItemList = new ArrayList<>();
    private databaseSystem dbHelper;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String queryFilter = "";
    private int statusFilter = 0; // 0=all, 1=available, 2=sold
    private int typeFilter = 0; // 0=all, 1=gold, 2=silver, 3=other
    private int weightFilter = 0; // 0=all, 1=<5g, 2=5-20g, 3=>20g
    private GmsBarcodeScanner scanner;
    private static final String TAG = "StockManagementFragment";
    private final String[] statusOptions = new String[]{"All", "Available", "Sold"};
    private final String[] typeOptions = new String[]{"All Types", "Gold", "Silver", "Other"};
    private final String[] weightOptions = new String[]{"All Weights", "Light (<5g)", "5-20g", "Heavy (>20g)"};


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build();
        scanner = GmsBarcodeScanning.getClient(requireContext(), options);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentStockManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.maintain_menu, menu);

                MenuItem searchItem = menu.findItem(R.id.action_search);
                SearchView searchView = (SearchView) searchItem.getActionView();
                if (searchView != null) {
                    searchView.setQueryHint("Search by name or type...");
                    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                        @Override public boolean onQueryTextSubmit(String query) {
                            filter(query);
                            return false;
                        }
                        @Override public boolean onQueryTextChange(String newText) {
                            filter(newText);
                            return true;
                        }
                    });
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_generate_pdf) {
                    generateStockPdf();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        dbHelper = new databaseSystem(getContext());
        
        setupRecyclerView();
        setupStockFilters();
        UiAnimationHelper.configureEmptyState(
                binding.stockEmptyState.getRoot(),
                R.drawable.ic_empty_inventory,
                "Inventory is empty",
                "Add your first gold or silver item to start tracking stock.",
                "Add Item",
                this::showAddItemDialog
        );
        loadItemsFromDB(); // Load initial data

        binding.fabAddStockItem.setOnClickListener(v -> showAddItemDialog());
    }

    private void setupRecyclerView() {
        adapter = new item_recycler_adapter_stocks(requireContext(), filteredItemList, this);
        
        // Dynamic column count for tablets
        boolean isTablet = getResources().getConfiguration().smallestScreenWidthDp >= 600;
        if (isTablet) {
            binding.stockItemRecyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(getContext(), 2));
        } else {
            binding.stockItemRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        binding.stockItemRecyclerView.setAdapter(adapter);
        UiAnimationHelper.setupRecyclerViewAnimations(binding.stockItemRecyclerView);
    }

    private void loadItemsFromDB() {
        databaseSystem.databaseExecutor.execute(() -> {
            Cursor cursor = dbHelper.fetchItems();
            ArrayList<item_recycler_model_stocks> loadedItems = new ArrayList<>();
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow("id");
                int nameCol = cursor.getColumnIndexOrThrow("name");
                int weightCol = cursor.getColumnIndexOrThrow("weight");
                int typeCol = cursor.getColumnIndexOrThrow("type");
                int barcodeCol = cursor.getColumnIndexOrThrow("barcode");
                int soldCol = cursor.getColumnIndexOrThrow("is_sold");

                while (cursor.moveToNext()) {
                    loadedItems.add(new item_recycler_model_stocks(
                            cursor.getInt(idCol),
                            cursor.getString(nameCol),
                            cursor.getDouble(weightCol),
                            cursor.getString(typeCol),
                            cursor.getString(barcodeCol),
                            cursor.getInt(soldCol) == 1
                    ));
                }
                cursor.close();
            }

            mainHandler.post(() -> {
                if (binding != null) {
                    itemList.clear();
                    itemList.addAll(loadedItems);
                    filter(queryFilter);
                    refreshStockSummary();
                }
            });
        });
    }

    // <-- Added: Filter logic -->
    private void filter(String text) {
        ArrayList<item_recycler_model_stocks> newFilteredList = new ArrayList<>();
        queryFilter = text == null ? "" : text;
        String searchText = queryFilter.toLowerCase(Locale.getDefault());
        for (item_recycler_model_stocks item : itemList) {
            String itemType = item.getType() == null ? "" : item.getType().toLowerCase(Locale.getDefault());
            double weight = item.getWeight();
            boolean matchesStatus = statusFilter == 0
                    || (statusFilter == 1 && !item.isSold())
                    || (statusFilter == 2 && item.isSold());

            boolean matchesType = typeFilter == 0
                || (typeFilter == 1 && itemType.contains("gold"))
                || (typeFilter == 2 && itemType.contains("silver"))
                || (typeFilter == 3 && !itemType.contains("gold") && !itemType.contains("silver"));

            boolean matchesWeight = weightFilter == 0
                || (weightFilter == 1 && weight < 5.0)
                || (weightFilter == 2 && weight >= 5.0 && weight <= 20.0)
                || (weightFilter == 3 && weight > 20.0);

            boolean matchesSearch = searchText.isEmpty()
                    || item.getName().toLowerCase(Locale.getDefault()).contains(searchText)
                || itemType.contains(searchText);
            if (matchesStatus && matchesType && matchesWeight && matchesSearch) {
                newFilteredList.add(item);
            }
        }

        filteredItemList.clear();
        filteredItemList.addAll(newFilteredList);
        
        if (adapter != null) {
            adapter.notifyDataSetChanged(); // For filter results, whole set usually changes
            binding.stockItemRecyclerView.scheduleLayoutAnimation();
        }
        UiAnimationHelper.setVisible(binding.stockEmptyState.getRoot(), filteredItemList.isEmpty());
    }

    private void setupStockFilters() {
        binding.stockStatusDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, statusOptions));
        binding.stockTypeDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, typeOptions));
        binding.stockWeightDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, weightOptions));

        binding.stockStatusDropdown.setText(statusOptions[statusFilter], false);
        binding.stockTypeDropdown.setText(typeOptions[typeFilter], false);
        binding.stockWeightDropdown.setText(weightOptions[weightFilter], false);

        binding.stockStatusDropdown.setOnItemClickListener((parent, view, position, id) -> {
            statusFilter = position;
            filter(queryFilter);
        });

        binding.stockTypeDropdown.setOnItemClickListener((parent, view, position, id) -> {
            typeFilter = position;
            filter(queryFilter);
        });

        binding.stockWeightDropdown.setOnItemClickListener((parent, view, position, id) -> {
            weightFilter = position;
            filter(queryFilter);
        });
    }

    private void refreshStockSummary() {
        int total = itemList.size();
        int sold = 0;
        for (item_recycler_model_stocks item : itemList) {
            if (item.isSold()) {
                sold++;
            }
        }
        int available = total - sold;

        binding.stockTotalCount.setText(String.valueOf(total));
        binding.stockAvailableCount.setText(String.valueOf(available));
        binding.stockSoldCount.setText(String.valueOf(sold));
    }

    public void showEditItemDialog(item_recycler_model_stocks item) {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_item_dialog);
        UiAnimationHelper.applyDialogAnimations(dialog);

        final TextView title = dialog.findViewById(R.id.dialog_title);
        final EditText editName = dialog.findViewById(R.id.edit_item_name);
        final EditText editWeight = dialog.findViewById(R.id.edit_item_weight);
        final EditText editBarcode = dialog.findViewById(R.id.edit_item_barcode);
        final TextInputLayout barcodeLayout = dialog.findViewById(R.id.layout_item_barcode);
        final RadioGroup typeGroup = dialog.findViewById(R.id.radio_group_type);
        Button saveButton = dialog.findViewById(R.id.save_item_button);

        if (title != null) title.setText("Edit Stock Item");
        editName.setText(item.getName());
        editWeight.setText(String.valueOf(item.getWeight()));
        editBarcode.setText(item.getBarcode());
        if ("Gold".equalsIgnoreCase(item.getType())) {
            typeGroup.check(R.id.radio_gold);
        } else {
            typeGroup.check(R.id.radio_silver);
        }

        if (barcodeLayout != null) {
            barcodeLayout.setEndIconOnClickListener(v -> {
                if (scanner != null) {
                    scanner.startScan()
                            .addOnSuccessListener(barcode -> {
                                if (editBarcode != null) editBarcode.setText(barcode.getRawValue());
                            });
                }
            });
        }

        saveButton.setText("Update Item");
        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            String barcode = editBarcode.getText().toString().trim();
            if (name.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                int selectedTypeId = typeGroup.getCheckedRadioButtonId();
                RadioButton selectedType = dialog.findViewById(selectedTypeId);
                String type = selectedType.getText().toString();

                databaseSystem.databaseExecutor.execute(() -> {
                    dbHelper.updateItem(item.getId(), name, weight, type, barcode.isEmpty() ? null : barcode);

                    mainHandler.post(() -> {
                        Toast.makeText(getContext(), "Item updated!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadItemsFromDB();
                    });
                });
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showAddItemDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.add_item_dialog);
        UiAnimationHelper.applyDialogAnimations(dialog);

        final TextView title = dialog.findViewById(R.id.dialog_title);
        final EditText editName = dialog.findViewById(R.id.edit_item_name);
        final EditText editWeight = dialog.findViewById(R.id.edit_item_weight);
        final EditText editBarcode = dialog.findViewById(R.id.edit_item_barcode);
        final TextInputLayout barcodeLayout = dialog.findViewById(R.id.layout_item_barcode);
        final RadioGroup typeGroup = dialog.findViewById(R.id.radio_group_type);
        Button saveButton = dialog.findViewById(R.id.save_item_button);

        if (title != null) title.setText("Add New Stock Item");

        if (barcodeLayout != null) {
            barcodeLayout.setEndIconOnClickListener(v -> {
                if (scanner != null) {
                    scanner.startScan()
                            .addOnSuccessListener(barcode -> {
                                if (editBarcode != null) editBarcode.setText(barcode.getRawValue());
                            });
                }
            });
        }

        saveButton.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String weightStr = editWeight.getText().toString().trim();
            String barcode = editBarcode.getText().toString().trim();
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

                databaseSystem.databaseExecutor.execute(() -> {
                    // Insert with isSold = false by default
                    dbHelper.insertItem(name, weight, type, barcode.isEmpty() ? null : barcode, false);

                    mainHandler.post(() -> {
                        Toast.makeText(getContext(), "Item saved!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadItemsFromDB();
                    });
                });
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
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
