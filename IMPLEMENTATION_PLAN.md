# Finance Tracker Microservice — Implementation Plan

## Context Summary

- **Project:** `com.marakicode.financetracker` — Spring Boot 3.5.0 / Java 17 / Maven monolith
- **Architecture:** PostgreSQL + Flyway + JPA + Lombok + REST Docs, DDD-style package layout
- **Current State:** Empty skeleton — only `FinancetrackerApplication.java` and context-loads test exist. No migrations, no business logic.
- **Target State:** Fully functional finance tracker with JWT auth, CRUD for users/accounts/transactions, financial reporting, pagination, validation, and REST documentation.

### Key Files Discovered

| File | Status |
|------|--------|
| `pom.xml` | **Missing critical dependencies**: `spring-boot-starter-web`, `spring-boot-starter-security`, `jjwt-api/impl/jackson`, `spring-boot-starter-validation`, `spring-boot-starter-test` |
| `src/main/resources/application.yaml` | PostgreSQL datasource configured (localhost:5432/financetracker) |
| `src/main/resources/db/migration/` | Empty — no migrations yet |
| `src/main/java/.../FinancetrackerApplication.java` | Plain `@SpringBootApplication` entrypoint |
| `AGENTS.md` | DDD conventions, sub-package rule (only `entity/`, `repo/`, `dto/`, `service/` when >2 files of that type) |

---

## Requirements

| ID | Requirement | Domain | Priority |
|----|------------|--------|----------|
| R1 | Update pom.xml with missing dependencies | Build | High |
| R2 | Create shared foundation (BaseEntity, ApiResponse, PagedResponse, GlobalExceptionHandler) | `common/` | High |
| R3 | User management CRUD (create, read user profiles) | `users/` | High |
| R4 | JWT authentication (login, register, refresh tokens) | `auth/` | High |
| R5 | Account management CRUD (bank/investment accounts) | `accounts/` | High |
| R6 | Transaction management CRUD (financial transactions linked to accounts) | `transactions/` | High |
| R7 | Financial reporting (summaries by category and time period) | `transactions/` | Medium |
| R8 | Data validation (positive amounts, transaction types) | Service Layer | Medium |
| R9 | Pagination for list endpoints | Controller + Service | Medium |
| R10 | Standard error handling with HTTP status codes | `common/` | Medium |
| R11 | Audit tracking (createdAt, updatedAt timestamps) | Entity Layer | Medium |
| R12 | REST documentation (AsciiDoc snippets) | Tests + `src/main/asciidoc/` | Medium |

---

## Phase Breakdown

### Phase 1: Foundation (`common/` + Build Config)

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P1.T1 | Update pom.xml with missing dependencies | `pom.xml` | — | @feature-engineer | `./mvnw compile` succeeds; `spring-boot-starter-web`, `spring-boot-starter-security`, `jjwt-api`/`jjwt-impl`/`jjwt-jackson`, `spring-boot-starter-validation`, `spring-boot-starter-test` are present |
| P1.T2 | Create BaseEntity `@MappedSuperclass` | `src/main/java/com/marakicode/financetracker/common/BaseEntity.java` | P1.T1 | @feature-engineer | Compiles; has `id` (Long, `@Id @GeneratedValue`), `createdAt`, `updatedAt` (`@PrePersist`/`@PreUpdate`), uses `@MappedSuperclass` and Lombok `@Getter` |
| P1.T3 | Create `ApiResponse<T>` record | `src/main/java/com/marakicode/financetracker/common/ApiResponse.java` | P1.T1 | @feature-engineer | Compiles; generic record with `success`, `message`, `data` fields |
| P1.T4 | Create `PagedResponse<T>` record | `src/main/java/com/marakicode/financetracker/common/PagedResponse.java` | P1.T1 | @feature-engineer | Compiles; generic record wrapping `content`, `page`, `size`, `totalElements`, `totalPages` |
| P1.T5 | Create `GlobalExceptionHandler` | `src/main/java/com/marakicode/financetracker/common/GlobalExceptionHandler.java` | P1.T3 | @feature-engineer | Compiles; `@RestControllerAdvice`; handles `MethodArgumentNotValidException`, `ResourceNotFoundException`, generic `Exception`; returns `ApiResponse<?>` with correct HTTP status codes |

