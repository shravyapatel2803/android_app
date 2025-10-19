package com.example.billgenerator;

public class BillHistoryModel {
    final int billId;
    final String customerName;
    final String billDate;
    final double totalAmount;

    public BillHistoryModel(int billId, String customerName, String billDate, double totalAmount) {
        this.billId = billId;
        this.customerName = customerName;
        this.billDate = billDate;
        this.totalAmount = totalAmount;
    }
}
