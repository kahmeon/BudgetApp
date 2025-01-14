package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AccountsAdapter extends RecyclerView.Adapter<AccountsAdapter.AccountViewHolder> {

    private List<AccountModel> accountsList;

    public AccountsAdapter(List<AccountModel> accountsList) {
        this.accountsList = accountsList;
    }

    @NonNull
    @Override
    public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account, parent, false);
        return new AccountViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AccountViewHolder holder, int position) {
        AccountModel account = accountsList.get(position);
        holder.tvAccountName.setText(account.getAccountName());
        holder.tvAccountBalance.setText(String.format("Balance: RM %.2f", account.getAccountBalance()));
    }

    @Override
    public int getItemCount() {
        return accountsList.size();
    }

    static class AccountViewHolder extends RecyclerView.ViewHolder {
        TextView tvAccountName, tvAccountBalance;

        public AccountViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAccountName = itemView.findViewById(R.id.tv_account_name);
            tvAccountBalance = itemView.findViewById(R.id.tv_account_balance);
        }
    }
}
