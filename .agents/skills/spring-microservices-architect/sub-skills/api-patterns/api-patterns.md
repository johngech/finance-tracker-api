# API Patterns Sub-Skill — Interface-Driven Microservice Design

This sub-skill documents the **API-first, interface-driven** pattern used to build every microservice in the landscape. It is service-agnostic — the same structure applies whether you are building a Product, Review, Recommendation, Rating, or any future core service.

> `${basePackage}` is a project-configurable placeholder. `<Entity>` represents the domain concept (e.g., Product, Review, Recommendation).

## Core Concept: Interface in `api/`, Implementation in `microservices/`

The architecture cleanly separates **contract** from **implementation**:

```
api/                                         ← Shared library (no Spring Boot plugin)
  src/main/java/${basePackage}/api/
    core/<entity>/
      <Entity>.java                          ← DTO (data contract)
      <Entity>Service.java                   ← Interface (REST contract)
    composite/<domain>/
      <Domain>Aggregate.java                 ← Composite DTO
      <Domain>CompositeService.java          ← Composite interface
    event/
      Event.java                             ← Generic event envelope (messaging contract)
    exceptions/
      NotFoundException.java
      InvalidInputException.java
      EventProcessingException.java

microservices/<entity>-service/              ← Spring Boot application
  src/main/java/${basePackage}/microservices/core/<entity>/
    <Entity>ServiceApplication.java
    services/
      <Entity>ServiceImpl.java               ← REST implementation (@RestController)
      <Entity>Mapper.java                    ← MapStruct DTO ↔ Entity mapper
      MessageProcessorConfig.java            ← Cloud Stream consumer (event-driven)
    persistence/
      <Entity>Entity.java                    ← MongoDB document or JPA entity
      <Entity>Repository.java                ← Reactive or blocking repository
```

The `api/` module is a pure Java library shared by **both** the service itself and any client (e.g., the composite service). This guarantees contract consistency at compile time.

---

## 1. DTO — `<Entity>.java`

Plain POJO with no framework annotations. Serializable via Jackson.

```java
package ${basePackage}.api.core.<entity>;

public class <Entity> {
  private int <parentId>;          // foreign key (e.g., productId)
  private int <entityId>;          // natural key (e.g., recommendationId)
  private String <field1>;         // domain fields
  private int <field2>;
  private String <field3>;
  private String serviceAddress;   // runtime-only, set by service impl

  public <Entity>() {}             // default constructor for Jackson

  public <Entity>(int <parentId>, int <entityId>,
      String <field1>, int <field2>, String <field3>,
      String serviceAddress) {
    // assign all fields
  }

  // Getters and setters for all fields...
}
```

**Design rules:**
- No Spring, JPA, or MongoDB annotations — those belong on the Entity class
- `serviceAddress` is a runtime-only field set by the service implementation (ignored in persistence)
- Field naming may differ between DTO and Entity (e.g., DTO `rate` vs Entity `rating`) — MapStruct handles the mapping
- Include a no-arg constructor for Jackson deserialization

---

## 2. Service Interface — `<Entity>Service.java`

The interface defines the REST contract. It evolves through layers:

### Base-services layer (synchronous, blocking)

```java
package ${basePackage}.api.core.<entity>;

import java.util.List;
import org.springframework.web.bind.annotation.*;

public interface <Entity>Service {

  @GetMapping(value = "/<entity>", produces = "application/json")
  List<<Entity>> get<Entity>s(
      @RequestParam(value = "<parentId>", required = true) int <parentId>);
}
```

### Reactive layer (non-blocking, with create/delete for event-driven)

```java
package ${basePackage}.api.core.<entity>;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface <Entity>Service {

  Mono<<Entity>> create<Entity>(<Entity> body);

  @GetMapping(value = "/<entity>", produces = "application/json")
  Flux<<Entity>> get<Entity>s(
      @RequestParam(value = "<parentId>", required = true) int <parentId>);

  Mono<Void> delete<Entity>s(int <parentId>);
}
```

**Key design decisions:**
- `@GetMapping` stays on the read method — it is called via REST by the gateway/composite
- `create<Entity>` and `delete<Entity>s` have **no** `@PostMapping` / `@DeleteMapping` — they are invoked via **messaging** (Cloud Stream), not HTTP
- Return types are `Mono`/`Flux` (Project Reactor) for non-blocking I/O
- The same interface is implemented on both the core service side (persister) and the composite side (publisher)

