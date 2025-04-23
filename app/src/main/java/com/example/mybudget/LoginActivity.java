package com.example.mybudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private TextView registerLink, forgotPasswordLink;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if the user is already logged in
        if (isUserLoggedIn()) {
            redirectToMainActivity();
            return;
        }

        // Set content view
        setContentView(R.layout.activity_login);

        // Initialize Firebase and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        LinearLayout googleLoginButton = findViewById(R.id.google_login_button);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your Firebase web client ID
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set click listeners
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
        loginButton.setOnClickListener(v -> handleLogin());
        registerLink.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
        forgotPasswordLink.setOnClickListener(v -> handleForgotPassword());
    }

    private void handleLogin() {
        String usernameOrEmail = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (usernameOrEmail.isEmpty()) {
            usernameInput.setError(getString(R.string.error_empty_username));
            return;
        }

        if (!usernameOrEmail.contains("@") && usernameOrEmail.length() < 3) {
            usernameInput.setError(getString(R.string.error_invalid_email));
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.error_empty_password));
            return;
        }

        if (usernameOrEmail.contains("@")) {
            // Login with email
            signInWithEmail(usernameOrEmail, password);
        } else {
            // Login with username
            signInWithUsername(usernameOrEmail, password);
        }
    }



    private void handleForgotPassword() {
        String email = usernameInput.getText().toString().trim();
        if (email.isEmpty()) {
            usernameInput.setError(getString(R.string.error_empty_username));
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Password reset email sent.", Toast.LENGTH_SHORT).show();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_reset_email_failed);
                        Log.e("LoginActivity", "Failed to send reset email: " + errorMessage);
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithUsername(String username, String password) {
        db.collection("users")
                .whereEqualTo("username", username.toLowerCase()) // Query Firestore for the username
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String email = doc.getString("email");

                        if (email != null) {
                            // Sign in with email and password
                            signInWithEmail(email, password);
                        } else {
                            usernameInput.setError(getString(R.string.error_user_not_found));
                        }
                    } else {
                        usernameInput.setError(getString(R.string.error_user_not_found));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LoginActivity", "Failed to query username: ", e);
                    Toast.makeText(LoginActivity.this, getString(R.string.error_username_query_failed), Toast.LENGTH_SHORT).show();
                });
    }


    private void signInWithEmail(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        saveLoginState();
                        String userId = auth.getCurrentUser().getUid();
                        insertDefaultCategoriesIfNeeded(userId, () -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        });

                    } else {


                        String errorMessage;
                        if (task.getException() instanceof FirebaseAuthException) {
                            FirebaseAuthException exception = (FirebaseAuthException) task.getException();
                            switch (exception.getErrorCode()) {
                                case "ERROR_WRONG_PASSWORD":
                                    errorMessage = getString(R.string.error_wrong_password);
                                    passwordInput.setError(errorMessage);
                                    break;
                                case "ERROR_USER_NOT_FOUND":
                                    errorMessage = getString(R.string.error_user_not_found);
                                    usernameInput.setError(errorMessage);
                                    break;
                                default:
                                    errorMessage = exception.getMessage();
                                    break;
                            }
                        } else {
                            errorMessage = task.getException() != null ? task.getException().getMessage() : getString(R.string.error_invalid_credentials);
                        }
                        Log.e("LoginActivity", "Login failed: " + errorMessage);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }






    private void saveLoginState() {
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("isLoggedIn", true).apply();
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }

    private void redirectToMainActivity() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.e("LoginActivity", "Google sign-in failed: ", e);
                Toast.makeText(this, getString(R.string.error_google_sign_in), Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        // Check Firestore for user profile (username field)
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists() && documentSnapshot.contains("username")) {
                                        // User has a username, go to MainActivity
                                        saveLoginState();
                                        insertDefaultCategoriesIfNeeded(userId, () -> {
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        });
                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();

                                    } else {
                                        // Redirect to SetupProfileActivity if username doesn't exist
                                        Intent intent = new Intent(LoginActivity.this, SetupProfileActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(LoginActivity.this, "Error checking user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void insertDefaultCategoriesIfNeeded(String userId, Runnable onComplete) {
        CollectionReference categoriesRef = db.collection("users")
                .document(userId)
                .collection("categories");

        categoriesRef.get().addOnSuccessListener(snapshot -> {
            List<String> existingNames = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot) {
                String name = doc.getString("name");
                if (name != null) {
                    existingNames.add(name.toLowerCase());
                }
            }

            String[][] defaultCategories = {
                    {"Salary", "Income"},
                    {"Bonus", "Income"},
                    {"Food", "Expense"},
                    {"Transport", "Expense"},
                    {"Utilities", "Expense"},
                    {"Entertainment", "Expense"},
                    {"Health", "Expense"},
                    {"Travel", "Expense"},
                    {"Shopping", "Expense"},
                    {"Snacks", "Expense"}
            };

            List<Task<?>> tasks = new ArrayList<>();
            for (String[] pair : defaultCategories) {
                String name = pair[0];
                String type = pair[1];

                if (!existingNames.contains(name.toLowerCase())) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("name", name);
                    data.put("type", type);
                    tasks.add(categoriesRef.add(data));
                }
            }

            if (tasks.isEmpty()) {
                onComplete.run(); // Nothing to add, continue
            } else {
                Tasks.whenAllSuccess(tasks)
                        .addOnSuccessListener(results -> onComplete.run())
                        .addOnFailureListener(e -> {
                            Log.e("LoginActivity", "Failed inserting defaults", e);
                            onComplete.run(); // Continue even on failure
                        });
            }
        });
    }


}
