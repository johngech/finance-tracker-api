# Finance Tracker — Implementation Plan

## Context Summary

- **Project:** `com.marakicode.financetracker` — Spring Boot 3.5.0 / Java 17 / Maven monolith
- **Architecture:** PostgreSQL + Flyway + JPA + Lombok + MapStruct + Spring Security 6.5.0 + REST Docs, DDD-style package layout
- **Current State:** Foundation, User Management, Auth/JWT, Accounts, Transactions, Code Review Fixes (Rounds 1 & 2), Enum Error Handling, 3NF Refactoring, and Reports Domain are all complete. JWT-based stateless authentication with login, register, refresh, and profile endpoints is fully implemented. Ownership-based authorization via `UserIdPrincipal` and `@PreAuthorize` is enforced on all domain endpoints. Transaction type and category are normalized to 3NF reference tables (Flyway V6/V7). `SecurityUtils` provides shared current-user resolution. Reports domain provides read-only JPQL aggregation queries (SUM, COUNT, GROUP BY, CASE, COALESCE, date functions) over existing transaction data — no new tables. 227 tests passing, 2 skipped.
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
| R6 | Transaction management CRUD (financial transactions linked to accounts) | `transactions/` | High | ✅ Done |
| R7 | Financial reporting (summaries by category and time period) | `reports/` | Medium | ✅ Done |
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
                       # constants, SearchUtils, EmailNormalizer, SecurityUtils
  auth/                # Security config, JWT auth (JwtConfig, JwtService, Jwt,
                       # JwtAuthenticationFilter, UserIdPrincipal, AuthController,
                       # AuthService, AuthSecurityRules, UserDetailsServiceImpl)
  users/               # User domain: entity, CRUD, validation, email normalization
  users/exceptions/    # PasswordMismatchException
  accounts/            # Account domain: entity, CRUD, ownership enforcement, filtering
  accounts/dto/        # AccountCreateRequest, AccountResponse, CurrencyUpdateRequest,
                       # UpdateAccountTypeRequest
  transactions/        # Transaction domain: entity, CRUD, balance management, ownership
                       # enforcement, dynamic filtering, 3NF type/category reference tables
  transactions/dto/    # TransactionCreateRequest, TransactionUpdateRequest,
                       # TransactionResponse
  reports/             # Reports read-only aggregation: controller, service, repository,
                       # security rules
  reports/dto/         # SummaryResponse, CategoryBreakdownResponse,
                       # MonthlyBreakdownResponse, AccountBreakdownResponse
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
| **SecurityUtils** | Static utility in `common/` that resolves the current authenticated user from `SecurityContextHolder`. Guards against null/blank auth, throws `AccessDeniedException`. Shared by `TransactionService` and `AccountService` — eliminates duplicate current-user resolution logic. |
| **3NF Reference Tables** | Transaction type and category are normalized into reference tables (`transaction_types`, `transaction_categories`) with FK constraints. Service layer uses find-or-create for dynamic categories (`insertIfAbsent` + `findByName`). Mapper uses `@Named` converters to translate entities to DTOs. Specifications JOIN reference tables for filtering. |
| **JPQL Aggregation** | Read-only reports domain uses JPQL aggregate queries (`SUM`, `COUNT`, `GROUP BY`, `CASE` inside `SUM`, `COALESCE`, `MONTH()`/`YEAR()` date functions) on existing transaction data. Repository returns `Object[]` / `List<Object[]>`, service maps to DTO records. Null-safe optional parameters via `:param IS NULL OR ...` pattern. No new tables — derived data computed at query time. |

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
| `MissingServletRequestParameterException` | 400 | `GlobalExceptionHandler` (missing required query params) |
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

### Phase 5: Transaction Management — ✅ DONE

