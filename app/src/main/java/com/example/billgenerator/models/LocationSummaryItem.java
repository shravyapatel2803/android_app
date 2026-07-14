package com.example.billgenerator.models;

public class LocationSummaryItem {
    public final String village;
    public final int customerCount;
    public final double totalDebt;
    public final double latitude;
    public final double longitude;

    public LocationSummaryItem(String village, int customerCount, double totalDebt, double latitude, double longitude) {
        this.village = village;
        this.customerCount = customerCount;
        this.totalDebt = totalDebt;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
