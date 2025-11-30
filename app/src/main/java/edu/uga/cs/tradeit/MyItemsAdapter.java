package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MyItemsAdapter extends RecyclerView.Adapter<MyItemsAdapter.ViewHolder> {

    private List<TradeItem> items;

    public MyItemsAdapter(List<TradeItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TradeItem item = items.get(position);

        // Name
        holder.textViewName.setText(item.getName());

        // Category (using catId – later you can map this to a human-readable name)
        holder.textViewCategory.setText("Category: " + item.getCatId());

        // Price is stored as a String ("free" or "34.99")
        String price = item.getPrice();
        if (price == null || price.trim().isEmpty() || price.equalsIgnoreCase("free")) {
            holder.textViewPrice.setText("Free");
        } else {
            holder.textViewPrice.setText("$" + price);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewCategory;
        TextView textViewPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewItemName);
            textViewCategory = itemView.findViewById(R.id.textViewItemCategory);
            textViewPrice = itemView.findViewById(R.id.textViewItemPrice);
        }
    }
}