**Goal:** CRUD for financial transactions linked to accounts, with balance updates and filtering.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **Create Transaction** | POST endpoint (authenticated). Validates amount (`@DecimalMin("0.01")`), type (INCOME/EXPENSE), category, and date (`@PastOrPresent`). Updates account balance — INCOME adds, EXPENSE subtracts. Throws `InsufficientFundsException` on insufficient funds. |
| **Get Transaction by ID** | GET endpoint (authenticated). Returns transaction details. Ownership verification via account ownership. |
| **List Transactions** | Paginated list (authenticated). Dynamic filtering via JPA Specifications: filter by accountId, type, category, description search, and date range. |
| **Update Transaction** | PATCH endpoint (authenticated). Partial update — only mutable fields (description, amount, category, date). Empty body returns existing transaction unchanged (no-op). Balance only adjusted when type or amount actually changed. Account assignment immutable after creation. |
| **Delete Transaction** | DELETE endpoint (authenticated). Removes transaction, reverses balance effect, returns 204. Ownership verification via account. |

**Database:**
- `transactions` table with `account_id` FK, type, amount, description, transaction_date, category
- `@NamedEntityGraph("Transaction.withAccount")` to prevent N+1 queries

**Security:**
- All transaction operations enforce ownership via account ownership — users can only access transactions on their own accounts
- `TransactionSecurityRules` component implementing `SecurityRules`

**Architecture:**
- `TransactionMapper` (MapStruct) for entity ↔ DTO mapping with explicit nested path `@Mapping` annotations
- `TransactionSpecification` — dynamic JPA Specifications for filtering
- `InsufficientFundsException` handled in `TransactionController` with 400 Bad Request
- Empty PATCH body handling: all-null update fields return existing transaction unchanged

**Test coverage:** 56 tests (12 repo + 25 service + 19 controller)

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

### Phase 8: Transaction Type & Category 3NF Refactoring — ✅ DONE

**Goal:** Normalize transaction type and category from flat strings/enums to proper 3NF reference tables.

**What was delivered:**

| Category | Change |
|----------|--------|
| **DB** | Flyway V6 creates `transaction_types` and `transaction_categories` reference tables, seeds INCOME/EXPENSE types, migrates existing category data, adds FK constraints. Flyway V7 adds indexes on FK columns and cleans up whitespace-only categories. |
| **Entity** | `Transaction.type` changed from `@Enumerated(EnumType.STRING)` to `@ManyToOne(LAZY)` → `TransactionTypeEntity`. `Transaction.category` changed from free-text `String` to `@ManyToOne(LAZY)` → `TransactionCategoryEntity`. |
| **Reference Tables** | `TransactionTypeEntity` + `TransactionTypeRepository` (fixed: INCOME, EXPENSE). `TransactionCategoryEntity` + `TransactionCategoryRepository` (dynamic: find-or-create on first use). |
| **Mapper** | `TransactionMapper` ignores `type`/`category` in `toEntity`/`updateEntity`. Adds `@Named("toEnum")` and `@Named("toCategoryName")` converters for `toResponse`. |
| **Service** | `TransactionService` injects `TransactionTypeRepository` and `TransactionCategoryRepository`. `resolveType()` looks up type entity. `resolveCategory()` does find-or-create. `toTransactionType()` converts entity to enum for balance logic. |
| **Specifications** | `TransactionSpecification.typeEquals()` and `categoryEquals()` join reference tables via `root.join("type")` / `root.join("category")` and compare on `name` field. |
| **Entity Graph** | `Transaction.withAccount` now includes `type` and `category` attribute nodes to prevent N+1. |
| **API Contract** | Unchanged — DTOs still accept `TransactionType` enum and `String category`; response still returns enum type and string category. |

---

### Phase 9: Code Review Round 1 — ✅ DONE

**Goal:** Address race conditions, performance indexes, balance guard logic, DRY violations, and defensive coding.

**What was delivered:**

