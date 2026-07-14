package com.example.fitnesstracker;

import java.util.List;
import java.util.ArrayList;

public class Exercise {
    private String id;
    private String name;
    private List<String> metrics;

    public Exercise() {}

    public Exercise(String id, String name, List<String> metrics) {
        this.id = id;
        this.name = name;
        if (metrics != null) {
            this.metrics = metrics;
        } else {
            this.metrics = new ArrayList<>();
        }
    }

    public String getID() { return id; }
    public String getName() {
        return name;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }
}