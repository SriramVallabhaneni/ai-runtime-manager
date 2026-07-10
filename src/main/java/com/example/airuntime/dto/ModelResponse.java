package com.example.airuntime.dto;

public class ModelResponse {
    private String name;
    private int replicas;
    private int availableReplicas;
    private String status;

    public ModelResponse(String name, int replicas, int availableReplicas, String status) {
        this.name = name;
        this.replicas = replicas;
        this.availableReplicas = availableReplicas;
        this.status = status;
    }

    public String getName() { return name; }
    public int getReplicas() { return replicas; }
    public int getAvailableReplicas() { return availableReplicas; }
    public String getStatus() { return status; }
}
