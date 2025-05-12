package com.example.mybudget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class NotificationUtils {

    // ✅ Now allow passing custom reminderTime
    public static void scheduleDailyReminder(Context context, Calendar reminderTime) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Intent settingsIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                settingsIntent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settingsIntent);
                return;
            }

            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.getTimeInMillis(),
                    pendingIntent
            );

            Toast.makeText(context, "Daily reminder rescheduled", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Backup simple version for activities that don't have Calendar
    public static void scheduleDailyReminder(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
        int hour = prefs.getInt("hour", 8);
        int minute = prefs.getInt("minute", 0);

        Calendar reminderTime = Calendar.getInstance();
        reminderTime.set(Calendar.HOUR_OF_DAY, hour);
        reminderTime.set(Calendar.MINUTE, minute);
        reminderTime.set(Calendar.SECOND, 0);
        reminderTime.set(Calendar.MILLISECOND, 0);

        // Move to tomorrow if time has passed
        if (reminderTime.before(Calendar.getInstance())) {
            reminderTime.add(Calendar.DATE, 1);
        }

        scheduleDailyReminder(context, reminderTime);
    }

    public static void rescheduleAllBillReminders(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "User not signed in. Skipping bill reminders.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("billReminders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int index = 1;
                    for (var doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        Long dueDateMillis = doc.getLong("dueDateMillis");

                        if (name != null && dueDateMillis != null) {
                            Calendar now = Calendar.getInstance();
                            Calendar dueDate = Calendar.getInstance();
                            dueDate.setTimeInMillis(dueDateMillis);

                            // If bill time already passed -> Move it to next month
                            while (dueDate.before(now)) {
                                dueDate.add(Calendar.MONTH, 1);
                            }

                            Intent intent = new Intent(context, BillReminderReceiver.class);
                            intent.putExtra("bill_name", name);
                            intent.putExtra("due_date", dueDate.getTimeInMillis());
                            intent.putExtra("request_code", index);

                            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                                    context,
                                    100 + index,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                            );

                            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                            if (alarmManager != null) {
                                alarmManager.setExactAndAllowWhileIdle(
                                        AlarmManager.RTC_WAKEUP,
                                        dueDate.getTimeInMillis(),
                                        pendingIntent
                                );
                            }
                            index++;
                        }
                    }

                    Toast.makeText(context, "Bill reminders rescheduled", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to load bill reminders: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
