package com.example.mybudget;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {

    private List<TransactionModel> transactionList;
    private int swipedPosition = -1;


    public TransactionsAdapter(List<TransactionModel> transactionList) {
        this.transactionList = transactionList;
    }

    public void updateList(List<TransactionModel> newList) {
        transactionList.clear();
        transactionList.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        TransactionModel transaction = transactionList.get(position);

        // Set category, type, and description
        holder.tvCategory.setText(transaction.getCategory());
        holder.tvType.setText(transaction.getType());
        holder.tvDescription.setText(transaction.getDescription());

        // Format and display the date
        holder.tvDate.setText(formatDate(transaction.getDate()));
        holder.tvBank.setText(transaction.getPaymentMethod());

        // Set amount and color based on type
        String type = transaction.getType();
        if ("Income".equalsIgnoreCase(type)) {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "+RM%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#388E3C")); // Green for income
        } else if ("Expense".equalsIgnoreCase(type)) {
            holder.tvAmount.setText(String.format(Locale.getDefault(), "-RM%.2f", transaction.getAmount()));
            holder.tvAmount.setTextColor(Color.parseColor("#D32F2F")); // Red for expense
        }


        // Handle click on the transaction item
        holder.itemView.setOnClickListener(v -> {
            TransactionDetailsDialog dialog = TransactionDetailsDialog.newInstance(transaction);
            dialog.show(((AppCompatActivity) v.getContext()).getSupportFragmentManager(), "TransactionDetailsDialog");
        });




        // Check if this item is the currently swiped one
        if (position == swipedPosition) {
            holder.itemView.findViewById(R.id.delete_icon).setVisibility(View.VISIBLE);
            holder.itemView.findViewById(R.id.delete_icon).setOnClickListener(v -> {
                // Show confirmation dialog
                new AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Delete Transaction")
                        .setMessage("Are you sure you want to delete this transaction?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            // Remove item from the list and update the RecyclerView
                            transactionList.remove(position);
                            notifyItemRemoved(position);
                            swipedPosition = -1; // Reset swiped position
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            dialog.dismiss();
                            notifyItemChanged(position); // Reset the swipe
                        })
                        .show();
            });
        } else {
            holder.itemView.findViewById(R.id.delete_icon).setVisibility(View.GONE);
        }

    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public static class TransactionViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvCategory, tvType, tvDescription, tvAmount, tvBank;

        public TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvCategory = itemView.findViewById(R.id.tv_transaction_category);
            tvType = itemView.findViewById(R.id.tv_transaction_type);
            tvDescription = itemView.findViewById(R.id.tv_transaction_description);
            tvAmount = itemView.findViewById(R.id.tv_transaction_amount);
            tvBank = itemView.findViewById(R.id.tv_transaction_bank);
        }
    }

    // Helper method to format date
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }



}
