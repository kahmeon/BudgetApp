package com.example.mybudget;

public class BudgetHistoryItem {
    private String month;
    private int budget;
    private int spent;
    private int percent;

    public BudgetHistoryItem(String month, int budget, int spent, int percent) {
        this.month = month;
        this.budget = budget;
        this.spent = spent;
        this.percent = percent;
    }

    public String getMonth() {
        return month;
    }

    public int getBudget() {
        return budget;
    }

    public int getSpent() {
        return spent;
    }

    public int getPercent() {
        return percent;
    }
}
