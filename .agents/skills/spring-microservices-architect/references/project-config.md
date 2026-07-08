# Project Configuration — Portable Settings for Any Spring Boot Microservice Project

This reference defines **all configurable variables** that make the skill transferable to any Spring Boot microservice project. Every adopter project MUST create a `.agents/project.yml` (or set these values in their first interaction) before generating code.

> The skill uses `${variable}` placeholders throughout all sub-skills. This file maps each variable to its purpose and default value.

---

## Required Variables

These MUST be set before any code generation. The skill will prompt for them on first use.

| Variable | Description | Example Values |
|----------|-------------|----------------|
| `${basePackage}` | Root Java package for all modules | `com.projects`, `org.acme.shop`, `com.projects` |
| `${projectName}` | Human-readable project name (used in docs/commits) | `E-Commerce Platform`, `Logistics Hub` |
| `${artifactPrefix}` | Maven/Gradle group ID prefix | `com.projects`, `org.acme` |

---

## Service Registry

Define your microservices here. The skill generates code for **whatever services you declare** — it is not limited to any fixed set.

### Structure

```yaml
# .agents/project.yml (create this file in your project root)

project:
  name: "${projectName}"
  basePackage: "${basePackage}"
  artifactPrefix: "${artifactPrefix}"

services:
  core:
    # Define as many core services as your domain requires.
    # Each entry maps a service key → its configuration.
    - name: product-service
      entity: Product
      package: product
      port: 7001
      persistence: mongodb-reactive    # mongodb-reactive | mongodb | jpa | redis | elasticsearch
      eventTopic: products

    - name: review-service
      entity: Review
      package: review
      port: 7002
      persistence: jpa
      eventTopic: reviews

    # Add more core services as needed...

  composite:
    - name: product-composite-service
      package: product
      port: 7000
      aggregates: [product-service, review-service]  # services it calls

  infrastructure:
    eureka-server:
      port: 8761
    gateway:
      port: 8443
    authorization-server:
      port: 9999
    config-server:
      port: 8888
```

### Port Assignment Convention

Ports are project-specific. Use any scheme that avoids conflicts. Recommended pattern:

| Range | Purpose |
|-------|---------|
| `7000–7099` | Application microservices |
| `8000–8099` | Shared libraries (if needed) |
| `8443` | Gateway (HTTPS) |
| `8761` | Eureka Server |
| `8888` | Config Server |
| `9999` | Authorization Server |
| `9411` | Zipkin |
| `27017` | MongoDB |
| `3306` | MySQL |
| `5672` / `15672` | RabbitMQ |
| `6379` | Redis |
| `9200` | Elasticsearch |

> **Rule:** All services run on port `8080` inside Docker containers. The "default port" is only used for local development (`application.yml → server.port`).

---

## Persistence Defaults

| Strategy Key | Starter Artifact | Entity Annotation | Repository Base | ID Type |
|-------------|-----------------|-------------------|-----------------|---------|
| `mongodb-reactive` | `spring-boot-starter-data-mongodb-reactive` | `@Document` | `ReactiveCrudRepository` | `String` |
| `mongodb` | `spring-boot-starter-data-mongodb` | `@Document` | `MongoRepository` | `String` |
| `jpa` | `spring-boot-starter-data-jpa` | `@jakarta.persistence.Entity` | `CrudRepository` / `JpaRepository` | `int` / `long` |
| `redis` | `spring-boot-starter-data-redis-reactive` | `@RedisHash` | `CrudRepository` | `String` |
| `elasticsearch` | `spring-boot-starter-data-elasticsearch` | `@Document` (ES) | `ElasticsearchRepository` | `String` |

---

## Build System

| Setting | Default | Options |
|---------|---------|---------|
| Build tool | Gradle (Kotlin DSL or Groovy DSL) | Gradle only (enforced) |
| Wrapper | `gradlew` / `gradlew.bat` | Committed to repo |
| Multi-project | Root `settings.gradle` | All modules declared here |

---

## Docker Configuration

| Setting | Default | Customizable |
|---------|---------|-------------|
| Base image | `eclipse-temurin:${javaVersion}-jre` | Yes — change Java version |
| Compose version | V2 (no `version:` key) | No — Compose V2 is mandatory |
| Memory limit | `512m` per service | Yes — per service |
| Spring profile | `docker` | Additional profiles additive |
| Registry prefix | (none — local images) | Set `${dockerRegistry}` for push |

---

## Adoption Checklist

When transferring this skill to a new project:

1. **Create `.agents/project.yml`** with your service registry (or answer prompts on first use)
2. **Set `${basePackage}`** — all package names derive from this
3. **Define your core services** — not limited to any preset list
4. **Choose persistence per service** — MongoDB, JPA, Redis, or Elasticsearch
5. **Assign ports** — any non-conflicting scheme
6. **Run `scaffold` sub-skill** — generates the project structure from your registry
7. **Run `governance` sub-skill** — validates everything is wired correctly

> **Key principle:** The skill generates code for YOUR services, not a fixed set. The book's product/review/recommendation services are one example instantiation. An e-commerce platform, logistics hub, or fintech system all use the same patterns with different service names.

---

## Example Instantiations

The same skill patterns work for any domain:

### Example A — Book Reference (Product Reviews)
```yaml
services:
  core:
    - { name: product-service, entity: Product, port: 7001, persistence: mongodb-reactive }
    - { name: review-service, entity: Review, port: 7002, persistence: jpa }
    - { name: recommendation-service, entity: Recommendation, port: 7003, persistence: mongodb-reactive }
  composite:
    - { name: product-composite-service, package: product, port: 7000 }
```

### Example B — E-Commerce Platform
```yaml
services:
  core:
    - { name: customer-service, entity: Customer, port: 7010, persistence: mongodb-reactive }
    - { name: product-catalog-service, entity: ProductCatalog, port: 7012, persistence: mongodb-reactive }
    - { name: order-service, entity: Order, port: 7016, persistence: mongodb-reactive }
    - { name: payment-service, entity: Payment, port: 7017, persistence: mongodb-reactive }
    - { name: inventory-service, entity: Inventory, port: 7014, persistence: mongodb-reactive }
  composite:
    - { name: storefront-composite-service, package: storefront, port: 7009 }
```

### Example C — Logistics Platform
```yaml
services:
  core:
    - { name: fleet-service, entity: Vehicle, port: 7001, persistence: mongodb-reactive }
    - { name: route-service, entity: Route, port: 7002, persistence: mongodb-reactive }
    - { name: shipment-service, entity: Shipment, port: 7003, persistence: jpa }
    - { name: tracking-service, entity: TrackingEvent, port: 7004, persistence: mongodb-reactive }
    - { name: warehouse-service, entity: Warehouse, port: 7005, persistence: jpa }
  composite:
    - { name: dispatch-composite-service, package: dispatch, port: 7000 }
```

### Example D — FinTech Platform
```yaml
services:
  core:
    - { name: account-service, entity: Account, port: 7001, persistence: jpa }
    - { name: transaction-service, entity: Transaction, port: 7002, persistence: jpa }
    - { name: kyc-service, entity: KycRecord, port: 7003, persistence: mongodb-reactive }
    - { name: notification-service, entity: Notification, port: 7004, persistence: mongodb-reactive }
  composite:
    - { name: banking-composite-service, package: banking, port: 7000 }
```
