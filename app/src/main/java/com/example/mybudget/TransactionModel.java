package com.example.mybudget;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;

/**
 * Model class for representing a transaction in the budget app.
 */
public class TransactionModel implements Parcelable {
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

    // 🔹 Constructor for HomeFragment
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

    // 🔹 Parcelable constructor
    protected TransactionModel(Parcel in) {
        amount = in.readDouble();
        category = in.readString();
        paymentMethod = in.readString();
        type = in.readString();
        description = in.readString();
        date = in.readLong();
        imageUrl = in.readString();
        location = in.readString();
        id = in.readString();
    }

    public static final Creator<TransactionModel> CREATOR = new Creator<TransactionModel>() {
        @Override
        public TransactionModel createFromParcel(Parcel in) {
            return new TransactionModel(in);
        }

        @Override
        public TransactionModel[] newArray(int size) {
            return new TransactionModel[size];
        }
    };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(amount);
        dest.writeString(category);
        dest.writeString(paymentMethod);
        dest.writeString(type);
        dest.writeString(description);
        dest.writeLong(date);
        dest.writeString(imageUrl);
        dest.writeString(location);
        dest.writeString(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    // Getters and Setters
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public long getDate() { return date; }
    public void setDate(Object date) {
        if (date instanceof Timestamp) {
            this.date = ((Timestamp) date).toDate().getTime();
        } else if (date instanceof Long) {
            this.date = (Long) date;
        }
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public boolean hasImage() { return imageUrl != null && !imageUrl.isEmpty(); }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
