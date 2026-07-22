/*
Copyright 2026.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package controller

import (
	"context"

	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	logf "sigs.k8s.io/controller-runtime/pkg/log"

	runtimev1alpha1 "github.com/SriramVallabhaneni/ai-runtime-manager/operator/api/v1alpha1"
)

// AIModelDeploymentReconciler reconciles a AIModelDeployment object
type AIModelDeploymentReconciler struct {
	client.Client
	Scheme *runtime.Scheme
}

// +kubebuilder:rbac:groups=runtime.airuntime.dev,resources=aimodeldeployments,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups=runtime.airuntime.dev,resources=aimodeldeployments/status,verbs=get;update;patch
// +kubebuilder:rbac:groups=runtime.airuntime.dev,resources=aimodeldeployments/finalizers,verbs=update
// +kubebuilder:rbac:groups=apps,resources=deployments,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups="",resources=persistentvolumeclaims,verbs=get;list;watch;create;update;patch;delete
// +kubebuilder:rbac:groups="",resources=services,verbs=get;list;watch;create;update;patch;delete

// Reconcile is part of the main kubernetes reconciliation loop which aims to
// move the current state of the cluster closer to the desired state.
// TODO(user): Modify the Reconcile function to compare the state specified by
// the AIModelDeployment object against the actual cluster state, and then
// perform operations to make the cluster state reflect the state specified by
// the user.
//
// For more details, check Reconcile and its Result here:
// - https://pkg.go.dev/sigs.k8s.io/controller-runtime@v0.24.1/pkg/reconcile
func (r *AIModelDeploymentReconciler) Reconcile(
	ctx context.Context,
	req ctrl.Request,
) (ctrl.Result, error) {
	logger := logf.FromContext(ctx)

	aiModelDeployment := &runtimev1alpha1.AIModelDeployment{}

	if err := r.Get(ctx, req.NamespacedName, aiModelDeployment); err != nil {
		if apierrors.IsNotFound(err) {
			return ctrl.Result{}, nil
		}

		logger.Error(err, "unable to fetch AIModelDeployment")
		return ctrl.Result{}, err
	}

	// Reconcile the PVC first because the runtime Deployment will
	// eventually mount it.
	if err := r.reconcilePVC(ctx, aiModelDeployment); err != nil {
		return ctrl.Result{}, err
	}

	// Reconcile the child Deployment.
	if err := r.reconcileDeployment(ctx, aiModelDeployment); err != nil {
		return ctrl.Result{}, err
	}

	// Reconcile the Service that exposes the Ollama API.
	if err := r.reconcileService(ctx, aiModelDeployment); err != nil {
		return ctrl.Result{}, err
	}

	return ctrl.Result{}, nil
}

func (r *AIModelDeploymentReconciler) reconcilePVC(
	ctx context.Context,
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) error {
	logger := logf.FromContext(ctx)

	pvcName := aiModelDeployment.Name + "-models"

	existingPVC := &corev1.PersistentVolumeClaim{}

	err := r.Get(
		ctx,
		types.NamespacedName{
			Name:      pvcName,
			Namespace: aiModelDeployment.Namespace,
		},
		existingPVC,
	)

	if err == nil {
		logger.Info(
			"child PersistentVolumeClaim already exists",
			"persistentVolumeClaim",
			existingPVC.Name,
		)

		return nil
	}

	if !apierrors.IsNotFound(err) {
		logger.Error(err, "unable to fetch child PersistentVolumeClaim")
		return err
	}

	pvc, err := r.pvcForAIModel(aiModelDeployment)
	if err != nil {
		logger.Error(err, "unable to build child PersistentVolumeClaim")
		return err
	}

	if err := controllerutil.SetControllerReference(
		aiModelDeployment,
		pvc,
		r.Scheme,
	); err != nil {
		logger.Error(
			err,
			"unable to set owner reference on PersistentVolumeClaim",
		)
		return err
	}

	logger.Info(
		"creating child PersistentVolumeClaim",
		"persistentVolumeClaim",
		pvc.Name,
	)

	if err := r.Create(ctx, pvc); err != nil {
		logger.Error(err, "unable to create child PersistentVolumeClaim")
		return err
	}

	return nil
}

func (r *AIModelDeploymentReconciler) reconcileDeployment(
	ctx context.Context,
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) error {
	logger := logf.FromContext(ctx)

	deployment := &appsv1.Deployment{}

	err := r.Get(
		ctx,
		types.NamespacedName{
			Name:      aiModelDeployment.Name,
			Namespace: aiModelDeployment.Namespace,
		},
		deployment,
	)

	if err == nil {
		logger.Info(
			"child Deployment already exists",
			"deployment",
			deployment.Name,
		)

		return nil
	}

	if !apierrors.IsNotFound(err) {
		logger.Error(err, "unable to fetch child Deployment")
		return err
	}

	deployment = r.deploymentForAIModel(aiModelDeployment)

	if err := controllerutil.SetControllerReference(
		aiModelDeployment,
		deployment,
		r.Scheme,
	); err != nil {
		logger.Error(err, "unable to set owner reference on Deployment")
		return err
	}

	logger.Info(
		"creating child Deployment",
		"deployment",
		deployment.Name,
	)

	if err := r.Create(ctx, deployment); err != nil {
		logger.Error(err, "unable to create child Deployment")
		return err
	}

	return nil
}

func (r *AIModelDeploymentReconciler) reconcileService(
	ctx context.Context,
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) error {
	logger := logf.FromContext(ctx)

	serviceName := aiModelDeployment.Name + "-service"
	service := &corev1.Service{}

	err := r.Get(
		ctx,
		types.NamespacedName{
			Name:      serviceName,
			Namespace: aiModelDeployment.Namespace,
		},
		service,
	)

	if err == nil {
		logger.Info(
			"child Service already exists",
			"service",
			service.Name,
		)

		return nil
	}

	if !apierrors.IsNotFound(err) {
		logger.Error(err, "unable to fetch child Service")
		return err
	}

	service = r.serviceForAIModel(aiModelDeployment)

	if err := controllerutil.SetControllerReference(
		aiModelDeployment,
		service,
		r.Scheme,
	); err != nil {
		logger.Error(err, "unable to set owner reference on Service")
		return err
	}

	logger.Info(
		"creating child Service",
		"service",
		service.Name,
	)

	if err := r.Create(ctx, service); err != nil {
		logger.Error(err, "unable to create child Service")
		return err
	}

	return nil
}

func (r *AIModelDeploymentReconciler) pvcForAIModel(
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) (*corev1.PersistentVolumeClaim, error) {
	storageSize := aiModelDeployment.Spec.StorageSize
	if storageSize == "" {
		storageSize = "5Gi"
	}

	storageQuantity, err := resource.ParseQuantity(storageSize)
	if err != nil {
		return nil, err
	}

	labels := map[string]string{
		"app.kubernetes.io/name":       "ai-model-runtime",
		"app.kubernetes.io/instance":   aiModelDeployment.Name,
		"app.kubernetes.io/managed-by": "ai-runtime-operator",
	}

	return &corev1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name:      aiModelDeployment.Name + "-models",
			Namespace: aiModelDeployment.Namespace,
			Labels:    labels,
		},
		Spec: corev1.PersistentVolumeClaimSpec{
			AccessModes: []corev1.PersistentVolumeAccessMode{
				corev1.ReadWriteOnce,
			},
			Resources: corev1.VolumeResourceRequirements{
				Requests: corev1.ResourceList{
					corev1.ResourceStorage: storageQuantity,
				},
			},
		},
	}, nil
}

func (r *AIModelDeploymentReconciler) serviceForAIModel(
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) *corev1.Service {
	labels := map[string]string{
		"app.kubernetes.io/name":       "ai-model-runtime",
		"app.kubernetes.io/instance":   aiModelDeployment.Name,
		"app.kubernetes.io/managed-by": "ai-runtime-operator",
	}

	return &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{
			Name:      aiModelDeployment.Name + "-service",
			Namespace: aiModelDeployment.Namespace,
			Labels:    labels,
		},
		Spec: corev1.ServiceSpec{
			Type:     corev1.ServiceTypeClusterIP,
			Selector: labels,
			Ports: []corev1.ServicePort{
				{
					Name:       "http",
					Port:       11434,
					TargetPort: intstr.FromString("http"),
					Protocol:   corev1.ProtocolTCP,
				},
			},
		},
	}
}

func (r *AIModelDeploymentReconciler) deploymentForAIModel(
	aiModelDeployment *runtimev1alpha1.AIModelDeployment,
) *appsv1.Deployment {
	labels := map[string]string{
		"app.kubernetes.io/name":       "ai-model-runtime",
		"app.kubernetes.io/instance":   aiModelDeployment.Name,
		"app.kubernetes.io/managed-by": "ai-runtime-operator",
	}

	replicas := aiModelDeployment.Spec.Replicas
	if replicas == 0 {
		replicas = 1
	}

	return &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{
			Name:      aiModelDeployment.Name,
			Namespace: aiModelDeployment.Namespace,
			Labels:    labels,
		},
		Spec: appsv1.DeploymentSpec{
			Replicas: &replicas,
			Selector: &metav1.LabelSelector{
				MatchLabels: labels,
			},
			Template: corev1.PodTemplateSpec{
				ObjectMeta: metav1.ObjectMeta{
					Labels: labels,
				},
				Spec: corev1.PodSpec{
					Volumes: []corev1.Volume{
						{
							Name: "model-storage",
							VolumeSource: corev1.VolumeSource{
								PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
									ClaimName: aiModelDeployment.Name + "-models",
								},
							},
						},
					},
					InitContainers: []corev1.Container{
						{
							Name:    "pull-model",
							Image:   "ollama/ollama:latest",
							Command: []string{"/bin/sh", "-c"},
							Args: []string{
								"ollama serve & pid=$!; sleep 5; ollama pull " +
									aiModelDeployment.Spec.Model +
									"; kill $pid",
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "model-storage",
									MountPath: "/root/.ollama",
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Name:  "runtime",
							Image: "ollama/ollama:latest",
							Ports: []corev1.ContainerPort{
								{
									Name:          "http",
									ContainerPort: 11434,
								},
							},
							VolumeMounts: []corev1.VolumeMount{
								{
									Name:      "model-storage",
									MountPath: "/root/.ollama",
								},
							},
						},
					},
				},
			},
		},
	}
}

// SetupWithManager sets up the controller with the Manager.
func (r *AIModelDeploymentReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&runtimev1alpha1.AIModelDeployment{}).
		Owns(&corev1.PersistentVolumeClaim{}).
		Owns(&appsv1.Deployment{}).
		Owns(&corev1.Service{}).
		Named("aimodeldeployment").
		Complete(r)
}
