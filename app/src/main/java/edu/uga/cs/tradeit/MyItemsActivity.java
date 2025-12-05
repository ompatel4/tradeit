package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;

import java.util.Map;

/**
 * MyItemsActivity displays user's posted items
 * Allows for update and delete actions
 */

public class MyItemsActivity extends AppCompatActivity {

    private RecyclerView rvMyItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    /**
     * Inits activity layout and RecyclerView
     * Sets up Firebase query for user's items
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        rvMyItems = findViewById(R.id.recyclerViewMyItems);
        rvMyItems.setLayoutManager(new LinearLayoutManager(this));

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        Query query = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("items")
                .orderByChild("postedDate");

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

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ViewItemsActivity.ItemViewHolder holder,
                                            int position,
                                            @NonNull Map<String, Object> model) {

                String itemId = getRef(position).getKey();

                String catId = null;
                if (model.get("catId") != null) {
                    catId = model.get("catId").toString();
                }

                holder.bind(model, catId, itemId);
            }

            @NonNull
            @Override
            public ViewItemsActivity.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                                       int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_layout, parent, false);
                return new ViewItemsActivity.ItemViewHolder(view);
            }
        };

        rvMyItems.setAdapter(adapter);
    }

    /**
     * Starts adapter listener
     */

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    /**
     * Stops adapter listener to avoid mem leaks
     */

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }
}
