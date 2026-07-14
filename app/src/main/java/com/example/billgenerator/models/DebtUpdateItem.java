package com.example.billgenerator.models;

public class DebtUpdateItem {
    public final long id;
    public final long billId;
    public final double debtChange;
    public final double resultingBalance;
    public final double billedAmount;
    public final double paidAmount;
    public final String dueDate;
    public final String note;
    public final String createdAt;

    public DebtUpdateItem(long id, long billId, double debtChange, double resultingBalance, double billedAmount,
                          double paidAmount, String dueDate, String note, String createdAt) {
        this.id = id;
        this.billId = billId;
        this.debtChange = debtChange;
        this.resultingBalance = resultingBalance;
        this.billedAmount = billedAmount;
        this.paidAmount = paidAmount;
        this.dueDate = dueDate;
        this.note = note;
        this.createdAt = createdAt;
    }
}
