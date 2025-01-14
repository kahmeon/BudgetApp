package com.example.mybudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.data.PieEntry;
import java.util.List;

public class LegendAdapter extends RecyclerView.Adapter<LegendAdapter.LegendViewHolder> {

    private final List<PieEntry> pieEntries; // Data for the legend
    private final List<Integer> colors; // Corresponding colors for each legend item

    public LegendAdapter(List<PieEntry> pieEntries, List<Integer> colors) {
        this.pieEntries = pieEntries;
        this.colors = colors;
    }

    @NonNull
    @Override
    public LegendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the legend item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_legend, parent, false);
        return new LegendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LegendViewHolder holder, int position) {
        // Set the text and color for each legend item
        PieEntry entry = pieEntries.get(position);
        holder.legendLabel.setText(entry.getLabel());
        holder.legendColor.setBackgroundColor(colors.get(position));
    }

    @Override
    public int getItemCount() {
        // Return the number of legend items
        return pieEntries.size();
    }

    // ViewHolder class for legend items
    public static class LegendViewHolder extends RecyclerView.ViewHolder {
        TextView legendLabel;
        View legendColor;

        public LegendViewHolder(@NonNull View itemView) {
            super(itemView);
            legendLabel = itemView.findViewById(R.id.legend_label); // TextView for legend label
            legendColor = itemView.findViewById(R.id.legend_color); // View for color indicator
        }
    }
}

