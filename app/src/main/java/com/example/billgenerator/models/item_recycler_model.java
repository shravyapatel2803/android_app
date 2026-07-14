package com.example.billgenerator.models;

public class item_recycler_model { // <-- FIXED: Added public
    public int id; // <-- FIXED: Added public
    public String name,weight,type; // <-- FIXED: Added public

    // <-- FIXED: Added public
    public item_recycler_model(int id, String name, String weight, String type){
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
    }
}