---

## 3. Generic Event Envelope — `Event.java`

A single generic class wraps all create/delete events across **all** services. Written once, reused everywhere.

```java
package ${basePackage}.api.event;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;
import java.time.ZonedDateTime;
import static java.time.ZonedDateTime.now;

public class Event<K, T> {

  public enum Type { CREATE, DELETE }

  private final Type eventType;
  private final K key;
  private final T data;
  private final ZonedDateTime eventCreatedAt;

  public Event() {                       // Jackson deserialization
    this.eventType = null;
    this.key = null;
    this.data = null;
    this.eventCreatedAt = null;
  }

  public Event(Type eventType, K key, T data) {
    this.eventType = eventType;
    this.key = key;
    this.data = data;
    this.eventCreatedAt = now();
  }

  public Type getEventType() { return eventType; }
  public K getKey() { return key; }
  public T getData() { return data; }

  @JsonSerialize(using = ZonedDateTimeSerializer.class)
  public ZonedDateTime getEventCreatedAt() { return eventCreatedAt; }
}
```

**Usage pattern:**
- `Event<Integer, <Entity>>` — key is the parent/natural ID, data is the DTO
- `Event.Type.CREATE` carries the full DTO in `data`
- `Event.Type.DELETE` carries the key, `data` is `null`
- The class is fully generic — adding a new service requires **no changes** to `Event.java`

---

## 4. Implementation Evolution

Every service impl follows the same two-stage evolution:

### Stage A: In-Memory Stub (base-services layer)

```java
package ${basePackage}.microservices.core.<entity>.services;

@RestController
public class <Entity>ServiceImpl implements <Entity>Service {

  private final ServiceUtil serviceUtil;

  @Autowired
  public <Entity>ServiceImpl(ServiceUtil serviceUtil) {
    this.serviceUtil = serviceUtil;
  }

  @Override
  public List<<Entity>> get<Entity>s(int <parentId>) {
    if (<parentId> < 1) {
      throw new InvalidInputException("Invalid <parentId>: " + <parentId>);
    }
    // Return hardcoded list for integration testing
    List<<Entity>> list = new ArrayList<>();
    list.add(new <Entity>(<parentId>, 1, "Author 1", 1, "Content 1",
        serviceUtil.getServiceAddress()));
    // ... more stubs
    return list;
  }
}
```

**Purpose:** Prove end-to-end wiring works before adding persistence or messaging complexity.

### Stage B: Persistence + Reactive (persistence & reactive layers)

```java
package ${basePackage}.microservices.core.<entity>.services;

@RestController
public class <Entity>ServiceImpl implements <Entity>Service {

  private static final Logger LOG = LoggerFactory.getLogger(<Entity>ServiceImpl.class);
  private final <Entity>Repository repository;
  private final <Entity>Mapper mapper;
  private final ServiceUtil serviceUtil;

  @Autowired
  public <Entity>ServiceImpl(
      <Entity>Repository repository,
      <Entity>Mapper mapper,
      ServiceUtil serviceUtil) {
    this.repository = repository;
    this.mapper = mapper;
    this.serviceUtil = serviceUtil;
  }

  @Override
  public Mono<<Entity>> create<Entity>(<Entity> body) {
    if (body.get<ParentId>() < 1) {
      throw new InvalidInputException("Invalid <parentId>: " + body.get<ParentId>());
    }
    <Entity>Entity entity = mapper.apiToEntity(body);
    return repository.save(entity)
        .log(LOG.getName(), FINE)
        .onErrorMap(DuplicateKeyException.class,
            ex -> new InvalidInputException("Duplicate key, <ParentId>: "
                + body.get<ParentId>() + ", <EntityId>: " + body.get<EntityId>()))
        .map(mapper::entityToApi);
  }

  @Override
  public Flux<<Entity>> get<Entity>s(int <parentId>) {
    if (<parentId> < 1) {
      throw new InvalidInputException("Invalid <parentId>: " + <parentId>);
    }
    return repository.findBy<ParentId>(<parentId>)
        .log(LOG.getName(), FINE)
        .map(mapper::entityToApi)
        .map(this::setServiceAddress);
  }

  @Override
  public Mono<Void> delete<Entity>s(int <parentId>) {
    if (<parentId> < 1) {
      throw new InvalidInputException("Invalid <parentId>: " + <parentId>);
    }
    return repository.deleteAll(repository.findBy<ParentId>(<parentId>));
  }

  private <Entity> setServiceAddress(<Entity> e) {
    e.setServiceAddress(serviceUtil.getServiceAddress());
    return e;
  }
}
```