**Phase 1 Dependency Graph:** P1.T1 → [P1.T2, P1.T3, P1.T4] (parallel) → P1.T5

---

### Phase 2: User Management (`users/`)

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P2.T1 | Create Flyway migration for users table | `src/main/resources/db/migration/V1__create_users_table.sql` | P1.T1 | @database-engineer | `./mvnw flyway:migrate` succeeds; table `users` has columns: `id` BIGSERIAL PK, `username` VARCHAR UNIQUE NOT NULL, `email` VARCHAR UNIQUE NOT NULL, `password_hash` VARCHAR NOT NULL, `created_at` TIMESTAMPTZ NOT NULL, `updated_at` TIMESTAMPTZ NOT NULL |
| P2.T2 | Create User entity | `src/main/java/com/marakicode/financetracker/users/User.java` | P1.T2, P2.T1 | @feature-engineer | Compiles; `@Entity @Table(name = "users")`; extends `BaseEntity`; Lombok `@Getter @NoArgsConstructor`; fields map 1:1 to migration columns |
| P2.T3 | Create UserRepository | `src/main/java/com/marakicode/financetracker/users/UserRepository.java` | P2.T2 | @feature-engineer | Compiles; extends `JpaRepository<User, Long>`; has `findByUsername(String)`, `findByEmail(String)`, `existsByUsername(String)`, `existsByEmail(String)` |
| P2.T4 | Create UserRequest & UserResponse DTOs | `src/main/java/com/marakicode/financetracker/users/UserRequest.java`, `src/main/java/com/marakicode/financetracker/users/UserResponse.java` | P2.T2 | @feature-engineer | Compiles; `UserRequest` is a record with `@NotBlank`/`@Email` validation on `username`, `email`, `password`; `UserResponse` is a record with `id`, `username`, `email`, `createdAt` (no password exposed) |
| P2.T5 | Create CreateUserCommand | `src/main/java/com/marakicode/financetracker/users/CreateUserCommand.java` | P2.T3, P2.T4 | @feature-engineer | Compiles; `@Service`; accepts `UserRequest`, checks duplicate username/email (throws exception), hashes password with `BCryptPasswordEncoder`, saves via `UserRepository`, returns `UserResponse` |
| P2.T6 | Create GetUserQuery | `src/main/java/com/marakicode/financetracker/users/GetUserQuery.java` | P2.T3, P2.T4 | @feature-engineer | Compiles; `@Service`; `findById(Long)` returns `UserResponse`; `findAll(Pageable)` returns `PagedResponse<UserResponse>` |
| P2.T7 | Create UserController | `src/main/java/com/marakicode/financetracker/users/UserController.java` | P2.T5, P2.T6, P2.T4 | @feature-engineer | Compiles; `@RestController @RequestMapping("/api/v1/users")`; `POST /` (create, returns 201), `GET /{id}` (returns 200), `GET /` (paginated list, returns 200); uses `ApiResponse<T>` wrapper; validates `@Valid @RequestBody` |
| P2.T8 | Create User tests | `src/test/java/com/marakicode/financetracker/users/UserRepositoryTest.java`, `src/test/java/com/marakicode/financetracker/users/UserControllerTest.java` | P2.T7 | @test-engineer | `@DataJpaTest` repo test passes; `@WebMvcTest` controller test passes with mocked service; REST docs snippets generated |

**Phase 2 Dependency Graph:** P2.T1 → P2.T2 → [P2.T3, P2.T4] (parallel) → [P2.T5, P2.T6] (parallel) → P2.T7 → P2.T8

---

