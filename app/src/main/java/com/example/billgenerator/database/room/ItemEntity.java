package com.example.billgenerator.database.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "items")
public class ItemEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "weight")
    public double weight;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "barcode")
    public String barcode;

    @ColumnInfo(name = "is_sold", defaultValue = "0")
    public int isSold;
}
