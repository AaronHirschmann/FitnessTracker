package com.example.fitnesstracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etPasswordConfirm;
    private Button btnRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etUsername = findViewById(R.id.etUserName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPasswordConfirm = findViewById(R.id.etPasswordConfirm);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(view -> registerUser());
    }

    // registerUser() erstellen

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("Bitte Nutzername eingeben");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Bitte Passwort eingeben");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Passwort muss mind. 6 Zeichen haben");
            return;
        }
        if (!password.equals(passwordConfirm)) { // wir nutzen .equals, weil wir einen String prüfen (kein !=)!!
            etPasswordConfirm.setError("Passwörter stimmen nicht überein");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Registrierung erfolgreich!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Fehler: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}