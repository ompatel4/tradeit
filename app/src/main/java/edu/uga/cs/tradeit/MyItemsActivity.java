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
 * Views user's posted items (supports update/delete from ViewItems).
 * Queries user's items (denormalized for simplicity, Story 9-10).
 */
public class MyItemsActivity extends AppCompatActivity {

    private RecyclerView rvMyItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

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

        // /users/{uid}/items ordered by postedDate
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

                // itemId under users/{uid}/items/{itemId}
                String itemId = getRef(position).getKey();

                // category id should be stored in the model as "catId"
                String catId = null;
                if (model.get("catId") != null) {
                    catId = model.get("catId").toString();
                }

                // NOW we pass real catId + itemId so update/delete know the path
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
}
