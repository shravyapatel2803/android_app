package com.example.billgenerator.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import com.example.billgenerator.models.Item;
import com.example.billgenerator.models.ReturnItem;
import com.example.billgenerator.models.SelectedItem;

import java.util.ArrayList;
import java.util.List;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

public class databaseSystem extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "my_database.db";
    private static final int DATABASE_VERSION = 13;

    private static final String TABLE_ITEMS = "items";
    private static final String TABLE_CUSTOMERS = "customer";
    private static final String TABLE_BILLS = "bills";
    private static final String TABLE_BILL_ITEMS = "bill_items";
    private static final String TABLE_BILL_RETURN_ITEMS = "bill_return_items";
    private static final String TABLE_NOTIFICATION_HISTORY = "notification_history";
    private static final String TABLE_DEBT_UPDATES = "debt_updates";
    private static final String TABLE_SUPPLIERS = "suppliers";
    private static final String TABLE_PURCHASE_BILLS = "purchase_bills";
    private static final String TABLE_EXPENSES = "expenses";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";

    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_TYPE = "type";
    private static final String COLUMN_IS_SOLD = "is_sold";
    private static final String COLUMN_BARCODE = "barcode";

    private static final String COLUMN_PHONE = "phone";
    private static final String COLUMN_VILLAGE = "village";
    private static final String COLUMN_DEBT = "debt";

    private static final String COLUMN_CUSTOMER_ID = "customer_id";
    private static final String COLUMN_CALCULATED_GOLD_RATE = "calc_gold_rate";
    private static final String COLUMN_CALCULATED_SILVER_RATE = "calc_silver_rate";
    private static final String COLUMN_TOTAL_AMOUNT = "total_amount";
    private static final String COLUMN_BILLED_AMOUNT = "billed_amount";
    private static final String COLUMN_PAID_AMOUNT = "paid_amount";
    private static final String COLUMN_BILL_DATE = "bill_date";
    private static final String COLUMN_GST_PERCENT = "gst_percent";
    private static final String COLUMN_PAYMENT_MODE = "payment_mode";
    private static final String COLUMN_PAYMENT_DETAILS = "payment_details";
    private static final String COLUMN_RETURN_ITEM_TYPE = "return_item_type";
    private static final String COLUMN_RETURN_ITEM_WEIGHT = "return_item_weight";
    private static final String COLUMN_RETURN_ITEM_DEDUCT_AMOUNT = "return_item_deduct_amount";
    private static final String COLUMN_DEBT_DUE_DATE = "debt_due_date";
    private static final String COLUMN_DEBT_AMOUNT = "debt_amount";

    private static final String COLUMN_BILL_ID = "bill_id";
    private static final String COLUMN_ITEM_ID = "item_id";

    private static final String COLUMN_RETURN_TYPE = "return_type";
    private static final String COLUMN_RETURN_WEIGHT = "return_weight";
    private static final String COLUMN_RETURN_DEDUCT_AMOUNT = "return_deduct_amount";

    private static final String COLUMN_NOTIFICATION_TYPE = "notification_type";
    private static final String COLUMN_NOTIFICATION_MESSAGE = "message";
    private static final String COLUMN_NOTIFICATION_CUSTOMER_NAME = "customer_name";
    private static final String COLUMN_NOTIFICATION_NOTIFIED_DATE = "notified_date";
    private static final String COLUMN_NOTIFICATION_CREATED_AT = "created_at";

    private static final String COLUMN_DEBT_UPDATE_BILL_ID = "bill_id";
    private static final String COLUMN_DEBT_UPDATE_CHANGE_AMOUNT = "change_amount";
    private static final String COLUMN_DEBT_UPDATE_RESULTING_BALANCE = "resulting_balance";
    private static final String COLUMN_DEBT_UPDATE_BILLED_AMOUNT = "billed_amount";
    private static final String COLUMN_DEBT_UPDATE_PAID_AMOUNT = "paid_amount";
    private static final String COLUMN_DEBT_UPDATE_DUE_DATE = "due_date";
    private static final String COLUMN_DEBT_UPDATE_NOTE = "note";
    private static final String COLUMN_DEBT_UPDATE_CREATED_AT = "created_at";

    // Supplier Table Columns
    private static final String COLUMN_SUPPLIER_ADDRESS = "address";

    // Purchase Bill Columns
    private static final String COLUMN_SUPPLIER_ID = "supplier_id";
    private static final String COLUMN_PURCHASE_DATE = "purchase_date";

    // Expense Columns
    private static final String COLUMN_EXPENSE_TITLE = "title";
    private static final String COLUMN_EXPENSE_AMOUNT = "amount";
    private static final String COLUMN_EXPENSE_CATEGORY = "category";
    private static final String COLUMN_EXPENSE_DATE = "date";
    private static final String COLUMN_EXPENSE_CREATED_AT = "created_at";

    public databaseSystem(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Database", "Creating new database schema version " + DATABASE_VERSION);
        String CREATE_ITEMS_TABLE = "CREATE TABLE " + TABLE_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_WEIGHT + " REAL,"
                + COLUMN_TYPE + " TEXT,"
                + COLUMN_BARCODE + " TEXT,"
                + COLUMN_IS_SOLD + " INTEGER DEFAULT 0" + ")";
        db.execSQL(CREATE_ITEMS_TABLE);

        String CREATE_CUSTOMERS_TABLE = "CREATE TABLE " + TABLE_CUSTOMERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PHONE + " TEXT UNIQUE,"
                + COLUMN_VILLAGE + " TEXT,"
                + COLUMN_DEBT + " REAL DEFAULT 0.0" + ")";
        db.execSQL(CREATE_CUSTOMERS_TABLE);

        String CREATE_BILLS_TABLE = "CREATE TABLE " + TABLE_BILLS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CUSTOMER_ID + " INTEGER,"
                + COLUMN_CALCULATED_GOLD_RATE + " REAL DEFAULT 0.0,"
                + COLUMN_CALCULATED_SILVER_RATE + " REAL DEFAULT 0.0,"
                + COLUMN_TOTAL_AMOUNT + " REAL,"
                + COLUMN_BILLED_AMOUNT + " REAL DEFAULT 0.0,"
                + COLUMN_PAID_AMOUNT + " REAL DEFAULT 0.0,"
                + COLUMN_GST_PERCENT + " REAL DEFAULT 0.0,"
                + COLUMN_PAYMENT_MODE + " TEXT,"
                + COLUMN_PAYMENT_DETAILS + " TEXT,"
                + COLUMN_BILL_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_RETURN_ITEM_TYPE + " TEXT,"
                + COLUMN_RETURN_ITEM_WEIGHT + " REAL,"
                + COLUMN_RETURN_ITEM_DEDUCT_AMOUNT + " REAL,"
            + COLUMN_DEBT_DUE_DATE + " TEXT,"
            + COLUMN_DEBT_AMOUNT + " REAL DEFAULT 0.0,"
                + "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILLS_TABLE);

        String CREATE_BILL_ITEMS_TABLE = "CREATE TABLE " + TABLE_BILL_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BILL_ID + " INTEGER,"
                + COLUMN_ITEM_ID + " INTEGER,"
                + "FOREIGN KEY(" + COLUMN_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + COLUMN_ID + "),"
                + "FOREIGN KEY(" + COLUMN_ITEM_ID + ") REFERENCES " + TABLE_ITEMS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILL_ITEMS_TABLE);

        String CREATE_NOTIFICATION_HISTORY_TABLE = "CREATE TABLE " + TABLE_NOTIFICATION_HISTORY + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + COLUMN_BILL_ID + " INTEGER,"
            + COLUMN_NOTIFICATION_CUSTOMER_NAME + " TEXT,"
            + COLUMN_NOTIFICATION_MESSAGE + " TEXT,"
            + COLUMN_NOTIFICATION_TYPE + " TEXT,"
            + COLUMN_NOTIFICATION_NOTIFIED_DATE + " TEXT,"
            + COLUMN_NOTIFICATION_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        db.execSQL(CREATE_NOTIFICATION_HISTORY_TABLE);

        String CREATE_DEBT_UPDATES_TABLE = "CREATE TABLE " + TABLE_DEBT_UPDATES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_CUSTOMER_ID + " INTEGER NOT NULL,"
                + COLUMN_DEBT_UPDATE_BILL_ID + " INTEGER,"
                + COLUMN_DEBT_UPDATE_CHANGE_AMOUNT + " REAL NOT NULL,"
                + COLUMN_DEBT_UPDATE_RESULTING_BALANCE + " REAL NOT NULL,"
                + COLUMN_DEBT_UPDATE_BILLED_AMOUNT + " REAL DEFAULT 0.0,"
                + COLUMN_DEBT_UPDATE_PAID_AMOUNT + " REAL DEFAULT 0.0,"
                + COLUMN_DEBT_UPDATE_DUE_DATE + " TEXT,"
                + COLUMN_DEBT_UPDATE_NOTE + " TEXT,"
                + COLUMN_DEBT_UPDATE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_DEBT_UPDATES_TABLE);

        String CREATE_BILL_RETURN_ITEMS_TABLE = "CREATE TABLE " + TABLE_BILL_RETURN_ITEMS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_BILL_ID + " INTEGER,"
                + COLUMN_RETURN_TYPE + " TEXT,"
                + COLUMN_RETURN_WEIGHT + " REAL,"
                + COLUMN_RETURN_DEDUCT_AMOUNT + " REAL,"
                + "FOREIGN KEY(" + COLUMN_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_BILL_RETURN_ITEMS_TABLE);

        String CREATE_SUPPLIERS_TABLE = "CREATE TABLE " + TABLE_SUPPLIERS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_NAME + " TEXT,"
                + COLUMN_PHONE + " TEXT,"
                + COLUMN_SUPPLIER_ADDRESS + " TEXT)";
        db.execSQL(CREATE_SUPPLIERS_TABLE);

        String CREATE_PURCHASE_BILLS_TABLE = "CREATE TABLE " + TABLE_PURCHASE_BILLS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_SUPPLIER_ID + " INTEGER,"
                + COLUMN_TOTAL_AMOUNT + " REAL,"
                + COLUMN_PURCHASE_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_PAYMENT_DETAILS + " TEXT,"
                + "FOREIGN KEY(" + COLUMN_SUPPLIER_ID + ") REFERENCES " + TABLE_SUPPLIERS + "(" + COLUMN_ID + "))";
        db.execSQL(CREATE_PURCHASE_BILLS_TABLE);

        String CREATE_EXPENSES_TABLE = "CREATE TABLE " + TABLE_EXPENSES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_EXPENSE_TITLE + " TEXT,"
                + COLUMN_EXPENSE_AMOUNT + " REAL,"
                + COLUMN_EXPENSE_CATEGORY + " TEXT,"
                + COLUMN_EXPENSE_DATE + " TEXT,"
                + COLUMN_EXPENSE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
        db.execSQL(CREATE_EXPENSES_TABLE);

        Log.i("Database", "Database tables created successfully.");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseUpgrade", "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_RETURN_ITEM_TYPE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_RETURN_ITEM_WEIGHT + " REAL");
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_RETURN_ITEM_DEDUCT_AMOUNT + " REAL");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_DEBT_DUE_DATE + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_DEBT_AMOUNT + " REAL DEFAULT 0.0");
        }
        if (oldVersion < 8) {
            String CREATE_NOTIFICATION_HISTORY_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NOTIFICATION_HISTORY + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_BILL_ID + " INTEGER,"
                    + COLUMN_NOTIFICATION_CUSTOMER_NAME + " TEXT,"
                    + COLUMN_NOTIFICATION_MESSAGE + " TEXT,"
                    + COLUMN_NOTIFICATION_TYPE + " TEXT,"
                    + COLUMN_NOTIFICATION_NOTIFIED_DATE + " TEXT,"
                    + COLUMN_NOTIFICATION_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
            db.execSQL(CREATE_NOTIFICATION_HISTORY_TABLE);
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_BILLED_AMOUNT + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_BILLS + " ADD COLUMN " + COLUMN_PAID_AMOUNT + " REAL DEFAULT 0.0");
            db.execSQL("UPDATE " + TABLE_BILLS + " SET " + COLUMN_BILLED_AMOUNT + " = " + COLUMN_TOTAL_AMOUNT + " WHERE " + COLUMN_BILLED_AMOUNT + " = 0");
            db.execSQL("UPDATE " + TABLE_BILLS + " SET " + COLUMN_PAID_AMOUNT + " = CASE WHEN " + COLUMN_DEBT_AMOUNT + " > 0 THEN " + COLUMN_TOTAL_AMOUNT + " - " + COLUMN_DEBT_AMOUNT + " ELSE " + COLUMN_TOTAL_AMOUNT + " END WHERE " + COLUMN_PAID_AMOUNT + " = 0");
        }
        if (oldVersion < 10) {
            String CREATE_DEBT_UPDATES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_DEBT_UPDATES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_CUSTOMER_ID + " INTEGER NOT NULL,"
                    + COLUMN_DEBT_UPDATE_BILL_ID + " INTEGER,"
                    + COLUMN_DEBT_UPDATE_CHANGE_AMOUNT + " REAL NOT NULL,"
                    + COLUMN_DEBT_UPDATE_RESULTING_BALANCE + " REAL NOT NULL,"
                    + COLUMN_DEBT_UPDATE_BILLED_AMOUNT + " REAL DEFAULT 0.0,"
                    + COLUMN_DEBT_UPDATE_PAID_AMOUNT + " REAL DEFAULT 0.0,"
                    + COLUMN_DEBT_UPDATE_DUE_DATE + " TEXT,"
                    + COLUMN_DEBT_UPDATE_NOTE + " TEXT,"
                    + COLUMN_DEBT_UPDATE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "FOREIGN KEY(" + COLUMN_CUSTOMER_ID + ") REFERENCES " + TABLE_CUSTOMERS + "(" + COLUMN_ID + "))";
            db.execSQL(CREATE_DEBT_UPDATES_TABLE);
        }
        if (oldVersion < 11) {
            String CREATE_BILL_RETURN_ITEMS_TABLE = "CREATE TABLE " + TABLE_BILL_RETURN_ITEMS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_BILL_ID + " INTEGER,"
                    + COLUMN_RETURN_TYPE + " TEXT,"
                    + COLUMN_RETURN_WEIGHT + " REAL,"
                    + COLUMN_RETURN_DEDUCT_AMOUNT + " REAL,"
                    + "FOREIGN KEY(" + COLUMN_BILL_ID + ") REFERENCES " + TABLE_BILLS + "(" + COLUMN_ID + "))";
            db.execSQL(CREATE_BILL_RETURN_ITEMS_TABLE);
        }
        if (oldVersion < 12) {
            db.execSQL("ALTER TABLE " + TABLE_ITEMS + " ADD COLUMN " + COLUMN_BARCODE + " TEXT");
            
            String CREATE_SUPPLIERS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_SUPPLIERS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NAME + " TEXT,"
                    + COLUMN_PHONE + " TEXT,"
                    + COLUMN_SUPPLIER_ADDRESS + " TEXT)";
            db.execSQL(CREATE_SUPPLIERS_TABLE);

            String CREATE_PURCHASE_BILLS_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_PURCHASE_BILLS + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_SUPPLIER_ID + " INTEGER,"
                    + COLUMN_TOTAL_AMOUNT + " REAL,"
                    + COLUMN_PURCHASE_DATE + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + COLUMN_PAYMENT_DETAILS + " TEXT,"
                    + "FOREIGN KEY(" + COLUMN_SUPPLIER_ID + ") REFERENCES " + TABLE_SUPPLIERS + "(" + COLUMN_ID + "))";
            db.execSQL(CREATE_PURCHASE_BILLS_TABLE);
        }
        if (oldVersion < 13) {
            String CREATE_EXPENSES_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_EXPENSES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_EXPENSE_TITLE + " TEXT,"
                    + COLUMN_EXPENSE_AMOUNT + " REAL,"
                    + COLUMN_EXPENSE_CATEGORY + " TEXT,"
                    + COLUMN_EXPENSE_DATE + " TEXT,"
                    + COLUMN_EXPENSE_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
            db.execSQL(CREATE_EXPENSES_TABLE);
        }
    }

    public void insertItem(String name, double weight, String type, String barcode, boolean isSold) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_BARCODE, barcode);
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        long result = db.insert(TABLE_ITEMS, null, values);
        if (result == -1) {
            Log.e("Database", "Error inserting item: " + name);
        } else {
            Log.d("Database", "Inserted item ID: " + result + " Name: " + name);
        }
    }

    public void deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ITEMS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor fetchItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all items for stock screen.");
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_IS_SOLD + " ASC, " + COLUMN_ID + " DESC");
    }

    public Cursor fetchAllItemsCursor() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all items cursor.");
        return db.query(TABLE_ITEMS, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    public Item findItemByBarcode(String barcode) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_ITEMS,
                    new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_WEIGHT, COLUMN_TYPE, COLUMN_BARCODE},
                    COLUMN_BARCODE + " = ? AND " + COLUMN_IS_SOLD + " = 0",
                    new String[]{barcode}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return new Item(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BARCODE))
                );
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public List<Item> fetchAllItems() {
        Log.d("Database", "Fetching UNSOLD items for AddItemDialog.");
        List<Item> itemList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ITEMS,
                null,
                COLUMN_IS_SOLD + " = ?",
                new String[]{"0"},
                null, null,
                COLUMN_NAME + " ASC");

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

    public void updateItem(int id, String name, double weight, String type, String barcode) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_BARCODE, barcode);
        db.update(TABLE_ITEMS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public long insertCustomer(String name, String phone, String village) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_VILLAGE, village);
        values.put(COLUMN_DEBT, 0.0);
        Log.d("Database", "Attempting to insert new customer: " + name + ", Phone: " + phone);
        long result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
            result = db.insertWithOnConflict(TABLE_CUSTOMERS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
        if (result == -1) {
            Log.w("Database", "Customer insertion ignored or failed (Phone likely exists): " + phone);
        } else {
            Log.i("Database", "Inserted new customer ID: " + result);
        }
        return result;
    }

    public int updateCustomer(long id, String name, String village, double debt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_VILLAGE, village);
        values.put(COLUMN_DEBT, debt);
        Log.d("Database", "Updating customer ID " + id + " - Name: " + name + ", Village: " + village + ", Debt: " + debt);
        int rows = db.update(TABLE_CUSTOMERS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows <= 0) {
            Log.w("Database", "Failed to update customer ID " + id);
        }
        return rows;
    }

    public int updateCustomerDebt(long customerId, double debtChange) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("UpdateDebt", "Attempting to add debt " + debtChange + " to customer ID: " + customerId);
        String updateQuery = "UPDATE " + TABLE_CUSTOMERS +
                " SET " + COLUMN_DEBT + " = " + COLUMN_DEBT + " + ?" +
                " WHERE " + COLUMN_ID + " = ?";
        int rowsAffected = 0;
        db.beginTransaction();
        try {
            db.execSQL(updateQuery, new Object[]{debtChange, customerId});
            db.setTransactionSuccessful();
            rowsAffected = 1;
            Log.i("UpdateDebt", "Successfully updated debt for customer " + customerId + " by " + debtChange);
        } catch (Exception e) {
            Log.e("UpdateDebt", "Error updating debt for customer " + customerId, e);
            rowsAffected = 0;
        } finally {
            db.endTransaction();
        }
        return rowsAffected;
    }

    public double getCustomerDebt(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CUSTOMERS,
                    new String[]{COLUMN_DEBT},
                    COLUMN_ID + " = ?",
                    new String[]{String.valueOf(customerId)},
                    null,
                    null,
                    null,
                    "1");
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_DEBT));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0.0;
    }

    public long insertDebtUpdate(long customerId, long billId, double debtChange, double resultingBalance,
                                 double billedAmount, double paidAmount, String dueDate, String note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_DEBT_UPDATE_BILL_ID, billId > 0 ? billId : null);
        values.put(COLUMN_DEBT_UPDATE_CHANGE_AMOUNT, debtChange);
        values.put(COLUMN_DEBT_UPDATE_RESULTING_BALANCE, resultingBalance);
        values.put(COLUMN_DEBT_UPDATE_BILLED_AMOUNT, billedAmount);
        values.put(COLUMN_DEBT_UPDATE_PAID_AMOUNT, paidAmount);
        values.put(COLUMN_DEBT_UPDATE_DUE_DATE, dueDate);
        values.put(COLUMN_DEBT_UPDATE_NOTE, note);
        return db.insert(TABLE_DEBT_UPDATES, null, values);
    }

    public long insertDebtUpdateWithDate(long customerId, long billId, double debtChange, double resultingBalance,
                                         double billedAmount, double paidAmount, String dueDate, String note, String createdAt) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_DEBT_UPDATE_BILL_ID, billId > 0 ? billId : null);
        values.put(COLUMN_DEBT_UPDATE_CHANGE_AMOUNT, debtChange);
        values.put(COLUMN_DEBT_UPDATE_RESULTING_BALANCE, resultingBalance);
        values.put(COLUMN_DEBT_UPDATE_BILLED_AMOUNT, billedAmount);
        values.put(COLUMN_DEBT_UPDATE_PAID_AMOUNT, paidAmount);
        values.put(COLUMN_DEBT_UPDATE_DUE_DATE, dueDate);
        values.put(COLUMN_DEBT_UPDATE_NOTE, note);
        if (!TextUtils.isEmpty(createdAt)) {
            values.put(COLUMN_DEBT_UPDATE_CREATED_AT, createdAt);
        }
        return db.insert(TABLE_DEBT_UPDATES, null, values);
    }

    public Cursor fetchDebtUpdatesForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_DEBT_UPDATES,
                null,
                COLUMN_CUSTOMER_ID + " = ?",
                new String[]{String.valueOf(customerId)},
                null,
                null,
                "datetime(" + COLUMN_DEBT_UPDATE_CREATED_AT + ") DESC, " + COLUMN_ID + " DESC");
    }

    public String getCustomerPhoneById(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CUSTOMERS,
                    new String[]{COLUMN_PHONE},
                    COLUMN_ID + " = ?",
                    new String[]{String.valueOf(customerId)},
                    null,
                    null,
                    null,
                    "1");
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHONE));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return "";
    }

    public long insertOrGetCustomer(String name, String phone, String village) {
        SQLiteDatabase db = this.getWritableDatabase();
        long customerId = -1;
        Cursor cursor = null;
        Log.d("Database", "Checking for customer with phone: " + phone);
        try {
            cursor = db.query(TABLE_CUSTOMERS, new String[]{COLUMN_ID}, COLUMN_PHONE + " = ?", new String[]{phone}, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(COLUMN_ID);
                if (idIndex != -1) {
                    customerId = cursor.getLong(idIndex);
                    ContentValues updateValues = new ContentValues();
                    updateValues.put(COLUMN_NAME, name);
                    updateValues.put(COLUMN_VILLAGE, village);
                    db.update(TABLE_CUSTOMERS, updateValues, COLUMN_ID + " = ?", new String[]{String.valueOf(customerId)});
                    Log.d("Database", "Customer found with ID: " + customerId + ". Updated name/village from latest bill input.");
                    return customerId;
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        // If we've reached here, the customer was not found.
        Log.d("Database", "Customer not found. Inserting new customer: " + name);
        ContentValues insertValues = new ContentValues();
        insertValues.put(COLUMN_NAME, name);
        insertValues.put(COLUMN_PHONE, phone);
        insertValues.put(COLUMN_VILLAGE, village);
        insertValues.put(COLUMN_DEBT, 0.0);
        long newId = db.insert(TABLE_CUSTOMERS, null, insertValues);
        if (newId == -1) {
            Log.e("Database", "Failed to insert new customer: " + name);
        } else {
            Log.i("Database", "Inserted new customer ID: " + newId);
        }
        return newId;
    }

    public Cursor getCustomerByPhone(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching customer by phone: " + phone);
        return db.query(TABLE_CUSTOMERS, null, COLUMN_PHONE + " = ?", new String[]{phone}, null, null, null);
    }

    public Cursor fetchCustomers() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching all customers.");
        return db.query(TABLE_CUSTOMERS, null, null, null, null, null, COLUMN_NAME + " ASC");
    }

    public Cursor searchCustomersByName(String prefix, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safePrefix = prefix == null ? "" : prefix.trim();
        String safeLimit = String.valueOf(Math.max(1, limit));
        return db.query(
                TABLE_CUSTOMERS,
                new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_PHONE, COLUMN_VILLAGE, COLUMN_DEBT},
                COLUMN_NAME + " LIKE ?",
                new String[]{safePrefix + "%"},
                null,
                null,
                COLUMN_NAME + " ASC",
                safeLimit
        );
    }

    public Cursor searchCustomersByPhone(String prefix, int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safePrefix = prefix == null ? "" : prefix.trim();
        String safeLimit = String.valueOf(Math.max(1, limit));
        return db.query(
                TABLE_CUSTOMERS,
                new String[]{COLUMN_ID, COLUMN_NAME, COLUMN_PHONE, COLUMN_VILLAGE, COLUMN_DEBT},
                COLUMN_PHONE + " LIKE ?",
                new String[]{safePrefix + "%"},
                null,
                null,
                COLUMN_NAME + " ASC",
                safeLimit
        );
    }

    public Cursor fetchRecentCustomers(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String safeLimit = String.valueOf(Math.max(1, limit));
        String query = "SELECT "
                + "c." + COLUMN_ID + " AS customer_id, "
                + "c." + COLUMN_NAME + " AS name, "
                + "c." + COLUMN_PHONE + " AS phone, "
                + "c." + COLUMN_VILLAGE + " AS village, "
                + "c." + COLUMN_DEBT + " AS debt, "
                + "MAX(b." + COLUMN_BILL_DATE + ") AS last_bill_date "
                + "FROM " + TABLE_CUSTOMERS + " c "
                + "LEFT JOIN " + TABLE_BILLS + " b ON c." + COLUMN_ID + " = b." + COLUMN_CUSTOMER_ID + " "
                + "GROUP BY c." + COLUMN_ID + ", c." + COLUMN_NAME + ", c." + COLUMN_PHONE + ", c." + COLUMN_VILLAGE + ", c." + COLUMN_DEBT + " "
                + "ORDER BY CASE WHEN last_bill_date IS NULL THEN 1 ELSE 0 END, last_bill_date DESC "
                + "LIMIT " + safeLimit;
        return db.rawQuery(query, null);
    }

    public boolean deleteCustomer(int id) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check for existing bills for this customer
        try (Cursor cursor = db.query(TABLE_BILLS, new String[]{COLUMN_ID}, COLUMN_CUSTOMER_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null, "1")) {
            if (cursor != null && cursor.getCount() > 0) {
                Log.w("Database", "Attempt to delete customer ID " + id + " who has existing bills. Deletion aborted.");
                return false; // Bills exist, do not delete
            }
        }

        // If no bills, proceed with deletion
        Log.w("Database", "Deleting customer ID: " + id);
        int rows = db.delete(TABLE_CUSTOMERS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        if (rows > 0) {
            Log.i("Database", "Successfully deleted customer ID: " + id);
            return true; // Deletion successful
        } else {
            Log.w("Database", "Failed to delete customer ID " + id + " (no rows affected).");
            return false; // Deletion failed
        }
    }

    public long insertBill(long customerId, double calcGoldRate, double calcSilverRate, double finalTotalAmount, double gstPercent, String paymentMode, String paymentDetails, List<SelectedItem> items, String returnItemType, double returnItemWeight, double returnItemDeductAmount) {
        List<ReturnItem> returnItems = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (returnItemType != null && !returnItemType.isEmpty()) {
                returnItems.add(new ReturnItem(returnItemType, returnItemWeight, returnItemDeductAmount));
            }
        }
        return insertBill(customerId, calcGoldRate, calcSilverRate, finalTotalAmount, gstPercent, paymentMode, paymentDetails, items, returnItems, null, 0.0, finalTotalAmount, finalTotalAmount);
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public long insertBill(long customerId, double calcGoldRate, double calcSilverRate, double finalTotalAmount, double gstPercent, String paymentMode, String paymentDetails, List<SelectedItem> items, String returnItemType, double returnItemWeight, double returnItemDeductAmount, String debtDueDate, double debtAmount) {
        List<ReturnItem> returnItems = new ArrayList<>();
        if (returnItemType != null && !returnItemType.isEmpty()) {
            returnItems.add(new ReturnItem(returnItemType, returnItemWeight, returnItemDeductAmount));
        }
        return insertBill(customerId, calcGoldRate, calcSilverRate, finalTotalAmount, gstPercent, paymentMode, paymentDetails, items, returnItems, debtDueDate, debtAmount, finalTotalAmount, finalTotalAmount);
    }

    public long insertBill(long customerId, double calcGoldRate, double calcSilverRate, double finalTotalAmount, double gstPercent, String paymentMode, String paymentDetails, List<SelectedItem> items, List<ReturnItem> returnItems, String debtDueDate, double debtAmount, double billedAmount, double paidAmount) {
        return insertBill(null, customerId, calcGoldRate, calcSilverRate, finalTotalAmount, gstPercent, paymentMode, paymentDetails, items, returnItems, debtDueDate, debtAmount, billedAmount, paidAmount);
    }

    public long insertBill(Integer explicitBillId, long customerId, double calcGoldRate, double calcSilverRate, double finalTotalAmount, double gstPercent, String paymentMode, String paymentDetails, List<SelectedItem> items, List<ReturnItem> returnItems, String debtDueDate, double debtAmount, double billedAmount, double paidAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (explicitBillId != null) {
            values.put(COLUMN_ID, explicitBillId);
        }
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_CALCULATED_GOLD_RATE, calcGoldRate);
        values.put(COLUMN_CALCULATED_SILVER_RATE, calcSilverRate);
        values.put(COLUMN_TOTAL_AMOUNT, finalTotalAmount);
        values.put(COLUMN_BILLED_AMOUNT, billedAmount);
        values.put(COLUMN_PAID_AMOUNT, paidAmount);
        values.put(COLUMN_GST_PERCENT, gstPercent);
        values.put(COLUMN_PAYMENT_MODE, paymentMode);
        values.put(COLUMN_PAYMENT_DETAILS, paymentDetails);

        // Keep single return item columns for backward compatibility if needed, or set to null
        if (returnItems != null && !returnItems.isEmpty()) {
            values.put(COLUMN_RETURN_ITEM_TYPE, returnItems.get(0).getType());
            values.put(COLUMN_RETURN_ITEM_WEIGHT, returnItems.get(0).getWeight());
            values.put(COLUMN_RETURN_ITEM_DEDUCT_AMOUNT, returnItems.get(0).getDeductAmount());
        }

        values.put(COLUMN_DEBT_DUE_DATE, debtDueDate);
        values.put(COLUMN_DEBT_AMOUNT, debtAmount);

        Log.d("Database", "Inserting bill for customer ID: " + customerId + ", Amount: " + finalTotalAmount + ", GST: " + gstPercent + "%, Payment Mode: " + paymentMode);
        long billId = db.insert(TABLE_BILLS, null, values);

        if (billId != -1) {
            Log.i("Database", "Inserted bill with ID: " + billId);
            if (items != null && !items.isEmpty()) {
                Log.d("Database", "Inserting " + items.size() + " items for bill ID: " + billId);
                for (SelectedItem item : items) {
                    insertBillItem(billId, item.getId());
                }
            }

            if (returnItems != null && !returnItems.isEmpty()) {
                Log.d("Database", "Inserting " + returnItems.size() + " return items for bill ID: " + billId);
                for (ReturnItem returnItem : returnItems) {
                    insertBillReturnItem(billId, returnItem);
                }
            }
        } else {
            Log.e("Database", "Failed to insert bill for customer ID: " + customerId);
        }
        return billId;
    }

    public void insertBillReturnItem(long billId, ReturnItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BILL_ID, billId);
        values.put(COLUMN_RETURN_TYPE, item.getType());
        values.put(COLUMN_RETURN_WEIGHT, item.getWeight());
        values.put(COLUMN_RETURN_DEDUCT_AMOUNT, item.getDeductAmount());
        db.insert(TABLE_BILL_RETURN_ITEMS, null, values);
    }

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

    public Cursor fetchBillHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching bill history summary.");
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +
                "c." + COLUMN_NAME + ", " +
                "b." + COLUMN_BILL_DATE + ", " +
            "b." + COLUMN_TOTAL_AMOUNT + ", " +
            "b." + COLUMN_DEBT_AMOUNT +
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " ORDER BY b." + COLUMN_ID + " DESC";
        return db.rawQuery(query, null);
    }

    public String getBillIdsForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<String> billIds = new ArrayList<>();
        Log.d("Database", "Fetching bill IDs for customer ID: " + customerId);
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

    public Cursor getBillDetails(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching details for bill ID: " + billId);
        String query = "SELECT " +
                "b." + COLUMN_ID + ", " +
                "b." + COLUMN_CUSTOMER_ID + ", " +
                "c." + COLUMN_NAME + ", " +
                "c." + COLUMN_PHONE + ", " +
                "b." + COLUMN_BILL_DATE + ", " +
                "b." + COLUMN_CALCULATED_GOLD_RATE + ", " +
                "b." + COLUMN_CALCULATED_SILVER_RATE + ", " +
                "b." + COLUMN_TOTAL_AMOUNT + ", " +
                "b." + COLUMN_BILLED_AMOUNT + ", " +
                "b." + COLUMN_PAID_AMOUNT + ", " +
                "b." + COLUMN_GST_PERCENT + ", " +
                "b." + COLUMN_PAYMENT_MODE + ", " +
                "b." + COLUMN_PAYMENT_DETAILS + ", " +
                "b." + COLUMN_RETURN_ITEM_TYPE + ", " +
                "b." + COLUMN_RETURN_ITEM_WEIGHT + ", " +
                "b." + COLUMN_RETURN_ITEM_DEDUCT_AMOUNT + ", " +
                "b." + COLUMN_DEBT_DUE_DATE + ", " +
                "b." + COLUMN_DEBT_AMOUNT +
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " WHERE b." + COLUMN_ID + " = ?";

        String[] selectionArgs = {String.valueOf(billId)};
        return db.rawQuery(query, selectionArgs);
    }

    public Cursor getItemsForBill(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d("Database", "Fetching items for bill ID: " + billId);
        String query = "SELECT " +
                "i." + COLUMN_ID + ", " +
                "i." + COLUMN_NAME + ", " +
                "i." + COLUMN_WEIGHT + ", " +
                "i." + COLUMN_TYPE +
                " FROM " + TABLE_BILL_ITEMS + " bi" +
                " JOIN " + TABLE_ITEMS + " i ON bi." + COLUMN_ITEM_ID + " = i." + COLUMN_ID +
                " WHERE bi." + COLUMN_BILL_ID + " = ?";

        String[] selectionArgs = {String.valueOf(billId)};
        Cursor cursor = db.rawQuery(query, selectionArgs);
        if (cursor != null) {
            Log.d("Database", "Found " + cursor.getCount() + " items for bill ID: " + billId);
        } else {
            Log.w("Database", "Cursor is null when fetching items for bill ID: " + billId);
        }
        return cursor;
    }

    public Cursor getReturnItemsForBill(long billId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " +
                COLUMN_RETURN_TYPE + ", " +
                COLUMN_RETURN_WEIGHT + ", " +
                COLUMN_RETURN_DEDUCT_AMOUNT +
                " FROM " + TABLE_BILL_RETURN_ITEMS +
                " WHERE " + COLUMN_BILL_ID + " = ?";
        return db.rawQuery(query, new String[]{String.valueOf(billId)});
    }

    public void clearAllDataForImport(SQLiteDatabase db) {
        db.delete(TABLE_BILL_ITEMS, null, null);
        db.delete(TABLE_BILLS, null, null);
        db.delete(TABLE_CUSTOMERS, null, null);
        db.delete(TABLE_ITEMS, null, null);
        db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('" + TABLE_BILL_ITEMS + "','" + TABLE_BILLS + "','" + TABLE_CUSTOMERS + "','" + TABLE_ITEMS + "')");
    }

    public long upsertCustomerForImport(SQLiteDatabase db, String name, String phone, String village, double debt) {
        String safePhone = phone == null ? "" : phone.trim();
        String safeName = name == null ? "Unknown" : name.trim();
        String safeVillage = village == null ? "" : village.trim();

        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_CUSTOMERS, new String[]{COLUMN_ID}, COLUMN_PHONE + " = ?", new String[]{safePhone}, null, null, null, "1");
            if (cursor != null && cursor.moveToFirst()) {
                long existingId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                ContentValues updateValues = new ContentValues();
                updateValues.put(COLUMN_NAME, safeName);
                updateValues.put(COLUMN_VILLAGE, safeVillage);
                updateValues.put(COLUMN_DEBT, debt);
                db.update(TABLE_CUSTOMERS, updateValues, COLUMN_ID + " = ?", new String[]{String.valueOf(existingId)});
                return existingId;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, safeName);
        values.put(COLUMN_PHONE, safePhone);
        values.put(COLUMN_VILLAGE, safeVillage);
        values.put(COLUMN_DEBT, debt);
        return db.insert(TABLE_CUSTOMERS, null, values);
    }

    public long insertItemForImport(SQLiteDatabase db, Integer explicitId, String name, double weight, String type, boolean isSold) {
        ContentValues values = new ContentValues();
        if (explicitId != null) {
            values.put(COLUMN_ID, explicitId);
        }
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_TYPE, type);
        values.put(COLUMN_IS_SOLD, isSold ? 1 : 0);
        return db.insert(TABLE_ITEMS, null, values);
    }

    public long insertBillForImport(SQLiteDatabase db,
                                    Integer explicitBillId,
                                    long customerId,
                                    double totalAmount,
                                    double gstPercent,
                                    String paymentMode,
                                    String paymentDetails,
                                    String billDate) {
        ContentValues values = new ContentValues();
        if (explicitBillId != null) {
            values.put(COLUMN_ID, explicitBillId);
        }
        values.put(COLUMN_CUSTOMER_ID, customerId);
        values.put(COLUMN_CALCULATED_GOLD_RATE, 0.0);
        values.put(COLUMN_CALCULATED_SILVER_RATE, 0.0);
        values.put(COLUMN_TOTAL_AMOUNT, totalAmount);
        values.put(COLUMN_BILLED_AMOUNT, totalAmount);
        values.put(COLUMN_PAID_AMOUNT, totalAmount);
        values.put(COLUMN_GST_PERCENT, gstPercent);
        values.put(COLUMN_PAYMENT_MODE, paymentMode == null ? "Cash" : paymentMode);
        values.put(COLUMN_PAYMENT_DETAILS, paymentDetails == null ? "" : paymentDetails);
        if (!TextUtils.isEmpty(billDate)) {
            values.put(COLUMN_BILL_DATE, billDate);
        }
        values.put(COLUMN_RETURN_ITEM_TYPE, (String) null);
        values.put(COLUMN_RETURN_ITEM_WEIGHT, 0.0);
        values.put(COLUMN_RETURN_ITEM_DEDUCT_AMOUNT, 0.0);
        values.put(COLUMN_DEBT_DUE_DATE, (String) null);
        values.put(COLUMN_DEBT_AMOUNT, 0.0);
        return db.insert(TABLE_BILLS, null, values);
    }

    public Cursor fetchCustomersWithDebt() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_CUSTOMERS, null, COLUMN_DEBT + " > ?", new String[]{"0"}, null, null, COLUMN_DEBT + " DESC");
    }

    public Cursor fetchDueDebtBills(String dueDateIso) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT b." + COLUMN_ID + " AS bill_id, c." + COLUMN_ID + " AS customer_id, c." + COLUMN_NAME + " AS name, c." + COLUMN_PHONE + " AS phone, b." + COLUMN_DEBT_AMOUNT + " AS debt_amount, b." + COLUMN_DEBT_DUE_DATE + " AS debt_due_date" +
                " FROM " + TABLE_BILLS + " b" +
                " JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID +
                " WHERE b." + COLUMN_DEBT_AMOUNT + " > 0 AND b." + COLUMN_DEBT_DUE_DATE + " = ?";
        return db.rawQuery(query, new String[]{dueDateIso});
    }

    public boolean hasRecentSimilarBillByPhone(String phone, double billedAmount, int minutesWindow) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            String query = "SELECT COUNT(1) FROM " + TABLE_BILLS + " b "
                    + "JOIN " + TABLE_CUSTOMERS + " c ON b." + COLUMN_CUSTOMER_ID + " = c." + COLUMN_ID + " "
                    + "WHERE c." + COLUMN_PHONE + " = ? "
                    + "AND ABS(b." + COLUMN_BILLED_AMOUNT + " - ?) <= 0.01 "
                    + "AND datetime(b." + COLUMN_BILL_DATE + ") >= datetime('now', ?)";
            String windowExpr = "-" + Math.max(1, minutesWindow) + " minutes";
            cursor = db.rawQuery(query, new String[]{phone, String.valueOf(billedAmount), windowExpr});
            return cursor != null && cursor.moveToFirst() && cursor.getInt(0) > 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public boolean hasNotificationSentForBill(int billId, String type, String notifiedDateIso) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NOTIFICATION_HISTORY,
                    new String[]{COLUMN_ID},
                    COLUMN_BILL_ID + " = ? AND " + COLUMN_NOTIFICATION_TYPE + " = ? AND " + COLUMN_NOTIFICATION_NOTIFIED_DATE + " = ?",
                    new String[]{String.valueOf(billId), type, notifiedDateIso},
                    null, null, null, "1");
            return cursor != null && cursor.moveToFirst();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public long insertNotificationHistory(int billId, String customerName, String message, String type, String notifiedDateIso) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_BILL_ID, billId);
        values.put(COLUMN_NOTIFICATION_CUSTOMER_NAME, customerName);
        values.put(COLUMN_NOTIFICATION_MESSAGE, message);
        values.put(COLUMN_NOTIFICATION_TYPE, type);
        values.put(COLUMN_NOTIFICATION_NOTIFIED_DATE, notifiedDateIso);
        return db.insert(TABLE_NOTIFICATION_HISTORY, null, values);
    }

    public Cursor fetchNotificationHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_NOTIFICATION_HISTORY, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    public void clearOldNotifications(int days) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = "datetime(" + COLUMN_NOTIFICATION_CREATED_AT + ") < datetime('now', '-" + days + " days')";
        db.delete(TABLE_NOTIFICATION_HISTORY, where, null);
    }

    public void clearAllNotifications() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NOTIFICATION_HISTORY, null, null);
    }

    public Cursor fetchDebtCustomerDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT "
                + "c." + COLUMN_ID + " AS customer_id, "
                + "c." + COLUMN_NAME + " AS customer_name, "
                + "c." + COLUMN_PHONE + " AS customer_phone, "
                + "c." + COLUMN_VILLAGE + " AS customer_village, "
                + "c." + COLUMN_DEBT + " AS customer_total_debt, "
                + "SUM(CASE WHEN b." + COLUMN_DEBT_AMOUNT + " > 0 THEN b." + COLUMN_DEBT_AMOUNT + " ELSE 0 END) AS active_bill_debt, "
                + "MIN(CASE WHEN b." + COLUMN_DEBT_AMOUNT + " > 0 THEN b." + COLUMN_DEBT_DUE_DATE + " END) AS nearest_due_date, "
                + "MAX(b." + COLUMN_BILL_DATE + ") AS last_bill_date "
                + "FROM " + TABLE_CUSTOMERS + " c "
                + "LEFT JOIN " + TABLE_BILLS + " b ON c." + COLUMN_ID + " = b." + COLUMN_CUSTOMER_ID + " "
                + "WHERE c." + COLUMN_DEBT + " != 0 "
                + "GROUP BY c." + COLUMN_ID + ", c." + COLUMN_NAME + ", c." + COLUMN_PHONE + ", c." + COLUMN_VILLAGE + ", c." + COLUMN_DEBT + " "
                + "ORDER BY ABS(c." + COLUMN_DEBT + ") DESC";
        return db.rawQuery(query, null);
    }

    public long insertBillItemForImport(SQLiteDatabase db, long billId, int itemId) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_BILL_ID, billId);
        values.put(COLUMN_ITEM_ID, itemId);
        return db.insert(TABLE_BILL_ITEMS, null, values);
    }

    public long insertSupplier(String name, String phone, String address) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_PHONE, phone);
        values.put(COLUMN_SUPPLIER_ADDRESS, address);
        return db.insert(TABLE_SUPPLIERS, null, values);
    }

    public long insertPurchaseBill(int supplierId, double totalAmount, String paymentDetails) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SUPPLIER_ID, supplierId);
        values.put(COLUMN_TOTAL_AMOUNT, totalAmount);
        values.put(COLUMN_PAYMENT_DETAILS, paymentDetails);
        return db.insert(TABLE_PURCHASE_BILLS, null, values);
    }

    public Cursor fetchSuppliers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_SUPPLIERS, null, null, null, null, null, COLUMN_NAME + " ASC");
    }

    public Cursor fetchPurchaseHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT p.*, s." + COLUMN_NAME + " AS supplier_name FROM " + TABLE_PURCHASE_BILLS + " p " +
                "JOIN " + TABLE_SUPPLIERS + " s ON p." + COLUMN_SUPPLIER_ID + " = s." + COLUMN_ID + " " +
                "ORDER BY p." + COLUMN_PURCHASE_DATE + " DESC";
        return db.rawQuery(query, null);
    }

    public void deleteBill(int billId) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Start a transaction for safety
        db.beginTransaction();
        try {
            // 1. Delete linked items from the bill_items table
            db.delete(TABLE_BILL_ITEMS, COLUMN_BILL_ID + " = ?", new String[]{String.valueOf(billId)});

            // 2. Delete the bill itself from the bills table
            db.delete(TABLE_BILLS, COLUMN_ID + " = ?", new String[]{String.valueOf(billId)});

            db.setTransactionSuccessful();
            Log.i("Database", "Successfully deleted bill ID: " + billId);
        } catch (Exception e) {
            Log.e("Database", "Error deleting bill ID: " + billId, e);
        } finally {
            db.endTransaction();
        }
    }

    public long insertExpense(String title, double amount, String category, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPENSE_TITLE, title);
        values.put(COLUMN_EXPENSE_AMOUNT, amount);
        values.put(COLUMN_EXPENSE_CATEGORY, category);
        values.put(COLUMN_EXPENSE_DATE, date);
        return db.insert(TABLE_EXPENSES, null, values);
    }

    public Cursor fetchExpenses() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_EXPENSES, null, null, null, null, null, COLUMN_EXPENSE_DATE + " DESC, " + COLUMN_ID + " DESC");
    }

    public void deleteExpense(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_EXPENSES, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public Cursor fetchAllTransactionsForCustomer(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 'bill' as type, " + COLUMN_ID + " as id, " + COLUMN_BILL_DATE + " as date, " + COLUMN_TOTAL_AMOUNT + " as amount, " + COLUMN_PAID_AMOUNT + " as paid, " + COLUMN_DEBT_AMOUNT + " as balance, '' as note " +
                "FROM " + TABLE_BILLS + " WHERE " + COLUMN_CUSTOMER_ID + " = ? " +
                "UNION ALL " +
                "SELECT 'debt_update' as type, " + COLUMN_ID + " as id, " + COLUMN_DEBT_UPDATE_CREATED_AT + " as date, " + COLUMN_DEBT_UPDATE_CHANGE_AMOUNT + " as amount, " + COLUMN_DEBT_UPDATE_PAID_AMOUNT + " as paid, " + COLUMN_DEBT_UPDATE_RESULTING_BALANCE + " as balance, " + COLUMN_DEBT_UPDATE_NOTE + " as note " +
                "FROM " + TABLE_DEBT_UPDATES + " WHERE " + COLUMN_CUSTOMER_ID + " = ? " +
                "ORDER BY date DESC, id DESC";
        return db.rawQuery(query, new String[]{String.valueOf(customerId), String.valueOf(customerId)});
    }
}
