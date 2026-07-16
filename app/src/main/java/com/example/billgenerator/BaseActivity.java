package com.example.billgenerator;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.billgenerator.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply Language
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply Font Size Theme
        applyFontSizeTheme();
        super.onCreate(savedInstanceState);
    }

    private void applyFontSizeTheme() {
        SharedPreferences prefs = getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE);
        String fontSize = prefs.getString("font_size", "Normal");

        switch (fontSize) {
            case "Small":
                setTheme(R.style.Theme_BillGenerator_Small);
                break;
            case "Large":
                setTheme(R.style.Theme_BillGenerator_Large);
                break;
            case "Extra Large":
                setTheme(R.style.Theme_BillGenerator_ExtraLarge);
                break;
            default:
                setTheme(R.style.Theme_BillGenerator);
                break;
        }
    }
}
