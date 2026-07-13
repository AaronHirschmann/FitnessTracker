package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvCurrentWeight, tvLastWeight, tvTodayWorkout;
    private Button btnUpdateWeight, btnShowPlannedWorkout, btnAddWorkout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvCurrentWeight = view.findViewById(R.id.tvCurrentWeight);
        tvLastWeight = view.findViewById(R.id.tvLastWeight);
        tvTodayWorkout = view.findViewById(R.id.tvTodayWorkout);
        btnUpdateWeight = view.findViewById(R.id.btnUpdateWeight);
        btnShowPlannedWorkout = view.findViewById(R.id.btnShowPlannedWorkout);
        btnAddWorkout = view.findViewById(R.id.btnAddWorkout);

        btnUpdateWeight.setOnClickListener(v -> showUpdateWeightDialog());

        loadUserData();


        return view;
    }

    private void loadUserData() {
        String userID = mAuth.getCurrentUser().getUid();


        db.collection("users").document(userID).get().addOnSuccessListener(document -> {
        // collection "users" ist der Ordner in Firebase
        // document().get() öffnet das dokument mit der UserID
        // addOnSuccessListener heißt, die Funktion wird erst aufgerufen, wenn Firebase fertig geladen ist
            if (document.exists()) { // prüft ob daten vorhanden sind
                Double currentWeight = document.getDouble("currentWeight"); // Werte werden ausgelesen
                Double lastWeight = document.getDouble("lastWeight");

                //Werte aktualisieren
                if (currentWeight != null) {
                    tvCurrentWeight.setText(currentWeight + " kg");
                }
                if (lastWeight != null) {
                    tvLastWeight.setText("Letztes Gewicht: " + lastWeight + " kg");
                }
            }

        }) //Sicherheit, falls ein Fehler entsteht
        .addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
        });
    }


    // Dialog-Fenster für das Aktualisieren von Gewicht
    private void showUpdateWeightDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_weight, null);  // lädt die xml
        EditText etWeight = dialogView.findViewById(R.id.etWeight);  // Das Eingabefeld aus der xml

        // Erstellt den Dialog-Builder baut unser xml in das dialog feld ein
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        //bestätigungsbutton mit Umwandlung von String in Double
        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String weightStr = etWeight.getText().toString().trim();
            if (!weightStr.isEmpty()) {
                saveWeight(Double.parseDouble(weightStr));
            } else {
                Toast.makeText(getContext(), "Bitte Gewicht eingeben", Toast.LENGTH_SHORT).show();
            }
        });

        //back button
        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    //Speichern der Gewichtsdaten
    private void saveWeight(double newWeight) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    //Erstellung der Value Paare - (String = "currentWeight", Object = "newWeight" oder 63,5)
                    //Somit werden Daten dann zu Firebase geschickt
                    Map<String, Object> data = new HashMap<>();

                    //Altes Gewicht wird zu lastWeight, Neues Gewicht wird zu currentWeight
                    if (documentSnapshot.exists() && documentSnapshot.getDouble("currentWeight") != null) {
                        data.put("lastWeight", documentSnapshot.getDouble("currentWeight"));
                    }

                    data.put("currentWeight", newWeight);

                    db.collection("users").document(userID)
                            .set(data, com.google.firebase.firestore.SetOptions.merge()) //SetOptions.merge(): nur neue Daten werden überschrieben, nichts anderes gelöscht
                            .addOnSuccessListener(aVoid -> {
                                tvCurrentWeight.setText(newWeight + " kg");
                                Toast.makeText(getContext(), "Gewicht gespeichert!", Toast.LENGTH_SHORT).show();
                                loadUserData();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Fehler beim Speichern!", Toast.LENGTH_SHORT).show();
                            });
                });

    }
}