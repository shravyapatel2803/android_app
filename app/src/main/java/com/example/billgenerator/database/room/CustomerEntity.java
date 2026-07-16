package com.example.billgenerator.database.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "customer", indices = {@Index(value = {"phone"}, unique = true)})
public class CustomerEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "village")
    public String village;

    @ColumnInfo(name = "debt", defaultValue = "0.0")
    public double debt;
}
