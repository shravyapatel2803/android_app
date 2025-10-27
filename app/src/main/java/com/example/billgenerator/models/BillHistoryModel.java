package com.example.billgenerator.models;

public class BillHistoryModel {
    public final int billId;
    public final String customerName;
    final String billDate;
    final double totalAmount;

    public BillHistoryModel(int billId, String customerName, String billDate, double totalAmount) {
        this.billId = billId;
        this.customerName = customerName;
        this.billDate = billDate;
        this.totalAmount = totalAmount;
    }
}
