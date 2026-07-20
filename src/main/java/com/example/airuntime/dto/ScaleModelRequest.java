package com.example.airuntime.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public class ScaleModelRequest {

    @Min(value = 1, message = "Replicas must be at least 1")
    @Max(value = 1, message = "Multiple replicas are not currently supported")
    private int replicas;

    public int getReplicas() {
        return replicas;
    }
}
