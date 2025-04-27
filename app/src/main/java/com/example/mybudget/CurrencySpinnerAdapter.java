package com.example.mybudget;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class CurrencySpinnerAdapter extends ArrayAdapter<CurrencyItem> {

    public CurrencySpinnerAdapter(Context context, List<CurrencyItem> currencies) {
        super(context, 0, currencies);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createCurrencyView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createCurrencyView(position, convertView, parent);
    }

    private View createCurrencyView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.currency_spinner_item, parent, false);
        }

        CurrencyItem item = getItem(position);
        ImageView imageView = convertView.findViewById(R.id.iv_flag);
        TextView textView = convertView.findViewById(R.id.tv_currency_code);

        if (item != null) {
            imageView.setImageResource(item.getFlagResId());
            textView.setText(item.getCode());
        }

        return convertView;
    }
}

