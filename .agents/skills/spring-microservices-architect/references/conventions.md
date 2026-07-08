# Conventions — Enforced Coding Standards

All generated code MUST follow these conventions. The governance sub-skill validates compliance.

> **Note:** `${basePackage}` is a project-configurable placeholder. Replace it with your project's actual base package (e.g., `com.projects`, `org.acme`, etc.) when generating code.

## Package Naming

| Module | Base Package | Example |
|--------|-------------|---------|
| API library | `${basePackage}.api` | `${basePackage}.api.core.product.Product` |
| API core interfaces | `${basePackage}.api.core.<service>` | `${basePackage}.api.core.product.ProductService` |
| API composite interfaces | `${basePackage}.api.composite.<domain>` | `${basePackage}.api.composite.product.ProductCompositeService` |
| API events | `${basePackage}.api.event` | `${basePackage}.api.event.Event` |
| API exceptions | `${basePackage}.api.exceptions` | `${basePackage}.api.exceptions.NotFoundException` |
| Util library | `${basePackage}.util` | `${basePackage}.util.http.ServiceUtil` |
| Core microservices | `${basePackage}.microservices.core.<service>` | `${basePackage}.microservices.core.product.ProductServiceApplication` |
| Core service impl | `${basePackage}.microservices.core.<service>.services` | `${basePackage}.microservices.core.product.services.ProductServiceImpl` |
| Core persistence | `${basePackage}.microservices.core.<service>.persistence` | `${basePackage}.microservices.core.product.persistence.ProductEntity` |
| Composite microservices | `${basePackage}.microservices.composite.<domain>` | `${basePackage}.microservices.composite.product.ProductCompositeServiceApplication` |
| Composite service impl | `${basePackage}.microservices.composite.<domain>.services` | `${basePackage}.microservices.composite.product.services.ProductCompositeIntegration` |
| Spring Cloud modules | `${basePackage}.springcloud.<module>` | `${basePackage}.springcloud.eurekaserver.EurekaServerApplication` |

## Gradle Multi-Project Structure

### Root `settings.gradle`

Declare every module in a single root `settings.gradle`. Use names from your `.agents/project.yml` service registry (see [project-config.md](project-config.md)).

```groovy
// Shared libraries — always present:
include ':api'
include ':util'

// Core microservices — one per domain entity / bounded context:
include ':microservices:<service-a>'
include ':microservices:<service-b>'
include ':microservices:<service-c>'
// ... add as many as your domain requires

// Composite / aggregator services:
include ':microservices:<composite-service>'

// Spring Cloud infrastructure (add progressively per layer-map):
include ':spring-cloud:eureka-server'
include ':spring-cloud:gateway'
include ':spring-cloud:authorization-server'
include ':spring-cloud:config-server'
```

> **Example instantiation** (book reference): `<service-a>` = `product-service`, `<service-b>` = `review-service`, `<service-c>` = `recommendation-service`, `<composite-service>` = `product-composite-service`. See [project-config.md](project-config.md) for more examples.

### Library Projects (`api/`, `util/`)

- Use `io.spring.dependency-management` plugin version `1.1.7` (NOT `org.springframework.boot`)
- Declare `java` plugin
- Configure Java toolchain: `java { toolchain { languageVersion = JavaLanguageVersion.of(26) } }`
- Import Spring Boot BOM via `implementation platform("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")`
- Do NOT have `jar { enabled = false }`

### Service Projects (`microservices/*`, `spring-cloud/*`)

- Use both `org.springframework.boot` version `4.1.0` and `io.spring.dependency-management` version `1.1.7` plugins
- Declare `java` plugin
- Configure Java toolchain: `java { toolchain { languageVersion = JavaLanguageVersion.of(26) } }`
- Must have `jar { enabled = false }` to prevent plain jar generation
- Must declare `implementation project(':api')` and `implementation project(':util')`
- Use `useJUnitPlatform()` in test task

## Dockerfile Standard

All microservice Dockerfiles MUST use this multi-stage layered pattern:

```dockerfile
FROM eclipse-temurin:26-jre as builder
WORKDIR extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:26-jre
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

## Docker Compose Standards

- Do **NOT** include a top-level `version:` key — it is obsolete in Docker Compose V2 (Docker Engine ≥ 25.0)
- Set `mem_limit: 512m` for all services
- Use `SPRING_PROFILES_ACTIVE=docker` environment variable
- Use `depends_on` with `condition: service_healthy` for database/messaging dependencies
- Use health checks for infrastructure services (MongoDB, MySQL, RabbitMQ)

### Health Check Patterns

```yaml
# MongoDB
healthcheck:
  test: "mongostat -n 1"
  interval: 5s
  timeout: 2s
  retries: 60

# MySQL
healthcheck:
  test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW DATABASES;\""
  interval: 5s
  timeout: 2s
  retries: 60

# RabbitMQ
healthcheck:
  test: ["CMD", "rabbitmqctl", "status"]
  interval: 5s
  timeout: 2s
  retries: 60
```

## Spring Profile Naming

| Profile | Purpose |
|---------|---------|
| `docker` | Docker Compose deployment, overrides hostnames to container names |
| `streaming_partitioned` | Enable consumer group partitioning for Cloud Stream |
| `streaming_instance_0` | First partition instance |
| `streaming_instance_1` | Second partition instance |
| `kafka` | Use Kafka binder instead of default RabbitMQ |

## Test Patterns

### Integration Test Script (`test-em-all.bash`)

Every project MUST have a `test-em-all.bash` at the root that:
1. Defines `assertCurl()` and `assertEqual()` helper functions
2. Supports `start` and `stop` arguments for Docker Compose lifecycle
3. Uses `waitForService` to poll until services are healthy
4. Tests happy paths (200 OK with expected response shapes)
5. Tests error paths (404 Not Found, 422 Unprocessable Entity, 400 Bad Request)
6. Uses `jq` for JSON response parsing

### Unit/Integration Tests

- Use JUnit 5 (`useJUnitPlatform()`)
- Use Testcontainers for database tests (when persistence layer is enabled)
- Use `reactor-test` for reactive stream tests (when reactive layer is enabled)
- Use `@SpringBootTest` with `webEnvironment = RANDOM_PORT` for integration tests

## Naming Conventions

| Item | Convention | Example |
|------|-----------|---------|
| Service interface | `<Entity>Service` | `ProductService`, `ReviewService` |
| Service implementation | `<Entity>ServiceImpl` | `ProductServiceImpl` |
| DTO / Model | `<Entity>` | `Product`, `Review`, `Recommendation` |
| Aggregate DTO | `<Entity>Aggregate` | `ProductAggregate` |
| Summary DTO | `<Entity>Summary` | `ReviewSummary`, `RecommendationSummary` |
| Entity class | `<Entity>Entity` | `ProductEntity`, `ReviewEntity` |
| Repository interface | `<Entity>Repository` | `ProductRepository` |
| Mapper (MapStruct) | `<Entity>Mapper` | `ProductMapper` |
| Integration helper | `<Domain>Integration` | `ProductCompositeIntegration` |
| Application class | `<Service>Application` | `ProductServiceApplication` |
| Exception classes | `<Description>Exception` | `NotFoundException`, `InvalidInputException` |
| Event envelope | `Event<K, T>` | `Event<Integer, Recommendation>` |
| Message processor | `MessageProcessorConfig` | `MessageProcessorConfig` (one per core service) |

## Error Handling

- All exceptions flow through `GlobalControllerExceptionHandler` in the `util` module
- Standard HTTP error responses use `HttpErrorInfo` record/class
- Two custom exceptions: `NotFoundException` (→ 404) and `InvalidInputException` (→ 422)
- Event processing failures use `EventProcessingException`
- Composite service wraps downstream HTTP errors and re-throws appropriate exceptions

## Commit Message Format

```
iteration(<layer>): <short description>

- <bullet point of what was added/changed>
- <bullet point of what was added/changed>
```

Example:
```
iteration(persistence): add persistence layer with MongoDB and MySQL

- Added Spring Data MongoDB to MongoDB-backed core services
- Added Spring Data JPA to JPA-backed core services with MySQL
- Added MapStruct entity-to-DTO mapping
- Added Testcontainers for database integration tests
- Extended composite API with create/delete operations
```
