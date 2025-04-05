package com.example.mybudget;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currencySymbol;
    private RecyclerView rvTransactions;
    private TransactionsAdapter transactionsAdapter;
    private TextView tvCurrentBalance,tvViewAll;
    private ProgressBar progressBudget;

    private TextView tvTotalIncome, tvTotalExpense,tvSavingsGoal;
    private List<String> paymentMethodsList = new ArrayList<>();

    private List<TransactionModel> transactionsList = new ArrayList<>();
    private ArrayAdapter<String> paymentMethodsAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currencySymbol = getUserCurrencySymbol();

        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        tvSavingsGoal = view.findViewById(R.id.tv_savings_goal);
        progressBudget = view.findViewById(R.id.progress_budget);

        // RecyclerView setup
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter(transactionsList);
        rvTransactions.setAdapter(transactionsAdapter);
        tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        tvViewAll = view.findViewById(R.id.tv_view_all);


        // Load transactions for the specific user
        loadUserTransactions();




        // Add transaction button
        view.findViewById(R.id.fab_add_transaction).setOnClickListener(v -> showAddTransactionDialog());

        // Add swipe-to-delete functionality
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false; // No drag-and-drop functionality
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    // Get the position of the swiped item
                    int position = viewHolder.getAdapterPosition();

                    // Pause swipe by notifying the adapter
                    transactionsAdapter.notifyItemChanged(position);

                    // Display confirmation dialog
                    new AlertDialog.Builder(getContext())
                            .setTitle("Delete Transaction")
                            .setMessage("Are you sure you want to delete this transaction?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                // Delete transaction from the database and list
                                deleteTransactionFromFirestore(position);
                            })
                            .setNegativeButton("No", (dialog, which) -> {
                                // Reset the swiped item
                                dialog.dismiss();
                                transactionsAdapter.notifyItemChanged(position);
                            })
                            .show();
                }
            }
            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);

                    // Draw red background
                    RectF background = new RectF(
                            viewHolder.itemView.getRight() + dX,
                            viewHolder.itemView.getTop(),
                            viewHolder.itemView.getRight(),
                            viewHolder.itemView.getBottom()
                    );
                    c.drawRect(background, paint);

                    // Draw delete icon
                    Drawable deleteIcon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_delete);
                    if (deleteIcon != null) {
                        // Scale the delete icon to smaller dimensions (e.g., 24dp x 24dp)
                        int iconSize = (int) (24 * recyclerView.getContext().getResources().getDisplayMetrics().density); // Convert dp to pixels
                        int iconMargin = (viewHolder.itemView.getHeight() - iconSize) / 2;
                        int iconTop = viewHolder.itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + iconSize;
                        int iconLeft = viewHolder.itemView.getRight() - iconMargin - iconSize;
                        int iconRight = viewHolder.itemView.getRight() - iconMargin;

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(c);
                    }
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvTransactions);



        return view;
    }

    private String getUserCurrencySymbol() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("userPrefs", Context.MODE_PRIVATE);
        String currency = prefs.getString("currency", "USD"); // Default to USD if not set

        switch (currency) {
            case "MYR": return "RM";
            case "USD": return "$";
            case "EUR": return "€";
            default: return "$"; // Default fallback
        }
    }

    private void loadUserTransactions() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        Log.d("TransactionsFragment", "Loading transactions for user: " + userId);

        firestore.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionsList.clear();
                    double totalIncome = 0;
                    double totalExpense = 0;

                    List<TransactionModel> allTransactions = new ArrayList<>(); // Store all transactions for "View All"

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        TransactionModel transaction = document.toObject(TransactionModel.class);
                        if (transaction != null) {
                            transaction.setId(document.getId());
                            Log.d("Firestore Debug", "Transaction Loaded: ID=" + document.getId());
                            allTransactions.add(transaction); // Add to the complete list

                            // Add only the first 3 transactions to the displayed list
                            if (transactionsList.size() < 3) {
                                transactionsList.add(transaction);
                            }

                            // Calculate totals based on transaction type
                            if ("Income".equals(transaction.getType())) {
                                totalIncome += transaction.getAmount();
                            } else if ("Expense".equals(transaction.getType())) {
                                totalExpense += transaction.getAmount();
                            }
                        }
                    }

                    // Update RecyclerView
                    transactionsAdapter.notifyDataSetChanged();

                    // Update Income and Expense TextViews
                    tvTotalIncome.setText(String.format("Income: $%.2f", totalIncome));
                    tvTotalExpense.setText(String.format("Expense: $%.2f", totalExpense));

                    // Calculate and Update Current Balance
                    double currentBalance = totalIncome - totalExpense;
                    tvCurrentBalance.setText(String.format("$%.2f", currentBalance));

                    // Fetch and update the monthly budget progress
                    fetchMonthlyBudgetProgress(userId, totalExpense);

                    // Set up "View All" functionality
                    tvViewAll.setOnClickListener(v -> {
                        // Navigate to TransactionsFragment to show all transactions
                        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragment_container, TransactionsFragment.newInstance(new ArrayList<>(allTransactions)));
                        transaction.addToBackStack(null); // Allow navigation back
                        transaction.commit();
                    });


                    Log.d("TransactionsFragment", "Total Income: " + totalIncome + ", Total Expense: " + totalExpense + ", Current Balance: " + currentBalance);
                })
                .addOnFailureListener(e -> Log.e("TransactionsFragment", "Error loading transactions", e));
    }


    private void fetchMonthlyBudgetProgress(String userId, double totalMonthlyExpense) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double monthlyBudget = documentSnapshot.getDouble("monthlyBudget");

                        if (monthlyBudget != null && monthlyBudget > 0) {
                            // Calculate the monthly budget progress percentage
                            int progressPercentage = (int) ((totalMonthlyExpense / monthlyBudget) * 100);
                            progressPercentage = Math.min(100, Math.max(0, progressPercentage)); // Clamp to 0–100

                            // Update ProgressBar and TextView
                            progressBudget.setProgress(progressPercentage);
                            tvSavingsGoal.setText(String.format("Goal: %s%.2f / %s%.2f",
                                    currencySymbol, totalMonthlyExpense, currencySymbol, monthlyBudget));

                            // Log details for debugging
                            Log.d("BudgetProgress", "Monthly Budget: " + monthlyBudget);
                            Log.d("BudgetProgress", "Total Monthly Expense: " + totalMonthlyExpense);
                            Log.d("BudgetProgress", "Monthly Budget Progress: " + progressPercentage + "%");
                        } else {
                            // Monthly budget not set
                            progressBudget.setProgress(0);
                            tvSavingsGoal.setText("Monthly budget not set.");

                            Log.e("BudgetProgress", "Monthly budget not set or zero.");
                        }
                    } else {
                        // User document does not exist
                        progressBudget.setProgress(0);
                        tvSavingsGoal.setText("Monthly budget not set.");
                        Log.e("BudgetProgress", "User document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    progressBudget.setProgress(0);
                    tvSavingsGoal.setText("Failed to load monthly budget.");
                    Log.e("BudgetProgress", "Failed to load monthly budget", e);
                    Toast.makeText(getContext(), "Failed to load monthly budget", Toast.LENGTH_SHORT).show();
                });
    }




    private void deleteTransactionFromFirestore(int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        TransactionModel transactionToDelete = transactionsList.get(position);

        // Reference to the transaction in Firestore
        firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("date", transactionToDelete.getDate())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Remove the transaction from the local list
                                    transactionsList.remove(position);
                                    transactionsAdapter.notifyItemRemoved(position);
                                    Toast.makeText(getContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show());
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error finding transaction", Toast.LENGTH_SHORT).show());
    }


    private void loadPaymentMethods(Spinner spinner) {
        String userId = auth.getCurrentUser().getUid();

        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(userId).collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paymentMethodsList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        paymentMethodsList.add(document.getString("accountName"));
                    }
                    paymentMethodsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, paymentMethodsList);
                    paymentMethodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(paymentMethodsAdapter);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load payment methods", Toast.LENGTH_SHORT).show());
    }


    private void showAddTransactionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_transaction, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etAmount = dialogView.findViewById(R.id.et_amount);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        Spinner spinnerCategory = dialogView.findViewById(R.id.spinner_category);
        Spinner spinnerPaymentMethod = dialogView.findViewById(R.id.spinner_payment_methods);
        RadioGroup rgType = dialogView.findViewById(R.id.rg_type);
        Button btnSelectDate = dialogView.findViewById(R.id.btn_select_date);
        Button btnAddTransaction = dialogView.findViewById(R.id.btn_add_transaction);

        // Initialize date as today's date as a fallback
        final long[] selectedDate = {System.currentTimeMillis()};
        loadPaymentMethods(spinnerPaymentMethod);

        // Set up date picker dialog
        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);

                // Ensure no future dates are selected
                if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
                    Toast.makeText(getContext(), "You cannot select a future date", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedDate[0] = calendar.getTimeInMillis(); // Store the selected date
                String formattedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                tvSelectedDate.setText(formattedDate);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

            // Set max date to today
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Update categories based on transaction type selection
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isIncome = checkedId == R.id.rb_income;
            setCategoryOptions(spinnerCategory, isIncome);
        });

        setCategoryOptions(spinnerCategory, true);

        btnAddTransaction.setOnClickListener(v -> {
            String amountText = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
            long dateInMillis = System.currentTimeMillis();
            Timestamp timestamp = new Timestamp(new Date(dateInMillis));
            int selectedTypeId = rgType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(amountText) || TextUtils.isEmpty(description) || TextUtils.isEmpty(category) ||
                    selectedTypeId == -1 || TextUtils.isEmpty(paymentMethod)) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountText);
            String type = ((RadioButton) dialogView.findViewById(selectedTypeId)).getText().toString();

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String userId = currentUser.getUid();

            // Prepare transaction data with the selected date
            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("amount", amount);
            transactionData.put("description", description);
            transactionData.put("category", category);
            transactionData.put("paymentMethod", paymentMethod);
            transactionData.put("type", type);
            transactionData.put("date", timestamp); // Store as Timestamp

            // Save transaction to Firestore
            firestore.collection("users").document(userId).collection("transactions")
                    .add(transactionData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(getContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadUserTransactions();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add transaction", Toast.LENGTH_SHORT).show());
        });

        dialog.show();
    }


    private void setCategoryOptions(Spinner spinnerCategory, boolean isIncome) {
        List<String> categories = new ArrayList<>();
        if (isIncome) {
            categories.add("Salary");
            categories.add("Investment");
            categories.add("Other");
        } else {
            categories.add("Food");
            categories.add("Rent");
            categories.add("Transport");
            categories.add("Travel");
            categories.add("Other");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }


}
