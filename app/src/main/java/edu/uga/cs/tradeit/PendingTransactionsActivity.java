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

public class PendingTransactionsActivity extends AppCompatActivity {

    private static final String TAG = "PendingTransactionsAct";

    private RecyclerView recyclerView;
    private PendingTransactionsAdapter adapter;
    private List<PendingTransaction> pendingList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference pendingRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_transactions);

        recyclerView = findViewById(R.id.recyclerViewPending);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PendingTransactionsAdapter(pendingList);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        pendingRef = FirebaseDatabase.getInstance()
                .getReference("transactions")
                .child("pending");

        loadPendingTransactions();
    }

    private void loadPendingTransactions() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this,
                    "You must be logged in to view pending transactions",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();

        pendingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pendingList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    PendingTransaction tx = child.getValue(PendingTransaction.class);
                    if (tx != null) {
                        tx.setId(child.getKey());

                        // include if I'm buyer OR seller
                        if (uid.equals(tx.getBuyerUid()) || uid.equals(tx.getSellerUid())) {
                            pendingList.add(tx);
                        }
                    }
                }

                // sort by postedDate: newest → oldest
                Collections.sort(pendingList, new Comparator<PendingTransaction>() {
                    @Override
                    public int compare(PendingTransaction o1, PendingTransaction o2) {
                        return Long.compare(o2.getPostedDate(), o1.getPostedDate());
                    }
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadPendingTransactions:onCancelled", error.toException());
                Toast.makeText(PendingTransactionsActivity.this,
                        "Failed to load: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
