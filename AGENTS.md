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
- **DB migrations**: Flyway SQL in `src/main/resources/db/migration/` (`V1__*.sql`, `V2__*.sql`, `V3__*.sql`)
- **REST Docs**: tests use `@AutoConfigureRestDocs` + `MockMvc`; AsciiDoc in `src/main/asciidoc/`
- **Test config**: `src/test/resources/application.yaml` (H2 in-memory, Flyway disabled)

## DDD-style package layout

```
com.marakicode.financetracker/
  common/              # ApiResponse, PagedResponse, BaseEntity, GlobalExceptionHandler,
                       # ErrorDto, ResourceNotFoundException, DuplicateResourceException,
                       # ValidationConstants, SecurityRules (functional interface),
                       # EmailNormalizer
  auth/                # SecurityConfig, CorsProperties, JwtConfig, JwtService, Jwt,
                       # JwtAuthenticationFilter, UserDetailsServiceImpl, AuthService,
                       # AuthController, AuthSecurityRules, JwtResponse,
                       # InvalidJwtAuthenticationException, LoginRequest,
                       # RegisterRequest
  users/               # User, Role, UserDto, UserCreateRequest, UserUpdateRequest,
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
- Each domain provides a `@Component` implementing `SecurityRules` (e.g., `UserSecurityRules`, `AuthSecurityRules`)
- `SecurityConfig` collects all `SecurityRules` beans, applies them, then adds `anyRequest().authenticated()` as final catch-all
- `PasswordEncoder` bean (BCrypt) defined in `SecurityConfig`
- CORS config externalized via `CorsProperties` (`app.cors.*` in `application.yaml`)

### JWT Authentication

- `Jwt` — immutable value object wrapping jjwt `Claims` + `SecretKey`; provides `isExpired()`, `getUserId()`, `getRole()`, `toString()`
- `JwtConfig` — `@Configuration @Data` class with prefix `spring.jwt`; holds `secret`, `accessTokenExpiration`, `refreshTokenExpiration`; caches a `SecretKey` derived from the secret
- `JwtService` — stateless token provider using jjwt; accepts a `User` entity, generates `Jwt` access/refresh tokens (includes email + role claims), parses and validates tokens (returns `Optional<Jwt>`)
- `JwtAuthenticationFilter` — `OncePerRequestFilter` that extracts `Authorization: Bearer <token>` header, validates via `JwtService`, loads `User` by ID from JWT claims, creates `ROLE_USER`/`ROLE_ADMIN` authorities from role claim, sets `SecurityContextHolder`
- `UserDetailsServiceImpl` — `UserDetailsService` implementation that loads user by email via `UserRepository.findByEmailIgnoreCase` (used by `AuthenticationManager` during login); creates authorities from user's role
- `AuthService` — orchestrates login (delegates to `AuthenticationManager`, sets refresh token cookie), register (delegates to `UserService`, sets refresh token cookie), refresh (validates refresh token, rotates access token only), and `/me` (extracts email from `SecurityContextHolder`, returns current user)
- `AuthController` — REST endpoints at `/api/v1/auth/*`; handles `InvalidJwtAuthenticationException` and `BadCredentialsException` with 401 responses; uses `@CookieValue("refreshToken")` for refresh; controller delegates cookie-setting to service; returns `JwtResponse(accessToken)` wrapped in `ApiResponse<T>`
- `AuthSecurityRules` — permits unauthenticated access to `/api/v1/auth/login`, `/register`, `/refresh`; all other endpoints require authentication
- No refresh token table — tokens are validated cryptographically (stateless); refresh rotates only the access token, the same refresh token is reused until expiration
- Refresh token cookie named `refreshToken`, set as HttpOnly with SameSite=Lax, path `/api/v1/auth`

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
- `@RequiredArgsConstructor` on services (Lombok); `@AllArgsConstructor` on controllers with single dependency (Lombok)
- `@Transactional` on write methods, `@Transactional(readOnly = true)` on read methods

## Test inventory (82 tests)

| Test class | Type | Count |
|---|---|---|
| `SecurityConfigTest` | `@SpringBootTest` integration | 9 |
| `FinancetrackerApplicationTests` | `@SpringBootTest` | 1 |
| `UserRepositoryTest` | `@DataJpaTest` | 6 |
| `UserControllerTest` | `@WebMvcTest` (mocked service) | 14 |
| `UserServiceTest` | `@ExtendWith(MockitoExtension.class)` | 17 |
| `AuthControllerTest` | `@WebMvcTest` (mocked service) | 9 |
| `AuthServiceTest` | `@ExtendWith(MockitoExtension.class)` | 8 |
| `JwtServiceTest` | `@ExtendWith(MockitoExtension.class)` | 11 |
| `JwtAuthenticationFilterTest` | `@ExtendWith(MockitoExtension.class)` | 5 |
| `UserDetailsServiceImplTest` | `@ExtendWith(MockitoExtension.class)` | 2 |

## User domain endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users` | authenticated | Create user |
| GET | `/api/v1/users` | authenticated | List users (paginated) |
| GET | `/api/v1/users/{id}` | authenticated | Get user by ID |
| PATCH | `/api/v1/users/{id}` | authenticated | Update user (partial) |
| DELETE | `/api/v1/users/{id}` | authenticated | Delete user |
| PATCH | `/api/v1/users/{id}/change-password` | authenticated | Change password |

## Auth domain endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | permitAll | Login with email + password, returns JWT tokens |
| POST | `/api/v1/auth/register` | permitAll | Register new user, returns user data and sets refresh token cookie |
| POST | `/api/v1/auth/refresh` | permitAll | Refresh access token using refresh token |
| GET | `/api/v1/auth/me` | authenticated | Get current authenticated user profile |

## Implementation status

### Phase 1: Foundation — DONE
- pom.xml dependencies, BaseEntity, ApiResponse, PagedResponse, GlobalExceptionHandler,
  ErrorDto, ResourceNotFoundException, DuplicateResourceException

### Phase 2: User Management — DONE
- User entity (firstName, lastName, email, passwordHash, role)
- Full CRUD: create, read, update (PATCH), delete (returns 204), change password
- MapStruct UserMapper, validation on all DTOs
- Email validation: custom regex requiring TLD, case-insensitive storage (lowercase)
- Flyway V2: case-insensitive unique index on email
- Flyway V3: add role column, drop redundant UNIQUE constraint on email
- Role-based access: POST `/api/v1/users` requires authentication
- 25 tests (6 repo + 12 controller + 12 service + 5 security integration)

### Phase 3: Auth/JWT — DONE
- Role enum (USER, ADMIN) with `@Enumerated(EnumType.STRING)` on User entity
- JwtConfig — `@Configuration @Data` with prefix `spring.jwt`; field `secret` (was `secretKey`); caches `SecretKey` derived from secret
- JwtService — token generation (includes `role` claim), parsing (`Optional<Jwt>`), validation using jjwt; accepts `User` entity, returns `Jwt` objects
- JwtAuthenticationFilter — extracts Bearer token, validates, loads `User` by ID from JWT claims, creates authorities from role claim, sets SecurityContext
- UserDetailsServiceImpl — loads user by email, creates authorities from user's role
- AuthService — login, register, refresh, /me orchestration; uses `UserService` (not `UserRepository`) for data access; refresh token cookie uses `ResponseCookie` with SameSite=Lax
- AuthController — `/api/v1/auth/*` endpoints with exception handlers
- AuthSecurityRules — permits unauthenticated access to login, register, refresh
- Security filter chain: JWT filter inserted before `UsernamePasswordAuthenticationFilter`; `@EnableMethodSecurity` on SecurityConfig
- `EmailNormalizer` utility for consistent email normalization
- `ValidationConstants` now includes `EMAIL_REGEX` and `EMAIL_MESSAGE`
- 36 new tests (8 controller + 7 service + 11 JWT service + 5 filter + 2 UserDetailsService + 3 integration)

### Phase 4: Accounts — NOT STARTED
### Phase 5: Transactions — NOT STARTED
### Phase 6: Polish — NOT STARTED
