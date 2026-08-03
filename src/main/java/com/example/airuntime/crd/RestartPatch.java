package com.example.airuntime.crd;

public class RestartPatch {

    private Spec spec;

    public RestartPatch(long restartGeneration) {
        this.spec = new Spec(restartGeneration);
    }

    public Spec getSpec() {
        return spec;
    }

    public static class Spec {

        private long restartGeneration;

        public Spec(long restartGeneration) {
            this.restartGeneration = restartGeneration;
        }

        public long getRestartGeneration() {
            return restartGeneration;
        }
    }
}