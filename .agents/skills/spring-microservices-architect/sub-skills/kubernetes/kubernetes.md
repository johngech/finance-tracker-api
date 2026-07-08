# Kubernetes Sub-Skill — Helm, K8s-native Patterns, Istio

This sub-skill handles deployment to Kubernetes using Helm charts, Kubernetes-native replacements for Spring Cloud services, Istio service mesh integration, and cert-manager for TLS.

## Pre-Checks

Before Kubernetes deployment:
1. Verify all Spring Cloud layers are complete (tracing layer and prior)
2. Docker images are built and available (Docker Engine ≥ 29.3.0)
3. `kubectl` and `helm` CLI tools are available
4. A Kubernetes v1.35+ cluster is accessible (Minikube for dev)

**Injection checks:**
```bash
kubectl version --client --short 2>/dev/null || echo "no-kubectl"
helm version --short 2>/dev/null || echo "no-helm"
kubectl config current-context 2>/dev/null || echo "no-cluster"
```

## Workflow 1: Helm Chart Structure

### Directory Layout

```
kubernetes/
  helm/
    common/                          ← Library chart (shared templates)
      Chart.yaml
      values.yaml
      .helmignore
      templates/
        _helpers.tpl                 ← Template helper functions
        _deployment.yaml             ← Generic Deployment template
        _service.yaml                ← Generic Service template
        _configmap_from_file.yaml    ← ConfigMap from file template
        _secrets.yaml                ← Secrets template
        _ingress.yaml                ← Ingress template
        _issuer.yaml                 ← cert-manager Issuer template
    components/                      ← Per-service charts
      product/
        Chart.yaml                   ← Depends on common
        values.yaml                  ← Service-specific values
      product-composite/
        Chart.yaml
        values.yaml
      recommendation/
        Chart.yaml
        values.yaml
      review/
        Chart.yaml
        values.yaml
      auth-server/
        Chart.yaml
        values.yaml
      mongodb/
        Chart.yaml
        values.yaml
      mysql/
        Chart.yaml
        values.yaml
      rabbitmq/
        Chart.yaml
        values.yaml
      zipkin-server/
        Chart.yaml
        values.yaml
    environments/                    ← Environment-specific charts
      dev-env/
        Chart.yaml                   ← Depends on all component charts
        Chart.lock
        values.yaml                  ← Dev environment overrides
        templates/
          secrets.yaml
          issuer.yaml
          ingress.yaml
```

### Common Chart — Library Templates

**`_deployment.yaml` template (key sections):**
```yaml
{{- define "common.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Values.fullnameOverride | default .Release.Name }}
spec:
  replicas: {{ .Values.replicaCount | default 1 }}
  selector:
    matchLabels:
      app: {{ .Values.fullnameOverride | default .Release.Name }}
  template:
    metadata:
      labels:
        app: {{ .Values.fullnameOverride | default .Release.Name }}
    spec:
      containers:
        - name: {{ .Chart.Name }}
          image: "{{ .Values.image.repository }}:{{ .Values.image.tag }}"
          ports:
            - containerPort: {{ .Values.containerPort | default 8080 }}
          {{- if .Values.livenessProbe }}
          livenessProbe: {{ toYaml .Values.livenessProbe | nindent 12 }}
          {{- end }}
          {{- if .Values.readinessProbe }}
          readinessProbe: {{ toYaml .Values.readinessProbe | nindent 12 }}
          {{- end }}
          resources:
            limits:
              memory: {{ .Values.resources.limits.memory | default "512Mi" }}
{{- end -}}
```

**`_service.yaml` template:**
```yaml
{{- define "common.service" -}}
apiVersion: v1
kind: Service
metadata:
  name: {{ .Values.fullnameOverride | default .Release.Name }}
spec:
  type: {{ .Values.service.type | default "ClusterIP" }}
  ports:
    - port: {{ .Values.service.port | default 80 }}
      targetPort: {{ .Values.containerPort | default 8080 }}
  selector:
    app: {{ .Values.fullnameOverride | default .Release.Name }}
{{- end -}}
```

