# Tech Stack — Version-Pinned Dependency Matrix

All generated code MUST use these exact versions. Do NOT upgrade or change versions unless explicitly requested.

## Core Platform

| Technology | Version | Notes |
|-----------|---------|-------|
| Java | 26 | `JavaLanguageVersion.of(26)` via toolchain |
| Spring Boot | 4.1.0 | `org.springframework.boot` plugin |
| Spring Framework | 7.0.8 | Bundled with Spring Boot 4.1.0 |
| Spring Cloud | 2025.1.2 | BOM: `spring-cloud-dependencies` |
| Gradle | 7.x / 8.x | Wrapper included in project |
| Jakarta EE | 9+ | Replaces javax.* packages |
| Docker Engine | 29.3.0 | Required for multi-stage builds and Compose V2 |
| Kubernetes | 1.35 | Target cluster version for Helm deployments |

## Spring Boot Starters

| Starter | Artifact | Layer |
|---------|----------|-------|
| Web (Reactive) | `spring-boot-starter-webflux` | base-services |
| Actuator | `spring-boot-starter-actuator` | base-services |
| MongoDB Reactive | `spring-boot-starter-data-mongodb-reactive` | persistence |
| JPA | `spring-boot-starter-data-jpa` | persistence |
| Security | `spring-boot-starter-security` | security |
| JDBC | `spring-boot-starter-jdbc` | security-db |
| Spring Session JDBC | `org.springframework.session:spring-session-jdbc` | security-db |
| OAuth2 Authorization Server | `spring-security-oauth2-authorization-server` | security-db |
| OAuth2 Resource Server | `spring-boot-starter-oauth2-resource-server` | security |
| Flyway Core | `org.flywaydb:flyway-core` | security-db |
| Flyway MySQL | `org.flywaydb:flyway-mysql` | security-db |

## Spring Cloud Dependencies

| Component | Artifact | Layer |
|-----------|----------|-------|
| Cloud Stream (RabbitMQ) | `spring-cloud-starter-stream-rabbit` | reactive |
| Cloud Stream (Kafka) | `spring-cloud-starter-stream-kafka` | reactive |
| Eureka Server | `spring-cloud-starter-netflix-eureka-server` | discovery |
| Eureka Client | `spring-cloud-starter-netflix-eureka-client` | discovery |
| Gateway | `spring-cloud-starter-gateway` | edge-server |
| Config Server | `spring-cloud-config-server` | centralized-config |
| Config Client | `spring-cloud-starter-config` | centralized-config |
| Resilience4j | `spring-cloud-starter-circuitbreaker-resilience4j` | resilience |

## Third-Party Libraries

| Library | Version | Artifact | Layer |
|---------|---------|----------|-------|
| MapStruct | 1.6.3 | `org.mapstruct:mapstruct` | persistence |
| Testcontainers BOM | 1.20.4 | `org.testcontainers:testcontainers-bom` | persistence |
| Testcontainers MongoDB | (BOM) | `org.testcontainers:mongodb` | persistence |
| Testcontainers MySQL | (BOM) | `org.testcontainers:mysql` | persistence |
| Testcontainers JUnit Jupiter | (BOM) | `org.testcontainers:junit-jupiter` | persistence |
| Netty macOS resolver | (managed) | `io.netty:netty-resolver-dns-native-macos` | base-services |
| MySQL Connector | (managed) | `com.mysql:mysql-connector-j` | persistence, security-db |
| reactor-test | (managed) | `io.projectreactor:reactor-test` | base-services |
| Nimbus JOSE+JWT | (managed) | `com.nimbusds:nimbus-jose-jwt` | security-db (JWK) |

## E-Commerce Libraries

| Library | Version | Artifact | Services |
|---------|---------|----------|----------|
| jjwt API | 0.12.5 | `io.jsonwebtoken:jjwt-api` | Auth Service |
| jjwt Impl | 0.12.5 | `io.jsonwebtoken:jjwt-impl` (runtime) | Auth Service |
| jjwt Jackson | 0.12.5 | `io.jsonwebtoken:jjwt-jackson` (runtime) | Auth Service |
| Spring Data Elasticsearch | (managed) | `spring-boot-starter-data-elasticsearch` | Search Service |
| Spring Data Redis Reactive | (managed) | `spring-boot-starter-data-redis-reactive` | Cart, Reservation, Inventory Visibility |
| Spring Mail | (managed) | `spring-boot-starter-mail` | Notification Service |
| Twilio SDK | 10.1.0 | `com.twilio.sdk:twilio` | Notification Service |
| Firebase Admin | 9.2.0 | `com.google.firebase:firebase-admin` | Notification Service |
| Thymeleaf | (managed) | `spring-boot-starter-thymeleaf` | Notification Service (email templates) |

## Infrastructure Images

| Service | Docker Image | Layer |
|---------|-------------|-------|
| MongoDB | `mongo:7.0` | persistence |
| MySQL | `mysql:8.4` | persistence |
| Redis | `redis:7.2-alpine` | e-commerce (Cart, Reservation, Inventory Visibility) |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.12.2` | e-commerce (Search Service), logging |
| RabbitMQ | `rabbitmq:3.13-management` | reactive |
| Kafka (via Confluent) | varies | reactive |
| Zipkin | `openzipkin/zipkin:2.24.3` | tracing |
| Kibana | `docker.elastic.co/kibana/kibana:8.12.2` | logging |

## Base Docker Image

| Purpose | Image |
|---------|-------|
| Builder stage | `eclipse-temurin:26-jre` |
| Runtime stage | `eclipse-temurin:26-jre` |

## Gradle Plugin Versions

| Plugin | Version |
|--------|---------|
| `org.springframework.boot` | `4.1.0` |
| `io.spring.dependency-management` | `1.1.7` |
| `java` | (built-in) |

## Build Configuration Constants

```groovy
// Service build.gradle
plugins {
    id 'org.springframework.boot' version '4.1.0'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = '${basePackage}.microservices.core.<service>'
version = '1.0.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

// Library build.gradle (api/, util/)
plugins {
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

ext {
    springBootVersion = '4.1.0'
}
```

## MySQL Default Configuration

```yaml
MYSQL_ROOT_PASSWORD: rootpwd
MYSQL_DATABASE: review-db
MYSQL_USER: user
MYSQL_PASSWORD: pwd
```

## Spring Cloud Stream Defaults

```yaml
# Default binder
spring.cloud.stream.defaultBinder: rabbit

# Kafka binder defaults
spring.cloud.stream.kafka.binder:
  brokers: 127.0.0.1
  defaultBrokerPort: 9092

# RabbitMQ defaults
spring.rabbitmq:
  host: 127.0.0.1
  port: 5672
  username: guest
  password: guest
```

## Port Assignments

Port assignments are **project-specific** — they belong in your `.agents/project.yml` service registry, not hardcoded here.

See [project-config.md](project-config.md) for:
- Port range conventions (7000–7099 for app services, 8xxx for infrastructure)
- Service-to-port mapping in your service registry
- The rule: all services run on port `8080` inside Docker; the "default port" is for local development only

### Infrastructure Ports (fixed)

| Service | Port |
|---------|------|
| Eureka Server | 8761 |
| Gateway (HTTPS) | 8443 |
| Config Server | 8888 |
| Authorization Server | 9999 |
| MongoDB | 27017 |
| MySQL | 3306 |
| RabbitMQ | 5672 / 15672 |
| Zipkin | 9411 |
| Redis | 6379 |
| Elasticsearch | 9200 |
