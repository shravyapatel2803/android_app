package com.example.billgenerator;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;
import androidx.biometric.BiometricPrompt;
import com.example.billgenerator.adapters.ViewPagerAdapter;
import com.example.billgenerator.security.BiometricAuthHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private ViewPagerAdapter viewPagerAdapter;
    private Toolbar toolbar;

    // --- Reset isAuthenticated on startup ---
    // We want to authenticate every time the app starts from scratch.
    private boolean isAuthenticated = false;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        // --- Initially hide ---
        if (tabLayout != null) tabLayout.setVisibility(View.GONE); else Log.e(TAG, "TabLayout is NULL in onCreate");
        if (viewPager != null) viewPager.setVisibility(View.GONE); else Log.e(TAG, "ViewPager is NULL in onCreate");

        Log.d(TAG, "onCreate finished, basic views found, authentication pending.");
        // Authentication will be triggered in onResume ONLY if needed
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called. isAuthenticated: " + isAuthenticated);

        // Only show prompt if not authenticated during this app session/lifecycle
        if (!isAuthenticated) {
            Log.i(TAG, "User not authenticated, showing biometric prompt.");
            if (tabLayout != null) tabLayout.setVisibility(View.GONE);
            if (viewPager != null) viewPager.setVisibility(View.GONE);

            // --- REMOVE THE BYPASS ---
            // Log.w(TAG, "TEMP: Bypassing authentication for testing.");
            // onAuthenticationSuccessBypass(); // Remove this call
            // --- END REMOVAL ---

            // --- RESTORE ACTUAL AUTH CALL ---
            showAuthenticationPrompt();
            // --- END RESTORE ---

        } else {
            Log.d(TAG, "User already authenticated, ensuring UI is visible.");
            // Ensure UI is set up and visible if resuming while already authenticated
            if (viewPager != null && tabLayout != null && viewPager.getVisibility() == View.GONE) {
                Log.d(TAG, "UI was hidden, setting up and showing.");
                setupViewPagerAndTabs();
            } else if (viewPager != null && tabLayout != null) {
                Log.d(TAG, "UI seems already visible.");
            }
            else {
                Log.e(TAG, "Cannot ensure UI visibility on resume, views are null!");
            }
        }
    }

    // --- REMOVE THE FAKE SUCCESS METHOD ---
    // private void onAuthenticationSuccessBypass() { /* ... remove this ... */ }
    // --- END REMOVAL ---


    private void setupViewPagerAndTabs() {
        // ... (This method remains the same - sets up adapter, mediator, makes views visible) ...
        Log.d(TAG, "Setting up ViewPager and TabLayout...");
        if (viewPager == null || tabLayout == null) { Log.e(TAG, "ViewPager or TabLayout became null! Re-finding..."); tabLayout = findViewById(R.id.tab_layout); viewPager = findViewById(R.id.view_pager); if (viewPager == null || tabLayout == null) { Log.e(TAG, "CRITICAL: Cannot setup UI - views not found."); Toast.makeText(this, "UI Error.", Toast.LENGTH_SHORT).show(); return; } }
        try { if (viewPager.getAdapter() == null) { viewPagerAdapter = new ViewPagerAdapter(this); viewPager.setAdapter(viewPagerAdapter); Log.d(TAG, "ViewPager adapter set."); } new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> { switch (position) { case 0: tab.setText("Generate Bill"); break; case 1: tab.setText("History"); break; case 2: tab.setText("Customers"); break; case 3: tab.setText("Stock"); break; } }).attach(); Log.d(TAG, "TabLayoutMediator attached."); tabLayout.setVisibility(View.VISIBLE); viewPager.setVisibility(View.VISIBLE); Log.d(TAG, "UI content made visible."); } catch (Exception e) { Log.e(TAG, "Error during UI setup: " + e.getMessage(), e); Toast.makeText(this, "Error setting up UI.", Toast.LENGTH_SHORT).show(); }
    }


    // --- ACTUAL AUTHENTICATION PROMPT with refined finish() logic ---
    private void showAuthenticationPrompt() {
        Log.d(TAG, "Calling BiometricAuthHelper.showBiometricPrompt");
        BiometricAuthHelper.showBiometricPrompt(this, new BiometricAuthHelper.AuthCallback() {
            @Override
            public void onAuthenticationSuccess() {
                Log.i(TAG, "BiometricAuthHelper reported SUCCESS.");
                isAuthenticated = true; // Mark as authenticated for this session
                runOnUiThread(() -> {
                    Log.d(TAG, "Auth Success: Setting up UI.");
                    // Check views again just in case
                    if (viewPager != null && tabLayout != null) {
                        setupViewPagerAndTabs(); // Setup and show UI
                        // Toast.makeText(MainActivity.this, "Welcome!", Toast.LENGTH_SHORT).show(); // Optional welcome
                    } else {
                        Log.e(TAG, "Auth Success but views became null! Cannot setup UI.");
                        Toast.makeText(MainActivity.this, "UI Error after Auth.", Toast.LENGTH_SHORT).show();
                        finish(); // Finish if UI cannot be shown
                    }
                });
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Log.e(TAG, "BiometricAuthHelper reported ERROR: Code " + errorCode + " - " + errString);
                isAuthenticated = false; // Ensure not marked as authenticated

                // --- Refined finish() logic ---
                // Only finish() immediately on explicit user cancellation or fatal errors.
                // For temporary errors (like HW unavailable), maybe allow retry later? (Not implemented here)
                // For lockouts, the system handles the timeout message.
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ) {
                    Log.w(TAG, "Authentication explicitly cancelled by user. Finishing activity.");
                    Toast.makeText(MainActivity.this, "Authentication required to use the app.", Toast.LENGTH_SHORT).show();
                    finishAffinity(); // Close the app and related tasks cleanly
                } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    Log.w(TAG, "Authentication locked out. System message shown.");
                    // Don't finish immediately, let user see the system lockout message.
                    // The prompt won't show again until lockout expires or user unlocks differently.
                    // finish(); // Maybe finish after a delay? Or let user background the app.
                    // For simplicity, we will finish here too, but could be handled differently.
                    finishAffinity();
                }
                else {
                    // Treat other errors (No hardware, HW unavailable, None enrolled AND no credential fallback, etc.) as reasons to close.
                    Log.e(TAG, "Unrecoverable authentication error or unsupported setup. Finishing activity.");
                    // Toast is likely shown by the helper for these cases.
                    finishAffinity(); // Close the app cleanly
                }
            }

            @Override
            public void onAuthenticationFailed() {
                Log.w(TAG, "BiometricAuthHelper reported FAILURE (e.g., wrong pin/fingerprint). Prompt should handle retries.");
                isAuthenticated = false;
                // DO NOT finish() here. Let the BiometricPrompt handle retries.
            }

            // These two are less likely with DEVICE_CREDENTIAL enabled but good to handle
            @Override
            public void onDeviceNotSecured() {
                Log.e(TAG, "Device not secured. Cannot authenticate.");
                isAuthenticated = false;
                Toast.makeText(MainActivity.this, "Device security (PIN, Pattern, Password) not set up.", Toast.LENGTH_LONG).show();
                finishAffinity(); // Close app
            }

            @Override
            public void onBiometricsUnavailable() {
                Log.e(TAG, "Biometrics unavailable (hardware error, security update needed, etc.).");
                isAuthenticated = false;
                // Toast is likely shown by the helper
                finishAffinity(); // Close app
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
        // Resetting isAuthenticated here means user MUST authenticate every time they resume the app.
        // Comment it out if you only want authentication on fresh launch.
        // isAuthenticated = false;
    }

    // Optional: If you want auth on fresh launch only, reset here.
    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called.");
        // If you want auth only when app starts from killed state or icon launch
        // (not just resuming from background), resetting here might be better than onPause.
        isAuthenticated = false;
    }
}