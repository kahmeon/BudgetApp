package com.example.mybudget;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.github.mikephil.charting.charts.PieChart;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportsFragment extends Fragment {

    private Spinner spinnerTimePeriod;
    private Spinner spinnerMonth, spinnerYear;
    private TextView textViewSelectDate;
    private LinearLayout dateSelectionLayout;
    private TextView tabExpense, tabRevenue,tabExpensesRevenue;
    private RecyclerView recyclerViewReports;
    private TransactionsAdapter adapter;
    private List<TransactionModel> transactionList; // Changed to TransactionModel
    private FirebaseFirestore db;
    private String currentTab = "Expense";
    private PieChart pieChart;


    public ReportsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        // Initialize UI elements

        tabExpense = view.findViewById(R.id.tab_expense);
        tabRevenue = view.findViewById(R.id.tab_revenue);
        tabExpensesRevenue=view.findViewById(R.id.tab_expenses_revenue);
        recyclerViewReports = view.findViewById(R.id.recycler_view_reports);
        pieChart = view.findViewById(R.id.pie_chart);
        spinnerMonth = view.findViewById(R.id.spinner_month);
        spinnerYear = view.findViewById(R.id.spinner_year);
        textViewSelectDate = view.findViewById(R.id.text_view_date_range);
        dateSelectionLayout = view.findViewById(R.id.date_selection_layout);

        initializeDefaults();


        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        // Setup RecyclerView
        recyclerViewReports.setLayoutManager(new LinearLayoutManager(getContext()));
        transactionList = new ArrayList<>();
        adapter = new TransactionsAdapter(transactionList);
        recyclerViewReports.setAdapter(adapter);

        // Set up tabs and spinner listeners
        setupTabListeners();


        // Load initial data for "Expense" tab
        fetchTransactions();
        fetchChartData();



        // After fetching or updating your category sums and totalAmount
        HashMap<String, Float> categorySums = new HashMap<>();
        float totalAmount = calculateTotalAmount(categorySums); // Implement this method or variable


        return view;
    }

    private float calculateTotalAmount(HashMap<String, Float> categorySums) {
        float total = 0f;
        for (float value : categorySums.values()) {
            total += value;
        }
        return total;
    }


    private void setupTabListeners() {
        // Set click listener for Expense tab
        tabExpense.setOnClickListener(view -> {
            currentTab = "Expense";
            fetchTransactions();
            fetchChartData();
        });

        // Set click listener for Revenue tab
        tabRevenue.setOnClickListener(view -> {
            currentTab = "Income";
            fetchTransactions();
            fetchChartData();
        });

        // Set click listener for All Transactions
        tabExpensesRevenue.setOnClickListener(view -> {
            currentTab = "All"; // New "All" tab
            fetchTransactions();
            fetchChartData();
        });
    }



    private long getStartDate(String timePeriod) {
        long currentTime = System.currentTimeMillis();
        switch (timePeriod) {
            case "Weekly":
                return currentTime - (7L * 24 * 60 * 60 * 1000); // Last 7 days
            case "Monthly":
                return currentTime - (30L * 24 * 60 * 60 * 1000); // Last 30 days
            case "Yearly":
                return currentTime - (365L * 24 * 60 * 60 * 1000); // Last 365 days
            default:
                return 0; // "All Time" - no filtering
        }
    }

    private void handleTimePeriodSelection(String selectedPeriod) {
        dateSelectionLayout.setVisibility(View.VISIBLE);

        switch (selectedPeriod) {
            case "Weekly":
                configureWeeklySelection();
                break;
            case "Monthly":
                configureMonthlySelection();
                break;
            case "Yearly":
                configureYearlySelection();
                break;
            default:
                dateSelectionLayout.setVisibility(View.GONE);
                fetchTransactions();
                break;
        }
    }

    private void configureWeeklySelection() {
        spinnerMonth.setVisibility(View.GONE);
        spinnerYear.setVisibility(View.GONE);
        textViewSelectDate.setVisibility(View.VISIBLE);
        textViewSelectDate.setOnClickListener(v -> openDateRangePicker());
    }

    private void configureMonthlySelection() {
        spinnerMonth.setVisibility(View.VISIBLE); // Show the month spinner
        spinnerYear.setVisibility(View.VISIBLE); // Show the year spinner
        textViewSelectDate.setVisibility(View.GONE);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int month = position; // 0 = January
                String selectedYear = spinnerYear.getSelectedItem().toString();
                int year = Integer.parseInt(selectedYear);

                // Fetch transactions for the selected year and month
                fetchMonthlyTransactions(year, month);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int year = Integer.parseInt(parent.getItemAtPosition(position).toString());
                int month = spinnerMonth.getSelectedItemPosition();

                // Fetch transactions for the selected year and month
                fetchMonthlyTransactions(year, month);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }



    private void configureYearlySelection() {
        spinnerMonth.setVisibility(View.GONE); // Hide the month spinner
        spinnerYear.setVisibility(View.VISIBLE); // Show the year spinner
        textViewSelectDate.setVisibility(View.GONE);

        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedYear = parent.getItemAtPosition(position).toString();
                int year = Integer.parseInt(selectedYear);

                // Fetch transactions for the selected year
                fetchYearlyTransactions(year);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }



    private void setupDateRangePicker() {
        textViewSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            DatePickerDialog startDatePicker = new DatePickerDialog(getContext(),
                    (startDateView, startYear, startMonth, startDayOfMonth) -> {
                        Calendar startDate = Calendar.getInstance();

                        startDate.set(startYear, startMonth, startDayOfMonth, 0, 0, 0);
                        startDate.set(Calendar.MILLISECOND, 0);
                        textViewSelectDate.setEnabled(false);

                        DatePickerDialog endDatePicker = new DatePickerDialog(getContext(),
                                (endDateView, endYear, endMonth, endDayOfMonth) -> {
                                    Calendar endDate = Calendar.getInstance();
                                    endDate.set(endYear, endMonth, endDayOfMonth, 23, 59, 59);
                                    endDate.set(Calendar.MILLISECOND, 999);

                                    if (endDate.before(startDate)) {
                                        Toast.makeText(getContext(), "End date must be after start date.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        fetchTransactionsInRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
                                    }
                                },
                                startYear, startMonth, startDayOfMonth);
                        endDatePicker.show();
                    },
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            startDatePicker.show();
        });
    }


    private void openDateRangePicker() {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog startDatePicker = new DatePickerDialog(getContext(),
                (startDateView, startYear, startMonth, startDay) -> {
                    Calendar startDate = Calendar.getInstance();
                    startDate.set(startYear, startMonth, startDay);

                    DatePickerDialog endDatePicker = new DatePickerDialog(getContext(),
                            (endDateView, endYear, endMonth, endDay) -> {
                                Calendar endDate = Calendar.getInstance();
                                endDate.set(endYear, endMonth, endDay);

                                if (endDate.before(startDate)) {
                                    Toast.makeText(getContext(), "End date must be after start date.", Toast.LENGTH_SHORT).show();
                                } else {
                                    fetchTransactionsInRange(startDate.getTimeInMillis(), endDate.getTimeInMillis());
                                }
                            },
                            startYear, startMonth, startDay);
                    endDatePicker.show();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        startDatePicker.show();
    }


    private void setupMonthlyListeners() {
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int month = position; // Month index (0 = January)
                String yearString = spinnerYear.getSelectedItem().toString();
                if (yearString.isEmpty()) {
                    Log.e("ReportsFragment", "Year not selected!");
                    return;
                }
                int year = Integer.parseInt(yearString);

                Log.d("ReportsFragment", "Monthly Selected - Year: " + year + ", Month: " + month);

                fetchMonthlyTransactions(year, month);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void setupYearlyListeners() {
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String yearString = parent.getItemAtPosition(position).toString();
                if (yearString.isEmpty()) {
                    Log.e("ReportsFragment", "Year not selected!");
                    return;
                }
                int year = Integer.parseInt(yearString);

                Log.d("ReportsFragment", "Yearly Selected - Year: " + year);

                fetchYearlyTransactions(year);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });
    }

    private void fetchTransactionsInRange(long startDate, long endDate) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("Firestore", "User is not authenticated.");
            return;
        }

        Log.d("Firestore", "Fetching transactions in range: Start Date = " + startDate + ", End Date = " + endDate);

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("type", currentTab) // Filter by current tab (Expense or Income)
                .whereGreaterThanOrEqualTo("date", startDate) // Start date filter
                .whereLessThanOrEqualTo("date", endDate) // End date filter
                .orderBy("date", Query.Direction.DESCENDING) // Required for range queries
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions found for the selected period.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            TransactionModel transaction = document.toObject(TransactionModel.class);
                            if (transaction != null) {
                                transactionList.add(transaction);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Log.d("Firestore", "Transactions fetched: " + transactionList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching transactions: ", e);
                });
    }


    private void fetchMonthlyTransactions(int year, int month) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("Firestore", "User not authenticated.");
            return;
        }

        Calendar startDate = Calendar.getInstance();
        startDate.set(year, month, 1, 0, 0, 0); // Start of the month
        startDate.set(Calendar.MILLISECOND, 0);

        Calendar endDate = Calendar.getInstance();
        endDate.set(year, month, startDate.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59); // End of the month
        endDate.set(Calendar.MILLISECOND, 999);

        long startMillis = startDate.getTimeInMillis();
        long endMillis = endDate.getTimeInMillis();

        Log.d("Firestore", "Monthly Filter - Start: " + startMillis + ", End: " + endMillis);

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("type", currentTab) // Filter by tab (Expense or Income)
                .whereGreaterThanOrEqualTo("date", startMillis) // Filter for the start of the month
                .whereLessThanOrEqualTo("date", endMillis) // Filter for the end of the month
                .orderBy("type", Query.Direction.ASCENDING) // Matches index field order
                .orderBy("date", Query.Direction.DESCENDING) // Order by date
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions found for the selected month.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            TransactionModel transaction = document.toObject(TransactionModel.class);
                            if (transaction != null) {
                                transactionList.add(transaction);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    Log.d("Firestore", "Monthly Transactions fetched: " + transactionList.size());
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching monthly transactions: ", e));
    }


    private void fetchYearlyTransactions(int year) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("Firestore", "User not authenticated.");
            return;
        }

        Calendar startDate = Calendar.getInstance();
        startDate.set(year, Calendar.JANUARY, 1, 0, 0, 0); // Start of the year
        startDate.set(Calendar.MILLISECOND, 0);

        Calendar endDate = Calendar.getInstance();
        endDate.set(year, Calendar.DECEMBER, 31, 23, 59, 59); // End of the year
        endDate.set(Calendar.MILLISECOND, 999);

        long startMillis = startDate.getTimeInMillis();
        long endMillis = endDate.getTimeInMillis();

        Log.d("Firestore", "Yearly Filter - Start: " + startMillis + ", End: " + endMillis);

        db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("type", currentTab) // Filter by tab (Expense or Income)
                .whereGreaterThanOrEqualTo("date", startMillis) // Filter for the start of the year
                .whereLessThanOrEqualTo("date", endMillis) // Filter for the end of the year
                .orderBy("type", Query.Direction.ASCENDING)
                .orderBy("date", Query.Direction.DESCENDING) // Order by date
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No transactions found for the selected year.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            TransactionModel transaction = document.toObject(TransactionModel.class);
                            if (transaction != null) {
                                transactionList.add(transaction);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    Log.d("Firestore", "Yearly Transactions fetched: " + transactionList.size());
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching yearly transactions: ", e));
    }

    private void fetchTransactions() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("Firestore", "User is not authenticated.");
            return;
        }

        // Build query based on currentTab
        Query query = db.collection("users")
                .document(userId)
                .collection("transactions");

        if (!"All".equals(currentTab)) {
            query = query.whereEqualTo("type", currentTab); // Filter by type if not "All"
        }

        query.orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactionList.clear(); // Clear the list before adding new data
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d("Firestore", "No transactions found.");
                        Toast.makeText(getContext(), "No transactions found.", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            TransactionModel transaction = document.toObject(TransactionModel.class);
                            if (transaction != null) {
                                transactionList.add(transaction);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged(); // Notify the adapter of data changes
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching transactions: " + e.getMessage());
                    Toast.makeText(getContext(), "Failed to load transactions.", Toast.LENGTH_SHORT).show();
                });

        // Call fetchChartData for updated Pie Chart
        fetchChartData();
    }

    private void fetchChartData() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Log.e("Firestore", "User is not authenticated.");
            return;
        }

        // Build query based on currentTab
        Query query = db.collection("users")
                .document(userId)
                .collection("transactions");

        if (!"All".equals(currentTab)) {
            query = query.whereEqualTo("type", currentTab); // Filter by type if not "All"
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No chart data found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<PieEntry> entries = new ArrayList<>();
                    HashMap<String, Float> categorySums = new HashMap<>();
                    float totalAmount = 0f;

                    // Process transactions to calculate category sums
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String category = document.getString("category");
                        Double amount = document.getDouble("amount");

                        if (category != null && amount != null) {
                            categorySums.put(category, categorySums.getOrDefault(category, 0f) + amount.floatValue());
                            totalAmount += amount.floatValue();
                        }
                    }

                    // Convert sums to PieChart entries with percentages
                    for (Map.Entry<String, Float> entry : categorySums.entrySet()) {
                        float percentage = (entry.getValue() / totalAmount) * 100;
                        entries.add(new PieEntry(entry.getValue(), entry.getKey() + " (" + String.format("%.1f", percentage) + "%)"));
                    }

                    if (entries.isEmpty()) {
                        Toast.makeText(getContext(), "No valid data for chart.", Toast.LENGTH_SHORT).show();
                    } else {
                        updatePieChart(entries);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error fetching chart data: ", e);
                    Toast.makeText(getContext(), "Failed to load chart data.", Toast.LENGTH_SHORT).show();
                });
    }


    private void initializeDefaults() {
        Calendar today = Calendar.getInstance();
        int currentYear = today.get(Calendar.YEAR);
        int currentMonth = today.get(Calendar.MONTH); // 0 = January

        // Set default values for spinners
        spinnerYear.post(() -> spinnerYear.setSelection(getYearIndex(currentYear))); // Ensure spinner is populated before setting
        spinnerMonth.post(() -> spinnerMonth.setSelection(currentMonth));
    }

    // Helper method to get the index of the year in the spinner
    private int getYearIndex(int year) {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinnerYear.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(String.valueOf(year))) {
                return i;
            }
        }
        return 0; // Default to the first item if not found
    }


    private void updatePieChart(ArrayList<PieEntry> entries) {
        // Create a dataset and assign it to the PieChart
        PieDataSet dataSet = new PieDataSet(entries, "Transactions");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS); // Set colors
        dataSet.setValueTextSize(12f); // Set text size
        dataSet.setSliceSpace(3f); // Add spacing between slices
        dataSet.setSelectionShift(5f); // Increase selection shift for selected slices

        // Create PieData and set it to the PieChart
        PieData pieData = new PieData(dataSet);
        pieData.setValueTextColor(Color.WHITE); // Set text color
        pieData.setValueTextSize(12f);

        pieChart.setData(pieData);
        pieChart.invalidate(); // Refresh the chart
        pieChart.setUsePercentValues(true); // Show values as percentages
        pieChart.getDescription().setEnabled(false); // Disable description
        pieChart.setDrawHoleEnabled(true); // Enable hole in the center
        pieChart.setHoleRadius(40f); // Set the hole radius
        pieChart.setTransparentCircleRadius(50f); // Set the transparent circle radius
        pieChart.setRotationEnabled(true); // Enable rotation
    }



}
