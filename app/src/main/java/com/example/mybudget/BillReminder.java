package com.example.mybudget;

public class BillReminder {
    private String name;
    private double amount;
    private long dueDateMillis;

    public BillReminder(String name, double amount, long dueDateMillis) {
        this.name = name;
        this.amount = amount;
        this.dueDateMillis = dueDateMillis;
    }

    public String getName() { return name; }
    public double getAmount() { return amount; }
    public long getDueDateMillis() { return dueDateMillis; }
}
