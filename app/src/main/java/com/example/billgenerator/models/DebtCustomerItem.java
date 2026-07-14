package com.example.billgenerator.models;

public class DebtCustomerItem {
    public final int customerId;
    public final String name;
    public final String phone;
    public final String village;
    public final double totalDebt;
    public final double activeBillDebt;
    public final String nearestDueDate;
    public final String lastBillDate;

    public DebtCustomerItem(int customerId, String name, String phone, String village, double totalDebt, double activeBillDebt, String nearestDueDate, String lastBillDate) {
        this.customerId = customerId;
        this.name = name;
        this.phone = phone;
        this.village = village;
        this.totalDebt = totalDebt;
        this.activeBillDebt = activeBillDebt;
        this.nearestDueDate = nearestDueDate;
        this.lastBillDate = lastBillDate;
    }
}
