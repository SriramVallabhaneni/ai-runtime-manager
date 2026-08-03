package com.example.airuntime.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.airuntime.dto.DeployModelRequest;
import com.example.airuntime.dto.ModelResponse;
import com.example.airuntime.dto.ScaleModelRequest;
import com.example.airuntime.dto.UpdateImageRequest;
import com.example.airuntime.model.AiModel;
import com.example.airuntime.model.AiModelRegistry;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Deployment;

@Service
public class KubernetesDeploymentService {

    private final AppsV1Api appsApi;
    private final CustomObjectsApi customObjectsApi;
    private final String namespace = "default";

    private final AiModelRegistry aiModelRegistry;

    public KubernetesDeploymentService(ApiClient apiClient, AiModelRegistry aiModelRegistry) {
        this.appsApi = new AppsV1Api(apiClient);
        this.aiModelRegistry = aiModelRegistry;
        this.customObjectsApi = new CustomObjectsApi(apiClient);
    }

    public String deployModel(DeployModelRequest request) throws Exception {
        AiModel aiModel = aiModelRegistry.get(request.getModel());

        if (aiModel == null) {
            throw new IllegalArgumentException("Unsupported model: " + request.getModel());
        }

        Map<String, Object> resource = Map.of(
                "apiVersion", "runtime.airuntime.dev/v1alpha1",
                "kind", "AIModelDeployment",
                "metadata", Map.of(
                "name", request.getDeploymentName()
                ),
                "spec", Map.of(
                        "model", request.getModel(),
                        "replicas", request.getReplicas(),
                        "storageSize", "5Gi"
                )
        );

        customObjectsApi.createNamespacedCustomObject(
                "runtime.airuntime.dev",
                "v1alpha1",
                namespace,
                "aimodeldeployments",
                resource
        ).execute();

        return "Deployed AI model: " + request.getDeploymentName();
    }

    public List<ModelResponse> listModels() throws Exception {
        return appsApi.listNamespacedDeployment(namespace)
                .labelSelector("airuntime.dev/managed=true")
                .execute()
                .getItems()
                .stream()
                .map(this::toModelResponse)
                .toList();
    }

    public ModelResponse getModel(String name) throws Exception {
        V1Deployment deployment = appsApi.readNamespacedDeployment(name, namespace).execute();
        return toModelResponse(deployment);
    }

    private ModelResponse toModelResponse(V1Deployment deployment) {
        String name = deployment.getMetadata().getName();

        int replicas = deployment.getSpec().getReplicas() == null
                ? 0
                : deployment.getSpec().getReplicas();

        int availableReplicas = deployment.getStatus().getAvailableReplicas() == null
                ? 0
                : deployment.getStatus().getAvailableReplicas();

        String status = availableReplicas >= replicas ? "Running" : "Pending";

        return new ModelResponse(name, replicas, availableReplicas, status);
    }

    public ModelResponse scaleModel(String name, ScaleModelRequest request) throws Exception {
        V1Deployment deployment = appsApi.readNamespacedDeployment(name, namespace).execute();

        deployment.getSpec().setReplicas(request.getReplicas());

        V1Deployment updatedDeployment = appsApi.replaceNamespacedDeployment(
                name,
                namespace,
                deployment
        ).execute();

        return toModelResponse(updatedDeployment);
    }

    public String deleteModel(String name) throws Exception {
        customObjectsApi.deleteNamespacedCustomObject(
            "runtime.airuntime.dev",
            "v1alpha1",
            namespace,
            "aimodeldeployments",
            name
        ).execute();

    return "Deleted AI model deployment: " + name;
    }

    public ModelResponse updateImage(String name, UpdateImageRequest request) throws Exception {
        V1Deployment deployment = appsApi.readNamespacedDeployment(name, namespace).execute();

        deployment.getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0)
                .setImage(request.getImage());

        V1Deployment updatedDeployment = appsApi.replaceNamespacedDeployment(
                name,
                namespace,
                deployment
        ).execute();

        return toModelResponse(updatedDeployment);
    }

    public ModelResponse restartModel(String name) throws Exception {
        V1Deployment deployment = appsApi.readNamespacedDeployment(name, namespace).execute();

        Map<String, String> annotations = deployment.getSpec()
                .getTemplate()
                .getMetadata()
                .getAnnotations();

        if (annotations == null) {
            annotations = new java.util.HashMap<>();
            deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
        }

        annotations.put("kubectl.kubernetes.io/restartedAt", java.time.Instant.now().toString());

        V1Deployment updatedDeployment = appsApi.replaceNamespacedDeployment(
                name,
                namespace,
                deployment
        ).execute();

        return toModelResponse(updatedDeployment);
    }
}