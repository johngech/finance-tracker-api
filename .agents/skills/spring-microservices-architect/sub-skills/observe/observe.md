# Observe Sub-Skill — Logging, Monitoring, Native Compilation

This sub-skill covers three independent observability and optimization layers that can be applied after the kubernetes layer is deployed:

1. **Centralized Logging** — EFK stack
2. **Monitoring** — Prometheus & Grafana
3. **Native Compilation** — GraalVM & Spring AOT

## Pre-Checks

Before applying any observe layer:
1. Verify the kubernetes layer is complete (Helm charts deployed)
2. Kubernetes cluster is accessible with sufficient resources
3. For native compilation: Docker and GraalVM are available

---

## Layer 1: Centralized Logging — EFK Stack

### Components

| Component | Role | Deployment |
|-----------|------|-----------|
| **Elasticsearch** | Distributed search/analytics database for log storage | StatefulSet |
| **Fluentd** | Log collector, transformer, and forwarder | DaemonSet |
| **Kibana** | Visualization and exploration UI | Deployment |

### Fluentd DaemonSet Configuration

Fluentd runs on every Kubernetes node to collect container logs:

```yaml
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluentd
  namespace: kube-system
spec:
  selector:
    matchLabels:
      app: fluentd
  template:
    spec:
      containers:
        - name: fluentd
          image: fluent/fluentd-kubernetes-daemonset:v1-debian-elasticsearch
          env:
            - name: FLUENT_ELASTICSEARCH_HOST
              value: "elasticsearch.logging"
            - name: FLUENT_ELASTICSEARCH_PORT
              value: "9200"
          volumeMounts:
            - name: varlog
              mountPath: /var/log
            - name: dockercontainers
              mountPath: /var/lib/docker/containers
              readOnly: true
      volumes:
        - name: varlog
          hostPath:
            path: /var/log
        - name: dockercontainers
          hostPath:
            path: /var/lib/docker/containers
```

### Elasticsearch StatefulSet

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: elasticsearch
  namespace: logging
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: elasticsearch
          image: docker.elastic.co/elasticsearch/elasticsearch:8.12.2
          ports:
            - containerPort: 9200
          env:
            - name: discovery.type
              value: single-node
            - name: ES_JAVA_OPTS
              value: "-Xms512m -Xmx512m"
          resources:
            limits:
              memory: 1Gi
```

### Kibana Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: kibana
  namespace: logging
spec:
  replicas: 1
  template:
    spec:
      containers:
        - name: kibana
          image: docker.elastic.co/kibana/kibana:8.12.2
          ports:
            - containerPort: 5601
          env:
            - name: ELASTICSEARCH_HOSTS
              value: "http://elasticsearch:9200"
```

### Log Analysis Patterns

Use Kibana to:
1. Create index pattern matching `fluentd-*`
2. Search by `kubernetes.labels.app: <service-name>` to filter per-service logs
3. Correlate logs across services using trace IDs (from Micrometer Tracing)
4. Perform root cause analysis by time-range filtering around error events

---

## Layer 2: Monitoring — Prometheus & Grafana

### Spring Boot Actuator Configuration

All microservices must expose Prometheus-compatible metrics:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

Add Prometheus dependency:
```groovy
implementation 'io.micrometer:micrometer-registry-prometheus'
```

### Prometheus Configuration

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    scrape_configs:
      - job_name: 'spring-boot'
        metrics_path: /actuator/prometheus
        kubernetes_sd_configs:
          - role: pod
        relabel_configs:
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
            action: keep
            regex: true
          - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
            action: replace
            target_label: __metrics_path__
            regex: (.+)
```

### Pod Annotations for Scraping

Add to Helm Deployment template:
```yaml
metadata:
  annotations:
    prometheus.io/scrape: "true"
    prometheus.io/path: "/actuator/prometheus"
    prometheus.io/port: "8080"
