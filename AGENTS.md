# financetracker

Spring Boot 3.5.0 / Java 17 / Maven monolith. PostgreSQL + Flyway + JPA + Lombok + MapStruct + Spring Security 6.5.0 + REST Docs.

## Quick start

```bash
source ~/.sdkman/bin/sdkman-init.sh && sdk env   # activate java=17.0.8-tem
mvn compile                                       # compile + Lombok/MapStruct annotation processing
mvn test                                          # JUnit 5, single test: mvn test -Dtest=ClassName
mvn package                                       # triggers asciidoctor doc generation
mvn spring-boot:run                               # requires a running PostgreSQL (configure datasource in application.yaml)
```

**Note**: Use `mvn`, not `./mvnw`. The wrapper is broken. Always run `source ~/.sdkman/bin/sdkman-init.sh && sdk env` first.

## Tech stack

- Spring Boot 3.5.0, Java 17, Maven
- PostgreSQL (prod) / H2 (tests)
- Flyway (migrations in `src/main/resources/db/migration/`)
- Spring Security 6.5.0 (stateless, CSRF disabled, BCrypt)
- MapStruct 1.6.3 (componentModel = "spring")
- Lombok (@RequiredArgsConstructor for injection, @Getter/@Setter on entities — NO @Data on entities)
- Records for DTOs, `ApiResponse<T>` / `PagedResponse<T>` wrappers
- JPA with `@Id @GeneratedValue` on each entity (NOT on BaseEntity)
- `@PrePersist` / `@PreUpdate` on BaseEntity for audit timestamps

## Architecture

- **Package**: `com.marakicode.financetracker`
- **Entrypoint**: `FinancetrackerApplication.java`
- **DB migrations**: Flyway SQL in `src/main/resources/db/migration/` (`V1__*.sql`)
- **REST Docs**: tests use `@AutoConfigureRestDocs` + `MockMvc`; AsciiDoc in `src/main/asciidoc/`
- **Test config**: `src/test/resources/application.yaml` (H2 in-memory, Flyway disabled)

## DDD-style package layout

```
com.marakicode.financetracker/
  common/              # ApiResponse, PagedResponse, BaseEntity, GlobalExceptionHandler,
                       # ErrorDto, ResourceNotFoundException, DuplicateResourceException,
                       # ValidationConstants, SecurityRules (functional interface)
  auth/                # SecurityConfig, CorsProperties (PasswordEncoder, SecurityFilterChain, CORS)
  users/               # User, UserDto, UserCreateRequest, UserUpdateRequest,
                       # PasswordUpdateRequest, UserMapper, UserRepository,
                       # UserService, UserController, UserSecurityRules
  users/exceptions/    # PasswordMismatchException
  accounts/            # (planned)
  transactions/        # (planned)
```

Each domain package is self-contained with a controller at the domain root. Only create `entity/`, `repository/`, `dto/`, `service/` sub-packages when a domain has **more than 2 files** of that type — otherwise, files sit flat at the domain root.

## Key patterns

### Security (modular SecurityRules)

- `common/SecurityRules.java` — functional interface for per-domain security config
- Each domain provides a `@Component` implementing `SecurityRules` (e.g., `UserSecurityRules`)
- `SecurityConfig` collects all `SecurityRules` beans, applies them, then adds `anyRequest().authenticated()` as final catch-all
- `PasswordEncoder` bean (BCrypt) defined in `SecurityConfig`
- CORS config externalized via `CorsProperties` (`app.cors.*` in `application.yaml`)

### API responses

- Success: `ApiResponse.success("message", data)` → `{ success: true, message: "...", data: {...} }`
- Error: `ErrorDto.of(status, error, message, path)` → `{ timestamp, status, error, message, path, fieldErrors }`
- Pagination: `PagedResponse<T>(content, page, size, count, totalPages)`

### Exceptions

- `ResourceNotFoundException` → 404 via `GlobalExceptionHandler`
- `DuplicateResourceException` → 409 via `GlobalExceptionHandler`
- `MethodArgumentNotValidException` → 400 with field errors
- `PasswordMismatchException` → 400 via `UserController` handler
- `HttpRequestMethodNotSupportedException` → 405 via `GlobalExceptionHandler`
- `HttpMessageNotReadableException` → 400 via `GlobalExceptionHandler`
- `MethodArgumentTypeMismatchException` → 400 via `GlobalExceptionHandler`

## Conventions

- Use `mvn` (not `./mvnw`)
- **DDD-style package layout** — organized by bounded context
- Tests: `@SpringBootTest` for integration, `@DataJpaTest` for repos, `@WebMvcTest` for controllers
- `@AutoConfigureMockMvc(addFilters = false)` to skip security filters in controller tests
- `@MockitoBean` (Spring Boot 3.4+), NOT `@MockBean`
- Flyway: one migration per change, never edit an applied migration
- Records for DTOs with Jakarta Validation annotations
- MapStruct for entity ↔ DTO mapping (avoid manual `from()` factories)
- `@RequiredArgsConstructor` on services and controllers (Lombok)
- `@Transactional` on write methods, `@Transactional(readOnly = true)` on read methods

## Test inventory (39 tests)

| Test class | Type | Count |
|---|---|---|
| `SecurityConfigTest` | `@SpringBootTest` integration | 6 |
| `FinancetrackerApplicationTests` | `@SpringBootTest` | 1 |
| `UserRepositoryTest` | `@DataJpaTest` | 6 |
| `UserControllerTest` | `@WebMvcTest` (mocked service) | 14 |
| `UserServiceTest` | `@ExtendWith(MockitoExtension.class)` | 12 |

## User domain endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users` | permitAll | Create user |
| GET | `/api/v1/users` | authenticated | List users (paginated) |
| GET | `/api/v1/users/{id}` | authenticated | Get user by ID |
| PATCH | `/api/v1/users/{id}` | authenticated | Update user (partial) |
| DELETE | `/api/v1/users/{id}` | authenticated | Delete user |
| PATCH | `/api/v1/users/{id}/change-password` | authenticated | Change password |

## Implementation status

### Phase 1: Foundation — DONE
- pom.xml dependencies, BaseEntity, ApiResponse, PagedResponse, GlobalExceptionHandler,
  ErrorDto, ResourceNotFoundException, DuplicateResourceException

### Phase 2: User Management — DONE
- User entity (firstName, lastName, email, passwordHash)
- Full CRUD: create, read, update (PATCH), delete, change password
- MapStruct UserMapper, validation on all DTOs
- Email validation: custom regex requiring TLD, case-insensitive storage (lowercase)
- Flyway V2: case-insensitive unique index on email
- 25 tests (6 repo + 12 controller + 12 service + 5 security integration)

### Phase 3: Auth/JWT — IN PROGRESS
- SecurityConfig with modular SecurityRules, CORS, BCrypt PasswordEncoder
- Security rules: POST /api/v1/users permitAll, all else authenticated
- Security integration tests (6 tests)
- **TODO**: JWT token provider, JwtAuthenticationFilter, UserDetailsService

### Phase 4: Accounts — NOT STARTED
### Phase 5: Transactions — NOT STARTED
### Phase 6: Polish — NOT STARTED
