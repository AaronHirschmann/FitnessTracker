package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvCurrentWeight, tvLastWeight, tvTodayWorkout;
    private Button btnUpdateWeight, btnShowPlannedWorkout, btnAddWorkout;
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

        btnUpdateWeight.setOnClickListener(v -> showUpdateWeightDialog());

        loadUserData();


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

        })
        .addOnFailureListener(e -> {
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
}