**Pattern notes:**
- All input validation throws `InvalidInputException` (maps to HTTP 422)
- `DuplicateKeyException` from the data layer is mapped to `InvalidInputException`
- `setServiceAddress` decorates the response with the host/port of the answering instance (useful for debugging load balancing)

---

## 5. Persistence Layer

### Entity — `<Entity>Entity.java`

Choose **one** persistence strategy per service:

**MongoDB (reactive):**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "<entities>")
@CompoundIndex(name = "<parent>-<entity>-id", unique = true,
    def = "{'<parentId>': 1, '<entityId>' : 1}")
public class <Entity>Entity {
  @Id private String id;
  @Version private Integer version;
  private int <parentId>;
  private int <entityId>;
  // domain fields...

  public <Entity>Entity() {}
  // All-args constructor, getters, setters...
}
```

**JPA/MySQL (blocking):**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "<entities>",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"<parentId>", "<entityId>"}))
public class <Entity>Entity {
  @Id @GeneratedValue private int id;
  @Version private int version;
  private int <parentId>;
  private int <entityId>;
  // domain fields...
}
```

### Repository — `<Entity>Repository.java`

**Reactive (MongoDB):**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface <Entity>Repository
    extends ReactiveCrudRepository<<Entity>Entity, String> {
  Flux<<Entity>Entity> findBy<ParentId>(int <parentId>);
}
```

**Blocking (JPA):**
```java
package ${basePackage}.microservices.core.<entity>.persistence;

import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface <Entity>Repository
    extends CrudRepository<<Entity>Entity, Integer> {
  List<<Entity>Entity> findBy<ParentId>(int <parentId>);
}
```

### Mapper — `<Entity>Mapper.java` (MapStruct)

```java
package ${basePackage}.microservices.core.<entity>.services;

import org.mapstruct.*;
import ${basePackage}.api.core.<entity>.<Entity>;
import ${basePackage}.microservices.core.<entity>.persistence.<Entity>Entity;
import java.util.List;

@Mapper(componentModel = "spring")
public interface <Entity>Mapper {

  @Mappings({
    @Mapping(target = "<dtoField>", source = "entity.<entityField>"),  // when names differ
    @Mapping(target = "serviceAddress", ignore = true)
  })
  <Entity> entityToApi(<Entity>Entity entity);

  @Mappings({
    @Mapping(target = "<entityField>", source = "api.<dtoField>"),     // reverse mapping
    @Mapping(target = "id", ignore = true),
    @Mapping(target = "version", ignore = true)
  })
  <Entity>Entity apiToEntity(<Entity> api);

  List<<Entity>> entityListToApiList(List<<Entity>Entity> entity);
  List<<Entity>Entity> apiListToEntityList(List<<Entity>> api);
}
```

**Key pattern:** The mapper bridges naming gaps between DTO fields and entity fields, ignores persistence-only fields (`id`, `version`), and ignores runtime-only fields (`serviceAddress`). If all field names match, the `@Mappings` block can be omitted — MapStruct maps by convention.

---

## 6. Event-Driven Messaging — `MessageProcessorConfig.java`

The **consumer side** — each core service has one `MessageProcessorConfig` that processes incoming events from Cloud Stream.

```java
package ${basePackage}.microservices.core.<entity>.services;

import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ${basePackage}.api.core.<entity>.<Entity>;
import ${basePackage}.api.core.<entity>.<Entity>Service;
import ${basePackage}.api.event.Event;
import ${basePackage}.api.exceptions.EventProcessingException;

@Configuration
public class MessageProcessorConfig {

  private static final Logger LOG = LoggerFactory.getLogger(MessageProcessorConfig.class);
  private final <Entity>Service service;

  @Autowired
  public MessageProcessorConfig(<Entity>Service service) {
    this.service = service;
  }

