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

## CI/CD & Security

### Git Hooks

Local hooks catch issues before they reach CI. Set up once after cloning:

```bash
bash .githooks/setup.sh   # configures git core.hooksPath
```

| Hook | What it does | Bypass |
|------|-------------|--------|
| `pre-commit` | Gitleaks secret scan + `mvn compile` + `mvn test` (blocks commit on failure) | `git commit --no-verify` |
| `pre-push` | `mvn compile` + `mvn test` (blocks push on failure) | `git push --no-verify` or `SKIP_PUSH_HOOKS=1 git push` |

**Prerequisites:** `gitleaks` (`brew install gitleaks` or `go install github.com/gitleaks/gitleaks/v8@latest`), Java 17 + Maven.

### Maven Security Plugins

| Plugin | Command | When it runs |
|--------|---------|-------------|
| SpotBugs + FindSecBugs | `mvn spotbugs:spotbugs` | `verify` phase (CI main branch) |

- SpotBugs with FindSecBugs detects code-level vulnerabilities (SQL injection, hardcoded creds, insecure crypto, XSS)
- Bound to `verify` phase — `mvn test` (local dev) skips it
- Reports generated in `target/spotbugsXml.xml`
- Exclude filters in `config/spotbugs-exclude.xml`

### GitHub Actions CI

Workflow: `.github/workflows/ci.yml` — triggered on push to `main` and PRs.

| Job | Runs on | What it does |
|-----|---------|-------------|
| Build & Test | PR + main | Compile + Surefire test reports |
| Gitleaks | PR + main | Full-history secret scan (`fetch-depth: 0`) |
| SpotBugs Scan | main only | Code vulnerability scan + report upload |

**Environment-based behavior:** PRs get fast feedback (build + test + Gitleaks). Main pushes get full security scans (SpotBugs).

### Dependabot

`.github/dependabot.yml` — automated dependency update PRs:
- Maven dependencies: weekly (Monday)
- GitHub Actions: monthly

## Architecture

- **Package**: `com.marakicode.financetracker`
- **Entrypoint**: `FinancetrackerApplication.java`
- **DB migrations**: Flyway SQL in `src/main/resources/db/migration/` (`V1__*.sql` through `V7__*.sql`)
- **REST Docs**: tests use `@AutoConfigureRestDocs` + `MockMvc`; AsciiDoc in `src/main/asciidoc/`
- **Test config**: `src/test/resources/application.yaml` (H2 in-memory, Flyway disabled)

## DDD-style package layout

```
com.marakicode.financetracker/
  common/              # ApiResponse, PagedResponse, BaseEntity, GlobalExceptionHandler,
                       # ErrorDto, ResourceNotFoundException, DuplicateResourceException,
                       # ValidationConstants, SecurityRules (functional interface),
                       # EmailNormalizer, SearchUtils, CurrentUserProvider
  auth/                # SecurityConfig, CorsProperties, JwtConfig, JwtService, Jwt,
                       # JwtAuthenticationFilter, UserIdPrincipal, UserDetailsServiceImpl,
                       # AuthService, AuthController, AuthSecurityRules, JwtResponse,
                       # InvalidJwtAuthenticationException, LoginRequest, RegisterRequest,
                       # AuthCurrentUserProvider
  users/               # User, Role, UserDto, UserCreateRequest, UserUpdateRequest,
                       # PasswordUpdateRequest, UserMapper, UserRepository,
                       # UserService, UserController, UserSecurityRules
  users/exceptions/    # PasswordMismatchException
  accounts/            # Account, AccountController, AccountMapper, AccountRepository,
                       # AccountSecurityRules, AccountService, AccountSpecification,
                       # AccountType (enum), AccountTypeEntity, AccountTypeRepository
  accounts/dto/        # AccountCreateRequest, AccountResponse, CurrencyUpdateRequest,
                       # UpdateAccountTypeRequest
  transactions/        # Transaction, TransactionType (enum), TransactionTypeEntity,
                       # TransactionTypeRepository, TransactionCategoryEntity,
                       # TransactionCategoryRepository, TransactionRepository,
                       # TransactionMapper, TransactionService, TransactionController,
                       # TransactionSecurityRules, InsufficientFundsException,
                       # TransactionSpecification
  transactions/dto/    # TransactionCreateRequest, TransactionUpdateRequest,
                       # TransactionResponse
  reports/               # ReportsController, ReportsService, ReportsRepository,
                         # ReportsSecurityRules
  reports/dto/           # SummaryResponse, CategoryBreakdownResponse,
                         # MonthlyBreakdownResponse, AccountBreakdownResponse
```

