# Finance Tracker — Implementation Plan

## Context Summary

- **Project:** `com.marakicode.financetracker` — Spring Boot 3.5.0 / Java 17 / Maven monolith
- **Architecture:** PostgreSQL + Flyway + JPA + Lombok + MapStruct + Spring Security 6.5.0 + REST Docs, DDD-style package layout
- **Current State:** Foundation, User Management, Auth/JWT, and Accounts are complete. Code review fixes and enum error handling are done. JWT-based stateless authentication with login, register, refresh, and profile endpoints is fully implemented. Ownership-based authorization via `UserIdPrincipal` and `@PreAuthorize` is enforced on all user and account endpoints. 128 tests passing.
- **Target State:** Fully functional finance tracker with JWT auth, CRUD for users/accounts/transactions, financial reporting, pagination, validation, and REST documentation.

---

## Requirements

| ID | Requirement | Domain | Priority | Status |
|----|------------|--------|----------|--------|
| R1 | Shared foundation (base entities, API wrappers, exception handling) | `common/` | High | ✅ Done |
| R2 | User management CRUD with validation and email normalization | `users/` | High | ✅ Done |
| R3 | Security skeleton with modular per-domain rules | `auth/` | High | ✅ Done |
| R4 | JWT authentication (login, register, refresh tokens) | `auth/` | High | ✅ Done |
| R5 | Account management CRUD (bank/investment accounts) | `accounts/` | High | ✅ Done |
| R6 | Transaction management CRUD (financial transactions linked to accounts) | `transactions/` | High | ⬜ Not started |
| R7 | Financial reporting (summaries by category and time period) | `transactions/` | Medium | ⬜ Not started |
| R8 | Data validation (positive amounts, transaction types) | Service Layer | Medium | ⬜ Not started |
| R9 | Pagination for list endpoints | Controller + Service | Medium | ✅ Done |
| R10 | Standard error handling with HTTP status codes | `common/` | Medium | ✅ Done |
| R11 | Audit tracking (createdAt, updatedAt timestamps) | Entity Layer | Medium | ✅ Done |
| R12 | REST documentation (AsciiDoc snippets) | Tests + docs | Medium | ⬜ Not started |
| R13 | Ownership-based authorization on all domain endpoints | Security | High | ✅ Done |
| R14 | JWT token type discrimination (access vs refresh) | `auth/` | High | ✅ Done |
| R15 | Descriptive enum error messages | `common/` | Medium | ✅ Done |

---

## Architecture Decisions

### DDD-Style Package Layout

Each domain is a self-contained bounded context with its own controller, service, repository, entity, DTOs, and exceptions. Sub-packages (`entity/`, `repository/`, `dto/`, `service/`) are only created when a domain has **more than 2 files** of that type — otherwise files sit flat at the domain root.

```
com.marakicode.financetracker/
  common/              # Shared foundation: base entities, API wrappers, exception handling,
                       # constants, SearchUtils, EmailNormalizer
  auth/                # Security config, JWT auth (JwtConfig, JwtService, Jwt,
                       # JwtAuthenticationFilter, UserIdPrincipal, AuthController,
                       # AuthService, AuthSecurityRules, UserDetailsServiceImpl)
  users/               # User domain: entity, CRUD, validation, email normalization
  users/exceptions/    # PasswordMismatchException
  accounts/            # Account domain: entity, CRUD, ownership enforcement, filtering
  accounts/dto/        # AccountCreateRequest, AccountResponse, CurrencyUpdateRequest,
                       # UpdateAccountTypeRequest
  transactions/        # (planned) Transaction domain
```

### Key Design Patterns

