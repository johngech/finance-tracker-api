# Finance Tracker — Implementation Plan

## Context Summary

- **Project:** `com.marakicode.financetracker` — Spring Boot 3.5.0 / Java 17 / Maven monolith
- **Architecture:** PostgreSQL + Flyway + JPA + Lombok + MapStruct + Spring Security 6.5.0 + REST Docs, DDD-style package layout
- **Current State:** Foundation, User Management, and Auth/JWT are complete. JWT-based stateless authentication with login, register, refresh, and profile endpoints is fully implemented and tested. `Jwt` is an immutable value object wrapping `Claims` + `SecretKey` with `isExpired()`, `getUserId()`, `getRole()` methods.
- **Target State:** Fully functional finance tracker with JWT auth, CRUD for users/accounts/transactions, financial reporting, pagination, validation, and REST documentation.

---

## Requirements

| ID | Requirement | Domain | Priority |
|----|------------|--------|----------|
| R1 | Shared foundation (base entities, API wrappers, exception handling) | `common/` | High |
| R2 | User management CRUD with validation and email normalization | `users/` | High |
| R3 | Security skeleton with modular per-domain rules | `auth/` | High |
| R4 | JWT authentication (login, register, refresh tokens) | `auth/` | High |
| R5 | Account management CRUD (bank/investment accounts) | `accounts/` | High |
| R6 | Transaction management CRUD (financial transactions linked to accounts) | `transactions/` | High |
| R7 | Financial reporting (summaries by category and time period) | `transactions/` | Medium |
| R8 | Data validation (positive amounts, transaction types) | Service Layer | Medium |
| R9 | Pagination for list endpoints | Controller + Service | Medium |
| R10 | Standard error handling with HTTP status codes | `common/` | Medium |
| R11 | Audit tracking (createdAt, updatedAt timestamps) | Entity Layer | Medium |
| R12 | REST documentation (AsciiDoc snippets) | Tests + docs | Medium |

---

## Architecture Decisions

### DDD-Style Package Layout

Each domain is a self-contained bounded context with its own controller, service, repository, entity, DTOs, and exceptions. Sub-packages (`entity/`, `repository/`, `dto/`, `service/`) are only created when a domain has **more than 2 files** of that type — otherwise files sit flat at the domain root.

```
com.marakicode.financetracker/
  common/              # Shared foundation: base entities, API wrappers, exception handling, constants
  auth/                # Security config, JWT auth (JwtConfig, JwtService, JwtAuthenticationFilter,
                       # AuthController, AuthService, AuthSecurityRules, UserDetailsService)
  users/               # User domain: entity, CRUD, validation, email normalization
  accounts/            # (planned) Account domain
  transactions/        # (planned) Transaction domain
```

### Key Design Patterns

| Pattern | Description |
|---------|-------------|
| **Modular SecurityRules** | Each domain provides a `SecurityRules` bean. `SecurityConfig` aggregates all beans and applies them, then adds `anyRequest().authenticated()` as a catch-all. |
| **JWT Authentication** | Stateless auth via `JwtService` (jjwt). `Jwt` is an immutable value object wrapping `Claims` + `SecretKey`. `JwtAuthenticationFilter` extracts Bearer tokens, validates, loads `User` by ID from JWT claims, sets `SecurityContextHolder`. `JwtConfig` is a `@Configuration @Data` class that externalizes secret and expiration via `spring.jwt.*` properties and caches a `SecretKey`. |
| **ApiResponse / PagedResponse** | All endpoints return `ApiResponse<T>` for success and `ErrorDto` for errors. List endpoints use `PagedResponse<T>` for pagination. |
| **GlobalExceptionHandler** | Centralized `@RestControllerAdvice` handles validation errors, resource not found, duplicates, method not allowed, malformed requests, type mismatches, and unexpected exceptions. Domain-specific handlers live in their respective controllers. |
| **MapStruct + Lombok** | Entities use `@Getter/@Setter` (never `@Data`). DTOs are records. MapStruct mappers use `componentModel = "spring"` for injection. Services and controllers use `@RequiredArgsConstructor` for constructor injection. |
| **Method-Level @Transactional** | Write methods get `@Transactional`, read methods get `@Transactional(readOnly = true)`. No class-level annotations. |
| **Flyway Migrations** | One migration per schema change, never edit an applied migration. Migrations are versioned sequentially (V1, V2, V3...). |
| **Case-Insensitive Email** | Emails normalized to lowercase before persistence. Database enforces uniqueness via a `LOWER()` function index. |

