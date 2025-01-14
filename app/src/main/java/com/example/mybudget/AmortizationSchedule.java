package com.example.mybudget;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AmortizationSchedule extends AppCompatActivity {

    private static final DecimalFormat CURRENCY_FORMAT = new DecimalFormat("RM #,##0.00");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_amortization_schedule);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TableLayout tableLayout = findViewById(R.id.tableLayout);

        Intent intent = getIntent();
        double loanAmount = intent.getDoubleExtra("LOAN_AMOUNT", 0);
        double interestRate = intent.getDoubleExtra("INTEREST_RATE", 0);
        int loanTenure = intent.getIntExtra("LOAN_TENURE", 0);
        ArrayList<String[]> schedule = (ArrayList<String[]>) intent.getSerializableExtra("AMORTIZATION_DATA");

        // Set user input information
        ((TextView) findViewById(R.id.tvLoanAmount)).setText(CURRENCY_FORMAT.format(loanAmount));
        ((TextView) findViewById(R.id.tvInterestRate)).setText(String.format(Locale.getDefault(), "%.2f%%", interestRate));
        ((TextView) findViewById(R.id.tvLoanTenure)).setText(String.valueOf(loanTenure));

        if (schedule != null && schedule.size() > 0) {
            // Add table headers
            TableRow headerRow = createTableRow();
            headerRow.addView(createTextView("Payment number"));
            headerRow.addView(createTextView("Beginning balance (RM)"));
            headerRow.addView(createTextView("Monthly repayment (RM)"));
            headerRow.addView(createTextView("Interest paid (RM)"));
            headerRow.addView(createTextView("Principal paid (RM)"));
            tableLayout.addView(headerRow);

            // Populate table with data
            for (String[] row : schedule) {
                TableRow dataRow = createTableRow();
                for (String cell : row) {
                    dataRow.addView(createTextView(cell));
                }
                tableLayout.addView(dataRow);
            }
        }
    }

    private TableRow createTableRow() {
        TableRow tableRow = new TableRow(this);
        tableRow.setPadding(0, 8, 0, 8); // Add padding to rows
        tableRow.setBackgroundResource(android.R.color.darker_gray); // Add background color for table rows
        return tableRow;
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(16, 8, 16, 8); // Add padding to text views
        textView.setMaxLines(1);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(Gravity.CENTER); // Center align text
        return textView;
    }
}
