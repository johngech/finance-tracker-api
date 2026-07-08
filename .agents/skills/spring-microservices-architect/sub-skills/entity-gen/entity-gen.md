# Entity Generation Sub-Skill — Domain Entity Workflow

This sub-skill generates domain entities and verifies their integration into the microservice project. It follows a strict **analyze → implement → integrate → verify** pipeline that ensures every new entity is consistent with existing patterns, properly mapped, and fully tested before commit.

> `${basePackage}` is a project-configurable placeholder. `<Entity>` represents the domain concept being generated.

---

## Metadata

```yaml
name: entity-gen
description: >
  Generates domain entities and verifies their integration into the project.
  Produces DTO, Entity, Repository, Mapper, and wiring in a single atomic workflow.
triggers:
  - entity
  - entities
  - domain model
  - persistence model
  - create entity
  - add entity
  - new entity
  - generate entity
  - MongoDB document
  - JPA entity
```

---

## Pre-Checks

Before generating any entity, execute these checks — **abort if any fail**:

1. **Project layer detected:** Confirm the project has at least the **persistence** layer active (check for `spring-boot-starter-data-mongodb` or `spring-boot-starter-data-jpa` in `build.gradle`)
2. **Conventions loaded:** Read [conventions.md](../../references/conventions.md) and [tech-stack.md](../../references/tech-stack.md)
3. **Existing patterns scanned:** List all files under `microservices/*/src/main/java/**/persistence/` to understand the current entity style (field types, annotation patterns, index strategies)

```bash
# Pre-check: persistence layer exists
find . -name "build.gradle" -exec grep -l "data-mongodb\|data-jpa\|data-redis\|data-elasticsearch" {} \;

# Pre-check: scan existing entities for patterns
find . -name "*Entity.java" -path "*/persistence/*" | head -20
find . -name "*Repository.java" -path "*/persistence/*" | head -20
find . -name "*Mapper.java" -path "*/services/*" | head -20
```

---

## 4-Phase Workflow

### Phase 1 — Analyze

Scan the target service's existing code to establish patterns before writing anything.

**Step 1.1 — Locate the service module:**
```
microservices/<entity>-service/
  src/main/java/${basePackage}/microservices/core/<entity>/
    persistence/     ← target directory for Entity + Repository
    services/        ← target directory for Mapper
```

**Step 1.2 — Detect persistence strategy:**

| Signal | Strategy | Entity Annotation | Repository Base |
|--------|----------|-------------------|-----------------|
| `spring-boot-starter-data-mongodb-reactive` | MongoDB Reactive | `@Document` | `ReactiveCrudRepository` |
| `spring-boot-starter-data-mongodb` | MongoDB Blocking | `@Document` | `MongoRepository` |
| `spring-boot-starter-data-jpa` | JPA/MySQL | `@jakarta.persistence.Entity` | `CrudRepository` / `JpaRepository` |
| `spring-boot-starter-data-redis-reactive` | Redis | `@RedisHash` | `CrudRepository` (Spring Data Redis) |
| `spring-boot-starter-data-elasticsearch` | Elasticsearch | `@Document` (ES) | `ElasticsearchRepository` |

**Step 1.3 — Extract existing patterns:**

Read 2–3 existing `*Entity.java` files in the project and record:
- ID field type (`String` for MongoDB, `int`/`long` for JPA)
- Whether `@Version` (optimistic locking) is used
- Compound index style (`@CompoundIndex` for MongoDB, `@UniqueConstraint` for JPA)
- Timestamp patterns (`LocalDateTime createdAt/updatedAt` or absent)
- Embedded value objects vs flat fields

**Step 1.4 — Check the DTO already exists:**

If the corresponding DTO (`<Entity>.java` in `api/core/<entity>/`) already exists, read it to:
- Map DTO fields → Entity fields (identify naming gaps for MapStruct)
- Identify runtime-only fields to ignore (`serviceAddress`)
- Identify persistence-only fields to add (`id`, `version`, `createdAt`, `updatedAt`)

> **If the DTO does NOT exist:** Generate it first following [api-patterns.md](../api-patterns/api-patterns.md) Section 1 before proceeding.

---

### Phase 2 — Implement

Generate the entity files one at a time in strict order. Each file MUST compile before proceeding to the next.

#### 2.1 — Entity Class: `<Entity>Entity.java`

**MongoDB (reactive) template:**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "<entities>")
@CompoundIndex(name = "<parent>-<entity>-id", unique = true,
    def = "{'<parentId>': 1, '<entityId>': 1}")
public class <Entity>Entity {

  @Id private String id;
  @Version private Integer version;

  // --- Domain fields (mirror DTO, may use different names) ---
  private int <parentId>;
  private int <entityId>;
  // ... additional domain fields ...