Each domain package is self-contained with a controller at the domain root. Only create `entity/`, `repository/`, `dto/`, `service/` sub-packages when a domain has **more than 2 files** of that type — otherwise, files sit flat at the domain root.

## Key patterns

### Security (modular SecurityRules)

- `common/SecurityRules.java` — functional interface for per-domain security config
- Each domain provides a `@Component` implementing `SecurityRules` (e.g., `UserSecurityRules`, `AuthSecurityRules`)
- `SecurityConfig` collects all `SecurityRules` beans, applies them, then adds `anyRequest().authenticated()` as final catch-all
- `PasswordEncoder` bean (BCrypt) defined in `SecurityConfig`
- CORS config externalized via `CorsProperties` (`app.cors.*` in `application.yaml`); uses `allowedOriginPatterns` for Spring 6+ credentials compatibility

### JWT Authentication

- `Jwt` — immutable value object wrapping jjwt `Claims` + `SecretKey`; provides `isExpired()`, `getUserId()`, `getRole()`, `getType()`, `isRefreshToken()`, `toString()`; tokens carry a `type` claim (`"access"` or `"refresh"`)
- `JwtConfig` — `@Configuration @Getter @Setter` class with prefix `spring.jwt`; holds `secret`, `accessTokenExpiration`, `refreshTokenExpiration`; `@ToString.Exclude` on `secret` to prevent log leakage; caches `SecretKey` with thread-safe double-checked locking and UTF-8 encoding
- `JwtService` — stateless token provider using jjwt; accepts a `User` entity, generates `Jwt` access/refresh tokens (includes email + role + type claims), parses and validates tokens (returns `Optional<Jwt>`)
- `JwtAuthenticationFilter` — `OncePerRequestFilter` that extracts `Authorization: Bearer <token>` header, validates via `JwtService`, loads `User` by ID from JWT claims, creates `ROLE_USER`/`ROLE_ADMIN` authorities from role claim, stores `UserIdPrincipal` (id + email) in `SecurityContextHolder`
- `UserIdPrincipal` — record holding user ID + email; stored in authentication principal for ownership checks in `@PreAuthorize` expressions; `toString()` returns email for backward compatibility with `AuthService.me()`
- `UserDetailsServiceImpl` — `UserDetailsService` implementation that loads user by email via `UserService` (not `UserRepository` directly); catches `ResourceNotFoundException` and wraps as `UsernameNotFoundException`
- `AuthService` — orchestrates login (delegates to `AuthenticationManager`, sets refresh token cookie), register (delegates to `UserService`, sets refresh token cookie), refresh (validates refresh token is actually a refresh token via type claim, rotates access token only), and `/me` (extracts email from `SecurityContextHolder`, returns current user)
- `AuthController` — REST endpoints at `/api/v1/auth/*`; handles `InvalidJwtAuthenticationException` and `BadCredentialsException` with 401 responses; uses `@CookieValue("refreshToken")` for refresh; controller delegates cookie-setting to service; returns `JwtResponse(accessToken)` wrapped in `ApiResponse<T>`
- `AuthSecurityRules` — permits unauthenticated access to `/api/v1/auth/login`, `/register`, `/refresh`; all other endpoints require authentication
- No refresh token table — tokens are validated cryptographically (stateless); refresh rotates only the access token, the same refresh token is reused until expiration; access tokens cannot be used as refresh tokens (type claim validated)
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
- `InsufficientFundsException` → 400 via `TransactionController` handler
- `HttpRequestMethodNotSupportedException` → 405 via `GlobalExceptionHandler`
- `HttpMessageNotReadableException` → 400 via `GlobalExceptionHandler` (enum deserialization errors return valid values in message)
- `MethodArgumentTypeMismatchException` → 400 via `GlobalExceptionHandler`
- `MissingServletRequestParameterException` → 400 via `GlobalExceptionHandler` (missing required query params like `year`)

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
- Mockito strict mode: per-stub `lenient()` on `@BeforeEach` stubs only (NOT class-level `@MockitoSettings(strictness = Strictness.LENIENT)`)

