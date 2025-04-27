package com.example.mybudget;

import com.google.firebase.Timestamp;

/**
 * Model class for representing a transaction in the budget app.
 */
public class TransactionModel {
    private double amount;
    private String category;
    private String paymentMethod;
    private String type;
    private String description;
    private long date;
    private String imageUrl;
    private String location;
    private String id;

    // 🔹 Default constructor (needed for Firestore deserialization)
    public TransactionModel() {}

    // 🔹 Full constructor
    public TransactionModel(String id, String category, String type, String description, String paymentMethod,
                            double amount, long date, String imageUrl, String location) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.type = type;
        this.description = description;
        this.date = date;
        this.imageUrl = imageUrl;
        this.location = location;
    }

    // 🔹 NEW: Simplified constructor for use in HomeFragment when adding a transaction
    public TransactionModel(String id, double amount, String description, String category,
                            String paymentMethod, String type, Timestamp timestamp) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.paymentMethod = paymentMethod;
        this.type = type;
        this.date = timestamp.toDate().getTime(); // convert Timestamp to long
        this.imageUrl = null;
        this.location = null;
    }

    // Getters and Setters

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDate() {
        return date;
    }

    public void setDate(Object date) {
        if (date instanceof Timestamp) {
            this.date = ((Timestamp) date).toDate().getTime();
        } else if (date instanceof Long) {
            this.date = (Long) date;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "TransactionModel{" +
                "id='" + id + '\'' +
                ", amount=" + amount +
                ", category='" + category + '\'' +
                ", paymentMethod='" + paymentMethod + '\'' +
                ", type='" + type + '\'' +
                ", description='" + description + '\'' +
                ", date=" + date +
                ", location='" + location + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
