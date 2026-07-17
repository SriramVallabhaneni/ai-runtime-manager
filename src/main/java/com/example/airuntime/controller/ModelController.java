package com.example.airuntime.controller;

import com.example.airuntime.dto.DeployModelRequest;
import com.example.airuntime.service.KubernetesDeploymentService;
import com.example.airuntime.dto.ModelResponse;
import com.example.airuntime.dto.ScaleModelRequest;
import com.example.airuntime.dto.UpdateImageRequest;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/models")
public class ModelController {

    private final KubernetesDeploymentService service;

    public ModelController(KubernetesDeploymentService service) {
        this.service = service;
    }

    @Operation(
            summary = "Deploy a supported AI model",
            description = "Creates a Kubernetes Deployment and Service for a supported AI runtime."
    )
    @PostMapping
    public String deployModel(@Valid @RequestBody DeployModelRequest request) throws Exception {
        return service.deployModel(request);
    }

    @GetMapping
    public List<ModelResponse> listModels() throws Exception {
        return service.listModels();
    }

    @GetMapping("/{name}")
    public ModelResponse getModel(@PathVariable String name) throws Exception {
        return service.getModel(name);
    }

    @Operation(
            summary = "Delete deployment",
            description = "Deletes both the Deployment and its Service."
    )
    @DeleteMapping("/{name}")
    public String deleteModel(@PathVariable String name) throws Exception {
        return service.deleteModel(name);
    }

    @Operation(
            summary = "Scale a deployment",
            description = "Updates the replica count of an existing deployment."
    )
    @PatchMapping("/{name}/scale")
    public ModelResponse scaleModel(
            @PathVariable String name,
            @Valid @RequestBody ScaleModelRequest request
    ) throws Exception {
        return service.scaleModel(name, request);
    }

    @Operation(
            summary = "Update container image",
            description = "Performs a rolling update by changing the deployment image."
    )
    @PatchMapping("/{name}/image")
    public ModelResponse updateImage(
            @PathVariable String name,
            @Valid @RequestBody UpdateImageRequest request
    ) throws Exception {
        return service.updateImage(name, request);
    }

    @Operation(
            summary = "Restart deployment",
            description = "Triggers a rollout restart of the deployment."
    )
   @PostMapping("/{name}/restart")
    public ModelResponse restartModel(@PathVariable String name) throws Exception {
        return service.restartModel(name);
   }
}