| Pattern | Description |
|---------|-------------|
| **Modular SecurityRules** | Each domain provides a `SecurityRules` bean. `SecurityConfig` aggregates all beans and applies them, then adds `anyRequest().authenticated()` as a catch-all. |
| **JWT Authentication** | Stateless auth via `JwtService` (jjwt). `Jwt` is an immutable value object wrapping `Claims` + `SecretKey`, providing `isExpired()`, `getUserId()`, `getRole()`, `getType()`, `isRefreshToken()`. Tokens carry a `type` claim (`"access"` or `"refresh"`). `JwtAuthenticationFilter` extracts Bearer tokens, validates, loads `User` by ID from JWT claims, stores `UserIdPrincipal` (id + email) in `SecurityContextHolder`. `JwtConfig` is a `@Configuration @Getter @Setter` class with thread-safe `SecretKey` caching (double-checked locking) and UTF-8 encoding. |
| **Ownership-Based Authorization** | `UserIdPrincipal` record stored in `SecurityContextHolder` for ownership checks via `@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")`. |
| **ApiResponse / PagedResponse** | All endpoints return `ApiResponse<T>` for success and `ErrorDto` for errors. List endpoints use `PagedResponse<T>` for pagination. |
| **GlobalExceptionHandler** | Centralized `@RestControllerAdvice` handles validation errors, resource not found, duplicates, method not allowed, malformed requests (with descriptive enum error messages), type mismatches, and unexpected exceptions. Domain-specific handlers live in their respective controllers. |
| **MapStruct + Lombok** | Entities use `@Getter/@Setter` (never `@Data`). DTOs are records. MapStruct mappers use `componentModel = "spring"` for injection. Services use `@RequiredArgsConstructor`, controllers use `@AllArgsConstructor` for constructor injection. |
| **Method-Level @Transactional** | Write methods get `@Transactional`, read methods get `@Transactional(readOnly = true)`. No class-level annotations. |
| **Flyway Migrations** | One migration per schema change, never edit an applied migration. Migrations are versioned sequentially (V1, V2, V3...). |
| **Case-Insensitive Email** | Emails normalized to lowercase before persistence. Database enforces uniqueness via a `LOWER()` function index. |
| **JPA Specifications** | Dynamic filtering via `Specification` for search and filter parameters on list endpoints. `SearchUtils` escapes LIKE wildcards to prevent injection. |

### API Response Convention

- **Success:** `{ "success": true, "message": "...", "data": {...} }`
- **Error:** `{ "timestamp": "...", "status": 400, "error": "...", "message": "...", "path": "...", "fieldErrors": [...] }`
- **Pagination:** `{ "content": [...], "page": 0, "size": 10, "count": 1, "totalPages": 1 }`

### Exception Handling Convention

| Exception | HTTP Status | Handler Location |
|-----------|-------------|-----------------|
| `MethodArgumentNotValidException` | 400 | `GlobalExceptionHandler` |
| `HttpMessageNotReadableException` | 400 | `GlobalExceptionHandler` (enum errors return valid values in message) |
| `MethodArgumentTypeMismatchException` | 400 | `GlobalExceptionHandler` |
| `ResourceNotFoundException` | 404 | `GlobalExceptionHandler` |
| `DuplicateResourceException` | 409 | `GlobalExceptionHandler` |
| `HttpRequestMethodNotSupportedException` | 405 | `GlobalExceptionHandler` |
| `AccessDeniedException` | 403 | `GlobalExceptionHandler` |
| `InvalidJwtAuthenticationException` | 401 | `AuthController` |
| `BadCredentialsException` | 401 | `AuthController` |
| Domain-specific exceptions (e.g., password mismatch) | varies | Respective domain controller |

---

## Phase Breakdown

### Phase 1: Foundation — ✅ DONE

**Goal:** Build the shared infrastructure that all domains depend on.

**What was delivered:**
- Build configuration with all required dependencies (Web, Security, Validation, JPA, Flyway, Lombok, MapStruct, Test)
- Base entity superclass providing audit timestamps (`createdAt`, `updatedAt`) via `@PrePersist`/`@PreUpdate`
- Standardized API response wrappers: single-resource and paginated
- Centralized exception handling with `ErrorDto` for consistent error responses
- Custom exceptions for common business rules (resource not found, duplicate resource)
- Validation constants shared across domains

