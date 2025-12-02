package edu.uga.cs.tradeit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class PendingTransactionsAdapter
        extends RecyclerView.Adapter<PendingTransactionsAdapter.ViewHolder> {

    private List<PendingTransaction> transactions;
    private String currentUid;

    public PendingTransactionsAdapter(List<PendingTransaction> transactions) {
        this.transactions = transactions;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUid = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingTransaction tx = transactions.get(position);

        holder.textViewItemName.setText(tx.getItemName());

        // Role = are you buyer or seller?
        String role;
        if (currentUid != null && currentUid.equals(tx.getBuyerUid())) {
            role = "Role: Buyer";
        } else if (currentUid != null && currentUid.equals(tx.getSellerUid())) {
            role = "Role: Seller";
        } else {
            role = "Role: N/A";
        }
        holder.textViewRole.setText(role);

        holder.textViewCategory.setText("Category: " + tx.getCatId());

        String price = tx.getPrice();
        if (price == null || price.trim().isEmpty()) {
            price = "free";
        }
        holder.textViewPrice.setText("Price: " + price);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textViewItemName;
        TextView textViewRole;
        TextView textViewCategory;
        TextView textViewPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItemName = itemView.findViewById(R.id.textViewPendingItemName);
            textViewRole = itemView.findViewById(R.id.textViewPendingRole);
            textViewCategory = itemView.findViewById(R.id.textViewPendingCategory);
            textViewPrice = itemView.findViewById(R.id.textViewPendingPrice);
        }
    }
}
