package com.example.billgenerator.models;

public class item_recycler_model {
    int id;
    String name,weight,type;
    item_recycler_model(int id, String name, String weight, String type){
        this.id = id;
        this.name = name;
        this.weight = weight;
        this.type = type;
    }
}