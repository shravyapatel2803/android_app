package com.example.billgenerator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log; // <-- Added for logging

// Make sure these imports exist and point to the correct model package
import com.example.billgenerator.models.Item;
import com.example.billgenerator.models.SelectedItem;


import java.util.ArrayList;
import java.util.List; // Keep List import
import android.text.TextUtils;


public class databaseSystem extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_database.db";
    // <-- IMPORTANT: Increment Database Version for schema change -->
    private static final int DATABASE_VERSION = 4; // Changed from 3

    // Table and column names
    private static final String TABLE_ITEMS = "items";
    private static final String TABLE_CUSTOMERS = "customer";
    private static final String TABLE_BILLS = "bills";
    private static final String TABLE_BILL_ITEMS = "bill_items";

    // Common columns
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";

    // Items table columns
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_IS_SOLD = "is_sold";

    // Customer table columns
    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_VILLAGE = "village";
    // <-- New Column for Customer Debt -->
    private static final String COLUMN_DEBT = "debt";

    // Bills table columns
    private static final String COLUMN_CUSTOMER_ID = "customer_id";
    // Store calculated rates if needed for display, or remove if not storing
    private static final String COLUMN_CALCULATED_GOLD_RATE = "calc_gold_rate";
    private static final String COLUMN_CALCULATED_SILVER_RATE = "calc_silver_rate";
    private static final String COLUMN_TOTAL_AMOUNT = "total_amount"; // This will be the FINAL amount saved
    private static final String COLUMN_BILL_DATE = "bill_date";
    // <-- New Columns for GST -->
    private static final String COLUMN_GST_PERCENT = "gst_percent"; // Store GST % applied (0 if none)


    // Bill Items table columns (Unchanged)
    private static final String COLUMN_BILL_ID = "bill_id";
    private static final String COLUMN_ITEM_ID = "item_id";


    public databaseSystem(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Database", "Creating new database schema version " + DATABASE_VERSION);
        // Items Table (Unchanged)
        String CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_WEIGHT + " REAL,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_IS_SOLD + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_ITEMS_TABLE);

        // Customers Table (Added Debt column)
        String CREATE_CUSTOMERS_TABLE = "CREATE TABLE " + TABLE_CUSTOMERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PHONE + " TEXT UNIQUE,"
                + COLUMN_VILLAGE + " TEXT,"
                + COLUMN_DEBT + " REAL DEFAULT 0.0" + ")"; // <-- Added Debt with default 0.0
        db.execSQL(CREATE_CUSTOMERS_TABLE);

        // Bills Table (Added GST, Renamed Rate Columns)
        String CREATE_BILLS_TABLE = "CREATE TABLE " + TABLE_BILLS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CUSTOMER_ID + " INTEGER,"
                + COLUMN_CALCULATED_GOLD_RATE + " REAL DEFAULT 0.0," // Store calculated rate
                + COLUMN_CALCULATED_SILVER_RATE + " REAL DEFAULT 0.0," // Store calculated rate
                + COLUMN_TOTAL_AMOUNT + " REAL," // Final amount (manual or calculated+GST)
                + COLUMN_GST_PERCENT + " REAL DEFAULT 0.0," // GST Percentage applied
                + COLUMN_BILL_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILLS_TABLE);

        // Bill Items Table (Unchanged)
        String CREATE_BILL_ITEMS_TABLE = "CREATE TABLE " + TABLE_BILL_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BILL_ID + " INTEGER,"
                + COLUMN_ITEM_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_ITEM_ID + ") REFERENCES " + TABLE_ITEMS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILL_ITEMS_TABLE);
        Log.i("Database", "Database tables created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion);
        // --- Handle Schema Changes ---
        if (oldVersion < 4) {
            Log.i("DatabaseUpgrade", "Applying changes for version 4...");
            // Add Debt column to Customers if it doesn't exist
            try {
                Log.d("DatabaseUpgrade", "Attempting to add debt column to customer table...");
                db.execSQL("ALTER TABLE " + TABLE_CUSTOMERS + " ADD COLUMN " + COLUMN_DEBT + " REAL DEFAULT 0.0");
                Log.i("DatabaseUpgrade", "Successfully added debt column to customer table.");
            } catch (Exception e) {
                Log.e("DatabaseUpgrade", "Failed to add debt column to customers (maybe it exists?): " + e.getMessage());
                // Don't necessarily fail the whole upgrade if column exists
            }
            // Add GST column and rename rate columns in Bills
            try {
                Log.d("DatabaseUpgrade", "Attempting to add gst_percent column to bills table...");
                db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_GST_PERCENT + " REAL DEFAULT 0.0");
                Log.i("DatabaseUpgrade", "Successfully added gst_percent column to bills table.");

                // Rename old rate columns cautiously (check if they exist first might be safer)
                // Assuming they were named 'gold_rate' and 'silver_rate' before version 4
                Log.d("DatabaseUpgrade", "Attempting to rename rate columns in bills table...");
                try {
                    db.execSQL("ALTER TABLE " + TABLE_BILLS + " RENAME COLUMN gold_rate TO " + COLUMN_CALCULATED_GOLD_RATE);
                    Log.i("DatabaseUpgrade", "Renamed gold_rate column.");
                } catch (Exception re) { Log.w("DatabaseUpgrade", "Could not rename gold_rate (maybe already renamed or doesn't exist): " + re.getMessage());}
                try {
                    db.execSQL("ALTER TABLE " + TABLE_BILLS + " RENAME COLUMN silver_rate TO " + COLUMN_CALCULATED_SILVER_RATE);
                    Log.i("DatabaseUpgrade", "Renamed silver_rate column.");
                } catch (Exception re) { Log.w("DatabaseUpgrade", "Could not rename silver_rate (maybe already renamed or doesn't exist): " + re.getMessage());}


            } catch (Exception e) {
                Log.e("DatabaseUpgrade", "Failed to modify bills table during ALTER/RENAME: " + e.getMessage());
                // Fallback: Drop all tables and recreate them if ALTER fails catastrophically
                // This WILL delete all existing user data! Use with caution.
                Log.e("DatabaseUpgrade", "!!! CRITICAL: ALTER failed. Dropping all tables and recreating schema. DATA WILL BE LOST !!!");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILL_ITEMS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILLS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
                onCreate(db); // Recreate all tables
                return; // Stop further upgrade steps if we dropped tables
            }
        }
        // Add more 'if (oldVersion < X)' blocks for future versions below
        Log.w("DatabaseUpgrade", "Database upgrade finished.");
    }


    // --- ITEM METHODS ---
    public void insertItem(String name, double weight, String type, boolean isSold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        long result = db.insert(TABLE_ITEMS, null, values);
        if (result == -1) {
            Log.e("Database", "Error inserting item: " + name);
        } else {
            Log.d("Database", "Inserted item ID: " + result + " Name: " + name);
        }
    }

    // Fetches all items, used by Stock Management screen
    public Cursor fetchItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all items for stock screen.");
        // Order by available first, then by ID descending
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_IS_SOLD + " ASC, " + COLUMN_ID + " DESC");
    }

    // Fetches all items, used by PDF generation and potentially other places
    public Cursor fetchAllItemsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all items cursor.");
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    // Fetches only UNSOLD items as Item objects, used by GenerateBillFragment AddItemDialog
    public List<Item> fetchAllItems() {
        Log.d("Database", "Fetching UNSOLD items for AddItemDialog.");
        List<Item> itemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS,
                null,
                COLUMN_IS_SOLD + " = ?", // Only fetch items WHERE is_sold = 0
                new String[]{"0"},
                null, null,
                COLUMN_NAME + " ASC"); // Order alphabetically

        if (cursor != null) {
            try {
                int idCol = cursor.getColumnIndexOrThrow(COLUMN_ID);
                int nameCol = cursor.getColumnIndexOrThrow(COLUMN_NAME);
                int weightCol = cursor.getColumnIndexOrThrow(COLUMN_WEIGHT);
                int typeCol = cursor.getColumnIndexOrThrow(COLUMN_TYPE);

                while (cursor.moveToNext()) {
                    int id = cursor.getInt(idCol);
                    String name = cursor.getString(nameCol);
                    double weight = cursor.getDouble(weightCol);
                    String type = cursor.getString(typeCol);
                    itemList.add(new Item(id, name, weight, type));
                    Log.d("FetchUnsold", "Found: ID=" + id + ", Name=" + name);
                }
            } catch (Exception e) {
                Log.e("Database", "Error fetching unsold items: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }
        Log.d("Database", "Found " + itemList.size() + " unsold items.");
        return itemList;
    }

    // Updates the sold status of an item
    public void updateItemSoldStatus(int id, boolean isSold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        int rows = db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            Log.d("Database", "Updated sold status for item ID " + id + " to " + isSold);
        } else {
            Log.w("Database", "Failed to update sold status for item ID " + id);
        }
    }

    // --- CUSTOMER METHODS ---

    // Inserts a new customer ONLY IF the phone number doesn't already exist.
    public long insertCustomer(String name, String phone, String village) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone); // Relies on UNIQUE constraint
        values.put(COLUMN_VILLAGE, village);
        values.put(COLUMN_DEBT, 0.0); // Initialize debt
        Log.d("Database", "Attempting to insert new customer: " + name + ", Phone: " + phone);
        // Use insertWithOnConflict to ignore if phone exists, returns -1 if ignored or error
        long result = db.insertWithOnConflict(TABLE_CUSTOMERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (result == -1) {
            Log.w("Database", "Customer insertion ignored or failed (Phone likely exists): " + phone);
        } else {
            Log.i("Database", "Inserted new customer ID: " + result);
        }
        return result;
    }

    // Updates Name, Village, AND Debt for an existing customer ID.
    public int updateCustomer(long id, String name, String village, double debt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_VILLAGE, village);
        values.put(COLUMN_DEBT, debt); // Update debt as well
        Log.d("Database", "Updating customer ID " + id + " - Name: " + name + ", Village: " + village + ", Debt: " + debt);
        int rows = db.update(TABLE_CUSTOMERS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows <= 0) {
            Log.w("Database", "Failed to update customer ID " + id);
        }
        return rows;
    }

    // Adds (or subtracts if debtChange is negative) to the customer's current debt. Uses transaction for safety.
    public int updateCustomerDebt(long customerId, double debtChange) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("UpdateDebt", "Attempting to add debt " + debtChange + " to customer ID: " + customerId);
        // Use SQL expression to add to existing debt atomically
        String updateQuery = "UPDATE " + TABLE_CUSTOMERS +
                " SET " + COLUMN_DEBT + " = " + COLUMN_DEBT + " + ?" +
                " WHERE " + COLUMN_ID + " = ?";
        int rowsAffected = 0;
        db.beginTransaction();
        try {
            db.execSQL(updateQuery, new Object[]{debtChange, customerId});
            db.setTransactionSuccessful();
            rowsAffected = 1; // Assume success if transaction completes without error
            Log.i("UpdateDebt", "Successfully updated debt for customer " + customerId + " by " + debtChange);
        } catch (Exception e){
            Log.e("UpdateDebt", "Error updating debt for customer " + customerId, e);
            rowsAffected = 0; // Indicate failure
        } finally {
            db.endTransaction();
        }
        return rowsAffected; // Return 1 on success, 0 on failure
    }


    // Finds customer by phone. If exists, updates Name/Village and returns ID. If not, inserts new customer (with 0 debt) and returns new ID.
    public long insertOrGetCustomer(String name, String phone, String village) {
        SQLiteDatabase dbRead = this.getReadableDatabase();
        long customerId = -1;
        Cursor cursor = null;
        Log.d("Database", "Checking for customer with phone: " + phone);
        try {
            cursor = dbRead.query(TABLE_CUSTOMERS, new String[]{COLUMN_ID}, COLUMN_PHONE + " = ?", new String[]{phone}, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(COLUMN_ID);
                if (idIndex != -1) {
                    customerId = cursor.getLong(idIndex);
                    Log.d("Database", "Customer found with ID: " + customerId);
                }
            } else {
                Log.d("Database", "Customer not found with phone: " + phone);
            }
        } finally {
            if (cursor != null) cursor.close();
        }

        SQLiteDatabase dbWrite = this.getWritableDatabase();
        if (customerId != -1) {
            Log.d("Database", "Updating existing customer ID: " + customerId);
            ContentValues updateValues = new ContentValues();
            updateValues.put(COLUMN_NAME, name);
            updateValues.put(COLUMN_VILLAGE, village);
            // DO NOT update debt here, only name/village are updated on fetch/update
            dbWrite.update(TABLE_CUSTOMERS, updateValues, COLUMN_ID + " = ?", new String[]{String.valueOf(customerId)});
            return customerId; // Return existing ID
        } else {
            Log.d("Database", "Inserting new customer: " + name);
            ContentValues insertValues = new ContentValues();
            insertValues.put(COLUMN_NAME, name);
            insertValues.put(COLUMN_PHONE, phone);
            insertValues.put(COLUMN_VILLAGE, village);
            insertValues.put(COLUMN_DEBT, 0.0); // Initialize debt for new customer
            long newId = dbWrite.insert(TABLE_CUSTOMERS, null, insertValues);
            if (newId == -1) {
                Log.e("Database", "Failed to insert new customer: " + name);
            } else {
                Log.i("Database", "Inserted new customer ID: " + newId);
            }
            return newId; // Return new ID (or -1 if insert fails)
        }
    }

    // Gets customer details (including debt) by phone number.
    public Cursor getCustomerByPhone(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching customer by phone: " + phone);
        // Fetch all columns, including debt
        return db.query(TABLE_CUSTOMERS, null, COLUMN_PHONE + " = ?", new String[]{phone}, null, null, null);
    }

    // Fetches all customer details (including debt) ordered by name.
    public Cursor fetchCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all customers.");
        // Select all columns including debt
        return db.query(TABLE_CUSTOMERS, null, null, null, null, null, COLUMN_NAME + " ASC");
    }

    // Deletes a customer by ID.
    public void deleteCustomer(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        // Consider implications: What happens to bills associated with this customer?
        // Add foreign key constraint with ON DELETE SET NULL or CASCADE if needed.
        Log.w("Database", "Deleting customer ID: " + id);
        int rows = db.delete(TABLE_CUSTOMERS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows <= 0) {
            Log.w("Database", "Failed to delete customer ID " + id);
        }
    }


    // --- BILLING METHODS ---

    // Saves the main bill record and associated items.
    public long insertBill(long customerId, double calcGoldRate, double calcSilverRate, double finalTotalAmount, double gstPercent, List<SelectedItem> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_CALCULATED_GOLD_RATE, calcGoldRate); // Store potentially calculated rates (or 0)
        values.put(COLUMN_CALCULATED_SILVER_RATE, calcSilverRate);
        values.put(COLUMN_TOTAL_AMOUNT, finalTotalAmount); // Store the final amount entered/confirmed by user
        values.put(COLUMN_GST_PERCENT, gstPercent); // Store GST % applied

        Log.d("Database", "Inserting bill for customer ID: " + customerId + ", Amount: " + finalTotalAmount + ", GST: " + gstPercent + "%");
        long billId = db.insert(TABLE_BILLS, null, values);

        if (billId != -1) {
            Log.i("Database", "Inserted bill with ID: " + billId);
            // Insert items into bill_items table
            if (items != null && !items.isEmpty()) {
                Log.d("Database", "Inserting " + items.size() + " items for bill ID: " + billId);
                for (SelectedItem item : items) {
                    insertBillItem(billId, item.getId());
                    // Item sold status should already be updated by AddItemAdapter or GenerateBillFragment
                    // updateItemSoldStatus(item.getId(), true); // Redundant if done elsewhere
                }
            } else {
                Log.w("Database", "No items provided to insert for bill ID: " + billId);
            }
        } else {
            Log.e("Database", "Failed to insert bill for customer ID: " + customerId);
        }
        return billId;
    }


    // Links an item to a bill in the bill_items table.
    public void insertBillItem(long billId, int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BILL_ID, billId);
        values.put(COLUMN_ITEM_ID, itemId);
        long result = db.insert(TABLE_BILL_ITEMS, null, values);
        if (result == -1) {
            Log.e("Database", "Failed to insert bill item link: BillID=" + billId + ", ItemID=" + itemId);
        } else {
            Log.d("Database", "Linked Item ID " + itemId + " to Bill ID " + billId);
        }
    }

    // Fetches summary data (ID, Customer Name, Date, Final Total) for the bill history list.
    public Cursor fetchBillHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching bill history summary.");
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +
                "c." + COLUMN_NAME + ", " +          // Customer Name
                "b." + COLUMN_BILL_DATE + ", " +     // Bill Date
                "b." + COLUMN_TOTAL_AMOUNT +         // Final Total Amount
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " ORDER BY b." + COLUMN_ID + " DESC"; // Order by most recent bill
        return db.rawQuery(query, null);
    }


    // Gets comma-separated string of Bill IDs for a specific customer.
    public String getBillIdsForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> billIds = new ArrayList<>();
        Log.d("Database", "Fetching bill IDs for customer ID: " + customerId);
        // Order by most recent bill ID first
        Cursor cursor = db.query(TABLE_BILLS, new String[]{COLUMN_ID}, COLUMN_CUSTOMER_ID + " = ?", new String[]{String.valueOf(customerId)}, null, null, COLUMN_ID + " DESC");

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int idCol = cursor.getColumnIndexOrThrow(COLUMN_ID);
                    do {
                        billIds.add(String.valueOf(cursor.getInt(idCol)));
                    } while (cursor.moveToNext());
                }
            } finally {
                cursor.close();
            }
        }
        Log.d("Database", "Found bill IDs: [" + TextUtils.join(", ", billIds) + "] for customer ID: " + customerId);
        return TextUtils.join(", ", billIds);
    }

    // Gets detailed information for a single bill (including GST, rates).
    public Cursor getBillDetails(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching details for bill ID: " + billId);
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +             // Bill ID
                "c." + COLUMN_NAME + ", " +           // Customer Name
                "c." + COLUMN_PHONE + ", " +          // Customer Phone
                "b." + COLUMN_BILL_DATE + ", " +      // Bill Date
                "b." + COLUMN_CALCULATED_GOLD_RATE + ", " + // Calculated Gold Rate
                "b." + COLUMN_CALCULATED_SILVER_RATE + ", " + // Calculated Silver Rate
                "b." + COLUMN_TOTAL_AMOUNT + ", " +   // Final Total Amount
                "b." + COLUMN_GST_PERCENT +          // GST Percent
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " WHERE b." + COLUMN_ID + " = ?"; // Use placeholder

        String[] selectionArgs = { String.valueOf(billId) };
        return db.rawQuery(query, selectionArgs);
    }


    // Fetches all items (name, weight, type) linked to a specific bill ID.
    public Cursor getItemsForBill(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching items for bill ID: " + billId);
        String query = "SELECT " +
                "i." + COLUMN_NAME + ", " +
                "i." + COLUMN_WEIGHT + ", " +
                "i." + COLUMN_TYPE +
                " FROM " + TABLE_BILL_ITEMS + " bi" +
                " JOIN " + TABLE_ITEMS + " i ON bi." + COLUMN_ITEM_ID + " = i." + COLUMN_ID +
                " WHERE bi." + COLUMN_BILL_ID + " = ?"; // Use placeholder

        String[] selectionArgs = { String.valueOf(billId) };
        Cursor cursor = db.rawQuery(query, selectionArgs);
        if (cursor != null) {
            Log.d("Database", "Found " + cursor.getCount() + " items for bill ID: " + billId);
        } else {
            Log.w("Database", "Cursor is null when fetching items for bill ID: " + billId);
        }
        return cursor;
    }
}