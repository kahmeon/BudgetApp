package com.example.mybudget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Calculator extends AppCompatActivity {

    private static final String TAG = "CalculatorActivity";
    private TextView birthdateText;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up click listeners for loan options
        findViewById(R.id.personalloan).setOnClickListener(v -> {
            Log.d(TAG, "Personal Loan button clicked");
            openPersonalLoan();
        });

        findViewById(R.id.housing_loan).setOnClickListener(v -> {
            Log.d(TAG, "Housing Loan button clicked");
            openHousingLoan();
        });

        // Set up the username text
        TextView usernameText = findViewById(R.id.name_text);
        String username = getUsernameFromSharedPreferences();
        usernameText.setText(username);

        // Initialize calendar and birthdate TextView
        calendar = Calendar.getInstance();
        birthdateText = findViewById(R.id.birthdate_text);

        // Display saved birthdate and set up click listener
        displaySavedBirthdate();
        birthdateText.setOnClickListener(v -> showDatePickerDialog());
    }

    private void displaySavedBirthdate() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String birthdate = sharedPreferences.getString("birthdate", "Set Birthdate");
        if (birthdateText != null) {
            birthdateText.setText(birthdate);
        }
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateBirthdate();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateBirthdate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String birthdate = dateFormat.format(calendar.getTime());

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("birthdate", birthdate);
        editor.apply();

        if (birthdateText != null) {
            birthdateText.setText(birthdate);
        }
    }

    private String getUsernameFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return sharedPreferences.getString("username", "Guest");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_edit_info) {
            Intent intent = new Intent(Calculator.this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onEditUserInfoClick(View view) {
        Log.d(TAG, "Edit User Info button clicked");
        Intent intent = new Intent(Calculator.this, MainActivity.class);
        startActivity(intent);
    }

    private void openPersonalLoan() {
        Log.d(TAG, "openPersonalLoan method called");
        try {
            Intent intent = new Intent(Calculator.this, personalloan.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Personal Loan activity", e);
        }
    }

    private void openHousingLoan() {
        Log.d(TAG, "openHousingLoan method called");
        try {
            Intent intent = new Intent(Calculator.this, housingloan.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Housing Loan activity", e);
        }
    }
}
