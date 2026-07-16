package com.example.billgenerator.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.billgenerator.R;
import com.example.billgenerator.utils.LocaleHelper;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    private Spinner appLanguageSpinner, pdfLanguageSpinner, fontSizeSpinner;
    private SwitchMaterial pdcSwitch, photoSwitch;
    private Button saveButton;
    private SharedPreferences sharedPrefs;

    private static final String PREFS_NAME = "app_settings_prefs";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sharedPrefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        appLanguageSpinner = view.findViewById(R.id.spinner_app_language);
        pdfLanguageSpinner = view.findViewById(R.id.spinner_pdf_language);
        fontSizeSpinner = view.findViewById(R.id.spinner_font_size);
        pdcSwitch = view.findViewById(R.id.switch_enable_pdc);
        photoSwitch = view.findViewById(R.id.switch_customer_photo);
        saveButton = view.findViewById(R.id.btn_save_settings);

        setupSpinners();
        loadSettings();

        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void setupSpinners() {
        String[] languages = {"English", "Hindi", "Gujarati"};
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages);
        appLanguageSpinner.setAdapter(langAdapter);
        pdfLanguageSpinner.setAdapter(langAdapter);

        String[] sizes = {"Small", "Normal", "Large", "Extra Large"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sizes);
        fontSizeSpinner.setAdapter(sizeAdapter);
    }

    private void loadSettings() {
        appLanguageSpinner.setSelection(getLanguageIndex(sharedPrefs.getString("app_language", "English")));
        pdfLanguageSpinner.setSelection(getLanguageIndex(sharedPrefs.getString("pdf_language", "English")));
        
        String fontSize = sharedPrefs.getString("font_size", "Normal");
        int sizeIndex = 1;
        if ("Small".equals(fontSize)) sizeIndex = 0;
        else if ("Large".equals(fontSize)) sizeIndex = 2;
        else if ("Extra Large".equals(fontSize)) sizeIndex = 3;
        fontSizeSpinner.setSelection(sizeIndex);

        pdcSwitch.setChecked(sharedPrefs.getBoolean("enable_pdc", true));
        photoSwitch.setChecked(sharedPrefs.getBoolean("enable_customer_photo", true));
    }

    private int getLanguageIndex(String lang) {
        if ("Hindi".equals(lang)) return 1;
        if ("Gujarati".equals(lang)) return 2;
        return 0;
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        String appLang = appLanguageSpinner.getSelectedItem().toString();
        editor.putString("app_language", appLang);
        editor.putString("pdf_language", pdfLanguageSpinner.getSelectedItem().toString());
        editor.putString("font_size", fontSizeSpinner.getSelectedItem().toString());
        editor.putBoolean("enable_pdc", pdcSwitch.isChecked());
        editor.putBoolean("enable_customer_photo", photoSwitch.isChecked());
        editor.apply();

        // Apply locale immediately
        LocaleHelper.setLocale(requireContext(), appLang);

        Toast.makeText(getContext(), "Settings saved. Restart recommended.", Toast.LENGTH_LONG).show();
        
        // Recreate activity to apply theme/locale
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }
}
