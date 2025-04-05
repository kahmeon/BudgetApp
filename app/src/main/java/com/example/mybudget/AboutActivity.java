package com.example.mybudget;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Initialize UI elements
        ImageButton btnGitHub = findViewById(R.id.btn_github);
        ImageButton btnPlayStore = findViewById(R.id.btn_play_store);
        ImageButton btnEmail = findViewById(R.id.btn_email);
        TextView developerEmail = findViewById(R.id.developer_email);

        // Open GitHub Profile
        btnGitHub.setOnClickListener(v -> openWebPage("https://github.com/yourusername"));

        // Open Google Play Store (Replace with your actual app's package name)
        btnPlayStore.setOnClickListener(v -> openWebPage("https://play.google.com/store/apps/details?id=com.example.mybudget"));

        // Send Email
        View.OnClickListener emailClickListener = v -> sendEmail("kahmeon@example.com", "Inquiry about MyBudget App");

        btnEmail.setOnClickListener(emailClickListener);
        developerEmail.setOnClickListener(emailClickListener);
    }

    // Method to open a web page
    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    // Method to send an email
    private void sendEmail(String email, String subject) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + email));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        startActivity(Intent.createChooser(emailIntent, "Send Email"));
    }
}
