package com.example.billgenerator.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    private static final NumberFormat indianCurrencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    static {
        // Remove the currency symbol (₹) from the format for use in EditText
        if (indianCurrencyFormat instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) indianCurrencyFormat;
            df.setPositivePrefix("");
            df.setNegativePrefix("-");
        }
    }

    public static String formatIndianCurrency(double amount) {
        return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(amount);
    }

    public static String formatAmountWithoutSymbol(double amount) {
        return indianCurrencyFormat.format(amount).trim();
    }

    public static void setupAmountFormatter(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (cleanString.length() > 0) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            // We use a custom formatting approach for Indian style because default NumberFormat 
                            // might not handle it perfectly in all Android versions for real-time EditText
                            String formatted = formatWithIndianCommas(cleanString);
                            current = formatted;
                            editText.setText(formatted);
                            editText.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    } else {
                        current = "";
                        editText.setText("");
                    }

                    editText.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private static String formatWithIndianCommas(String cleanString) {
        if (cleanString == null || cleanString.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder(cleanString);
        int n = sb.length();
        
        // Indian numbering system: 12,34,56,789
        // Last group is 3 digits, others are 2 digits
        
        if (n <= 3) return cleanString;
        
        sb.insert(n - 3, ",");
        int pos = n - 3;
        
        while (pos > 2) {
            pos -= 2;
            sb.insert(pos, ",");
        }
        
        return sb.toString();
    }
}
