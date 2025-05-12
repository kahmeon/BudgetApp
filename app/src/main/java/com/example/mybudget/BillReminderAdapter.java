package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BillReminderAdapter extends RecyclerView.Adapter<BillReminderAdapter.ViewHolder> {

    private final List<BillReminder> billList;
    private OnBillDeleteClickListener deleteClickListener;
    private OnBillEditClickListener editClickListener;

    // Constructor
    public BillReminderAdapter(List<BillReminder> bills) {
        this.billList = bills;
    }

    // Interface for delete click callback
    public interface OnBillDeleteClickListener {
        void onDelete(BillReminder bill, int position);
    }

    public interface OnBillEditClickListener {
        void onEdit(BillReminder bill, int position);
    }

    public void setOnBillDeleteClickListener(OnBillDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setOnBillEditClickListener(OnBillEditClickListener listener) {
        this.editClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bill, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        BillReminder bill = billList.get(position);

        holder.nameView.setText(bill.getName());

        // ✅ Format and set the amount
        String formattedAmount = NumberFormat.getCurrencyInstance(new Locale("en", "MY")).format(bill.getAmount());
        holder.amountView.setText(formattedAmount);

        // Format and set the due date
        String dateFormatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(new Date(bill.getDueDateMillis()));
        holder.dateView.setText(dateFormatted);

        // Format and set the scheduled time
        String timeFormatted = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(bill.getDueDateMillis()));
        holder.scheduledTimeView.setText("Scheduled: " + timeFormatted);

        // Delete click
        holder.deleteBtn.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (deleteClickListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                deleteClickListener.onDelete(billList.get(adapterPosition), adapterPosition);
            }
        });

        // Edit click
        holder.editBtn.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (editClickListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                editClickListener.onEdit(billList.get(adapterPosition), adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }

    // Inner ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, dateView, amountView, scheduledTimeView;
        ImageButton deleteBtn, editBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tv_bill_name);
            dateView = itemView.findViewById(R.id.tv_bill_date);
            amountView = itemView.findViewById(R.id.tv_bill_amount);
            scheduledTimeView = itemView.findViewById(R.id.tv_scheduled_time);
            deleteBtn = itemView.findViewById(R.id.btn_delete_bill);
            editBtn = itemView.findViewById(R.id.btn_edit_bill);
        }
    }
}
