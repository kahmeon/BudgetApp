package com.example.mybudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Buttons for navigation
        LinearLayout openCalculatorButton = view.findViewById(R.id.openCalculatorButton);
        openCalculatorButton.setOnClickListener(v -> openCalculator());

        LinearLayout openBudgetButton = view.findViewById(R.id.openBudgetButton);
        openBudgetButton.setOnClickListener(v -> openBudget());

        LinearLayout openFeedbackButton = view.findViewById(R.id.openFeedbackButton);
        openFeedbackButton.setOnClickListener(v -> openFeedback());

        LinearLayout openAboutButton = view.findViewById(R.id.openAboutButton);
        openAboutButton.setOnClickListener(v -> openAbout());

        LinearLayout openSavingGoalsButton = view.findViewById(R.id.openSavingGoalsButton);
        openSavingGoalsButton.setOnClickListener(v -> openSavingGoals());

        CardView openCategoryButton = view.findViewById(R.id.openCategoryButton);
        openCategoryButton.setOnClickListener(v -> openCategoryPage());

        CardView openNotificationButton = view.findViewById(R.id.openNotificationButton);

        openNotificationButton.setOnClickListener(v -> openNotificationPage());

        // Logout Button
        CardView logoutButton = view.findViewById(R.id.logout);
        logoutButton.setOnClickListener(v -> logout());

        return view;
    }

    private void openCalculator() {
        Intent intent = new Intent(getActivity(), Calculator.class);
        startActivity(intent);
    }

    private void openBudget() {
        Intent intent = new Intent(getActivity(), BudgetActivity.class);
        startActivity(intent);
    }

    private void openSavingGoals() {
        Intent intent = new Intent(getActivity(), SavingGoalsActivity.class);
        startActivity(intent);
    }

    private void openFeedback() {
        Intent intent = new Intent(getActivity(), FeedbackActivity.class);
        startActivity(intent);
    }

    private void openAbout() {
        Intent intent = new Intent(getActivity(), AboutActivity.class);
        startActivity(intent);
    }

    private void openCategoryPage() {
        Intent intent = new Intent(getActivity(), AddCategoryActivity.class);
        startActivity(intent);
    }

    private void openNotificationPage() {
        Intent intent = new Intent(getActivity(), NotificationSettingsActivity.class);
        startActivity(intent);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut(); // Firebase sign out

        SharedPreferences prefs = requireActivity().getSharedPreferences("loginPrefs", requireActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        requireActivity().finishAffinity();

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}
