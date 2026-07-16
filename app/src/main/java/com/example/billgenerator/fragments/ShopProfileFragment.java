package com.example.billgenerator.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.billgenerator.R;
import com.example.billgenerator.databinding.FragmentShopProfileBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ShopProfileFragment extends Fragment {

    public static final String PREFS_NAME = "shop_profile_prefs";
    public static final String KEY_SHOP_NAME = "shop_name";
    public static final String KEY_OWNER_NAME = "owner_name";
    public static final String KEY_PHONE = "phone";
    public static final String KEY_WHATSAPP = "whatsapp";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_GSTIN = "gstin";
    public static final String KEY_TAGLINE = "tagline";
    public static final String KEY_NOTES = "notes";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_WHATSAPP_NOTE = "whatsapp_note";
    public static final String KEY_LOGO_URI = "shop_logo_uri";

    private FragmentShopProfileBinding binding;
    private SharedPreferences preferences;
    private Uri selectedLogoUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    Uri internalUri = copyUriToInternalStorage(uri);
                    if (internalUri != null) {
                        selectedLogoUri = internalUri;
                        if (binding.profileLogoPreview != null) binding.profileLogoPreview.setImageURI(selectedLogoUri);
                    } else {
                        Toast.makeText(requireContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private Uri copyUriToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            File file = new File(requireContext().getFilesDir(), "shop_logo.png");
            OutputStream outputStream = new FileOutputStream(file);
            
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            
            outputStream.close();
            inputStream.close();
            
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e("ShopProfile", "Failed to copy logo to internal storage", e);
            return null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShopProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        loadProfile();

        binding.profileSaveButton.setOnClickListener(v -> saveProfile());
        binding.profileClearButton.setOnClickListener(v -> clearProfile());
        if (binding.profilePickLogoButton != null) {
            binding.profilePickLogoButton.setOnClickListener(v -> pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));
        }
    }

    private void loadProfile() {
        if (binding == null) return;
        binding.profileShopNameInput.setText(preferences.getString(KEY_SHOP_NAME, ""));
        binding.profileOwnerNameInput.setText(preferences.getString(KEY_OWNER_NAME, ""));
        binding.profilePhoneInput.setText(preferences.getString(KEY_PHONE, ""));
        binding.profileWhatsappInput.setText(preferences.getString(KEY_WHATSAPP, ""));
        binding.profileAddressInput.setText(preferences.getString(KEY_ADDRESS, ""));
        binding.profileGstinInput.setText(preferences.getString(KEY_GSTIN, ""));
        binding.profileTaglineInput.setText(preferences.getString(KEY_TAGLINE, ""));
        binding.profileNotesInput.setText(preferences.getString(KEY_NOTES, ""));
        binding.profileUpiIdInput.setText(preferences.getString(KEY_UPI_ID, ""));
        binding.profileWhatsappNoteInput.setText(preferences.getString(KEY_WHATSAPP_NOTE, ""));

        String logoUriStr = preferences.getString(KEY_LOGO_URI, null);
        if (logoUriStr != null) {
            selectedLogoUri = Uri.parse(logoUriStr);
            if (binding.profileLogoPreview != null) binding.profileLogoPreview.setImageURI(selectedLogoUri);
        }
    }

    private void saveProfile() {
        if (binding == null) return;
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_SHOP_NAME, textFrom(binding.profileShopNameInput));
        editor.putString(KEY_OWNER_NAME, textFrom(binding.profileOwnerNameInput));
        editor.putString(KEY_PHONE, textFrom(binding.profilePhoneInput));
        editor.putString(KEY_WHATSAPP, textFrom(binding.profileWhatsappInput));
        editor.putString(KEY_ADDRESS, textFrom(binding.profileAddressInput));
        editor.putString(KEY_GSTIN, textFrom(binding.profileGstinInput));
        editor.putString(KEY_TAGLINE, textFrom(binding.profileTaglineInput));
        editor.putString(KEY_NOTES, textFrom(binding.profileNotesInput));
        editor.putString(KEY_UPI_ID, textFrom(binding.profileUpiIdInput));
        editor.putString(KEY_WHATSAPP_NOTE, textFrom(binding.profileWhatsappNoteInput));
        
        if (selectedLogoUri != null) {
            editor.putString(KEY_LOGO_URI, selectedLogoUri.toString());
        }

        editor.apply();
        Toast.makeText(requireContext(), "Profile Saved!", Toast.LENGTH_SHORT).show();
    }

    private void clearProfile() {
        preferences.edit().clear().apply();
        selectedLogoUri = null;
        if (binding != null) {
            binding.profileLogoPreview.setImageURI(null);
            loadProfile();
        }
        Toast.makeText(requireContext(), "Profile Cleared", Toast.LENGTH_SHORT).show();
    }

    private String textFrom(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
