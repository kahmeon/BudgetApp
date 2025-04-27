package com.example.mybudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null || !isUserLoggedIn()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemIconTintList(null); // Ensure icons retain original colors

        // Load the default fragment (HomeFragment)
        loadFragment(new HomeFragment());

        // Set listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_dashboard) {
                selectedFragment = new HomeFragment();

            } else if (item.getItemId() == R.id.nav_wallets) {
                selectedFragment = new WalletFragment();
            } else if (item.getItemId() == R.id.nav_reports) {
                selectedFragment = new ReportsFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
            }

            return true;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Double-check login status when activity starts
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !isUserLoggedIn()) {
            redirectToLogin();
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