### Phase 3: JWT Authentication (`auth/`)

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P3.T1 | Create JwtTokenProvider utility | `src/main/java/com/marakicode/financetracker/auth/JwtTokenProvider.java` | P1.T1 | @feature-engineer | Compiles; generates access tokens (15min) and refresh tokens (7 days); validates tokens; extracts username from token; uses `io.jsonwebtoken` library |
| P3.T2 | Create CustomUserDetailsService | `src/main/java/com/marakicode/financetracker/auth/CustomUserDetailsService.java` | P2.T3 | @feature-engineer | Compiles; implements `UserDetailsService`; loads user by username via `UserRepository`; returns Spring Security `UserDetails` |
| P3.T3 | Create JwtAuthenticationFilter | `src/main/java/com/marakicode/financetracker/auth/JwtAuthenticationFilter.java` | P3.T1, P3.T2 | @feature-engineer | Compiles; extends `OncePerRequestFilter`; extracts JWT from `Authorization: Bearer` header; validates token; sets `SecurityContext` authentication; skips public endpoints |
| P3.T4 | Create AuthRequest & AuthResponse DTOs | `src/main/java/com/marakicode/financetracker/auth/AuthRequest.java`, `src/main/java/com/marakicode/financetracker/auth/AuthResponse.java` | P1.T1 | @feature-engineer | Compiles; `AuthRequest` record with `@NotBlank username`, `@NotBlank password`; `AuthResponse` record with `accessToken`, `refreshToken`, `tokenType`, `expiresIn` |
| P3.T5 | Create AuthController | `src/main/java/com/marakicode/financetracker/auth/AuthController.java` | P2.T5, P3.T1, P3.T4 | @feature-engineer | Compiles; `@RestController @RequestMapping("/api/v1/auth")`; `POST /login` (returns `AuthResponse`), `POST /register` (delegates to `CreateUserCommand`, returns `AuthResponse`), `POST /refresh` (validates refresh token, returns new `AuthResponse`); public (no security) |
| P3.T6 | Create SecurityConfig | `src/main/java/com/marakicode/financetracker/auth/SecurityConfig.java` | P3.T3, P3.T5 | @feature-engineer | Compiles; `@Configuration @EnableWebSecurity`; permits `/api/v1/auth/**`; protects all other `/api/v1/**`; registers `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`; `BCryptPasswordEncoder` bean; `SecurityFilterChain` bean |
| P3.T7 | Create Auth tests | `src/test/java/com/marakicode/financetracker/auth/AuthControllerTest.java` | P3.T6 | @test-engineer | `@WebMvcTest` passes; login returns JWT; register creates user and returns JWT; invalid credentials return 401; REST docs snippets generated |

**Phase 3 Dependency Graph:** [P3.T1, P3.T4] (parallel) → P3.T2 (needs P2.T3) → P3.T3 → P3.T5 → P3.T6 → P3.T7

---

