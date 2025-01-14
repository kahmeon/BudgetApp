package com.example.mybudget;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionDetailsDialog extends DialogFragment {

    private TextView tvDetailCategory, tvDetailType, tvDetailAmount, tvDetailDate, tvDetailDescription, tvDetailPaymentMethod, tvDetailLocation;
    private ImageView ivUserImage, ivEditTransaction, ivDeleteTransaction;

    private String transactionId, category, type, description, paymentMethod, location;
    private long date;
    private double amount;
    private boolean hasImage;
    private Object image; // Replace Object with your image data type (e.g., Bitmap or URI).
    private FirebaseAuth auth;

    public static TransactionDetailsDialog newInstance(TransactionModel transaction) {
        TransactionDetailsDialog dialog = new TransactionDetailsDialog();
        Bundle args = new Bundle();
        args.putString("TRANSACTION_ID", transaction.getId());
        args.putString("CATEGORY", transaction.getCategory());
        args.putString("TYPE", transaction.getType());
        args.putString("DESCRIPTION", transaction.getDescription());
        args.putString("PAYMENT_METHOD", transaction.getPaymentMethod());
        args.putString("LOCATION", transaction.getLocation());
        args.putLong("DATE", transaction.getDate());
        args.putDouble("AMOUNT", transaction.getAmount());
        args.putBoolean("HAS_IMAGE", transaction.hasImage());
        if (transaction.hasImage()) {
            args.putString("IMAGE_URL", transaction.getImageUrl());
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_transaction_details, container, false);

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance();

        // Initialize UI components
        tvDetailCategory = view.findViewById(R.id.tv_detail_category);
        tvDetailType = view.findViewById(R.id.tv_detail_type);
        tvDetailAmount = view.findViewById(R.id.tv_detail_amount);
        tvDetailDate = view.findViewById(R.id.tv_detail_date);
        tvDetailDescription = view.findViewById(R.id.tv_detail_description);
        tvDetailPaymentMethod = view.findViewById(R.id.tv_detail_payment_method);
        tvDetailLocation = view.findViewById(R.id.tv_detail_location);
        ivUserImage = view.findViewById(R.id.iv_user_image);
        ivEditTransaction = view.findViewById(R.id.iv_edit_transaction);
        ivDeleteTransaction = view.findViewById(R.id.iv_delete_transaction);

        // Retrieve data from arguments
        if (getArguments() != null) {
            transactionId = getArguments().getString("TRANSACTION_ID");
            category = getArguments().getString("CATEGORY");
            type = getArguments().getString("TYPE");
            description = getArguments().getString("DESCRIPTION");
            paymentMethod = getArguments().getString("PAYMENT_METHOD");
            location = getArguments().getString("LOCATION");
            date = getArguments().getLong("DATE");
            amount = getArguments().getDouble("AMOUNT");
            hasImage = getArguments().getBoolean("HAS_IMAGE");
            image = getArguments().getParcelable("IMAGE");
        }

        // Set data to UI components
        tvDetailCategory.setText("Category: " + category);
        tvDetailType.setText("Type: " + type);
        tvDetailAmount.setText("Amount: RM" + amount);
        tvDetailDate.setText("Date: " + formatDate(date));
        tvDetailDescription.setText("Description: " + description);
        tvDetailPaymentMethod.setText("Payment Method: " + paymentMethod);

        if ("Expense".equalsIgnoreCase(type)) {
            tvDetailAmount.setText("- RM" + String.format(Locale.getDefault(), "%.2f", amount));
            tvDetailAmount.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        } else if ("Income".equalsIgnoreCase(type)) {
            tvDetailAmount.setText("+ RM" + String.format(Locale.getDefault(), "%.2f", amount));
            tvDetailAmount.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        }

        if (!TextUtils.isEmpty(location)) {
            tvDetailLocation.setVisibility(View.VISIBLE);
            tvDetailLocation.setText("Location: " + location);
        } else {
            tvDetailLocation.setVisibility(View.GONE);
        }

        if (hasImage && image != null) {
            ivUserImage.setVisibility(View.VISIBLE);
        } else {
            ivUserImage.setVisibility(View.GONE);
        }

        ivEditTransaction.setOnClickListener(v -> showEditDialog());
        ivDeleteTransaction.setOnClickListener(v -> showDeleteConfirmation());

        return view;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void showEditDialog() {
        EditTransactionDialog editDialog = EditTransactionDialog.newInstance(
                transactionId, category, type, description, amount, date, paymentMethod, location
        );
        editDialog.show(requireActivity().getSupportFragmentManager(), "EditTransactionDialog");
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?")
                .setPositiveButton("Yes", (dialog, which) -> deleteTransactionFromFirebase())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteTransactionFromFirebase() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("transactions")
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete transaction", Toast.LENGTH_SHORT).show();
                });
    }
}