## Test inventory (333 tests, 2 skipped)

| Test class | Type | Count |
|---|---|---|
| `SecurityConfigTest` | `@SpringBootTest` integration | 12 |
| `FinancetrackerApplicationTests` | `@SpringBootTest` | 1 |
| `UserRepositoryTest` | `@DataJpaTest` | 6 |
| `UserControllerTest` | `@WebMvcTest` (mocked service) | 14 |
| `UserServiceTest` | `@ExtendWith(MockitoExtension.class)` | 17 |
| `AuthControllerTest` | `@WebMvcTest` (mocked service) | 10 |
| `AuthServiceTest` | `@ExtendWith(MockitoExtension.class)` | 10 |
| `JwtServiceTest` | `@ExtendWith(MockitoExtension.class)` | 13 |
| `JwtAuthenticationFilterTest` | `@ExtendWith(MockitoExtension.class)` | 5 |
| `UserDetailsServiceImplTest` | `@ExtendWith(MockitoExtension.class)` | 2 |
| `AccountRepositoryTest` | `@DataJpaTest` | 8 |
| `AccountControllerTest` | `@WebMvcTest` (mocked service) | 18 |
| `AccountServiceTest` | `@ExtendWith(MockitoExtension.class)` | 14 |
| `TransactionRepositoryTest` | `@DataJpaTest` | 12 |
| `TransactionServiceTest` | `@ExtendWith(MockitoExtension.class)` | 25 |
| `TransactionControllerTest` | `@WebMvcTest` (mocked service) | 19 |
| `TransactionCategoryRepositoryTest` | `@DataJpaTest` | 3 (+2 skipped) |
| `AuthCurrentUserProviderTest` | `@ExtendWith(MockitoExtension.class)` | 3 |
| `ReportsRepositoryTest` | `@DataJpaTest` | 10 |
| `ReportsServiceTest` | `@ExtendWith(MockitoExtension.class)` | 10 |
| `ReportsControllerTest` | `@WebMvcTest` (mocked service) | 11 |

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
| POST | `/api/v1/auth/logout` | authenticated | Clear refresh token cookie and security context |

## Transactions domain endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/transactions` | authenticated | Create transaction |
| GET | `/api/v1/transactions/{id}` | authenticated | Get transaction by ID |
| GET | `/api/v1/transactions` | authenticated | List transactions (paginated, filterable) |
| PATCH | `/api/v1/transactions/{id}` | authenticated | Update transaction (partial) |
| DELETE | `/api/v1/transactions/{id}` | authenticated | Delete transaction |

## Reports domain endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/reports/summary` | authenticated | Income/Expense summary for a date range |
| GET | `/api/v1/reports/by-category` | authenticated | Spending breakdown by category |
| GET | `/api/v1/reports/monthly` | authenticated | Month-by-month income vs expense |
| GET | `/api/v1/reports/by-account` | authenticated | Per-account income/expense breakdown |

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
- JwtConfig — `@Configuration @Getter @Setter` with prefix `spring.jwt`; field `secret` (was `secretKey`); caches `SecretKey` derived from secret with thread-safe double-checked locking and UTF-8 encoding; `@ToString.Exclude` on `secret` prevents log leakage
- JwtService — token generation (includes `role` + `type` claims), parsing (`Optional<Jwt>`), validation using jjwt; accepts `User` entity, returns `Jwt` objects; access and refresh tokens distinguished by `type` claim
- JwtAuthenticationFilter — extracts Bearer token, validates, loads `User` by ID from JWT claims, creates authorities from role claim, stores `UserIdPrincipal` in SecurityContext
- `UserIdPrincipal` — record holding user ID + email for ownership-based `@PreAuthorize` checks
- UserDetailsServiceImpl — loads user by email via `UserService`, catches `ResourceNotFoundException` and wraps as `UsernameNotFoundException`
- AuthService — login, register, refresh, /me orchestration; uses `UserService` (not `UserRepository`) for data access; refresh validates type claim (access tokens cannot be used as refresh tokens); refresh token cookie uses `ResponseCookie` with SameSite=Lax
- AuthController — `/api/v1/auth/*` endpoints with exception handlers
- AuthSecurityRules — permits unauthenticated access to login, register, refresh
- Security filter chain: JWT filter inserted before `UsernamePasswordAuthenticationFilter`; `@EnableMethodSecurity` on SecurityConfig
- `EmailNormalizer` utility for consistent email normalization
- `ValidationConstants` now includes `EMAIL_REGEX` and `EMAIL_MESSAGE`
- CORS config uses `allowedOriginPatterns` instead of `allowedOrigins` (Spring 6+ compatibility with credentials)
- Consistent email validation: `LoginRequest` now uses `@Pattern(EMAIL_REGEX)` (same as `RegisterRequest`)
- 36 new tests (8 controller + 7 service + 11 JWT service + 5 filter + 2 UserDetailsService + 3 integration)