### Phase 4: Account Management (`accounts/`)

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P4.T1 | Create Flyway migration for accounts table | `src/main/resources/db/migration/V2__create_accounts_table.sql` | P2.T1 | @database-engineer | `./mvnw flyway:migrate` succeeds; table `accounts` has: `id` BIGSERIAL PK, `name` VARCHAR NOT NULL, `type` VARCHAR NOT NULL (CHECK: CHECKING/SAVINGS/INVESTMENT), `currency` VARCHAR NOT NULL, `balance` DECIMAL(19,4) NOT NULL DEFAULT 0, `user_id` BIGINT NOT NULL FK→users(id), `created_at`/`updated_at` TIMESTAMPTZ NOT NULL |
| P4.T2 | Create Account entity | `src/main/java/com/marakicode/financetracker/accounts/Account.java` | P1.T2, P4.T1 | @feature-engineer | Compiles; `@Entity @Table(name = "accounts")`; extends `BaseEntity`; `@ManyToOne(fetch = LAZY) @JoinColumn(name = "user_id") User user`; enum `Type {CHECKING, SAVINGS, INVESTMENT}` |
| P4.T3 | Create AccountRepository | `src/main/java/com/marakicode/financetracker/accounts/AccountRepository.java` | P4.T2 | @feature-engineer | Compiles; extends `JpaRepository<Account, Long>`; has `findAllByUserId(Long, Pageable)` |
| P4.T4 | Create AccountRequest & AccountResponse DTOs | `src/main/java/com/marakicode/financetracker/accounts/AccountRequest.java`, `src/main/java/com/marakicode/financetracker/accounts/AccountResponse.java` | P4.T2 | @feature-engineer | Compiles; `AccountRequest` record with `@NotBlank name`, `@NotNull type`, `@NotBlank currency`, `@PositiveOrZero initialBalance`; `AccountResponse` record with `id`, `name`, `type`, `currency`, `balance`, `userId`, `createdAt`, `updatedAt` |
| P4.T5 | Create CreateAccountCommand | `src/main/java/com/marakicode/financetracker/accounts/CreateAccountCommand.java` | P4.T3, P4.T4 | @feature-engineer | Compiles; `@Service`; maps `AccountRequest` → `Account` entity (sets user from security context), saves, returns `AccountResponse` |
| P4.T6 | Create GetAccountQuery | `src/main/java/com/marakicode/financetracker/accounts/GetAccountQuery.java` | P4.T3, P4.T4 | @feature-engineer | Compiles; `@Service`; `findById(Long)` returns `AccountResponse`; `findAllByUserId(Long, Pageable)` returns `PagedResponse<AccountResponse>` |
| P4.T7 | Create AccountController | `src/main/java/com/marakicode/financetracker/accounts/AccountController.java` | P4.T5, P4.T6 | @feature-engineer | Compiles; `@RestController @RequestMapping("/api/v1/accounts")`; `POST /` (201), `GET /{id}` (200), `GET /` (paginated, 200); secured (requires authenticated user); uses `ApiResponse<T>` |
| P4.T8 | Create Account tests | `src/test/java/com/marakicode/financetracker/accounts/AccountRepositoryTest.java`, `src/test/java/com/marakicode/financetracker/accounts/AccountControllerTest.java` | P4.T7 | @test-engineer | `@DataJpaTest` repo test passes; `@WebMvcTest` controller test passes; REST docs snippets generated |

**Phase 4 Dependency Graph:** P4.T1 → P4.T2 → [P4.T3, P4.T4] (parallel) → [P4.T5, P4.T6] (parallel) → P4.T7 → P4.T8

---

### Phase 5: Transaction Management (`transactions/`)

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P5.T1 | Create Flyway migration for transactions table | `src/main/resources/db/migration/V3__create_transactions_table.sql` | P4.T1 | @database-engineer | `./mvnw flyway:migrate` succeeds; table `transactions` has: `id` BIGSERIAL PK, `account_id` BIGINT NOT NULL FK→accounts(id), `type` VARCHAR NOT NULL (CHECK: INCOME/EXPENSE/TRANSFER), `amount` DECIMAL(19,4) NOT NULL, `category` VARCHAR NOT NULL, `description` VARCHAR, `transaction_date` DATE NOT NULL, `created_at`/`updated_at` TIMESTAMPTZ NOT NULL |
| P5.T2 | Create Transaction entity | `src/main/java/com/marakicode/financetracker/transactions/Transaction.java` | P1.T2, P5.T1 | @feature-engineer | Compiles; `@Entity @Table(name = "transactions")`; extends `BaseEntity`; `@ManyToOne(fetch = LAZY) @JoinColumn(name = "account_id") Account account`; enum `Type {INCOME, EXPENSE, TRANSFER}`; `LocalDate transactionDate` |
| P5.T3 | Create TransactionRepository | `src/main/java/com/marakicode/financetracker/transactions/TransactionRepository.java` | P5.T2 | @feature-engineer | Compiles; extends `JpaRepository<Transaction, Long>`; has `findAllByAccountId(Long, Pageable)`, `findByAccountIdAndTransactionDateBetween(...)`, `findByAccountIdAndCategory(...)` |
| P5.T4 | Create TransactionRequest & TransactionResponse DTOs | `src/main/java/com/marakicode/financetracker/transactions/TransactionRequest.java`, `src/main/java/com/marakicode/financetracker/transactions/TransactionResponse.java` | P5.T2 | @feature-engineer | Compiles; `TransactionRequest` record with `@NotNull accountId`, `@NotNull type`, `@Positive amount`, `@NotBlank category`, `description`, `@NotNull transactionDate`; `TransactionResponse` record with all fields including `id`, `createdAt`, `updatedAt` |
| P5.T5 | Create CreateTransactionCommand | `src/main/java/com/marakicode/financetracker/transactions/CreateTransactionCommand.java` | P5.T3, P5.T4, P4.T3 | @feature-engineer | Compiles; `@Service @Transactional`; validates account exists and belongs to current user; updates account balance (add for INCOME, subtract for EXPENSE); saves transaction; returns `TransactionResponse` |
| P5.T6 | Create GetTransactionQuery | `src/main/java/com/marakicode/financetracker/transactions/GetTransactionQuery.java` | P5.T3, P5.T4 | @feature-engineer | Compiles; `@Service`; `findById(Long)` returns `TransactionResponse`; `findAllByAccountId(Long, Pageable)` returns `PagedResponse<TransactionResponse>`; `findByDateRange(...)` for reporting |
| P5.T7 | Create TransactionController | `src/main/java/com/marakicode/financetracker/transactions/TransactionController.java` | P5.T5, P5.T6 | @feature-engineer | Compiles; `@RestController @RequestMapping("/api/v1/transactions")`; `POST /` (201), `GET /{id}` (200), `GET /` (paginated by accountId, 200), `GET /report` (summary by category/date range); secured; uses `ApiResponse<T>` |
| P5.T8 | Create Transaction tests | `src/test/java/com/marakicode/financetracker/transactions/TransactionRepositoryTest.java`, `src/test/java/com/marakicode/financetracker/transactions/TransactionControllerTest.java` | P5.T7 | @test-engineer | `@DataJpaTest` repo test passes; `@WebMvcTest` controller test passes; REST docs snippets generated |

