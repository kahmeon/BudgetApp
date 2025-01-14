package com.example.mybudget;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class SetupProfileActivity extends AppCompatActivity {

    private EditText usernameEditText, passwordEditText;
    private Button saveButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        usernameEditText = findViewById(R.id.username);
        passwordEditText = findViewById(R.id.password);
        saveButton = findViewById(R.id.save_button);

        saveButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty()) {
                usernameEditText.setError(getString(R.string.error_empty_fields));
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError(getString(R.string.error_empty_fields));
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError(getString(R.string.error_password_too_short));
                return;
            }

            checkUsernameAndSaveProfile(username, password);
        });

    }

    private void checkUsernameAndSaveProfile(String username, String password) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            usernameEditText.setError(getString(R.string.error_username_taken));
                        } else {
                            saveUserProfile(username, password);
                        }
                    } else {
                        String errorMessage = task.getException() != null
                                ? String.format(getString(R.string.error_checking_username), task.getException().getMessage())
                                : getString(R.string.error_checking_username, "Unknown error");
                        Toast.makeText(SetupProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = String.format(getString(R.string.error_checking_username), e.getMessage());
                    Toast.makeText(SetupProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
    }


    private void saveUserProfile(String username, String password) {
        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> userDetails = new HashMap<>();
        userDetails.put("username", username.toLowerCase());
        userDetails.put("email", auth.getCurrentUser().getEmail());

        db.collection("users").document(userId).set(userDetails)
                .addOnSuccessListener(aVoid -> {
                    auth.getCurrentUser().updatePassword(password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(SetupProfileActivity.this, getString(R.string.success_profile_saved), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    String errorMessage = task.getException() != null
                                            ? String.format(getString(R.string.error_updating_password), task.getException().getMessage())
                                            : getString(R.string.error_updating_password, "Unknown error");
                                    Toast.makeText(SetupProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    String errorMessage = String.format(getString(R.string.error_saving_profile), e.getMessage());
                    Toast.makeText(SetupProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                });
    }



}
