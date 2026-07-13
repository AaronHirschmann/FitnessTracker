package com.example.fitnesstracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class ExerciseFragment extends Fragment implements ExerciseAdapter.OnExerciseActionListener {

    private ExerciseAdapter adapter;
    private final List<Exercise> exerciseList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewExercises);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new ExerciseAdapter(exerciseList, this);
        recyclerView.setAdapter(adapter);

        // Testdaten
        exerciseList.add(new Exercise("Bankdrücken", new ArrayList<>()));
        exerciseList.add(new Exercise("Kniebeuge", new ArrayList<>()));
        exerciseList.add(new Exercise("Kreuzheben", new ArrayList<>()));
        exerciseList.add(new Exercise("Rudern", new ArrayList<>()));
        exerciseList.add(new Exercise("Klimmzüge", new ArrayList<>()));
        exerciseList.add(new Exercise("Liegestütze", new ArrayList<>()));
        exerciseList.add(new Exercise("Dips", new ArrayList<>()));
        exerciseList.add(new Exercise("Laufen", new ArrayList<>()));
        exerciseList.add(new Exercise("Schwimmen", new ArrayList<>()));
        adapter.notifyDataSetChanged();

        View fabAddExercise = view.findViewById(R.id.fab_add_exercise);
        fabAddExercise.setOnClickListener(v -> showAddExerciseDialog());
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
            String name = inputName.getText().toString().trim();
            if (name.isEmpty()) {
                return;
            }

            List<String> metrics = new ArrayList<>();
            if (checkWeight.isChecked()) metrics.add("Gewicht");
            if (checkReps.isChecked()) metrics.add("Wiederholungen");
            if (checkSets.isChecked()) metrics.add("Sätze");
            if (checkTime.isChecked()) metrics.add("Zeit");
            if (checkDistance.isChecked()) metrics.add("Distanz");

            exerciseList.add(new Exercise(name, metrics));
            adapter.notifyItemInserted(exerciseList.size() - 1);
            dialog.dismiss();
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    public void onEditClicked(Exercise exercise) {
        // TODO: Bearbeiten-Dialog öffnen
    }

    @Override
    public void onDeleteClicked(Exercise exercise) {
        int index = exerciseList.indexOf(exercise);
        if (index != -1) {
            exerciseList.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }
}