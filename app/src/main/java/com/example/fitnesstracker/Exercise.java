package com.example.fitnesstracker;

import java.util.ArrayList;
import java.util.List;

public class Exercise {
    private String name;
    private List<String> metrics;

    public Exercise(String name, List<String> metrics) {
        this.name = name;
        this.metrics = metrics != null ? metrics : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<String> metrics) {
        this.metrics = metrics;
    }
}
