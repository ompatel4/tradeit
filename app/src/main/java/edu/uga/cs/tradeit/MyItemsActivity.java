package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MyItemsActivity extends AppCompatActivity {

    private static final String TAG = "MyItemsActivity";

    private RecyclerView recyclerView;
    private MyItemsAdapter adapter;
    private List<TradeItem> myItems = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference itemsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        recyclerView = findViewById(R.id.recyclerViewMyItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyItemsAdapter(myItems);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        itemsRef = FirebaseDatabase.getInstance().getReference("items");

        loadMyItems();
    }

    private void loadMyItems() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to view your items",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        itemsRef.orderByChild("posterUid").equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        myItems.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            TradeItem item = child.getValue(TradeItem.class);
                            if (item != null) {
                                myItems.add(item);
                            }
                        }

                        // newest to oldest by postedDate
                        Collections.sort(myItems, new Comparator<TradeItem>() {
                            @Override
                            public int compare(TradeItem o1, TradeItem o2) {
                                return Long.compare(o2.getPostedDate(), o1.getPostedDate());
                            }
                        });

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadMyItems:onCancelled", error.toException());
                        Toast.makeText(MyItemsActivity.this,
                                "Failed to load items: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
