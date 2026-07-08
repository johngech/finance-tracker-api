# Layer Map — Capability Layer Progression

This reference maps each capability layer to the features it adds, the key modules/files changed, and the detection signals that indicate this layer is present.

## Layer Progression

| Layer | Layer Name | Capabilities Added | Key Modules/Files | Detection Signal |
|-------|-----------|-------------------|-------------------|-----------------|
| **base-services** | Base Microservices | Cooperating microservices with REST APIs, shared API library, shared util library | `api/`, `util/`, `microservices/<core-services>/`, `microservices/<composite-service>/` | `settings.gradle` includes `:api`, `:util`, `:microservices:*` |
| **docker** | Docker Deployment | Dockerfiles, Docker Compose orchestration, Spring `docker` profile | `Dockerfile` per service, `docker-compose.yml`, `test-em-all.bash` | `Dockerfile` exists in each microservice dir |
| **openapi** | OpenAPI Documentation | springdoc-openapi integration, Swagger UI, API annotations | `build.gradle` adds springdoc dep, OpenAPI annotations on API interfaces | `springdoc-openapi` in `build.gradle` dependencies |
| **persistence** | Persistence | MongoDB / MySQL per service (configurable), MapStruct entity mapping, Testcontainers | `build.gradle` adds data-mongodb / data-jpa, entity classes, repository interfaces | `spring-boot-starter-data-mongodb` or `spring-boot-starter-data-jpa` in `build.gradle` |
| **reactive** | Reactive + Messaging | Non-blocking REST, Spring Cloud Stream, RabbitMQ/Kafka, reactive MongoDB driver, event-driven | `build.gradle` adds `spring-cloud-starter-stream-rabbit/kafka`, `docker-compose-kafka.yml`, `docker-compose-partitions.yml` | `spring-cloud-starter-stream` in `build.gradle` |
| **discovery** | Service Discovery | Netflix Eureka server, Eureka client registration, Spring Cloud LoadBalancer | `spring-cloud/eureka-server/`, Eureka client config in `application.yml` | `spring-cloud/eureka-server/` directory exists |
| **edge-server** | Edge Server | Spring Cloud Gateway, routing rules, API exposure control | `spring-cloud/gateway/`, gateway routing config | `spring-cloud/gateway/` directory exists |
| **security** | API Security | OAuth 2.0 Authorization Server, HTTPS, OpenID Connect, Auth0 external provider | `spring-cloud/authorization-server/`, `auth0/` config, HTTPS keystore | `spring-cloud/authorization-server/` directory exists |
| **security-db** | Security DB Integration | Factor-aware user schema (MFA, WebAuthn/Passkeys), custom UserDetailsService, OAuth2 JDBC storage, JdbcUserDetailsManager, OTT persistence, Spring Session JDBC, security audit logging, Flyway migrations | `**/persistence/UserEntity.java` (with MFA fields), `**/security/FactorAwareUserDetailsService.java`, `**/security/JdbcOneTimeTokenService.java`, `**/config/AuditConfig.java`, `db/migration/V*__*security*.sql` | `spring-session-jdbc` in `build.gradle` OR `UserEntity.java` contains `mfa_secret` / `webauthn_credentials` |
| **centralized-config** | Centralized Config | Spring Cloud Config Server, config repository, encrypted properties | `spring-cloud/config-server/`, `config-repo/` with per-service YAML files, `.env` file | `spring-cloud/config-server/` and `config-repo/` directories exist |
| **resilience** | Resilience | Resilience4j circuit breaker, retry, time limiter, fallback methods | Resilience4j config in `application.yml`, `@CircuitBreaker` annotations | `resilience4j` in `build.gradle` |
| **tracing** | Distributed Tracing | Micrometer Tracing, Zipkin trace collection and visualization | `micrometer-tracing-bridge-otel`, Zipkin in `docker-compose.yml` | `micrometer-tracing` in `build.gradle` |
| **kubernetes** | Kubernetes Deployment | Helm charts, packaged microservices, K8s service discovery replaces Eureka | `kubernetes/helm/` with component and environment charts | `kubernetes/helm/` directory exists |
| **k8s-native** | K8s-native Patterns | ConfigMaps replace Config Server, Secrets, Ingress replaces Gateway, cert-manager | `kubernetes/helm/common/templates/_configmap_from_file.yaml`, `_secrets.yaml`, `_ingress.yaml`, `_issuer.yaml` | Helm templates include `_configmap_from_file.yaml` |
| **service-mesh** | Service Mesh | Istio sidecar injection, mTLS, traffic management, observability | Istio VirtualService, DestinationRule, Gateway manifests | Istio-related YAML files in `kubernetes/` |
| **logging** | Centralized Logging | EFK stack (Elasticsearch, Fluentd, Kibana) on Kubernetes | Fluentd DaemonSet, Elasticsearch StatefulSet, Kibana Deployment | EFK manifests in `kubernetes/` |
| **monitoring** | Monitoring | Prometheus scraping, Grafana dashboards, alerting | Prometheus ServiceMonitor, Grafana dashboard JSON | Prometheus/Grafana configs in `kubernetes/` |
| **native** | Native Compilation | GraalVM Native Image, Spring AOT, native Docker Compose | `docker-compose-native.yml`, `docker-compose-partitions-native.yml` | `docker-compose-native.yml` exists |

