package com.example.mybudget;

public class BillModel {
    private String id;
    private String title;
    private long reminderTime; // in milliseconds
    private boolean isRepeating;

    public BillModel() {} // Required for Firebase

    public BillModel(String id, String title, long reminderTime, boolean isRepeating) {
        this.id = id;
        this.title = title;
        this.reminderTime = reminderTime;
        this.isRepeating = isRepeating;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public boolean isRepeating() {
        return isRepeating;
    }
}