**Architecture:**
- `common/` package contains all shared infrastructure
- No domain logic lives here — only cross-cutting concerns

---

### Phase 2: User Management — ✅ DONE

**Goal:** Complete CRUD for user profiles with validation, email normalization, and security.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **Create User** | POST endpoint (ADMIN only). Validates all fields, normalizes email to lowercase, hashes password with BCrypt, returns 201 with user data. |
| **Get User by ID** | GET endpoint (owner or ADMIN). Returns 200 with user data or 404 if not found. |
| **List Users** | GET endpoint (ADMIN only). Returns paginated results with configurable page size. |
| **Update User** | PATCH endpoint (owner or ADMIN). Partial update — only first name and last name are mutable. |
| **Delete User** | DELETE endpoint (owner or ADMIN). Removes user, returns 204. |
| **Change Password** | PATCH endpoint (owner or ADMIN). Validates old password matches, enforces password complexity rules, hashes and saves new password. |

**Validation rules:**
- First/last name: required, 2–50 characters
- Email: required, must include TLD (e.g., `user@domain.com`), stored lowercase
- Password: required, 8–100 characters, must contain uppercase, lowercase, digit, and special character
- Old password (change-password): required, must match current hash

**Database:**
- `users` table with case-insensitive unique email index via `LOWER()` function
- Separate Flyway migrations for unique index and role column

**Security:**
- `POST /api/v1/users` requires ADMIN role
- All other user endpoints enforce ownership via `@PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id()")`
- `UserIdPrincipal` record stored in `SecurityContextHolder` for ownership checks

**Architecture:**
- Single `UserService` handles all business logic (creation, lookup, update, deletion, password change)
- `UserMapper` (MapStruct) handles entity ↔ DTO conversion; email mapping is service-owned (ignored in mapper)
- `UserSecurityRules` defines which user endpoints are public vs. authenticated
- Domain exception (`PasswordMismatchException`) handled in `UserController`, not in `GlobalExceptionHandler`

---

### Phase 3: Auth/JWT — ✅ DONE

