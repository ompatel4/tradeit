package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Views items in category desc by date (11). Buy creates pending (12). Update/delete own (9,10).
 * FAB for posting new item (8). State saving for Story 16: Saves/restores catId on rotation.
 * Same implementation as Categories: Key from getRef in bind, stored in holder, used in dialogs (no "invalid ID").
 */
public class ViewItemsActivity extends AppCompatActivity {
    private RecyclerView rvItems;
    private FirebaseRecyclerAdapter<Map<String, Object>, ItemViewHolder> adapter;
    private String catId;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final String KEY_CAT_ID = "category_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_items);

        if (savedInstanceState != null) {
            catId = savedInstanceState.getString(KEY_CAT_ID);
        } else {
            catId = getIntent().getStringExtra("CATEGORY_ID");
        }

        rvItems = findViewById(R.id.rvItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));

        // FAB for posting new item (Story 8)
        FloatingActionButton fabPostItem = findViewById(R.id.fabPostItem);
        fabPostItem.setOnClickListener(v -> {
            Intent intent = new Intent(ViewItemsActivity.this, PostItemActivity.class);
            intent.putExtra("CATEGORY_ID", catId);
            startActivity(intent);
        });

        Query query = FirebaseDatabase.getInstance().getReference("categories").child(catId).child("items")
                .orderByChild("postedDate");

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

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, ItemViewHolder>(options) {
            @Override
            protected void onBindViewHolder(ItemViewHolder holder, int position, @NonNull Map<String, Object> model) {
                String itemId = getRef(position).getKey();  // Key from getRef (like Categories)
                holder.bind(model, catId, itemId);  // Pass to holder (like Categories)
            }

            @NonNull
            @Override
            public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
                return new ItemViewHolder(view);
            }
        };
        rvItems.setAdapter(adapter);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CAT_ID, catId);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        catId = savedInstanceState.getString(KEY_CAT_ID);
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

    // ItemViewHolder with full functional update/delete dialogs (Stories 9-10)
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvDate;
        Button btnBuy, btnUpdate, btnDelete;
        Map<String, Object> itemData;
        String itemId;  // Stored from bind (like Categories catId)
        String catId;  // Stored from bind

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
            itemData = data;
            this.itemId = itemId;  // Stored (like Categories)
            this.catId = catId;  // Stored (like Categories)
            tvName.setText((String) data.get("name"));
            tvPrice.setText((String) data.get("price"));
            tvDate.setText(data.get("postedDate").toString());
            String posterUid = (String) data.get("posterUid");
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            boolean isOwn = posterUid.equals(mAuth.getCurrentUser().getUid());
            btnUpdate.setVisibility(isOwn ? View.VISIBLE : View.GONE);
            btnDelete.setVisibility(isOwn ? View.VISIBLE : View.GONE);
            btnBuy.setVisibility(isOwn ? View.GONE : View.VISIBLE);

            btnBuy.setOnClickListener(v -> createPendingTransaction(this.catId));  // Use stored
            btnUpdate.setOnClickListener(v -> showUpdateDialog(this.catId, this.itemId));  // Use stored (like Categories)
            btnDelete.setOnClickListener(v -> showDeleteDialog(this.catId, this.itemId));  // Use stored (like Categories)
        }

        private void createPendingTransaction(String catId) {
            String buyerUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String sellerUid = (String) itemData.get("posterUid");
            DatabaseReference transRef = FirebaseDatabase.getInstance().getReference("transactions").child("pending").push();
            Map<String, Object> trans = new HashMap<>();
            trans.put("buyerUid", buyerUid);
            trans.put("sellerUid", sellerUid);
            trans.put("itemName", itemData.get("name"));
            trans.put("catId", catId);
            trans.put("postedDate", ServerValue.TIMESTAMP);
            trans.put("price", itemData.get("price"));
            transRef.setValue(trans);

            DatabaseReference itemRef = FirebaseDatabase.getInstance().getReference("categories").child(catId).child("items").child(this.itemId);
            itemRef.removeValue();
            Toast.makeText(itemView.getContext(), "Request placed", Toast.LENGTH_SHORT).show();
        }

        private void showUpdateDialog(String catId, String itemId) {
            // Full functional dialog for item update (Story 9)
            if (itemId == null || itemId.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Error: Invalid item ID", Toast.LENGTH_SHORT).show();
                return;
            }
            EditText etNewName = new EditText(itemView.getContext());
            etNewName.setText((String) itemData.get("name"));
            EditText etNewPrice = new EditText(itemView.getContext());
            etNewPrice.setText((String) itemData.get("price"));
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Update Item")
                    .setView(etNewName)
                    .setView(etNewPrice)
                    .setPositiveButton("Update", (dialog, which) -> {
                        String newName = etNewName.getText().toString().trim();
                        String newPrice = etNewPrice.getText().toString().trim().isEmpty() ? "free" : etNewPrice.getText().toString();
                        if (!newName.isEmpty()) {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("name", newName);
                            updateData.put("price", newPrice);
                            updateData.put("updatedDate", ServerValue.TIMESTAMP);

                            // Update categories path
                            DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories").child(catId).child("items").child(itemId);
                            catRef.updateChildren(updateData);

                            // Sync to users path with same ID
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("items").child(itemId);
                            userRef.updateChildren(updateData);

                            Toast.makeText(itemView.getContext(), "Item updated", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

        private void showDeleteDialog(String catId, String itemId) {
            // Full functional confirm dialog for item delete (Story 10)
            if (itemId == null || itemId.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Error: Invalid item ID", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("Delete Item")
                    .setMessage("Confirm delete " + itemData.get("name") + "?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Delete categories path
                        DatabaseReference catRef = FirebaseDatabase.getInstance().getReference("categories").child(catId).child("items").child(itemId);
                        catRef.removeValue();

                        // Sync to users path with same ID
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid).child("items").child(itemId);
                        userRef.removeValue();

                        Toast.makeText(itemView.getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}