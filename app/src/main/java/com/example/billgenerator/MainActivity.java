package com.example.billgenerator;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.biometric.BiometricPrompt;
import androidx.viewpager2.widget.ViewPager2;

import com.example.billgenerator.adapters.ViewPagerAdapter;
import com.example.billgenerator.databinding.ActivityMainBinding;
import com.example.billgenerator.workers.DebtReminderWorker;
import com.google.android.material.navigation.NavigationView;
import com.example.billgenerator.security.BiometricAuthHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final String EXTRA_OPEN_DESTINATION = "extra_open_destination";
    public static final String EXTRA_OPEN_CUSTOMER_ID = "extra_open_customer_id";
    public static final String EXTRA_OPEN_BILL_ID = "extra_open_bill_id";

    private ActivityMainBinding binding;
    private ViewPagerAdapter viewPagerAdapter;
    private ActionBarDrawerToggle drawerToggle;

    private boolean isAuthenticated = false;
    private static boolean isAuthenticatedForSession = false;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_POST_NOTIFICATIONS = 2201;
    private int pendingDestinationIndex = -1;

    private final ActivityResultLauncher<Intent> backupLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        exportDatabase(uri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> restoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importDatabase(uri);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        if (binding.drawerLayout != null && binding.toolbar != null && binding.navigationView != null && binding.navigationView.getParent() == binding.drawerLayout) {
            drawerToggle = new ActionBarDrawerToggle(this, binding.drawerLayout, binding.toolbar, R.string.drawer_open, R.string.drawer_close);
            binding.drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
        }
        if (binding.navigationView != null) {
            binding.navigationView.setNavigationItemSelectedListener(this);
            binding.navigationView.setCheckedItem(R.id.nav_dashboard);
        }

        handleLaunchIntent(getIntent());

        isAuthenticated = isAuthenticatedForSession;

        Log.d(TAG, "onCreate finished, basic views found, authentication pending.");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (binding.drawerLayout != null && binding.navigationView != null && binding.navigationView.getParent() == binding.drawerLayout) {
                    if (binding.drawerLayout.isDrawerOpen(binding.navigationView)) {
                        binding.drawerLayout.closeDrawer(binding.navigationView);
                        return;
                    }
                }
                if (binding.viewPager != null && binding.viewPager.getCurrentItem() != 0) {
                    binding.viewPager.setCurrentItem(0, false);
                    if (binding.navigationView != null) {
                        binding.navigationView.setCheckedItem(R.id.nav_dashboard);
                    }
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAuthenticated = isAuthenticatedForSession;
        Log.d(TAG, "onResume called. isAuthenticated: " + isAuthenticated);

        if (!isAuthenticated) {
            Log.i(TAG, "User not authenticated, showing biometric prompt.");
            binding.viewPager.setVisibility(View.GONE);

            showAuthenticationPrompt();
        } else {
            Log.d(TAG, "User already authenticated, ensuring UI is visible.");
            if (binding.viewPager.getVisibility() == View.GONE) {
                Log.d(TAG, "UI was hidden, setting up and showing.");
                setupMainNavigation();
            } else {
                Log.d(TAG, "UI seems already visible.");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
        if (isAuthenticated && binding.viewPager.getAdapter() != null) {
            applyPendingNavigationRequest();
        }
    }

    private void setupMainNavigation() {
        Log.d(TAG, "Setting up main navigation...");

        if (binding.viewPager.getAdapter() == null) {
            viewPagerAdapter = new ViewPagerAdapter(this);
            binding.viewPager.setAdapter(viewPagerAdapter);
        }
        binding.viewPager.setUserInputEnabled(false);
        
        // Show a brief loading screen to pre-load critical data
        if (findViewById(R.id.splash_screen) != null) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                findViewById(R.id.splash_screen).animate()
                        .alpha(0f)
                        .setDuration(400)
                        .withEndAction(() -> {
                            findViewById(R.id.splash_screen).setVisibility(View.GONE);
                            binding.viewPager.setVisibility(View.VISIBLE);
                        });
            }, 1200); // 1.2 seconds of "loading" to ensure background tasks start
        } else {
            binding.viewPager.setVisibility(View.VISIBLE);
        }

        applyPendingNavigationRequest();

        scheduleDebtReminderWorker();
        requestNotificationPermissionIfNeeded();
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        int destination = intent.getIntExtra(EXTRA_OPEN_DESTINATION, -1);
        if (destination >= 0) {
            pendingDestinationIndex = destination;
        }
    }

    private void applyPendingNavigationRequest() {
        if (pendingDestinationIndex < 0) {
            return;
        }

        int destination = pendingDestinationIndex;
        pendingDestinationIndex = -1;
        binding.viewPager.setCurrentItem(destination, false);

        if (destination == 1) { // Generate Bill
            int editBillId = getIntent().getIntExtra(EXTRA_OPEN_BILL_ID, -1);
            if (editBillId != -1) {
                // Find fragment and load
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + 1);
                if (fragment instanceof com.example.billgenerator.fragments.GenerateBillFragment) {
                    ((com.example.billgenerator.fragments.GenerateBillFragment) fragment).loadBillForEditing(editBillId);
                    getIntent().removeExtra(EXTRA_OPEN_BILL_ID);
                }
            }
        }

        if (binding.navigationView != null) {
            binding.navigationView.setCheckedItem(getDrawerItemForDestination(destination));
        }
    }

    private int getDrawerItemForDestination(int destination) {
        if (destination == 1) return R.id.nav_generate_bill;
        if (destination == 2) return R.id.nav_bill_history;
        if (destination == 3) return R.id.nav_customers;
        if (destination == 4) return R.id.nav_stock;
        if (destination == 5) return R.id.nav_notifications;
        if (destination == 6) return R.id.nav_stats;
        if (destination == 7) return R.id.nav_debt_customers;
        if (destination == 8) return R.id.nav_collection_mode;
        if (destination == 9) return R.id.nav_shop_profile;
        if (destination == 10) return R.id.nav_suppliers;
        if (destination == 11) return R.id.nav_worker_ledger;
        if (destination == 12) return R.id.nav_settings;
        return R.id.nav_dashboard;
    }

    public void navigateToDestination(int destination) {
        pendingDestinationIndex = destination;
        applyPendingNavigationRequest();
    }

    public void navigateToEditBill(int billId) {
        pendingDestinationIndex = 1; // R.id.nav_generate_bill
        getIntent().putExtra(EXTRA_OPEN_BILL_ID, billId);
        applyPendingNavigationRequest();
    }

    private void showAuthenticationPrompt() {
        Log.d(TAG, "Calling BiometricAuthHelper.showBiometricPrompt");
        BiometricAuthHelper.showBiometricPrompt(this, new BiometricAuthHelper.AuthCallback() {
            @Override
            public void onAuthenticationSuccess() {
                Log.i(TAG, "BiometricAuthHelper reported SUCCESS.");
                isAuthenticated = true;
                isAuthenticatedForSession = true;
                runOnUiThread(() -> {
                    Log.d(TAG, "Auth Success: Setting up UI.");
                    if (binding.viewPager != null) {
                        setupMainNavigation();
                    } else {
                        Log.e(TAG, "Auth Success but views became null! Cannot setup UI.");
                        Toast.makeText(MainActivity.this, "UI Error after Auth.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                Log.e(TAG, "BiometricAuthHelper reported ERROR: Code " + errorCode + " - " + errString);
                isAuthenticated = false;

                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_CANCELED ) {
                    Log.w(TAG, "Authentication explicitly cancelled by user. Finishing activity.");
                    Toast.makeText(MainActivity.this, "Authentication required to use the app.", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                } else if (errorCode == BiometricPrompt.ERROR_LOCKOUT || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT) {
                    Log.w(TAG, "Authentication locked out. System message shown.");
                    finishAffinity();
                }
                else {
                    Log.e(TAG, "Unrecoverable authentication error or unsupported setup. Finishing activity.");
                    finishAffinity();
                }
            }

            @Override
            public void onAuthenticationFailed() {
                Log.w(TAG, "BiometricAuthHelper reported FAILURE (e.g., wrong pin/fingerprint). Prompt should handle retries.");
                isAuthenticated = false;
            }

            @Override
            public void onDeviceNotSecured() {
                Log.e(TAG, "Device not secured. Cannot authenticate.");
                isAuthenticated = false;
                Toast.makeText(MainActivity.this, "Device security (PIN, Pattern, Password) not set up.", Toast.LENGTH_LONG).show();
                finishAffinity();
            }

            @Override
            public void onBiometricsUnavailable() {
                Log.e(TAG, "Biometrics unavailable (hardware error, security update needed, etc.).");
                isAuthenticated = false;
                finishAffinity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_backup) {
            backupDatabase();
            return true;
        } else if (item.getItemId() == R.id.action_restore_backup) {
            restoreDatabaseFromBackup();
            return true;
        } else if (item.getItemId() == R.id.action_run_debt_reminder_now) {
            triggerDebtReminderNow();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void triggerDebtReminderNow() {
        OneTimeWorkRequest runNowRequest = new OneTimeWorkRequest.Builder(DebtReminderWorker.class).build();
        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("debt_due_reminder_manual", ExistingWorkPolicy.REPLACE, runNowRequest);
        Toast.makeText(this, R.string.debt_reminder_triggered, Toast.LENGTH_SHORT).show();
    }

    private void backupDatabase() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "BillGenerator_Backup_" + timeStamp + ".db";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        backupLauncher.launch(intent);
    }

    private void restoreDatabaseFromBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        restoreLauncher.launch(intent);
    }

    private void importDatabase(Uri uri) {
        File targetDb = getDatabasePath("my_database.db");
        File parent = targetDb.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                Toast.makeText(this, "Restore failed: Could not create directory.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(targetDb, false)) {
            if (in == null) {
                Toast.makeText(this, "Restore failed: Could not read backup file.", Toast.LENGTH_LONG).show();
                return;
            }

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();

            Toast.makeText(this, "Restore successful. Please restart app.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error importing database", e);
            Toast.makeText(this, "Restore failed!", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportDatabase(Uri uri) {
        File sourceDb = getDatabasePath("my_database.db");
        if (!sourceDb.exists()) {
            Toast.makeText(this, "No database found to export.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try (InputStream in = new FileInputStream(sourceDb);
             OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) {
                Toast.makeText(this, "Export failed: Could not write to destination.", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            out.flush();
            Toast.makeText(this, "Backup Successful!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Error exporting database", e);
            Toast.makeText(this, "Backup Failed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
    }

    private void scheduleDebtReminderWorker() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1);
        }
        long initialDelayMinutes = Duration.between(now, nextRun).toMinutes();
        if (initialDelayMinutes < 1) {
            initialDelayMinutes = 1;
        }

        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(DebtReminderWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniquePeriodicWork("debt_due_reminder_work", ExistingPeriodicWorkPolicy.UPDATE, reminderRequest);
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Debt reminders will be saved in history, but alerts may not appear.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (binding.viewPager == null) {
            return false;
        }
        int destination = 0;
        int itemId = item.getItemId();
        if (itemId == R.id.nav_dashboard) {
            destination = 0;
        } else if (itemId == R.id.nav_generate_bill) {
            destination = 1;
        } else if (itemId == R.id.nav_bill_history) {
            destination = 2;
        } else if (itemId == R.id.nav_customers) {
            destination = 3;
        } else if (itemId == R.id.nav_stock) {
            destination = 4;
        } else if (itemId == R.id.nav_notifications) {
            destination = 5;
        } else if (itemId == R.id.nav_stats) {
            destination = 6;
        } else if (itemId == R.id.nav_debt_customers) {
            destination = 7;
        } else if (itemId == R.id.nav_collection_mode) {
            destination = 8;
        } else if (itemId == R.id.nav_shop_profile) {
            destination = 9;
        } else if (itemId == R.id.nav_suppliers) {
            destination = 10;
        } else if (itemId == R.id.nav_worker_ledger) {
            destination = 11;
        } else if (itemId == R.id.nav_settings) {
            destination = 12;
        }
        binding.viewPager.setCurrentItem(destination, false);
        if (binding.drawerLayout != null) {
            binding.drawerLayout.closeDrawers();
        }
        return true;
    }



    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called.");
    }
}
