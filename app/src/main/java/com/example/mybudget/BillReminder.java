package com.example.mybudget;

public class BillReminder {
    private String name;
    private double amount;
    private long dueDateMillis;
    private String documentId;

    public BillReminder(String name, double amount, long dueDateMillis, String documentId) {
        this.name = name;
        this.amount = amount;
        this.dueDateMillis = dueDateMillis;
        this.documentId = documentId;
    }


    public BillReminder(String name, double amount, long dueDateMillis) {
        this(name, amount, dueDateMillis, null);
    }

    public String getName() { return name; }
    public double getAmount() { return amount; }
    public long getDueDateMillis() { return dueDateMillis; }

    public String getDocumentId() {
        return documentId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setDueDateMillis(long dueDateMillis) {
        this.dueDateMillis = dueDateMillis;
    }

}
