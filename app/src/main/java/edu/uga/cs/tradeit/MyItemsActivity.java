package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import java.util.Map;

/**
 * Views user's posted items (supports update/delete from ViewItems).
 * Queries user's items (denormalized for simplicity, Story 9-10).
 * Overloaded bind for MyItems (no catId/itemId needed).
 */
public class MyItemsActivity extends AppCompatActivity {
    private RecyclerView rvMyItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);
        rvMyItems = findViewById(R.id.rvMyItems);
        rvMyItems.setLayoutManager(new LinearLayoutManager(this));

        String uid = mAuth.getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Denormalized query: /users/{uid}/items (copy on post)
        Query query = FirebaseDatabase.getInstance().getReference("users").child(uid).child("items").orderByChild("postedDate");

        // Custom SnapshotParser with GenericTypeIndicator
        SnapshotParser<Map<String, Object>> parser = new SnapshotParser<Map<String, Object>>() {
            @NonNull
            @Override
            public Map<String, Object> parseSnapshot(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<Map<String, Object>> indicator = new GenericTypeIndicator<Map<String, Object>>() {};
                return snapshot.getValue(indicator);
            }
        };

        FirebaseRecyclerOptions.Builder<Map<String, Object>> optionsBuilder = new FirebaseRecyclerOptions.Builder<Map<String, Object>>();
        optionsBuilder.setQuery(query, parser);
        FirebaseRecyclerOptions<Map<String, Object>> options = optionsBuilder.build();

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder>(options) {
            @Override
            protected void onBindViewHolder(ViewItemsActivity.ItemViewHolder holder, int position, @NonNull Map<String, Object> model) {
                holder.bind(model, null, null);  // MyItems: no catId/itemId (overloaded bind handles nulls)
            }

            @NonNull
            @Override
            public ViewItemsActivity.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
                return new ViewItemsActivity.ItemViewHolder(view);
            }
        };
        rvMyItems.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}