package com.example.airuntime.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

public class Spec {

    private String model;
    private int replicas;
    private String storageSize;

    public String getModel() {
        return model;
    }

    public int getReplicas() {
        return replicas;
    }

    public String getStorageSize() {
        return storageSize;
    }
}