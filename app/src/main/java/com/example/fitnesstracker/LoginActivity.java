package com.example.fitnesstracker;



import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Toast;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance(); // Firebase wird initialisiert

        // Verbinden der UI Elemente mit XML aus activity_login.xml
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        btnLogin.setOnClickListener(view -> {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();


                if (email.isEmpty()) {
                    etEmail.setError("Bitte E-Mail eingeben");
                    return;
                }

                if (password.isEmpty()) {
                    etPassword.setError("Bitte Passwort eingeben");
                    return;
                }

                // Firebase Login

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Login fehlgeschlagen: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
        });

        tvRegister.setOnClickListener(view -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
}