**Goal:** Implement JWT-based stateless authentication with token type discrimination.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **JWT Token Provider** | `JwtConfig` is a `@Configuration @Getter @Setter` class with prefix `spring.jwt`, externalizing secret and expiration via properties. Thread-safe `SecretKey` caching with double-checked locking and UTF-8 encoding. `JwtService` accepts a `User` entity and returns `Jwt` value objects wrapping jjwt `Claims` + `SecretKey`. Tokens carry a `type` claim (`"access"` or `"refresh"`). |
| **UserDetailsService** | `UserDetailsServiceImpl` loads user by email via `UserService` (not `UserRepository` directly); catches `ResourceNotFoundException` and wraps as `UsernameNotFoundException`. |
| **JWT Authentication Filter** | `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) intercepts requests, extracts JWT from `Authorization: Bearer` header, validates via `JwtService`, loads `User` by ID from JWT claims, stores `UserIdPrincipal` (id + email) in `SecurityContextHolder`. Inserted before `UsernamePasswordAuthenticationFilter`. |
| **Login Endpoint** | `POST /api/v1/auth/login` (public). Accepts email + password, delegates to `AuthenticationManager`, returns JWT access token wrapped in `ApiResponse`. Sets refresh token cookie. |
| **Register Endpoint** | `POST /api/v1/auth/register` (public). Delegates to `UserService.createUser`, returns user data. Sets refresh token cookie. Returns 201 on success, 409 on duplicate email. |
| **Token Refresh** | `POST /api/v1/auth/refresh` (public). Validates refresh token cryptographically (type claim must be `"refresh"`), returns new access token while reusing the same refresh token. Access tokens cannot be used as refresh tokens. |
| **Profile Endpoint** | `GET /api/v1/auth/me` (authenticated). Returns current user profile from security context. |
| **Logout Endpoint** | `POST /api/v1/auth/logout` (authenticated). Clears the refresh token cookie (sets maxAge to 0) and clears the SecurityContext. The access token expires naturally on its TTL. |

**Architecture:**
- Auth domain sits in `auth/` alongside existing security configuration
- JWT filter inserts before Spring's default filter chain
- Auth endpoints (`login`, `register`, `refresh`) are public via `AuthSecurityRules`; all other endpoints remain protected
- No refresh token table — tokens are validated cryptographically (stateless)
- `AuthController` handles `InvalidJwtAuthenticationException` and `BadCredentialsException` with 401 responses
- `CorsProperties` externalizes CORS config (`app.cors.*` in `application.yaml`); uses `allowedOriginPatterns` for Spring 6+ compatibility
- `EmailNormalizer` utility for consistent email normalization
- `ValidationConstants` includes `EMAIL_REGEX` and `EMAIL_MESSAGE`
- Consistent email validation: `LoginRequest` uses `@Pattern(EMAIL_REGEX)` (same as `RegisterRequest`)

---

### Phase 4: Account Management — ✅ DONE

**Goal:** CRUD for bank and investment accounts linked to users, with ownership enforcement and dynamic filtering.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **Create Account** | POST endpoint (authenticated). Validates name (3–20 alphanumeric chars), type (CHECKING/SAVINGS/INVESTMENT), currency (3-letter ISO), and non-negative initial balance. Returns 201. |
| **Get Account by ID** | GET endpoint (authenticated). Returns account details with type. Ownership verification via `findOwnedAccount()`. |
| **List User's Accounts** | GET endpoint (authenticated). Paginated list filtered by authenticated user. Supports search by name, filter by type and currency via JPA Specifications. |
| **Update Account** | PATCH endpoint (authenticated). Mutable field: currency. Ownership verification enforced. |
| **Update Account Type** | PATCH endpoint (ADMIN only, `@PreAuthorize`). Changes account type. |
| **Delete Account** | DELETE endpoint (authenticated). Removes account, returns 204. Ownership verification enforced. |

**Database:**
- `accounts` table with `user_id` FK, name, type FK to `account_types` lookup table, balance, currency
- `account_types` reference table: CHECKING, SAVINGS, INVESTMENT
- `@EntityGraph("Account.withType")` to prevent N+1 queries

**Security:**
- All account operations enforce ownership — users can only access their own accounts
- `PATCH /{id}/type` requires ADMIN role via `@PreAuthorize("hasRole('ADMIN')")`
- `AccountService.findOwnedAccount()` loads account and verifies `user_id` matches authenticated user

**Architecture:**
- `AccountTypeEntity` — reference/lookup table for account types
- `AccountSpecification` — dynamic JPA Specifications for search and filter
- `SearchUtils` — LIKE wildcard escaping to prevent LIKE injection
- `AccountMapper` (MapStruct) for entity ↔ DTO mapping
- `AccountSecurityRules` component implementing `SecurityRules`

---

### Phase 5: Transaction Management — ⬜ NOT STARTED

**Goal:** CRUD for financial transactions linked to accounts, with balance updates and reporting.

**Planned functionality:**

| Functionality | Description |
|---------------|-------------|
| **Create Transaction** | POST endpoint. Validates amount (positive), type (income/expense/transfer), category, and date. Updates account balance. |
| **Get Transaction by ID** | Returns transaction details. Ownership verification via account. |
| **List Account Transactions** | Paginated list for a specific account. |
| **Transaction Reports** | Summary by category and date range. Computed from transaction data (no separate report entity). |

**Architecture:**
- `transactions/` package with entity, repository, service, controller, DTOs
- `Transaction` entity with `@ManyToOne` to `Account`
- Transaction type enforced via enum (INCOME, EXPENSE, TRANSFER)
- Balance updates are transactional — atomic read-modify-write on account balance
- Reports are computed queries, not stored data (YAGNI)
- Flyway migration with foreign key to accounts table

---

### Phase 6: Code Review Fixes — ✅ DONE

**Goal:** Address all code review findings across security, architecture, code quality, and testing.

**What was delivered:**

| Category | Fix |
|----------|-----|
| **Security** | JWT tokens carry a `type` claim (`"access"`/`"refresh"`); access tokens cannot be used as refresh tokens |
| **Security** | `JwtConfig.secret` excluded from `toString()` via `@ToString.Exclude` to prevent log leakage |
| **Security** | `JwtConfig.getSecretKey()` uses thread-safe double-checked locking with `volatile` field |
| **Security** | UTF-8 encoding for JWT secret key derivation (`StandardCharsets.UTF_8`) |
| **Security** | `UserIdPrincipal` record stored in `SecurityContextHolder` for ownership-based `@PreAuthorize` checks |
| **Security** | User CRUD endpoints enforce ownership — users can only view/update/delete their own profile |
| **Security** | CORS uses `allowedOriginPatterns` instead of `allowedOrigins` (Spring 6+ compatibility) |
| **Architecture** | `UserDetailsServiceImpl` uses `UserService` (not `UserRepository` directly); wraps `ResourceNotFoundException` as `UsernameNotFoundException` |
| **Architecture** | Removed redundant service-level role check from `AccountService.updateAccountType()` |
| **Code Quality** | `JwtService.parseToken()` returns `Optional<Jwt>` instead of nullable |
| **Code Quality** | `Jwt.getType()` and `Jwt.isRefreshToken()` added for token type discrimination |
| **Code Quality** | `LoginRequest` email validation uses `@Pattern(EMAIL_REGEX)` (consistent with `RegisterRequest`) |
| **Code Quality** | `UserDto` includes `role` field |
| **Code Quality** | `AccountSpecification.nameContains` handles null/blank input safely |
| **Code Quality** | `AccountCreateRequest` name minimum reduced from 10 to 3 characters |
| **Code Quality** | Fixed fully qualified class name in `AuthService.register()` |
| **Testing** | `AccountServiceTest` properly cleans up `SecurityContextHolder` in `@AfterEach` |
| **Testing** | `AuthServiceTest` properly cleans up `SecurityContextHolder` in `@AfterEach` |
| **Testing** | `AuthServiceTest` covers access-token-as-refresh-token rejection |
| **Testing** | Removed unnecessary `UserService` mock from `AuthControllerTest` |
| **Testing** | `FinancetrackerApplicationTests` has `@DisplayName` annotation |

---

### Phase 7: Enum Error Handling — ✅ DONE

**Goal:** Provide descriptive error messages for invalid enum values in request bodies.

**What was delivered:**
- `GlobalExceptionHandler.handleMalformedRequest()` detects `InvalidFormatException` from Jackson enum deserialization failures
- Extracted into `buildInvalidEnumResponse()` private method for clean separation
- Returns clear message: `"Invalid value 'X' for field 'type'. Allowed values are: CHECKING, SAVINGS, INVESTMENT"`
- Works for any enum field in any request body (not just `AccountType`)
- Null-safe guards on `targetType` and `path`

**Test coverage (4 tests):**
- Invalid enum on `POST /accounts` (`"type": "INVALID"`)
- Invalid enum on `PATCH /accounts/{id}/type` (`"type": "BROKERAGE"`)
- Lowercase enum (`"type": "checking"`) — verifies case sensitivity
- Empty string enum (`"type": ""`) — verifies fallback to generic handler

---

### Phase 8: REST Documentation & Final Integration — ⬜ NOT STARTED

**Goal:** Generate API documentation and verify end-to-end flows.

**Planned functionality:**

| Functionality | Description |
|---------------|-------------|
| **AsciiDoc Generation** | All controller tests produce REST Docs snippets via `@AutoConfigureRestDocs`. |
| **API Documentation Index** | Centralized documentation covering all endpoints with request/response examples. |
| **End-to-End Integration Test** | Full flow: register → login → create account → create transaction → get report. |

---

## Dependency Graph

```
Phase 1 (Foundation)
  └── Phase 2 (Users)
        ├── Phase 3 (Auth/JWT) — builds on user repository and security rules
        └── Phase 4 (Accounts) — FK to users table
              └── Phase 5 (Transactions) — FK to accounts table
                    └── Phase 8 (Docs & Integration)

