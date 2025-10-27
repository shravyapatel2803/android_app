package com.example.billgenerator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

import com.example.billgenerator.models.Item;

public class databaseSystem extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_database.db";
    // Increment version to trigger onUpgrade and create the new tables
    private static final int DATABASE_VERSION = 3;

    // Table and column names
    private static final String TABLE_ITEMS = "items";
    private static final String TABLE_CUSTOMERS = "customer";
    private static final String TABLE_BILLS = "bills"; // NEW
    private static final String TABLE_BILL_ITEMS = "bill_items"; // NEW

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

    // Bills table columns (NEW)
    private static final String COLUMN_CUSTOMER_ID = "customer_id";
    private static final String COLUMN_GOLD_RATE = "gold_rate";
    private static final String COLUMN_SILVER_RATE = "silver_rate";
    private static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    private static final String COLUMN_BILL_DATE = "bill_date";

    // Bill Items table columns (NEW)
    private static final String COLUMN_BILL_ID = "bill_id";
    private static final String COLUMN_ITEM_ID = "item_id";


    public databaseSystem(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_WEIGHT + " REAL,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_IS_SOLD + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_ITEMS_TABLE);

        String CREATE_CUSTOMERS_TABLE = "CREATE TABLE " + TABLE_CUSTOMERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PHONE + " TEXT UNIQUE," // Phone should be unique
                + COLUMN_VILLAGE + " TEXT" + ")";
        db.execSQL(CREATE_CUSTOMERS_TABLE);

        // --- NEW TABLES ---
        String CREATE_BILLS_TABLE = "CREATE TABLE " + TABLE_BILLS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CUSTOMER_ID + " INTEGER,"
                + COLUMN_GOLD_RATE + " REAL,"
                + COLUMN_SILVER_RATE + " REAL,"
                + COLUMN_TOTAL_AMOUNT + " REAL,"
                + COLUMN_BILL_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILLS_TABLE);

        String CREATE_BILL_ITEMS_TABLE = "CREATE TABLE " + TABLE_BILL_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BILL_ID + " INTEGER,"
                + COLUMN_ITEM_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_ITEM_ID + ") REFERENCES " + TABLE_ITEMS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILL_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILL_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BILLS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CUSTOMERS);
        onCreate(db);
    }

    // --- ITEM METHODS ---
    public void insertItem(String name, double weight, String type, boolean isSold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        db.insert(TABLE_ITEMS, null, values);
    }

    public Cursor fetchItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    public void updateItemSoldStatus(int id, boolean isSold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // --- CUSTOMER METHODS ---
    public long insertCustomer(String name, String phone, String village) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_VILLAGE, village);
        return db.insert(TABLE_CUSTOMERS, null, values);
    }

    public int updateCustomer(long id, String name, String village) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_VILLAGE, village);
        return db.update(TABLE_CUSTOMERS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor getCustomerByPhone(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, COLUMN_PHONE + " = ?", new String[]{phone}, null, null, null);
    }

    public Cursor fetchCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, null, null, null, null, COLUMN_NAME + " ASC");
    }

    // --- BILLING METHODS (NEW) ---
    public long insertBill(long customerId, double goldRate, double silverRate, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_GOLD_RATE, goldRate);
        values.put(COLUMN_SILVER_RATE, silverRate);
        values.put(COLUMN_TOTAL_AMOUNT, totalAmount);
        return db.insert(TABLE_BILLS, null, values);
    }

    public void insertBillItem(long billId, int itemId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BILL_ID, billId);
        values.put(COLUMN_ITEM_ID, itemId);
        db.insert(TABLE_BILL_ITEMS, null, values);
    }

    public void deleteCustomer(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CUSTOMERS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // --- BILL HISTORY METHOD (NEW) ---
    public Cursor fetchBillHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Use a JOIN to get customer name along with bill details
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +
                "c." + COLUMN_NAME + ", " +
                "b." + COLUMN_BILL_DATE + ", " +
                "b." + COLUMN_TOTAL_AMOUNT +
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " ORDER BY b." + COLUMN_ID + " DESC";

        return db.rawQuery(query, null);
    }
    // --- NEW: Method to get all bill IDs for a customer ---
    public String getBillIdsForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> billIds = new ArrayList<>();
        Cursor cursor = db.query(TABLE_BILLS,
                new String[]{COLUMN_ID}, // Select only the bill ID column
                COLUMN_CUSTOMER_ID + " = ?", // Where the customer_id matches
                new String[]{String.valueOf(customerId)},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            int idCol = cursor.getColumnIndex(COLUMN_ID);
            do {
                billIds.add(String.valueOf(cursor.getInt(idCol)));
            } while (cursor.moveToNext());
            cursor.close();
        }

        // Return a comma-separated string, e.g., "101, 105, 112"