**Phase 5 Dependency Graph:** P5.T1 → P5.T2 → [P5.T3, P5.T4] (parallel) → [P5.T5, P5.T6] (parallel) → P5.T7 → P5.T8

---

### Phase 6: REST Documentation & Final Integration

| ID | Task | Files | Dependencies | Assigned To | Acceptance Criteria |
|----|------|-------|-------------|-------------|---------------------|
| P6.T1 | Create AsciiDoc index file | `src/main/asciidoc/index.adoc` | P2.T8, P3.T7, P4.T8, P5.T8 | @feature-engineer | `./mvnw package` succeeds; generated HTML docs include all API endpoints with request/response examples |
| P6.T2 | Create end-to-end integration test | `src/test/java/com/marakicode/financetracker/FinanceTrackerIntegrationTest.java` | P6.T1 | @test-engineer | `@SpringBootTest` with `@AutoConfigureMockMvc`; full flow: register → login → create account → create transaction → get report; all assertions pass |

**Phase 6 Dependency Graph:** P6.T1 → P6.T2

---

## Overall Dependency Graph

```
Phase 1: P1.T1 → [P1.T2, P1.T3, P1.T4] → P1.T5

Phase 2: P2.T1 → P2.T2 → [P2.T3, P2.T4] → [P2.T5, P2.T6] → P2.T7 → P2.T8

Phase 3: [P3.T1, P3.T4] → P3.T2 → P3.T3 → P3.T5 → P3.T6 → P3.T7

Phase 4: P4.T1 → P4.T2 → [P4.T3, P4.T4] → [P4.T5, P4.T6] → P4.T7 → P4.T8

Phase 5: P5.T1 → P5.T2 → [P5.T3, P5.T4] → [P5.T5, P5.T6] → P5.T7 → P5.T8

Phase 6: P6.T1 → P6.T2
```

### Cross-Phase Dependencies

| From | To | Reason |
|------|-----|--------|
| P2.T1 | P1.T1 | pom.xml must have Flyway dependency |
| P2.T2 | P1.T2 | BaseEntity must exist |
| P3.T2 | P2.T3 | UserRepository must exist for UserDetailsService |
| P3.T5 | P2.T5 | CreateUserCommand needed for registration endpoint |
| P4.T1 | P2.T1 | users table migration must be applied first (FK target) |
| P4.T2 | P1.T2 | BaseEntity must exist |
| P5.T1 | P4.T1 | accounts table migration must be applied first (FK target) |
| P5.T2 | P1.T2 | BaseEntity must exist |
| P5.T5 | P4.T3 | AccountRepository needed for balance updates |

