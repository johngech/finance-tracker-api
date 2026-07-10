# Finance Tracker — Implementation Plan

## Context Summary

- **Project:** `com.marakicode.financetracker` — Spring Boot 3.5.0 / Java 17 / Maven monolith
- **Architecture:** PostgreSQL + Flyway + JPA + Lombok + MapStruct + Spring Security 6.5.0 + REST Docs, DDD-style package layout
- **Current State:** Foundation and User Management are complete. Security skeleton with modular rules is in place. JWT authentication is pending.
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
  auth/                # Security configuration, CORS, password encoding, (JWT pending)
  users/               # User domain: entity, CRUD, validation, email normalization
  accounts/            # (planned) Account domain
  transactions/        # (planned) Transaction domain
```

### Key Design Patterns

| Pattern | Description |
|---------|-------------|
| **Modular SecurityRules** | Each domain provides a `SecurityRules` bean. `SecurityConfig` aggregates all beans and applies them, then adds `anyRequest().authenticated()` as a catch-all. |
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

### Phase 3: Auth/JWT — 🔄 IN PROGRESS

**Goal:** Implement JWT-based stateless authentication.

**What's done:**
- `SecurityConfig` with modular `SecurityRules` aggregation, CORS externalized via configuration properties, BCrypt `PasswordEncoder` bean
- Security rules: user registration is public, all other endpoints require authentication
- Security integration tests verifying access control

**What's pending:**

| Functionality | Description |
|---------------|-------------|
| **JWT Token Provider** | Generates access tokens (short-lived) and refresh tokens (long-lived). Validates tokens, extracts claims. |
| **UserDetailsService** | Loads user by email for Spring Security authentication. |
| **JWT Authentication Filter** | Intercepts requests, extracts JWT from `Authorization: Bearer` header, validates, sets security context. |
| **Login Endpoint** | Accepts email + password, returns JWT access and refresh tokens. |
| **Register Endpoint** | Delegates to user creation, returns JWT tokens. |
| **Token Refresh** | Validates refresh token, returns new access token. |

**Architecture:**
- Auth domain sits in `auth/` alongside existing security configuration
- JWT filter inserts before Spring's default filter chain
- Auth endpoints are public; all other endpoints remain protected
- No refresh token table — tokens are validated cryptographically (stateless)

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
| Phase 3: Auth/JWT | 🔄 In Progress | 6 security integration tests |
| Phase 4: Accounts | ⬜ Not Started | — |
| Phase 5: Transactions | ⬜ Not Started | — |
| Phase 6: Docs & Integration | ⬜ Not Started | — |