//        return com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.util.TextUtils.join(", ", billIds);
        // Return a comma-separated string, e.g., "101, 105, 112"
        return TextUtils.join(", ", billIds);
    }

    // --- NEW: Method to get details for a single bill ---
    // --- NEW: Method to get details for a single bill (Corrected & Secure) ---
    public Cursor getBillDetails(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +
                "c." + COLUMN_NAME + ", " +
                "c." + COLUMN_PHONE + ", " +
                "b." + COLUMN_BILL_DATE + ", " +
                "b." + COLUMN_GOLD_RATE + ", " +
                "b." + COLUMN_SILVER_RATE + ", " +
                "b." + COLUMN_TOTAL_AMOUNT +
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " WHERE b." + COLUMN_ID + " = ?"; // <-- FIXED: Was ' = billId' which is wrong

        // Pass the arguments in a separate array
        String[] selectionArgs = { String.valueOf(billId) };

        return db.rawQuery(query, selectionArgs); // <-- Pass the arguments here
    }
    // --- NEW: Method to get all items for a specific bill ---
    // --- NEW: Method to get all items for a specific bill (Corrected & Secure) ---
    public Cursor getItemsForBill(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                "i." + COLUMN_NAME + ", " +
                "i." + COLUMN_WEIGHT + ", " +
                "i." + COLUMN_TYPE +
                " FROM " + TABLE_BILL_ITEMS + " bi" +
                " JOIN " + TABLE_ITEMS + " i ON bi." + COLUMN_ITEM_ID + " = i." + COLUMN_ID +
                " WHERE bi." + COLUMN_BILL_ID + " = ?"; // <-- Use a placeholder '?'

        // Pass the arguments in a separate array
        String[] selectionArgs = { String.valueOf(billId) };

        return db.rawQuery(query, selectionArgs); // <-- Pass the arguments here
    }

   public List<Item> fetchAllItems(){
        List<Item> itemList = new ArrayList<>();
       Cursor cursor = this.fetchAllItemsCursor();

       if (cursor != null) {
           int idCol = cursor.getColumnIndexOrThrow(COLUMN_ID);
           int nameCol = cursor.getColumnIndexOrThrow(COLUMN_NAME);
           int weightCol = cursor.getColumnIndexOrThrow(COLUMN_WEIGHT);
           int typeCol = cursor.getColumnIndexOrThrow(COLUMN_TYPE);
           int soldCol = cursor.getColumnIndexOrThrow(COLUMN_IS_SOLD);

           while (cursor.moveToNext()) {
               // We only want to add items that are NOT sold
               int isSoldInt = cursor.getInt(soldCol);
               boolean isSold = (isSoldInt == 1);

               if (!isSold) {
                   int id = cursor.getInt(idCol);
                   String name = cursor.getString(nameCol);
                   double weight = cursor.getDouble(weightCol);
                   String type = cursor.getString(typeCol);
                   itemList.add(new Item(id, name, weight, type));
               }
           }
           cursor.close();
       }
       return itemList;
   }

    public Cursor fetchAllItemsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_ID + " DESC");

    }

    public long insertOrGetCustomer(String name, String phone, String village) {
        long customerId = -1;
        Cursor cursor = getCustomerByPhone(phone);

        if (cursor != null && cursor.moveToFirst()) {
            // Customer exists, get the ID
            int idCol = cursor.getColumnIndex(COLUMN_ID);
            customerId = cursor.getLong(idCol);
            cursor.close();
        } else {
            // Customer doesn't exist, insert a new one
            customerId = insertCustomer(name, phone, village);
        }
        return customerId;
    }
}