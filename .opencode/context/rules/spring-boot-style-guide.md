# Spring Boot Style Guide — Intent & Conventions

## Guiding Philosophy

Every rule below exists to enforce one or more of: **SOLID**, **GRASP**, **CQS**, **KISS**, **YAGNI**, **DRY**, **Information Expert**, **Design by Contract**. Rules are written to reveal *why* they exist, not just *what* to do.

---

## Architecture (Intent: Separation of Concerns)

- **Layered architecture:** Controller → Service → Repository → Entity
  - *Why:* Each layer has one responsibility. Controllers handle HTTP; services coordinate domain logic; repositories persist.
- **DTOs in `dto/` subpackage per module**
  - *Why:* Decouples API contract from persistence model. Changes to DB schema don't leak to consumers.
- **Records for immutable DTOs**
  - *Why:* Immutability eliminates defensive copies, ensures thread safety, and enforces CQS (no side effects in data carriers).

## Dependency Injection

- **Constructor injection only** (never `@Autowired` on fields)
  - *Why:* Enforces DIP (dependencies are explicit), simplifies testing (no reflection), prevents NullPointerException on uninitialized fields.
- **Lombok `@RequiredArgsConstructor` for final fields, `@AllArgsConstructor` for all fields**
  - *Why:* Eliminates boilerplate while keeping constructor injection visible.

---

## REST API (Intent: Uniform Interface)

- **Base path:** `/api/v1/<domain>/...`
  - *Why:* Versioning enables backward-compatible evolution. Domain scoping prevents name collisions.
- **All responses wrapped in `ApiResponse<T>`:** `{success, message, data, timestamp}`
  - *Why:* Standard envelope allows clients to write one parser instead of per-endpoint parsing logic.
- **Paginated endpoints return `PagedResponse<T>`:** `{content, page, size, totalElements, totalPages, last}`
  - *Why:* Pagination is a cross-cutting concern. A shared DTO ensures consistent client handling.
- **Error responses:** `{status, error, message, path, timestamp, errors[]}`
  - *Why:* Machine-readable errors let clients display field-level validation without string parsing.

---

## Database (Intent: Schema Integrity)

- **PostgreSQL (runtime), H2 (testing)**
  - *Why:* PostgreSQL provides the most advanced SQL standard compliance and JSON support.
- **Flyway manages all schema changes** in `src/main/resources/db/migration/`
  - *Why:* Version-controlled, idempotent, auditable migrations. Eliminates drift between dev/staging/prod.
- **JPA `ddl-auto: validate`**
  - *Why:* Schema is owned by Flyway, not Hibernate. `validate` catches mismatches between entities and migrations at startup.

---

## Testing (Intent: FIRST + REST Docs)

- **`@SpringBootTest` with RANDOM_PORT** for integration tests
  - *Why:* Random port avoids port conflicts. Full context validates wiring.
- **`@WebMvcTest`** for controller slice tests
  - *Why:* Tests exactly one controller in isolation. Faster than full context.
- **`@DataJpaTest`** for repository slice tests
  - *Why:* Tests only JPA layer. In-memory DB, no services or controllers.
- **`@AutoConfigureRestDocs`** on controller tests
  - *Why:* Single source of truth — API docs generated from tests that prove the code works.
- **`@DisplayName` on every test method**
  - *Why:* Makes test reports human-readable. Reveals the scenario being verified.

---

## Code Style (Intent: Self-Documenting Code)

| Rule | Why |
|---|---|
| **≤10 lines per method** | Enforces SRP. A method that fits on screen is understood at a glance. |
| **No `null` returns** — use `Optional<T>` | Eliminates NPE. Type system forces caller to handle absent. |
| **No magic values** — use constants/enums | Self-documenting. Change is isolated to one definition. |
| **Records for DTOs** | Immutable, compact, equals/hashCode/toString for free. |

---

## Exception Handling (Intent: Graceful Degradation)

- **Domain exceptions** (e.g., `ResourceNotFoundException`, `BadRequestException`) in `common/exception/`
  - *Why:* Exception types map 1:1 to HTTP status, so `@ControllerAdvice` is a simple mapper with no business logic.
- **`@ControllerAdvice` with `@ExceptionHandler`**
  - *Why:* Centralized error handling. No try-catch noise in controllers.
- **Error codes in response body**
  - *Why:* Clients can handle errors programmatically without parsing message strings.

---

## Common Patterns (Intent-Revealing Examples)

*These are illustrative, not prescriptive. Adjust to your domain.*

```java
// Controller — thin: delegates all logic, returns consistent envelope
@RestController
@RequestMapping("/api/v1/{domain}")
@RequiredArgsConstructor
public class DomainController {
    private final CreateEntityCommand createCommand;
    private final GetEntityQuery getQuery;

    @PostMapping
    public ResponseEntity<ApiResponse<EntityResponse>> create(
            @Valid @RequestBody CreateEntityRequest request) {
        var entity = createCommand.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(EntityResponse.from(entity)));
    }
}

// Service (Command) — one mutation, one reason to change
@Service
@RequiredArgsConstructor
public class CreateEntityCommand {
    private final EntityRepository repository;

    public EntityResponse execute(CreateEntityRequest request) {
        var entity = Entity.builder()
            .fieldA(request.fieldA())
            .fieldB(request.fieldB())
            .build();
        return EntityResponse.from(repository.save(entity));
    }
}

// Service (Query) — reads only, no side effects
@Service
@RequiredArgsConstructor
public class GetEntityQuery {
    private final EntityRepository repository;

    public PagedResponse<EntityResponse> execute(Pageable pageable) {
        return PagedResponse.from(repository.findAll(pageable)
            .map(EntityResponse::from));
    }
}

// Entity — JPA with audit fields
@Entity
@Table(name = "entities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

// DTO — immutable record, validates on construction
public record CreateEntityRequest(
    @NotBlank String name
) {}

public record EntityResponse(
    Long id,
    String name,
    LocalDateTime createdAt
) {
    public static EntityResponse from(MyEntity entity) {
        return new EntityResponse(entity.getId(), entity.getName(), entity.getCreatedAt());
    }
}
```

---

## Key Convention Reminders

- Every method ≤10 lines — extract if longer
- Command/Query classes are **separate files** — never mix read and write in one service
- All external dependencies injected in the constructor — never `new` for services
- `@Valid` on every request DTO — validation happens before the service method is invoked
- REST Docs tests generate snippets — docs are never hand-written