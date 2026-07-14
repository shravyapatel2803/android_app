package com.example.billgenerator.models;

public class customer_recycler_model {
    public int id;
    public String name, phone, village;
    public double debt; // <-- Added Debt field

    // <-- Updated Constructor -->
    public customer_recycler_model(int id, String name, String phone, String village, double debt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.village = village;
        this.debt = debt; // <-- Initialize Debt
    }
}