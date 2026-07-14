package com.example.billgenerator.workers;

import android.Manifest;
import android.app.PendingIntent;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.billgenerator.MainActivity;
import com.example.billgenerator.R;
import com.example.billgenerator.database.databaseSystem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DebtReminderWorker extends Worker {

    private static final String CHANNEL_ID = "debt_reminder_channel";
    private static final String CHANNEL_NAME = "Debt Reminders";
    private static final String NOTIFICATION_TYPE = "DEBT_DUE";

    public DebtReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        databaseSystem dbHelper = new databaseSystem(context);
        ensureNotificationChannel(context);

        String todayIso = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Cursor dueCursor = null;
        try {
            dueCursor = dbHelper.fetchDueDebtBills(todayIso);
            if (dueCursor == null) {
                return Result.success();
            }

            int billIdCol = dueCursor.getColumnIndexOrThrow("bill_id");
            int customerIdCol = dueCursor.getColumnIndexOrThrow("customer_id");
            int nameCol = dueCursor.getColumnIndexOrThrow("name");
            int debtAmountCol = dueCursor.getColumnIndexOrThrow("debt_amount");

            while (dueCursor.moveToNext()) {
                int billId = dueCursor.getInt(billIdCol);
                int customerId = dueCursor.getInt(customerIdCol);
                String customerName = dueCursor.getString(nameCol);
                double debtAmount = dueCursor.getDouble(debtAmountCol);

                boolean alreadyNotified = dbHelper.hasNotificationSentForBill(billId, NOTIFICATION_TYPE, todayIso);
                if (alreadyNotified) {
                    continue;
                }

                String message = "Debt reminder: " + customerName + " has pending amount of Rs " + String.format(Locale.getDefault(), "%.2f", debtAmount) + " due today.";
                String customerPhone = dueCursor.getString(dueCursor.getColumnIndexOrThrow("phone"));
                showNotificationIfAllowed(context, billId, customerId, customerName, message, customerPhone);
                dbHelper.insertNotificationHistory(billId, customerName, message, NOTIFICATION_TYPE, todayIso);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        } finally {
            if (dueCursor != null) {
                dueCursor.close();
            }
        }
    }

    private void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminders for debt due dates");
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showNotificationIfAllowed(Context context, int billId, int customerId, String customerName, String message, String phone) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(MainActivity.EXTRA_OPEN_DESTINATION, 3);
        openIntent.putExtra(MainActivity.EXTRA_OPEN_CUSTOMER_ID, customerId);
        openIntent.putExtra(MainActivity.EXTRA_OPEN_BILL_ID, billId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                billId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // WhatsApp Intent
        Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
        try {
            String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" +
                    Uri.encode("Hello " + customerName + ", a friendly reminder for your pending debt of Rs " + message.split("Rs ")[1] + " which is due today. Please contact us for details.");
            whatsappIntent.setData(Uri.parse(url));
        } catch (Exception e) {
            Log.e("DebtReminderWorker", "Error creating WhatsApp URL", e);
        }

        PendingIntent whatsappPendingIntent = PendingIntent.getActivity(
                context,
                billId + 1000,
                whatsappIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Debt Due Today - " + customerName)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_whatsapp, "WhatsApp Remind", whatsappPendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(9000 + billId, builder.build());
    }
}
