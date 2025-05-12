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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
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
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyTransactions;
    private ProgressBar progressLoading;
    private SavingGoalsAdapter savingGoalsAdapter;


    private ConnectivityManager.NetworkCallback networkCallback;

    private RecyclerView recyclerViewSavingGoals;


    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e("UncaughtException", "App Crashed", throwable);
        });

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupNetworkMonitoring();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupNetworkMonitoring();
    }

    @Override
    public void onStop() {
        super.onStop();
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (networkCallback != null && cm != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private void setupNetworkMonitoring() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder().build();
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                syncPendingTransactions();
                syncPendingSavingsGoals();
            }

            @Override
            public void onLost(Network network) {
                if (isAdded() && getContext() != null)
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Offline mode - changes will sync when online", Toast.LENGTH_SHORT).show());
            }
        };
        cm.registerNetworkCallback(request, networkCallback);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);




        currencySymbol = getUserCurrencySymbol();

        rvTransactions = view.findViewById(R.id.rv_transactions);
        tvTotalIncome = view.findViewById(R.id.tv_total_income);
        tvTotalExpense = view.findViewById(R.id.tv_total_expense);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        tvEmptyTransactions = view.findViewById(R.id.tv_empty_transactions);
        progressLoading = view.findViewById(R.id.progress_loading);
        recyclerViewSavingGoals = view.findViewById(R.id.rv_saving_goals);
        tvCurrentBalance = view.findViewById(R.id.tv_current_balance);
        tvViewAll = view.findViewById(R.id.tv_view_all);

        // Setup Saving Goals RecyclerView
        recyclerViewSavingGoals.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        List<GoalItem> goalItems = new ArrayList<>();
        savingGoalsAdapter = new SavingGoalsAdapter(goalItems, new SavingGoalsAdapter.OnGoalActionListener() {

            @Override
            public void onDelete(String goalId) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) return;

                firestore.collection("users")
                        .document(user.getUid())
                        .collection("savings")
                        .document(goalId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Goal deleted", Toast.LENGTH_SHORT).show();
                            loadSavingGoals(savingGoalsAdapter); // Refresh the list
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to delete goal", Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onUpdate(GoalItem goalItem, int newSavedAmount) {
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) return;

                Log.d("DEBUG", "Updating goal: ID=" + goalItem.getId());

                Map<String, Object> updateMap = new HashMap<>();
                updateMap.put("saved", newSavedAmount);

                firestore.collection("users")
                        .document(user.getUid())
                        .collection("savings")
                        .document(goalItem.getId())
                        .update(updateMap)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("DEBUG", "Goal updated successfully");
                            Toast.makeText(getContext(), "Goal updated", Toast.LENGTH_SHORT).show();
                            loadSavingGoals(savingGoalsAdapter);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("DEBUG", "Failed to update goal", e);
                            Toast.makeText(getContext(), "Failed to update goal", Toast.LENGTH_SHORT).show();
                        });
            }



        });
        recyclerViewSavingGoals.setAdapter(savingGoalsAdapter);

        // Load Saving Goals from Firestore
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            firestore.collection("users")
                    .document(user.getUid())
                    .collection("savings")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<GoalItem> fetchedGoals = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            Double target = doc.getDouble("target");
                            Double saved = doc.getDouble("saved");

                            if (name != null && target != null && saved != null) {
                                GoalItem goal = new GoalItem(id, name, saved, target);  // ✅ use full constructor
                                fetchedGoals.add(goal);
                            }
                        }
                        savingGoalsAdapter.setGoalList(fetchedGoals); // refresh adapter
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load saving goals", Toast.LENGTH_SHORT).show();
                        Log.e("HomeFragment", "Error loading savings", e);
                    });
        }

        // Setup Transactions RecyclerView
        rvTransactions.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionsAdapter = new TransactionsAdapter(transactionsList);
        rvTransactions.setAdapter(transactionsAdapter);

        // Load user transactions
        loadUserTransactions();

        swipeRefreshLayout.setOnRefreshListener(this::loadUserTransactions);

        // FAB to add transaction
        view.findViewById(R.id.fab_add_transaction).setOnClickListener(v -> showAddTransactionDialog());

        // Swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                transactionsAdapter.notifyItemChanged(position); // reset swipe

                new AlertDialog.Builder(getContext())
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to delete this transaction?")
                        .setPositiveButton("Yes", (dialog, which) -> deleteTransactionFromFirestore(position))
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && dX < 0) {
                    Paint paint = new Paint();
                    paint.setColor(Color.RED);

                    RectF background = new RectF(
                            viewHolder.itemView.getRight() + dX,
                            viewHolder.itemView.getTop(),
                            viewHolder.itemView.getRight(),
                            viewHolder.itemView.getBottom()
                    );
                    c.drawRect(background, paint);

                    Drawable deleteIcon = ContextCompat.getDrawable(recyclerView.getContext(), R.drawable.ic_delete);
                    if (deleteIcon != null) {
                        int iconSize = (int) (24 * recyclerView.getContext().getResources().getDisplayMetrics().density);
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
        String currency = prefs.getString("currency", "MYR"); // Default to USD if not set

        switch (currency) {
            case "MYR": return "RM";
            case "USD": return "$";
            case "EUR": return "€";
            default: return "RM"; // Default fallback
        }
    }
    private void syncPendingTransactions() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !isNetworkAvailable()) return;

        SharedPreferences deletedPrefs = requireActivity().getSharedPreferences("OfflineDeleted", Context.MODE_PRIVATE);
        Map<String, ?> allDeleted = deletedPrefs.getAll();

        for (String txnId : allDeleted.keySet()) {
            final String transactionId = txnId; // ✅ make a final copy for the lambda

            firestore.collection("users")
                    .document(user.getUid())
                    .collection("transactions")
                    .document(transactionId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("OfflineSync", "Deleted synced: " + transactionId);
                        deletedPrefs.edit().remove(transactionId).apply();
                    })
                    .addOnFailureListener(e -> {
                        Log.w("OfflineSync", "Failed to sync delete for " + transactionId, e);
                    });
        }

        loadUserTransactions(); // ✅ Still fine to refresh afterwards
    }


    private void loadSavingGoals(SavingGoalsAdapter adapter) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            firestore.collection("users")
                    .document(user.getUid())
                    .collection("savings")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<GoalItem> fetchedGoals = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String id = doc.getId();
                            String name = doc.getString("name");
                            Double target = doc.getDouble("target");
                            Double saved = doc.getDouble("saved");

                            if (name != null && target != null && saved != null) {
                                GoalItem goal = new GoalItem(id, name, saved, target);
                                fetchedGoals.add(goal);
                            }
                        }
                        adapter.setGoalList(fetchedGoals); // update adapter
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load saving goals", Toast.LENGTH_SHORT).show();
                        Log.e("HomeFragment", "Error loading savings", e);
                    });
        }
    }



    private void syncPendingSavingsGoals() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !isNetworkAvailable() || savingGoalsAdapter == null) return;
        loadSavingGoals(savingGoalsAdapter);
    }

    private void loadUserTransactions() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String userId = currentUser.getUid();

        // Show cached data first
        firestore.collection("users").document(userId).collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get(Source.CACHE)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    processTransactionData(queryDocumentSnapshots);

                    // Then try to get latest from server
                    if (isNetworkAvailable()) {
                        firestore.collection("users").document(userId).collection("transactions")
                                .orderBy("date", Query.Direction.DESCENDING)
                                .get(Source.SERVER)
                                .addOnSuccessListener(this::processTransactionData)
                                .addOnFailureListener(e -> Log.w("HomeFragment", "Server load failed, using cache", e));
                    }
                })
                .addOnFailureListener(e -> {
                    if (isNetworkAvailable()) {
                        // Try server if cache fails
                        firestore.collection("users").document(userId).collection("transactions")
                                .orderBy("date", Query.Direction.DESCENDING)
                                .get(Source.SERVER)
                                .addOnSuccessListener(this::processTransactionData)
                                .addOnFailureListener(e2 -> showErrorState());
                    } else {
                        showErrorState();
                    }
                });
    }

    private void showErrorState() {
        requireActivity().runOnUiThread(() -> {
            progressLoading.setVisibility(View.GONE);
            tvEmptyTransactions.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
        });
    }
    private void processTransactionData(QuerySnapshot queryDocumentSnapshots) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        Set<String> seenIds = new HashSet<>();
        transactionsList.clear();
        double totalIncome = 0;
        double totalExpense = 0;
        List<TransactionModel> allTransactions = new ArrayList<>();

        for (DocumentSnapshot document : queryDocumentSnapshots) {
            TransactionModel transaction = document.toObject(TransactionModel.class);
            if (transaction != null && !seenIds.contains(document.getId())) {
                transaction.setId(document.getId());
                seenIds.add(document.getId());

                allTransactions.add(transaction);
                if (transactionsList.size() < 3) {
                    transactionsList.add(transaction);
                }

                if ("Income".equals(transaction.getType())) {
                    totalIncome += transaction.getAmount();
                } else if ("Expense".equals(transaction.getType())) {
                    totalExpense += transaction.getAmount();
                }
            }
        }

        final double income = totalIncome;
        final double expense = totalExpense;
        final double balance = income - expense;
        final List<TransactionModel> allTx = new ArrayList<>(allTransactions); // Pass this to View All

        requireActivity().runOnUiThread(() -> {
            transactionsAdapter.notifyDataSetChanged();
            tvTotalIncome.setText(String.format("Income: %s%.2f", currencySymbol, income));
            tvTotalExpense.setText(String.format("Expense: %s%.2f", currencySymbol, expense));
            tvCurrentBalance.setText(String.format("%s%.2f", currencySymbol, balance));

            if (transactionsList.isEmpty()) {
                tvEmptyTransactions.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyTransactions.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
            }

            // 🟢 View All Transactions button logic (restored)
            tvViewAll.setOnClickListener(v -> {
                if (!isAdded() || getActivity() == null || getParentFragmentManager().isStateSaved()) {
                    Log.w("HomeFragment", "Fragment not in safe state for transaction.");
                    return;
                }

                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, TransactionsFragment.newInstance(new ArrayList<>(allTransactions)));
                transaction.addToBackStack(null);
                transaction.commit();
            });



            swipeRefreshLayout.setRefreshing(false);
            progressLoading.setVisibility(View.GONE);
        });
    }




    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) return false;
            android.net.NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } else {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
    }




    private void deleteTransactionFromFirestore(int position) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        TransactionModel txn = transactionsList.get(position);

        if (txn.getId() == null || txn.getId().isEmpty()) return;

        firestore.collection("users").document(userId).collection("transactions")
                .document(txn.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getActivity() == null) return;
                    transactionsList.remove(position);
                    transactionsAdapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Transaction deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getActivity() == null) return;
                    if (!isNetworkAvailable()) {
                        SharedPreferences prefs = requireActivity().getSharedPreferences("OfflineDeleted", Context.MODE_PRIVATE);
                        prefs.edit().putBoolean(txn.getId(), true).apply();

                        transactionsList.remove(position);
                        transactionsAdapter.notifyItemRemoved(position);
                        Toast.makeText(getContext(), "Marked for deletion offline", Toast.LENGTH_SHORT).show();
                    } else {
                        transactionsAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Delete failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }



    private void loadPaymentMethods(Spinner spinner) {
        String userId = auth.getCurrentUser().getUid();

        if (userId == null) {
            if (!isAdded() || getActivity() == null) return;
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users").document(userId).collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getActivity() == null) return;
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
            loadCategories(spinnerCategory, isIncome);
        });


        loadCategories(spinnerCategory, true);


        btnAddTransaction.setOnClickListener(v -> {
            String amountText = etAmount.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (spinnerCategory.getSelectedItem() == null || spinnerPaymentMethod.getSelectedItem() == null) {
                Toast.makeText(getContext(), "Please select category and payment method", Toast.LENGTH_SHORT).show();
                return;
            }

            String category = spinnerCategory.getSelectedItem().toString();
            String paymentMethod = spinnerPaymentMethod.getSelectedItem().toString();
            Timestamp timestamp = new Timestamp(new Date(selectedDate[0]));

            int selectedTypeId = rgType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(amountText) || TextUtils.isEmpty(description) ||
                    selectedTypeId == -1 || TextUtils.isEmpty(paymentMethod)) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountText);
            String type = ((RadioButton) dialogView.findViewById(selectedTypeId)).getText().toString();

            Map<String, Object> transactionData = new HashMap<>();
            transactionData.put("amount", amount);
            transactionData.put("description", description);
            transactionData.put("category", category);
            transactionData.put("paymentMethod", paymentMethod);
            transactionData.put("type", type);
            transactionData.put("date", timestamp);

            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) return;
            String userId = currentUser.getUid();

            firestore.collection("users").document(userId).collection("transactions")
                    .add(transactionData)
                    .addOnSuccessListener(documentReference -> {
                        transactionData.put("id", documentReference.getId());
                        Toast.makeText(getContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        if (type.equals("Expense")) {
                            updateCategorySpent(userId, category, amount);
                        }

                        TransactionModel model = new TransactionModel(
                                documentReference.getId(), amount, description, category,
                                paymentMethod, type, timestamp
                        );
                        transactionsList.add(0, model);
                        transactionsAdapter.notifyItemInserted(0);
                        rvTransactions.scrollToPosition(0);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to add transaction", Toast.LENGTH_SHORT).show();
                    });
        });


        dialog.show();
    }


    private void updateCategorySpent(String userId, String category, double amount) {
        firestore.collection("users")
                .document(userId)
                .collection("budget")
                .document("monthly")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded() || getActivity() == null) return;
                    if (documentSnapshot.exists()) {
                        Map<String, Object> categoriesMap = (Map<String, Object>) documentSnapshot.get("categories");
                        if (categoriesMap == null) categoriesMap = new HashMap<>();

                        Object categoryObj = categoriesMap.get(category);
                        Map<String, Object> categoryData;

                        if (categoryObj instanceof Map) {
                            categoryData = (Map<String, Object>) categoryObj;
                        } else {
                            categoryData = new HashMap<>();
                            categoryData.put("budget", categoryObj instanceof Number ? ((Number) categoryObj).intValue() : 0);
                            categoryData.put("spent", 0);
                        }

                        double currentSpent = categoryData.get("spent") instanceof Number
                                ? ((Number) categoryData.get("spent")).doubleValue() : 0;

                        categoryData.put("spent", currentSpent + amount);
                        categoriesMap.put(category, categoryData);

                        firestore.collection("users")
                                .document(userId)
                                .collection("budget")
                                .document("monthly")
                                .update("categories", categoriesMap)
                                .addOnSuccessListener(aVoid -> {
                                    if (!isAdded() || getActivity() == null) return;
                                    Log.d("HomeFragment", "Updated category spent: " + category);
                                })
                                .addOnFailureListener(e -> {
                                    if (!isAdded() || getActivity() == null) return;
                                    Log.e("HomeFragment", "Failed to update category spent", e);
                                });
                    }
                });
    }


    private void loadCategories(Spinner spinnerCategory, boolean isIncome) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            if (!isAdded() || getActivity() == null) return;
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        firestore.collection("users").document(userId)
                .collection("categories")
                .whereEqualTo("type", isIncome ? "Income" : "Expense")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> categories = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String name = document.getString("name");
                        if (name != null) {
                            categories.add(name);
                        }
                    }

                    if (categories.isEmpty()) {
                        categories.add("No categories found");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load categories", Toast.LENGTH_SHORT).show());
    }

}
