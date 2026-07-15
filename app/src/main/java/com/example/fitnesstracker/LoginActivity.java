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
    private TextView tvRegister,tvForgotPassword;
    private FirebaseAuth mAuth;



    @Override
    protected void onCreate(Bundle savedInstanceState) { // savedInstanceState = letzter Zustand der App
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // xml mit Layout Screen verknüpfen, sodass unsere xml aufgebaut wird

        mAuth = FirebaseAuth.getInstance(); // Firebase wird initialisiert


        //Prüfen ob bereits ein User eingeloggt ist...
        if (mAuth.getCurrentUser() != null) { // JA -> direkt die MainActivity starten
            startActivity(new Intent(this, MainActivity.class)); // newIntent öffnet neuen Screen
            finish(); // schließt die LoginActivity -> mit "back" kommt man nicht mehr zurück
            return; // onCreate wird hier beendet
        }

        // Verbinden der UI Elemente mit XML aus activity_login.xml
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);


        // Methode für den Klick auf "login"
        btnLogin.setOnClickListener(clickedButton -> { // clickedButton ist eine View -> jedes UI-Element in Android ist ein View
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                //Validierung! Felder dürfen nicht leer sein
                if (email.isEmpty()) {
                    etEmail.setError("Bitte E-Mail eingeben");
                    return;
                }

                if (password.isEmpty()) {
                    etPassword.setError("Bitte Passwort eingeben");
                    return;
                }

                //Firebase Authentification: Hier ist der Login-Versuch
                mAuth.signInWithEmailAndPassword(email, password) //asynchron als Firebase-methode
                        .addOnCompleteListener(task -> { // task ist ein Objekt vom Typ Task und repräsentiert das Ergebnis von Firebase Authentification
                            if (task.isSuccessful()) { //task. wird hier aufgerufen, wenn Login erfolgreich: starte die MainActivity und beende LoginActivity
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            } else { // Falls Fehler, Flashmeldung mit Fehler
                                Toast.makeText(this,
                                        "Login fehlgeschlagen: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show(); // LENGTH_LONG ist eine längere Anzeigedauer
                            }
                        });
        });

        // Mit Klick auf "Registrieren" wechseln wir zur RegiserActivity
        tvRegister.setOnClickListener(registerClicked -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Mit Klick auf "Passwort vergessen" wechseln wir zur ResetPasswordActivity
        tvForgotPassword.setOnClickListener(forgotPasswordClicked -> {
            startActivity(new Intent(this, ResetPasswordActivity.class));
        });
    }
}