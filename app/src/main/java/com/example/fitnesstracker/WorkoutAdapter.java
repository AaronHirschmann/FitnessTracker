package com.example.fitnesstracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    public interface OnWorkoutActionListener {
        void onEditClicked(Workout workout);
        void onDeleteClicked(Workout workout);
    }

    private final List<Workout> workoutList;
    private final OnWorkoutActionListener listener;

    public WorkoutAdapter(List<Workout> workoutList, OnWorkoutActionListener listener) {
        this.workoutList = workoutList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        Workout workout = workoutList.get(position);
        holder.textName.setText(workout.getName());
        holder.btnEdit.setOnClickListener(v -> listener.onEditClicked(workout));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(workout));
    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        ImageButton btnEdit;
        ImageButton btnDelete;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textWorkoutName);
            btnEdit = itemView.findViewById(R.id.btnEditWorkout);
            btnDelete = itemView.findViewById(R.id.btnDeleteWorkout);
        }
    }
}