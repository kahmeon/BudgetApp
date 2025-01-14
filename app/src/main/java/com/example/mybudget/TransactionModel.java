package com.example.mybudget;

import android.os.Parcelable;

/**
 * Model class for representing a transaction in the budget app.
 */
public class TransactionModel {
    private double amount;         // Transaction amount
    private String category;       // Transaction category (e.g., Food, Rent, etc.)
    private String paymentMethod;  // Payment method (e.g., Credit Card, Cash)
    private String type;           // Transaction type (Income or Expense)
    private String description;    // Description of the transaction
    private long date;             // Timestamp for the transaction date
    private String imageUrl;       // URL for the image (Firebase Storage)
    private String location;       // Location of the transaction
    private String id;             // Firestore document ID

    /**
     * Default constructor (required for Firebase Firestore).
     */
    public TransactionModel() {}

    /**
     * Parameterized constructor for initializing the transaction model.
     *
     * @param id            Firestore document ID.
     * @param category      The transaction category.
     * @param type          The type of transaction (Income or Expense).
     * @param description   A brief description of the transaction.
     * @param paymentMethod The payment method used.
     * @param amount        The transaction amount.
     * @param date          The timestamp of the transaction.
     * @param imageUrl      URL of the image stored in Firebase Storage.
     * @param location      Location of the transaction.
     */
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

    public void setDate(long date) {
        this.date = date;
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
