package com.example.billgenerator.models;

/**
 * Model class for an item that has been selected to be part of a new bill.
 * This is used by GenerateBillFragment.
 */
public class SelectedItem {
    private int id;
    private String name;
    private double weight;
    private String type;

    public SelectedItem(int id, String name, double weight, String type) {
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