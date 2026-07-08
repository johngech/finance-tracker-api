# Spring Cloud Sub-Skill — Cloud-Native Infrastructure Patterns

This sub-skill covers the six Spring Cloud patterns added progressively to the microservice landscape. Each pattern adds a dedicated module under `spring-cloud/` and modifies existing microservices.

> `${basePackage}` is a project-configurable placeholder. Substitute your project's base package when generating code.

---

## Pattern 1 — Service Discovery (Eureka)

### New Module: `spring-cloud/eureka-server/`

**`build.gradle`:**
```groovy
plugins {
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = '${basePackage}.springcloud.eurekaserver'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

jar { enabled = false }

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = "2025.1.2"
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

**`EurekaServerApplication.java`:**
```java
package ${basePackage}.springcloud.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**`application.yml`:**
```yaml
server.port: 8761

eureka:
  instance:
    hostname: localhost
  client:
    registerWithEureka: false
    fetchRegistry: false
    serviceUrl:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    waitTimeInMsWhenSyncEmpty: 0
    response-cache-update-interval-ms: 5000
```

### Modifications to Existing Microservices

Add `spring-cloud-starter-netflix-eureka-client` dependency each microservice `build.gradle`:
```groovy
implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
```

Add to each `application.yml`:
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5

spring.application.name: <service-name>
```

### Docker Compose Addition

```yaml
eureka:
  build: spring-cloud/eureka-server
  mem_limit: 512m
  ports:
    - "8761:8761"
```

---

## Pattern 2 — Edge Server (Gateway)

### New Module: `spring-cloud/gateway/`

**`build.gradle`:**
```groovy
plugins {
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = '${basePackage}.springcloud.gateway'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

jar { enabled = false }

repositories {
    mavenCentral()
}

ext {
    springCloudVersion = "2025.1.2"
}

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
    }
}
```

**`GatewayApplication.java`:**
```java
package ${basePackage}.springcloud.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

**`application.yml`:**
```yaml
server.port: 8443

# Define routes for each composite/aggregate service.
# Replace <composite-service> with your actual service name from project.yml.
spring.cloud.gateway.routes:
  - id: <composite-service>
    uri: lb://<composite-service>
    predicates:
      - Path=/<composite-path>/**

  - id: <composite-service>-swagger-ui
    uri: lb://<composite-service>
    predicates:
      - Path=/openapi/**

  - id: eureka-api
    uri: http://${app.eureka-server}:8761
    predicates:
      - Path=/eureka/api/{segment}
    filters:
      - SetPath=/eureka/{segment}

  - id: eureka-web-start
    uri: http://${app.eureka-server}:8761
    predicates:
      - Path=/eureka/web
    filters:
      - SetPath=/

  - id: eureka-web-other
    uri: http://${app.eureka-server}:8761
    predicates:
      - Path=/eureka/**

app:
  eureka-server: localhost
```

> **Example:** In the book reference, `<composite-service>` = `product-composite`, `<composite-path>` = `product-composite`.

### HTTPS Configuration

For secure gateway setup, generate a self-signed certificate:
```bash
keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore edge.p12 -validity 3650
```

Add to `application.yml`:
```yaml
server:
  port: 8443
  ssl:
    key-store-type: PKCS12
    key-store: classpath:keystore/edge.p12
    key-store-password: ${GATEWAY_TLS_PWD}
    key-alias: localhost
```

---

## Pattern 3 — Security (OAuth 2.0 / OIDC)

### New Module: `spring-cloud/authorization-server/`

- Spring Authorization Server or Auth0 integration
- Issues JWT access tokens
- Configures `SecurityWebFilterChain` with JWT resource server

### Modifications to Existing Services

**Composite Service (or any API-exposing service):**
- `@PreAuthorize("hasAuthority('SCOPE_<domain>:read')")` on read endpoints
- `@PreAuthorize("hasAuthority('SCOPE_<domain>:write')")` on write endpoints
- `SecurityConfig.java` — configures `SecurityWebFilterChain`

