package com.example.fitnesstracker;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionActivity extends AppCompatActivity {

    private static final String SETS_METRIC = "Sätze";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String date;

    private LinearLayout container;
    private TextView tvTitle;
    private Button btnSave;

    private List<Map<String, Object>> sessionExercises = new ArrayList<>();
    private String workoutId;
    private String workoutName;

    private final List<List<Map<String, EditText>>> inputRefs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Daten aus dem Intent empfangen
        date = getIntent().getStringExtra("date");

        tvTitle = findViewById(R.id.tv_session_title);
        container = findViewById(R.id.container_session_exercises);
        btnSave = findViewById(R.id.btn_save_session);

        btnSave.setOnClickListener(v -> saveSession());

        loadOrCreateSession(); // Methode für das Laden der Session
    }

    // Prüft ob bereits eine Session für diesen Tag existiert
    // → falls ja: laden und weiterführen
    // → falls nein: neue Session erstellen
    private void loadOrCreateSession() {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .get()
                .addOnSuccessListener(plannedDoc -> {
                    if (!plannedDoc.exists()) {
                        Toast.makeText(this, "Kein Workout für dieses Datum geplant", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    String plannedWorkoutId = plannedDoc.getString("workoutId");
                    String plannedWorkoutName = plannedDoc.getString("workoutName");

                    if (plannedWorkoutId == null) {
                        Toast.makeText(this, "Geplantes Workout hat keine gültige ID", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    db.collection("users").document(userID)
                            .collection("sessions").document(date)
                            .get()
                            .addOnSuccessListener(sessionDoc -> {
                                //Übungen aus Session holen falls sie existiert
                                Object rawExercises;
                                if (sessionDoc.exists()) {
                                    rawExercises = sessionDoc.get("exercises");
                                } else {
                                    rawExercises = null;
                                }

                                //Hat die Session Übungen?
                                boolean hasExercises;
                                if (rawExercises instanceof List) {
                                    if (!((List<?>) rawExercises).isEmpty()) {
                                        hasExercises = true;
                                    } else {
                                        hasExercises = false;
                                    }
                                } else {
                                    hasExercises = false;
                                }

                                //Workout-ID der gespeicherten Session holen
                                String sessionWorkoutId;
                                if (sessionDoc.exists()) {
                                    sessionWorkoutId = sessionDoc.getString("workoutId");
                                } else {
                                    sessionWorkoutId = null;
                                }

                                //Prüfen ob selbes workout
                                boolean sameWorkout;
                                if (plannedWorkoutId.equals(sessionWorkoutId)) {
                                    sameWorkout = true;
                                } else {
                                    sameWorkout = false;
                                }

                                if (sessionDoc.exists() && hasExercises && sameWorkout) {
                                    workoutId = sessionWorkoutId;
                                    workoutName = sessionDoc.getString("workoutName");
                                    sessionExercises = (List<Map<String, Object>>) rawExercises;
                                    normalizeExercisesData(); // eigene Methode für normalisierung der Daten
                                    renderExercises(); // eigene Methode für rendern der Daten
                                } else { // Absicherung
                                    workoutId = plannedWorkoutId;
                                    workoutName = plannedWorkoutName;
                                    loadWorkoutDetails(userID);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Fehler beim Laden der Session: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden des geplanten Workouts: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    //Workout Details laden
    private void loadWorkoutDetails(String userID) {
        db.collection("users").document(userID)
                .collection("workouts").document(workoutId)
                .get()
                .addOnSuccessListener(workoutDoc -> {
                    if (!workoutDoc.exists()) {
                        Toast.makeText(this, "Workout wurde nicht gefunden (evtl. gelöscht)", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    // Übungsnamen aus Workout Dokument holen
                    Object rawNames = workoutDoc.get("exerciseNames");
                    List<String> exerciseNames = new ArrayList<>();
                    if (rawNames instanceof List) {
                        for (Object o : (List<?>) rawNames) {
                            if (o instanceof String) {
                                exerciseNames.add((String) o);
                            }
                        }
                    }

                    if (exerciseNames.isEmpty()) {
                        Toast.makeText(this, "Diesem Workout sind noch keine Übungen zugeordnet (im Workouts-Tab bearbeiten)", Toast.LENGTH_LONG).show();
                    }

                    //Metriken für die Übungen laden
                    loadExerciseCatalogAndBuildSession(userID, exerciseNames); //eigene Methode
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden des Workouts: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Übungs-Katalog laden und Session abrufen
    // holt Metriken für jede Übung aus der Exercise Collection
    private void loadExerciseCatalogAndBuildSession(String userID, List<String> exerciseNames) {
        db.collection("users").document(userID)
                .collection("exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    Map<String, List<String>> metricsByName = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        Object rawMetrics = doc.get("metrics");
                        List<String> metrics = new ArrayList<>();
                        if (rawMetrics instanceof List) {
                            for (Object o : (List<?>) rawMetrics) {
                                if (o instanceof String) {
                                    metrics.add((String) o);
                                }
                            }
                        }
                        if (name != null) {
                            metricsByName.put(name.trim(), metrics);
                        }
                    }

                    //Session-Übungen aufbauen
                    sessionExercises = new ArrayList<>();
                    for (String exerciseName : exerciseNames) {
                        String trimmedName = exerciseName.trim();
                        boolean foundInCatalog = metricsByName.containsKey(trimmedName);
                        List<String> metrics = metricsByName.get(trimmedName);
                        if (metrics == null) {
                            metrics = new ArrayList<>();
                        }

                        if (!foundInCatalog) {
                            Toast.makeText(this, "\"" + exerciseName + "\" wurde im Übungen-Katalog nicht gefunden", Toast.LENGTH_LONG).show();
                        } else if (metrics.isEmpty()) {
                            Toast.makeText(this, "\"" + exerciseName + "\" hat keine Metriken hinterlegt", Toast.LENGTH_LONG).show();
                        }

                        // Übungs eintrag für Session erstellen
                        Map<String, Object> exerciseEntry = new HashMap<>();
                        exerciseEntry.put("name", exerciseName);
                        exerciseEntry.put("metrics", metrics);

                        // Erster Leerer Satz automatisch hinzufügen
                        List<Map<String, Object>> sets = new ArrayList<>();
                        sets.add(new HashMap<>());
                        exerciseEntry.put("sets", sets);

                        sessionExercises.add(exerciseEntry);
                    }

                    saveSessionToFirestore(); // Session speichern
                    renderExercises(); // Session rendern
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden der Übungen: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // Sätze absichern für ältere Sessions die vor der Satz-Funktion gespeichert wurden
    private void normalizeExercisesData() {
        for (Map<String, Object> exercise : sessionExercises) {
            Object rawSets = exercise.get("sets");
            if (!(rawSets instanceof List) || ((List<?>) rawSets).isEmpty()) {
                List<Map<String, Object>> sets = new ArrayList<>();
                sets.add(new HashMap<>());
                exercise.put("sets", sets);
            }
        }
    }

    // Hilfsmethode: Metriken aus einem Übungs-Map holen
    private List<String> getMetrics(Map<String, Object> exercise) {
        Object rawMetrics = exercise.get("metrics");
        List<String> metrics = new ArrayList<>();
        if (rawMetrics instanceof List) {
            for (Object o : (List<?>) rawMetrics) {
                if (o instanceof String) {
                    metrics.add((String) o);
                }
            }
        }
        return metrics;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSets(Map<String, Object> exercise) {
        Object rawSets = exercise.get("sets");
        if (rawSets instanceof List) {
            return (List<Map<String, Object>>) rawSets;
        }
        List<Map<String, Object>> sets = new ArrayList<>();
        sets.add(new HashMap<>());
        exercise.put("sets", sets);
        return sets;
    }

    //Einheiten für die Metrik zurückgeben
    private String getMetricUnit(String metric) {
        switch (metric) {
            case "Gewicht":
                return "kg";
            case "Zeit":
                return "min";
            case "Distanz":
                return "km";
            case "Wiederholungen":
                return "Wdh.";
            default:
                return "";
        }
    }

    // Baut die komplette Eingabemaske für alle Übungen auf
    private void renderExercises() {
        if (workoutName != null) {
            tvTitle.setText("Session: " + workoutName);
        } else {
            tvTitle.setText("Session");
        }
        container.removeAllViews(); // UI leeren bevor neu aufgebaut wird
        inputRefs.clear();

        if (sessionExercises.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Keine Übungen in dieser Session.");
            container.addView(empty);
            return;
        }

        for (int i = 0; i < sessionExercises.size(); i++) {
            Map<String, Object> exercise = sessionExercises.get(i);
            String name = (String) exercise.get("name");
            List<String> metrics = getMetrics(exercise);

            // Übungsname als Titel
            TextView exerciseTitle = new TextView(this);
            if (name != null) {
                exerciseTitle.setText(name);
            } else {
                exerciseTitle.setText("Übung");
            }
            exerciseTitle.setTextSize(18);
            exerciseTitle.setPadding(0, 24, 0, 8);
            container.addView(exerciseTitle);

            // Keine Metriken -> Hinweis anzeigen, nächste Übung
            if (metrics.isEmpty()) {
                TextView noMetrics = new TextView(this);
                noMetrics.setText("Keine Metriken für diese Übung festgelegt (im Übungen-Tab bearbeiten)");
                container.addView(noMetrics);
                inputRefs.add(new ArrayList<>());
                continue;
            }

            // Ist die ausgewählte Metrik "Sets"
            boolean hasSets = metrics.contains(SETS_METRIC);
            //Metriken ohne "Sets" werden pro Satz angezeigt
            List<String> rowMetrics = new ArrayList<>(metrics);
            rowMetrics.remove(SETS_METRIC);

            // Container für die Satz zeilen
            LinearLayout setsContainer = new LinearLayout(this);
            setsContainer.setOrientation(LinearLayout.VERTICAL);
            container.addView(setsContainer);

            inputRefs.add(new ArrayList<>());

            final int exerciseIndex = i;

            renderSetsForExercise(exerciseIndex, setsContainer, rowMetrics, hasSets);//eigene Methode zum rendern der einzelnen Übungen

            // nur ein plus, wenn "Sets" als Metrik eingestellt
            if (hasSets) {
                Button btnAddSet = new Button(this);
                btnAddSet.setText("+ Satz hinzufügen");
                btnAddSet.setOnClickListener(v -> {
                    syncExerciseInputsToData(exerciseIndex);
                    getSets(sessionExercises.get(exerciseIndex)).add(new HashMap<>());
                    renderSetsForExercise(exerciseIndex, setsContainer, rowMetrics, true);
                });
                container.addView(btnAddSet);
            }
        }
    }

    // Satz Zeilen für eine Übung aufbauen
    private void renderSetsForExercise(int exerciseIndex, LinearLayout setsContainer, List<String> rowMetrics, boolean hasSets) {
        setsContainer.removeAllViews(); //alte Zeilen löschen
        inputRefs.get(exerciseIndex).clear();

        List<Map<String, Object>> sets = getSets(sessionExercises.get(exerciseIndex));

        for (int j = 0; j < sets.size(); j++) {
            Map<String, Object> setValues = sets.get(j);
            final int setIndex = j;

            // Zeile = hotizontales LinearLayout
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, 8, 0, 8);

            // "Satz 1" Label nur, wenn hasSets true ist
            if (hasSets) {
                TextView setLabel = new TextView(this);
                setLabel.setText("Satz " + (j + 1) + ": ");
                row.addView(setLabel);
            }

            Map<String, EditText> fieldsForSet = new HashMap<>();

            if (rowMetrics.isEmpty()) {
                TextView noOtherMetrics = new TextView(this);
                noOtherMetrics.setText("(keine weiteren Metriken hinterlegt)");
                row.addView(noOtherMetrics);
            }

            // Für jede Metrik ein Eingabefeld erstellen
            for (String metric : rowMetrics) {
                EditText input = new EditText(this);
                // GEÄNDERT: Hint zeigt wieder nur den reinen Metriknamen (ohne Einheit in Klammern)
                if (metric.equals("Wiederholungen")) {
                    input.setHint("Anzahl");
                } else {
                    input.setHint(metric);
                }
                input.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                // vorhandene Werte eintragen (wenn Session bereits besteht)
                Object existingValue = setValues.get(metric);
                if (existingValue != null) {
                    input.setText(existingValue.toString());
                }

                fieldsForSet.put(metric, input);
                row.addView(input);

                //Einheit rechts neben dem Eingabefeld anzeigen
                String unit = getMetricUnit(metric);
                if (!unit.isEmpty()) {
                    TextView unitLabel = new TextView(this);
                    unitLabel.setText(unit);
                    unitLabel.setPadding(8, 0, 16, 0);
                    row.addView(unitLabel);
                }
            }

            //"X" Button zum Löschen eines Satzes
            if (hasSets) {
                Button btnDeleteSet = new Button(this);
                btnDeleteSet.setText("X");
                btnDeleteSet.setOnClickListener(deleteClicked -> {
                    syncExerciseInputsToData(exerciseIndex);
                    List<Map<String, Object>> currentSets = getSets(sessionExercises.get(exerciseIndex));
                    if (setIndex < currentSets.size()) {
                        currentSets.remove(setIndex);
                    }
                    // mind. 1 Satz muss vorhanden sein
                    if (currentSets.isEmpty()) {
                        currentSets.add(new HashMap<>());
                    }
                    renderSetsForExercise(exerciseIndex, setsContainer, rowMetrics, true);
                });
                row.addView(btnDeleteSet);
            }

            inputRefs.get(exerciseIndex).add(fieldsForSet);
            setsContainer.addView(row);
        }
    }


    private void syncExerciseInputsToData(int exerciseIndex) {
        List<Map<String, Object>> sets = getSets(sessionExercises.get(exerciseIndex));
        List<Map<String, EditText>> fieldsPerSet = inputRefs.get(exerciseIndex);

        for (int j = 0; j < fieldsPerSet.size() && j < sets.size(); j++) {
            Map<String, EditText> fields = fieldsPerSet.get(j);
            Map<String, Object> setValues = new HashMap<>();
            for (Map.Entry<String, EditText> entry : fields.entrySet()) {
                setValues.put(entry.getKey(), entry.getValue().getText().toString().trim());
            }
            sets.set(j, setValues);
        }
    }

    // Session speichern
    private void saveSession() {
        for (int i = 0; i < sessionExercises.size(); i++) {
            if (i < inputRefs.size()) {
                syncExerciseInputsToData(i);
            }
        }
        saveSessionToFirestore();
    }

    // Session in Firestore speichern
    private void saveSessionToFirestore() {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("workoutId", workoutId);
        data.put("workoutName", workoutName);
        data.put("exercises", sessionExercises);

        db.collection("users").document(userID)
                .collection("sessions").document(date)
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Session gespeichert!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Speichern: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}