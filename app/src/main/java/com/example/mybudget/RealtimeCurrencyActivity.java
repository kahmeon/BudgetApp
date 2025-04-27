package com.example.mybudget;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RealtimeCurrencyActivity extends AppCompatActivity {

    private Spinner spinnerFrom, spinnerTo;
    private TextInputEditText etFromAmount, etToAmount;
    private TextView tvConversionRate, tvLastUpdated, tvNetworkStatus;
    private MaterialButton btnConvert, btnSwap;
    private LineChart chartHistoricalRates;
    private View loadingIndicator;

    private RequestQueue requestQueue;
    private boolean isNetworkAvailable = true;
    private final Map<String, Double> cachedRates = new HashMap<>();

    private static final String API_BASE_URL = "https://api.exchangerate.host/";
    private static final String PREFS_NAME = "currencyRates";
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final List<String> currencies = Arrays.asList(
            "USD", "MYR", "EUR", "JPY", "GBP", "AUD", "CAD", "INR", "SGD", "CNY", "THB", "IDR"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.realtimecurrency);

        requestQueue = Volley.newRequestQueue(this); // move this up
        initViews();
        setupSpinners();
        setupListeners();
        checkNetworkStatus(); // now this is accurate
        loadCachedRates();

    }

    private void initViews() {
        spinnerFrom = findViewById(R.id.spinner_from_currency);
        spinnerTo = findViewById(R.id.spinner_to_currency);
        etFromAmount = findViewById(R.id.et_from_amount);
        etToAmount = findViewById(R.id.et_to_amount);
        tvConversionRate = findViewById(R.id.tv_conversion_rate);
        tvLastUpdated = findViewById(R.id.tv_last_updated);
        tvNetworkStatus = findViewById(R.id.tv_network_status);
        btnConvert = findViewById(R.id.btn_convert);
        btnSwap = findViewById(R.id.btn_swap_currencies);
        chartHistoricalRates = findViewById(R.id.chart_historical_rates);
        loadingIndicator = findViewById(R.id.loading_indicator);
    }

    private void setupSpinners() {
        List<CurrencyItem> currencyItems = Arrays.asList(
                new CurrencyItem("USD", R.drawable.flag_usd),
                new CurrencyItem("MYR", R.drawable.flag_myr),
                new CurrencyItem("EUR", R.drawable.flag_eur),
                new CurrencyItem("JPY", R.drawable.flag_jpy),
                new CurrencyItem("GBP", R.drawable.flag_gbp),
                new CurrencyItem("AUD", R.drawable.flag_aud),
                new CurrencyItem("CAD", R.drawable.flag_cad),
                new CurrencyItem("INR", R.drawable.flag_inr),
                new CurrencyItem("SGD", R.drawable.flag_sgd),
                new CurrencyItem("CNY", R.drawable.flag_cny),
                new CurrencyItem("THB", R.drawable.flag_thb),
                new CurrencyItem("IDR", R.drawable.flag_idr)
        );

        CurrencySpinnerAdapter adapter = new CurrencySpinnerAdapter(this, currencyItems);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        spinnerFrom.setSelection(findCurrencyIndex(currencyItems, "USD"));
        spinnerTo.setSelection(findCurrencyIndex(currencyItems, "MYR"));

        spinnerFrom.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!etFromAmount.getText().toString().isEmpty()) {
                    performConversion();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerTo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!etFromAmount.getText().toString().isEmpty()) {
                    performConversion();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        btnConvert.setOnClickListener(v -> performConversion());

        btnSwap.setOnClickListener(v -> swapCurrencies());

        etFromAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    performConversion();
                } else {
                    etToAmount.setText("");
                    tvConversionRate.setText("");
                }
            }
        });
    }

    private void performConversion() {
        String amountStr = etFromAmount.getText().toString();
        if (amountStr.isEmpty()) {
            etToAmount.setText("");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            CurrencyItem fromCurrency = (CurrencyItem) spinnerFrom.getSelectedItem();
            CurrencyItem toCurrency = (CurrencyItem) spinnerTo.getSelectedItem();

            String from = fromCurrency.getCode();
            String to = toCurrency.getCode();

            // Check cache first
            String cacheKey = from + "_" + to;
            if (cachedRates.containsKey(cacheKey)) {
                double cachedRate = cachedRates.get(cacheKey);
                updateConversionResult(amount * cachedRate, cachedRate, from, to, true);
                return;
            }

            if (!isNetworkAvailable) {
                useOfflineRate(from, to, amount);
                return;
            }

            showLoading(true);
            fetchConversionRate(from, to, amount, false);
        } catch (NumberFormatException e) {
            etFromAmount.setError("Invalid amount");
            showLoading(false);
        }
    }


    private void fetchConversionRate(String from, String to, double amount, boolean fallback) {
        String url = "https://api.frankfurter.app/latest?amount=" + amount + "&from=" + from + "&to=" + to;
        Log.d("CurrencyDebug", "Request URL: " + url);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    showLoading(false);

                    try {
                        Log.d("CurrencyDebug", "Raw response: " + response.toString());

                        if (!response.has("rates")) {
                            Log.e("CurrencyDebug", "Missing 'rates' in response.");
                            useOfflineRate(from, to, amount);
                            return;
                        }

                        JSONObject rates = response.getJSONObject("rates");

                        if (!rates.has(to)) {
                            Log.e("CurrencyDebug", "Target currency not found in rates.");
                            useOfflineRate(from, to, amount);
                            return;
                        }

                        double result = rates.getDouble(to);
                        double rate = result / amount;

                        // Cache rate
                        cachedRates.put(from + "_" + to, rate);
                        cachedRates.put(to + "_" + from, 1 / rate);

                        // Update UI
                        updateConversionResult(result, rate, from, to, false);

                    } catch (Exception e) {
                        Log.e("CurrencyDebug", "Parse error: ", e);
                        useOfflineRate(from, to, amount);
                    }
                },
                error -> {
                    showLoading(false);
                    Log.e("CurrencyDebug", "Network error: ", error);
                    isNetworkAvailable = false;
                    updateNetworkStatus();
                    useOfflineRate(from, to, amount);
                });

        requestQueue.add(request);
    }


    private void swapCurrencies() {
        int fromPos = spinnerFrom.getSelectedItemPosition();
        int toPos = spinnerTo.getSelectedItemPosition();
        spinnerFrom.setSelection(toPos);
        spinnerTo.setSelection(fromPos);

        String fromAmount = etFromAmount.getText().toString();
        String toAmount = etToAmount.getText().toString();
        if (!fromAmount.isEmpty() && !toAmount.isEmpty()) {
            etFromAmount.setText(toAmount);
            performConversion();
        }
    }

    private void updateConversionResult(double result, double rate, String from, String to, boolean isCached) {
        runOnUiThread(() -> {
            etToAmount.setText(String.format(Locale.getDefault(), "%.2f", result));
            String rateText = String.format("1 %s = %.4f %s", from, rate, to);
            if (isCached) {
                rateText += " (Cached)";
            }
            tvConversionRate.setText(rateText);
            updateLastUpdatedTime();
            saveLastRate(from, to, rate);
        });
    }

    private void saveLastRate(String from, String to, double rate) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat(from + "_" + to, (float) rate);
        editor.putFloat(to + "_" + from, (float) (1/rate));
        editor.putLong("lastUpdatedTime", System.currentTimeMillis());
        editor.putString("lastUpdated", new SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date()));
        editor.apply();
    }

    private void loadCachedRates() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastUpdateTime = prefs.getLong("lastUpdatedTime", 0);

        if (System.currentTimeMillis() - lastUpdateTime < CACHE_EXPIRY_MS) {
            for (String from : currencies) {
                for (String to : currencies) {
                    if (!from.equals(to)) {
                        float rate = prefs.getFloat(from + "_" + to, -1f);
                        if (rate != -1f) {
                            cachedRates.put(from + "_" + to, (double) rate);
                        }
                    }
                }
            }
        }
    }

    private void useOfflineRate(String from, String to, double amount) {
        double savedRate = getSavedRate(from, to);
        runOnUiThread(() -> {
            if (savedRate != -1f) {
                double result = amount * savedRate;
                etToAmount.setText(String.format(Locale.getDefault(), "%.2f", result));
                tvConversionRate.setText(String.format("1 %s = %.4f %s (Offline)", from, savedRate, to));
                tvLastUpdated.setText("Last known: " + getSavedTimestamp());
            } else {
                tvConversionRate.setText("No rate available");
                etToAmount.setText("");
                Toast.makeText(this, "No internet and no saved rate found", Toast.LENGTH_LONG).show();
            }
        });
    }

    private double getSavedRate(String from, String to) {
        String cacheKey = from + "_" + to;
        if (cachedRates.containsKey(cacheKey)) {
            return cachedRates.get(cacheKey);
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getFloat(cacheKey, -1f);
    }

    private String getSavedTimestamp() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString("lastUpdated", "Unknown");
    }

    private void updateLastUpdatedTime() {
        String time = new SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(new Date());
        tvLastUpdated.setText("Rates updated: " + time);
    }

    private void checkNetworkStatus() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isNetworkAvailable = activeNetwork != null && activeNetwork.isConnected();
        updateNetworkStatus();
    }

    private void updateNetworkStatus() {
        runOnUiThread(() -> {
            if (isNetworkAvailable) {
                tvNetworkStatus.setText("Online");
                tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvNetworkStatus.setText("Offline");
                tvNetworkStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        });
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
            btnConvert.setEnabled(!show);
        });
    }

    private int findCurrencyIndex(List<CurrencyItem> list, String code) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getCode().equals(code)) return i;
        }
        return 0;
    }
}