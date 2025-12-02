package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;

import java.util.Map;

/**
 * Views completed transactions in date order (Story 15).
 * Each user only sees their own completed transactions:
 *   where buyerUid == currentUser or sellerUid == currentUser.
 * Keeps selected transaction ID across rotation (Story 16).
 */
public class CompletedTransactionsActivity extends AppCompatActivity {

    private RecyclerView rvCompleted;
    private FirebaseRecyclerAdapter<Map<String, Object>, CompletedViewHolder> adapter;

    private static final String KEY_SELECTED_TRANS = "selected_trans_id";

    private String selectedTransId;
    private String currentUid;  // 👈 logged-in user id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);

        rvCompleted = findViewById(R.id.rvCompleted);
        rvCompleted.setLayoutManager(new LinearLayoutManager(this));

        // Get current user uid
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // no user -> nothing to show
            finish();
            return;
        }
        currentUid = user.getUid();

        if (savedInstanceState != null) {
            selectedTransId = savedInstanceState.getString(KEY_SELECTED_TRANS);
        }

        Query query = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child("completed")
                .orderByChild("completionDate");

        // Custom SnapshotParser with GenericTypeIndicator
        SnapshotParser<Map<String, Object>> parser = new SnapshotParser<Map<String, Object>>() {
            @NonNull
            @Override
            public Map<String, Object> parseSnapshot(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<Map<String, Object>> indicator =
                        new GenericTypeIndicator<Map<String, Object>>() {};
                return snapshot.getValue(indicator);
            }
        };

        FirebaseRecyclerOptions<Map<String, Object>> options =
                new FirebaseRecyclerOptions.Builder<Map<String, Object>>()
                        .setQuery(query, parser)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, CompletedViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CompletedViewHolder holder,
                                            int position,
                                            @NonNull Map<String, Object> model) {

                String transId = getRef(position).getKey();

                // Read buyer/seller from the model
                String buyerUid = (String) model.get("buyerUid");
                String sellerUid = (String) model.get("sellerUid");

                // 👇 Only show this item if current user is buyer OR seller
                boolean isMine = (buyerUid != null && buyerUid.equals(currentUid))
                        || (sellerUid != null && sellerUid.equals(currentUid));

                if (!isMine) {
                    // Hide this row completely for other users' transactions
                    holder.itemView.setVisibility(View.GONE);
                    ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                    if (params != null) {
                        params.height = 0;
                        holder.itemView.setLayoutParams(params);
                    }
                    return;
                } else {
                    // Make sure visible for my transactions
                    holder.itemView.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
                    if (params != null && params.height == 0) {
                        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                        holder.itemView.setLayoutParams(params);
                    }
                }

                holder.bind(model, transId);

                if (transId != null && transId.equals(selectedTransId)) {
                    holder.itemView.setSelected(true);
                } else {
                    holder.itemView.setSelected(false);
                }
            }

            @NonNull
            @Override
            public CompletedViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
                // Reuse pending_item layout, but hide Confirm button
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.pending_item, parent, false);
                return new CompletedViewHolder(view);
            }
        };

        rvCompleted.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_TRANS, selectedTransId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedTransId = savedInstanceState.getString(KEY_SELECTED_TRANS);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    // ViewHolder for completed transactions
    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem, tvRole, tvDate;
        Button btnConfirm;  // hidden for completed

        CompletedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tvItemName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnConfirm.setVisibility(View.GONE); // no confirm for completed
        }

        void bind(Map<String, Object> data, String transId) {
            String itemName = (String) data.get("itemName");
            Object completionDateObj = data.get("completionDate");

            tvItem.setText(itemName != null ? itemName : "(no name)");
            tvRole.setText("Transaction: Completed");
            tvDate.setText(completionDateObj != null
                    ? completionDateObj.toString()
                    : "");

            itemView.setOnClickListener(v -> {
                if (itemView.getContext() instanceof CompletedTransactionsActivity) {
                    ((CompletedTransactionsActivity) itemView.getContext()).selectedTransId = transId;
                }
            });
        }
    }
}
