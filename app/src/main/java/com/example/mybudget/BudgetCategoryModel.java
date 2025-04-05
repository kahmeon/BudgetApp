package com.example.mybudget;

public class BudgetCategoryModel {
    private String category;
    private double amount;

    public BudgetCategoryModel() {
        // Default constructor required for Firestore
    }

    public BudgetCategoryModel(String category, double amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }
}
