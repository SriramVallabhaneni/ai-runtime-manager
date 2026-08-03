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

package v1alpha1

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// AIModelDeploymentSpec defines the desired state of AIModelDeployment
type AIModelDeploymentSpec struct {
	// Model is the supported Ollama model to deploy.
	// +kubebuilder:validation:MinLength=1
	Model string `json:"model"`

	// Replicas is currently restricted to one because each runtime
	// uses a ReadWriteOnce persistent volume.
	// +kubebuilder:validation:Minimum=1
	// +kubebuilder:validation:Maximum=1
	// +kubebuilder:default=1
	Replicas int32 `json:"replicas,omitempty"`

	// StorageSize controls the requested model-storage capacity.
	// +kubebuilder:default="5Gi"
	StorageSize string `json:"storageSize,omitempty"`

	// RestartGeneration increments whenever a rollout restart is requested.
	// +kubebuilder:default=0
	RestartGeneration int64 `json:"restartGeneration,omitempty"`
}

// AIModelDeploymentStatus defines the observed state of AIModelDeployment.
type AIModelDeploymentStatus struct {
	// Phase describes the current runtime state.
	Phase string `json:"phase,omitempty"`

	// Ready indicates whether the AI runtime is available.
	Ready bool `json:"ready,omitempty"`

	// Message contains additional status information.
	Message string `json:"message,omitempty"`

	// ObservedGeneration records the resource generation processed
	// by the controller.
	ObservedGeneration int64 `json:"observedGeneration,omitempty"`
}

// +kubebuilder:object:root=true
// +kubebuilder:subresource:status

// AIModelDeployment is the Schema for the aimodeldeployments API
type AIModelDeployment struct {
	metav1.TypeMeta `json:",inline"`

	// metadata is a standard object metadata
	// +optional
	metav1.ObjectMeta `json:"metadata,omitzero"`

	// spec defines the desired state of AIModelDeployment
	// +required
	Spec AIModelDeploymentSpec `json:"spec"`

	// status defines the observed state of AIModelDeployment
	// +optional
	Status AIModelDeploymentStatus `json:"status,omitzero"`
}

// +kubebuilder:object:root=true

// AIModelDeploymentList contains a list of AIModelDeployment
type AIModelDeploymentList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitzero"`
	Items           []AIModelDeployment `json:"items"`
}

func init() {
	SchemeBuilder.Register(func(s *runtime.Scheme) error {
		s.AddKnownTypes(SchemeGroupVersion, &AIModelDeployment{}, &AIModelDeploymentList{})
		return nil
	})
}
