package com.katza.myfirestoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        auth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.email);
        etPassword = findViewById(R.id.password);
        btnLogin = findViewById(R.id.login);
        btnSignup = findViewById(R.id.signup);
        btnLogin.setOnClickListener(view -> login());
        btnSignup.setOnClickListener(view -> signup());
    }

    private void login() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty()) {
            etEmail.setError("Enter your email.");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Enter your password.");
            return;
        }
        setButtonsEnabled(false);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> saveUser(email))
                .addOnFailureListener(error -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void signup() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        if (email.isEmpty()) {
            etEmail.setError("Enter your email");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Enter your password");
            return;
        }
        setButtonsEnabled(false);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> saveUser(email))
                .addOnFailureListener(error -> {
                    setButtonsEnabled(true);
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUser(String email) {
        String uid = auth.getCurrentUser().getUid();
        Map<String, Object> userRecord = new HashMap<>();
        userRecord.put("email", email);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(userRecord, SetOptions.merge())
                .addOnSuccessListener(unused -> openProjects())
                .addOnFailureListener(error -> {
                    auth.signOut();
                    setButtonsEnabled(true);
                    Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setButtonsEnabled(boolean enabled) {
        btnLogin.setEnabled(enabled);
        btnSignup.setEnabled(enabled);
    }

    private void openProjects() {
        Intent intent = new Intent(this, ProjectsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            openProjects();
        }
    }
}
