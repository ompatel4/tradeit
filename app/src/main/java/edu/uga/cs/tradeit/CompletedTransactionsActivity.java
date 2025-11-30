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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import java.util.Map;

/**
 * Views completed transactions desc by date (15).
 * State saving for Story 16: Saves/restores selected trans ID on rotation/interruption.
 * Firebase startListening() in onStart() ensures data reloads post-interruption.
 */
public class CompletedTransactionsActivity extends AppCompatActivity {
    private RecyclerView rvCompleted;
    private FirebaseRecyclerAdapter<Map<String, Object>, CompletedViewHolder> adapter;
    private static final String KEY_SELECTED_TRANS = "selected_trans_id";

    private String selectedTransId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed);
        rvCompleted = findViewById(R.id.rvCompleted);
        rvCompleted.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            selectedTransId = savedInstanceState.getString(KEY_SELECTED_TRANS);
        }

        Query query = FirebaseDatabase.getInstance().getReference("transactions/completed")
                .orderByChild("completionDate");

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

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, CompletedViewHolder>(options) {
            @Override
            protected void onBindViewHolder(CompletedViewHolder holder, int position, @NonNull Map<String, Object> model) {
                String transId = getRef(position).getKey();
                holder.bind(model, transId);
                if (transId.equals(selectedTransId)) {
                    holder.itemView.setSelected(true);
                }
            }

            @NonNull
            @Override
            public CompletedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_item, parent, false);
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
        adapter.startListening();  // Reloads data post-interruption
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

    // CompletedViewHolder (reuse Pending, hide confirm)
    static class CompletedViewHolder extends RecyclerView.ViewHolder {
        TextView tvItem, tvRole, tvDate;
        Button btnConfirm;  // Hidden

        CompletedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvItem = itemView.findViewById(R.id.tvItemName);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnConfirm.setVisibility(View.GONE);  // Always hidden
        }

        void bind(Map<String, Object> data, String transId) {
            tvItem.setText((String) data.get("itemName"));
            // Role from data or query
            tvRole.setText("Role: Completed");
            tvDate.setText(data.get("completionDate").toString());

            itemView.setOnClickListener(v -> ((CompletedTransactionsActivity) itemView.getContext()).selectedTransId = transId);
        }
    }
}