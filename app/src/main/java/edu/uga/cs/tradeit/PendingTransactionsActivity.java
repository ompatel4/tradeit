package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Views pending transactions ordered by date (13). Seller confirms (14).
 * State saving for Story 16: Saves/restores selected trans ID on rotation/interruption.
 * Firebase startListening() in onStart() ensures data reloads post-interruption.
 */
public class PendingTransactionsActivity extends AppCompatActivity {
    private RecyclerView rvPending;
    private FirebaseRecyclerAdapter<Map<String, Object>, PendingViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final String KEY_SELECTED_TRANS = "selected_trans_id";

    private String selectedTransId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending);
        rvPending = findViewById(R.id.rvPending);
        rvPending.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            selectedTransId = savedInstanceState.getString(KEY_SELECTED_TRANS);
        }

        String uid = mAuth.getCurrentUser().getUid();
        Query query = FirebaseDatabase.getInstance().getReference("transactions/pending")
                .orderByChild("postedDate");

        // Custom SnapshotParser for Map<String, Object>
        SnapshotParser<Map<String, Object>> parser = new SnapshotParser<Map<String, Object>>() {
            @NonNull
            @Override
            public Map<String, Object> parseSnapshot(@NonNull DataSnapshot snapshot) {
                return snapshot.getValue(Map.class);
            }
        };

        FirebaseRecyclerOptions.Builder<Map<String, Object>> optionsBuilder = new FirebaseRecyclerOptions.Builder<Map<String, Object>>();
        optionsBuilder.setQuery(query, parser);
        FirebaseRecyclerOptions<Map<String, Object>> options = optionsBuilder.build();

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, PendingViewHolder>(options) {
            @Override
            protected void onBindViewHolder(PendingViewHolder holder, int position, @NonNull Map<String, Object> model) {
                String transId = getRef(position).getKey();  // Get trans ID
                holder.bind(model, uid, transId);
                if (transId.equals(selectedTransId)) {
                    holder.itemView.setSelected(true);  // Highlight restored
                }
            }

            @NonNull
            @Override
            public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_item, parent, false);
                return new PendingViewHolder(view);
            }
        };
        rvPending.setAdapter(adapter);
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
        adapter.startListening();  // Reloads data post-interruption
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    // PendingViewHolder (updated to handle selection)
    static class PendingViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem, tvRole, tvDate;
        Button btnConfirm;

        PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tvItemName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
        }

        void bind(Map<String, Object> data, String uid, String transId) {
            tvItem.setText((String) data.get("itemName"));
            String role = ((String) data.get("buyerUid")).equals(uid) ? "Buyer" : "Seller";
            tvRole.setText("Role: " + role);
            tvDate.setText(data.get("postedDate").toString());
            btnConfirm.setVisibility(role.equals("Seller") ? View.VISIBLE : View.GONE);

            // Save selection on click
            itemView.setOnClickListener(v -> ((PendingTransactionsActivity) itemView.getContext()).selectedTransId = transId);

            btnConfirm.setOnClickListener(v -> {
                DatabaseReference pendingRef = FirebaseDatabase.getInstance().getReference("transactions/pending").child(transId);
                DatabaseReference completedRef = FirebaseDatabase.getInstance().getReference("transactions/completed").child(transId);
                pendingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> completed = new HashMap<>((Map) snapshot.getValue());
                        completed.put("completionDate", ServerValue.TIMESTAMP);
                        completedRef.setValue(completed);
                        pendingRef.removeValue();
                        Toast.makeText(itemView.getContext(), "Confirmed", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(itemView.getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }
}