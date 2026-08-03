package com.example.airuntime.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

public class AIModelDeployment {

    private Metadata metadata;
    private Spec spec;
    private Status status;

    public Metadata getMetadata() {
        return metadata;
    }

    public Spec getSpec() {
        return spec;
    }

    public Status getStatus() {
        return status;
    }
}