### Component Chart Example — `product/`

**`Chart.yaml`:**
```yaml
apiVersion: v2
name: <service>
description: <Service> core microservice
version: 1.0.0
dependencies:
  - name: common
    version: 1.0.0
    repository: "file://../common"
```

**`values.yaml`:**
```yaml
image:
  repository: <registry>/<service-name>
  tag: latest
containerPort: 8080
service:
  port: 80
resources:
  limits:
    memory: 512Mi
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
```

## Workflow 2: K8s-native Pattern Replacements

### Replace Config Server with ConfigMaps

Instead of `spring-cloud/config-server/` + `config-repo/`, use:

```yaml
{{- define "common.configmap_from_file" -}}
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ .Release.Name }}-config
data:
  application.yml: |
    {{ .Files.Get "config/application.yml" | nindent 4 }}
{{- end -}}
```

Mount as volume in Deployment:
```yaml
volumes:
  - name: config
    configMap:
      name: {{ .Release.Name }}-config
volumeMounts:
  - name: config
    mountPath: /config
```

### Replace Gateway with Ingress

```yaml
{{- define "common.ingress" -}}
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: {{ .Release.Name }}-ingress
  annotations:
    cert-manager.io/issuer: {{ .Values.ingress.issuer | default "letsencrypt" }}
spec:
  tls:
    - hosts:
        - {{ .Values.ingress.host }}
      secretName: {{ .Values.ingress.tlsSecret }}
  rules:
    - host: {{ .Values.ingress.host }}
      http:
        paths:
          - path: /product-composite
            pathType: Prefix
            backend:
              service:
                name: product-composite
                port:
                  number: 80
{{- end -}}
```

### Secrets for Sensitive Configuration

```yaml
{{- define "common.secrets" -}}
apiVersion: v1
kind: Secret
metadata:
  name: {{ .Release.Name }}-secrets
type: Opaque
data:
  {{- range $key, $value := .Values.secrets }}
  {{ $key }}: {{ $value | b64enc }}
  {{- end }}
{{- end -}}
```

### cert-manager Issuer

```yaml
{{- define "common.issuer" -}}
apiVersion: cert-manager.io/v1
kind: Issuer
metadata:
  name: {{ .Values.issuer.name | default "selfsigned" }}
spec:
  selfSigned: {}
{{- end -}}
```

## Workflow 3: Istio Service Mesh

### Enable Istio Injection

Label the namespace:
```bash
kubectl label namespace default istio-injection=enabled
```

### Istio VirtualService Example
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-composite-vs
spec:
  hosts:
    - product-composite
  http:
    - route:
        - destination:
            host: product-composite
            subset: v1
          weight: 100
```

### Istio DestinationRule Example
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: product-composite-dr
spec:
  host: product-composite
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
  subsets:
    - name: v1
      labels:
        version: v1
```

## Deployment Commands

```bash
# Create namespace
kubectl create namespace hands-on

# Deploy with Helm
helm dependency update kubernetes/helm/environments/dev-env
helm install hands-on-dev kubernetes/helm/environments/dev-env -n hands-on

# Verify
kubectl get pods -n hands-on
kubectl get svc -n hands-on

# Run integration tests against K8s
HOST=minikube.me PORT=443 ./test-em-all.bash

# Uninstall
helm uninstall hands-on-dev -n hands-on
```

## Post-Kubernetes Checklist

- [ ] `kubernetes/helm/common/` library chart created with all shared templates
- [ ] Component chart exists for every microservice and infrastructure service
- [ ] Environment chart (`dev-env/`) aggregates all components
- [ ] Liveness and readiness probes configured for all Spring Boot services
- [ ] ConfigMaps replace Config Server configuration
- [ ] Secrets store sensitive data (passwords, keys)
- [ ] Ingress replaces Gateway for external access
- [ ] cert-manager Issuer provisioned for TLS
- [ ] `helm install` succeeds without errors
- [ ] `test-em-all.bash` passes against Kubernetes cluster
- [ ] Services still work via `docker compose` (platform independence)
