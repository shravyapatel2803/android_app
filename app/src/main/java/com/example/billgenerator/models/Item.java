package com.example.billgenerator.models;

/**
 * Model class for a single item in the inventory (stock).
 * This is used by StockManagementFragment and GenerateBillFragment.
 */
public class Item {
    private int id;
    private String name;
    private double weight;
    private String type;
    private String barcode;

    public Item(int id, String name, double weight, String type) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
    }

    public Item(int id, String name, double weight, String type, String barcode) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
        this.barcode = barcode;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getWeight() {
        return weight;
    }

    public String getType() {
        return type;
    }

    public String getBarcode() {
        return barcode;
    }
}
