package com.example.fitnesstracker;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail,tvWeightProfile, tvHeight;
    private Button btnEditProfile, btnSettings;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvWeightProfile = view.findViewById(R.id.tvWeightProfile);
        tvHeight = view.findViewById(R.id.tvHeight);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSettings = view.findViewById(R.id.btnSettings);

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnSettings.setOnClickListener(v-> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });
        loadProfileData();

        return view;
    }

    private void loadProfileData() {
        String userID = mAuth.getCurrentUser().getUid();

        String email = mAuth.getCurrentUser().getEmail();
        tvEmail.setText(email);

        db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        Double weight = documentSnapshot.getDouble("currentWeight");
                        Double height = documentSnapshot.getDouble("height");

                        if (username != null) {
                            tvUsername.setText(username);
                        }
                        if (weight != null) {
                            tvWeightProfile.setText("Aktuelles Gewicht: " + weight + " kg");
                        }
                        if (height != null) {
                            tvHeight.setText("Größe: " + height + " cm");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Laden", Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        EditText etUsername = dialogView.findViewById(R.id.etUsername);
        EditText etHeight = dialogView.findViewById(R.id.etHeight);

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Profil bearbeiten");
        builder.setView(dialogView);

        builder.setPositiveButton("Speichern", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String heightStr = etHeight.getText().toString().trim();

            if (username.isEmpty() && heightStr.isEmpty()) {
                Toast.makeText(getContext(), "Bitte mindestens ein Feld ausfüllen", Toast.LENGTH_SHORT).show();
                return;
            }

            saveProfileData(username, heightStr);
        });

        builder.setNegativeButton("Abbrechen", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveProfileData(String username, String heightStr) {
        String userID = mAuth.getCurrentUser().getUid();

        Map<String, Object> data = new HashMap<>();

        if (!username.isEmpty()) {
            data.put("username", username);
        }
        if (!heightStr.isEmpty()) {
            data.put("height", Double.parseDouble(heightStr));
        }

        db.collection("users").document(userID)
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid-> {
                    Toast.makeText(getContext(), "Profil gespeichert!", Toast.LENGTH_SHORT).show();
                    loadProfileData();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Fehler beim Speichern", Toast.LENGTH_SHORT).show();
                });
    }
}