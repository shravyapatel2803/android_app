package com.example.billgenerator.models;

public class item_recycler_model_stocks {
    int id;
    String name;
    double weight;
    String type; // "Gold" or "Silver"
    String barcode;
    public boolean isSold; // <-- MAKE SURE this is public OR add a public setSold(boolean sold) method

    // Updated Constructor
    public item_recycler_model_stocks(int id, String name, double weight, String type, String barcode, boolean isSold) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
        this.barcode = barcode;
        this.isSold = isSold; // Add to constructor
    }

    public item_recycler_model_stocks(int id, String name, double weight, String type, boolean isSold) {
        this(id, name, weight, type, null, isSold);
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public double getWeight() { return weight; }
    public String getType() { return type; }
    public String getBarcode() { return barcode; }
    public boolean isSold() { return isSold; } // NEW GETTER
}
