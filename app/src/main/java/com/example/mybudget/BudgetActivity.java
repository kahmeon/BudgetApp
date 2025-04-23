package com.example.mybudget;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
import com.example.mybudget.BudgetCategoryAdapter.OnCategoryClickListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetActivity extends AppCompatActivity {

    // UI Components
    private TextView currentBudget, budgetProgressText, totalBudgetText, totalSpentText;
    private EditText budgetInput;
    private FloatingActionButton setBudgetButton, addCategoryButton;
    private Button saveBudgetButton;
    private ProgressBar budgetProgress;
    private RecyclerView budgetHistoryRecycler, categoriesRecycler;

    // Firebase
    private FirebaseFirestore db;
    private String userId;

    // Adapters
    private BudgetHistoryAdapter historyAdapter;
    private BudgetCategoryAdapter categoryAdapter;

    // Data
    private List<BudgetHistoryItem> historyItems = new ArrayList<>();
    private List<BudgetCategory> categories = new ArrayList<>();
    private int currentMonthlyBudget = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.budget);

        initializeViews();
        initializeFirebase();
        setupRecyclerViews();
        setupClickListeners();
        loadData();
    }

    private void initializeViews() {
        currentBudget = findViewById(R.id.current_budget);
        totalBudgetText = findViewById(R.id.total_budget);
        totalSpentText = findViewById(R.id.total_spent);
        budgetInput = findViewById(R.id.budget_input);
        setBudgetButton = findViewById(R.id.set_budget_button);
        addCategoryButton = findViewById(R.id.add_category_fab);
        saveBudgetButton = findViewById(R.id.save_budget_button);
        budgetProgress = findViewById(R.id.budget_progress);
        budgetProgressText = findViewById(R.id.budget_progress_text);
        budgetHistoryRecycler = findViewById(R.id.budget_history_recycler);
        categoriesRecycler = findViewById(R.id.categories_recycler);
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = user.getUid();
    }

    private void setupRecyclerViews() {
        // Budget History RecyclerView
        historyAdapter = new BudgetHistoryAdapter(historyItems);
        budgetHistoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        budgetHistoryRecycler.setAdapter(historyAdapter);

        // Categories RecyclerView
        categoryAdapter = new BudgetCategoryAdapter(categories, this::onCategoryClicked);
        categoriesRecycler.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecycler.setAdapter(categoryAdapter);
    }

    private void setupClickListeners() {
        setBudgetButton.setOnClickListener(v -> showBudgetInput());

        saveBudgetButton.setOnClickListener(v -> {
            String budgetText = budgetInput.getText().toString().trim();
            if (budgetText.isEmpty()) {
                showToast("Please enter a budget amount");
                return;
            }
            try {
                int amount = Integer.parseInt(budgetText);
                if (amount <= 0) {
                    showToast("Budget amount must be positive");
                    return;
                }
                setMonthlyBudget(amount);
            } catch (NumberFormatException e) {
                showToast("Invalid budget amount");
            }
        });

        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void loadData() {
        loadBudget();
        loadBudgetHistory();
        loadCategories();
    }

    private void showBudgetInput() {
        findViewById(R.id.input_budget_layout).setVisibility(View.VISIBLE);
        saveBudgetButton.setVisibility(View.VISIBLE);
        budgetInput.requestFocus();
    }

    private void hideBudgetInput() {
        findViewById(R.id.input_budget_layout).setVisibility(View.GONE);
        saveBudgetButton.setVisibility(View.GONE);
        budgetInput.setText("");
    }

    private void loadBudgetHistory() {
        db.collection("users")
                .document(userId)
                .collection("budget")
                .document("monthly")
                .collection("budget_history")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    historyItems.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String month = doc.getId();
                        Long budget = doc.getLong("monthly_budget");
                        Long spent = doc.getLong("spent");

                        if (budget != null && spent != null) {
                            int percent = (int) ((spent * 100.0f) / budget);
                            historyItems.add(new BudgetHistoryItem(month, budget.intValue(), spent.intValue(), percent));
                        }
                    }
                    historyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load budget history: " + e.getMessage());
                });
    }

    private void setMonthlyBudget(int amount) {
        DocumentReference budgetRef = getBudgetDocumentReference();

        Map<String, Object> budgetData = new HashMap<>();
        budgetData.put("monthly_budget", amount);
        budgetData.put("setAt", new Timestamp(new Date()));
        budgetData.put("categories", new HashMap<>()); // Initialize empty categories map

        budgetRef.set(budgetData)
                .addOnSuccessListener(aVoid -> {
                    currentMonthlyBudget = amount;
                    updateBudgetUI(amount);
                    hideBudgetInput();
                    showToast("Budget set: RM" + amount);
                    calculateSpendingAndUpdateProgress(amount);
                })
                .addOnFailureListener(e -> showToast("Failed to set budget: " + e.getMessage()));
    }

    private void updateBudgetUI(int amount) {
        String month = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        currentBudget.setText("Budget for " + month + ": RM " + amount);
        totalBudgetText.setText("RM " + amount);
    }

    private void loadBudget() {
        getBudgetDocumentReference().get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long budget = documentSnapshot.getLong("monthly_budget");
                        if (budget != null) {
                            currentMonthlyBudget = budget.intValue();
                            updateBudgetUI(budget.intValue());
                            checkForMonthChange(documentSnapshot);
                        }
                    }
                })
                .addOnFailureListener(e -> showToast("Failed to load budget"));
    }

    private void checkForMonthChange(DocumentSnapshot documentSnapshot) {
        Timestamp setAt = documentSnapshot.getTimestamp("setAt");
        if (setAt != null) {
            String storedMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(setAt.toDate());
            String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

            if (!storedMonth.equals(currentMonth)) {
                archiveOldBudget(storedMonth);
            } else {
                calculateSpendingAndUpdateProgress(currentMonthlyBudget);
            }
        }
    }

    private void archiveOldBudget(String monthKey) {
        calculateSpendingAndArchive(currentMonthlyBudget, monthKey);
        getBudgetDocumentReference().delete()
                .addOnSuccessListener(aVoid -> {
                    currentMonthlyBudget = 0;
                    updateBudgetUI(0);
                    budgetProgress.setProgress(0);
                    budgetProgressText.setText("0% of budget used");
                    totalSpentText.setText("RM 0");
                });
    }



    private void calculateSpendingAndUpdateProgress(int budget) {
        if (budget <= 0) return;

        Date[] monthRange = getCurrentMonthRange();
        db.collection("users").document(userId)
                .collection("transactions")
                .whereEqualTo("type", "Expense")
                .whereGreaterThanOrEqualTo("date", new Timestamp(monthRange[0]))
                .whereLessThanOrEqualTo("date", new Timestamp(monthRange[1]))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int totalSpent = calculateTotalSpent(querySnapshot);
                    updateProgressUI(budget, totalSpent);
                    checkBudgetThreshold(totalSpent, budget);
                })
                .addOnFailureListener(e -> showToast("Failed to calculate spending"));
    }

    private void updateProgressUI(int budget, int totalSpent) {
        int percentSpent = (int) ((totalSpent * 100.0f) / budget);
        budgetProgress.setProgress(percentSpent);
        budgetProgressText.setText(percentSpent + "% of budget used");
        totalSpentText.setText("RM " + totalSpent);
    }

    private void checkBudgetThreshold(int totalSpent, int budget) {
        int percentSpent = (int) ((totalSpent * 100.0f) / budget);
        if (percentSpent >= 80) {
            showToast("⚠️ You've used " + percentSpent + "% of your budget!");
        }
    }

    private void loadCategories() {
        getBudgetDocumentReference().get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("categories")) {
                        Map<String, Object> categoriesMap = (Map<String, Object>) documentSnapshot.get("categories");
                        categories.clear();
                        for (Map.Entry<String, Object> entry : categoriesMap.entrySet()) {
                            if (entry.getValue() instanceof Map) {
                                Map<String, Object> data = (Map<String, Object>) entry.getValue();
                                int budget = ((Number) data.getOrDefault("budget", 0)).intValue();
                                int spent = ((Number) data.getOrDefault("spent", 0)).intValue();
                                categories.add(new BudgetCategory(entry.getKey(), budget, spent));
                            }
                        }

                        categoryAdapter.notifyDataSetChanged();
                    }
                });
    }




    private void onCategoryClicked(BudgetCategory category) {
        // Show edit dialog for the clicked category
        showEditCategoryDialog(category);
    }

    private void showAddCategoryDialog() {
        db.collection("users")
                .document(userId)
                .collection("categories")
                .whereEqualTo("type", "Expense")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> categoryNames = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        if (name != null) {
                            categoryNames.add(name);
                        }
                    }

                    // Now build the dialog once you have categories
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Set Category Budget");

                    View view = getLayoutInflater().inflate(R.layout.dialog_edit_category, null);
                    Spinner categorySpinner = view.findViewById(R.id.category_spinner);
                    EditText amountInput = view.findViewById(R.id.category_amount_input);

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    categorySpinner.setAdapter(spinnerAdapter);

                    builder.setView(view);
                    builder.setPositiveButton("Save", (dialog, which) -> {
                        String selectedCategory = categorySpinner.getSelectedItem().toString();
                        String amountStr = amountInput.getText().toString().trim();

                        if (amountStr.isEmpty()) {
                            showToast("Please enter a budget amount");
                            return;
                        }

                        try {
                            int amount = Integer.parseInt(amountStr);
                            if (amount <= 0) {
                                showToast("Amount must be positive");
                                return;
                            }

                            addCategoryToFirestore(selectedCategory, amount);
                        } catch (NumberFormatException e) {
                            showToast("Invalid budget amount");
                        }
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.show();

                })
                .addOnFailureListener(e -> showToast("Failed to load categories"));
    }


    private void addCategoryToFirestore(String name, int amount) {
        DocumentReference budgetRef = getBudgetDocumentReference();

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> categories = new HashMap<>();
            if (documentSnapshot.exists() && documentSnapshot.contains("categories")) {
                categories = (Map<String, Object>) documentSnapshot.get("categories");
            }

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("budget", amount);
            categoryData.put("spent", 0);

            categories.put(name, categoryData);

            budgetRef.update("categories", categories)
                    .addOnSuccessListener(aVoid -> {
                        showToast("Budget set for " + name);
                        loadCategories(); // Refresh UI
                    })
                    .addOnFailureListener(e -> showToast("Failed to save category budget"));
        });
    }


    private void showEditCategoryDialog(BudgetCategory category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Category");

        View view = getLayoutInflater().inflate(R.layout.dialog_edit_category, null);
        Spinner categorySpinner = view.findViewById(R.id.category_spinner);
        EditText amountInput = view.findViewById(R.id.category_amount_input);

        // Set current amount
        amountInput.setText(String.valueOf(category.getAmount()));

        // Populate the spinner with category names
        List<String> categoryNames = new ArrayList<>();
        for (BudgetCategory c : categories) {
            categoryNames.add(c.getName());
        }

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Set current selection in spinner
        int selectedIndex = categoryNames.indexOf(category.getName());
        if (selectedIndex != -1) {
            categorySpinner.setSelection(selectedIndex);
        }

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String selectedCategory = categorySpinner.getSelectedItem().toString();
            String amountStr = amountInput.getText().toString().trim();

            if (!amountStr.isEmpty()) {
                try {
                    int amount = Integer.parseInt(amountStr);
                    updateCategoryInFirestore(category.getName(), selectedCategory, amount);
                } catch (NumberFormatException e) {
                    showToast("Invalid amount");
                }
            } else {
                showToast("Please enter an amount");
            }
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Delete", (dialog, which) -> {
            deleteCategoryFromFirestore(category.getName());
        });

        builder.show();
    }




    private void updateCategoryInFirestore(String oldName, String newName, int amount) {
        DocumentReference budgetRef = getBudgetDocumentReference();

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> categoriesMap = (Map<String, Object>) documentSnapshot.get("categories");

                if (categoriesMap != null) {
                    categoriesMap.remove(oldName); // remove old
                    categoriesMap.put(newName, amount); // add new/updated

                    budgetRef.update("categories", categoriesMap)
                            .addOnSuccessListener(aVoid -> {
                                showToast("Category updated");
                                loadCategories();
                            })
                            .addOnFailureListener(e -> showToast("Failed to update category"));
                }
            }
        });
    }

    private void deleteCategoryFromFirestore(String name) {
        DocumentReference budgetRef = getBudgetDocumentReference();

        budgetRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Map<String, Object> categoriesMap = (Map<String, Object>) documentSnapshot.get("categories");

                if (categoriesMap != null) {
                    categoriesMap.remove(name);

                    budgetRef.update("categories", categoriesMap)
                            .addOnSuccessListener(aVoid -> {
                                showToast("Category deleted");
                                loadCategories();
                            })
                            .addOnFailureListener(e -> showToast("Failed to delete category"));
                }
            }
        });
    }




    private DocumentReference getBudgetDocumentReference() {
        return db.collection("users").document(userId)
                .collection("budget").document("monthly");
    }

    private Date[] getCurrentMonthRange() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endOfMonth = calendar.getTime();

        return new Date[]{startOfMonth, endOfMonth};
    }

    private int calculateTotalSpent(com.google.firebase.firestore.QuerySnapshot querySnapshot) {
        int total = 0;
        for (QueryDocumentSnapshot doc : querySnapshot) {
            Long amount = doc.getLong("amount");
            if (amount != null) total += amount;
        }
        return total;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Keep your existing calculateSpendingAndArchive and loadBudgetHistory methods
    private void calculateSpendingAndArchive(int budget, String monthKey) {
        // Parse the monthKey into start and end dates for that month
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        try {
            Date monthDate = format.parse(monthKey);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(monthDate);

            // Set to first day of month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date startOfMonth = calendar.getTime();

            // Set to last day of month
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date endOfMonth = calendar.getTime();

            // Query expenses for that month
            db.collection("users").document(userId)
                    .collection("transactions")
                    .whereEqualTo("type", "Expense")
                    .whereGreaterThanOrEqualTo("date", new Timestamp(startOfMonth))
                    .whereLessThanOrEqualTo("date", new Timestamp(endOfMonth))
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int totalSpent = 0;
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Long amount = doc.getLong("amount");
                            if (amount != null) {
                                totalSpent += amount;
                            }
                        }

                        // Create archive record
                        Map<String, Object> historyData = new HashMap<>();
                        historyData.put("monthly_budget", budget);
                        historyData.put("spent", totalSpent);
                        historyData.put("archivedAt", new Timestamp(new Date()));

                        // Save to history collection
                        db.collection("users").document(userId)
                                .collection("budget_history")
                                .document(monthKey)
                                .set(historyData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("BudgetActivity", "Budget archived for " + monthKey);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("BudgetActivity", "Error archiving budget", e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("BudgetActivity", "Error calculating spending", e);
                    });
        } catch (ParseException e) {
            Log.e("BudgetActivity", "Error parsing month key", e);
        }
    }
    // but apply similar improvements to them
}