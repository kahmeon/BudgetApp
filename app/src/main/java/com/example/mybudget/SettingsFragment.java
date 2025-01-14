package com.example.mybudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Open Calculator
        LinearLayout openCalculatorButton = view.findViewById(R.id.openCalculatorButton);
        openCalculatorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CalculatorActivity when the button is clicked
                Intent intent = new Intent(getActivity(), Calculator.class);
                startActivity(intent);
            }
        });

        // Logout Button
        CardView logoutButton = view.findViewById(R.id.logout);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        return view;
    }
    private void logout() {
        // Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // Update SharedPreferences to indicate the user is logged out
        SharedPreferences prefs = requireActivity().getSharedPreferences("loginPrefs", requireActivity().MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        // Redirect to LoginActivity with flags to clear back stack
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        // Close MainActivity to prevent back navigation
        requireActivity().finishAffinity();
    }


}
