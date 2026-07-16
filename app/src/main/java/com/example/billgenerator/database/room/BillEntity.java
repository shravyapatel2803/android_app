package com.example.billgenerator.database.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "bills",
    foreignKeys = @ForeignKey(
        entity = CustomerEntity.class,
        parentColumns = "id",
        childColumns = "customer_id",
        onDelete = ForeignKey.CASCADE
    )
)
public class BillEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "customer_id")
    public int customerId;

    @ColumnInfo(name = "calc_gold_rate", defaultValue = "0.0")
    public double calcGoldRate;

    @ColumnInfo(name = "calc_silver_rate", defaultValue = "0.0")
    public double calcSilverRate;

    @ColumnInfo(name = "total_amount")
    public double totalAmount;

    @ColumnInfo(name = "billed_amount", defaultValue = "0.0")
    public double billedAmount;

    @ColumnInfo(name = "paid_amount", defaultValue = "0.0")
    public double paidAmount;

    @ColumnInfo(name = "gst_percent", defaultValue = "0.0")
    public double gstPercent;

    @ColumnInfo(name = "payment_mode")
    public String paymentMode;

    @ColumnInfo(name = "payment_details")
    public String paymentDetails;

    @ColumnInfo(name = "bill_date", defaultValue = "CURRENT_TIMESTAMP")
    public String billDate;

    @ColumnInfo(name = "debt_due_date")
    public String debtDueDate;

    @ColumnInfo(name = "debt_amount", defaultValue = "0.0")
    public double debtAmount;
}