| Category | Fix |
|----------|-----|
| **P1 — Race condition** | `TransactionCategoryRepository.insertIfAbsent()` uses native `INSERT ... ON CONFLICT DO NOTHING`. `TransactionService.resolveCategory()` calls `insertIfAbsent` then `findByName` — atomic and safe under concurrency. |
| **P1 — FK indexes** | Flyway V7 adds `idx_transactions_type` and `idx_transactions_category` on FK columns for efficient Specification JOINs; also cleans up whitespace-only categories. |
| **P2 — Balance guard** | `TransactionService.updateTransaction()` only reverses/applies balance when `type` or `amount` actually changed — prevents transient incorrect balance on metadata-only updates and eliminates unnecessary `accountRepository.save()`. |
| **P2 — Method length** | Extracted `buildTransaction()` private method from `createTransaction()` to stay within 10-line limit. |
| **P2 — DRY** | Extracted `getCurrentUser()` to `common/SecurityUtils.getCurrentUser(UserService)` — shared by `TransactionService` and `AccountService`. |
| **P3 — Auth exception** | `SecurityUtils.getCurrentUser()` throws `AccessDeniedException("Not authenticated")` instead of misusing `ResourceNotFoundException`. |
| **Tests** | 8 new tests covering null category, invalid type, balance skip on metadata-only update, combined specification filters, validation errors, and empty PATCH body. |

---

### Phase 10: Code Review Round 2 — ✅ DONE

**Goal:** Fix migration safety, extract long methods, harden defensive coding, and improve test strictness.

**What was delivered:**

| Category | Fix |
|----------|-----|
| **P1 — Migration safety** | V7 now nulls out `transactions.category` before deleting whitespace-only `transaction_categories` rows — prevents FK constraint violation. |
| **P2 — Method length** | Extracted `reconcileBalanceIfNeeded()` private method (balance comparison + reverse/check/apply) from `updateTransaction()` — method now ≤10 lines. |
| **P2 — Defensive coding** | `SecurityUtils.getCurrentUser()` now guards against null/blank `auth.getName()` — throws `AccessDeniedException` instead of passing null/blank to `findByEmail()`. |
| **P2 — Mock strictness** | Removed class-level `@MockitoSettings(strictness = Strictness.LENIENT)` from `TransactionServiceTest`; replaced with per-stub `lenient()` on `@BeforeEach` stubs only. |
| **Tests** | `SecurityUtilsTest` (5 tests), `TransactionCategoryRepositoryTest` (3 tests, 2 skipped — native ON CONFLICT requires PostgreSQL), 4 new service tests (balance adjustment, category persistence, category creation failure, null category delete). |

---

### Phase 11: Reports Domain — ✅ DONE

**Goal:** Read-only aggregation endpoints for financial reporting — income/expense summaries, category breakdown, monthly trends, and per-account analysis.

**What was delivered:**

| Functionality | Description |
|---------------|-------------|
| **Summary Endpoint** | `GET /api/v1/reports/summary?from=&to=` — Returns totalIncome, totalExpense, netBalance, transactionCount. Uses `CASE` inside `SUM` for conditional aggregation. |
| **Category Breakdown** | `GET /api/v1/reports/by-category?type=&from=&to=` — Spending by category with percentage computation. Uses `GROUP BY` + `COALESCE` for null categories ("Uncategorized"). Optional type filter (INCOME/EXPENSE). |
| **Monthly Breakdown** | `GET /api/v1/reports/monthly?year=` — Month-by-month income vs expense. Uses `MONTH()` and `YEAR()` JPQL date functions. |
| **Account Breakdown** | `GET /api/v1/reports/by-account?from=&to=` — Per-account income/expense/net. Groups across entity relationships (`t.account.id`, `t.account.name`). |
| **Date Range Validation** | Private `validateDateRange()` throws `IllegalArgumentException` when `from` is after `to`. Handled by `@ExceptionHandler` in controller returning 400. |
| **Missing Parameter Handling** | `GlobalExceptionHandler` added `MissingServletRequestParameterException` handler — returns 400 for missing required params (e.g., `year`). |

**Architecture:**
- Read-only aggregation domain — **no new DB tables, no Flyway migration, no entities, no MapStruct**
- `ReportsRepository` — 4 JPQL aggregate queries returning `Object[]` / `List<Object[]>`
- `ReportsService` — orchestrates queries, maps `Object[]` → DTO records, computes category percentages
- `ReportsSecurityRules` — authentication enforced via catch-all (no permitAll rules)
- `ReportsController` — 4 GET endpoints at `/api/v1/reports/*` with `@DateTimeFormat` on date params
- DTOs are response records (no request DTOs, no validation annotations needed)
- Uses `SecurityUtils.getCurrentUser(userService)` for shared current-user resolution (DRY)