Phase 6 (Code Review Fixes) — applied across Phases 1–4
Phase 7 (Enum Error Handling) — applied to GlobalExceptionHandler
```

### Cross-Domain Dependencies

| From | To | Reason |
|------|-----|--------|
| Auth | Users | UserDetailsService loads users for authentication |
| Accounts | Users | Accounts belong to users (FK relationship) |
| Transactions | Accounts | Transactions belong to accounts (FK relationship) |
| Transactions | Accounts | Balance updates require account repository access |

---

## Migration Versioning

| Version | Domain | Schema |
|---------|--------|--------|
| V1 | Users | Users table with base columns |
| V2 | Users | Case-insensitive unique email index |
| V3 | Users | Add role column, drop redundant UNIQUE constraint on email |
| V4 | Accounts | Accounts table with FK to users |
| V5 | Accounts | Account types reference/lookup table |
| V6 | Transactions | (planned) Transactions table with FK to accounts |

---

## Current Status

| Phase | Status | Tests |
|-------|--------|-------|
| Phase 1: Foundation | ✅ Complete | — |
| Phase 2: User Management | ✅ Complete | 25 passing |
| Phase 3: Auth/JWT | ✅ Complete | 38 passing |
| Phase 4: Accounts | ✅ Complete | 38 passing |
| Phase 5: Transactions | ⬜ Not Started | — |
| Phase 6: Code Review Fixes | ✅ Complete | — |
| Phase 7: Enum Error Handling | ✅ Complete | 4 passing |
| Phase 8: Docs & Integration | ⬜ Not Started | — |
| **Total** | | **130 passing** |

---

## Test Inventory

| Test Class | Type | Count |
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
| **Total** | | **130** |

---

## API Endpoints

### User Domain

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/users` | ADMIN | Create user |
| GET | `/api/v1/users` | ADMIN | List users (paginated) |
| GET | `/api/v1/users/{id}` | Owner or ADMIN | Get user by ID |
| PATCH | `/api/v1/users/{id}` | Owner or ADMIN | Update user (partial) |
| DELETE | `/api/v1/users/{id}` | Owner or ADMIN | Delete user |
| PATCH | `/api/v1/users/{id}/change-password` | Owner or ADMIN | Change password |

### Auth Domain

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/login` | permitAll | Login with email + password |
| POST | `/api/v1/auth/register` | permitAll | Register new user |
| POST | `/api/v1/auth/refresh` | permitAll | Refresh access token |
| GET | `/api/v1/auth/me` | authenticated | Get current user profile |
| POST | `/api/v1/auth/logout` | authenticated | Clear refresh token cookie and security context |

### Account Domain

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/accounts` | authenticated | Create account |
| GET | `/api/v1/accounts/{id}` | authenticated (owner) | Get account by ID |
| GET | `/api/v1/accounts` | authenticated | List accounts (paginated, filtered) |
| PATCH | `/api/v1/accounts/{id}` | authenticated (owner) | Update account currency |
| PATCH | `/api/v1/accounts/{id}/type` | ADMIN | Update account type |
| DELETE | `/api/v1/accounts/{id}` | authenticated (owner) | Delete account |