  // --- Persistence-only fields ---
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public <Entity>Entity() {}

  // All-args constructor (excluding id, version — auto-managed)
  public <Entity>Entity(int <parentId>, int <entityId> /* , ... */) {
    this.<parentId> = <parentId>;
    this.<entityId> = <entityId>;
    this.createdAt = LocalDateTime.now();
  }

  // Getters and setters for ALL fields (including id, version)
}
```

**JPA/MySQL template:**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "<entities>",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"<parentId>", "<entityId>"}))
public class <Entity>Entity {

  @Id @GeneratedValue private int id;
  @Version private int version;

  private int <parentId>;
  private int <entityId>;
  // ... domain fields ...

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public <Entity>Entity() {}
  // All-args constructor, getters, setters
}
```

**Redis template (for cache/session entities):**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@RedisHash(value = "<entity>", timeToLive = 86400)  // 24h default
public class <Entity>RedisEntity {

  @Id private String <entityId>;
  @TimeToLive private Long ttlSeconds;
  // ... domain fields ...

  public <Entity>RedisEntity() {}
}
```

**Elasticsearch template:**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "<entities>")
public class <Entity>SearchDocument {

  @Id private String <entityId>;
  @Field(type = FieldType.Text, analyzer = "standard") private String <textField>;
  @Field(type = FieldType.Keyword) private String <keywordField>;
  @Field(type = FieldType.Double) private BigDecimal <numericField>;

  public <Entity>SearchDocument() {}
}
```

**Entity generation rules:**
- `@Id` is ALWAYS present — `String` for MongoDB/Redis/ES, `int` for JPA
- `@Version` is ALWAYS present for MongoDB and JPA (optimistic concurrency)
- Collection/table name is the **plural lowercase** of the entity (`products`, `orders`, `customers`)
- Compound indexes cover the most common query pattern (typically foreign-key + natural-key)
- `createdAt` / `updatedAt` timestamps are ALWAYS included for MongoDB and JPA
- Enum fields are stored as `String` in entities (not the enum type) for schema evolution safety
- Embedded value objects (e.g., `Address`) are stored as nested documents in MongoDB, or as `@Embedded` in JPA

---

#### 2.2 — Repository: `<Entity>Repository.java`

**MongoDB Reactive:**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface <Entity>Repository
    extends ReactiveCrudRepository<<Entity>Entity, String> {

  Flux<<Entity>Entity> findBy<ParentId>(int <parentId>);
}
```

**JPA/MySQL:**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface <Entity>Repository
    extends CrudRepository<<Entity>Entity, Integer> {

  List<<Entity>Entity> findBy<ParentId>(int <parentId>);
}
```

**Repository generation rules:**
- Extends `ReactiveCrudRepository` for reactive MongoDB, `CrudRepository` for JPA
- Primary query method matches the main lookup pattern (usually by parent/foreign key)
- Method naming follows Spring Data derived query convention: `findBy<FieldName>`
- Return type is `Flux<T>` for reactive, `List<T>` for blocking

---

#### 2.3 — Mapper: `<Entity>Mapper.java` (MapStruct)

```java
package ${basePackage}.microservices.core.<entity>.services;

import org.mapstruct.*;
import ${basePackage}.api.core.<entity>.<Entity>;
import ${basePackage}.microservices.core.<entity>.persistence.<Entity>Entity;
import java.util.List;

@Mapper(componentModel = "spring")
public interface <Entity>Mapper {

  @Mappings({
    @Mapping(target = "serviceAddress", ignore = true)
    // Add explicit mappings ONLY if DTO and Entity field names differ:
    // @Mapping(target = "<dtoField>", source = "entity.<entityField>")
  })
  <Entity> entityToApi(<Entity>Entity entity);

  @Mappings({
    @Mapping(target = "id", ignore = true),
    @Mapping(target = "version", ignore = true),
    @Mapping(target = "createdAt", ignore = true),
    @Mapping(target = "updatedAt", ignore = true)
    // Add explicit reverse mappings if names differ
  })
  <Entity>Entity apiToEntity(<Entity> api);

  List<<Entity>> entityListToApiList(List<<Entity>Entity> entity);
  List<<Entity>Entity> apiListToEntityList(List<<Entity>> api);
}
```

**Mapper generation rules:**
- `componentModel = "spring"` — always (Spring-managed bean)
- ALWAYS ignore `id`, `version` on `apiToEntity` (persistence-only)
- ALWAYS ignore `serviceAddress` on `entityToApi` (runtime-only)
- ALWAYS ignore `createdAt`, `updatedAt` on `apiToEntity`
- Only add `@Mapping` annotations when field names differ between DTO and Entity
- If all names match, omit `@Mappings` blocks entirely — MapStruct maps by convention
- Include batch conversion methods (`entityListToApiList` / `apiListToEntityList`)

