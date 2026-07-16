package com.example.billgenerator.models;

public class DraftItem {
    public int id;
    public String name, jsonData, date;

    public DraftItem(int id, String name, String jsonData, String date) {
        this.id = id;
        this.name = name;
        this.jsonData = jsonData;
        this.date = date;
    }
}
