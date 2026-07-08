# Verify Sub-Skill — Test-Driven Verification & Continuous Validation

This sub-skill provides **test-driven verification** workflows that run automatically after every code generation or modification. It enforces the principle that no code is considered "done" until it passes compilation, unit tests, integration tests, and pattern-diffing checks against the existing codebase.

> `${basePackage}` is a project-configurable placeholder. `<Entity>` / `<Service>` refer to the domain concept being verified.

---

## Metadata

```yaml
name: verify
description: >
  Test-driven verification and continuous validation for generated code.
  Writes failing tests first, implements fixes, and validates integration.
triggers:
  - verify
  - test
  - validate
  - check
  - integration test
  - failing test
  - test-driven
  - TDD
  - run tests
  - build check
  - quality check
```

---

## Core Philosophy

> **"Write a failing test that reproduces the integration gap, then fix it."**

Every code change follows this loop:

```
 ┌─────────────────────────────────────────────┐
 │  1. WRITE failing test (expected behavior)  │
 │  2. RUN test → confirm RED                  │
 │  3. IMPLEMENT code to make it pass          │
 │  4. RUN test → confirm GREEN                │
 │  5. DIFF against existing patterns          │
 │  6. RUN full build → confirm no regressions │
 └─────────────────────────────────────────────┘
```

---

## Pre-Checks

Before running any verification:

```bash
# Ensure Gradle wrapper is executable
chmod +x gradlew 2>/dev/null || true

# Confirm build dependencies are resolved
./gradlew dependencies --configuration compileClasspath -q | tail -5

# Check Docker is available (for Testcontainers)
docker info > /dev/null 2>&1 && echo "Docker: OK" || echo "Docker: UNAVAILABLE"
```

---

## Workflow 1 — Test-First Entity Verification

Use when a new entity, repository, or mapper has been generated.

### Step 1 — Generate the Failing Test

Write the test BEFORE confirming the implementation works. The test encodes the expected behavior:

**Persistence test template (MongoDB Reactive):**
```java
package ${basePackage}.microservices.core.<entity>;

import ${basePackage}.microservices.core.<entity>.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

@DataMongoTest
@Testcontainers
class <Entity>PersistenceTest {

  @Container
  private static MongoDBContainer database =
      new MongoDBContainer("mongo:7.0");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.host", database::getHost);
    registry.add("spring.data.mongodb.port", () -> database.getMappedPort(27017));
  }

  @Autowired private <Entity>Repository repository;

  private <Entity>Entity savedEntity;

  @BeforeEach
  void setup() {
    repository.deleteAll().block();
    savedEntity = repository.save(new <Entity>Entity(/* args */)).block();
    assertNotNull(savedEntity);
    assertNotNull(savedEntity.getId());
  }

  @Test void testCreate()       { /* save new → count == 2 */ }
  @Test void testRead()         { /* findById → matches saved */ }
  @Test void testUpdate()       { /* modify field → save → verify */ }
  @Test void testDelete()       { /* delete → existsById == false */ }
  @Test void testFindByKey()    { /* findBy<ParentId> → returns 1 */ }
  @Test void testDuplicate()    { /* same unique keys → DuplicateKeyException */ }
  @Test void testOptimisticLock() { /* stale version → OptimisticLockingFailureException */ }
}
```

**Persistence test template (JPA/MySQL):**
```java
package ${basePackage}.microservices.core.<entity>;

import ${basePackage}.microservices.core.<entity>.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class <Entity>PersistenceTest {

  @Container
  private static MySQLContainer database =
      new MySQLContainer("mysql:8.4");

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", database::getJdbcUrl);
    registry.add("spring.datasource.username", database::getUsername);
    registry.add("spring.datasource.password", database::getPassword);
  }

  @Autowired private <Entity>Repository repository;

  private <Entity>Entity savedEntity;

  @BeforeEach
  void setup() {
    repository.deleteAll();
    savedEntity = repository.save(new <Entity>Entity(/* args */));
    assertNotNull(savedEntity);
    assertTrue(savedEntity.getId() > 0);
  }

  @Test void testCreate()       { /* save → count == 2 */ }
  @Test void testRead()         { /* findById → matches */ }
  @Test void testUpdate()       { /* modify → save → verify */ }
  @Test void testDelete()       { /* delete → count == 0 */ }
  @Test void testFindByKey()    { /* findBy<ParentId> → returns 1 */ }
  @Test void testDuplicate()    { /* same unique keys → DataIntegrityViolationException */ }
  @Test void testOptimisticLock() { /* stale version → OptimisticLockingFailureException */ }
}
```

### Step 2 — Run Test → Confirm RED

