package com.example.fitnesstracker;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private FirebaseAuth mAuth;
    private boolean fromSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();
        fromSettings = getIntent().getBooleanExtra("fromSettings", false);

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        // Wenn von Settings → E-Mail Feld verstecken
        if (fromSettings) {
            etEmail.setVisibility(View.GONE);
        }

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email;

        if (fromSettings) {
            // E-Mail aus Firebase Auth holen
            email = mAuth.getCurrentUser().getEmail();
        } else {
            // E-Mail aus dem Eingabefeld holen
            email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                etEmail.setError("Bitte E-Mail eingeben");
                return;
            }
        }

        if (email == null) {
            Toast.makeText(this, "Keine E-Mail gefunden", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "E-Mail zum Zurücksetzen wurde an " + email + " gesendet!",
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Fehler: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}