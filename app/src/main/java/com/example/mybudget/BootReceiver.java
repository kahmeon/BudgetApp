package com.example.mybudget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE);
            int hour = prefs.getInt("hour", 8);
            int minute = prefs.getInt("minute", 0);

            Calendar reminderTime = Calendar.getInstance();
            reminderTime.set(Calendar.HOUR_OF_DAY, hour);
            reminderTime.set(Calendar.MINUTE, minute);
            reminderTime.set(Calendar.SECOND, 0);

            NotificationUtils.scheduleDailyReminder(context, reminderTime);
        }
    }
}
