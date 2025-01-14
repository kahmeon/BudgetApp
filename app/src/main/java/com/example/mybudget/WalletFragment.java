package com.example.mybudget;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletFragment extends Fragment {

    private RecyclerView recyclerViewAccounts;
    private AccountsAdapter accountsAdapter;
    private List<AccountModel> accountsList = new ArrayList<>();
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    public WalletFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wallets, container, false);

        recyclerViewAccounts = view.findViewById(R.id.recycler_view_accounts);
        View ivAddAccount = view.findViewById(R.id.iv_add_account);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        // Set up RecyclerView
        recyclerViewAccounts.setLayoutManager(new LinearLayoutManager(getContext()));
        accountsAdapter = new AccountsAdapter(accountsList);
        recyclerViewAccounts.setAdapter(accountsAdapter);

        // Enable swipe-to-delete
        enableSwipeToDelete();
        // Load accounts from Firestore
        loadAccountsFromFirestore();

        // Add account button click listener
        ivAddAccount.setOnClickListener(v -> showAddAccountDialog());

        return view;
    }

    private void showAddAccountDialog() {
        // Inflate the custom dialog layout
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_account, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();

        EditText etAccountName = dialogView.findViewById(R.id.et_account_name);
        EditText etAccountBalance = dialogView.findViewById(R.id.et_account_balance);
        Button btnAdd = dialogView.findViewById(R.id.btn_add_account);

        btnAdd.setOnClickListener(v -> {
            String accountName = etAccountName.getText().toString().trim();
            String accountBalanceStr = etAccountBalance.getText().toString().trim();

            if (TextUtils.isEmpty(accountName) || TextUtils.isEmpty(accountBalanceStr)) {
                Toast.makeText(getContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double accountBalance = Double.parseDouble(accountBalanceStr);

            // Validate and save account to Firestore
            saveAccountToFirestore(accountName, accountBalance, dialog);
        });

        dialog.show();
    }


    private void saveAccountToFirestore(String accountName, double accountBalance, AlertDialog dialog) {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if account name is unique
        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .whereEqualTo("accountName", accountName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Account name is not unique
                        Toast.makeText(getContext(), "Account name already exists! Please choose a different name.", Toast.LENGTH_SHORT).show();
                    } else {
                        // Account name is unique, proceed to add
                        addAccountToFirestore(accountName, accountBalance, dialog);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking account name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Helper method to add a unique account to Firestore and update the UI
    private void addAccountToFirestore(String accountName, double accountBalance, AlertDialog dialog) {
        String userId = auth.getCurrentUser().getUid();

        // Create account data
        AccountModel account = new AccountModel(accountName, accountBalance);

        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .add(account)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Account added successfully", Toast.LENGTH_SHORT).show();

                    // Add the new account to the list
                    accountsList.add(account);
                    accountsAdapter.notifyDataSetChanged();
                    calculateNetAssets(); // Recalculate Net Assets

                    dialog.dismiss(); // Dismiss the dialog only after successful addition
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    // Helper method to add a unique account to Firestore
    private void addAccountToFirestore(String accountName, double accountBalance) {
        String userId = auth.getCurrentUser().getUid();

        // Create account data
        AccountModel account = new AccountModel(accountName, accountBalance);

        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .add(account)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Account added successfully", Toast.LENGTH_SHORT).show();
                    accountsList.add(account);
                    accountsAdapter.notifyDataSetChanged();
                    calculateNetAssets(); // Recalculate Net Assets
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    private void loadAccountsFromFirestore() {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    accountsList.clear();

                    // Fetch transactions for each account
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        AccountModel account = document.toObject(AccountModel.class);
                        if (account != null) {
                            fetchTotalAmountForAccount(account); // Fetch transactions for this account
                        }
                    }
                    // Check if the default account exists
                    checkAndAddDefaultAccount();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load accounts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAndAddDefaultAccount() {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query for the default account
        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .whereEqualTo("accountName", "Default Account") // Query by account name
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // If the default account doesn't exist, add it
                        addDefaultAccount();
                    } else {
                        // Notify adapter if default account already exists
                        accountsAdapter.notifyDataSetChanged();
                        calculateNetAssets();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking default account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void addDefaultAccount() {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Define default account details
        String defaultAccountName = "Default Account";
        double defaultAccountBalance = 0.0;

        // Create a new AccountModel object with a unique field
        Map<String, Object> defaultAccount = new HashMap<>();
        defaultAccount.put("accountName", defaultAccountName);
        defaultAccount.put("accountBalance", defaultAccountBalance);
        defaultAccount.put("isDefault", true); // Unique identifier for the default account

        // Add the default account to Firestore
        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .add(defaultAccount)
                .addOnSuccessListener(documentReference -> {
                    // Add default account locally and update UI
                    accountsList.add(new AccountModel(defaultAccountName, defaultAccountBalance));
                    accountsAdapter.notifyDataSetChanged();
                    calculateNetAssets();

                    Toast.makeText(getContext(), "Default account added successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add default account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private void fetchTotalAmountForAccount(AccountModel account) {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("paymentMethod", account.getAccountName())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<TransactionModel> transactions = new ArrayList<>();

                    // Loop through each transaction
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        TransactionModel transaction = document.toObject(TransactionModel.class);
                        if (transaction != null) {
                            transactions.add(transaction); // Add transaction to the list
                        }
                    }

                    // Update account with transactions and recalculate balance
                    account.setTransactions(transactions);

                    // Add account to the list and update UI
                    accountsList.add(account);
                    accountsAdapter.notifyDataSetChanged();

                    // Recalculate assets, debt, and net assets
                    calculateNetAssets();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to fetch transactions for " + account.getAccountName(), Toast.LENGTH_SHORT).show();
                });
    }



    private void calculateNetAssets() {
        double totalAssets = 0; // Total "Income"
        double totalDebt = 0;   // Total "Expense"
        double netAssets = 0;   // Net Assets = Income - Expense

        for (AccountModel account : accountsList) {
            for (TransactionModel transaction : account.getTransactions()) {
                if ("Income".equalsIgnoreCase(transaction.getType())) {
                    totalAssets += transaction.getAmount();
                } else if ("Expense".equalsIgnoreCase(transaction.getType())) {
                    totalDebt += transaction.getAmount();
                }
            }

            // Net assets is the sum of all account balances
            netAssets += account.getAccountBalance();
        }

        // Update UI
        TextView tvNetAssets = getView().findViewById(R.id.tv_net_assets);
        TextView tvTotalAssets = getView().findViewById(R.id.tv_total_assets);
        TextView tvTotalDebt = getView().findViewById(R.id.tv_total_debt);

        if (tvNetAssets != null) {
            tvNetAssets.setText(String.format("Net Assets: RM %.2f", netAssets));
        }
        if (tvTotalAssets != null) {
            tvTotalAssets.setText(String.format("Assets: RM %.2f", totalAssets));
        }
        if (tvTotalDebt != null) {
            tvTotalDebt.setText(String.format("Debt: RM %.2f", totalDebt));
        }
    }



    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                // No move action needed
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Get position of the item
                int position = viewHolder.getAdapterPosition();
                AccountModel account = accountsList.get(position);

                // Show confirmation dialog
                showDeleteConfirmationDialog(account, position);
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;

                // Draw red background
                Paint paint = new Paint();
                paint.setColor(Color.RED);
                RectF background = new RectF((float) itemView.getRight() + dX,
                        (float) itemView.getTop(),
                        (float) itemView.getRight(),
                        (float) itemView.getBottom());
                c.drawRect(background, paint);

                // Draw delete icon
                Drawable deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_delete);
                if (deleteIcon != null) {
                    int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int iconTop = itemView.getTop() + iconMargin;
                    int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                    int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - iconMargin;

                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                    deleteIcon.draw(c);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }




        };

        // Attach ItemTouchHelper to RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewAccounts);
    }

    private void deleteAccountFromFirestore(AccountModel account, int position) {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        // Query Firestore for the specific account to delete
        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .whereEqualTo("accountName", account.getAccountName())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Delete the document
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(aVoid -> {
                                    // Remove from local list and update adapter
                                    accountsList.remove(position);
                                    accountsAdapter.notifyItemRemoved(position);
                                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                                    calculateNetAssets(); // Recalculate net assets after deletion
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Failed to delete account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    accountsAdapter.notifyItemChanged(position); // Reset swipe if delete fails
                                });
                    } else {
                        Toast.makeText(getContext(), "Account not found in Firestore", Toast.LENGTH_SHORT).show();
                        accountsAdapter.notifyItemChanged(position); // Reset swipe if account not found
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error finding account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    accountsAdapter.notifyItemChanged(position); // Reset swipe if query fails
                });
    }

    private void showDeleteConfirmationDialog(AccountModel account, int position) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete the account \"" + account.getAccountName() + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Proceed with deletion if user confirms
                    deleteAccountFromFirestore(account, position);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Cancel deletion and reset swipe
                    accountsAdapter.notifyItemChanged(position);
                })
                .setOnCancelListener(dialog -> {
                    // Reset swipe if dialog is dismissed
                    accountsAdapter.notifyItemChanged(position);
                })
                .show();
    }



}



