package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This is responsible for handling the user registration.
 * Validates the email and password, creates a new Firebase Auth user, and stores the users
 * metadata in Realtime Database.
 */
public class RegisterActivity extends AppCompatActivity {
    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;
    // Used to store the email for rotation
    private static final String KEY_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        // Restores the email if the orientation changes
        if (savedInstanceState != null) {
            etEmail.setText(savedInstanceState.getString(KEY_EMAIL));
            etPassword.setText("");  // Removes the password for security purposes
        }

        // When the user clicks Register this validates and then saves the user
        btnRegister.setOnClickListener(v -> registerUser());
    }

    // This validates the user input, creates the Firebase Auth account, and safely stores the user info.
    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }
        // Requires a stronger password length
        if (password.length() < 6) {
            Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
            return;
        }
        // Creates the account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid(); // Unique user ID
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(uid);
                        ref.child("email").setValue(email);
                        ref.child("createdDate").setValue(com.google.firebase.database.ServerValue.TIMESTAMP);
                        // Since registration is complete go to the dashboard
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Email already in use or error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_EMAIL, etEmail.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }
}