### Phase 4: Accounts — DONE
- Account entity with ownership (user_id), name, type (FK to account_types), balance, currency
- AccountTypeEntity — reference/lookup table for CHECKING, SAVINGS, INVESTMENT
- Full CRUD: create, read (by ID + paginated list), update (currency), delete (returns 204)
- Ownership enforcement: all account operations filter by authenticated user
- Dynamic filtering: search by name, filter by type and currency via JPA Specifications
- MapStruct AccountMapper for entity ↔ DTO mapping
- `@EntityGraph("Account.withType")` to prevent N+1 queries
- `@PreAuthorize` on `PATCH /{id}/type` for ADMIN-only access
- `SearchUtils` for LIKE wildcard escaping (prevents LIKE injection)
- `AccountCreateRequest` name validation: 3-20 chars, alphanumeric only

### Phase 5: Transactions — DONE
- Transaction entity with ownership (account_id FK), type (INCOME/EXPENSE), amount, description, transactionDate, category
- AccountTypeEntity — reference/lookup table for CHECKING, SAVINGS, INVESTMENT
- Full CRUD: create, read (by ID + paginated list), update (partial PATCH), delete (returns 204)
- Ownership enforcement: all transaction operations filter by authenticated user via account ownership
- Balance management: INCOME adds to account balance, EXPENSE subtracts; insufficient funds throws `InsufficientFundsException`
- Dynamic filtering: filter by accountId, type, category, description search, date range via JPA Specifications
- MapStruct TransactionMapper for entity ↔ DTO mapping with explicit nested path `@Mapping` annotations
- `@NamedEntityGraph("Transaction.withAccount")` to prevent N+1 queries
- Empty PATCH body handling: all-null update fields return existing transaction unchanged (no-op)
- Account assignment immutable after creation (no accountId in update request)
- `InsufficientFundsException` handled in controller with 400 Bad Request
- `@DecimalMin("0.01")` on amount, `@PastOrPresent` on transactionDate validation
- 44 new tests (10 repo + 18 service + 16 controller)
### Phase 6: Code Review Fixes — DONE
- **Security**: JWT tokens now carry a `type` claim (`"access"`/`"refresh"`); access tokens cannot be used as refresh tokens
- **Security**: `JwtConfig.secret` excluded from `toString()` via `@ToString.Exclude` to prevent log leakage
- **Security**: `JwtConfig.getSecretKey()` uses thread-safe double-checked locking with `volatile` field
- **Security**: UTF-8 encoding for JWT secret key derivation (`StandardCharsets.UTF_8`)
- **Security**: `UserIdPrincipal` record stored in `SecurityContextHolder` for ownership-based `@PreAuthorize` checks
- **Security**: User CRUD endpoints now enforce ownership — users can only view/update/delete their own profile; admin endpoints require `ROLE_ADMIN`
- **Security**: CORS uses `allowedOriginPatterns` instead of `allowedOrigins` (Spring 6+ compatibility)
- **Architecture**: `UserDetailsServiceImpl` now uses `UserService` (not `UserRepository` directly); wraps `ResourceNotFoundException` as `UsernameNotFoundException`
- **Architecture**: Removed redundant service-level role check from `AccountService.updateAccountType()` (controller's `@PreAuthorize` is sufficient)
- **Code Quality**: `JwtService.parseToken()` returns `Optional<Jwt>` instead of nullable
- **Code Quality**: `Jwt.getType()` and `Jwt.isRefreshToken()` added for token type discrimination
- **Code Quality**: `LoginRequest` email validation uses `@Pattern(EMAIL_REGEX)` (consistent with `RegisterRequest`)
- **Code Quality**: `UserDto` now includes `role` field
- **Code Quality**: `AccountSpecification.nameContains` handles null/blank input safely
- **Code Quality**: `AccountCreateRequest` name minimum reduced from 10 to 3 characters
- **Code Quality**: Fixed fully qualified class name in `AuthService.register()`
- **Testing**: `AccountServiceTest` now properly cleans up `SecurityContextHolder` in `@AfterEach`
- **Testing**: `AccountRepositoryTest` assertion uses `getContent()` for clarity
- **Testing**: `AuthServiceTest` covers access-token-as-refresh-token rejection
- **Testing**: `FinancetrackerApplicationTests` has `@DisplayName` annotation
- **Testing**: Removed unnecessary `UserService` mock from `AuthControllerTest`
- **DB**: `AccountRepositoryTest` assertion uses `getContent()` for clarity

### Phase 7: Enum Error Handling — DONE
- `GlobalExceptionHandler.handleMalformedRequest()` now detects `InvalidFormatException` from Jackson enum deserialization failures
- Returns clear message: `"Invalid value 'X' for field 'type'. Allowed values are: CHECKING, SAVINGS, INVESTMENT"`
- Works for any enum field in any request body (not just `AccountType`)
- 4 new tests: invalid enum on `POST /accounts`, `PATCH /accounts/{id}/type`, lowercase enum, empty string enum

### Phase 8: Transaction Type & Category 3NF Refactoring — DONE
- **DB**: Flyway V6 creates `transaction_types` and `transaction_categories` reference tables, seeds INCOME/EXPENSE types, migrates existing category data, adds FK constraints on `transactions.type` → `transaction_types(name)` and `transactions.category` → `transaction_categories(name)`
- **Entity**: `Transaction.type` changed from `@Enumerated(EnumType.STRING)` to `@ManyToOne(LAZY)` → `TransactionTypeEntity`; `Transaction.category` changed from free-text `String` to `@ManyToOne(LAZY)` → `TransactionCategoryEntity`
- **Reference tables**: `TransactionTypeEntity` + `TransactionTypeRepository` (fixed: INCOME, EXPENSE); `TransactionCategoryEntity` + `TransactionCategoryRepository` (dynamic: find-or-create on first use)
- **Mapper**: `TransactionMapper` now ignores `type`/`category` in `toEntity`/`updateEntity`; adds `@Named("toEnum")` (`TransactionTypeEntity` → `TransactionType` enum) and `@Named("toCategoryName")` (`TransactionCategoryEntity` → `String`) for `toResponse`
- **Service**: `TransactionService` injects `TransactionTypeRepository` and `TransactionCategoryRepository`; `resolveType()` looks up type entity; `resolveCategory()` does find-or-create; `toTransactionType()` converts entity to enum for balance logic comparisons
- **Specifications**: `TransactionSpecification.typeEquals()` and `categoryEquals()` now join reference tables via `root.join("type")` / `root.join("category")` and compare on `name` field
- **Entity graph**: `Transaction.withAccount` now includes `type` and `category` attribute nodes to prevent N+1
- **API contract unchanged**: DTOs still accept `TransactionType` enum and `String category`; response still returns enum type and string category

### Phase 9: Code Review Fixes — DONE
- **P1 — Race condition fix**: `TransactionCategoryRepository.insertIfAbsent()` uses native `INSERT ... ON CONFLICT DO NOTHING`; `TransactionService.resolveCategory()` calls `insertIfAbsent` then `findByName` — atomic and safe under concurrency
- **P1 — FK indexes**: Flyway V7 adds `idx_transactions_type` and `idx_transactions_category` on FK columns for efficient Specification JOINs; also cleans up whitespace-only categories
- **P2 — Balance guard**: `TransactionService.updateTransaction()` only reverses/applies balance when `type` or `amount` actually changed — prevents transient incorrect balance on metadata-only updates and eliminates unnecessary `accountRepository.save()`
- **P2 — Method length**: Extracted `buildTransaction()` private method from `createTransaction()` to stay within 10-line limit
- **P2 — DRY**: Extracted `getCurrentUser()` to `common/SecurityUtils.getCurrentUser(UserService)` — shared by `TransactionService` and `AccountService`; throws `AccessDeniedException` instead of `ResourceNotFoundException` (later replaced by `CurrentUserProvider`)
- **P3 — Auth exception**: `SecurityUtils.getCurrentUser()` throws `AccessDeniedException("Not authenticated")` instead of misusing `ResourceNotFoundException` (later replaced by `CurrentUserProvider`)
- **Tests — 8 new tests**: `createTransaction_nullCategory_succeeds`, `createTransaction_invalidType_throwsIllegalArgumentException`, `updateTransaction_onlyDescriptionUpdate_skipsBalanceMutation` (service); `findAll_withCombinedSpecifications_filtersByTypeAndCategoryAndDate`, `findAll_withCombinedSpecifications_filtersByAccountIdAndType` (repo); `updateTransaction_shouldReturn400_withDecimalMinViolation`, `updateTransaction_shouldReturn400_withFutureDate`, `updateTransaction_shouldReturn200_withEmptyBody` (controller)

### Phase 10: Code Review Round 2 Fixes — DONE
- **P1 — Migration safety**: V7 now nulls out `transactions.category` before deleting whitespace-only `transaction_categories` rows — prevents FK constraint violation
- **P2 — `updateTransaction` method length**: Extracted `reconcileBalanceIfNeeded()` private method (balance comparison + reverse/check/apply) from `updateTransaction()` — method now ≤10 lines
- **P2 — Defensive coding**: `SecurityUtils.getCurrentUser()` now guards against null/blank `auth.getName()` — throws `AccessDeniedException` instead of passing null/blank to `findByEmail()` (later replaced by `CurrentUserProvider`)
- **P2 — Mock strictness**: Removed class-level `@MockitoSettings(strictness = Strictness.LENIENT)` from `TransactionServiceTest`; replaced with per-stub `lenient()` on `@BeforeEach` stubs only — Mockito strict mode now catches unused stubs at method level
- **Tests — `SecurityUtilsTest` (5 tests)**: `getCurrentUser_validAuth_returnsUser`, `getCurrentUser_nullAuth_throwsAccessDeniedException`, `getCurrentUser_blankName_throwsAccessDeniedException`, `getCurrentUser_nullName_throwsAccessDeniedException`, `getCurrentUser_realToken_returnsUser` (later replaced by `AuthCurrentUserProviderTest`)
- **Tests — `TransactionCategoryRepositoryTest` (3 tests, 2 skipped)**: `findByName_returnsCategory`, `findByName_returnsEmpty_whenNotExists`, `findByName_caseSensitive`; `insertIfAbsent` tests skipped with `@Disabled` (native `ON CONFLICT` requires PostgreSQL)
- **Tests — 4 new service tests**: `updateTransaction_bothTypeAndAmountChange_adjustsBalance`, `createTransaction_newCategory_persistsCategory`, `createTransaction_categoryCreationFails_throwsIllegalStateException`, `deleteTransaction_nullCategory_succeeds`

### Phase 11: Reports Domain — DONE
- **Read-only aggregation domain** — no new tables, no Flyway migration, no entities
- `ReportsRepository` — 4 JPQL aggregate queries (SUM, COUNT, GROUP BY, CASE, COALESCE, MONTH/YEAR date functions)
- `ReportsService` — orchestrates queries, maps `Object[]` → DTO records, computes category percentages
- `ReportsController` — 4 GET endpoints at `/api/v1/reports/*` with date range validation
- `ReportsSecurityRules` — authentication enforced via catch-all
- `SummaryResponse` — totalIncome, totalExpense, netBalance, transactionCount
- `CategoryBreakdownResponse` — category, totalAmount, percentage
- `MonthlyBreakdownResponse` — month, income, expense
- `AccountBreakdownResponse` — accountId, accountName, totalIncome, totalExpense, netAmount
- `GlobalExceptionHandler` — added `MissingServletRequestParameterException` handler (400 for missing required params)
- H2 compatibility: `ReportsRepositoryTest` uses `flattenResult()` helper for nested Object[] from H2 aggregates
- 31 new tests (10 repo + 10 service + 11 controller)
