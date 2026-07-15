package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvDate, tvCurrentWeight, tvLastWeight, tvTodayWorkout, tvTodayExercises;
    private Button btnUpdateWeight, btnAddWorkout, btnStartSession;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    @Override
    // onCreateView, weil wir hier mit Fragmenten arbeiten
    // LayoutInflater baut xml auf
    // return values ist kein void, sondern eine View
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // inflater.inflate statt setContentView
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Firebase initialisieren
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // UI-Elemente verknüpfen
        tvDate = view.findViewById(R.id.tvDate);
        tvCurrentWeight = view.findViewById(R.id.tvCurrentWeight);
        tvLastWeight = view.findViewById(R.id.tvLastWeight);
        tvTodayWorkout = view.findViewById(R.id.tvTodayWorkout);
        tvTodayExercises = view.findViewById(R.id.tvTodayExercises);
        btnUpdateWeight = view.findViewById(R.id.btnUpdateWeight);
        btnAddWorkout = view.findViewById(R.id.btnAddWorkout);
        btnStartSession = view.findViewById(R.id.btnStartSession);

        btnUpdateWeight.setOnClickListener(updateWeightClicked -> showUpdateWeightDialog());
        btnAddWorkout.setOnClickListener(selectWorkoutClicked -> showSelectWorkoutDialog());
        btnStartSession.setOnClickListener(startSessionClicked -> startSession());

        //Datumsformat
        Calendar calendar = Calendar.getInstance(); //aktuelles Datum holen
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Java zählt von 0 - 11 und nicht von 1 - 12
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String date = String.format("%04d-%02d-%02d", year, month, day); //führende Nullen -> nicht 2026-7-1
        tvDate.setText(date);

        //Daten aus Firebase laden
        loadUserData();
        loadTodayWorkout();

        //view zurückgeben
        return view;
    }

    //Gewicht und letztes Gewicht aus Firebase laden
    private void loadUserData() {
        String userID = mAuth.getCurrentUser().getUid();

        // dokument "users" mit der userID laden
        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double currentWeight = document.getDouble("currentWeight"); //speicher currentWeight aus Firestore in Variable currentWeight
                        Double lastWeight = document.getDouble("lastWeight"); //speicher lastWeight aus Firestore in Variable lastWeight

                        if (currentWeight != null) {
                            tvCurrentWeight.setText(currentWeight + " kg"); // TextView zeigt currentWeight an
                        }
                        if (lastWeight != null) {
                            tvLastWeight.setText("Letztes Gewicht: " + lastWeight + " kg"); //TextView zeigt lastWeight an
                        }
                    }
                }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
            });
    }


    // Heutiges Datum als String im Format "yyyy-M-d" zurückgeben
    // → wird als Firestore Dokument-ID benutzt (z.B. "2026-7-15")
    // → WICHTIG: anderes Format als tvDate (kein String.format hier!)
    private String getTodayDateString() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }
    // Pop-Up zum Gewicht aktualisieren
    private void showUpdateWeightDialog() {
        // Dialog-Layout aus xml laden
        // getContext() leiht den Kontext von der übergeordneten Activity, weil Fragment keine Activity ist
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_weight, null);
        EditText etWeight = dialogView.findViewById(R.id.etWeight);

        //AlertDialog wird aufgebaut und in den Kontext des dialogView angepasst
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        //Was macht der PositiveButton
        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String weightStr = etWeight.getText().toString().trim();
            if (!weightStr.isEmpty()) {
                saveWeight(Double.parseDouble(weightStr)); //Speicher die angebene Zahl mithilfe der eigenen Methode
            } else {
                Toast.makeText(getContext(), "Bitte Gewicht eingeben", Toast.LENGTH_SHORT).show();
            }
        });

        //Was macht der NegativeButton
        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        //Zeige den Dialog
        builder.show();
    }

    //Methode zum Speichern von Gewicht
    private void saveWeight(double newWeight) {
        String userID = mAuth.getCurrentUser().getUid();

        //Aktuelles Gewicht lesen
        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> data = new HashMap<>();

                    // Aktuellestes Gewicht vor Änderung als "lastWeight"  speichern
                    if (documentSnapshot.exists() && documentSnapshot.getDouble("currentWeight") != null) {
                        data.put("lastWeight", documentSnapshot.getDouble("currentWeight"));
                    }

                    // neues Gewicht = aktuelles Gewicht
                    data.put("currentWeight", newWeight);

                    // in Firebase speichern
                    db.collection("users").document(userID)
                            // SetOptions.merge() nur das ausgewählte Feld wird verändert, der Rest bleibt unverändert
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(success -> {
                                tvCurrentWeight.setText(newWeight + " kg"); // Neues Gewicht anzeigen
                                Toast.makeText(getContext(), "Gewicht gespeichert!", Toast.LENGTH_SHORT).show();
                                loadUserData(); // Eigene Methode fürs laden der Daten
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Fehler beim Speichern!", Toast.LENGTH_SHORT).show();
                            });
                });
    }

    //Heutiges geplantes Workout aus Firebase laden
    private void loadTodayWorkout() {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString(); // z.B "2026-7-8"

        // Wir nutzen das Datum als DokumentID
        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today) // Hier wird das Datum als Dokument ID genutzt, damit wir sicherstellen, dass es nur ein Workout am Tag gibt
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String workoutName = documentSnapshot.getString("workoutName");
                        String workoutId = documentSnapshot.getString("workoutId");
                        if (workoutName != null) {
                            tvTodayWorkout.setText("Heutiges Workout: " + workoutName);
                        }
                        if (workoutId != null) {
                            loadWorkoutExercises(userID, workoutId);
                        }
                        btnAddWorkout.setText("Workout anpassen");
                    } else {
                        tvTodayWorkout.setText("Kein Workout für heute geplant");
                        tvTodayExercises.setText(""); // Wenn kein Workout -> keine Übungen
                        btnAddWorkout.setText("Workout hinzufügen");
                    }
                })
                .addOnFailureListener(e -> {
                    tvTodayWorkout.setText("Fehler beim Laden");
                });
    }

    // Übungen des heutigen Workouts laden und anzeigen
    private void loadWorkoutExercises(String userID, String workoutId) {
        db.collection("users").document(userID)
                .collection("workouts").document(workoutId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Object rawNames = documentSnapshot.get("exerciseNames"); // Firebase gibt nur Objekte, diese müssen wir jetzt füllen
                        List<String> exerciseNames = new ArrayList<>();
                        if (rawNames instanceof List) { //prüfen, ob es wirklich eine Liste ist
                            for (Object o : (List<?>) rawNames) { //"?" heißt der Datentyp der Liste ist unbekannt und checkt dann ob es ein String ist
                                if (o instanceof String) {
                                    exerciseNames.add((String) o);
                                }
                            }
                        }

                        if (exerciseNames.isEmpty()) {
                            tvTodayExercises.setText("Keine Übungen in diesem Workout");
                        } else {
                            StringBuilder sb = new StringBuilder(); // Bauen der Liste von Übungen
                            for (String name : exerciseNames) {
                                sb.append("- ").append(name).append("\n");
                            }
                            tvTodayExercises.setText(sb.toString().trim()); // TextView mit der Liste aus StringBuilder füllen
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvTodayExercises.setText("Fehler beim Laden der Übungen");
                });
    }

    //Dialog zum Auswählen eines Workouts für heute anzeigen
    private void showSelectWorkoutDialog() {
        String userID = mAuth.getCurrentUser().getUid();

        //Alle Workouts aus Firestore laden
        db.collection("users").document(userID)
                .collection("workouts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> { // Alle Dokumente der Collection Workouts von dem jeweiligen User laden
                    List<Workout> workouts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getId(); // Firebase Dokument-ID
                        String name = doc.getString("name");
                        workouts.add(new Workout(id, name));
                    }

                    if (workouts.isEmpty()) {
                        Toast.makeText(getContext(), "Bitte zuerst im Workouts-Tab ein Workout anlegen", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<String> workoutNames = new ArrayList<>();
                    for (Workout w : workouts) {
                        workoutNames.add(w.getName());
                    }

                    //Dropdown Menü
                    Spinner spinner = new Spinner(getContext());
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                            requireContext(), // verknüpft die Liste mit dem Spinner
                            android.R.layout.simple_spinner_dropdown_item, // Spinner-xml-Layout von Android
                            workoutNames); // die Liste welche angezeigt wird
                    spinner.setAdapter(spinnerAdapter); // Verbindung von Adapter und Spinner

                    //
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Workout für heute auswählen");
                    builder.setView(spinner);

                    builder.setPositiveButton("Speichern", (dialog, which) -> {
                        // das im Dropwdown ausgewählte Workout wird selected
                        int selectedIndex = spinner.getSelectedItemPosition();
                        Workout selectedWorkout = workouts.get(selectedIndex);
                        assignWorkoutToToday(selectedWorkout); // eigene Methode zum zuweisen von Workouts auf Datum
                    });

                    builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

                    builder.show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Laden der Workouts", Toast.LENGTH_SHORT).show());
    }

    // Ausgewähltes Workout für heute in Firestore speichern
    private void assignWorkoutToToday(Workout workout) {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString();

        Map<String, Object> data = new HashMap<>();
        data.put("workoutId", workout.getID());
        data.put("workoutName", workout.getName());

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today) // nimmt sich die Dokument-ID, welche ein Datum ist
                .set(data) // überschreibt vorhandenes Workout
                .addOnSuccessListener(success -> {
                    Toast.makeText(getContext(), "Workout für heute gespeichert!", Toast.LENGTH_SHORT).show();
                    loadTodayWorkout(); //UI wird aktualisiert
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show());
    }

    //Session starten
    // prüft ob Workout für heutigen Tag geplant ist
    private void startSession() {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) { // Wenn kein Workout für heute geplant, dann gebe aus:
                        Toast.makeText(getContext(), "Bitte zuerst ein Workout für heute auswählen", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Er sagt ich möchte einen neuen Screen
                    // getActivity() -> von wo, SessionActivity -> wohin
                    Intent intent = new Intent(getActivity(), SessionActivity.class);
                    intent.putExtra("date", today); // schicke Daten mit in den neuen Screen
                    startActivity(intent); // Startet neuen Screen
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Prüfen des Workouts", Toast.LENGTH_SHORT).show());
    }
}