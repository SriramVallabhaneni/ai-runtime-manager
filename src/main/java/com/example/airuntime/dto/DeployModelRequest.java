package com.example.airuntime.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DeployModelRequest {

    @NotBlank(message = "Deployment name is required")
    @Pattern(
            regexp = "^[a-z0-9]([-a-z0-9]*[a-z0-9])?$",
            message = "Deployment name must be lowercase and Kubernetes-compatible"
    )
    private String deploymentName;

    @NotBlank(message = "Model name is required")
    private String model;

    @Min(value = 1, message = "Replicas must be at least 1")
    private int replicas;

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getModel() {
        return model;
    }

    public int getReplicas() {
        return replicas;
    }
}