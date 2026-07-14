package com.example.billgenerator.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.billgenerator.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ShopProfileFragment extends Fragment {

    private static final String PREFS_NAME = "shop_profile_prefs";
    private static final String KEY_SHOP_NAME = "shop_name";
    private static final String KEY_OWNER_NAME = "owner_name";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_WHATSAPP = "whatsapp";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_GSTIN = "gstin";
    private static final String KEY_TAGLINE = "tagline";
    private static final String KEY_NOTES = "notes";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_WHATSAPP_NOTE = "whatsapp_note";
    public static final String KEY_LOGO_URI = "shop_logo_uri";

    private TextInputEditText shopNameInput, ownerNameInput, phoneInput, whatsappInput, addressInput, gstinInput, taglineInput, notesInput, upiIdInput, whatsappNoteInput;
    private ImageView logoPreview;
    private Uri selectedLogoUri;

    private SharedPreferences preferences;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> {
                if (uri != null) {
                    Uri internalUri = copyUriToInternalStorage(uri);
                    if (internalUri != null) {
                        selectedLogoUri = internalUri;
                        if (logoPreview != null) logoPreview.setImageURI(selectedLogoUri);
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
        return inflater.inflate(R.layout.fragment_shop_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        shopNameInput = view.findViewById(R.id.profile_shop_name_input);
        ownerNameInput = view.findViewById(R.id.profile_owner_name_input);
        phoneInput = view.findViewById(R.id.profile_phone_input);
        whatsappInput = view.findViewById(R.id.profile_whatsapp_input);
        addressInput = view.findViewById(R.id.profile_address_input);
        gstinInput = view.findViewById(R.id.profile_gstin_input);
        taglineInput = view.findViewById(R.id.profile_tagline_input);
        notesInput = view.findViewById(R.id.profile_notes_input);
        upiIdInput = view.findViewById(R.id.profile_upi_id_input);
        whatsappNoteInput = view.findViewById(R.id.profile_whatsapp_note_input);
        logoPreview = view.findViewById(R.id.profile_logo_preview);
        MaterialButton pickLogoButton = view.findViewById(R.id.profile_pick_logo_button);

        MaterialButton clearButton = view.findViewById(R.id.profile_clear_button);
        MaterialButton saveButton = view.findViewById(R.id.profile_save_button);

        loadProfile();

        saveButton.setOnClickListener(v -> saveProfile());
        clearButton.setOnClickListener(v -> clearProfile());
        if (pickLogoButton != null) {
            pickLogoButton.setOnClickListener(v -> pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));
        }
    }

    private void loadProfile() {
        shopNameInput.setText(preferences.getString(KEY_SHOP_NAME, ""));
        ownerNameInput.setText(preferences.getString(KEY_OWNER_NAME, ""));
        phoneInput.setText(preferences.getString(KEY_PHONE, ""));
        whatsappInput.setText(preferences.getString(KEY_WHATSAPP, ""));
        addressInput.setText(preferences.getString(KEY_ADDRESS, ""));
        gstinInput.setText(preferences.getString(KEY_GSTIN, ""));
        taglineInput.setText(preferences.getString(KEY_TAGLINE, ""));
        notesInput.setText(preferences.getString(KEY_NOTES, ""));
        if (upiIdInput != null) upiIdInput.setText(preferences.getString(KEY_UPI_ID, ""));
        if (whatsappNoteInput != null) whatsappNoteInput.setText(preferences.getString(KEY_WHATSAPP_NOTE, "Thank you for shopping with us! View your bill below."));
        
        String logoUriStr = preferences.getString(KEY_LOGO_URI, null);
        if (logoUriStr != null && logoPreview != null) {
            try {
                selectedLogoUri = Uri.parse(logoUriStr);
                if ("file".equals(selectedLogoUri.getScheme())) {
                    if (selectedLogoUri.getPath() != null) {
                        File file = new File(selectedLogoUri.getPath());
                        if (file.exists()) {
                            logoPreview.setImageURI(selectedLogoUri);
                        } else {
                            selectedLogoUri = null;
                        }
                    }
                } else {
                    // It's a legacy picker URI. Try to load and convert it.
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), selectedLogoUri);
                        logoPreview.setImageBitmap(bitmap);
                        
                        // Convert to internal storage for future use
                        Uri internalUri = copyUriToInternalStorage(selectedLogoUri);
                        if (internalUri != null) {
                            selectedLogoUri = internalUri;
                            preferences.edit().putString(KEY_LOGO_URI, selectedLogoUri.toString()).apply();
                        }
                    } catch (SecurityException se) {
                        Log.e("ShopProfile", "Legacy URI permission lost: " + logoUriStr);
                        preferences.edit().remove(KEY_LOGO_URI).apply();
                        selectedLogoUri = null;
                    }
                }
            } catch (Exception e) {
                Log.e("ShopProfile", "Error loading logo: " + logoUriStr, e);
                preferences.edit().remove(KEY_LOGO_URI).apply();
                selectedLogoUri = null;
            }
        }
    }

    private void saveProfile() {
        String shopName = textFrom(shopNameInput);
        if (shopName.isEmpty()) {
            shopNameInput.setError("Shop name is required");
            shopNameInput.requestFocus();
            return;
        }

        SharedPreferences.Editor editor = preferences.edit()
                .putString(KEY_SHOP_NAME, shopName)
                .putString(KEY_OWNER_NAME, textFrom(ownerNameInput))
                .putString(KEY_PHONE, textFrom(phoneInput))
                .putString(KEY_WHATSAPP, textFrom(whatsappInput))
                .putString(KEY_ADDRESS, textFrom(addressInput))
                .putString(KEY_GSTIN, textFrom(gstinInput))
                .putString(KEY_TAGLINE, textFrom(taglineInput))
                .putString(KEY_NOTES, textFrom(notesInput))
                .putString(KEY_UPI_ID, textFrom(upiIdInput))
                .putString(KEY_WHATSAPP_NOTE, textFrom(whatsappNoteInput));

        if (selectedLogoUri != null) {
            editor.putString(KEY_LOGO_URI, selectedLogoUri.toString());
        }

        editor.apply();
        Toast.makeText(requireContext(), "Shop profile saved", Toast.LENGTH_SHORT).show();
    }

    private void clearProfile() {
        preferences.edit().clear().apply();
        File file = new File(requireContext().getFilesDir(), "shop_logo.png");
        if (file.exists()) {
            file.delete();
        }
        logoPreview.setImageDrawable(null);
        selectedLogoUri = null;
        loadProfile();
        Toast.makeText(requireContext(), "Shop profile cleared", Toast.LENGTH_SHORT).show();
    }

    private String textFrom(TextInputEditText input) {
        if (input == null || input.getText() == null) return "";
        return input.getText().toString().trim();
    }
}
