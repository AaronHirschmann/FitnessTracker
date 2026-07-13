package com.example.fitnesstracker;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnResetPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            etEmail.setError("Bitte Email angeben");
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Email zum Zurücksetzen wurde versender", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Fehler: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
