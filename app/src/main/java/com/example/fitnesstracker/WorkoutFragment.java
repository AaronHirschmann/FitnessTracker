package com.example.fitnesstracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

        // Firebase initialisieren
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewWorkouts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new WorkoutAdapter(workoutList, this);
        recyclerView.setAdapter(adapter);

        // Workouts aus Firebase laden statt Testdaten
        loadWorkouts();

        View fabAddWorkout = view.findViewById(R.id.fab_add_workout);
        fabAddWorkout.setOnClickListener(v -> showAddWorkoutDialog());
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
                        workoutList.add(new Workout(id, name));
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveWorkout(String name) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);

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

    private void updateWorkout(Workout workout, String newName) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("name", newName);

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

    private void showAddWorkoutDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_workout);

        TextInputEditText inputName = dialog.findViewById(R.id.edittext_workout_name);
        Button btnSave = dialog.findViewById(R.id.btn_save_workout);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_workout);

        btnSave.setOnClickListener(v -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }

            if (name.isEmpty()) {
                return;
            }

            saveWorkout(name);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showEditWorkoutDialog(Workout workout) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_workout);

        TextInputEditText inputName = dialog.findViewById(R.id.edittext_workout_name);
        Button btnSave = dialog.findViewById(R.id.btn_save_workout);
        Button btnCancel = dialog.findViewById(R.id.btn_cancel_workout);

        // Aktuellen Namen vorab eintragen
        inputName.setText(workout.getName());
        btnSave.setText("Aktualisieren");

        btnSave.setOnClickListener(v -> {
            String name = "";
            if (inputName.getText() != null) {
                name = inputName.getText().toString().trim();
            }
            if (name.isEmpty()) {
                return;
            }
            updateWorkout(workout, name);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditClicked(Workout workout) {
        showEditWorkoutDialog(workout);
    }

    @Override
    public void onDeleteClicked(Workout workout) {
        deleteWorkout(workout);
    }
}