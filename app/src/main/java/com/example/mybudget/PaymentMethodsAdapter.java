package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PaymentMethodsAdapter extends RecyclerView.Adapter<PaymentMethodsAdapter.ViewHolder> {

    private List<String> paymentMethods;

    public PaymentMethodsAdapter(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String paymentMethod = paymentMethods.get(position);
        holder.tvPaymentMethod.setText(paymentMethod);
    }

    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPaymentMethod;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPaymentMethod = itemView.findViewById(R.id.tv_payment_method);
        }
    }
}