  @Bean
  public Consumer<Event<Integer, <Entity>>> messageProcessor() {
    return event -> {
      LOG.info("Process message created at {}...", event.getEventCreatedAt());

      switch (event.getEventType()) {
        case CREATE:
          <Entity> dto = event.getData();
          LOG.info("Create <entity> with ID: {}/{}", dto.get<ParentId>(), dto.get<EntityId>());
          service.create<Entity>(dto).block();
          break;

        case DELETE:
          int parentId = event.getKey();
          LOG.info("Delete <entity>s with <ParentId>: {}", parentId);
          service.delete<Entity>s(parentId).block();
          break;

        default:
          String errorMessage = "Incorrect event type: " + event.getEventType()
              + ", expected a CREATE or DELETE event";
          LOG.warn(errorMessage);
          throw new EventProcessingException(errorMessage);
      }

      LOG.info("Message processing done!");
    };
  }
}
```

**Key patterns:**
- Uses Spring Cloud Function's `Consumer<T>` functional bean (not the deprecated `@StreamListener`)
- Delegates to the same `<Entity>Service` interface, calling `.block()` since the message listener thread is inherently synchronous
- The bean name `messageProcessor` maps to the Cloud Stream binding `messageProcessor-in-0`
- Every `Event.Type` case must be handled; unknown types throw `EventProcessingException`

### Cloud Stream binding configuration (`application.yml`)

```yaml
spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings:
    messageProcessor-in-0:
      destination: <entities>              # topic/queue name
      group: <entities>Group               # consumer group

spring.cloud.stream.bindings.messageProcessor-in-0.consumer:
  maxAttempts: 3
  backOffInitialInterval: 500
  backOffMaxInterval: 1000
  backOffMultiplier: 2.0

spring.cloud.stream.rabbit.bindings.messageProcessor-in-0.consumer:
  autoBindDlq: true
  republishToDlq: true

spring.cloud.stream.kafka.bindings.messageProcessor-in-0.consumer:
  enableDlq: true
```

---

## 7. Composite Side — Publisher via `StreamBridge`

The composite integration class **implements** the same `<Entity>Service` interface but publishes events instead of persisting.

```java
package ${basePackage}.microservices.composite.<domain>.services;

@Component
public class <Domain>Integration
    implements ProductService, <Entity>Service /*, ... other services */ {

  private final Scheduler publishEventScheduler;
  private final WebClient webClient;
  private final ObjectMapper mapper;
  private final StreamBridge streamBridge;

  @Autowired
  public <Domain>Integration(
      @Qualifier("publishEventScheduler") Scheduler publishEventScheduler,
      WebClient.Builder webClientBuilder,
      ObjectMapper mapper,
      StreamBridge streamBridge) {
    this.webClient = webClientBuilder.build();
    this.publishEventScheduler = publishEventScheduler;
    this.mapper = mapper;
    this.streamBridge = streamBridge;
  }

  // --- READ via REST ---
  @Override
  public Flux<<Entity>> get<Entity>s(int <parentId>) {
    String url = <ENTITY>_SERVICE_URL + "/<entity>?<parentId>=" + <parentId>;
    return webClient.get().uri(url).retrieve()
        .bodyToFlux(<Entity>.class)
        .log(LOG.getName(), FINE)
        .onErrorResume(error -> empty());   // partial response on failure
  }

  // --- WRITE via Events ---
  @Override
  public Mono<<Entity>> create<Entity>(<Entity> body) {
    return Mono.fromCallable(() -> {
      sendMessage("<entities>-out-0",
          new Event(CREATE, body.get<ParentId>(), body));
      return body;
    }).subscribeOn(publishEventScheduler);
  }

  @Override
  public Mono<Void> delete<Entity>s(int <parentId>) {
    return Mono.fromRunnable(() ->
        sendMessage("<entities>-out-0",
            new Event(DELETE, <parentId>, null)))
        .subscribeOn(publishEventScheduler).then();
  }

  // --- Shared send helper ---
  private void sendMessage(String bindingName, Event event) {
    LOG.debug("Sending a {} message to {}", event.getEventType(), bindingName);
    Message message = MessageBuilder.withPayload(event)
        .setHeader("partitionKey", event.getKey())
        .build();
    streamBridge.send(bindingName, message);
  }
}
```

### Composite Cloud Stream binding (`application.yml`)

```yaml
spring.cloud.stream:
  defaultBinder: rabbit
  default.contentType: application/json
  bindings:
    <entities>-out-0:
      destination: <entities>
      producer:
        required-groups: <entities>Group
