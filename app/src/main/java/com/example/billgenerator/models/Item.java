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

    public Item(int id, String name, double weight, String type) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
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
}