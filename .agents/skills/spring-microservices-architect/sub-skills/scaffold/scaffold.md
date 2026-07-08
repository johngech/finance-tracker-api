# Scaffold Sub-Skill — Iterative Code Generation

This sub-skill generates microservice project code iteratively, adding capabilities layer-by-layer. Each invocation detects the current iteration and generates the appropriate next layer.

> `${basePackage}` is a project-configurable placeholder. Substitute your project's base package when generating code.

## Pre-Checks

Before scaffolding, verify:
1. The current iteration has been detected (Step 1 of the main playbook)
2. Conventions and tech-stack references have been loaded (Step 2)
3. The target layer is the next logical one in the dependency chain

## Scaffolding Workflows

### Layer: base-services — Cooperating Microservices

**When:** Starting from scratch, no existing microservices.

> Read service definitions from `.agents/project.yml` (see [project-config.md](../../references/project-config.md)). The examples below use placeholders — replace with your actual service names.

**Generate:**

1. **Root `settings.gradle`:**
```groovy
include ':api'
include ':util'
include ':microservices:<service-a>'
include ':microservices:<service-b>'
include ':microservices:<service-c>'
// ... one per core service in project.yml
include ':microservices:<composite-service>'
```

2. **`api/` module** — shared API interfaces and DTOs:
   - `api/build.gradle` — library project, no Spring Boot plugin
   - `${basePackage}.api.core.<service>.{Entity}` — DTO record/class (one per core service)
   - `${basePackage}.api.core.<service>.{Entity}Service` — REST interface with `@GetMapping`
   - `${basePackage}.api.composite.<domain>.{Domain}Aggregate` — composite DTO
   - `${basePackage}.api.composite.<domain>.{Domain}CompositeService` — composite REST interface
   - `${basePackage}.api.composite.<domain>.{Entity}Summary` — summary DTOs, `ServiceAddresses`
   - `${basePackage}.api.exceptions.NotFoundException` / `InvalidInputException`

3. **`util/` module** — shared utilities:
   - `util/build.gradle` — library project
   - `${basePackage}.util.http.GlobalControllerExceptionHandler` — `@RestControllerAdvice`
   - `${basePackage}.util.http.HttpErrorInfo` — error response DTO
   - `${basePackage}.util.http.ServiceUtil` — service address helper

4. **`microservices/<service-a>/`** (repeat for each core service):
   - `build.gradle` — Spring Boot 4.1.0 service project with `webflux`, `actuator`, Java 26 toolchain
   - `{Entity}ServiceApplication.java` — `@SpringBootApplication`
   - `services/{Entity}ServiceImpl.java` — implements `{Entity}Service`, in-memory stub
   - `src/main/resources/application.yml` — server port from project.yml

5. **`microservices/<composite-service>/` special files:**
   - `services/{Domain}Integration.java` — REST client calling core services
   - `services/{Domain}CompositeServiceImpl.java` — aggregates core service responses

6. **`test-em-all.bash`** — integration test script with `assertCurl`, `assertEqual`, `waitForService`

> **After scaffolding base-services, read [api-patterns.md](../api-patterns/api-patterns.md)** to understand the DTO → Interface → Implementation → Entity → Mapper → MessageProcessor pattern that all services follow.

---

### Layer: openapi — API Documentation

**When:** base-services layer is present, no springdoc-openapi in dependencies.

**Add:**
- `springdoc-openapi-starter-webflux-ui` dependency to `api/build.gradle`
- OpenAPI annotations on `ProductCompositeService` interface:
  - `@Tag(name = "...")` on the interface
  - `@Operation(summary = "...")` on methods
  - `@ApiResponse` annotations for status codes
- OpenAPI configuration bean in composite service

---

### Layer: persistence — Data Storage

**When:** openapi layer is present, no data-mongodb/data-jpa in dependencies.

**Add to MongoDB-backed core services (e.g., `product-service`):**
- `spring-boot-starter-data-mongodb-reactive` dependency
- `${basePackage}.microservices.core.<service>.persistence.{Entity}Entity` — MongoDB document
- `${basePackage}.microservices.core.<service>.persistence.{Entity}Repository` — reactive repository
- `${basePackage}.microservices.core.<service>.services.{Entity}Mapper` — MapStruct mapper
- Update `{Entity}ServiceImpl` to use repository instead of in-memory stub
- Testcontainers MongoDB test class

**Repeat for other MongoDB services (e.g., `recommendation-service`):**
- Same as above (MongoDB)

**Add to JPA-backed core services (e.g., `review-service`):**
- `spring-boot-starter-data-jpa` + `mysql-connector-j` dependencies
- `${basePackage}.microservices.core.<service>.persistence.{Entity}Entity` — JPA entity
- `${basePackage}.microservices.core.<service>.persistence.{Entity}Repository` — JPA repository
- Testcontainers MySQL test class

