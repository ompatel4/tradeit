package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * View items in a category, post new item, buy/update/delete, etc.
 * This version is conservative with RecyclerView + Firebase to avoid
 * "Inconsistency detected" crashes, especially on back navigation.
 */
public class ViewItemsActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ItemViewHolder> adapter;
    private String catId;

    private static final String KEY_CAT_ID = "category_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        // Restore category id after rotation, otherwise read from Intent
        if (savedInstanceState != null) {
            catId = savedInstanceState.getString(KEY_CAT_ID);
        } else {
            catId = getIntent().getStringExtra("CATEGORY_ID");
        }

        if (catId == null) {
            Toast.makeText(this, "Error: No category selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvItems = findViewById(R.id.rvItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        // Disable default item animations – they often cause issues
        rvItems.setItemAnimator(null);

        FloatingActionButton fabPostItem = findViewById(R.id.fabPostItem);
        fabPostItem.setOnClickListener(v -> {
            android.content.Intent intent =
                    new android.content.Intent(ViewItemsActivity.this, PostItemActivity.class);
            intent.putExtra("CATEGORY_ID", catId);
            startActivity(intent);
        });

        setupAdapter();
    }

    private void setupAdapter() {
        Query query = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child(catId)
                .child("items")
                .orderByChild("postedDate");

        SnapshotParser<Map<String, Object>> parser = new SnapshotParser<Map<String, Object>>() {
            @NonNull
            @Override
            public Map<String, Object> parseSnapshot(@NonNull DataSnapshot snapshot) {
                GenericTypeIndicator<Map<String, Object>> indicator =
                        new GenericTypeIndicator<Map<String, Object>>() {};
                Map<String, Object> value = snapshot.getValue(indicator);
                return value != null ? value : new HashMap<>();
            }
        };

        FirebaseRecyclerOptions<Map<String, Object>> options =
                new FirebaseRecyclerOptions.Builder<Map<String, Object>>()
                        .setQuery(query, parser)
                        .build();

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, ItemViewHolder>(options) {

            @Override
            protected void onBindViewHolder(@NonNull ItemViewHolder holder,
                                            int position,
                                            @NonNull Map<String, Object> model) {
                String itemId = getRef(position).getKey();
                holder.bind(model, catId, itemId);
            }

            @NonNull
            @Override
            public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_layout, parent, false);
                return new ItemViewHolder(view);
            }

            // NOTE: no getItemId override, no stable IDs for items
        };

        rvItems.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CAT_ID, catId);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Last line of defense: fully detach adapter when Activity is destroyed
        if (rvItems != null) {
            rvItems.setAdapter(null);
        }
    }

    // The key bit for your crash: make back explicitly detach the adapter
    @Override
    public void onBackPressed() {
        if (adapter != null) {
            adapter.stopListening();
            rvItems.setAdapter(null);
            adapter = null;
        }
        super.onBackPressed();
    }

    // -----------------------
    // ViewHolder for each item
    // -----------------------
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvDate;
        Button btnBuy, btnUpdate, btnDelete;

        Map<String, Object> itemData;
        String itemId;
        String catId;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnBuy = itemView.findViewById(R.id.btnBuy);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        void bind(Map<String, Object> data, String catId, String itemId) {
            this.itemData = data;
            this.catId = catId;
            this.itemId = itemId;

            String name = (String) data.get("name");
            tvName.setText(name != null ? name : "(no name)");

            String price = (String) data.get("price");
            if (price == null || price.isEmpty()) {
                price = "free";
            }
            tvPrice.setText(price);

            Object tsObj = data.get("postedDate");
            if (tsObj instanceof Long) {
                Date d = new Date((Long) tsObj);
                tvDate.setText(DateFormat.getDateTimeInstance().format(d));
            } else {
                tvDate.setText("n/a");
            }

            String posterUid = (String) data.get("posterUid");
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            String currentUid = mAuth.getCurrentUser() != null
                    ? mAuth.getCurrentUser().getUid()
                    : null;
            boolean isOwn = (posterUid != null && posterUid.equals(currentUid));

            btnUpdate.setVisibility(isOwn ? View.VISIBLE : View.GONE);
            btnDelete.setVisibility(isOwn ? View.VISIBLE : View.GONE);
            btnBuy.setVisibility(isOwn ? View.GONE : View.VISIBLE);

            btnBuy.setOnClickListener(v -> createPendingTransaction());
            btnUpdate.setOnClickListener(v -> showUpdateDialog());
            btnDelete.setOnClickListener(v -> showDeleteDialog());
        }

        private void createPendingTransaction() {
            String buyerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String sellerUid = (String) itemData.get("posterUid");

            DatabaseReference transRef = FirebaseDatabase.getInstance()
                    .getReference("transactions")
                    .child("pending")
                    .push();

            Map<String, Object> trans = new HashMap<>();
            trans.put("buyerUid", buyerUid);
            trans.put("sellerUid", sellerUid);
            trans.put("itemName", itemData.get("name"));
            trans.put("catId", catId);
            trans.put("postedDate", ServerValue.TIMESTAMP);
            trans.put("price", itemData.get("price"));

            transRef.setValue(trans);

            if (itemId != null) {
                DatabaseReference itemRef = FirebaseDatabase.getInstance()
                        .getReference("categories")
                        .child(catId)
                        .child("items")
                        .child(itemId);
                itemRef.removeValue();
            }

            Toast.makeText(itemView.getContext(), "Request placed", Toast.LENGTH_SHORT).show();
        }

        private void showUpdateDialog() {
            if (itemId == null || itemId.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Error: Invalid item ID", Toast.LENGTH_SHORT).show();
                return;
            }

            LinearLayout layout = new LinearLayout(itemView.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            int padding = (int) (16 * itemView.getResources().getDisplayMetrics().density);
            layout.setPadding(padding, padding, padding, padding);

            EditText etNewName = new EditText(itemView.getContext());
            etNewName.setHint("Item name");
            etNewName.setText((String) itemData.get("name"));
            layout.addView(etNewName);

            EditText etNewPrice = new EditText(itemView.getContext());
            etNewPrice.setHint("Price (or leave empty for free)");
            String currentPrice = (String) itemData.get("price");
            if (currentPrice != null && !"free".equalsIgnoreCase(currentPrice)) {
                etNewPrice.setText(currentPrice);
            }
            layout.addView(etNewPrice);

            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Update Item")
                    .setView(layout)
                    .setPositiveButton("Update", (dialog, which) -> {
                        String newName = etNewName.getText().toString().trim();
                        String newPriceInput = etNewPrice.getText().toString().trim();
                        String newPrice = newPriceInput.isEmpty() ? "free" : newPriceInput;

                        if (newName.isEmpty()) {
                            Toast.makeText(itemView.getContext(),
                                    "Name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("name", newName);
                        updateData.put("price", newPrice);
                        updateData.put("updatedDate", ServerValue.TIMESTAMP);

                        DatabaseReference catRef = FirebaseDatabase.getInstance()
                                .getReference("categories")
                                .child(catId)
                                .child("items")
                                .child(itemId);
                        catRef.updateChildren(updateData);

                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(uid)
                                .child("items")
                                .child(itemId);
                        userRef.updateChildren(updateData);

                        Toast.makeText(itemView.getContext(),
                                "Item updated", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showDeleteDialog() {
            if (itemId == null || itemId.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Error: Invalid item ID", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Delete Item")
                    .setMessage("Delete \"" + itemData.get("name") + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DatabaseReference catRef = FirebaseDatabase.getInstance()
                                .getReference("categories")
                                .child(catId)
                                .child("items")
                                .child(itemId);
                        catRef.removeValue();

                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(uid)
                                .child("items")
                                .child(itemId);
                        userRef.removeValue();

                        Toast.makeText(itemView.getContext(),
                                "Item deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}
