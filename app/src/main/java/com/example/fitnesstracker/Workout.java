package com.example.fitnesstracker;

import java.util.ArrayList;
import java.util.List;

public class Workout {
    private String id;
    private String name;
    private List<String> exerciseNames;

    public Workout() {}

    public Workout(String id, String name) {
        this.id = id;
        this.name = name;
        this.exerciseNames = new ArrayList<>();
    }

    public Workout(String id, String name, List<String> exerciseNames) {
        this.id = id;
        this.name = name;
        if (exerciseNames != null) {
            this.exerciseNames = exerciseNames;
        } else {
            this.exerciseNames = new ArrayList<>();
        }
    }

    public String getID() {
        return id;
    }
    public void setID(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<String> getExerciseNames() {
        return exerciseNames;
    }
    public void setExerciseNames(List<String> exerciseNames) {
        this.exerciseNames = exerciseNames;
    }
}