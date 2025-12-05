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
 * This is responsible for posting a new item to a category.
 * It adds each item in the database with specific IDs and names.
 *
 * The user will enter an item name and add an optional price. Then it will be posted under that
 * specific category.
 */
public class PostItemActivity extends AppCompatActivity {
    private EditText etName, etPrice;
    // This is used to add a category ID for each item.
    private String catId;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final String KEY_ITEM_NAME = "item_name";
    private static final String KEY_PRICE = "price";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_item);
        // Category ID is passed from the ViewItemsActivity
        catId = getIntent().getStringExtra("CATEGORY_ID");
        etName = findViewById(R.id.etItemName);
        etPrice = findViewById(R.id.etPrice);
        Button btnPost = findViewById(R.id.btnPost);

        // Restores any unsaved data
        if (savedInstanceState != null) {
            etName.setText(savedInstanceState.getString(KEY_ITEM_NAME));
            etPrice.setText(savedInstanceState.getString(KEY_PRICE));
        }

        // THis ensures that when the user taps post it will save it to the Firebase
        btnPost.setOnClickListener(v -> postItem());
    }


    // This is for validating the input, generating the Firebase ID, and build the item object.
    private void postItem() {
        // This collects the users input
        String name = etName.getText().toString().trim();
        String price = etPrice.getText().toString().trim().isEmpty()
                ? "free"
                : etPrice.getText().toString().trim();

        // This is a check to ensure that the item name is actually inputted
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter item name", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference rootRef = db.getReference();

        // Write the data for Firebase to be called later
        DatabaseReference catRef = rootRef.child("categories")
                .child(catId)
                .child("items");

        String itemId = catRef.push().getKey();
        if (itemId == null) {
            Toast.makeText(this, "Error generating ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Builds the item object
        Map<String, Object> itemData = new HashMap<>();
        itemData.put("id", itemId); // ID
        itemData.put("name", name); // Name
        itemData.put("postedDate", ServerValue.TIMESTAMP); // Server Timestamp
        itemData.put("posterUid", uid); // Owner of the item
        itemData.put("price", price); // Price
        itemData.put("catId", catId);   // Which category it is in

        // Writes item to categories
        catRef.child(itemId).setValue(itemData);

        // Saves the data for the user so that the user can see its own items
        DatabaseReference userItemRef = rootRef.child("users")
                .child(uid)
                .child("items")
                .child(itemId);

        userItemRef.setValue(itemData);

        Toast.makeText(this, "Item posted", Toast.LENGTH_SHORT).show();
        finish();
    }


    // Saves for orientation changes
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ITEM_NAME, etName.getText().toString());
        outState.putString(KEY_PRICE, etPrice.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}