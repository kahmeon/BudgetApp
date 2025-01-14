package com.example.mybudget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class housingloan extends AppCompatActivity {

    private EditText etLoanAmount, etInterestRate, etTermMonths, etSpecifiedMonth, etStartDate;
    private Button btnCalculate, btnCalculateInterest, btnShowAmortization;
    private TextView tvMonthlyPayment, tvInterestPaid, tvTotalPayments, tvInterestForMonth,
            tvResultsTitle, tvLastPaymentDate, tvBirthYear;
    private double monthlyRepayment;
    private double monthlyInterestRate;
    private double loanAmount;
    private int totalMonths;
    private int birthYear;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_housingloan);

        // Retrieve birthdate from SharedPreferences and extract the year
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String birthdate = sharedPreferences.getString("birthdate", "");
        birthYear = Integer.parseInt(birthdate.split("-")[0]);

        // Set up the toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initializeViews();
        setupDatePicker();
        setupListeners();

        // Display birthdate
        tvBirthYear.setText("Birthdate: " + birthdate);
    }

    private void initializeViews() {
        etLoanAmount = findViewById(R.id.et_loan_amount);
        etInterestRate = findViewById(R.id.et_interest_rate);
        etTermMonths = findViewById(R.id.et_term_months);
        etSpecifiedMonth = findViewById(R.id.et_specified_month);
        etStartDate = findViewById(R.id.et_start_date);
        btnCalculate = findViewById(R.id.btn_calculate);
        btnCalculateInterest = findViewById(R.id.btn_calculate_interest);
        btnShowAmortization = findViewById(R.id.btn_show_amortization);
        tvMonthlyPayment = findViewById(R.id.tv_monthly_payment);
        tvInterestPaid = findViewById(R.id.tv_interest_paid);
        tvTotalPayments = findViewById(R.id.tv_total_payments);
        tvInterestForMonth = findViewById(R.id.tv_interest_for_month);
        tvResultsTitle = findViewById(R.id.tv_results_title);
        tvLastPaymentDate = findViewById(R.id.tv_last_payment_date);
        tvBirthYear = findViewById(R.id.tv_birth_year);

        // Initialize calendar
        calendar = Calendar.getInstance();

        // Add TextWatcher to etLoanAmount and etInterestRate
        addCurrencyTextWatcher(etLoanAmount, "RM ");
        addPercentageTextWatcher(etInterestRate);
    }

    private void setupDatePicker() {
        etStartDate.setOnClickListener(v -> {
            new DatePickerDialog(housingloan.this, (view, year, monthOfYear, dayOfMonth) -> {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, monthOfYear);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateLabel();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.
                    get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void setupListeners() {
        btnCalculate.setOnClickListener(v -> calculateHousingLoan());
        btnCalculateInterest.setOnClickListener(v -> calculateInterestForSpecifiedMonth());
        btnShowAmortization.setOnClickListener(v -> showAmortizationSchedule());
    }

    private void addCurrencyTextWatcher(EditText editText, String prefix) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                String input = s.toString().replace(prefix, "").
                        replaceAll(",", "");
                if (!input.isEmpty()) {
                    double parsed = Double.parseDouble(input);
                    String formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed);
                    editText.setText(prefix + formatted);
                    editText.setSelection(editText.getText().length());
                }

                isEditing = false;
            }
        });
    }

    private void addPercentageTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                String input = s.toString().replace("%", "");
                if (!input.isEmpty()) {
                    editText.setText(input + "%");
                    editText.setSelection(editText.getText().length() - 1);
                }

                isEditing = false;
            }
        });
    }

    private void updateLabel() {
        String myFormat = "yyyy-MM-dd"; // In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
        etStartDate.setText(sdf.format(calendar.getTime()));
    }

    private void calculateHousingLoan() {
        // Retrieve and validate input values
        String loanAmountStr = etLoanAmount.getText().toString().replace
                ("RM ", "").replaceAll(",", "");
        if (loanAmountStr.isEmpty()) {
            showError("Please enter a valid loan amount");
            return;
        }
        loanAmount = Double.parseDouble(loanAmountStr);

        String interestRateStr = etInterestRate.getText().toString().
                replace("%", "");
        if (interestRateStr.isEmpty()) {
            showError("Please enter a valid interest rate");
            return;
        }
        double interestRate = Double.parseDouble(interestRateStr);

        String termMonthsStr = etTermMonths.getText().toString().trim();
        if (termMonthsStr.isEmpty()) {
            showError("Please enter a valid term in months");
            return;
        }
        totalMonths = Integer.parseInt(termMonthsStr);

        String startDateStr = etStartDate.getText().toString().trim();
        if (startDateStr.isEmpty()) {
            showError("Please enter a valid start date");
            return;
        }

        // Calculate the maximum allowed loan tenure
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        int userAge = currentYear - birthYear;
        int maxLoanTenureYears = Math.min(35, 70 - userAge);
        int maxLoanTenureMonths = maxLoanTenureYears * 12;

        // Validate loan tenure
        if (totalMonths > maxLoanTenureMonths || totalMonths < 1) {
            Toast.makeText(this, "Loan tenure exceeds maximum " +
                    "allowed", Toast.LENGTH_LONG).show();
            return;
        }


        // Calculate monthly repayment using the formula: P = [r*P* (1+r)^N] / [(1+r)^N – 1]
        monthlyInterestRate = interestRate / 12 / 100;
        monthlyRepayment = (loanAmount * monthlyInterestRate *
                Math.pow(1 + monthlyInterestRate, totalMonths)) /
                (Math.pow(1 + monthlyInterestRate, totalMonths) - 1);
        double totalRepayment = monthlyRepayment * totalMonths;
        double interestPaid = totalRepayment - loanAmount;

        // Calculate the last payment date
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date startDate = sdf.parse(startDateStr);
            calendar.setTime(startDate);
            calendar.add(Calendar.MONTH, totalMonths);
            Date lastPaymentDate = calendar.getTime();
            String lastPaymentDateString = sdf.format(lastPaymentDate);

            // Update UI with results
            tvResultsTitle.setVisibility(View.VISIBLE);
            tvResultsTitle.setTextColor(Color.BLACK);
            tvMonthlyPayment.setVisibility(View.VISIBLE);
            tvInterestPaid.setVisibility(View.VISIBLE);
            tvTotalPayments.setVisibility(View.VISIBLE);
            tvLastPaymentDate.setVisibility(View.VISIBLE);

            DecimalFormat df = new DecimalFormat("#,##0.00");

            tvMonthlyPayment.setText(String.format(Locale.getDefault(), "Monthly Payment: RM %s", df.format(monthlyRepayment)));
            tvInterestPaid.setText(String.format(Locale.getDefault(), "Interest Paid: RM %s", df.format(interestPaid)));
            tvTotalPayments.setText(String.format(Locale.getDefault(), "Total Payments: RM %s", df.format(totalRepayment)));
            tvLastPaymentDate.setText(String.format(Locale.getDefault(), "Last Payment Date: %s", lastPaymentDateString));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void calculateInterestForSpecifiedMonth() {
        String specifiedMonthStr = etSpecifiedMonth.getText().toString().trim();
        if (specifiedMonthStr.isEmpty()) {
            showError("Please enter a valid month");
            return;
        }
        int specifiedMonth = Integer.parseInt(specifiedMonthStr);

        if (specifiedMonth < 1 || specifiedMonth > totalMonths) {
            showError("Specified month exceeds loan term");
            return;
        }

        double balance = loanAmount;
        double interestPaid = 0;
        double principalPaid = 0;

        for (int month = 1; month <= specifiedMonth; month++) {
            double interest = balance * monthlyInterestRate;
            double principal = monthlyRepayment - interest;
            balance -= principal;

            if (month == specifiedMonth) {
                interestPaid = interest;
                principalPaid = principal;
            }
        }

        DecimalFormat df = new DecimalFormat("#,##0.00");

        LinearLayout llInterestDetails = findViewById(R.id.ll_interest_details);
        llInterestDetails.setVisibility(View.VISIBLE);

        TextView tvInterestForMonth = findViewById(R.id.tv_interest_for_month);
        TextView tvPrincipalForMonth = findViewById(R.id.tv_principal_for_month);
        TextView tvRemainingBalanceForMonth = findViewById(R.id.tv_remaining_balance_for_month);

        tvInterestForMonth.setText(String.format(Locale.getDefault(),
                "Interest for Month %d: RM %s", specifiedMonth,
                df.format(interestPaid)));
        tvPrincipalForMonth.setText(String.format(Locale.getDefault(),
                "Principal for Month %d: RM %s", specifiedMonth, df.format(principalPaid)));
        tvRemainingBalanceForMonth.setText(String.format(Locale.getDefault(),
                "Remaining Balance: RM %s", df.format(balance)));
    }


    private void showAmortizationSchedule() {
        double balance = loanAmount;
        ArrayList<String[]> schedule = new ArrayList<>();

        DecimalFormat df = new DecimalFormat("#,##0.00");

        for (int month = 1; month <= totalMonths; month++) {
            double interest = balance * monthlyInterestRate;
            double principal = monthlyRepayment - interest;
            double beginningBalance = balance;
            balance -= principal;

            schedule.add(new String[]{String.valueOf(month), df.format(beginningBalance),
                    df.format(monthlyRepayment), df.format(interest), df.format(principal)});
        }

        Intent intent = new Intent(housingloan.this, AmortizationSchedule.class);
        intent.putExtra("AMORTIZATION_DATA", schedule);
        intent.putExtra("LOAN_AMOUNT", loanAmount);
        intent.putExtra("INTEREST_RATE", Double.parseDouble(etInterestRate.getText().
                toString().replace("%", ""))); // Pass the user input interest rate
        intent.putExtra("LOAN_TENURE", totalMonths);
        startActivity(intent);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        tvResultsTitle.setVisibility(View.GONE);
        tvMonthlyPayment.setVisibility(View.GONE);
        tvInterestPaid.setVisibility(View.GONE);
        tvTotalPayments.setVisibility(View.GONE);
        tvLastPaymentDate.setVisibility(View.GONE);
    }
}