```bash
./gradlew :microservices:<entity>-service:test --tests "*<Entity>PersistenceTest" 2>&1 | tail -20
```

Expected: Tests fail because the entity/repository has not been fully wired yet. This confirms the test is actually checking something.

### Step 3 — Implement

Generate or fix the entity code (see [entity-gen.md](../entity-gen/entity-gen.md)).

### Step 4 — Run Test → Confirm GREEN

```bash
./gradlew :microservices:<entity>-service:test --tests "*<Entity>PersistenceTest"
```

Expected: **ALL GREEN**. If any test fails:
1. Read the failure message and stacktrace
2. Fix the most specific failure first
3. Re-run only the failing test
4. Repeat until all pass

---

## Workflow 2 — Service Integration Verification

Use when a service implementation has been modified (new endpoint, messaging, composite wiring).

### Step 1 — Write Integration Test

```java
package ${basePackage}.microservices.core.<entity>;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class <Entity>ServiceIntegrationTest {

  @Autowired private WebTestClient client;

  @Test
  void getByParentId() {
    int parentId = 1;
    client.get()
        .uri("/<entity>?<parentId>=" + parentId)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.length()").isEqualTo(/* expected count */);
  }

  @Test
  void getNotFound() {
    client.get()
        .uri("/<entity>?<parentId>=999")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.length()").isEqualTo(0);
  }

  @Test
  void getInvalidInput() {
    client.get()
        .uri("/<entity>?<parentId>=-1")
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
        .expectBody()
        .jsonPath("$.message").isNotEmpty();
  }
}
```

### Step 2 — Run → RED → Fix → GREEN

```bash
./gradlew :microservices:<entity>-service:test --tests "*IntegrationTest"
```

---

## Workflow 3 — Event-Driven Verification

Use when `MessageProcessorConfig` or `StreamBridge` publishers are added or modified.

### Step 1 — Write Messaging Test

```java
package ${basePackage}.microservices.core.<entity>;

import ${basePackage}.api.core.<entity>.*;
import ${basePackage}.api.event.Event;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;

import static ${basePackage}.api.event.Event.Type.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestChannelBinderConfiguration.class)
class <Entity>MessagingTest {

  @Autowired private InputDestination input;

  @Test
  void createEvent() {
    <Entity> dto = new <Entity>(/* args */);
    Event<Integer, <Entity>> event = new Event<>(CREATE, dto.get<ParentId>(), dto);
    input.send(MessageBuilder.withPayload(event).build(), "<entities>");

    // Verify entity was persisted
    // GET endpoint should now return the created entity
  }

  @Test
  void deleteEvent() {
    // First create, then delete
    Event<Integer, <Entity>> deleteEvent = new Event<>(DELETE, 1, null);
    input.send(MessageBuilder.withPayload(deleteEvent).build(), "<entities>");

    // Verify entity was removed
  }

  @Test
  void invalidEventType() {
    // Verify EventProcessingException is thrown for unknown types
  }
}
```

### Step 2 — Run Messaging Tests

```bash
./gradlew :microservices:<entity>-service:test --tests "*MessagingTest"
```

---

## Workflow 4 — Full-Stack Regression

Use after ANY code change to ensure nothing is broken across the landscape.

### Step 1 — Compile All

```bash
./gradlew compileJava
```

**Pass:** Exit 0, no errors.  
**Fail:** Fix compilation errors in dependency order (api → util → core services → composite).

### Step 2 — Run All Unit Tests

```bash
./gradlew test
```

**Pass:** All green.  
**Fail:** Identify which service broke. Run that service's tests in isolation to debug.

### Step 3 — Run Full Integration Suite

```bash
# Build all JARs
./gradlew build

# Build Docker images
docker compose build

# Start landscape
docker compose up -d

# Wait for health and run integration tests
./test-em-all.bash start stop
```

**Pass:** `test-em-all.bash` exits with code 0.  
**Fail:** Check Docker Compose logs for the failing service:
```bash
docker compose logs <service-name> | tail -50
```

### Step 4 — Pattern Diff

After all tests pass, verify the new code is consistent with existing patterns:

