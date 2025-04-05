package com.example.mybudget;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackEditText;
    private RatingBar ratingBar;
    private Button submitFeedbackButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize UI components
        feedbackEditText = findViewById(R.id.feedback_text);
        ratingBar = findViewById(R.id.rating_bar);
        submitFeedbackButton = findViewById(R.id.submit_feedback_button);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set click listener for the submit button
        submitFeedbackButton.setOnClickListener(v -> submitFeedback());
    }

    private void submitFeedback() {
        Log.d("FeedbackActivity", "Submit button clicked");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("FeedbackActivity", "User not logged in!");
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        float rating = ratingBar.getRating();
        String feedbackText = feedbackEditText.getText().toString().trim();

        if (feedbackText.isEmpty()) {
            Log.w("FeedbackActivity", "Feedback is empty");
            feedbackEditText.setError("Please provide feedback");
            return;
        }

        Log.d("FeedbackActivity", "Preparing feedback data");

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("message", feedbackText);
        feedbackData.put("rating", rating);
        feedbackData.put("timestamp", new Timestamp(new Date()));

        Log.d("FeedbackActivity", "Sending data to Firestore");

        db.collection("users")
                .document(userId)
                .collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FeedbackActivity", "Feedback submitted successfully");
                    Toast.makeText(FeedbackActivity.this, "Feedback submitted!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("FeedbackActivity", "Failed to submit feedback: " + e.getMessage());
                    Toast.makeText(FeedbackActivity.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
