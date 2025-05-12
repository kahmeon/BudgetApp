package com.example.mybudget;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class BillReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "bill_reminder_channel";
    private static final String CHANNEL_NAME = "Bill Reminder";
    private static final String CHANNEL_DESC = "Reminders for upcoming bill due dates";

    @Override
    public void onReceive(Context context, Intent intent) {
        String billName = intent.getStringExtra("bill_name");
        long currentMillis = intent.getLongExtra("due_date", 0);
        int requestCode = intent.getIntExtra("request_code", 0);

        if (billName == null || billName.trim().isEmpty()) {
            billName = "a bill";
        }

        // 1. Create notification channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // 2. Show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Bill Reminder")
                .setContentText("Your bill \"" + billName + "\" is due today.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = (int) System.currentTimeMillis(); // unique ID
            notificationManager.notify(notificationId, builder.build());
        }

        // 3. Reschedule for the same date next month
        if (currentMillis > 0) {
            Calendar nextReminder = Calendar.getInstance();
            nextReminder.setTimeInMillis(currentMillis);
            nextReminder.add(Calendar.MONTH, 1); // ⏳ shift to next month

            Intent nextIntent = new Intent(context, BillReminderReceiver.class);
            nextIntent.putExtra("bill_name", billName);
            nextIntent.putExtra("due_date", nextReminder.getTimeInMillis());
            nextIntent.putExtra("request_code", requestCode);

            PendingIntent nextPendingIntent = PendingIntent.getBroadcast(
                    context,
                    100 + requestCode, // keep request code consistent
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextReminder.getTimeInMillis(),
                        nextPendingIntent
                );
            }
        }
    }
}
