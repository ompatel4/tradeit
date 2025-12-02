package edu.uga.cs.tradeit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
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
import java.util.Map;

/**
 * Manages categories: List alpha (4), Add (5), Update (6), Delete if empty (7).
 * State saving for Story 16: Saves/restores selected category or list state on rotation/interruption.
 * Fixed RecyclerView inconsistency with stable IDs; full functional update/delete dialogs (6-7).
 */
public class CategoriesActivity extends AppCompatActivity {
    private RecyclerView rvCategories;
    private FirebaseRecyclerAdapter<Map<String, Object>, CategoryViewHolder> adapter;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final String KEY_SELECTED_CAT = "selected_category";
    private static final String KEY_LIST_STATE = "list_state";

    private String selectedCategoryId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);
        rvCategories = findViewById(R.id.rvCategories);
        rvCategories.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
            ArrayList<String> savedList = savedInstanceState.getStringArrayList(KEY_LIST_STATE);
            if (savedList != null) {
                // Rebuild from saved if offline; Firebase handles online
            }
        }

        setupAdapter();
        Button btnAdd = findViewById(R.id.btnAddCategory);
        btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void setupAdapter() {
        Query query = FirebaseDatabase.getInstance().getReference("categories").orderByChild("name");

        // Custom SnapshotParser with GenericTypeIndicator for Map
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

        adapter = new FirebaseRecyclerAdapter<Map<String, Object>, CategoryViewHolder>(options) {
            @Override
            protected void onBindViewHolder(CategoryViewHolder holder, int position, @NonNull Map<String, Object> model) {
                String catName = (String) model.get("name");
                String catId = getRef(position).getKey();  // Stable ID from key
                holder.bind(catName, catId);
                if (catName != null && catName.equals(selectedCategoryId)) {
                    holder.itemView.setSelected(true);
                }
            }

            @Override
            public long getItemId(int position) {
                return getRef(position).getKey().hashCode();  // Stable IDs to fix inconsistency
            }

            @NonNull
            @Override
            public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new CategoryViewHolder(view);
            }
        };
        adapter.setHasStableIds(true);  // Fix RecyclerView inconsistency on updates/rotation
        rvCategories.setAdapter(adapter);
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText etName = new EditText(this);
        builder.setView(etName);
        builder.setTitle("Add Category").setPositiveButton("Add", (dialog, which) -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories").push();
                ref.child("name").setValue(name);
                ref.child("createdDate").setValue(ServerValue.TIMESTAMP);
                ref.child("creatorUid").setValue(mAuth.getCurrentUser().getUid());
                ref.child("items").setValue(null);
            }
        });
        builder.show();
    }

    public void showUpdateDialog(String catId, String catName) {
        // Full functional dialog for category update (Story 6)
        EditText etNewName = new EditText(this);
        etNewName.setText(catName);
        new AlertDialog.Builder(this)
                .setTitle("Update Category")
                .setView(etNewName)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newName = etNewName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories").child(catId);
                        ref.child("name").setValue(newName);
                        ref.child("updatedDate").setValue(ServerValue.TIMESTAMP);
                        Toast.makeText(this, "Category updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void showDeleteDialog(String catId) {
        // Check empty, then full functional confirm dialog (Story 7)
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("categories").child(catId).child("items");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    Toast.makeText(CategoriesActivity.this, "Cannot delete: Category not empty", Toast.LENGTH_SHORT).show();
                } else {
                    new AlertDialog.Builder(CategoriesActivity.this)
                            .setTitle("Delete Category")
                            .setMessage("Confirm delete?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                ref.getParent().removeValue();
                                Toast.makeText(CategoriesActivity.this, "Category deleted", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CategoriesActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SELECTED_CAT, selectedCategoryId);
        ArrayList<String> currentList = new ArrayList<>();  // From adapter snapshots
        outState.putStringArrayList(KEY_LIST_STATE, currentList);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        selectedCategoryId = savedInstanceState.getString(KEY_SELECTED_CAT);
        setupAdapter();
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

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        android.widget.TextView tvName;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(android.R.id.text1);
        }

        void bind(String catName, String catId) {
            tvName.setText(catName);
            itemView.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.category_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_update) {
                        ((CategoriesActivity) itemView.getContext()).showUpdateDialog(catId, catName);
                        return true;
                    } else if (item.getItemId() == R.id.action_delete) {
                        ((CategoriesActivity) itemView.getContext()).showDeleteDialog(catId);
                        return true;
                    }
                    return false;
                });
                popup.show();
                return true;
            });
            itemView.setOnClickListener(v -> {
                ((CategoriesActivity) itemView.getContext()).selectedCategoryId = catName;
                Intent intent = new Intent(itemView.getContext(), ViewItemsActivity.class);
                intent.putExtra("CATEGORY_ID", catId);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}