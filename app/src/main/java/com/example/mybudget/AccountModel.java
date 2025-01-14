package com.example.mybudget;

import java.util.ArrayList;
import java.util.List;

public class AccountModel {
    private String accountName;
    private double accountBalance; // Balance = income - expense
    private List<TransactionModel> transactions; // Store all related transactions

    // No-argument constructor for Firestore
    public AccountModel() {
        this.transactions = new ArrayList<>();
    }

    public AccountModel(String accountName, double accountBalance) {
        this.accountName = accountName;
        this.accountBalance = accountBalance;
        this.transactions = new ArrayList<>();
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public double getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(double accountBalance) {
        this.accountBalance = accountBalance;
    }

    public List<TransactionModel> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<TransactionModel> transactions) {
        this.transactions = transactions;
        calculateAccountBalance(); // Recalculate when transactions are updated
    }

    // Add individual transaction and recalculate balance
    public void addTransaction(TransactionModel transaction) {
        this.transactions.add(transaction);
        calculateAccountBalance();
    }

    // Calculate balance = income - expense
    private void calculateAccountBalance() {
        double income = 0;
        double expense = 0;

        for (TransactionModel transaction : transactions) {
            if ("Income".equalsIgnoreCase(transaction.getType())) {
                income += transaction.getAmount();
            } else if ("Expense".equalsIgnoreCase(transaction.getType())) {
                expense += transaction.getAmount();
            }
        }

        this.accountBalance = income - expense;
    }
}
