package com.example.mybudget;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
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

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String birthdate = sharedPreferences.getString("birthdate", "");

        if (birthdate.isEmpty() || !birthdate.contains("-")) {
            Toast.makeText(this, "Please set your birthdate in profile first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        birthYear = Integer.parseInt(birthdate.split("-")[0]);

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

        calendar = Calendar.getInstance();

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
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
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

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;

                String input = s.toString().replace(prefix, "").replaceAll(",", "");
                if (!input.isEmpty()) {
                    try {
                        double parsed = Double.parseDouble(input);
                        String formatted = NumberFormat.getNumberInstance(Locale.US).format(parsed);
                        editText.setText(prefix + formatted);
                        editText.setSelection(editText.getText().length());
                    } catch (Exception e) {
                        // Ignore
                    }
                }

                isEditing = false;
            }
        });
    }

    private void addPercentageTextWatcher(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isEditing = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
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
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        etStartDate.setText(sdf.format(calendar.getTime()));
    }

    private void calculateHousingLoan() {
        String loanAmountStr = etLoanAmount.getText().toString().replace("RM ", "").replaceAll(",", "");
        String interestRateStr = etInterestRate.getText().toString().replace("%", "");
        String termMonthsStr = etTermMonths.getText().toString().trim();
        String startDateStr = etStartDate.getText().toString().trim();

        if (loanAmountStr.isEmpty() || interestRateStr.isEmpty() || termMonthsStr.isEmpty() || startDateStr.isEmpty()) {
            showError("Please fill all fields");
            return;
        }

        try {
            loanAmount = Double.parseDouble(loanAmountStr);
            double interestRate = Double.parseDouble(interestRateStr);
            totalMonths = Integer.parseInt(termMonthsStr);

            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int userAge = currentYear - birthYear;
            int maxLoanTenureYears = Math.min(35, 70 - userAge);
            int maxLoanTenureMonths = maxLoanTenureYears * 12;

            if (totalMonths > maxLoanTenureMonths || totalMonths < 1) {
                Toast.makeText(this, "Loan tenure exceeds maximum allowed", Toast.LENGTH_LONG).show();
                return;
            }

            monthlyInterestRate = interestRate / 12 / 100;
            monthlyRepayment = (loanAmount * monthlyInterestRate * Math.pow(1 + monthlyInterestRate, totalMonths)) /
                    (Math.pow(1 + monthlyInterestRate, totalMonths) - 1);

            double totalRepayment = monthlyRepayment * totalMonths;
            double interestPaid = totalRepayment - loanAmount;

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date startDate = sdf.parse(startDateStr);
            calendar.setTime(startDate);
            calendar.add(Calendar.MONTH, totalMonths);
            String lastPaymentDateString = sdf.format(calendar.getTime());

            DecimalFormat df = new DecimalFormat("#,##0.00");

            tvResultsTitle.setVisibility(View.VISIBLE);
            tvResultsTitle.setTextColor(Color.BLACK);
            tvMonthlyPayment.setVisibility(View.VISIBLE);
            tvInterestPaid.setVisibility(View.VISIBLE);
            tvTotalPayments.setVisibility(View.VISIBLE);
            tvLastPaymentDate.setVisibility(View.VISIBLE);

            tvMonthlyPayment.setText("Monthly Payment: RM " + df.format(monthlyRepayment));
            tvInterestPaid.setText("Interest Paid: RM " + df.format(interestPaid));
            tvTotalPayments.setText("Total Payments: RM " + df.format(totalRepayment));
            tvLastPaymentDate.setText("Last Payment Date: " + lastPaymentDateString);

        } catch (Exception e) {
            showError("Error calculating loan. Please check inputs.");
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

        TextView tvPrincipalForMonth = findViewById(R.id.tv_principal_for_month);
        TextView tvRemainingBalanceForMonth = findViewById(R.id.tv_remaining_balance_for_month);

        tvInterestForMonth.setText(String.format("Interest for Month %d: RM %s", specifiedMonth, df.format(interestPaid)));
        tvPrincipalForMonth.setText(String.format("Principal for Month %d: RM %s", specifiedMonth, df.format(principalPaid)));
        tvRemainingBalanceForMonth.setText(String.format("Remaining Balance: RM %s", df.format(balance)));
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
        intent.putExtra("INTEREST_RATE", Double.parseDouble(etInterestRate.getText().toString().replace("%", "")));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_housingloan, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            showInfoDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Housing Loan Calculation Guide")
                .setMessage(
                        "This calculator helps you analyze your housing loan with accurate formulas.\n\n" +

                                "\u270D\ufe0f **Main Formula: Monthly Repayment**\n" +
                                "  P = [r × L × (1 + r)^n] / [(1 + r)^n – 1]\n" +
                                "  Where:\n" +
                                "  • L = Loan amount\n" +
                                "  • r = Monthly interest rate (annual rate ÷ 12 ÷ 100)\n" +
                                "  • n = Loan term in months\n" +
                                "  • P = Monthly payment\n\n" +

                                "\uD83D\uDCB8 **Total Interest Paid**\n" +
                                "  = (Monthly Payment × Total Months) – Loan Amount\n\n" +

                                "\uD83D\uDCB0 **Total Repayment**\n" +
                                "  = Monthly Payment × Total Months\n\n" +

                                "\uD83D\uDCC5 **Last Payment Date**\n" +
                                "  = Start Date + Loan Tenure (in months)\n\n" +

                                "\uDCCB **Specified Month Calculation**\n" +
                                "  For any month:\n" +
                                "  • Interest = Remaining Balance × Monthly Interest Rate\n" +
                                "  • Principal = Monthly Payment – Interest\n" +
                                "  • New Balance = Previous Balance – Principal\n\n" +

                                "\uD83D\uDCCA **Amortization Schedule**\n" +
                                "  Shows a monthly breakdown of:\n" +
                                "  • Beginning Balance\n" +
                                "  • Monthly Payment\n" +
                                "  • Interest Paid\n" +
                                "  • Principal Paid\n" +
                                "  • Remaining Balance\n\n" +

                                "\uD83D\uDCA1 Tip:\n" +
                                "  • Max tenure: Up to 35 years or until age 70\n" +
                                "  • Fill all fields correctly for best results\n" +
                                "  • Use amortization to plan early repayment"
                )
                .setPositiveButton("OK", null)
                .show();
    }


}