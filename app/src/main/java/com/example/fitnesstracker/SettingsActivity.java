package com.example.fitnesstracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private Button btnLogout, btnResetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        btnLogout = findViewById(R.id.btnLogout);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnLogout.setOnClickListener(v -> logoutUser());

        btnResetPassword.setOnClickListener(v -> {
            Intent intent = new Intent(this, ResetPasswordActivity.class);
            intent.putExtra("fromSettings", true);
            startActivity(intent);
        });
    }

    private void logoutUser() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}