> Choose persistence strategy per service from [project-config.md](../../references/project-config.md): `mongodb-reactive`, `mongodb`, `jpa`, `redis`, or `elasticsearch`.

**Add to `api/`:**
- `createProduct()`, `deleteProduct()` methods to `ProductService` interface
- Same for Review and Recommendation service interfaces
- `createCompositeProduct()`, `deleteCompositeProduct()` to `ProductCompositeService`
- `Event<K, T>` generic event envelope class in `${basePackage}.api.event`

**Add MapStruct dependencies to affected `build.gradle` files:**
```groovy
ext {
    mapstructVersion = "1.6.3"
}
implementation "org.mapstruct:mapstruct:${mapstructVersion}"
compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
```

**Add Testcontainers:**
```groovy
implementation platform('org.testcontainers:testcontainers-bom:1.20.4')
testImplementation 'org.testcontainers:testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mongodb'  // or :mysql
```

---

### Layer: reactive — Reactive Streams & Messaging

**When:** persistence layer is present, no spring-cloud-starter-stream in dependencies.

**Add:**
- `spring-cloud-starter-stream-rabbit` and `spring-cloud-starter-stream-kafka` to all microservice `build.gradle` files
- Spring Cloud BOM (`spring-cloud-dependencies:2025.1.2`) in `dependencyManagement`
- Message consumer/producer configurations in `application.yml`
- `MessageProcessorConfig.java` in each core service (functional `Consumer<Event<K, T>>` bean)
- `StreamBridge`-based publisher in composite service's Integration class
- `docker-compose-kafka.yml` — Kafka variant
- `docker-compose-partitions.yml` — partitioned variant with `_p1` service duplicates
- Spring profiles: `streaming_partitioned`, `streaming_instance_0`, `streaming_instance_1`, `kafka`

**Convert core services** from synchronous REST to event-driven for create/delete:
- Product/Review/Recommendation services consume events from topics via `MessageProcessorConfig`
- Composite service publishes events via `StreamBridge` to topics
- GET operations remain synchronous REST via `WebClient`

> See [api-patterns.md](../api-patterns/api-patterns.md) for the complete generic implementation showing this dual REST+Event pattern applied to any core service.

---

### Layer: discovery — Service Discovery

**When:** reactive layer is present, no `spring-cloud/eureka-server/` directory.

**Generate:**
- `spring-cloud/eureka-server/` module:
  - `build.gradle` with `spring-cloud-starter-netflix-eureka-server`, Spring Boot 4.1.0, Java 26 toolchain
  - `EurekaServerApplication.java` with `@EnableEurekaServer`
  - `application.yml` with Eureka server config
  - `Dockerfile`

**Modify:**
- Add `spring-cloud-starter-netflix-eureka-client` to all microservice `build.gradle` files
- Add Eureka client configuration to all `application.yml` files
- Update `settings.gradle` to include `:spring-cloud:eureka-server`

---

### Remaining Layers

The following layers follow the same detect → generate → extend pattern. Each adds specific modules, dependencies, configurations, and tests. Reference the repository source code for exact file templates:

| Layer | What It Adds |
|-------|-------------|
| **edge-server** | `spring-cloud/gateway/` module, route config, HTTPS setup |
| **security** | `spring-cloud/authorization-server/`, OAuth 2.0 / OIDC, `@PreAuthorize`, token relay |
| **centralized-config** | `spring-cloud/config-server/`, `config-repo/` directory, `.env` for secrets |
| **resilience** | Resilience4j circuit breaker, time limiter, retry on composite service |
| **tracing** | Micrometer Tracing + Zipkin, `micrometer-tracing-bridge-otel`, distributed trace IDs |
| **kubernetes** | Helm charts under `kubernetes/helm/`, common library chart, component charts, dev-env |
| **k8s-native** | Replace Config Server with ConfigMaps, Gateway with Ingress, Secrets for credentials |
| **service-mesh** | Istio injection, VirtualService, DestinationRule, mutual TLS |
| **logging** | EFK stack (Elasticsearch, Fluentd DaemonSet, Kibana) on Kubernetes |
| **monitoring** | Prometheus scraping via pod annotations, Grafana dashboards, alert rules |
| **native** | GraalVM native image, Spring AOT, `graalvm-buildtools` plugin, native Dockerfile |

## Post-Scaffold Checklist

After generating code for any layer:
- [ ] All new `build.gradle` files follow conventions (correct plugins, group, version, Java 26 toolchain)
- [ ] All new Java files use correct package naming (`${basePackage}.*`)
- [ ] All new services have corresponding test classes
- [ ] `settings.gradle` includes all new modules
- [ ] `docker-compose.yml` includes all new services with health checks
- [ ] `test-em-all.bash` has been updated with new test cases
- [ ] Run governance sub-skill for validation
