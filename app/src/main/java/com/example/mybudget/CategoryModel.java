package com.example.mybudget;

public class CategoryModel {
    private String id;   // <-- Firestore document ID
    private String name;
    private String type;

    // Required empty constructor for Firestore
    public CategoryModel() {}

    // Constructor with ID (used when loading from Firestore)
    public CategoryModel(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    // Optional setters if you need them
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }
}
