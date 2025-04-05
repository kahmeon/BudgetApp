package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavingGoalAdapter extends RecyclerView.Adapter<SavingGoalAdapter.GoalViewHolder> {

    private final List<GoalItem> goals;
    private final OnGoalActionListener listener;

    public interface OnGoalActionListener {
        void onDelete(String goalId);
        void onUpdate(GoalItem goalItem, int newSavedAmount);
    }

    public SavingGoalAdapter(List<GoalItem> goals, OnGoalActionListener listener) {
        this.goals = goals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.goal_item, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        GoalItem goal = goals.get(position);
        holder.goalName.setText(goal.getName());
        holder.goalAmount.setText("Target: RM " + goal.getTargetAmount());
        holder.savedAmount.setText("Saved: RM " + goal.getSavedAmount());

        int percent = (int) ((goal.getSavedAmount() * 100.0f) / goal.getTargetAmount());
        holder.goalProgress.setProgress(percent);
        holder.goalProgressText.setText(percent + "%");

        holder.updateButton.setOnClickListener(v -> listener.onUpdate(goal, goal.getSavedAmount() + 100));
        holder.deleteButton.setOnClickListener(v -> listener.onDelete(goal.getId()));
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView goalName, goalAmount, savedAmount, goalProgressText;
        ProgressBar goalProgress;
        Button updateButton, deleteButton;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            goalName = itemView.findViewById(R.id.goal_title);
            goalAmount = itemView.findViewById(R.id.goal_target);
            savedAmount = itemView.findViewById(R.id.goal_saved);
            goalProgress = itemView.findViewById(R.id.goal_progress_bar);
            goalProgressText = itemView.findViewById(R.id.goal_progress_text);
            updateButton = itemView.findViewById(R.id.btn_update_goal);
            deleteButton = itemView.findViewById(R.id.btn_delete_goal);
        }
    }
}
