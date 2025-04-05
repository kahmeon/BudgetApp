package com.example.mybudget;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    private TextView currentBudget, budgetProgressText;
    private EditText budgetInput;
    private FloatingActionButton setBudgetButton;
    private Button saveBudgetButton;
    private ProgressBar budgetProgress;
    private RecyclerView budgetHistoryRecycler;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget);

        currentBudget = findViewById(R.id.current_budget);
        budgetInput = findViewById(R.id.budget_input);
        setBudgetButton = findViewById(R.id.set_budget_button);
        saveBudgetButton = findViewById(R.id.save_budget_button);
        budgetProgress = findViewById(R.id.budget_progress);
        budgetProgressText = findViewById(R.id.budget_progress_text);
        budgetHistoryRecycler = findViewById(R.id.budget_history_recycler);

        db = FirebaseFirestore.getInstance();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            userId = user.getUid();
            loadBudget();
            loadBudgetHistory();
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        setBudgetButton.setOnClickListener(v -> {
            findViewById(R.id.input_budget_layout).setVisibility(View.VISIBLE);
            saveBudgetButton.setVisibility(View.VISIBLE);
            budgetInput.requestFocus();
        });

        saveBudgetButton.setOnClickListener(v -> {
            String budgetText = budgetInput.getText().toString();
            if (!budgetText.isEmpty()) {
                try {
                    int amount = Integer.parseInt(budgetText);
                    setMonthlyBudget(amount);
                } catch (NumberFormatException e) {
                    Toast.makeText(BudgetActivity.this, "Invalid budget amount", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(BudgetActivity.this, "Please enter a budget", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setMonthlyBudget(int amount) {
        if (userId == null) return;

        DocumentReference userBudgetRef = db.collection("users").document(userId)
                .collection("budget").document("monthly");

        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("monthly_budget", amount);
        budgetData.put("setAt", new Timestamp(new Date()));

        userBudgetRef.set(budgetData)
                .addOnSuccessListener(aVoid -> {
                    String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
                    currentBudget.setText("Budget for " + month + ": RM " + amount);
                    findViewById(R.id.input_budget_layout).setVisibility(View.GONE);
                    saveBudgetButton.setVisibility(View.GONE);
                    budgetInput.setText("");
                    calculateSpendingAndUpdateProgress(amount);
                    Toast.makeText(BudgetActivity.this, "Budget Set: RM" + amount, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BudgetActivity.this, "Failed to set budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadBudget() {
        if (userId == null) return;

        DocumentReference userBudgetRef = db.collection("users").document(userId)
                .collection("budget").document("monthly");

        userBudgetRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("monthly_budget")) {
                        Long budget = documentSnapshot.getLong("monthly_budget");
                        Timestamp setAt = documentSnapshot.getTimestamp("setAt");
                        if (budget != null && setAt != null) {
                            String storedMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(setAt.toDate());
                            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

                            if (!storedMonth.equals(currentMonth)) {
                                // Auto-archive old budget to history
                                calculateSpendingAndArchive(budget.intValue(), storedMonth);
                                userBudgetRef.delete();
                                currentBudget.setText("No budget set");
                                budgetProgress.setProgress(0);
                                budgetProgressText.setText("Spent: RM 0 / RM 0 (0%)");
                            } else {
                                currentBudget.setText("Budget for " + new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date()) + ": RM " + budget);
                                calculateSpendingAndUpdateProgress(budget.intValue());
                            }
                        }
                    } else {
                        currentBudget.setText("No budget set");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BudgetActivity.this, "Failed to load budget: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void calculateSpendingAndArchive(int budget, String monthKey) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -1);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endOfMonth = calendar.getTime();

        db.collection("users").document(userId)
                .collection("transactions")
                .whereEqualTo("type", "Expense")
                .whereGreaterThanOrEqualTo("date", new Timestamp(startOfMonth))
                .whereLessThanOrEqualTo("date", new Timestamp(endOfMonth))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalSpent = 0;
                    for (var doc : querySnapshot.getDocuments()) {
                        Long amount = doc.getLong("amount");
                        if (amount != null) totalSpent += amount;
                    }
                    Map<String, Object> historyData = new HashMap<>();
                    historyData.put("monthly_budget", budget);
                    historyData.put("spent", totalSpent);
                    db.collection("users").document(userId)
                            .collection("budget").document("monthly")
                            .collection("budget_history")
                            .document(monthKey)
                            .set(historyData);
                });
    }

    private void calculateSpendingAndUpdateProgress(int budget) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endOfMonth = calendar.getTime();

        db.collection("users").document(userId)
                .collection("transactions")
                .whereEqualTo("type", "Expense")
                .whereGreaterThanOrEqualTo("date", new Timestamp(startOfMonth))
                .whereLessThanOrEqualTo("date", new Timestamp(endOfMonth))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalSpent = 0;
                    for (var doc : querySnapshot.getDocuments()) {
                        Long amount = doc.getLong("amount");
                        if (amount != null) totalSpent += amount;
                    }
                    int percentSpent = (int) ((totalSpent * 100.0f) / budget);
                    budgetProgress.setProgress(percentSpent);
                    budgetProgressText.setText("Spent: RM " + totalSpent + " / RM " + budget + " (" + percentSpent + "%)");

                    if (percentSpent >= 80) {
                        Toast.makeText(this, "⚠️ You're above 80% of your monthly budget!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BudgetActivity.this, "Failed to calculate spending: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void loadBudgetHistory() {
        db.collection("users")
                .document(userId)
                .collection("budget")
                .document("monthly")
                .collection("budget_history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    ArrayList<BudgetHistoryItem> historyItems = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String month = doc.getId();
                        Long budget = doc.getLong("monthly_budget");
                        Long spent = doc.getLong("spent");

                        if (budget != null && spent != null) {
                            int percent = (int) ((spent * 100.0f) / budget);
                            historyItems.add(new BudgetHistoryItem(month, budget.intValue(), spent.intValue(), percent));
                        }
                    }
                    BudgetHistoryAdapter adapter = new BudgetHistoryAdapter(historyItems);
                    budgetHistoryRecycler.setLayoutManager(new LinearLayoutManager(this));
                    budgetHistoryRecycler.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load budget history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
