package com.example.billgenerator.database.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomerDao {
    @Query("SELECT * FROM customer ORDER BY name ASC")
    List<CustomerEntity> getAll();

    @Query("SELECT * FROM customer WHERE id = :id")
    CustomerEntity getById(int id);

    @Query("SELECT * FROM customer WHERE phone = :phone LIMIT 1")
    CustomerEntity getByPhone(String phone);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(CustomerEntity customer);

    @Update
    void update(CustomerEntity customer);

    @Delete
    void delete(CustomerEntity customer);

    @Query("SELECT SUM(debt) FROM customer")
    double getTotalDebt();
}