```

### Grafana Dashboards

**Pre-built dashboards to import:**
- JVM (Micrometer) — Dashboard ID: 4701
- Spring Boot Statistics — Dashboard ID: 11378
- Kubernetes Pod Resources — Dashboard ID: 6417

**Custom dashboard for microservice landscape:**
- API response time (p50, p95, p99) per service
- Request rate per endpoint
- Circuit breaker state (open/closed/half-open)
- Consumer lag for Cloud Stream topics
- Pod CPU and memory utilization

### Grafana Alerting

```yaml
# Example alert rule for high response time
groups:
  - name: microservice-alerts
    rules:
      - alert: HighResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time on {{ $labels.application }}"
          description: "95th percentile response time > 2s for 5 minutes"

      - alert: CircuitBreakerOpen
        expr: resilience4j_circuitbreaker_state{state="open"} == 1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Circuit breaker open for {{ $labels.name }}"
```

---

## Layer 3: Native Compilation — GraalVM & Spring AOT

### Prerequisites

- GraalVM Native Image compiler installed
- Spring Boot 4.1.0 with Spring AOT support
- Sufficient build memory (8GB+ for native compilation)

### Build Configuration

Add to `build.gradle`:
```groovy
plugins {
    id 'org.graalvm.buildtools.native' version '0.10.5'
}

graalvmNative {
    binaries {
        main {
            imageName = project.name
            buildArgs.add('--no-fallback')
        }
    }
}
```

### Native-Specific Docker Compose

Create `docker-compose-native.yml` (no `version:` key — Compose V2):
```yaml
services:
  <service-a>:
    build: microservices/<service-a>
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    # Native image starts in milliseconds — no need for long health check waits
    depends_on:
      mongodb:
        condition: service_healthy
```

> Replace `<service-a>` with your actual service name. Key difference: Native images start in ~100ms vs ~10s for JVM.

### Native Dockerfile

```dockerfile
FROM ghcr.io/graalvm/native-image-community:26-muslib as builder
WORKDIR /app
COPY . .
RUN ./gradlew nativeCompile

FROM alpine:3.19
WORKDIR /app
COPY --from=builder /app/build/native/nativeCompile/* ./
EXPOSE 8080
ENTRYPOINT ["./<service-name>"]
```

> Replace `<service-name>` with your actual service executable name.

### Spring AOT Considerations

- Reflection hints: ensure all DTOs, entities, and configuration classes have `@RegisterReflectionForBinding`
- Resource hints: register `application.yml`, `META-INF/spring.factories`
- Proxy hints: Spring Data repositories, Spring Cloud interfaces require explicit proxy config
- Test with AOT processing: `./gradlew processAot` before native compile

### Build Commands

```bash
# AOT processing only (fast validation)
./gradlew processAot

# Full native compile (slow, ~5-10 min per service)
./gradlew nativeCompile

# Build native Docker images
docker compose -f docker-compose-native.yml build

# Test native images
docker compose -f docker-compose-native.yml up -d
./test-em-all.bash
```

---

## Post-Observe Checklist

### Logging
- [ ] Fluentd DaemonSet collecting logs from all pods
- [ ] Elasticsearch storing and indexing log data
- [ ] Kibana accessible with index pattern configured
- [ ] Can search logs by service name and trace ID

### Monitoring
- [ ] All microservices expose `/actuator/prometheus` endpoint
- [ ] Prometheus scraping all pods via annotations
- [ ] Grafana connected to Prometheus data source
- [ ] At least one dashboard showing service metrics
- [ ] Alert rules configured for critical thresholds

### Native Compilation
- [ ] `./gradlew processAot` succeeds for all services
- [ ] Native Docker images build successfully
- [ ] Native images start in under 1 second
- [ ] `docker-compose-native.yml` exists
- [ ] `test-em-all.bash` passes with native images
- [ ] Services still work with standard JVM images (backward compatibility)