```bash
# Count entities vs repositories vs mappers — should all match
echo "Entities:     $(find . -name '*Entity.java' -path '*/persistence/*' | wc -l)"
echo "Repositories: $(find . -name '*Repository.java' -path '*/persistence/*' | wc -l)"
echo "Mappers:      $(find . -name '*Mapper.java' -path '*/services/*' | wc -l)"

# Verify no MapStruct warnings
./gradlew compileJava 2>&1 | grep -i "unmapped" | head -10

# Verify all entities have @Version
echo "Entities with @Version: $(grep -rl '@Version' microservices/ --include='*Entity.java' | wc -l)"
echo "Total entities:         $(find . -name '*Entity.java' -path '*/persistence/*' | wc -l)"

# Verify all MessageProcessorConfig handle CREATE + DELETE
echo "CREATE handlers: $(grep -rn 'case CREATE' microservices/ --include='*.java' | wc -l)"
echo "DELETE handlers: $(grep -rn 'case DELETE' microservices/ --include='*.java' | wc -l)"
```

---

## Workflow 5 — Mapper Verification

Specifically for MapStruct mappers — the most common source of subtle bugs.

### Step 1 — Write Mapper Test

```java
package ${basePackage}.microservices.core.<entity>;

import ${basePackage}.api.core.<entity>.<Entity>;
import ${basePackage}.microservices.core.<entity>.persistence.<Entity>Entity;
import ${basePackage}.microservices.core.<entity>.services.<Entity>Mapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import static org.junit.jupiter.api.Assertions.*;

class <Entity>MapperTest {

  private <Entity>Mapper mapper = Mappers.getMapper(<Entity>Mapper.class);

  @Test
  void mapEntityToApi() {
    <Entity>Entity entity = new <Entity>Entity(/* all fields */);
    <Entity> api = mapper.entityToApi(entity);

    assertEquals(entity.get<ParentId>(), api.get<ParentId>());
    assertEquals(entity.get<EntityId>(), api.get<EntityId>());
    // ... assert all domain fields ...
    assertNull(api.getServiceAddress());  // must be ignored
  }

  @Test
  void mapApiToEntity() {
    <Entity> api = new <Entity>(/* all fields */);
    <Entity>Entity entity = mapper.apiToEntity(api);

    assertEquals(api.get<ParentId>(), entity.get<ParentId>());
    assertEquals(api.get<EntityId>(), entity.get<EntityId>());
    // ... assert all domain fields ...
    assertNull(entity.getId());       // must be ignored
    assertNull(entity.getVersion());  // must be ignored
  }

  @Test
  void mapListEntityToApi() {
    List<<Entity>Entity> entities = List.of(
        new <Entity>Entity(/* args1 */),
        new <Entity>Entity(/* args2 */)
    );
    List<<Entity>> apiList = mapper.entityListToApiList(entities);
    assertEquals(2, apiList.size());
  }
}
```

### Step 2 — Verify No Unmapped Warnings

```bash
./gradlew :microservices:<entity>-service:compileJava 2>&1 | grep -c "unmapped"
# Expected: 0
```

---

## Mandatory Post-Change Rules

These rules MUST be followed after every code generation or modification:

### Rule 1 — Always Compile After Changes
```bash
./gradlew :microservices:<entity>-service:compileJava
```
No exceptions. Compilation is the minimum bar.

### Rule 2 — Always Run Service Tests After Entity Changes
```bash
./gradlew :microservices:<entity>-service:test
```

### Rule 3 — Always Run Full Build Before Commit
```bash
./gradlew build
```

### Rule 4 — Always Run Integration Tests Before Push
```bash
./test-em-all.bash start stop
```

### Rule 5 — Always Pattern-Diff Against Existing Code
Compare counts, annotations, and structure of new files against existing ones. Any deviation must be intentional and documented.

---

## Verification Exit Criteria

A change is considered **VERIFIED** when all of the following are true:

| Criterion | Check | Command |
|-----------|-------|---------|
| Compiles | Exit 0, no errors | `./gradlew compileJava` |
| No MapStruct warnings | 0 unmapped properties | `./gradlew compileJava 2>&1 \| grep unmapped` |
| Unit tests pass | All green | `./gradlew test` |
| Integration tests pass | Exit 0 | `./test-em-all.bash` |
| Pattern-consistent | Same annotations/structure as peers | Pattern diff commands |
| Docker build works | Exit 0 | `docker compose build` |

---

## Checklist

- [ ] Failing test written BEFORE implementation (test-first)
- [ ] Test confirmed RED before code was implemented
- [ ] Test confirmed GREEN after implementation
- [ ] No MapStruct "unmapped target property" warnings
- [ ] Entity count == Repository count == Mapper count (within each service)
- [ ] `@Version` present on all entities (except Redis `@TimeToLive`)
- [ ] All `MessageProcessorConfig` handle both `CREATE` and `DELETE` types
- [ ] `./gradlew build` passes with no errors
- [ ] `./test-em-all.bash` exits with code 0
- [ ] Pattern diff shows structural consistency with existing services
- [ ] Commit message follows `iteration(<layer>): <description>` format
