package com.example.mybudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private BottomNavigationView bottomNavigationView;
    private Fragment currentFragment = null;
    private long lastClickTime = 0; // ✅ Debounce tracking

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

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setItemIconTintList(null);

        if (savedInstanceState == null && getIntent() != null && !getIntent().hasExtra("notification_type")) {
            loadFragment(new HomeFragment()); // Default only if no intent
        }

        handleNotificationIntent(getIntent());
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            // ✅ Debounce logic: Ignore if tapped too quickly
            if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
                return false;
            }
            lastClickTime = SystemClock.elapsedRealtime();

            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard && !(currentFragment instanceof HomeFragment)) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_wallets && !(currentFragment instanceof WalletFragment)) {
                selectedFragment = new WalletFragment();
            } else if (itemId == R.id.nav_reports && !(currentFragment instanceof ReportsFragment)) {
                selectedFragment = new ReportsFragment();
            } else if (itemId == R.id.nav_settings && !(currentFragment instanceof SettingsFragment)) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNotificationIntent(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNotificationIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !isUserLoggedIn()) {
            redirectToLogin();
        }
    }

    private void loadFragment(@NonNull Fragment fragment) {
        currentFragment = fragment;
        new Handler().post(() -> {
            try {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commitAllowingStateLoss(); // safer
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
    private void handleNotificationIntent(Intent intent) {
        if (intent != null && intent.hasExtra("notification_type")) {
            String type = intent.getStringExtra("notification_type");
            getIntent().removeExtra("notification_type");

            if ("view_transactions".equals(type)) {
                loadFragment(TransactionsFragment.newInstance(null));
            } else if ("view_bill".equals(type)) {
                startActivity(new Intent(this, NotificationSettingsActivity.class));
            }
        }
    }


}
