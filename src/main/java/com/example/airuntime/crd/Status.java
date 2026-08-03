package com.example.airuntime.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

public class Status {

    private String phase;
    private boolean ready;
    private String message;

    public String getPhase() {
        return phase;
    }

    public boolean isReady() {
        return ready;
    }

    public String getMessage() {
        return message;
    }
}