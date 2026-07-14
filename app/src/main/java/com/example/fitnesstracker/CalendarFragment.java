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

    private CalendarView calendarView;
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


        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            tvSelectedDate.setText("Ausgewähltes Datum: " + selectedDate);
            loadWorkoutForDate(selectedDate);
        });

        btnAddWorkoutToDate.setOnClickListener(v -> showAddWorkoutDialog());
        btnRemoveWorkoutFromDate.setOnClickListener(v -> removeWorkoutFromDate());

        String today = getTodayDateString();
        tvSelectedDate.setText("Ausgewähltes Datum " + today);
        selectedDate = today;
        loadWorkoutForDate(today);

        return view;
    }

    private String getTodayDateString() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        return year + "-" + month + "-" + day;
    }
    private void loadWorkoutForDate(String date) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String workoutName = documentSnapshot.getString("workoutName");
                        String workoutId = documentSnapshot.getString("workoutId");
                        btnRemoveWorkoutFromDate.setVisibility(View.VISIBLE);
                        if (workoutName != null) {
                            tvWorkoutOnDate.setText("Workout: " + workoutName);
                        }
                        if (workoutId != null) {
                            loadExercisesForWorkout(userID, workoutId);
                        }
                    } else {
                        tvWorkoutOnDate.setText("Kein Workout geplant");
                        tvExercisesOnDate.setText("");
                        btnRemoveWorkoutFromDate.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvWorkoutOnDate.setText("Fehler beim Laden");
                });
    }

    private void loadExercisesForWorkout(String userID, String workoutId) {
        db.collection("users").document(userID)
                .collection("workouts").document(workoutId)
                .get()
                .addOnSuccessListener(workoutDoc -> {
                    if (workoutDoc.exists()) {
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
                            tvExercisesOnDate.setText("Keine Übungen in diesem Workout");
                        } else {
                            StringBuilder sb = new StringBuilder();
                            for (String name : exerciseNames) {
                                sb.append("• ").append(name).append("\n");
                            }
                            tvExercisesOnDate.setText(sb.toString().trim());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvExercisesOnDate.setText("Fehler beim Laden der Übungen");
                });
    }

    private void removeWorkoutFromDate() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Bitte zuerst ein Datum auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(selectedDate)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout entfernt!", Toast.LENGTH_SHORT).show();
                    tvWorkoutOnDate.setText("Kein Workout geplant");
                    tvExercisesOnDate.setText("");
                    btnRemoveWorkoutFromDate.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Entfernen", Toast.LENGTH_SHORT).show();
                });
    }

    private void showAddWorkoutDialog() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Bitte zuerst ein Datum auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

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
                    builder.setTitle("Workout für " + selectedDate + " zuweisen");
                    builder.setView(spinner);

                    builder.setPositiveButton("Speichern", (dialog, which) -> {
                        int selectedIndex = spinner.getSelectedItemPosition();
                        Workout selectedWorkout = workouts.get(selectedIndex);
                        assignWorkoutToDate(selectedDate, selectedWorkout);
                    });

                    builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

                    builder.show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler beim Laden der Workouts", Toast.LENGTH_SHORT).show());
    }

    private void assignWorkoutToDate(String date, Workout workout) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();
        data.put("workoutId", workout.getID());
        data.put("workoutName", workout.getName());

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Workout geplant!", Toast.LENGTH_SHORT).show();
                    loadWorkoutForDate(date);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                });
    }
}