> **Example:** `SCOPE_product:read`, `SCOPE_product:write` for the product domain. Replace `<domain>` with your actual scope prefix.

**Gateway:**
- Token relay filter: `TokenRelay` gateway filter
- CSRF disabled for stateless API

**All services:** Add `spring-boot-starter-security` and `spring-security-oauth2-resource-server` dependencies.

---

## Pattern 4 — Centralized Configuration (Config Server)

### New Module: `spring-cloud/config-server/`

- `spring-cloud-config-server` dependency
- `@EnableConfigServer` annotation
- `application.yml` pointing to `config-repo/` directory

### Config Repository: `config-repo/`

One YAML file per service:
```
config-repo/
├── application.yml           # shared defaults
├── <core-service-a>.yml
├── <core-service-b>.yml
├── <composite-service>.yml
├── eureka-server.yml
├── gateway.yml
└── auth-server.yml
```

> Create one YAML per service declared in `settings.gradle`. Names match `spring.application.name` of each service.

### Modifications

All services add `spring-cloud-starter-config` and `spring.config.import: configserver:` to bootstrap.

Sensitive values (DB passwords, JWT secrets) use:
```yaml
spring.config.import: "configserver:"
encrypt:
  key: ${CONFIG_SERVER_ENCRYPT_KEY}
```

---

## Pattern 5 — Resilience (Resilience4j)

Applied to the composite service (the service that aggregates core services):

### Circuit Breaker
```yaml
resilience4j.circuitbreaker:
  instances:
    product:
      allowHealthIndicatorToFail: false
      registerHealthIndicator: true
      slidingWindowType: COUNT_BASED
      slidingWindowSize: 5
      failureRateThreshold: 50
      waitDurationInOpenState: 10000
      permittedNumberOfCallsInHalfOpenState: 3
      automaticTransitionFromOpenToHalfOpenEnabled: true
```

### Time Limiter
```yaml
resilience4j.timelimiter:
  instances:
    product:
      timeoutDuration: 2s
```

### Retry
```yaml
resilience4j.retry:
  instances:
    product:
      maxAttempts: 3
      waitDuration: 1000
      retryExceptions:
        - org.springframework.web.reactive.function.client.WebClientResponseException$InternalServerError
```

### Implementation Pattern
```java
@CircuitBreaker(name = "product", fallbackMethod = "getProductFallback")
@TimeLimiter(name = "product")
@Retry(name = "product")
public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
    // ...
}

private Mono<Product> getProductFallback(int productId, int delay, int faultPercent, Throwable ex) {
    // Return cached or default response
}
```

---

## Pattern 6 — Distributed Tracing

### Dependencies

All microservices add:
```groovy
implementation 'io.micrometer:micrometer-tracing-bridge-otel'
implementation 'io.opentelemetry:opentelemetry-exporter-zipkin'
```

### Configuration
```yaml
management.tracing:
  sampling:
    probability: 1.0

management.zipkin.tracing.endpoint: http://zipkin:9411/api/v2/spans

logging.pattern.level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Docker Compose
```yaml
zipkin:
  image: openzipkin/zipkin:2.24.3
  mem_limit: 1024m
  ports:
    - "9411:9411"
  environment:
    - STORAGE_TYPE=mem
```

### Verification
After tracing is enabled, `test-em-all.bash` should show correlated trace IDs across all services in the call chain. The Zipkin UI at `http://localhost:9411` shows the distributed trace waterfall.

---

## Pattern Application Order

These patterns must be applied in sequence because each builds on infrastructure from the previous:

```
base-services → openapi → persistence → reactive
    → discovery (Pattern 1)
    → edge-server (Pattern 2)
    → security (Pattern 3)
    → centralized-config (Pattern 4)
    → resilience (Pattern 5)
    → tracing (Pattern 6)
```

Each pattern corresponds to a scaffold layer. After each pattern is applied, run the governance sub-skill to validate compliance.
