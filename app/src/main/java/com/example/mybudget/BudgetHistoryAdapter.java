package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetHistoryAdapter extends RecyclerView.Adapter<BudgetHistoryAdapter.ViewHolder> {

    private final List<BudgetHistoryItem> budgetList;

    public BudgetHistoryAdapter(List<BudgetHistoryItem> budgetList) {
        this.budgetList = budgetList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.budget_history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BudgetHistoryItem item = budgetList.get(position);
        holder.monthText.setText(item.getMonth());
        holder.budgetText.setText("RM " + item.getBudget());
        holder.spentText.setText("RM " + item.getSpent());
        holder.percentText.setText(item.getPercent() + "%");
    }

    @Override
    public int getItemCount() {
        return budgetList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView monthText, budgetText, spentText, percentText;

        ViewHolder(View view) {
            super(view);
            monthText = view.findViewById(R.id.month_text);
            budgetText = view.findViewById(R.id.budget_text);
            spentText = view.findViewById(R.id.spent_text);
            percentText = view.findViewById(R.id.percent_text);
        }
    }
}
