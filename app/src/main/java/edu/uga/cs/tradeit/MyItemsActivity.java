package edu.uga.cs.tradeit;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import java.util.Map;

/**
 * Views user's posted items (supports update/delete from ViewItems).
 */
public class MyItemsActivity extends AppCompatActivity {
    private RecyclerView rvMyItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ViewItemsActivity.ItemViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);
        rvMyItems = findViewById(R.id.rvMyItems);
        rvMyItems.setLayoutManager(new LinearLayoutManager(this));

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Simplified query; for full, denormalize or loop categories
        Query query = FirebaseDatabase.getInstance().getReference("categories").child("items").orderByChild("posterUid").equalTo(uid);

        // Adapter similar to ViewItems, with update/delete visible
        // (Reuse ViewItemsActivity.ItemViewHolder)
    }

    @Override
    protected void onStart() {
        super.onStart();
        // adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // adapter.stopListening();
    }
}