package com.example.fitnesstracker;

public class Workout {
    private String id;
    private String name;

    public Workout() {}
    public Workout(String id, String name) {
        this.id = id;
        this.name = name;
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
}