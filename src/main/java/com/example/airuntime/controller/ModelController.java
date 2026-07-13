package com.example.airuntime.controller;

import com.example.airuntime.dto.ModelDeployRequest;
import com.example.airuntime.service.KubernetesDeploymentService;
import com.example.airuntime.dto.ModelResponse;
import com.example.airuntime.dto.ScaleModelRequest;
import com.example.airuntime.dto.UpdateImageRequest;
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

    @PostMapping
    public String deployModel(@Valid @RequestBody ModelDeployRequest request) throws Exception {
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

    @DeleteMapping("/{name}")
    public String deleteModel(@PathVariable String name) throws Exception {
        return service.deleteModel(name);
    }

    @PatchMapping("/{name}/scale")
    public ModelResponse scaleModel(
            @PathVariable String name,
            @Valid @RequestBody ScaleModelRequest request
    ) throws Exception {
        return service.scaleModel(name, request);
    }

    @PatchMapping("/{name}/image")
    public ModelResponse updateImage(
            @PathVariable String name,
            @Valid @RequestBody UpdateImageRequest request
    ) throws Exception {
        return service.updateImage(name, request);
    }

   @PostMapping("/{name}/restart")
    public ModelResponse restartModel(@PathVariable String name) throws Exception {
        return service.restartModel(name);
   }
}
