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
 * CategoriesActivity manages the list of trade categories which enables users to view, add, update and delete categories.
 * Only owners of a category can edit them and enables navigation to item sections within categories.
 */

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private FirebaseRecyclerAdapter<Map<String, Object>, CategoryViewHolder> adapter;
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private static final String KEY_SELECTED_CAT = "selected_category";
    private static final String KEY_LIST_STATE = "list_state";

    String selectedCategoryId;

    /**
     * Initializes the activity layout, RecyclerView and event listeners
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));
        rvCategories.setItemAnimator(null);

        if (savedInstanceState != null) {
            selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
            ArrayList<String> savedList = savedInstanceState.getStringArrayList(KEY_LIST_STATE);
        }

        setupAdapter();

        findViewById(R.id.btnAddCategory).setOnClickListener(v -> showAddDialog());
    }

    /**
     * Setup for Firebase for the categories list
     * Configures RecyclerView with adapter
     */

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

    /**
     * Shows dialog for new category
     * Checks inputs and saves to Firebase
     */

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
                    ref.child("items").setValue(null);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Shows dialog for updating a category
     * Checks input and updates Firebase
     */

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

    /**
     * Checks if category is empty and shows confirmation dialog for deleting a category
     * Deletes from Firebase once confirmed
     */

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

    /**
     * Saves category ID for state
     */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_CAT, selectedCategoryId);
        outState.putStringArrayList(KEY_LIST_STATE, new ArrayList<>());
    }

    /**
     * Restores category ID from bundle
     */

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
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
     * Stops adapter listener
     */

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    /**
     * ViewHolder binds category data to RecyclerView item
     * Handles click for nav and edit/delete button
     */

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnEdit, btnDelete;

        String catId;
        String creatorUid;
        String catName;

        /**
         * Constructor inits view from item layout
         */
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.textViewCategoryName);
            btnEdit = itemView.findViewById(R.id.buttonEditCategory);
            btnDelete = itemView.findViewById(R.id.buttonDeleteCategory);
        }

        /**
         * Binds category data to views and sets up click listeners
         * Shows edit/delete button for owner
         */

        void bind(String catName, String catId, String creatorUid) {
            this.catName = catName;
            this.catId = catId;
            this.creatorUid = creatorUid;

            tvName.setText(catName != null ? catName : "(no name)");

            itemView.setOnClickListener(v -> {
                CategoriesActivity activity = (CategoriesActivity) itemView.getContext();
                activity.selectedCategoryId = catId;

                Intent intent = new Intent(itemView.getContext(), ViewItemsActivity.class);
                intent.putExtra("CATEGORY_ID", catId);
                itemView.getContext().startActivity(intent);
            });

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
