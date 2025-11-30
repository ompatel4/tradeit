package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import edu.uga.cs.tradeit.R;

/**
 * Main dashboard after login. Logout (Story 3). Links to categories, pending, completed.
 * Fixed ActionBar conflict; added logging/null checks.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.w(TAG, "User null—redirecting to login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        Log.d(TAG, "Logged in as: " + user.getEmail());

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.e(TAG, "Toolbar null—check activity_main.xml");
            Toast.makeText(this, "UI error: Toolbar missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setSupportActionBar(toolbar);

        Button btnCategories = findViewById(R.id.btnCategories);
        Button btnMyItems = findViewById(R.id.btnMyItems);
        Button btnPending = findViewById(R.id.btnPending);
        Button btnCompleted = findViewById(R.id.btnCompleted);

        if (btnCategories == null || btnMyItems == null || btnPending == null || btnCompleted == null) {
            Log.e(TAG, "Button(s) null—check activity_main.xml IDs");
            Toast.makeText(this, "UI error: Buttons missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnCategories.setOnClickListener(v -> startActivity(new Intent(this, CategoriesActivity.class)));
        btnMyItems.setOnClickListener(v -> startActivity(new Intent(this, MyItemsActivity.class)));
        btnPending.setOnClickListener(v -> startActivity(new Intent(this, PendingTransactionsActivity.class)));
        btnCompleted.setOnClickListener(v -> startActivity(new Intent(this, CompletedTransactionsActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}