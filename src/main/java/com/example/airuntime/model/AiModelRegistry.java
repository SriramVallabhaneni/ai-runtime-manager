package com.example.airuntime.model;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AiModelRegistry {

    private final Map<String, AiModel> models = Map.of(
            "tinyllama", new AiModel("ollama/ollama", 11434),
            "llama3", new AiModel("ollama/ollama", 11434)
    );

    public AiModel get(String modelName) {
        return models.get(modelName);
    }
}