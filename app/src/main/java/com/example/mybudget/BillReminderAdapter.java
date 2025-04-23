package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BillReminderAdapter extends RecyclerView.Adapter<BillReminderAdapter.ViewHolder> {
    private List<BillReminder> billList;

    public BillReminderAdapter(List<BillReminder> bills) {
        this.billList = bills;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameView, dateView;
        public ViewHolder(View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.tv_bill_name);
            dateView = itemView.findViewById(R.id.tv_bill_date);

        }
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
        holder.dateView.setText(new SimpleDateFormat("dd MMM yyyy").format(new Date(bill.getDueDateMillis())));
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }
}