### API Response Convention

- **Success:** `{ "success": true, "message": "...", "data": {...} }`
- **Error:** `{ "timestamp": "...", "status": 400, "error": "...", "message": "...", "path": "...", "fieldErrors": [...] }`
- **Pagination:** `{ "content": [...], "page": 0, "size": 10, "count": 1, "totalPages": 1 }`

### Exception Handling Convention

| Exception | HTTP Status | Handler Location |
|-----------|-------------|-----------------|
| `MethodArgumentNotValidException` | 400 | `GlobalExceptionHandler` |
| `HttpMessageNotReadableException` | 400 | `GlobalExceptionHandler` |
| `MethodArgumentTypeMismatchException` | 400 | `GlobalExceptionHandler` |
| `ResourceNotFoundException` | 404 | `GlobalExceptionHandler` |
| `DuplicateResourceException` | 409 | `GlobalExceptionHandler` |
| `HttpRequestMethodNotSupportedException` | 405 | `GlobalExceptionHandler` |
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
| **Create User** | POST endpoint (public). Validates all fields, normalizes email to lowercase, hashes password with BCrypt, returns 201 with user data. |
| **Get User by ID** | GET endpoint (authenticated). Returns 200 with user data or 404 if not found. |
| **List Users** | GET endpoint (authenticated). Returns paginated results with configurable page size. |
| **Update User** | PATCH endpoint (authenticated). Partial update — only first name and last name are mutable. Email and password are immutable via this endpoint. |
| **Delete User** | DELETE endpoint (authenticated). Removes user, returns 200. |
| **Change Password** | PATCH endpoint (authenticated). Validates old password matches, enforces password complexity rules, hashes and saves new password. |

**Validation rules:**
- First/last name: required, 2–50 characters
- Email: required, must include TLD (e.g., `user@domain.com`), stored lowercase
- Password: required, 8–100 characters, must contain uppercase, lowercase, digit, and special character
- Old password (change-password): required, must match current hash

**Database:**
- `users` table with case-insensitive unique email index via `LOWER()` function
- Separate Flyway migration for the unique index (idempotent, uses `DROP INDEX IF EXISTS`)

**Security:**
- User registration is public (`POST /api/v1/users` → `permitAll`)
- All other user endpoints require authentication
- Security rules are modular — each domain contributes its own `SecurityRules` bean

**Architecture:**
- Single `UserService` handles all business logic (creation, lookup, update, deletion, password change)
- `UserMapper` (MapStruct) handles entity ↔ DTO conversion; email mapping is service-owned (ignored in mapper)
- `UserSecurityRules` defines which user endpoints are public vs. authenticated
- Domain exception (`PasswordMismatchException`) handled in `UserController`, not in `GlobalExceptionHandler`

**Test coverage (39 tests):**
- Repository tests: CRUD operations, case-insensitive email queries, unique constraint enforcement
- Controller tests: all endpoints with happy paths, error paths (404, 400, 409, 405), validation errors
- Service tests: business logic, email normalization, duplicate detection, password hashing
- Security integration tests: public vs. protected endpoint access control

---

### Phase 3: Auth/JWT — ✅ DONE

