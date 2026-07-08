---
name: spring-microservices-architect
description: "Production-grade governance agent for Spring Boot microservices. Scaffolds projects iteratively using capability-based layering, enforces coding standards, and validates against battle-tested reference patterns. Fully portable — works with any domain. USE FOR: microservice, Spring Boot, scaffold, Docker compose, kubernetes, helm, eureka, gateway, resilience4j, reactive, spring cloud, openapi, persistence, security, oauth, tracing, zipkin, monitoring, prometheus, grafana, native compilation, graalvm, code review, architecture review, quality gate, governance, spring cloud stream, rabbitmq, kafka, testcontainers, mapstruct, service discovery, edge server, config server, circuit breaker, distributed tracing, entity, entities, domain model, generate entity, persistence model, create entity, MongoDB document, JPA entity, MapStruct mapper, repository, test, verify, validate, TDD, test-driven, failing test, integration test, build check, regression test, quality check, security database, MFA, multi-factor, WebAuthn, passkey, UserDetailsService, JdbcUserDetailsManager, one-time token, OTT, magic link, passwordless, OAuth2 schema, session repository, security audit, password encoder, BCrypt, Argon2. DO NOT USE FOR: non-Java projects, frontend-only apps, Azure/AWS-specific deployments. For e-commerce domain services, use the spring-ecommerce-architect skill."
license: MIT
metadata:
  author: Jay Patil
  version: "3.0.0"
---

# Spring Microservices Architect Skill

Production-grade **software architect governance agent** for Spring Boot microservice projects. Scaffolds new microservices iteratively using capability-based layering, enforces coding standards, and validates against battle-tested reference patterns.

**Fully portable** — not tied to any specific domain. Works for e-commerce, logistics, fintech, SaaS, or any microservice architecture. See [project-config.md](references/project-config.md) for adoption.

---

## Transferability

This skill generates code for **your** services, not a fixed set. To adopt it for a new project:

1. Read [project-config.md](references/project-config.md) and create `.agents/project.yml`
2. Set `${basePackage}`, define your services, assign ports
3. Run `scaffold` — the skill generates YOUR project structure
4. All patterns (persistence, messaging, security, k8s) apply to any service names

> Templates use `<Entity>`, `<Service>`, and `${basePackage}` placeholders. You provide the concrete names.

---

## Context Gathering

Before each interaction, gather project state with these commands:

| Command | Purpose |
|---------|---------|
| `git log --oneline -10` | Detect current capability layer from commit history |
| `docker compose ps --format json 2>/dev/null` | Running containers to understand runtime landscape |
| `./gradlew projects --quiet 2>/dev/null` | Gradle multi-project structure — which modules exist |
| `cat .agents/project.yml 2>/dev/null` | Project-specific configuration (services, ports, packages) |

---

## Sub-Skills

> **MANDATORY: Before executing ANY workflow, you MUST read the corresponding sub-skill document.** The skill document contains required workflow steps, pre-checks, and validation logic that must be followed.

### Platform Layer — Infrastructure & Scaffolding

| Sub-Skill | When to Use | Reference |
|-----------|-------------|-----------|
| **scaffold** | Create new microservice, scaffold project structure, add layers | [scaffold.md](sub-skills/scaffold/scaffold.md) |
| **api-patterns** | API-first interface design, DTO → Service → Entity → Mapper → Event pattern | [api-patterns.md](sub-skills/api-patterns/api-patterns.md) |
| **dockerize** | Dockerize services, create/update docker-compose files | [dockerize.md](sub-skills/dockerize/dockerize.md) |
| **spring-cloud** | Eureka, Gateway, Config Server, OAuth2, Resilience4j, Tracing | [spring-cloud.md](sub-skills/spring-cloud/spring-cloud.md) |
| **kubernetes** | Helm charts, K8s-native patterns, Istio service mesh | [kubernetes.md](sub-skills/kubernetes/kubernetes.md) |
| **observe** | Prometheus/Grafana monitoring, EFK logging, GraalVM native | [observe.md](sub-skills/observe/observe.md) |

### Workflow Layer — Code Generation & Quality

| Sub-Skill | When to Use | Reference |
|-----------|-------------|-----------|
| **entity-gen** | Generate domain entities: DTO, Entity, Repository, Mapper, tests | [entity-gen.md](sub-skills/entity-gen/entity-gen.md) |
| **verify** | Test-driven verification: RED → GREEN → DIFF → REGRESSION | [verify.md](sub-skills/verify/verify.md) |
| **governance** | Quality gates, compliance checks, architecture review | [governance.md](sub-skills/governance/governance.md) |
| **security-db** | Spring Security 7.0 DB: MFA, OAuth2 JDBC, OTT, session, audit | [security-db.md](sub-skills/security-db/security-db.md) |

### Domain Layer — E-Commerce Extensions (Separate Skill)

E-commerce domain services have been moved to a dedicated skill for better modularity:

> **See [spring-ecommerce-architect](../spring-ecommerce-architect/SKILL.md)** — 21 production-grade commerce services (core, inventory, OMS, integration).

---

## Iteration Lifecycle

Match user intent to workflow. Read each sub-skill in order before executing.

### Core Workflows

| User Intent | Workflow Chain |
|-------------|----------------|
| Start a new project from scratch | scaffold → api-patterns → dockerize → governance |
| Add persistence layer | entity-gen → scaffold (persistence) → verify → governance |
| Make services reactive with messaging | scaffold (reactive) → api-patterns → dockerize → governance |
| Add service discovery + edge server | spring-cloud (Eureka + Gateway) → governance |
| Secure APIs with OAuth 2.0 | spring-cloud (Security) → governance |
| Centralize configuration | spring-cloud (Config) → governance |
| Add resilience and tracing | spring-cloud (Resilience4j + Tracing) → governance |
| Deploy to Kubernetes | kubernetes → governance |
| Add monitoring and logging | observe → governance |
| Compile to native images | observe (Native) → governance |
| Full architecture review | governance |

### Entity & Test Workflows

| User Intent | Workflow Chain |
|-------------|----------------|
| Generate a new domain entity | entity-gen → verify → governance |
| Run tests / verify code changes | verify → governance |
| Write failing test first (TDD) | verify (test-first) → entity-gen or scaffold → verify (green) → governance |

### Security Workflows

| User Intent | Workflow Chain |
|-------------|----------------|
| Add database-backed security / MFA | security-db → entity-gen (user schema) → verify → governance |
| Integrate OAuth2 AuthZ Server with DB | security-db (OAuth2 schema) → spring-cloud (Security) → verify → governance |
| Add passwordless / magic-link login | security-db (OTT) → verify → governance |
| Add WebAuthn / Passkey support | security-db (factor-aware schema) → verify → governance |
| Enable security audit logging | security-db (audit) → verify → governance |

### Domain Workflows (E-Commerce — Separate Skill)

E-commerce workflows are defined in the [spring-ecommerce-architect](../spring-ecommerce-architect/SKILL.md) skill.

---

## 4-Step Playbook

Every interaction follows these four steps:

### Step 1: Detect Iteration

Read the injected `git_iteration_state`, `gradle_project_structure`, and `project_config` outputs. Compare against [layer-map.md](references/layer-map.md).

**Detection rules (generic — works for any service names):**

| Signal | Layer |
|--------|-------|
| Only `api/`, `util/`, `microservices/` exist | **base-services** |
| `build.gradle` contains `springdoc-openapi` | **openapi** |
| `build.gradle` contains `data-mongodb` or `data-jpa` | **persistence** |
| `build.gradle` contains `spring-cloud-starter-stream` | **reactive** |
| `spring-cloud/eureka-server/` exists | **discovery** |
| `spring-cloud/gateway/` exists | **edge-server** |
| `spring-cloud/authorization-server/` exists | **security** |
| `build.gradle` contains `spring-session-jdbc` or Flyway security migrations | **security-db** |
| `spring-cloud/config-server/` exists | **centralized-config** |
| `build.gradle` contains `resilience4j` | **resilience** |
| `build.gradle` contains `micrometer-tracing` | **tracing** |
| `kubernetes/helm/` exists | **kubernetes** |
| Istio configs exist | **service-mesh** |
| `docker-compose-native.yml` exists | **native** |

### Step 2: Load Conventions

Read [conventions.md](references/conventions.md), [tech-stack.md](references/tech-stack.md), and [project-config.md](references/project-config.md). Non-negotiable standards:

- Package naming via `${basePackage}`
- Gradle multi-project with root `settings.gradle`
- Java 26 via toolchain
- Spring Boot 4.1.0 with Spring Cloud 2025.1.x
- Layered Dockerfiles using `eclipse-temurin:26-jre`
- Test patterns: `test-em-all.bash` + JUnit 5 + Testcontainers

### Step 3: Execute Sub-Skill

Route to the matched sub-skill. Generate or modify code following conventions.

### Step 4: Validate & Commit

1. **Verify:** Run [verify](sub-skills/verify/verify.md) — compile, unit tests, integration tests, pattern-diff
2. **Governance:** Run [governance](sub-skills/governance/governance.md) — all 9 quality gates
3. **Commit:** Suggest message in format `iteration(<layer>): <description>`

---

## References

| Reference | Purpose |
|-----------|---------|
| [Project Config](references/project-config.md) | **START HERE** — portable settings, service registry, adoption guide |
| [Conventions](references/conventions.md) | Enforced coding standards (packages, Dockerfiles, tests, naming) |
| [Tech Stack](references/tech-stack.md) | Version-pinned dependency matrix |
| [Layer Map](references/layer-map.md) | Capability layer progression and detection signals |
| [E-Commerce Skill](../spring-ecommerce-architect/SKILL.md) | Domain-specific e-commerce services (separate skill) |