**JPQL patterns demonstrated:**
- `SUM(CASE WHEN ... THEN ... ELSE ... END)` — conditional aggregation
- `COALESCE(..., 0)` — null-safe aggregates
- `GROUP BY ... ORDER BY SUM(...) DESC` — grouped sorted results
- `MONTH(t.transactionDate)`, `YEAR(t.transactionDate)` — date functions
- `:param IS NULL OR ...` — null-safe optional parameters
- `t.type.name`, `t.category.name`, `t.account.user.id` — navigating `@ManyToOne` relationships

**H2 compatibility:**
- `ReportsRepositoryTest` uses `flattenResult()` helper — H2 wraps multi-column aggregate results in a nested `Object[]` unlike PostgreSQL

**Test coverage:** 31 tests (10 repo + 10 service + 11 controller)

---

### Phase 13: CI/CD, Git Hooks & Static Analysis — DONE

**Goal:** Automated CI pipeline, local developer safety hooks, and security-focused static analysis tooling.

**What was delivered:**

| Category | What |
|----------|------|
| **Maven Plugins** | OWASP Dependency-Check (`12.1.1`) — scans dependencies for known CVEs, fails on CVSS ≥ 7, bound to `verify` phase. SpotBugs + FindSecBugs (`4.8.6.6` / `1.13.0`) — code-level vulnerability detection (SQL injection, hardcoded creds, insecure crypto, XSS), `effort=Max`, `threshold=Medium`, bound to `verify` phase. |
| **SpotBugs Filter** | `config/spotbugs-exclude.xml` — excludes Lombok inner classes, MapStruct mapper impls, JPA/Spring proxy classes, test classes, DTO packages, controller return-value-ignored findings, and `@ConfigurationProperties` false positives. |
| **OWASP Suppression** | `config/dependency-check-suppression.xml` — template for future false-positive suppressions. |
| **Gitleaks Config** | `.gitleaks.toml` — allowlist for test `application.yaml` (fake JWT secret + DB password), `target/`, `.idea/`, `.mvn/`; custom rule override for test secret patterns. |
| **Pre-commit Hook** | `.githooks/pre-commit` — runs `gitleaks detect --staged --redact` on staged files; blocks commit if secrets detected; gracefully skips if gitleaks not installed. |
| **Pre-push Hook** | `.githooks/pre-push` — runs `mvn compile -q` then `mvn test -q`; blocks push on failure; bypassable via `SKIP_PUSH_HOOKS=1` env var. |
| **Hooks Setup Script** | `.githooks/setup.sh` — one-command git hooks activation (`git config core.hooksPath .githooks`), idempotent. |
| **GitHub Actions CI** | `.github/workflows/ci.yml` — 4 parallel jobs: (1) Build & Test (PR + main), (2) Gitleaks secret scan with full history (PR + main), (3) OWASP Dependency-Check with NVD caching (main only), (4) SpotBugs/FindSecBugs (main only). Test reports uploaded as artifacts. |
| **Dependabot** | `.github/dependabot.yml` — weekly Maven dependency update PRs (Monday), monthly GitHub Actions dependency update PRs. |
| **Documentation** | `AGENTS.md` updated with CI/CD & Security section covering hooks setup, Maven analysis commands, CI workflow overview, and Dependabot. |

**Environment-based behavior:**

| Trigger | Build & Test | Gitleaks | OWASP | SpotBugs |
|---------|-------------|----------|-------|----------|
| PR to main | ✅ | ✅ | ❌ | ❌ |
| Push to main | ✅ | ✅ | ✅ | ✅ |
| Dependabot PR | ✅ | ✅ | ❌ | ❌ |

**Tools installed:**
- OWASP Dependency-Check: `mvn dependency-check:check`
- SpotBugs + FindSecBugs: `mvn spotbugs:spotbugs`
- Gitleaks: `gitleaks detect --config .gitleaks.toml`
- Git hooks: `bash .githooks/setup.sh`

