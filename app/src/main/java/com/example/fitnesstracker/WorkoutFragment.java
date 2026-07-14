package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkoutFragment extends Fragment implements WorkoutAdapter.OnWorkoutActionListener {

    private WorkoutAdapter adapter;
    private final List<Workout> workoutList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewWorkouts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new WorkoutAdapter(workoutList, this);
        recyclerView.setAdapter(adapter);

        loadWorkouts();

        View fabAddWorkout = view.findViewById(R.id.fab_add_workout);
        fabAddWorkout.setOnClickListener(v -> showWorkoutDialog(null));
    }

    private void loadWorkouts() {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("workouts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    workoutList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<String> exerciseNames = (List<String>) doc.get("exerciseNames");
                        workoutList.add(new Workout(id, name, exerciseNames));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveWorkout(String name, List<String> exerciseNames) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("exerciseNames", exerciseNames);

        db.collection("users").document(userID)
                .collection("workouts")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Workout gespeichert!", Toast.LENGTH_SHORT).show();
                    loadWorkouts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteWorkout(Workout workout) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("workouts").document(workout.getID())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout gelöscht!", Toast.LENGTH_SHORT).show();
                    loadWorkouts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateWorkout(Workout workout, String newName, List<String> newExerciseNames) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);
        data.put("exerciseNames", newExerciseNames);

        db.collection("users").document(userID)
                .collection("workouts").document(workout.getID())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout aktualisiert!", Toast.LENGTH_SHORT).show();
                    loadWorkouts();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Aktualisieren", Toast.LENGTH_SHORT).show();
                });
    }

    // Ein Dialog für Neu-Anlegen UND Bearbeiten: existingWorkout == null -> neues Workout
    private void showWorkoutDialog(@Nullable Workout existingWorkout) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_workout, null);

        TextInputEditText inputName = dialogView.findViewById(R.id.edittext_workout_name);
        LinearLayout exerciseContainer = dialogView.findViewById(R.id.container_workout_exercises);
        Spinner spinner = dialogView.findViewById(R.id.spinner_workout_exercise_select);
        Button btnAddExercise = dialogView.findViewById(R.id.btn_add_workout_exercise);

        // Arbeitskopie der zugeordneten Übungen (leer bei neuem Workout)
        List<String> currentExerciseNames = new ArrayList<>();
        if (existingWorkout != null) {
            inputName.setText(existingWorkout.getName());
            if (existingWorkout.getExerciseNames() != null) {
                currentExerciseNames.addAll(existingWorkout.getExerciseNames());
            }
        }
        showExerciseRows(exerciseContainer, currentExerciseNames);

        // Übungs-Katalog laden und ins Dropdown packen
        String userID = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userID)
                .collection("exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> allExerciseNames = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("name");
                        if (name != null) {
                            allExerciseNames.add(name);
                        }
                    }

                    if (allExerciseNames.isEmpty()) {
                        Toast.makeText(getContext(), "Bitte zuerst im Übungen-Tab eine Übung anlegen", Toast.LENGTH_LONG).show();
                        return;
                    }

                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, allExerciseNames);
                    spinner.setAdapter(spinnerAdapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Laden der Übungen", Toast.LENGTH_SHORT).show());

        btnAddExercise.setOnClickListener(v -> {
            if (spinner.getSelectedItem() == null) {
                return;
            }
            String selectedName = spinner.getSelectedItem().toString();

            if (currentExerciseNames.contains(selectedName)) {
                Toast.makeText(getContext(), "Übung ist bereits zugeordnet", Toast.LENGTH_SHORT).show();
                return;
            }

            currentExerciseNames.add(selectedName);
            showExerciseRows(exerciseContainer, currentExerciseNames);
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(existingWorkout == null ? "Workout hinzufügen" : "Workout bearbeiten");
        builder.setView(dialogView);

        builder.setPositiveButton(existingWorkout == null ? "Speichern" : "Aktualisieren", (dialog, which) -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existingWorkout == null) {
                saveWorkout(name, currentExerciseNames);
            } else {
                updateWorkout(existingWorkout, name, currentExerciseNames);
            }
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    // Baut die Liste der zugeordneten Übungen als einfache Zeilen (Text + Löschen-Button) neu auf
    private void showExerciseRows(LinearLayout container, List<String> exerciseNames) {
        container.removeAllViews();

        for (String exerciseName : exerciseNames) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView text = new TextView(requireContext());
            text.setText(exerciseName);
            text.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button btnDelete = new Button(requireContext());
            btnDelete.setText("X");
            btnDelete.setOnClickListener(v -> {
                exerciseNames.remove(exerciseName);
                showExerciseRows(container, exerciseNames);
            });

            row.addView(text);
            row.addView(btnDelete);
            container.addView(row);
        }
    }

    @Override
    public void onEditClicked(Workout workout) {
        showWorkoutDialog(workout);
    }

    @Override
    public void onDeleteClicked(Workout workout) {
        deleteWorkout(workout);
    }
}