### Overall Critical Path

```
P1.T1 → P2.T1 → P2.T2 → P2.T3 → P2.T5 → P3.T5 → P3.T6
                                                          ↓
P4.T1 → P4.T2 → P4.T3 → P4.T5 → P4.T7               → P6.T1 → P6.T2
                                                          ↑
P5.T1 → P5.T2 → P5.T3 → P5.T5 → P5.T7 ──────────────→
```

**Estimated Total Tasks:** 34 atomic tasks across 6 phases

---

## Validation Report

| Check | Status | Notes |
|-------|--------|-------|
| **Completeness** | ✅ PASS | All 12 requirements (R1–R12) mapped to specific tasks |
| **SOLID** | ✅ PASS | SRP: one entity/repo/service per domain; DIP: services depend on repository interfaces; ISP: focused service classes |
| **CQS** | ✅ PASS | Commands (`Create*`) and Queries (`Get*`) in separate classes per domain |
| **DRY** | ✅ PASS | BaseEntity shared across all entities; ApiResponse/PagedResponse shared across all controllers; no duplicated migration or entity definitions |
| **KISS** | ✅ PASS | Simple flat package structure; no over-engineering; records for DTOs |
| **YAGNI** | ✅ PASS | No Report entity (computed from transactions); no Role entity (MVP: single role); no refresh token table (stateless JWT) |
| **Correct Ordering** | ✅ PASS | Flyway → Entity → Repository → DTO → Service → Controller → Tests within each phase; Foundation before all domains; Users before Auth; Accounts before Transactions |
| **Blast Radius** | ✅ PASS | Max 2 files per task (except tests which are naturally grouped); single-file migrations |
| **Atomicity** | ✅ PASS | Each task is independently compilable and verifiable |
| **Phase Cohesion** | ✅ PASS | Phase 1 = foundation; Phase 2 = users; Phase 3 = auth; Phase 4 = accounts; Phase 5 = transactions; Phase 6 = docs + integration |
| **DDD Compliance** | ✅ PASS | Bounded contexts (users, auth, accounts, transactions, common) with self-contained packages; no sub-packages needed (≤2 files per type per domain) |
| **Migration Versioning** | ✅ PASS | V1 (users) → V2 (accounts, FK→users) → V3 (transactions, FK→accounts) — correct FK ordering |

---

## Verdict: ✅ READY FOR EXECUTION

### Summary

| Metric | Value |
|--------|-------|
| **Total Phases** | 6 |
| **Total Atomic Tasks** | 34 |
| **Max Files Per Task** | 2 (within 4-file blast radius limit) |
| **Max Sequential Dependencies** | 6 phases |
| **Risk Profile** | LOW — validation checkpoints between each phase |
| **DDD Bounded Contexts** | 5 (common, users, auth, accounts, transactions) |
| **Migrations** | 3 (V1 users, V2 accounts, V3 transactions) |
| **New Java Files** | ~28 source + ~6 test files |

### Key Design Decisions

1. **DDD-style bounded contexts** — code organized by domain (`users/`, `auth/`, `accounts/`, `transactions/`, `common/`), NOT by technical layer
2. **CQS pattern** — separate `Create*Command` and `Get*Query` service classes per domain
3. **Records for DTOs** — immutable, concise, validation-annotated request/response records
4. **Flat packages** — no sub-packages needed (each domain has ≤2 files per type)
5. **BaseEntity inheritance** — shared audit fields (`id`, `createdAt`, `updatedAt`) via `@MappedSuperclass`
6. **Migration FK ordering** — users → accounts → transactions (correct parent-before-child)
7. **No Report entity** — reports are computed from transactions (YAGNI)
8. **Stateless JWT** — no refresh token table; tokens validated cryptographically

### Implementation Readiness

This plan is ready for the `@lead-developer` to begin execution starting with Phase 1 (P1.T1: Update pom.xml).
