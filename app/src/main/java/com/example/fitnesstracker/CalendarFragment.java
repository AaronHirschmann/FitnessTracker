package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView; // Android eingebauter Kalender
    private TextView tvSelectedDate, tvWorkoutOnDate, tvExercisesOnDate;
    private Button btnAddWorkoutToDate, btnRemoveWorkoutFromDate;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String selectedDate;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calender, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        calendarView = view.findViewById(R.id.calendarView);
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate);
        tvWorkoutOnDate = view.findViewById(R.id.tvWorkoutOnDate);
        tvExercisesOnDate = view.findViewById(R.id.tvExercisesOnDate);
        btnAddWorkoutToDate = view.findViewById(R.id.btnAddWorkoutToDate);
        btnRemoveWorkoutFromDate = view.findViewById(R.id.btnRemoveWorkoutFromDate);


        // Beim öffnen des Kalenders wird automatisch der heutige Tag ausgewählt
        // User kann sehen ob heute ein Workout geplant ist
        String today = getTodayDateString(); // eigene Methode für den ausgewählten Tag
        tvSelectedDate.setText("Ausgewähltes Datum: " + today);
        selectedDate = today;
        loadWorkoutForDate(today); // ist heute ein Workout geplant?


        //wird ausgelöst, wenn der User auf ein Tag klickt
        calendarView.setOnDateChangeListener((calView, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            tvSelectedDate.setText("Ausgewähltes Datum: " + selectedDate);
            loadWorkoutForDate(selectedDate);
        });

        btnAddWorkoutToDate.setOnClickListener(v -> showAddWorkoutDialog());
        btnRemoveWorkoutFromDate.setOnClickListener(v -> removeWorkoutFromDate());


        return view;
    }

    // ohne führende Nullen, da wir ihn als DokumentID nutzen um in Firestore auf Dokumente zuzugreifen
    private String getTodayDateString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }

    //geplantes Workout für ein bestimmtes Datum aus Firestore laden
    private void loadWorkoutForDate(String date) { //Konnektor nutzt den date String
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) { //Wenn ein geplantes Workout besteht
                        String workoutName = documentSnapshot.getString("workoutName");
                        String workoutId = documentSnapshot.getString("workoutId");
                        btnAddWorkoutToDate.setVisibility(View.GONE); //Add Workout Button verschwindet
                        btnRemoveWorkoutFromDate.setVisibility(View.VISIBLE); //Remove Workout Button erscheint
                        if (workoutName != null) {
                            tvWorkoutOnDate.setText("Workout: " + workoutName);
                        }
                        if (workoutId != null) {
                            loadExercisesForWorkout(userID, workoutId); //eigene Methode für das Laden der Übungen der einzelnen Workouts
                        }
                    } else { //Wenn kein Workout für den Tag besteht
                        tvWorkoutOnDate.setText("Kein Workout geplant");
                        tvExercisesOnDate.setText("");
                        btnAddWorkoutToDate.setVisibility(View.VISIBLE); //Add Workout Button erscheint
                        btnRemoveWorkoutFromDate.setVisibility(View.GONE);//Remove Workout Button verschwindet
                    }
                })
                .addOnFailureListener(e -> {
                    tvWorkoutOnDate.setText("Fehler beim Laden");
                });
    }

    //Methode für das laden der Übungen der jeweiligen Workouts
    private void loadExercisesForWorkout(String userID, String workoutId) {
        db.collection("users").document(userID)
                .collection("workouts").document(workoutId)
                .get()
                .addOnSuccessListener(workoutDoc -> {
                    if (workoutDoc.exists()) {
                        //exerciseNames als Onject holen, weil Firestore Listen immer als Object übergibt
                        Object rawNames = workoutDoc.get("exerciseNames");
                        List<String> exerciseNames = new ArrayList<>();
                        if (rawNames instanceof List) { // Ist es wirklich eine Liste?
                            for (Object o : (List<?>) rawNames) {
                                if (o instanceof String) {
                                    exerciseNames.add((String) o);
                                }
                            }
                        }

                        if (exerciseNames.isEmpty()) {
                            tvExercisesOnDate.setText("Keine Übungen in diesem Workout");
                        } else {
                            // Übungen mit Strichen aufbauen und den String zeigen
                            StringBuilder sb = new StringBuilder();
                            for (String name : exerciseNames) {
                                sb.append("- ").append(name).append("\n");
                            }
                            tvExercisesOnDate.setText(sb.toString().trim());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvExercisesOnDate.setText("Fehler beim Laden der Übungen");
                });
    }

    //geplantes Workout von einem Datum entfernen  mit Bestätigungsdialog
    private void removeWorkoutFromDate() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Bitte zuerst ein Datum auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Workout entfernen")
                .setMessage("Möchtest du das Workout für " + selectedDate + " wirklich entfernen?")
                .setPositiveButton("Entfernen", (dialog, which) -> {
                    String userID = mAuth.getCurrentUser().getUid();
                    db.collection("users").document(userID)
                            .collection("plannedWorkouts").document(selectedDate)
                            .delete()
                            .addOnSuccessListener(success -> {
                                Toast.makeText(getContext(), "Workout entfernt!", Toast.LENGTH_SHORT).show();
                                tvWorkoutOnDate.setText("Kein Workout geplant"); // UI zurücksetzen
                                tvExercisesOnDate.setText("");
                                btnRemoveWorkoutFromDate.setVisibility(View.GONE); // Nach löschen removeWorkout Button verstecken
                                btnAddWorkoutToDate.setVisibility(View.VISIBLE); // Nach löschen addWorkout Button zeigen
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Fehler beim Entfernen", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss())
                .show();
    }

    // Dialog zum auswählen eines Workouts für ein Datum
    private void showAddWorkoutDialog() {
        if (selectedDate == null) { // ist ein Datum gewählt?
            Toast.makeText(getContext(), "Bitte zuerst ein Datum auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        String userID = mAuth.getCurrentUser().getUid();

        // Alle Workouts des Users laden für den Spinner
        db.collection("users").document(userID)
                .collection("workouts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Workout> workouts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        workouts.add(new Workout(id, name));
                    }

                    if (workouts.isEmpty()) {
                        Toast.makeText(getContext(), "Bitte zuerst im Workouts-Tab ein Workout anlegen", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Namen für Spinner extrahieren
                    List<String> workoutNames = new ArrayList<>();
                    for (Workout w : workouts) {
                        workoutNames.add(w.getName());
                    }

                    // Spinner mit Workout-Namen befüllen
                    Spinner spinner = new Spinner(getContext());
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, workoutNames);
                    spinner.setAdapter(spinnerAdapter);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Workout für " + selectedDate + " zuweisen");
                    builder.setView(spinner);

                    builder.setPositiveButton("Speichern", (dialog, which) -> {
                        // Index des ausgewählten Workouts im Spinner entnehmen
                        int selectedIndex = spinner.getSelectedItemPosition();
                        // Das dazugehörige Workout entnehmen
                        Workout selectedWorkout = workouts.get(selectedIndex);
                        assignWorkoutToDate(selectedDate, selectedWorkout); // eigene Methode für das hinzufügen von Workouts zu den Daten
                    });

                    builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

                    builder.show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Laden der Workouts", Toast.LENGTH_SHORT).show());
    }

    // Ausgewähltes Workout für ein Datum in Firestore speichern
    private void assignWorkoutToDate(String date, Workout workout) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("workoutId", workout.getID());
        data.put("workoutName", workout.getName());

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .set(data)
                .addOnSuccessListener(success -> {
                    Toast.makeText(getContext(), "Workout geplant!", Toast.LENGTH_SHORT).show();
                    loadWorkoutForDate(date);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                });
    }
}