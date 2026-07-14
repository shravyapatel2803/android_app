package com.example.billgenerator.models;

public class BillHistoryModel {
    public final int billId;
    public final String customerName;
    public final String billDate;
    public final String rawBillDate;
    public final double totalAmount;
    public final double debtAmount;

    public BillHistoryModel(int billId, String customerName, String billDate, double totalAmount) {
        this(billId, customerName, billDate, billDate, totalAmount, 0.0);
    }

    public BillHistoryModel(int billId, String customerName, String billDate, String rawBillDate, double totalAmount, double debtAmount) {
        this.billId = billId;
        this.customerName = customerName;
        this.billDate = billDate;
        this.rawBillDate = rawBillDate;
        this.totalAmount = totalAmount;
        this.debtAmount = debtAmount;
    }
}
