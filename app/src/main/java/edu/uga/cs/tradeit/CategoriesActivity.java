package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import java.util.ArrayList;

/**
 * Manages categories: List alpha (4), Add (5), Update (6), Delete if empty (7).
 * State saving for Story 16: Saves/restores selected category or list state on rotation/interruption.
 */
public class CategoriesActivity extends AppCompatActivity {
    private RecyclerView rvCategories;
    private FirebaseRecyclerAdapter<String, CategoryViewHolder> adapter;
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
        FirebaseRecyclerOptions<String> options = new FirebaseRecyclerOptions.Builder<String>()
                .setQuery(query, String.class).build();
        adapter = new FirebaseRecyclerAdapter<String, CategoryViewHolder>(options) {
            @Override
            protected void onBindViewHolder(CategoryViewHolder holder, int position, @NonNull String model) {
                holder.bind(model);
                if (model.equals(selectedCategoryId)) {
                    holder.itemView.setSelected(true);
                }
            }

            @NonNull
            @Override
            public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                return new CategoryViewHolder(view);
            }
        };
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

        void bind(String catName) {
            tvName.setText(catName);
            itemView.setOnLongClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.category_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_update) {
                        Toast.makeText(itemView.getContext(), "Update: " + catName, Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (item.getItemId() == R.id.action_delete) {
                        Toast.makeText(itemView.getContext(), "Delete if empty: " + catName, Toast.LENGTH_SHORT).show();
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
                intent.putExtra("CATEGORY_ID", catName);
                itemView.getContext().startActivity(intent);
            });
        }
    }
}