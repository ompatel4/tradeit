package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

/**
 * Handles user login (User Story 2).
 * Crash fixes: Full error handling, null checks, logcat for debugging.
 */
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";  // For Logcat
    private EditText etEmail, etPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_login);
        } catch (Exception e) {
            Log.e(TAG, "Layout load failed: " + e.getMessage());
            Toast.makeText(this, "Layout error—check activity_login.xml", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        if (etEmail == null || etPassword == null) {
            Log.e(TAG, "Layout IDs not found—check activity_login.xml");
            Toast.makeText(this, "UI error—IDs missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        if (btnLogin == null || btnRegister == null) {
            Log.e(TAG, "Button IDs not found");
            Toast.makeText(this, "UI error—buttons missing", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Attempting login for: " + email);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Log.e(TAG, "Login failed", task.getException());
                            String errorMsg = "Login failed";
                            if (task.getException() instanceof FirebaseAuthException) {
                                FirebaseAuthException authEx = (FirebaseAuthException) task.getException();
                                errorMsg = authEx.getErrorCode();  // e.g., "ERROR_USER_NOT_FOUND"
                            }
                            Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}