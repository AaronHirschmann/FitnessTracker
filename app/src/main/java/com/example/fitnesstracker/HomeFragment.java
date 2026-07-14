package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.content.Intent; // NEU: zum Starten der SessionActivity
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

    private TextView tvCurrentWeight, tvLastWeight, tvTodayWorkout;
    private Button btnUpdateWeight, btnShowPlannedWorkout, btnAddWorkout, btnStartSession; // NEU: btnStartSession

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
        btnStartSession = view.findViewById(R.id.btnStartSession); // NEU

        btnUpdateWeight.setOnClickListener(v -> showUpdateWeightDialog());
        btnAddWorkout.setOnClickListener(v -> showSelectWorkoutDialog());
        btnStartSession.setOnClickListener(v -> startSession()); // NEU

        loadUserData();
        loadTodayWorkout();

        return view;
    }

    private void loadUserData() {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                Double currentWeight = document.getDouble("currentWeight");
                Double lastWeight = document.getDouble("lastWeight");

                if (currentWeight != null) {
                    tvCurrentWeight.setText(currentWeight + " kg");
                }
                if (lastWeight != null) {
                    tvLastWeight.setText("Letztes Gewicht: " + lastWeight + " kg");
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
        });
    }

    private void showUpdateWeightDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_update_weight, null);
        EditText etWeight = dialogView.findViewById(R.id.etWeight);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String weightStr = etWeight.getText().toString().trim();
            if (!weightStr.isEmpty()) {
                saveWeight(Double.parseDouble(weightStr));
            } else {
                Toast.makeText(getContext(), "Bitte Gewicht eingeben", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveWeight(double newWeight) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> data = new HashMap<>();

                    if (documentSnapshot.exists() && documentSnapshot.getDouble("currentWeight") != null) {
                        data.put("lastWeight", documentSnapshot.getDouble("currentWeight"));
                    }

                    data.put("currentWeight", newWeight);

                    db.collection("users").document(userID)
                            .set(data, com.google.firebase.firestore.SetOptions.merge())
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

    private String getTodayDateString() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }

    private void loadTodayWorkout() {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String workoutName = documentSnapshot.getString("workoutName");
                        if (workoutName != null) {
                            tvTodayWorkout.setText("Heutiges Workout: " + workoutName);
                        }
                    } else {
                        tvTodayWorkout.setText("Kein Workout für heute geplant");
                    }
                })
                .addOnFailureListener(e -> {
                    tvTodayWorkout.setText("Fehler beim Laden");
                });
    }

    private void showSelectWorkoutDialog() {
        String userID = mAuth.getCurrentUser().getUid();

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

                    List<String> workoutNames = new ArrayList<>();
                    for (Workout w : workouts) {
                        workoutNames.add(w.getName());
                    }

                    Spinner spinner = new Spinner(getContext());
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_dropdown_item, workoutNames);
                    spinner.setAdapter(spinnerAdapter);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Workout für heute auswählen");
                    builder.setView(spinner);

                    builder.setPositiveButton("Speichern", (dialog, which) -> {
                        int selectedIndex = spinner.getSelectedItemPosition();
                        Workout selectedWorkout = workouts.get(selectedIndex);
                        assignWorkoutToToday(selectedWorkout);
                    });

                    builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

                    builder.show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Laden der Workouts", Toast.LENGTH_SHORT).show());
    }

    private void assignWorkoutToToday(Workout workout) {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString();

        Map<String, Object> data = new HashMap<>();
        data.put("workoutId", workout.getID());
        data.put("workoutName", workout.getName());

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout für heute gespeichert!", Toast.LENGTH_SHORT).show();
                    loadTodayWorkout();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show());
    }

    // NEU: prüft, ob für heute ein Workout geplant ist, und startet dann die SessionActivity dafür
    private void startSession() {
        String userID = mAuth.getCurrentUser().getUid();
        String today = getTodayDateString();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(today)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(getContext(), "Bitte zuerst ein Workout für heute auswählen", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(getActivity(), SessionActivity.class);
                    intent.putExtra("date", today);
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Prüfen des Workouts", Toast.LENGTH_SHORT).show());
    }
}