---

### Phase 3 — Integrate

Wire the new entity into the service implementation and build system.

**Step 3.1 — Update `<Entity>ServiceImpl.java`:**

If the service currently uses an in-memory stub (Stage A), upgrade to persistence (Stage B):

```java
// Before: in-memory stub
return List.of(new <Entity>(parentId, 1, "stub", 1, "content", serviceUtil.getServiceAddress()));

// After: repository-backed
return repository.findBy<ParentId>(parentId)
    .log(LOG.getName(), FINE)
    .map(mapper::entityToApi)
    .map(this::setServiceAddress);
```

Inject repository and mapper into the constructor:
```java
@Autowired
public <Entity>ServiceImpl(
    <Entity>Repository repository,
    <Entity>Mapper mapper,
    ServiceUtil serviceUtil) {
  this.repository = repository;
  this.mapper = mapper;
  this.serviceUtil = serviceUtil;
}
```

**Step 3.2 — Update `build.gradle`:**

Ensure the service's `build.gradle` includes:
```groovy
// MongoDB
implementation 'org.springframework.boot:spring-boot-starter-data-mongodb-reactive'

// OR JPA
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
runtimeOnly 'com.mysql:mysql-connector-j'

// MapStruct (if not already present)
ext { mapstructVersion = "1.6.3" }
implementation "org.mapstruct:mapstruct:${mapstructVersion}"
compileOnly "org.mapstruct:mapstruct-processor:${mapstructVersion}"
annotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"
testAnnotationProcessor "org.mapstruct:mapstruct-processor:${mapstructVersion}"

// Testcontainers
implementation platform('org.testcontainers:testcontainers-bom:1.20.4')
testImplementation 'org.testcontainers:testcontainers'
testImplementation 'org.testcontainers:junit-jupiter'
testImplementation 'org.testcontainers:mongodb'   // or :mysql
```

**Step 3.3 — Update `application.yml`:**

Add persistence config if not present:
```yaml
# MongoDB
spring.data.mongodb:
  host: localhost
  port: 27017
  database: <entity>-db

# Docker profile override
---
spring.config.activate.on-profile: docker
spring.data.mongodb.host: mongodb
```

**Step 3.4 — Update `docker-compose.yml`:**

Add `depends_on` for the database if not already present:
```yaml
  <entity>-service:
    depends_on:
      mongodb:
        condition: service_healthy
```

---

### Phase 4 — Verify

**Every entity generation MUST pass all verification steps.** Do not skip any.

#### 4.1 — Compilation Check

```bash
./gradlew :microservices:<entity>-service:compileJava
```

**Pass:** Exit code 0, no errors.  
**Fail:** Fix compilation errors before proceeding. Common issues:
- Missing import for `@Document`, `@Entity`, `@Id`
- MapStruct `@Mapping` target field doesn't exist
- Repository extends wrong base class

#### 4.2 — Pattern Diff

Compare the generated entity against existing entities to confirm structural consistency:

```bash
# List all entity annotations — should all follow the same pattern
grep -rn "@Document\|@Entity\|@RedisHash" microservices/ --include="*Entity.java"

# Verify all entities have @Version
grep -rn "@Version" microservices/ --include="*Entity.java" | wc -l
# Should equal number of Entity files (excluding Redis)

# Verify all mappers ignore id, version, serviceAddress
grep -rn "ignore = true" microservices/ --include="*Mapper.java"
```

#### 4.3 — Unit Test Generation

Generate a persistence test class for the new entity:

