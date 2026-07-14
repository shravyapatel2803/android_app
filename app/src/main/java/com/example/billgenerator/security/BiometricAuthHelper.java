package com.example.billgenerator.security;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

/**
 * Helper class to manage BiometricPrompt authentication.
 */
public class BiometricAuthHelper {

    private static final String TAG = "BiometricAuthHelper";

    public interface AuthCallback {
        void onAuthenticationSuccess();
        void onAuthenticationError(int errorCode, CharSequence errString);
        void onAuthenticationFailed();
        void onDeviceNotSecured();
        void onBiometricsUnavailable();
    }

    public static void showBiometricPrompt(AppCompatActivity activity, final AuthCallback callback) {

        Executor executor = ContextCompat.getMainExecutor(activity);
        BiometricManager biometricManager = BiometricManager.from(activity);

        // Define the types of authenticators allowed
        final int authenticators = BiometricManager.Authenticators.DEVICE_CREDENTIAL | BiometricManager.Authenticators.BIOMETRIC_STRONG;

        // Check if authentication is possible
        switch (biometricManager.canAuthenticate(authenticators)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.d(TAG, "App can authenticate using biometrics or device credentials.");
                break; // Proceed
            // Handle various error cases...
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.e(TAG, "No biometric features available.");
                Toast.makeText(activity, "Biometric features unavailable.", Toast.LENGTH_SHORT).show();
                callback.onBiometricsUnavailable(); return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.e(TAG, "Biometric features currently unavailable.");
                Toast.makeText(activity, "Biometrics currently unavailable.", Toast.LENGTH_SHORT).show();
                callback.onBiometricsUnavailable(); return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // IMPORTANT: If DEVICE_CREDENTIAL is allowed (as it is here),
                // this case means *biometrics* aren't enrolled, but PIN/Pattern/Pass *might* be.
                // The prompt will still work if a device credential is set.
                // If *only* BIOMETRIC_STRONG was allowed, this would be a hard failure.
                Log.w(TAG, "No biometrics enrolled, device credential will be used if available.");
                // We proceed because DEVICE_CREDENTIAL is allowed.
                break;
            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Log.e(TAG, "Biometric security update required.");
                Toast.makeText(activity, "Security update required.", Toast.LENGTH_LONG).show();
                callback.onBiometricsUnavailable(); return;
            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                Log.e(TAG, "Biometric authentication unsupported.");
                Toast.makeText(activity, "Biometrics unsupported.", Toast.LENGTH_LONG).show();
                callback.onBiometricsUnavailable(); return;
            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
            default: // Catch unknown status too
                Log.e(TAG, "Biometric status unknown or unexpected error.");
                Toast.makeText(activity, "Cannot determine biometric status.", Toast.LENGTH_LONG).show();
                callback.onBiometricsUnavailable(); return;
        }


        // --- Authentication Callbacks ---
        BiometricPrompt.AuthenticationCallback authCallback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Log.e(TAG, "Authentication error: Code " + errorCode + " - " + errString);
                callback.onAuthenticationError(errorCode, errString);
                // Avoid showing generic toast for user cancellations
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED) { // Check ERROR_CANCELED too
                    Toast.makeText(activity.getApplicationContext(), "Auth error: " + errString, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Log.i(TAG, "Authentication succeeded!");
                // Toast shown in MainActivity now
                // Toast.makeText(activity.getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                callback.onAuthenticationSuccess();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Log.w(TAG, "Authentication failed (e.g., wrong fingerprint/PIN).");
                // System handles showing retry message
                // Toast.makeText(activity.getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                callback.onAuthenticationFailed();
            }
        };

        // Create the BiometricPrompt instance
        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor, authCallback);


        // --- Configure the Prompt Dialog ---
        BiometricPrompt.PromptInfo.Builder promptInfoBuilder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authentication Required") // Simpler title
                .setSubtitle("Unlock Bill Generator")
                // .setDescription("Use screen lock or biometrics") // Optional
                .setAllowedAuthenticators(authenticators); // Use the defined authenticators

        // --- FIXED: Remove this line ---
        // .setNegativeButtonText("Cancel");
        // --- End Fix ---

        // Build the PromptInfo object
        BiometricPrompt.PromptInfo promptInfo;
        try {
            promptInfo = promptInfoBuilder.build(); // This is where the crash occurred
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error building PromptInfo: " + e.getMessage(), e);
            Toast.makeText(activity, "Biometric setup error.", Toast.LENGTH_LONG).show();
            callback.onBiometricsUnavailable(); // Treat as unavailable
            return;
        }


        // --- Trigger the Authentication ---
        Log.d(TAG, "Starting authentication...");
        biometricPrompt.authenticate(promptInfo);
    }
}