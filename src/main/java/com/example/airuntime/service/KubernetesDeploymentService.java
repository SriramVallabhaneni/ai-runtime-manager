package com.example.airuntime.service;

import com.example.airuntime.dto.DeployModelRequest;
import com.example.airuntime.dto.ModelResponse;
import com.example.airuntime.dto.ScaleModelRequest;
import com.example.airuntime.dto.UpdateImageRequest;
import com.example.airuntime.model.AiModel;
import com.example.airuntime.model.AiModelRegistry;
import java.util.List;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KubernetesDeploymentService {

    private final AppsV1Api appsApi;
    private final CoreV1Api coreApi;
    private final String namespace = "default";

    private final AiModelRegistry aiModelRegistry;

    public KubernetesDeploymentService(ApiClient apiClient, AiModelRegistry aiModelRegistry) {
        this.appsApi = new AppsV1Api(apiClient);
        this.coreApi = new CoreV1Api(apiClient);
        this.aiModelRegistry = aiModelRegistry;
    }

    public String deployModel(DeployModelRequest request) throws Exception {
        AiModel aiModel = aiModelRegistry.get(request.getModel());

        if (aiModel == null) {
            throw new IllegalArgumentException("Unsupported model: " + request.getModel());
        }

        String deploymentName = request.getDeploymentName();
        Map<String, String> labels = Map.of("app", deploymentName);

        V1Deployment deployment = new V1Deployment()
                .apiVersion("apps/v1")
                .kind("Deployment")
                .metadata(new V1ObjectMeta()
                        .name(deploymentName)
                        .labels(labels))
                .spec(new V1DeploymentSpec()
                        .replicas(request.getReplicas())
                        .selector(new V1LabelSelector().matchLabels(labels))
                        .template(new V1PodTemplateSpec()
                                .metadata(new V1ObjectMeta().labels(labels))
                                .spec(new V1PodSpec()
                                        .containers(List.of(
                                                new V1Container()
                                                        .name(deploymentName)
                                                        .image(aiModel.image())
                                                        .ports(List.of(
                                                                new V1ContainerPort()
                                                                        .containerPort(aiModel.port())
                                                        ))
                                        )))));

        appsApi.createNamespacedDeployment(namespace, deployment).execute();

        V1Service service = new V1Service()
                .apiVersion("v1")
                .kind("Service")
                .metadata(new V1ObjectMeta().name(deploymentName + "-service"))
                .spec(new V1ServiceSpec()
                        .selector(labels)
                        .ports(List.of(
                                new V1ServicePort()
                                        .port(aiModel.port())
                                        .targetPort(new IntOrString(aiModel.port()))
                        )));

        coreApi.createNamespacedService(namespace, service).execute();

        return "Deployed AI model: " + request.getModel() + " as " + deploymentName;
    }

    public List<ModelResponse> listModels() throws Exception {
        return appsApi.listNamespacedDeployment(namespace).execute()
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
        appsApi.deleteNamespacedDeployment(name, namespace).execute();
        coreApi.deleteNamespacedService(name + "-service", namespace).execute();
        return "Deleted model: " + name;
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