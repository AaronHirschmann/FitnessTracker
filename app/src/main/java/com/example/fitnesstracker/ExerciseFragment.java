package com.example.fitnesstracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
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

public class ExerciseFragment extends Fragment implements ExerciseAdapter.OnExerciseActionListener {

    private ExerciseAdapter adapter;
    private final List<Exercise> exerciseList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Firebase initialisieren
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewExercises);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ExerciseAdapter(exerciseList, this);
        recyclerView.setAdapter(adapter);

        // Übungen aus Firebase laden statt Testdaten
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
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
                });
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
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_exercise);

        TextInputEditText inputName = dialog.findViewById(R.id.edittext_exercise_name);
        CheckBox checkWeight = dialog.findViewById(R.id.checkbox_metric_weight);
        CheckBox checkReps = dialog.findViewById(R.id.checkbox_metric_reps);
        CheckBox checkSets = dialog.findViewById(R.id.checkbox_metric_sets);
        CheckBox checkTime = dialog.findViewById(R.id.checkbox_metric_time);
        CheckBox checkDistance = dialog.findViewById(R.id.checkbox_metric_distance);
        Button btnSave = dialog.findViewById(R.id.btn_save_exercise);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_exercise);

        btnSave.setOnClickListener(v -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                return;
            }

            List<String> metrics = new ArrayList<>();
            if (checkWeight.isChecked()) metrics.add("Gewicht");
            if (checkReps.isChecked()) metrics.add("Wiederholungen");
            if (checkSets.isChecked()) metrics.add("Sätze");
            if (checkTime.isChecked()) metrics.add("Zeit");
            if (checkDistance.isChecked()) metrics.add("Distanz");

            saveExercise(name, metrics);
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // NEU: die fehlende Methode
    private void showEditExerciseDialog(Exercise exercise) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_exercise);

        TextInputEditText inputName = dialog.findViewById(R.id.edittext_exercise_name);
        CheckBox checkWeight = dialog.findViewById(R.id.checkbox_metric_weight);
        CheckBox checkReps = dialog.findViewById(R.id.checkbox_metric_reps);
        CheckBox checkSets = dialog.findViewById(R.id.checkbox_metric_sets);
        CheckBox checkTime = dialog.findViewById(R.id.checkbox_metric_time);
        CheckBox checkDistance = dialog.findViewById(R.id.checkbox_metric_distance);
        Button btnSave = dialog.findViewById(R.id.btn_save_exercise);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_exercise);

        inputName.setText(exercise.getName());
        checkWeight.setChecked(exercise.getMetrics().contains("Gewicht"));
        checkReps.setChecked(exercise.getMetrics().contains("Wiederholungen"));
        checkSets.setChecked(exercise.getMetrics().contains("Sätze"));
        checkTime.setChecked(exercise.getMetrics().contains("Zeit"));
        checkDistance.setChecked(exercise.getMetrics().contains("Distanz"));

        btnSave.setText("Aktualisieren");

        btnSave.setOnClickListener(v -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                return;
            }

            List<String> metrics = new ArrayList<>();
            if (checkWeight.isChecked()) metrics.add("Gewicht");
            if (checkReps.isChecked()) metrics.add("Wiederholungen");
            if (checkSets.isChecked()) metrics.add("Sätze");
            if (checkTime.isChecked()) metrics.add("Zeit");
            if (checkDistance.isChecked()) metrics.add("Distanz");

          updateExercise(exercise, name, metrics);
          dialog.dismiss();

        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditClicked(Exercise exercise) {
        showEditExerciseDialog(exercise);   // ← ruft jetzt tatsächlich was auf
    }

    @Override
    public void onDeleteClicked(Exercise exercise) {
        deleteExercise(exercise);
        }
}