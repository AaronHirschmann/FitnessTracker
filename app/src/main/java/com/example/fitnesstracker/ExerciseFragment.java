package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.widget.ImageButton;
import android.graphics.Color;

public class ExerciseFragment extends Fragment {

    private final List<Exercise> exerciseList = new ArrayList<>();
    private LinearLayout exerciseContainer;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        exerciseContainer = view.findViewById(R.id.container_exercises);

        loadExercises();

        View fabAddExercise = view.findViewById(R.id.fab_add_exercise);
        fabAddExercise.setOnClickListener(v -> showAddExerciseDialog());
    }

    private void loadExercises() {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("exercises")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    exerciseList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<String> metrics = (List<String>) doc.get("metrics");
                        exerciseList.add(new Exercise(id, name, metrics));
                    }
                    renderExercises();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
                });
    }

    private void renderExercises() {
        exerciseContainer.removeAllViews();

        if (exerciseList.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Noch keine Übungen angelegt.");
            exerciseContainer.addView(empty);
            return;
        }

        for (Exercise exercise : exerciseList) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(8, 24, 8, 24);

            TextView text = new TextView(requireContext());
            text.setText(exercise.getName());
            text.setTextSize(16);
            text.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            ImageButton btnEdit = new ImageButton(requireContext());
            btnEdit.setImageResource(android.R.drawable.ic_menu_edit);
            btnEdit.setBackgroundColor(Color.TRANSPARENT);
            btnEdit.setContentDescription("Übung bearbeiten");
            btnEdit.setOnClickListener(v -> showEditExerciseDialog(exercise));

            ImageButton btnDelete = new ImageButton(requireContext());
            btnDelete.setImageResource(android.R.drawable.ic_menu_delete);
            btnDelete.setBackgroundColor(Color.TRANSPARENT);
            btnDelete.setContentDescription("Übung löschen");
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            deleteParams.leftMargin = 8;
            btnDelete.setLayoutParams(deleteParams);
            btnDelete.setOnClickListener(v -> deleteExercise(exercise));

            row.addView(text);
            row.addView(btnEdit);
            row.addView(btnDelete);
            exerciseContainer.addView(row);

            // Dünne Trennlinie zwischen den Zeilen
            View divider = new View(requireContext());
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2));
            divider.setBackgroundColor(Color.LTGRAY);
            exerciseContainer.addView(divider);
        }
    }

    private void saveExercise(String name, List<String> metrics) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("metrics", metrics);

        db.collection("users").document(userID)
                .collection("exercises")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Übung gespeichert!", Toast.LENGTH_SHORT).show();
                    loadExercises();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteExercise(Exercise exercise) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("exercises").document(exercise.getID())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Übung gelöscht!", Toast.LENGTH_SHORT).show();
                    loadExercises();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Löschen", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExercise(Exercise exercise, String newName, List<String> newMetrics) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);
        data.put("metrics", newMetrics);

        db.collection("users").document(userID)
                .collection("exercises").document(exercise.getID())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Übung aktualisiert", Toast.LENGTH_SHORT).show();
                    loadExercises();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Aktualisieren", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddExerciseDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_exercise, null);

        TextInputEditText inputName = dialogView.findViewById(R.id.edittext_exercise_name);
        CheckBox checkWeight = dialogView.findViewById(R.id.checkbox_metric_weight);
        CheckBox checkReps = dialogView.findViewById(R.id.checkbox_metric_reps);
        CheckBox checkSets = dialogView.findViewById(R.id.checkbox_metric_sets);
        CheckBox checkTime = dialogView.findViewById(R.id.checkbox_metric_time);
        CheckBox checkDistance = dialogView.findViewById(R.id.checkbox_metric_distance);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Übung hinzufügen");
        builder.setView(dialogView);

        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> metrics = new ArrayList<>();
            if (checkWeight.isChecked()) metrics.add("Gewicht");
            if (checkReps.isChecked()) metrics.add("Wiederholungen");
            if (checkSets.isChecked()) metrics.add("Sätze");
            if (checkTime.isChecked()) metrics.add("Zeit");
            if (checkDistance.isChecked()) metrics.add("Distanz");

            saveExercise(name, metrics);
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void showEditExerciseDialog(Exercise exercise) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_exercise, null);

        TextInputEditText inputName = dialogView.findViewById(R.id.edittext_exercise_name);
        CheckBox checkWeight = dialogView.findViewById(R.id.checkbox_metric_weight);
        CheckBox checkReps = dialogView.findViewById(R.id.checkbox_metric_reps);
        CheckBox checkSets = dialogView.findViewById(R.id.checkbox_metric_sets);
        CheckBox checkTime = dialogView.findViewById(R.id.checkbox_metric_time);
        CheckBox checkDistance = dialogView.findViewById(R.id.checkbox_metric_distance);

        inputName.setText(exercise.getName());
        checkWeight.setChecked(exercise.getMetrics().contains("Gewicht"));
        checkReps.setChecked(exercise.getMetrics().contains("Wiederholungen"));
        checkSets.setChecked(exercise.getMetrics().contains("Sätze"));
        checkTime.setChecked(exercise.getMetrics().contains("Zeit"));
        checkDistance.setChecked(exercise.getMetrics().contains("Distanz"));

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Übung bearbeiten");
        builder.setView(dialogView);

        builder.setPositiveButton("Aktualisieren", (dialog, which) -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> metrics = new ArrayList<>();
            if (checkWeight.isChecked()) metrics.add("Gewicht");
            if (checkReps.isChecked()) metrics.add("Wiederholungen");
            if (checkSets.isChecked()) metrics.add("Sätze");
            if (checkTime.isChecked()) metrics.add("Zeit");
            if (checkDistance.isChecked()) metrics.add("Distanz");

            updateExercise(exercise, name, metrics);
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }
}