package com.example.mybudget;

public class GoalItem {
    private String id;
    private String name;
    private int targetAmount;
    private int savedAmount;

    public GoalItem(String id, String name, int targetAmount, int savedAmount) {
        this.id = id;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTargetAmount() {
        return targetAmount;
    }

    public int getSavedAmount() {
        return savedAmount;
    }

    public void setSavedAmount(int savedAmount) {
        this.savedAmount = savedAmount;
    }
}