## E-Commerce Domain Layers (additive — can be applied after any layer ≥ reactive)

| Layer | Layer Name | Capabilities Added | Key Modules/Files | Detection Signal |
|-------|-----------|-------------------|-------------------|-----------------|
| **ecommerce-core** | E-Commerce Core | 10 foundational commerce services + storefront composite | `microservices/customer-service/`, `microservices/auth-service/`, `microservices/product-catalog-service/`, `microservices/search-service/`, `microservices/inventory-service/`, `microservices/cart-service/`, `microservices/order-service/`, `microservices/payment-service/`, `microservices/shipping-service/`, `microservices/notification-service/`, `microservices/storefront-composite-service/` | `settings.gradle` includes `:microservices:customer-service` |
| **ecommerce-inventory** | Inventory Domain | ATP visibility, stock audit, replenishment, reservation | `microservices/inventory-visibility-service/`, `microservices/stock-tracking-service/`, `microservices/replenishment-service/`, `microservices/reservation-service/` | `settings.gradle` includes `:microservices:inventory-visibility-service` |
| **ecommerce-oms** | OMS Pipeline | Order capture, orchestration, fulfillment, shipping logistics, returns | `microservices/order-capture-service/`, `microservices/order-orchestration-service/`, `microservices/fulfillment-service/`, `microservices/shipping-logistics-service/`, `microservices/returns-service/` | `settings.gradle` includes `:microservices:order-capture-service` |
| **ecommerce-integration** | External Integration | Marketplace sync, ERP/accounting connectors | `microservices/marketplace-sync-service/`, `microservices/erp-integration-service/` | `settings.gradle` includes `:microservices:marketplace-sync-service` |

## Dependency Chain

Each layer builds on all previous layers. The dependency chain is:

```
base-services
  → docker
    → openapi
      → persistence
        → reactive
          → discovery
            → edge-server
              → security
                → security-db (optional — adds DB-backed auth, MFA, OTT, audit)
                → centralized-config
                  → resilience
                    → tracing
                      → kubernetes
                        → k8s-native
                          → service-mesh
                          → logging
                          → monitoring
                          → native
```

> The service-mesh, logging, monitoring, and native layers can be applied independently after k8s-native.

## Module Evolution

This table shows how each module evolves as layers are added. Service names are generic — replace with your actual services from `.agents/project.yml` (see [project-config.md](project-config.md)).

| Module | base-services | openapi | persistence | reactive | discovery+ | centralized-config+ | k8s-native+ |
|--------|--------------|---------|-------------|----------|-----------|-------------------|------------|
| `api/` | REST interfaces, DTOs | + OpenAPI annotations | + create/delete operations | Unchanged | Unchanged | Unchanged | Unchanged |
| `util/` | Exception handlers, ServiceUtil | Unchanged | Unchanged | + event processing utils | Unchanged | Unchanged | Unchanged |
| `microservices/<core-service>` (MongoDB) | In-memory stub | Unchanged | + MongoDB persistence | + Reactive MongoDB, Cloud Stream | + Eureka client | + Config client | + K8s ConfigMap |
| `microservices/<core-service>` (JPA) | In-memory stub | Unchanged | + MySQL/JPA persistence | + Cloud Stream (blocking JPA) | + Eureka client | + Config client | + K8s ConfigMap |
| `microservices/<composite-service>` | Aggregator via REST | Unchanged | + create/delete composite | + Reactive aggregation | + Eureka client, LoadBalancer | + Config client | + K8s ConfigMap |
| `spring-cloud/eureka-server` | — | — | — | — | Created | + Config client | Removed (K8s DNS) |
| `spring-cloud/gateway` | — | — | — | — | — | + Config client | Replaced (Ingress) |
| `spring-cloud/authorization-server` | — | — | — | — | — | + Config client | Unchanged |
| `spring-cloud/config-server` | — | — | — | — | — | Created | Removed (ConfigMaps) |
| `config-repo/` | — | — | — | — | — | Created | Removed (ConfigMaps) |
| `kubernetes/helm/` | — | — | — | — | — | — | Created |

> **Example:** In the book reference project, `<core-service>` (MongoDB) = product-service & recommendation-service, `<core-service>` (JPA) = review-service, `<composite-service>` = product-composite-service.
