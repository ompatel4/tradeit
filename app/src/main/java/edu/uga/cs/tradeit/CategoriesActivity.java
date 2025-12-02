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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Categories:
 *  - View list alphabetically (4)
 *  - Add category (5)
 *  - Update own category (6)
 *  - Delete own category if empty (7)
 *  - Click category -> view items (11)
 *  - Saves basic state on rotation (16)
 */
public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private FirebaseRecyclerAdapter<Map<String, Object>, CategoryViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private static final String KEY_SELECTED_CAT = "selected_category";
    private static final String KEY_LIST_STATE = "list_state";

    String selectedCategoryId;   // so ViewHolder can update it

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
            ArrayList<String> savedList = savedInstanceState.getStringArrayList(KEY_LIST_STATE);
            // Firebase will reload live data; savedList is not strictly needed here
        }

        setupAdapter();

        findViewById(R.id.btnAddCategory).setOnClickListener(v -> showAddDialog());
    }

    private void setupAdapter() {
        Query query = FirebaseDatabase.getInstance()
                .getReference("categories")
                .orderByChild("name");

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

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, CategoryViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CategoryViewHolder holder,
                                            int position,
                                            @NonNull Map<String, Object> model) {
                String catName = (String) model.get("name");
                String catId = getRef(position).getKey();
                String creatorUid = (String) model.get("creatorUid");

                holder.bind(catName, catId, creatorUid);

                // simple selection highlight if you want
                if (catId != null && catId.equals(selectedCategoryId)) {
                    holder.itemView.setSelected(true);
                } else {
                    holder.itemView.setSelected(false);
                }
            }

            @NonNull
            @Override
            public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_category, parent, false);
                return new CategoryViewHolder(view);
            }
        };

        rvCategories.setAdapter(adapter);
    }

    private void showAddDialog() {
        EditText etName = new EditText(this);
        etName.setHint("Category name");

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(etName)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("categories")
                            .push();

                    ref.child("name").setValue(name);
                    ref.child("createdDate").setValue(ServerValue.TIMESTAMP);
                    ref.child("creatorUid").setValue(mAuth.getCurrentUser().getUid());
                    // optional: init items node
                    ref.child("items").setValue(null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Update category name (owner only)
    public void showUpdateDialog(String catId, String oldName) {
        EditText etNewName = new EditText(this);
        etNewName.setText(oldName);

        new AlertDialog.Builder(this)
                .setTitle("Update Category")
                .setView(etNewName)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = etNewName.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    DatabaseReference ref = FirebaseDatabase.getInstance()
                            .getReference("categories")
                            .child(catId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("name", newName);
                    updates.put("updatedDate", ServerValue.TIMESTAMP);
                    ref.updateChildren(updates);

                    Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Delete allowed only if category has no items (owner only)
    public void showDeleteDialog(String catId) {
        DatabaseReference itemsRef = FirebaseDatabase.getInstance()
                .getReference("categories")
                .child(catId)
                .child("items");

        itemsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Toast.makeText(CategoriesActivity.this,
                            "Cannot delete: Category not empty",
                            Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(CategoriesActivity.this)
                            .setTitle("Delete Category")
                            .setMessage("Delete this category?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                itemsRef.getParent().removeValue();
                                Toast.makeText(CategoriesActivity.this,
                                        "Category deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoriesActivity.this,
                        "Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_CAT, selectedCategoryId);
        outState.putStringArrayList(KEY_LIST_STATE, new ArrayList<>());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
        // adapter will rebind and apply selection
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

    // ------------------------
    // ViewHolder for categories
    // ------------------------
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnEdit, btnDelete;

        String catId;
        String creatorUid;
        String catName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textViewCategoryName);
            btnEdit = itemView.findViewById(R.id.buttonEditCategory);
            btnDelete = itemView.findViewById(R.id.buttonDeleteCategory);
        }

        void bind(String catName, String catId, String creatorUid) {
            this.catName = catName;
            this.catId = catId;
            this.creatorUid = creatorUid;

            tvName.setText(catName != null ? catName : "(no name)");

            // Tap row -> open items in that category
            itemView.setOnClickListener(v -> {
                CategoriesActivity activity = (CategoriesActivity) itemView.getContext();
                activity.selectedCategoryId = catId;

                Intent intent = new Intent(itemView.getContext(), ViewItemsActivity.class);
                intent.putExtra("CATEGORY_ID", catId);
                itemView.getContext().startActivity(intent);
            });

            // Only category owner sees Edit/Delete
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            boolean isOwner = creatorUid != null && creatorUid.equals(currentUid);

            if (isOwner) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnEdit.setOnClickListener(v -> {
                    CategoriesActivity activity = (CategoriesActivity) itemView.getContext();
                    activity.showUpdateDialog(catId, catName);
                });

                btnDelete.setOnClickListener(v -> {
                    CategoriesActivity activity = (CategoriesActivity) itemView.getContext();
                    activity.showDeleteDialog(catId);
                });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                btnEdit.setOnClickListener(null);
                btnDelete.setOnClickListener(null);
            }
        }
    }
}
