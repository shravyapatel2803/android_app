package com.example.billgenerator;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
//    Button generateBill = findViewById(R.id.generateBill);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
//        generateBill.setOnClickListener(view -> {
//            Intent intent = new Intent(MainActivity.this, BillGenerator.class);
//            startActivity(intent);
//        });
        Button generateBill = findViewById(R.id.generateBill);
        generateBill.setOnClickListener(v -> {
            Intent intent = new Intent(this, BillGenerator.class);
            startActivity(intent);
        });
        Button stockManagementButton = findViewById(R.id.stockManagementButton);
        stockManagementButton.setOnClickListener(v -> {
            Toast.makeText(this, "opened", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, Maintain.class);
            startActivity(intent);
        });
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Home Screen");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

//    @Override
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId(); // Get item ID once

        if (itemId == R.id.generateBillMenu) {
            Intent intent = new Intent(MainActivity.this, BillGenerator.class);
            startActivity(intent);
            return true; // Return true to indicate event was handled
        } else if (itemId == R.id.stockManagementMenu) {
            Intent intent = new Intent(MainActivity.this, Maintain.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.billHistoryMenu) { // Handle Bill History click
            Intent intent = new Intent(MainActivity.this, BillHistoryActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.customerDetailsMenu) { // Handle Customer Details click
            Intent intent = new Intent(MainActivity.this, customer_details_activity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}