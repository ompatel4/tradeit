package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts new item to category (Story 8).
 * State saving for Story 16: Saves/restores form inputs on rotation.
 */
public class PostItemActivity extends AppCompatActivity {
    private EditText etName, etPrice;
    private String catId;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final String KEY_ITEM_NAME = "item_name";
    private static final String KEY_PRICE = "price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);
        catId = getIntent().getStringExtra("CATEGORY_ID");
        etName = findViewById(R.id.etItemName);
        etPrice = findViewById(R.id.etPrice);
        Button btnPost = findViewById(R.id.btnPost);

        // Restore form data
        if (savedInstanceState != null) {
            etName.setText(savedInstanceState.getString(KEY_ITEM_NAME));
            etPrice.setText(savedInstanceState.getString(KEY_PRICE));
        }

        btnPost.setOnClickListener(v -> postItem());
    }

    private void postItem() {
        String name = etName.getText().toString().trim();
        String price = etPrice.getText().toString().trim().isEmpty()
                ? "free"
                : etPrice.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter item name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "You must be logged in to post items", Toast.LENGTH_SHORT).show();
            return;
        }

        String posterUid = mAuth.getCurrentUser().getUid();

        // 1) Reference to the root
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // 2) First write: under the category (what you already had)
        DatabaseReference catItemRef =
                rootRef.child("categories").child(catId).child("items").push();

        String key = catItemRef.getKey();
        if (key == null) {
            Toast.makeText(this, "Error generating item id", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> itemData = new HashMap<>();
        itemData.put("id", key);
        itemData.put("name", name);
        itemData.put("postedDate", ServerValue.TIMESTAMP);
        itemData.put("posterUid", posterUid);
        itemData.put("price", price);
        itemData.put("catId", catId);   // so we know what category it belongs to

        // Write under the category
        catItemRef.setValue(itemData);

        // 3) Second write: mirror into /items for "My Items" and other queries
        DatabaseReference allItemsRef = rootRef.child("items").child(key);
        allItemsRef.setValue(itemData);

        Toast.makeText(this, "Item posted", Toast.LENGTH_SHORT).show();
        finish();
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ITEM_NAME, etName.getText().toString());
        outState.putString(KEY_PRICE, etPrice.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Already restored in onCreate
    }
}