```

---

## 8. Communication Topology

```
┌──────────────────────────────────────────────────────────────────┐
│                      composite-service                           │
│                                                                  │
│  <Domain>Integration implements <Entity>Service                  │
│  ├── get<Entity>s()         ── REST (WebClient) ──────────►     │
│  ├── create<Entity>()       ── Event (StreamBridge) ──────►     │
│  └── delete<Entity>s()      ── Event (StreamBridge) ──────►     │
└──────────────────────────┬──────────────────────────┬────────────┘
                           │ REST GET                 │ Events
                           ▼                          ▼
                  ┌──────────────────┐     ┌──────────────────────┐
                  │  <entity>-service │     │  RabbitMQ / Kafka    │
                  │                  │◄────│  "<entities>" topic   │
                  │  @RestController │     └──────────────────────┘
                  │  <Entity>ServiceImpl                           │
                  │  ├── get<Entity>s()      ← HTTP GET            │
                  │  ├── create<Entity>()    ← MessageProcessorConfig
                  │  └── delete<Entity>s()   ← MessageProcessorConfig
                  │                  │                              │
                  │  Database (MongoDB reactive / MySQL JPA)        │
                  └────────────────────────────────────────────────┘
```

**Key insight:** Reads are synchronous REST (low latency), writes are asynchronous events (decoupling and resilience). Both sides share the **same** `<Entity>Service` interface — the composite implements it as a publisher, the core service implements it as a consumer + persister.

---

## 9. Applying This Pattern to a New Service

To add any new core service (replace `<Entity>` with your domain concept):

### Step 1 — `api/` module
- Create DTO: `${basePackage}.api.core.<entity>.<Entity>`
- Create interface: `${basePackage}.api.core.<entity>.<Entity>Service`
- Add methods: `Mono<<Entity>> create<Entity>(<Entity> body)`, `Flux<<Entity>> get<Entity>s(...)`, `Mono<Void> delete<Entity>s(...)`
- The generic `Event<K, T>` class requires **no changes**

### Step 2 — `microservices/<entity>-service/` module
- `<Entity>Entity.java` — persistence document/entity
- `<Entity>Repository.java` — reactive or blocking repository
- `<Entity>Mapper.java` — MapStruct mapper (handle any field name differences)
- `<Entity>ServiceImpl.java` — `@RestController implements <Entity>Service`
- `MessageProcessorConfig.java` — `Consumer<Event<Integer, <Entity>>> messageProcessor()`

### Step 3 — `microservices/<domain>-composite-service/`
- Add `<Entity>Service` to the `implements` list on the integration class
- REST GET via `WebClient`, create/delete via `StreamBridge` to `<entities>-out-0`

### Step 4 — Configuration & wiring
- Add Cloud Stream bindings: `<entities>-out-0` (composite) and `messageProcessor-in-0` → `<entities>` topic (service)
- Add service entry to `docker-compose.yml`
- Add module to `settings.gradle`
- Add test cases to `test-em-all.bash`

---

## Checklist

- [ ] DTO in `api/` has no framework annotations (pure POJO)
- [ ] Interface in `api/` uses `@GetMapping` only for reads; no HTTP mapping on write methods
- [ ] Return types are `Mono<T>` / `Flux<T>` at the reactive layer
- [ ] Entity uses `@Document` (MongoDB) **or** `@Entity` (JPA), never mixed
- [ ] MapStruct mapper handles any field name differences between DTO and Entity
- [ ] MapStruct mapper ignores `id`, `version` (persistence-only) and `serviceAddress` (runtime-only)
- [ ] `MessageProcessorConfig` uses `Consumer<Event<K, T>>` functional bean
- [ ] All `Event.Type` cases are handled; unknown types throw `EventProcessingException`
- [ ] Composite uses `StreamBridge.send()` for writes, `WebClient` for reads
- [ ] `partitionKey` header is set on outgoing messages for ordered processing
- [ ] Dead Letter Queue (DLQ) is configured for consumer error handling
- [ ] Same interface implemented on both sides (compile-time contract enforcement)
- [ ] `docker-compose.yml`, `settings.gradle`, and `test-em-all.bash` are updated
