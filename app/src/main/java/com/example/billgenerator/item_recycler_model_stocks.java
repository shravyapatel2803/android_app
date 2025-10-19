package com.example.billgenerator;

public class item_recycler_model_stocks {
    int id;
    String name;
    double weight;
    String type; // "Gold" or "Silver"
    boolean isSold; // NEW FIELD

    // Updated Constructor
    public item_recycler_model_stocks(int id, String name, double weight, String type, boolean isSold) {
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
        this.isSold = isSold; // Add to constructor
    }

    // --- Getters ---
    public int getId() { return id; }
    public String getName() { return name; }
    public double getWeight() { return weight; }
    public String getType() { return type; }
    public boolean isSold() { return isSold; } // NEW GETTER
}
