package com.example.mybudget;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class EditTransactionDialog extends DialogFragment {

    private EditText etAmount, etDescription;
    private TextView tvSelectedDate;
    private RadioGroup rgType;
    private Spinner spinnerCategory, spinnerPaymentMethods;
    private Button btnUpdateTransaction;

    private String transactionId, category, type, description, paymentMethod;
    private long date;
    private double amount;

    public static EditTransactionDialog newInstance(String transactionId, String category, String type,
                                                    String description, double amount, long date,
                                                    String paymentMethod, String location) {
        EditTransactionDialog dialog = new EditTransactionDialog();
        Bundle args = new Bundle();
        args.putString("TRANSACTION_ID", transactionId);
        args.putString("CATEGORY", category);
        args.putString("TYPE", type);
        args.putString("DESCRIPTION", description);
        args.putDouble("AMOUNT", amount);
        args.putLong("DATE", date);
        args.putString("PAYMENT_METHOD", paymentMethod);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_edit_transaction, container, false);

        // Retrieve arguments
        if (getArguments() != null) {
            transactionId = getArguments().getString("TRANSACTION_ID");
            category = getArguments().getString("CATEGORY");
            type = getArguments().getString("TYPE");
            description = getArguments().getString("DESCRIPTION");
            amount = getArguments().getDouble("AMOUNT");
            date = getArguments().getLong("DATE");
            paymentMethod = getArguments().getString("PAYMENT_METHOD");
        }

        // Initialize UI components
        etAmount = view.findViewById(R.id.et_amount);
        etDescription = view.findViewById(R.id.et_description);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        rgType = view.findViewById(R.id.rg_type);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerPaymentMethods = view.findViewById(R.id.spinner_payment_methods);
        btnUpdateTransaction = view.findViewById(R.id.btn_update_transaction);

        // Pre-fill data
        etAmount.setText(String.valueOf(amount));
        etDescription.setText(description);
        tvSelectedDate.setText(formatDate(date));
        rgType.check("Income".equals(type) ? R.id.rb_income : R.id.rb_expense);
        loadPaymentMethods(spinnerPaymentMethods);
        setCategoryOptions(spinnerCategory, "Income".equals(type));
        spinnerCategory.setSelection(getSpinnerIndex(spinnerCategory, category));
        spinnerPaymentMethods.setSelection(getSpinnerIndex(spinnerPaymentMethods, paymentMethod));

        // Set up the date picker
        tvSelectedDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(date);

            new DatePickerDialog(getContext(), (view1, year, month, day) -> {
                calendar.set(year, month, day);
                date = calendar.getTimeInMillis();
                tvSelectedDate.setText(formatDate(date));
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnUpdateTransaction.setOnClickListener(v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(getContext(), "User not signed in.", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = currentUser.getUid();

            if (TextUtils.isEmpty(transactionId)) {
                Toast.makeText(getContext(), "Invalid transaction ID.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Retrieve updated values
            String updatedAmountText = etAmount.getText().toString().trim();
            String updatedDescription = etDescription.getText().toString().trim();
            String updatedCategory = spinnerCategory.getSelectedItem().toString();
            String updatedPaymentMethod = spinnerPaymentMethods.getSelectedItem().toString();
            int selectedTypeId = rgType.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(updatedAmountText) || TextUtils.isEmpty(updatedDescription) ||
                    selectedTypeId == -1 || TextUtils.isEmpty(updatedCategory) || TextUtils.isEmpty(updatedPaymentMethod)) {
                Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            double updatedAmount = Double.parseDouble(updatedAmountText);
            String updatedType = selectedTypeId == R.id.rb_income ? "Income" : "Expense";

            // Create updated transaction map
            Map<String, Object> updatedTransaction = new HashMap<>();
            updatedTransaction.put("amount", updatedAmount);
            updatedTransaction.put("description", updatedDescription);
            updatedTransaction.put("type", updatedType);
            updatedTransaction.put("category", updatedCategory);
            updatedTransaction.put("paymentMethod", updatedPaymentMethod);
            Timestamp timestamp = new Timestamp(new Date(date));
            updatedTransaction.put("date", timestamp);

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            firestore.collection("users")
                    .document(userId)
                    .collection("transactions")
                    .document(transactionId)  // ✅ Update specific transaction
                    .update(updatedTransaction)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Update Debug", "Transaction updated successfully.");
                        Toast.makeText(getContext(), "Transaction updated successfully.", Toast.LENGTH_SHORT).show();
                        dismiss(); // Close the dialog after updating
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Update Debug", "Failed to update transaction.", e);
                        Toast.makeText(getContext(), "Failed to update transaction.", Toast.LENGTH_SHORT).show();
                    });

        });


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                // Set custom width for the dialog
                WindowManager.LayoutParams params = window.getAttributes();
                params.width = WindowManager.LayoutParams.MATCH_PARENT; // Full-width dialog
                params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Height wraps content
                window.setAttributes(params);
            }
        }
    }

    private String formatDate(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).equals(value)) return i;
        }
        return 0;
    }

    private void loadPaymentMethods(Spinner spinner) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .collection("accounts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> paymentMethods = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        String accountName = document.getString("accountName");
                        if (accountName != null) {
                            paymentMethods.add(accountName);
                        }
                    }

                    // Set the payment methods to the spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                            android.R.layout.simple_spinner_item, paymentMethods);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner.setAdapter(adapter);

                    // Pre-select the current payment method
                    if (paymentMethod != null) {
                        int index = paymentMethods.indexOf(paymentMethod);
                        if (index >= 0) {
                            spinner.setSelection(index);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to load payment methods", Toast.LENGTH_SHORT).show();
                });

        firestore.collection("users")
                .document(userId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot document : querySnapshot) {
                        String transactionId = document.getId(); // Retrieve the document ID
                        String category = document.getString("category");
                        String type = document.getString("type");
                        double amount = document.getDouble("amount");
                        Timestamp timestamp = document.getTimestamp("date");
                        long date = (timestamp != null) ? timestamp.toDate().getTime() : 0;

                        String paymentMethod = document.getString("paymentMethod");
                        String description = document.getString("description");

                        Log.d("Transaction Debug", "Transaction ID: " + transactionId);

                        // Use transactionId here or pass it to the dialog
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore Error", "Failed to fetch transactions", e));
    }


    private void setCategoryOptions(Spinner spinner, boolean isIncome) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item,
                isIncome ? Arrays.asList("Salary", "Business", "Investment") : Arrays.asList("Food", "Transport", "Utilities"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }


}
