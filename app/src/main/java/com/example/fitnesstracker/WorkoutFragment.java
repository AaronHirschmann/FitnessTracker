package com.example.fitnesstracker;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class WorkoutFragment extends Fragment implements WorkoutAdapter.OnWorkoutActionListener {

    private WorkoutAdapter adapter;
    private final List<Workout> workoutList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_workout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerViewWorkouts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Testdaten vor dem Adapter-Aufbau einfügen (kein notify... nötig)
        workoutList.add(new Workout("Push Day"));
        workoutList.add(new Workout("Pull Day"));
        workoutList.add(new Workout("Leg Day"));
        workoutList.add(new Workout("Upper 1"));
        workoutList.add(new Workout("Upper 2"));
        workoutList.add(new Workout("Lower"));
        workoutList.add(new Workout("Ausdauer"));

        adapter = new WorkoutAdapter(workoutList, this);
        recyclerView.setAdapter(adapter);

        View fabAddWorkout = view.findViewById(R.id.fab_add_workout);
        fabAddWorkout.setOnClickListener(v -> showAddWorkoutDialog());
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

            workoutList.add(new Workout(name));
            adapter.notifyItemInserted(workoutList.size() - 1);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onEditClicked(Workout workout) {
        // TODO: Bearbeiten-Dialog öffnen (später)
    }

    @Override
    public void onDeleteClicked(Workout workout) {
        int index = workoutList.indexOf(workout);
        if (index != -1) {
            workoutList.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }
}