**Files created/modified:**
- `pom.xml` — added OWASP + SpotBugs/FindSecBugs plugins
- `config/spotbugs-exclude.xml` — SpotBugs exclude filter
- `config/dependency-check-suppression.xml` — OWASP suppression template
- `.gitleaks.toml` — Gitleaks configuration
- `.githooks/pre-commit` — secret detection hook
- `.githooks/pre-push` — compile + test gate hook
- `.githooks/setup.sh` — hooks activation helper
- `.github/workflows/ci.yml` — CI pipeline
- `.github/dependabot.yml` — dependency updates
- `AGENTS.md` — documentation updates

---

### Phase 12: REST Documentation & Final Integration — ⬜ NOT STARTED

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
                    ├── Phase 8 (3NF Refactoring) — normalizes type/category to reference tables
                    ├── Phase 9 (Code Review Round 1) — race conditions, indexes, DRY, balance guard
                    ├── Phase 10 (Code Review Round 2) — migration safety, method extraction, defensive coding
                    └── Phase 11 (Reports) — read-only aggregation over transaction data

Phase 6 (Code Review Fixes) — applied across Phases 1–4
Phase 7 (Enum Error Handling) — applied to GlobalExceptionHandler
Phase 13 (CI/CD, Git Hooks & Static Analysis) — independent, no code dependencies
Phase 12 (Docs & Integration)
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
| V6 | Transactions | Transaction types + categories reference tables with FK constraints |
| V7 | Transactions | FK indexes on type/category + whitespace category cleanup |

---

## Current Status

| Phase | Status | Tests |
|-------|--------|-------|
| Phase 1: Foundation | ✅ Complete | — |
| Phase 2: User Management | ✅ Complete | 25 passing |
| Phase 3: Auth/JWT | ✅ Complete | 36 passing |
| Phase 4: Accounts | ✅ Complete | 32 passing |
| Phase 5: Transactions | ✅ Complete | 56 passing |
| Phase 6: Code Review Fixes | ✅ Complete | — |
| Phase 7: Enum Error Handling | ✅ Complete | 4 passing |
| Phase 8: 3NF Refactoring | ✅ Complete | — |
| Phase 9: Code Review Round 1 | ✅ Complete | 8 passing |
| Phase 10: Code Review Round 2 | ✅ Complete | 12 passing |
| Phase 11: Reports Domain | ✅ Complete | 31 passing |
| Phase 12: Docs & Integration | ⬜ Not Started | — |
| Phase 13: CI/CD, Git Hooks & Static Analysis | ✅ Complete | — |
| **Total** | | **227 passing (+2 skipped)** |

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
| `TransactionRepositoryTest` | `@DataJpaTest` | 12 |
| `TransactionServiceTest` | `@ExtendWith(MockitoExtension.class)` | 25 |
| `TransactionControllerTest` | `@WebMvcTest` (mocked service) | 19 |
| `TransactionCategoryRepositoryTest` | `@DataJpaTest` | 3 (+2 skipped) |
| `SecurityUtilsTest` | `@ExtendWith(MockitoExtension.class)` | 5 |
| `ReportsRepositoryTest` | `@DataJpaTest` | 10 |
| `ReportsServiceTest` | `@ExtendWith(MockitoExtension.class)` | 10 |
| `ReportsControllerTest` | `@WebMvcTest` (mocked service) | 11 |
| **Total** | | **227 (+2 skipped)** |

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

### Transaction Domain

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/transactions` | authenticated | Create transaction |
| GET | `/api/v1/transactions/{id}` | authenticated (owner via account) | Get transaction by ID |
| GET | `/api/v1/transactions` | authenticated | List transactions (paginated, filterable) |
| PATCH | `/api/v1/transactions/{id}` | authenticated (owner via account) | Update transaction (partial) |
| DELETE | `/api/v1/transactions/{id}` | authenticated (owner via account) | Delete transaction |

### Reports Domain

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/reports/summary` | authenticated | Income/Expense summary for a date range |
| GET | `/api/v1/reports/by-category` | authenticated | Spending breakdown by category |
| GET | `/api/v1/reports/monthly` | authenticated | Month-by-month income vs expense |
| GET | `/api/v1/reports/by-account` | authenticated | Per-account income/expense breakdown |
