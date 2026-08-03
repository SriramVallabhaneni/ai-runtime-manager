package com.example.airuntime.crd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

public class Metadata {

    private String name;

    public String getName() {
        return name;
    }
}