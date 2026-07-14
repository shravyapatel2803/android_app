package com.example.billgenerator.models;

public class ReturnItem {
    private String type;
    private double weight;
    private double deductAmount;

    public ReturnItem(String type, double weight, double deductAmount) {
        this.type = type;
        this.weight = weight;
        this.deductAmount = deductAmount;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getDeductAmount() {
        return deductAmount;
    }

    public void setDeductAmount(double deductAmount) {
        this.deductAmount = deductAmount;
    }
}
