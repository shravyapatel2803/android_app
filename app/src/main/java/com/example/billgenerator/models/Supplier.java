package com.example.billgenerator.models;

public class Supplier {
    private int id;
    private String name;
    private String phone;
    private String address;

    public Supplier(int id, String name, String phone, String address) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}