**Goal:** Implement JWT-based stateless authentication.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **JWT Token Provider** | `JwtConfig` is a `@Configuration @Data` class with prefix `spring.jwt`, externalizing secret and expiration via properties and caching a `SecretKey`. `JwtService` accepts a `User` entity and returns `Jwt` value objects wrapping jjwt `Claims` + `SecretKey`. `Jwt` provides `isExpired()`, `getUserId()`, `getRole()`. Uses jjwt with HMAC-SHA signing. |
| **UserDetailsService** | `UserDetailsServiceImpl` loads user by email via `UserRepository.findByEmailIgnoreCase` for Spring Security authentication. |
| **JWT Authentication Filter** | `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) intercepts requests, extracts JWT from `Authorization: Bearer` header, validates via `JwtService`, loads `User` by ID from JWT claims, sets `SecurityContextHolder`. Inserted before `UsernamePasswordAuthenticationFilter`. |
| **Login Endpoint** | `POST /api/v1/auth/login` (public). Accepts email + password, delegates to `AuthenticationManager`, returns JWT access and refresh tokens wrapped in `ApiResponse`. |
| **Register Endpoint** | `POST /api/v1/auth/register` (public). Delegates to `UserService.createUser`, returns JWT tokens. Returns 201 on success, 409 on duplicate email. |
| **Token Refresh** | `POST /api/v1/auth/refresh` (public). Validates refresh token cryptographically, returns new access token while reusing the same refresh token. |
| **Profile Endpoint** | `GET /api/v1/auth/me` (authenticated). Returns current user profile from security context. |

**Architecture:**
- Auth domain sits in `auth/` alongside existing security configuration
- JWT filter inserts before Spring's default filter chain
- Auth endpoints (`login`, `register`, `refresh`) are public via `AuthSecurityRules`; all other endpoints remain protected
- No refresh token table — tokens are validated cryptographically (stateless)
- `AuthController` handles `InvalidJwtAuthenticationException` and `BadCredentialsException` with 401 responses
- `CorsProperties` externalizes CORS config (`app.cors.*` in `application.yaml`)

**Test coverage (36 new tests, 75 total):**
- `AuthControllerTest` (8): login/register/refresh/me happy paths, validation errors (400), bad credentials (401), duplicate email (409)
- `AuthServiceTest` (7): login/register/refresh business logic, credential validation, token generation, /me lookup
- `JwtServiceTest` (11): token generation, parsing, validation, expiration, cross-user validation, `Jwt` value object methods
- `JwtAuthenticationFilterTest` (5): valid JWT sets context, missing header skipped, invalid JWT skipped, filter chain always proceeds, user lookup by ID
- `UserDetailsServiceImplTest` (2): user lookup by email, not-found exception
- `SecurityConfigTest` (+3): JWT-authenticated access, login/register integration with real JWT flow

---

### Phase 4: Account Management — ⬜ NOT STARTED

**Goal:** CRUD for bank and investment accounts linked to users.

**Planned functionality:**

| Functionality | Description |
|---------------|-------------|
| **Create Account** | POST endpoint. Validates account type (checking/savings/investment), currency, and non-negative initial balance. |
| **Get Account by ID** | Returns account details. Ownership verification required. |
| **List User's Accounts** | Paginated list of accounts belonging to the authenticated user. |
| **Update Account** | PATCH endpoint. Mutable fields: name, currency. |
| **Delete Account** | Removes account. Business rules around linked transactions TBD. |

**Architecture:**
- `accounts/` package with entity, repository, service, controller, DTOs
- `Account` entity with `@ManyToOne` relationship to `User`
- Account type enforced via enum and database CHECK constraint
- Security: users can only access their own accounts
- Flyway migration with foreign key to users table

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

### Phase 6: REST Documentation & Final Integration — ⬜ NOT STARTED

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
                    └── Phase 6 (Docs & Integration)
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
| V3 | Accounts | Accounts table with FK to users |
| V4 | Transactions | Transactions table with FK to accounts |

---

## Current Status

| Phase | Status | Tests |
|-------|--------|-------|
| Phase 1: Foundation | ✅ Complete | — |
| Phase 2: User Management | ✅ Complete | 39 passing |
| Phase 3: Auth/JWT | ✅ Complete | 75 passing |
| Phase 4: Accounts | ⬜ Not Started | — |
| Phase 5: Transactions | ⬜ Not Started | — |
| Phase 6: Docs & Integration | ⬜ Not Started | — |
