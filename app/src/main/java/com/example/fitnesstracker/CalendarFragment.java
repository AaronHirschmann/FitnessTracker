package com.example.fitnesstracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvSelectedDate, tvWorkoutOnDate;
    private Button btnAddWorkoutToDate;
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
        btnAddWorkoutToDate = view.findViewById(R.id.btnAddWorkoutToDate);


        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            selectedDate = year + "-" + (month + 1) + "-" + dayOfMonth;
            tvSelectedDate.setText("Ausgewähltes Datum: " + selectedDate);
            loadWorkoutForDate(selectedDate);
        });

        return view;
    }

    private void loadWorkoutForDate(String date) {
        String userID = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userID)
                .collection("plannedWorkouts").document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String workoutName = documentSnapshot.getString("workoutName");
                        if (workoutName != null) {
                            tvWorkoutOnDate.setText("Workout: " + workoutName);
                        }
                    } else {
                        tvWorkoutOnDate.setText("Kein Workout geplant");
                    }
                })
                .addOnFailureListener(e -> {
                    tvWorkoutOnDate.setText("Fehler beim Laden");
                });
    }

    private void showAddWorkoutDialog() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Bitte zuerst ein Datum auswählen", Toast.LENGTH_SHORT).show();
            return;
        }

    }
}