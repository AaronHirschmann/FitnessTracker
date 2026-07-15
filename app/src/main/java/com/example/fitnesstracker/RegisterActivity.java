package com.example.fitnesstracker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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

        //Klick "Registrieren" ruft Methode registerUser() auf, welche unten erstellt wird
        btnRegister.setOnClickListener(registerClicked -> registerUser());
    }

    //Methode für Registrierungslogik
    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();

        //Validierung, Felder müssen gefüllt sein, return stoppt methode, wenn ein Feld nicht ausgefüllt ist

        if (username.isEmpty()) {
            etUsername.setError("Bitte Nutzername eingeben");
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Bitte Email eingeben");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("Bitte Passwort eingeben");
            return;
        }

        //passwort muss in Firebase mind. 6 Zeichen haben, dewsegen prüfen wir hier auf mind. 6 Zeichen
        if (password.length() < 6) {
            etPassword.setError("Passwort muss mind. 6 Zeichen haben");
            return;
        }

        //Wiederholen des Passworts mit .equals, weil es ein String
        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("Passwörter stimmen nicht überein");
            return;
        }

        //Hier wird ein neuer Account angelegt
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> { //task ist ein Objekt vom Typ Task und repräsentiert das Ergebnis von Firebase Authentification
            if (task.isSuccessful()) {
                String userID = mAuth.getCurrentUser().getUid(); // jeder Firebase User kriegt eine Unique ID (Uid)

                // Dokument in Firestore vorbereiten
                // Zuordnung von Daten in mithilfe von HashMap und Map
                Map<String, Object> userData = new HashMap<>();
                userData.put("username", username);
                userData.put("email", email);
                userData.put("currentWeight", 0.0);
                userData.put("lastWeight", 0.0);
                userData.put("height", 0.0);

                FirebaseFirestore.getInstance()
                        .collection("users").document(userID)
                        .set(userData) // die in der HashMap gespeicherten Daten werden dem User zugeordnet
                        .addOnSuccessListener(success -> {
                            createDefaultExercisesAndWorkouts(userID); // Eigene Methode für vorgegebene Übungen und Workouts
                            Toast.makeText(this, "Registrierung erfolgreich!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, MainActivity.class)); // startet die MainActivity und beendet die RegisterActivity
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Fehler beim Anlegen des Profils", Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Fehler: " + task.getException().getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    //vorgegebene Übungen und Workouts für jeden User anlegen
    //nach erfolgreicher Registrierung
    private void createDefaultExercisesAndWorkouts(String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Array mit zwei Spalten
        // 1. Spalte Übungsname
        // 2. Spalte Metriken
        String[][] exercises = {
                {"Bankdrücken", "Gewicht", "Wiederholungen", "Sätze"},
                {"Kniebeuge", "Gewicht", "Wiederholungen", "Sätze"},
                {"Kreuzheben", "Gewicht", "Wiederholungen", "Sätze"},
                {"Schulterdrücken", "Gewicht", "Wiederholungen", "Sätze"},
                {"Klimmzüge", "Wiederholungen", "Sätze"},
                {"Dips", "Wiederholungen", "Sätze"},
                {"Laufen", "Zeit", "Distanz"},
                {"Radfahren", "Zeit", "Distanz"}
        };

        // Durch alle Spalte iterieren und in Firestore speichern
        for (String[] exercise : exercises) { //
            Map<String, Object> data = new HashMap<>();
            data.put("name", exercise[0]); // erste Spalte: name

            List<String> metrics = new ArrayList<>();
            for (int i = 1; i < exercise.length; i++) { //ab zweiter Spalte: metriken
                metrics.add(exercise[i]); // metriken werden in der ArrayList für die jeweilige Übung gespeichert
            }
            data.put("metrics", metrics);

            db.collection("users").document(userID)
                    .collection("exercises")
                    .add(data);
        }

        // Workouts nach 2 Sekunden anlegen
        // er wartet, weil Übungen erst in Firestore gespeichert werden müssen, dann kann er die Methode anwenden
        new android.os.Handler().postDelayed(() ->
                createDefaultWorkouts(userID, db), 2000);
    }

    // vorgegebene Workouts mit den vorgegebenen Übungen erstellen
    // gleicher Prinzip wie oben
    private void createDefaultWorkouts(String userID, FirebaseFirestore db) {
        Object[][] workouts = { // erste Spalte: name des Workouts, zweite Spalte: Übungen
                {"Push Day", new String[]{"Bankdrücken", "Schulterdrücken", "Dips"}},
                {"Pull Day", new String[]{"Klimmzüge", "Kreuzheben"}},
                {"Leg Day", new String[]{"Kniebeuge"}},
                {"Cardio", new String[]{"Laufen", "Radfahren"}}
        };

        // durch workouts iterieren
        for (Object[] workout : workouts) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", workout[0]);

            // Übungsnamen als Liste speichern
            List<String> exerciseNames = new ArrayList<>();
            for (String name : (String[]) workout[1]) {
                exerciseNames.add(name);
            }
            data.put("exerciseNames", exerciseNames);

            db.collection("users").document(userID)
                    .collection("workouts")
                    .add(data);
        }
    }
}