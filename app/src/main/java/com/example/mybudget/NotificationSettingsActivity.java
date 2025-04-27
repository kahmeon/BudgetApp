package com.example.mybudget;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchAllNotifications, switchDailyReminder, switchBillReminder;
    private Button btnPickTime, btnPreviewNotification;
    private TextView tvSelectedTime;
    private LinearLayout dailyReminderTimeContainer;
    private Calendar reminderTime = Calendar.getInstance();
    private List<BillReminder> billList = new ArrayList<>();
    private BillReminderAdapter adapter;
    private TextView tvEmptyState;

    private Calendar selectedDate = null;

    private static final String PREFS_NAME = "notification_prefs";
    private static final String KEY_DAILY_ENABLED = "daily_enabled";
    private static final String KEY_BILL_ENABLED = "bill_enabled";

    private static final String CHANNEL_ID = "daily_reminder_channel";
    private static final String KEY_ALL_NOTIFICATIONS_ENABLED = "all_notifications_enabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        switchAllNotifications = findViewById(R.id.switch_all_notifications);
        switchDailyReminder = findViewById(R.id.switch_daily_reminder);
        switchBillReminder = findViewById(R.id.switch_bill_reminder);
        btnPickTime = findViewById(R.id.btn_pick_time);
        btnPreviewNotification = findViewById(R.id.btn_preview_notification);
        tvSelectedTime = findViewById(R.id.tv_selected_time);
        dailyReminderTimeContainer = findViewById(R.id.daily_reminder_time_container);
        LinearLayout billReminderSettingsContainer = findViewById(R.id.bill_reminder_settings_container);
        findViewById(R.id.btn_add_bill).setOnClickListener(v -> showAddBillDialog());
        tvEmptyState = findViewById(R.id.tv_empty_bills);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        loadSavedReminderTime();
        createNotificationChannel();
        loadSavedBillReminders();

        btnPickTime.setOnClickListener(v -> showTimePicker());
        btnPreviewNotification.setOnClickListener(v -> showTestNotification());
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean allEnabled = prefs.getBoolean(KEY_ALL_NOTIFICATIONS_ENABLED, true);
        updateNotificationSectionsVisibility(allEnabled);

        switchAllNotifications.setChecked(prefs.getBoolean(KEY_ALL_NOTIFICATIONS_ENABLED, true));
        switchDailyReminder.setChecked(prefs.getBoolean(KEY_DAILY_ENABLED, true));
        switchBillReminder.setChecked(prefs.getBoolean(KEY_BILL_ENABLED, false));
        switchAllNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_ALL_NOTIFICATIONS_ENABLED, isChecked).apply();
            updateNotificationSectionsVisibility(isChecked);
        });

        if (switchDailyReminder.isChecked()) {
            dailyReminderTimeContainer.setVisibility(View.VISIBLE);
            scheduleDailyReminder();
        }
        if (switchBillReminder.isChecked()) scheduleBillReminder();

        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_DAILY_ENABLED, isChecked).apply();
            if (isChecked) {
                dailyReminderTimeContainer.setVisibility(View.VISIBLE);
                scheduleDailyReminder();
            } else {
                dailyReminderTimeContainer.setVisibility(View.GONE);
                cancelDailyReminder();
            }
        });

        switchBillReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BILL_ENABLED, isChecked).apply();
            if (isChecked) {
                scheduleBillReminder();
            } else {
                cancelBillReminder();
            }
        });

        adapter = new BillReminderAdapter(billList);
        RecyclerView recyclerView = findViewById(R.id.rv_bills);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        switchBillReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BILL_ENABLED, isChecked).apply();
            billReminderSettingsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (isChecked) {
                scheduleBillReminder();
            } else {
                cancelBillReminder();
            }
        });
        if (switchBillReminder.isChecked()) {
            billReminderSettingsContainer.setVisibility(View.VISIBLE);
            scheduleBillReminder();
            loadSavedBillReminders();
        }



        switchBillReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_BILL_ENABLED, isChecked).apply();
            billReminderSettingsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            if (isChecked) {
                scheduleBillReminder(); // Or scheduleAllBillReminders() if supporting multiple
            } else {
                cancelBillReminder();
            }
        });
        if (switchBillReminder.isChecked()) {
            billReminderSettingsContainer.setVisibility(View.VISIBLE);
            scheduleBillReminder();
        }
        adapter.setOnBillDeleteClickListener((bill, position) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Reminder")
                    .setMessage("Are you sure you want to delete this bill reminder?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteBillReminder(bill, position);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        LinearLayout dailySection = findViewById(R.id.daily_reminder_time_container);
        LinearLayout billSection = findViewById(R.id.bill_reminder_settings_container);

    }



    private void scheduleIndividualBillReminder(BillReminder bill, int requestCode) {
        Intent intent = new Intent(this, BillReminderReceiver.class);
        intent.putExtra("bill_name", bill.getName());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 100 + requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    bill.getDueDateMillis(), pendingIntent);
        }
    }

    private void cancelDailyReminder() {
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Daily reminder cancelled", Toast.LENGTH_SHORT).show();
        }
    }



    private void showTimePicker() {
        int hour = reminderTime.get(Calendar.HOUR_OF_DAY);
        int minute = reminderTime.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this, (TimePicker view, int hourOfDay, int minute1) -> {
            reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
            reminderTime.set(Calendar.MINUTE, minute1);
            reminderTime.set(Calendar.SECOND, 0);
            reminderTime.set(Calendar.MILLISECOND, 0);

            if (reminderTime.before(Calendar.getInstance())) {
                reminderTime.add(Calendar.DATE, 1);
            }

            String timeText = String.format("%02d:%02d", hourOfDay, minute1);
            tvSelectedTime.setText(timeText);

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit()
                    .putInt("hour", hourOfDay)
                    .putInt("minute", minute1)
                    .apply();

            scheduleDailyReminder();
        }, hour, minute, true);

        timePickerDialog.show();
    }

    private void loadSavedReminderTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int hour = prefs.getInt("hour", 8);
        int minute = prefs.getInt("minute", 0);
        reminderTime.set(Calendar.HOUR_OF_DAY, hour);
        reminderTime.set(Calendar.MINUTE, minute);
        reminderTime.set(Calendar.SECOND, 0);
        reminderTime.set(Calendar.MILLISECOND, 0);

        String timeText = String.format("%02d:%02d", hour, minute);
        tvSelectedTime.setText(timeText);
    }

    private void loadSavedBillReminders() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("billReminders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    billList.clear(); // Important: clear old items
                    for (var doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        Double amount = doc.getDouble("amount"); // use Double to prevent NPE
                        Long dueDateMillis = doc.getLong("dueDateMillis");

                        if (name != null && amount != null && dueDateMillis != null) {
                            BillReminder bill = new BillReminder(name, amount, dueDateMillis);
                            billList.add(bill);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    tvEmptyState.setVisibility(billList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load bills", e);
                    Toast.makeText(this, "Error loading reminders", Toast.LENGTH_SHORT).show();
                });
    }



    private void scheduleDailyReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "Permission required to schedule exact alarms", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime.getTimeInMillis(),
                    pendingIntent
            );
            Toast.makeText(this, "Daily reminder scheduled", Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleBillReminder() {
        Calendar billTime = Calendar.getInstance();
        billTime.set(Calendar.HOUR_OF_DAY, 20);
        billTime.set(Calendar.MINUTE, 0);
        billTime.set(Calendar.SECOND, 0);
        billTime.set(Calendar.MILLISECOND, 0);

        if (billTime.before(Calendar.getInstance())) {
            billTime.add(Calendar.DATE, 1);
        }

        Intent intent = new Intent(this, BillReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, billTime.getTimeInMillis(), pendingIntent);
        }
    }

    private void cancelBillReminder() {
        Intent intent = new Intent(this, BillReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void updateNotificationSectionsVisibility(boolean notificationsEnabled) {
        LinearLayout dailyReminderContainer = findViewById(R.id.daily_reminder_container); // Add this ID in XML
        LinearLayout billReminderContainer = findViewById(R.id.bill_reminder_container);   // Add this ID in XML

        dailyReminderContainer.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);
        billReminderContainer.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);

        if (!notificationsEnabled) {
            switchDailyReminder.setChecked(false);
            switchBillReminder.setChecked(false);
            dailyReminderTimeContainer.setVisibility(View.GONE);
            findViewById(R.id.bill_reminder_settings_container).setVisibility(View.GONE);
            cancelDailyReminder();
            cancelBillReminder();
        } else {
            if (switchDailyReminder.isChecked()) {
                dailyReminderTimeContainer.setVisibility(View.VISIBLE);
                scheduleDailyReminder();
            }
            if (switchBillReminder.isChecked()) {
                findViewById(R.id.bill_reminder_settings_container).setVisibility(View.VISIBLE);
                scheduleBillReminder();
            }
        }
    }


    private void showTestNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Test Notification")
                .setContentText("This is a test notification preview.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Daily Reminder";
            String description = "Channel for daily budget reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You may not receive reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showAddBillDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bill, null);
        EditText nameInput = dialogView.findViewById(R.id.et_bill_name);
        EditText amountInput = dialogView.findViewById(R.id.et_bill_amount);
        Button pickDateBtn = dialogView.findViewById(R.id.btn_pick_date);
        Button pickTimeBtn = dialogView.findViewById(R.id.btn_pick_time);
        TextView selectedDateView = dialogView.findViewById(R.id.tv_selected_datetime);

        selectedDate = Calendar.getInstance();
        updateSelectedDateTimeView(selectedDateView); // Show default time

        pickDateBtn.setOnClickListener(v -> {
            int year = selectedDate.get(Calendar.YEAR);
            int month = selectedDate.get(Calendar.MONTH);
            int day = selectedDate.get(Calendar.DAY_OF_MONTH);

            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate.set(y, m, d);
                updateSelectedDateTimeView(selectedDateView);
            }, year, month, day).show();
        });

        pickTimeBtn.setOnClickListener(v -> {
            int hour = selectedDate.get(Calendar.HOUR_OF_DAY);
            int minute = selectedDate.get(Calendar.MINUTE);

            new TimePickerDialog(this, (view, h, m) -> {
                selectedDate.set(Calendar.HOUR_OF_DAY, h);
                selectedDate.set(Calendar.MINUTE, m);
                updateSelectedDateTimeView(selectedDateView);
            }, hour, minute, true).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Bill Reminder")
                .setView(dialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        Button saveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        saveBtn.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String amountText = amountInput.getText().toString().trim();

            if (name.isEmpty() || amountText.isEmpty() || selectedDate == null) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Prevent duplicate bill names
            for (BillReminder existing : billList) {
                if (existing.getName().equalsIgnoreCase(name)) {
                    Toast.makeText(this, "A bill with this name already exists.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            double amount = Double.parseDouble(amountText);
            long dueDate = selectedDate.getTimeInMillis();

            BillReminder bill = new BillReminder(name, amount, dueDate);
            billList.add(bill);
            adapter.notifyItemInserted(billList.size() - 1);

            scheduleIndividualBillReminder(bill, billList.size());

            if (!billList.isEmpty()) {
                tvEmptyState.setVisibility(View.GONE);
            }

            // ✅ Save to Firestore
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "You must be signed in to save reminders", Toast.LENGTH_LONG).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> billData = new HashMap<>();
            billData.put("name", name);
            billData.put("amount", amount);
            billData.put("dueDateMillis", dueDate);

            db.collection("users")
                    .document(user.getUid())
                    .collection("billReminders")
                    .add(billData)
                    .addOnSuccessListener(docRef -> Toast.makeText(this, "Bill saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error saving bill: " + e.getMessage(), Toast.LENGTH_LONG).show());

            dialog.dismiss();
        });

    }
    private void updateSelectedDateTimeView(TextView textView) {
        java.text.DateFormat format = java.text.DateFormat.getDateTimeInstance();
        textView.setText(format.format(selectedDate.getTime()));
    }

    public interface OnBillDeleteClickListener {
        void onDelete(BillReminder bill, int position);
    }
    private void deleteBillReminder(BillReminder bill, int position) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("billReminders")
                .whereEqualTo("name", bill.getName())
                .whereEqualTo("dueDateMillis", bill.getDueDateMillis())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (var doc : snapshot.getDocuments()) {
                        doc.getReference().delete();
                    }
                    billList.remove(position);
                    adapter.notifyItemRemoved(position);

                    if (billList.isEmpty()) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}