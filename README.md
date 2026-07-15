<div align="center">

# 💰 FinanceTracker

### A Production-Grade Personal Finance Management API

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)
![Tests](https://img.shields.io/badge/Tests-333+-blue?style=flat-square&logo=junit5&logoColor=white)

RESTful API for tracking personal finances — manage accounts, record transactions, generate financial reports, and administer users with role-based access control.

[![CI](https://github.com/marakicode/financetracker/actions/workflows/ci.yml/badge.svg)](https://github.com/marakicode/financetracker/actions/workflows/ci.yml)

</div>

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Package Structure](#-package-structure)
- [Database Design](#-database-design)
- [Security Architecture](#-security-architecture)
- [API Endpoints](#-api-endpoints)
- [Code Quality & Patterns](#-code-quality--patterns)
- [Testing Strategy](#-testing-strategy)
- [CI/CD Pipeline](#-cicd-pipeline)
- [Getting Started](#-getting-started)

---

## 🔍 Overview

FinanceTracker is a **stateless REST API** that enables users to manage their personal finances through multiple bank-like accounts, record income and expense transactions with automatic balance reconciliation, and generate insightful financial reports — all secured with JWT-based authentication and role-based authorization.

### Core Capabilities

| Domain | Description |
|--------|-------------|
| **Authentication** | JWT-based login/register with refresh token rotation via HttpOnly cookies |
| **User Management** | Full CRUD with ownership enforcement, password change, role-based access |
| **Account Management** | Multiple accounts per user (CHECKING, SAVINGS, INVESTMENT) with currency support |
| **Transaction Tracking** | Income/Expense recording with automatic balance updates, category tagging, date filtering |
| **Financial Reports** | Summary, category breakdown, monthly trends, and per-account analytics |
| **Admin Dashboard** | Platform-wide user/account/transaction statistics and management (suspend, freeze, role changes) |

---

## 🛠 Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Framework** | Spring Boot 3.5.16 | Application bootstrap, auto-configuration, dependency injection |
| **Language** | Java 17 | LTS version with records for immutable DTOs |
| **Security** | Spring Security 6.5 | Stateless JWT authentication, method-level security (`@PreAuthorize`) |
| **Persistence** | Spring Data JPA + Hibernate | ORM, entity management, specification-based dynamic queries |
| **Database** | PostgreSQL | Production relational database with strong analytical query capabilities |
| **Migrations** | Flyway | Version-controlled, repeatable database schema evolution |
| **Mapping** | MapStruct 1.6.3 | Compile-time-safe entity ↔ DTO mapping (zero reflection overhead) |
| **Boilerplate** | Lombok | `@RequiredArgsConstructor` injection, `@Getter`/`@Setter` |
| **Validation** | Jakarta Bean Validation | Declarative request validation via annotations |
| **JWT** | jjwt 0.12.7 | Token generation, parsing, and cryptographic validation |
| **API Docs** | SpringDoc OpenAPI 2.8.17 | Swagger UI at `/swagger-ui.html`, auto-generated from annotations |
| **Monitoring** | Spring Actuator | Health checks, metrics, and application info endpoints |
| **Testing** | JUnit 5 + Mockito + MockMvc | Unit, integration, and slice tests |
| **Static Analysis** | SpotBugs + FindSecBugs | Automated security vulnerability detection in CI |
| **Build** | Maven | Dependency management, plugin orchestration |

---

## 🏗 Architecture

### High-Level Architecture Diagram

```mermaid
graph TB
    CLIENT["REST Client"]
    CLIENT --> CTRL["Controllers"]

    subgraph CTRL ["API Layer"]
        direction LR
        AC["AuthController"]
        UC["UserController"]
        ACC["AccountController"]
        TC["TransactionController"]
        RC["ReportsController"]
        ADC["Admin Controllers x4"]
    end

    subgraph SVC ["Business Layer"]
        direction LR
        AS["AuthService"]
        US["UserService"]
        ACS["AccountService"]
        TS["TransactionService"]
        RS["ReportsService"]
        ADS["Admin Services x4"]
    end

    subgraph FCD ["Facade Layer"]
        direction LR
        UF["UsersFacade"]
        AF["AccountsFacade"]
        TF["TransactionsFacade"]
        RF["ReportsFacade"]
    end

    subgraph REPO ["Data Layer"]
        direction LR
        UR["UserRepository"]
        ACR["AccountRepository"]
        TR["TransactionRepository"]
        RR["ReportsRepository"]
    end

    subgraph INFRA ["Infrastructure"]
        direction LR
        DB[("PostgreSQL")]
        FW["Flyway Migrations"]
        JWT["JwtService"]
    end

    CTRL --> SVC
    ADC --> ADS
    ADS --> FCD
    SVC --> REPO
    REPO --> DB
    FW -.-> DB
```

### Layered Responsibility Model

```mermaid
graph LR
    A["Controllers<br/>---<br/>HTTP routing<br/>Request validation<br/>Response wrapping<br/>Swagger docs"] --> B["Services<br/>---<br/>Business logic<br/>Transaction boundaries<br/>Balance reconciliation<br/>Ownership enforcement"] --> C["Repositories<br/>---<br/>JPA data access<br/>Specification queries<br/>Native SQL aggregations<br/>Entity graphs"] --> D["Database<br/>---<br/>FK constraints<br/>Unique indexes<br/>CHECK constraints<br/>Cascade deletes"]

    style A fill:#e3f2fd,stroke:#1565c0
    style B fill:#e8f5e9,stroke:#2e7d32
    style C fill:#fff3e0,stroke:#ef6c00
    style D fill:#fce4ec,stroke:#c62828
```

### Modular Security Architecture

```mermaid
graph TB
    REQ["HTTP Request"] --> FILTER["JwtAuthenticationFilter"]
    FILTER -->|"Extract Bearer token"| JWT_SVC["JwtService.parseToken()"]
    JWT_SVC -->|"Returns Jwt value object"| LOAD["Load User by ID"]
    LOAD -->|"Set SecurityContext"| CHAIN["SecurityFilterChain"]
    CHAIN --> RULES["SecurityRules Collection"]
    RULES --> AUTH_R["AuthSecurityRules: /auth/** permitAll"]
    RULES --> USER_R["UserSecurityRules: /users/** authenticated"]
    RULES --> ACCOUNT_R["AccountSecurityRules: /accounts/** authenticated"]
    RULES --> TXN_R["TransactionSecurityRules: /transactions/** authenticated"]
    RULES --> ADMIN_R["AdminSecurityRules: /admin/** ROLE_ADMIN"]
    RULES --> ACTUATOR_R["ActuatorSecurityRules: /actuator/health,/info permitAll"]
    RULES --> OPENAPI_R["OpenApiSecurityRules: /swagger-ui/** permitAll (non-prod)"]
    CHAIN --> CATCH_ALL["anyRequest().authenticated()"]
    CHAIN --> CONTROLLER["RestController"]

    style FILTER fill:#fff9c4,stroke:#f9a825
    style JWT_SVC fill:#e1f5fe,stroke:#0288d1
    style CHAIN fill:#f3e5f5,stroke:#7b1fa2
```

> **Key Insight**: Each bounded context provides its own `SecurityRules` implementation. `SecurityConfig` collects all beans and applies them dynamically — no monolithic URL-matching configuration. The `JwtAuthenticationFilter` authenticates requests by parsing the JWT and setting the `SecurityContext` directly (no `AuthenticationManager` involved). `AuthenticationManager` is only used during `POST /auth/login` for username/password authentication.

---

## 📁 Package Structure

```
com.marakicode.financetracker/
│
├── common/                          # Shared infrastructure
│   ├── ApiResponse.java             # Unified response wrapper: { success, message, data }
│   ├── PagedResponse.java           # Paginated list wrapper: { content, page, size, count, totalPages }
│   ├── BaseEntity.java              # @MappedSuperclass: createdAt, updatedAt with @PrePersist/@PreUpdate
│   ├── ErrorDto.java                # Structured error: { timestamp, status, error, message, path, fieldErrors }
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice: maps exceptions → HTTP responses
│   ├── SecurityRules.java           # @FunctionalInterface: per-domain security config
│   ├── CurrentUserProvider.java     # Abstraction for extracting authenticated user ID
│   ├── ResourceNotFoundException.java
│   ├── DuplicateResourceException.java
│   ├── ValidationConstants.java     # Shared regex patterns (email, etc.)
│   ├── EmailNormalizer.java         # Case-insensitive email normalization
│   ├── SearchUtils.java             # LIKE wildcard escaping (LIKE injection prevention)
│   ├── OpenApiConfig.java           # Swagger UI configuration
│   ├── OpenApiSecurityRules.java    # Swagger/OpenAPI access (non-prod only)
│   ├── ActuatorSecurityRules.java   # /actuator/health, /info permitAll; rest authenticated
│   └── RequestLoggingFilter.java    # HTTP request/response logging
│
├── auth/                            # Authentication & Authorization
│   ├── SecurityConfig.java          # Security filter chain, CORS, BCrypt, method security
│   ├── Jwt.java                     # Immutable value object wrapping jjwt Claims
│   ├── JwtConfig.java               # @ConfigurationProperties: secret, expiration, SecretKey cache
│   ├── JwtService.java              # Token generation, parsing, validation
│   ├── JwtAuthenticationFilter.java # OncePerRequestFilter: Bearer → SecurityContext
│   ├── UserIdPrincipal.java         # Record: userId + email for @PreAuthorize checks
│   ├── AuthService.java             # Orchestrates login/register/refresh/logout
│   ├── AuthController.java          # POST /login, /register, /refresh, /logout; GET /me
│   ├── AuthSecurityRules.java       # permitAll for /auth/**
│   ├── CorsProperties.java          # Externalized CORS config via app.cors.* properties
│   ├── UserDetailsServiceImpl.java  # Loads user by email for Spring Security
│   ├── AuthCurrentUserProvider.java # CurrentUserProvider implementation via SecurityContext
│   ├── AuthExceptionHandler.java    # Handles InvalidJwtAuthenticationException, BadCredentialsException
│   └── dto/                         # LoginRequest, RegisterRequest, JwtResponse, etc.
│
├── users/                           # User management
│   ├── User.java                    # @Entity: firstName, lastName, email, passwordHash, role, active
│   ├── Role.java                    # Enum: USER, ADMIN
│   ├── UserRepository.java          # JpaRepository<User, Long>
│   ├── UserService.java             # CRUD + password change + ownership enforcement
│   ├── UserController.java          # REST endpoints with @PreAuthorize ownership checks
│   ├── UserMapper.java              # MapStruct: User ↔ UserDto/UserCreateRequest
│   ├── UserSecurityRules.java       # Ownership-based access: users can view/edit own profile
│   ├── UserExceptionHandler.java    # Handles PasswordMismatchException, LastAdminActionException
│   ├── UsersFacade.java             # Interface for cross-domain operations and statistics
│   ├── UsersFacadeImpl.java         # Facade implementation delegating to UserService
│   ├── AdminInitializer.java        # Seeds default admin user on startup
│   ├── exceptions/
│   │   ├── PasswordMismatchException.java
│   │   └── LastAdminActionException.java
│   └── dto/                         # UserDto, UserCreateRequest, UserUpdateRequest, etc.
│
├── accounts/                        # Account management
│   ├── Account.java                 # @Entity: name, type (FK), balance, currency, frozen
│   ├── AccountType.java             # Enum: CHECKING, SAVINGS, INVESTMENT
│   ├── AccountTypeEntity.java       # Reference table entity (3NF)
│   ├── AccountRepository.java       # JpaRepository + Specifications for dynamic filtering
│   ├── AccountService.java          # CRUD + balance initialization + ownership enforcement
│   ├── AccountSpecification.java    # JPA Specifications: nameContains, typeEquals, currencyEquals
│   ├── AccountController.java       # REST endpoints with filtering and pagination
│   ├── AccountSecurityRules.java    # Ownership-based access for account operations
│   ├── AccountExceptionHandler.java # Handles AccountTypeNotFoundException
│   ├── AccountTypeNotFoundException.java
│   ├── AccountsFacade.java          # Interface for cross-domain operations and statistics
│   ├── AccountsFacadeImpl.java      # Facade implementation delegating to AccountService
│   └── dto/                         # AccountCreateRequest, AccountResponse, etc.
│
├── transactions/                    # Transaction tracking
│   ├── Transaction.java             # @Entity: account (FK), type (FK), amount, category (FK), date
│   ├── TransactionType.java         # Enum: INCOME, EXPENSE
│   ├── TransactionTypeEntity.java   # Reference table entity (3NF)
│   ├── TransactionTypeRepository.java
│   ├── TransactionCategoryEntity.java # Dynamic category entity (find-or-create)
│   ├── TransactionCategoryRepository.java # Native INSERT ON CONFLICT for atomic creation
│   ├── TransactionRepository.java   # JpaRepository + Specifications
│   ├── TransactionService.java      # CRUD + balance reconciliation + insufficient funds check
│   ├── TransactionMapper.java       # MapStruct: entity ↔ DTO with @Named methods
│   ├── TransactionSpecification.java # Dynamic filtering: account, type, category, date range
│   ├── TransactionController.java   # REST endpoints with rich filtering
│   ├── TransactionSecurityRules.java # Ownership-based access via account ownership
│   ├── TransactionExceptionHandler.java # Handles InsufficientFundsException, AccountFrozenException
│   ├── InsufficientFundsException.java
│   ├── AccountFrozenException.java
│   ├── TransactionsFacade.java      # Interface for cross-domain operations and statistics
│   ├── TransactionsFacadeImpl.java  # Facade implementation delegating to TransactionService
│   └── dto/                         # TransactionCreateRequest, TransactionResponse, etc.
│
├── reports/                         # Financial reporting (read-only aggregation)
│   ├── ReportsRepository.java       # Native SQL: SUM, COUNT, GROUP BY, CASE, COALESCE
│   ├── ReportsService.java          # Orchestrates queries, maps results to DTOs
│   ├── ReportsMapper.java           # Maps Object[] → DTOs, computes category percentages
│   ├── ReportsController.java       # 4 GET endpoints: summary, category, monthly, account
│   ├── ReportsSecurityRules.java    # Authentication enforced via catch-all
│   ├── ReportsFacade.java           # Interface for admin dashboard
│   ├── ReportsFacadeImpl.java       # Facade implementation for system-wide reports
│   └── dto/                         # SummaryResponse, CategoryBreakdownResponse, etc.
│
├── admin/                           # Admin-only operations
│   ├── AdminSecurityRules.java      # /admin/** requires ROLE_ADMIN
│   ├── AdminUserController.java     # Suspend/activate users, reset passwords, update roles
│   ├── AdminUserService.java        # Business logic for admin user operations
│   ├── AdminAccountController.java  # Freeze/unfreeze accounts
│   ├── AdminAccountService.java     # Business logic for admin account operations
│   ├── AdminTransactionController.java # View all transactions (read-only)
│   ├── AdminTransactionService.java # Business logic for admin transaction operations
│   ├── AdminDashboardController.java # Platform-wide statistics
│   ├── AdminDashboardService.java   # Aggregates statistics across all domains via Facades
│   └── dto/                         # DashboardResponse, RoleUpdateRequest, PasswordResetResponse
│
└── FinancetrackerApplication.java   # @SpringBootApplication entry point
```

---

## 🗄 Database Design

### Entity-Relationship Diagram

```mermaid
erDiagram
    USERS {
        bigserial id PK
        varchar first_name
        varchar last_name
        varchar email UK "case-insensitive UNIQUE via LOWER"
        varchar password_hash
        varchar role "USER or ADMIN"
        boolean active "default true"
        timestamptz created_at
        timestamptz updated_at
    }

    ACCOUNT_TYPES {
        bigserial id PK
        varchar name UK "CHECKING SAVINGS INVESTMENT"
    }

    ACCOUNTS {
        bigserial id PK
        bigint user_id FK "users.id ON DELETE CASCADE"
        varchar name
        varchar type FK "account_types.name"
        decimal balance "CHECK balance >= 0"
        varchar currency "ISO 4217, default USD"
        boolean frozen "default false"
        created_at created_at
        updated_at updated_at
    }

    TRANSACTION_TYPES {
        bigserial id PK
        varchar name UK "INCOME EXPENSE"
    }

    TRANSACTION_CATEGORIES {
        bigserial id PK
        varchar name UK "dynamic find-or-create"
    }

    TRANSACTIONS {
        bigserial id PK
        bigint account_id FK "accounts.id ON DELETE CASCADE"
        varchar type FK "transaction_types.name"
        decimal amount "CHECK amount > 0"
        varchar description
        date transaction_date "default CURRENT_DATE"
        varchar category FK "transaction_categories.name nullable"
        created_at created_at
        updated_at updated_at
    }

    USERS ||--o{ ACCOUNTS : "owns"
    ACCOUNT_TYPES ||--o{ ACCOUNTS : "typed by"
    ACCOUNTS ||--o{ TRANSACTIONS : "contains"
    TRANSACTION_TYPES ||--o{ TRANSACTIONS : "typed by"
    TRANSACTION_CATEGORIES ||--o{ TRANSACTIONS : "categorized by"
```

### Migration History

| Version | Description | Purpose |
|---------|-------------|---------|
| **V1** | Create `users` table | Foundation — user accounts with email index |
| **V2** | Case-insensitive email uniqueness | `LOWER(email)` unique index |
| **V3** | Add `role` column, fix email constraint | Role-based access control |
| **V4** | Create `accounts` + `account_types` tables | Financial accounts with FK to users |
| **V5** | Create `transactions` table | Transaction recording with date index |
| **V6** | Add `transaction_types` + `transaction_categories` | 3NF normalization — FK constraints |
| **V7** | FK indexes + trim categories | Performance indexes, data cleanup |
| **V8** | Add `active` column to users | Admin suspend/activate workflow |
| **V9** | Add `frozen` column to accounts | Admin freeze/unfreeze workflow |
| **V10** | Default `transaction_date` | Database-level `CURRENT_DATE` default |

### Design Decisions

- **3NF Reference Tables**: `account_types`, `transaction_types`, `transaction_categories` are separate tables with FK constraints — not just enums in Java code. This enables database-level integrity and future extensibility.
- **Named FK References**: `accounts.type` and `transactions.type` reference `account_types.name` / `transaction_types.name` (not `id`) — keeps queries readable without additional JOINs.
- **Cascade Deletes**: Account deletion cascades to transactions; user deletion cascades to accounts.
- **Immutable Audit Trail**: `created_at` is non-updatable; `updated_at` auto-refreshes via `@PreUpdate`.

---

## 🔒 Security Architecture

### JWT Authentication Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant AC as AuthController
    participant AS as AuthService
    participant JS as JwtService
    participant DB as Database
    participant SF as SecurityFilter

    Note over C,SF: Login Flow
    C->>AC: POST /api/v1/auth/login {email, password}
    AC->>AS: login(request, response)
    AS->>DB: Authenticate email + BCrypt
    AS->>JS: generateAccessToken(user)
    JS-->>AS: Jwt access token (15min)
    AS->>JS: generateRefreshToken(user)
    JS-->>AS: Jwt refresh token (7 days)
    AS->>AS: Set refreshToken cookie (HttpOnly, SameSite=Lax)
    AS-->>AC: JwtResponse {accessToken}
    AC-->>C: 200 OK with accessToken

    Note over C,SF: Authenticated Request
    C->>AC: GET /api/v1/transactions
    AC->>SF: JwtAuthenticationFilter intercepts
    SF->>JS: parseToken(token)
    JS-->>SF: Jwt {userId, email, role, type}
    SF->>DB: Load User by ID from JWT claims
    SF->>SF: Set SecurityContext (UserIdPrincipal)
    SF-->>AC: Authenticated request proceeds
    AC->>AC: @PreAuthorize ownership check
    AC-->>C: 200 OK with transaction data
```

### Security Controls

| Control | Implementation |
|---------|---------------|
| **Stateless Sessions** | `SessionCreationPolicy.STATELESS` — no server-side session |
| **Password Hashing** | BCrypt via `PasswordEncoder` bean |
| **JWT Access Tokens** | 15-minute expiration, carry `userId`, `email`, `role`, `type` claims |
| **JWT Refresh Tokens** | 7-day expiration, stored as HttpOnly cookie with `SameSite=Lax` |
| **Token Type Discrimination** | `type` claim (`"access"` / `"refresh"`) prevents access tokens from being used as refresh tokens |
| **Ownership Enforcement** | `@PreAuthorize("#id == authentication.principal.id()")` via `UserIdPrincipal` record |
| **Role-Based Access** | `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints |
| **Modular Security Rules** | Each domain provides `SecurityRules` bean — `SecurityConfig` collects and applies them |
| **Secret Key Protection** | `@ToString.Exclude` on `JwtConfig.secret`, thread-safe `SecretKey` cache with double-checked locking |
| **CORS** | Externalized via `CorsProperties`, uses `allowedOriginPatterns` for Spring 6+ credentials compatibility |
| **Input Validation** | Jakarta Bean Validation on all DTOs + custom `@Pattern` email regex |
| **LIKE Injection Prevention** | `SearchUtils.escapeLike()` escapes wildcard characters before SQL LIKE |

---

## 📡 API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/login` | `permitAll` | Authenticate with email + password → JWT access token + refresh cookie |
| `POST` | `/register` | `permitAll` | Create account → user data + refresh cookie |
| `POST` | `/refresh` | `permitAll` | Exchange refresh token cookie for new access token |
| `GET` | `/me` | `authenticated` | Get current user profile |
| `POST` | `/logout` | `authenticated` | Clear refresh token cookie |

### Users (`/api/v1/users`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/` | `ADMIN` | Create a new user |
| `GET` | `/` | `ADMIN` | List all users (paginated) |
| `GET` | `/{id}` | `ADMIN or owner` | Get user by ID |
| `PATCH` | `/{id}` | `ADMIN or owner` | Update user (first/last name) |
| `DELETE` | `/{id}` | `ADMIN or owner` | Delete user (returns 204) |
| `PATCH` | `/{id}/change-password` | `ADMIN or owner` | Change password (requires current password) |

### Accounts (`/api/v1/accounts`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/` | `authenticated` | Create account (CHECKING/SAVINGS/INVESTMENT) |
| `GET` | `/mine` | `authenticated` | List my accounts (filterable by name, type, currency) |
| `GET` | `/{id}` | `ADMIN` | Get account by ID |
| `GET` | `/` | `ADMIN` | List all accounts (paginated, filterable) |
| `PATCH` | `/{id}` | `ADMIN` | Update account currency |
| `PATCH` | `/{id}/type` | `ADMIN` | Update account type |
| `DELETE` | `/{id}` | `ADMIN` | Delete account (returns 204, cascades transactions) |

### Transactions (`/api/v1/transactions`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/` | `authenticated` | Create transaction (INCOME/EXPENSE, auto-updates balance) |
| `GET` | `/{id}` | `authenticated` | Get transaction by ID |
| `GET` | `/` | `authenticated` | List transactions (filterable: account, type, category, search, date range) |
| `PATCH` | `/{id}` | `authenticated` | Update transaction (partial, re-reconciles balance if type/amount changed) |
| `DELETE` | `/{id}` | `authenticated` | Delete transaction (reverses balance effect) |

### Reports (`/api/v1/reports`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/summary` | `authenticated` | Income/expense summary with optional date range |
| `GET` | `/by-category` | `authenticated` | Spending breakdown by category with percentages |
| `GET` | `/monthly` | `authenticated` | Month-by-month income vs expense for a year |
| `GET` | `/by-account` | `authenticated` | Per-account income/expense breakdown |

### Admin (`/api/v1/admin`)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/dashboard` | `ADMIN` | Platform-wide statistics (users, accounts, transactions) |
| `GET` | `/users` | `ADMIN` | List all users with search |
| `GET` | `/users/{id}` | `ADMIN` | Get any user by ID |
| `PATCH` | `/users/{id}/suspend` | `ADMIN` | Suspend user account |
| `PATCH` | `/users/{id}/activate` | `ADMIN` | Activate suspended user |
| `PATCH` | `/users/{id}/role` | `ADMIN` | Change user role |
| `POST` | `/users/{id}/reset-password` | `ADMIN` | Reset to random password |
| `GET` | `/accounts` | `ADMIN` | List all accounts with search |
| `GET` | `/accounts/{id}` | `ADMIN` | Get any account by ID |
| `PATCH` | `/accounts/{id}/freeze` | `ADMIN` | Freeze account (blocks transactions) |
| `PATCH` | `/accounts/{id}/unfreeze` | `ADMIN` | Unfreeze account |
| `GET` | `/transactions` | `ADMIN` | List all transactions with search |
| `GET` | `/transactions/{id}` | `ADMIN` | Get any transaction by ID |

### Error Response Format

All errors return a consistent `ErrorDto` structure:

```json
{
  "timestamp": "2025-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Request validation failed",
  "path": "/api/v1/users",
  "fieldErrors": [
    { "field": "email", "message": "must be a well-formed email address" }
  ]
}
```

| Exception | HTTP Status | Scenario |
|-----------|:-----------:|----------|
| `MethodArgumentNotValidException` | 400 | Jakarta validation failures (missing fields, bad format) |
| `HttpMessageNotReadableException` | 400 | Malformed JSON or invalid enum value |
| `MissingServletRequestParameterException` | 400 | Missing required query parameter (e.g., `year`) |
| `MethodArgumentTypeMismatchException` | 400 | Path parameter type mismatch (e.g., string for Long) |
| `InsufficientFundsException` | 400 | EXPENSE transaction exceeds account balance |
| `AccountFrozenException` | 400 | Transaction on a frozen account |
| `PasswordMismatchException` | 400 | Current password verification failed |
| `ResourceNotFoundException` | 404 | Entity not found by ID |
| `DuplicateResourceException` | 409 | Unique constraint violation (e.g., duplicate email) |
| `AccessDeniedException` | 403 | Ownership check failed or missing ROLE_ADMIN |
| `DisabledException` | 401 | Suspended user account |
| `InvalidJwtAuthenticationException` | 401 | Invalid or expired JWT token |

### Business Rules

- **Last Admin Protection**: The last active admin cannot be suspended, demoted, or deleted (`LastAdminActionException`)
- **Account Freeze**: Frozen accounts reject new transactions (`AccountFrozenException`)
- **Balance Integrity**: EXPENSE transactions check sufficient funds before recording
- **Ownership Enforcement**: Users can only access their own accounts, transactions, and profile — enforced via `@PreAuthorize` with `UserIdPrincipal`

---

## ✨ Code Quality & Patterns

### Design Patterns Applied

| Pattern | Where | Why |
|---------|-------|-----|
| **Read/Write Separation** | `@Transactional` (write) vs `@Transactional(readOnly=true)` (read) across all services | Semantic clarity, optimistic locking optimization |
| **Facade** | `UsersFacade`, `AccountsFacade`, `TransactionsFacade`, `ReportsFacade` | Decouple admin domain from user domain internals |
| **Specification** | `AccountSpecification`, `TransactionSpecification` | Dynamic query composition without query explosion |
| **Strategy** | `SecurityRules` functional interface — each domain provides its own | Modular, pluggable security configuration |
| **Value Object** | `Jwt` — immutable, wraps `Claims` + `SecretKey`, provides domain methods | Encapsulates JWT complexity behind a clean API |
| **DTO Mapping** | MapStruct with `@Named` methods and nested `@Mapping` | Zero-reflection, compile-time-safe entity ↔ DTO conversion |
| **Wrapper** | `ApiResponse<T>`, `PagedResponse<T>` | Consistent API response structure across all endpoints |
| **Exception Hierarchy** | `ResourceNotFoundException`, `DuplicateResourceException`, `InsufficientFundsException`, `AccountFrozenException` | Typed exceptions → precise HTTP status codes |
| **Lazy Entity Graphs** | `@NamedEntityGraph("Transaction.withAccount")` | N+1 query prevention with explicit fetch plans |

### SOLID Principles

| Principle | Evidence |
|-----------|----------|
| **S — Single Responsibility** | Each service handles one domain; each controller handles one resource; each exception maps to one HTTP status |
| **O — Open/Closed** | `SecurityRules` interface allows new domains without modifying `SecurityConfig`; `Specification` pattern allows new filters without modifying repositories |
| **L — Liskov Substitution** | `SecurityRules` implementations (`AuthSecurityRules`, `UserSecurityRules`, etc.) are interchangeable — any implementation can be substituted without affecting `SecurityConfig` |
| **I — Interface Segregation** | `SecurityRules` is a single-method functional interface; facade interfaces provide focused cross-domain APIs without exposing repository internals |
| **D — Dependency Inversion** | Services depend on repository interfaces (JpaRepository); controllers depend on service abstractions; `CurrentUserProvider` abstracts auth extraction |

---

## 🧪 Testing Strategy

### Test Pyramid

```mermaid
graph TB
    UNIT["Unit Tests<br/>---<br/>@ExtendWith(MockitoExtension.class)<br/>Services, JwtService, Auth, Filters"]
    SLICE["Slice Tests<br/>---<br/>@WebMvcTest + MockedBeans<br/>All Controllers"]
    INTEG["Integration Tests<br/>---<br/>@DataJpaTest H2<br/>All Repositories"]
    E2E["E2E Tests<br/>---<br/>@SpringBootTest<br/>SecurityConfigTest"]

    UNIT --> SLICE --> INTEG --> E2E

    style UNIT fill:#e8f5e9,stroke:#2e7d32
    style SLICE fill:#e3f2fd,stroke:#1565c0
    style INTEG fill:#fff3e0,stroke:#ef6c00
    style E2E fill:#fce4ec,stroke:#c62828
```

### Testing Conventions

| Convention | Detail |
|------------|--------|
| **Mock Framework** | Mockito with `@MockitoBean` (Spring Boot 3.4+) — never `@MockBean` |
| **Strict Mode** | Per-stub `lenient()` on `@BeforeEach` stubs only — class-level strict mode disabled |
| **MockMvc Filter Skipping** | `@AutoConfigureMockMvc(addFilters = false)` to bypass security filters in controller tests |
| **H2 for Repos** | `@DataJpaTest` with H2 in-memory DB — Flyway disabled in test profile |
| **Security Cleanup** | `@AfterEach` clears `SecurityContextHolder` to prevent test pollution |
| **Native Query Tests** | `TransactionCategoryRepositoryTest` uses `@Disabled` for PostgreSQL-specific `ON CONFLICT` tests |

---

## 🔄 CI/CD Pipeline

### GitHub Actions Workflow

```mermaid
graph LR
    PUSH["Push to main"] --> BUILD["Build and Test"]
    PUSH --> GITLEAKS["Gitleaks Secret Scan"]
    PUSH --> SPOTBUGS["SpotBugs FindSecBugs"]
    PUSH --> DOCKER["Docker Build"]

    PR["Pull Request"] --> BUILD
    PR --> GITLEAKS
    PR --> DOCKER

    BUILD --> REPORTS["Surefire Reports 14-day"]
    SPOTBUGS --> SB_REPORT["SpotBugs Report 14-day"]

    style BUILD fill:#e8f5e9,stroke:#2e7d32
    style GITLEAKS fill:#fff9c4,stroke:#f9a825
    style SPOTBUGS fill:#fce4ec,stroke:#c62828
    style DOCKER fill:#e1f5fe,stroke:#0288d1
```

### Pipeline Behavior

| Event | Build & Test | Gitleaks | SpotBugs | Docker Build |
|-------|:------------:|:--------:|:--------:|:------------:|
| **PR to main** | ✅ | ✅ | ❌ | ✅ |
| **Push to main** | ✅ | ✅ | ✅ | ✅ |

### Local Git Hooks

| Hook | What it does | Bypass |
|------|-------------|--------|
| `pre-commit` | Gitleaks secret scan + `mvn compile` + `mvn test` | `git commit --no-verify` |
| `pre-push` | `mvn compile` + `mvn test` | `git push --no-verify` |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** (`sdk use java 17.0.8-tem`)
- **Maven 3.9+**
- **PostgreSQL** (or use Docker)
- **Gitleaks** (for git hooks)

### Quick Start

```bash
# 1. Activate Java version
source ~/.sdkman/bin/sdkman-init.sh && sdk env

# 2. Create database
createdb financetracker

# 3. Run migrations + start app
mvn spring-boot:run

# 4. Access Swagger UI
open http://localhost:8080/swagger-ui.html

# 5. Run tests
mvn test
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Environment profile (`dev`, `stag`, `prod`) | `dev` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://localhost:5432/financetracker` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | — |
| `SPRING_JWT_SECRET` | JWT signing secret (HMAC-SHA) | — |

---

<div align="center">

**Built with ☕ and clean code principles**

</div>
