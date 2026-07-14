package com.example.billgenerator.models;

public class NotificationHistoryItem {
    public final int billId;
    public final String customerName;
    public final String message;
    public final String type;
    public final String notifiedDate;
    public final String createdAt;

    public NotificationHistoryItem(int billId, String customerName, String message, String type, String notifiedDate, String createdAt) {
        this.billId = billId;
        this.customerName = customerName;
        this.message = message;
        this.type = type;
        this.notifiedDate = notifiedDate;
        this.createdAt = createdAt;
    }
}
