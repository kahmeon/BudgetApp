package com.example.mybudget;

public class BudgetCategory {
    private String name;
    private int amount;
    private int spent;

    // Default constructor
    public BudgetCategory(String name, int amount) {
        this.name = name;
        this.amount = amount;
        this.spent = 0;
    }

    // Overloaded constructor to include spent
    public BudgetCategory(String name, int amount, int spent) {
        this.name = name;
        this.amount = amount;
        this.spent = spent;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getSpent() {
        return spent;
    }

    public void setSpent(int spent) {
        this.spent = spent;
    }

    public int getRemaining() {
        return amount - spent;
    }

    public int getProgressPercentage() {
        if (amount == 0) return 0;
        return (int) ((spent * 100.0f) / amount);
    }
}
