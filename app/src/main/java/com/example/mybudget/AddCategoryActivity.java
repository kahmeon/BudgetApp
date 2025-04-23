package com.example.mybudget;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddCategoryActivity extends AppCompatActivity {

    private EditText categoryNameInput;
    private ChipGroup categoryTypeGroup;
    private Button saveCategoryButton;
    private RecyclerView categoriesRecyclerView;

    private FirebaseFirestore db;
    private String userId;
    private static final String TAG = "AddCategoryActivity";

    private CategoryAdapter categoryAdapter;
    private final ArrayList<CategoryModel> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category);

        // Init views
        categoryNameInput = findViewById(R.id.category_name_input);
        categoryTypeGroup = findViewById(R.id.category_type_group);
        saveCategoryButton = findViewById(R.id.save_category_btn);
        categoriesRecyclerView = findViewById(R.id.categories_recycler_view);

        // Firebase
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // RecyclerView setup
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoryAdapter = new CategoryAdapter(categoryList, new CategoryAdapter.OnCategoryActionListener() {
            @Override
            public void onEdit(CategoryModel category) {
                // Placeholder for future edit functionality
                Toast.makeText(AddCategoryActivity.this, "Edit " + category.getName(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(CategoryModel category) {
                db.collection("users")
                        .document(userId)
                        .collection("categories")
                        .document(category.getId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AddCategoryActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                            loadCategories();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddCategoryActivity.this, "Failed to delete category", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        categoriesRecyclerView.setAdapter(categoryAdapter);

        loadCategories(); // Load existing categories

        saveCategoryButton.setOnClickListener(v -> {
            String categoryName = categoryNameInput.getText().toString().trim();
            int selectedId = categoryTypeGroup.getCheckedChipId();
            String categoryType = selectedId == R.id.income_radio ? "Income" : "Expense";

            if (categoryName.isEmpty()) {
                Toast.makeText(this, "Category name is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("name", categoryName);
            categoryData.put("type", categoryType);

            db.collection("users")
                    .document(userId)
                    .collection("categories")
                    .add(categoryData)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, "Category added!", Toast.LENGTH_SHORT).show();
                        loadCategories(); // Refresh list
                        categoryNameInput.setText(""); // Clear input
                        categoryTypeGroup.check(R.id.income_radio); // Reset
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add category", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void loadCategories() {
        db.collection("users")
                .document(userId)
                .collection("categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categoryList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String name = doc.getString("name");
                        String type = doc.getString("type");
                        String id = doc.getId(); // Get the Firestore document ID
                        if (name != null && type != null) {
                            categoryList.add(new CategoryModel(id, name, type));
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to load categories", e));
    }

    private void importDefaultCategoriesIfNeeded() {
        db.collection("users")
                .document(userId)
                .collection("metadata")
                .document("settings")
                .get()
                .addOnSuccessListener(doc -> {
                    Boolean hasImported = doc.getBoolean("hasImportedDefaults");
                    if (hasImported == null || !hasImported) {
                        // Import default categories only once
                        importDefaultCategories();

                        // Set flag so this never runs again
                        Map<String, Object> flag = new HashMap<>();
                        flag.put("hasImportedDefaults", true);
                        db.collection("users")
                                .document(userId)
                                .collection("metadata")
                                .document("settings")
                                .set(flag);
                    }
                });
    }
    private void importDefaultCategories() {
        String[][] defaults = {
                {"Groceries", "Expense"},
                {"Transport", "Expense"},
                {"Salary", "Income"},
                {"Utilities", "Expense"},
                {"Freelance", "Income"}
        };

        for (String[] item : defaults) {
            Map<String, Object> category = new HashMap<>();
            category.put("name", item[0]);
            category.put("type", item[1]);

            db.collection("users")
                    .document(userId)
                    .collection("categories")
                    .add(category);
        }

        Toast.makeText(this, "Default categories imported", Toast.LENGTH_SHORT).show();
    }

}