**MongoDB Reactive Test:**
```java
package ${basePackage}.microservices.core.<entity>;

import ${basePackage}.microservices.core.<entity>.persistence.<Entity>Entity;
import ${basePackage}.microservices.core.<entity>.persistence.<Entity>Repository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@DataMongoTest
@Testcontainers
class <Entity>EntityTest {

  @Container
  private static MongoDBContainer database =
      new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.host", database::getHost);
    registry.add("spring.data.mongodb.port", () -> database.getMappedPort(27017));
  }

  @Autowired
  private <Entity>Repository repository;

  private <Entity>Entity savedEntity;

  @BeforeEach
  void setupDb() {
    repository.deleteAll().block();
    <Entity>Entity entity = new <Entity>Entity(/* appropriate args */);
    savedEntity = repository.save(entity).block();
    assertNotNull(savedEntity);
    assertEqualsEntity(entity, savedEntity);
  }

  @Test
  void create() {
    <Entity>Entity newEntity = new <Entity>Entity(/* different args */);
    StepVerifier.create(repository.save(newEntity))
        .expectNextMatches(e -> e.getId() != null)
        .verifyComplete();
    StepVerifier.create(repository.count())
        .expectNext(2L)
        .verifyComplete();
  }

  @Test
  void update() {
    savedEntity.set<SomeField>("updated-value");
    StepVerifier.create(repository.save(savedEntity))
        .expectNextMatches(e -> e.get<SomeField>().equals("updated-value"))
        .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity))
        .verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId()))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void findBy<ParentId>() {
    StepVerifier.create(repository.findBy<ParentId>(savedEntity.get<ParentId>()))
        .expectNextCount(1)
        .verifyComplete();
  }

  @Test
  void duplicateError() {
    <Entity>Entity duplicate = new <Entity>Entity(
        savedEntity.get<ParentId>(), savedEntity.get<EntityId>() /* same keys */);
    StepVerifier.create(repository.save(duplicate))
        .expectError(org.springframework.dao.DuplicateKeyException.class)
        .verify();
  }

  @Test
  void optimisticLocking() {
    <Entity>Entity e1 = repository.findById(savedEntity.getId()).block();
    <Entity>Entity e2 = repository.findById(savedEntity.getId()).block();
    e1.set<SomeField>("first-update");
    repository.save(e1).block();
    // e2 has stale version — should fail
    e2.set<SomeField>("second-update");
    StepVerifier.create(repository.save(e2))
        .expectError(org.springframework.dao.OptimisticLockingFailureException.class)
        .verify();
  }

  private void assertEqualsEntity(<Entity>Entity expected, <Entity>Entity actual) {
    assertEquals(expected.get<ParentId>(), actual.get<ParentId>());
    assertEquals(expected.get<EntityId>(), actual.get<EntityId>());
    // ... assert domain fields ...
  }
}
```

#### 4.4 — Test Execution

```bash
# Run ONLY the entity test
./gradlew :microservices:<entity>-service:test --tests "*<Entity>EntityTest"

# Run ALL tests for the service
./gradlew :microservices:<entity>-service:test

# Run full integration suite
./test-em-all.bash
```

**Pass:** All tests green.  
**Fail:** Debug cycle:
1. Read the failure stacktrace
2. Check entity annotations match persistence strategy
3. Check mapper field mappings
4. Check `application.yml` database config
5. Fix and re-run

#### 4.5 — Build Log Verification

```bash
# Full build validation
./gradlew :microservices:<entity>-service:build

# Check for MapStruct warnings (indicates unmapped fields)
./gradlew :microservices:<entity>-service:compileJava 2>&1 | grep -i "unmapped"
```

Any MapStruct "unmapped target property" warning is a **BLOCKER** — add explicit `@Mapping(target = "...", ignore = true)` or map the field.

---

## Generated File Summary

For each entity generation, the following files are created or modified:

| File | Action | Location |
|------|--------|----------|
| `<Entity>Entity.java` | **CREATE** | `persistence/` |
| `<Entity>Repository.java` | **CREATE** | `persistence/` |
| `<Entity>Mapper.java` | **CREATE** | `services/` |
| `<Entity>ServiceImpl.java` | **MODIFY** | `services/` — inject repository + mapper |
| `<Entity>EntityTest.java` | **CREATE** | `src/test/java/.../` |
| `build.gradle` | **MODIFY** | add data starter, MapStruct, Testcontainers |
| `application.yml` | **MODIFY** | add database config |
| `docker-compose.yml` | **MODIFY** | add `depends_on` for database |

---

## Checklist

- [ ] Pre-checks passed (persistence layer active, conventions loaded, patterns scanned)
- [ ] Entity follows detected persistence strategy (MongoDB/JPA/Redis/ES)
- [ ] `@Id` and `@Version` present (except Redis `@TimeToLive` replaces `@Version`)
- [ ] Compound index covers the primary query pattern
- [ ] `createdAt` / `updatedAt` timestamps included
- [ ] Enum fields stored as `String` in entity
- [ ] Repository method follows Spring Data naming convention
- [ ] Mapper ignores `id`, `version`, `createdAt`, `updatedAt`, `serviceAddress`
- [ ] MapStruct generates no "unmapped" warnings
- [ ] ServiceImpl upgraded from in-memory stub to repository-backed (if applicable)
- [ ] `build.gradle` includes correct data starter + MapStruct + Testcontainers
- [ ] `application.yml` has database config + docker profile override
- [ ] Persistence test class created with CRUD + duplicate + optimistic-locking tests
- [ ] `./gradlew :microservices:<entity>-service:test` passes
- [ ] `./gradlew :microservices:<entity>-service:build` passes with no warnings
