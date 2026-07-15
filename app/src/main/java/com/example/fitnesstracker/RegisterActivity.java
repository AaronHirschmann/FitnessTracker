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
import java.util.List;
import java.util.Map;

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

    private void registerUser() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String passwordConfirm = etPasswordConfirm.getText().toString().trim();


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

        if (password.length() < 6) {
            etPassword.setError("Passwort muss mind. 6 Zeichen haben");
            return;
        }

        if (!password.equals(passwordConfirm)) {
            etPasswordConfirm.setError("Passwörter stimmen nicht überein");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userID = mAuth.getCurrentUser().getUid();

                Map<String, Object> userData = new HashMap<>();
                userData.put("username", username);
                userData.put("email", email);
                userData.put("currentWeight", 0.0);
                userData.put("lastWeight", 0.0);
                userData.put("height", 0.0);

                FirebaseFirestore.getInstance()
                        .collection("users").document(userID)
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            createDefaultExercisesAndWorkouts(userID);
                            Toast.makeText(this, "Registrierung erfolgreich!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, LoginActivity.class));
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

    private void createDefaultExercisesAndWorkouts(String userID) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

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

        for (String[] exercise : exercises) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", exercise[0]);

            List<String> metrics = new ArrayList<>();
            for (int i = 1; i < exercise.length; i++) {
                metrics.add(exercise[i]);
            }
            data.put("metrics", metrics);

            db.collection("users").document(userID)
                    .collection("exercises")
                    .add(data);
        }

        // Workouts nach 2 Sekunden anlegen
        new android.os.Handler().postDelayed(() ->
                createDefaultWorkouts(userID, db), 2000);
    }

    private void createDefaultWorkouts(String userID, FirebaseFirestore db) {
        Object[][] workouts = {
                {"Push Day", new String[]{"Bankdrücken", "Schulterdrücken", "Dips"}},
                {"Pull Day", new String[]{"Klimmzüge", "Kreuzheben"}},
                {"Leg Day", new String[]{"Kniebeuge"}},
                {"Cardio", new String[]{"Laufen", "Radfahren"}}
        };

        for (Object[] workout : workouts) {
            Map<String, Object> data = new HashMap<>();
            data.put("name", workout[0]);

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