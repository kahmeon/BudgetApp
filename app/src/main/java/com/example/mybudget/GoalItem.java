package com.example.mybudget;



    // Required no-argument constructor for Firestore
    public class GoalItem {
        private String id;
        private String name;
        private double savedAmount;
        private double targetAmount;

        // ✅ REQUIRED: No-argument constructor for Firestore
        public GoalItem() {}

        public GoalItem(String name, double savedAmount, double targetAmount) {
            this.name = name;
            this.savedAmount = savedAmount;
            this.targetAmount = targetAmount;
        }

        public GoalItem(String id, String name, double savedAmount, double targetAmount) {
            this.id = id;
            this.name = name;
            this.savedAmount = savedAmount;
            this.targetAmount = targetAmount;
        }
        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public double getSavedAmount() {
            return savedAmount;
        }

        public double getTargetAmount() {
            return targetAmount;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSavedAmount(double savedAmount) {
            this.savedAmount = savedAmount;
        }

        public void setTargetAmount(double targetAmount) {
            this.targetAmount = targetAmount;
        }
    }
