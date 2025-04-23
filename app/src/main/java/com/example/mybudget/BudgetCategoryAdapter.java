package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.CategoryViewHolder> {

    private List<BudgetCategory> categories;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(BudgetCategory category);
    }

    public BudgetCategoryAdapter(List<BudgetCategory> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        BudgetCategory category = categories.get(position);
        holder.bind(category);
        holder.itemView.setOnClickListener(v -> listener.onCategoryClick(category));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<BudgetCategory> newCategories) {
        categories.clear();
        categories.addAll(newCategories);
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryName;
        private TextView categoryAmount;
        private TextView categorySpent;
        private TextView categoryRemaining;
        private ProgressBar categoryProgress;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);
            categoryAmount = itemView.findViewById(R.id.category_amount);
            categorySpent = itemView.findViewById(R.id.category_spent);
            categoryRemaining = itemView.findViewById(R.id.category_remaining);
            categoryProgress = itemView.findViewById(R.id.category_progress);
        }

        public void bind(BudgetCategory category) {
            categoryName.setText(category.getName());
            categoryAmount.setText(String.format("Budget: RM%d", category.getAmount()));
            categorySpent.setText(String.format("Spent: RM%d", category.getSpent()));
            categoryRemaining.setText(String.format("Remaining: RM%d", category.getRemaining()));
            categoryProgress.setProgress(category.getProgressPercentage());
        }
    }
}