package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import edu.uga.cs.tradeit.R;  // Updated import for R class

/**
 * Main dashboard after login. Logout (Story 3). Links to categories, pending, completed.
 */
public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);  // Uses updated R
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        Toolbar toolbar = findViewById(R.id.toolbar);  // Uses updated R
        setSupportActionBar(toolbar);

        findViewById(R.id.btnCategories).setOnClickListener(v -> startActivity(new Intent(this, CategoriesActivity.class)));
        findViewById(R.id.btnPending).setOnClickListener(v -> startActivity(new Intent(this, PendingTransactionsActivity.class)));
        findViewById(R.id.btnCompleted).setOnClickListener(v -> startActivity(new Intent(this, CompletedTransactionsActivity.class)));
        findViewById(R.id.btnMyItems).setOnClickListener(v -> startActivity(new Intent(this, MyItemsActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);  // Uses updated R
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {  // Uses updated R
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}