package com.example.mrshub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 101;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private Button googleAuthButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        googleAuthButton = findViewById(R.id.btnGoogleAuth);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("727014629519-i1mbb35etiogq9g3vpqvebh2ada1vs7f.apps.googleusercontent.com") // <-- Replace with your Web Client ID
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleAuthButton.setOnClickListener(v -> {
            // Always sign out before showing the picker (forces account chooser every time)
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });

        // About App Button logic
        MaterialButton btnAbout = findViewById(R.id.btnAbout);
        btnAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void showAboutDialog() {
        String message = "Mini Resource Sharing Hub\n\n" +
                "Created by: Nish Deshmukh\n" +
                "GitHub: https://github.com/nixsh5\n\n" +
                "Use: This app is designed for college students and faculty to easily share, preview, and manage academic files and notes (images, PDFs, DOCX, etc.). " +
                "It streamlines the distribution of study materials and resources, making it simple to upload, preview, and delete files in a secure, user-friendly environment. " +
                "Perfect for sharing lecture notes, assignments, and important documents within your college community.";

        new AlertDialog.Builder(this)
                .setTitle("About This App")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Open GitHub", (dialog, which) -> {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/nixsh5/MRSHub"));
                    startActivity(browserIntent);
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirebase(user);
                        }
                    } else {
                        Toast.makeText(this, "Firebase Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirebase(FirebaseUser user) {
        String uid = user.getUid(); // Use UID, not email
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getDisplayName());
        userMap.put("email", user.getEmail());
        userMap.put("profile", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "N/A");

        database.getReference("users")
                .child(uid)
                .updateChildren(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, MainActivity2landingdrop.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Failed to save user", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
