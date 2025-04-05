package com.example.mybudget;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SavingGoalsActivity extends AppCompatActivity {

    private EditText goalInput, targetInput;
    private Button saveGoalButton;
    private RecyclerView goalsRecyclerView;

    private FirebaseFirestore db;
    private String userId;

    private GoalAdapter adapter;
    private ArrayList<GoalItem> goalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savinggoals);

        goalInput = findViewById(R.id.goal_name_input);
        targetInput = findViewById(R.id.goal_amount_input);
        saveGoalButton = findViewById(R.id.save_goal_button);
        goalsRecyclerView = findViewById(R.id.goals_recycler);
        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        // ✅ Use the correct listener implementation here
        adapter = new GoalAdapter(goalList, new GoalAdapter.OnGoalActionListener() {
            @Override
            public void onDelete(String goalId) {
                deleteGoal(goalId);
            }

            @Override
            public void onUpdate(GoalItem goalItem, int newSavedAmount) {
                updateGoal(goalItem, newSavedAmount);
            }
        });

        goalsRecyclerView.setAdapter(adapter);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadGoals();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        saveGoalButton.setOnClickListener(v -> {
            String goal = goalInput.getText().toString();
            String targetText = targetInput.getText().toString();
            if (!goal.isEmpty() && !targetText.isEmpty()) {
                try {
                    int target = Integer.parseInt(targetText);
                    saveGoal(goal, target);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid target amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveGoal(String goal, int amount) {
        String goalId = db.collection("users").document(userId).collection("savings").document().getId();
        DocumentReference goalRef = db.collection("users").document(userId).collection("savings").document(goalId);

        Map<String, Object> data = new HashMap<>();
        data.put("name", goal);
        data.put("target", amount);
        data.put("saved", 0);

        goalRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Saving goal added", Toast.LENGTH_SHORT).show();
                    goalInput.setText("");
                    targetInput.setText("");
                    loadGoals();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save goal: " + e.getMessage(), Toast.LENGTH_LONG).show());

    }

    private void loadGoals() {
        db.collection("users").document(userId).collection("savings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    goalList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        Long target = doc.getLong("target");
                        Long saved = doc.getLong("saved");

                        if (name != null && target != null && saved != null) {
                            goalList.add(new GoalItem(id, name, target.intValue(), saved.intValue()));
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteGoal(String goalId) {
        db.collection("users").document(userId).collection("savings").document(goalId)
                .delete()
                .addOnSuccessListener(unused -> loadGoals());
    }

    private void updateGoal(GoalItem goalItem, int newSavedAmount) {
        db.collection("users").document(userId).collection("savings").document(goalItem.getId())
                .update("saved", newSavedAmount)
                .addOnSuccessListener(unused -> loadGoals());
    }
}
