# financetracker

Spring Boot 3.5.0 / Java 17 / Maven monolith. PostgreSQL + Flyway + JPA + Lombok + REST Docs.

## Quick start

```bash
./mvnw compile                   # compile + Lombok annotation processing
./mvnw test                      # JUnit 5, single test: ./mvnw test -Dtest=ClassName
./mvnw package                   # triggers asciidoctor doc generation
./mvnw spring-boot:run           # requires a running PostgreSQL (configure datasource in application.yaml)
```

## Architecture

- **Package**: `com.marakicode.financetracker`
- **Entrypoint**: `FinancetrackerApplication.java` (plain `@SpringBootApplication`)
- **DB migrations**: Flyway SQL in `src/main/resources/db/migration/` (`V1__*.sql`)
- **REST Docs**: tests use `@AutoConfigureRestDocs` + `MockMvc`; AsciiDoc in `src/main/asciidoc/`, rendered via `asciidoctor-maven-plugin` during `package`
- **Lombok**: enabled via `maven-compiler-plugin` annotation processor paths (IDE must also have Lombok plugin)

## Conventions

- Use `./mvnw` (wrapper), not system `mvn`
- **DDD-style package layout** — code organized by bounded context, NOT by technical layer:
  ```
  com.marakicode.financetracker/
    common/              # Shared: ApiResponse, BaseEntity, GlobalExceptionHandler
    auth/                # Authentication & authorization
    users/               # User management
    accounts/            # Account management
    transactions/        # Transaction management
  ```
  Each domain package is self-contained with a controller at the domain root. Only create `entity/`, `repository/`, `dto/`, `service/` sub-packages when a domain has **more than 2 files** of that type — otherwise, files sit flat at the domain root.
- Tests: `@SpringBootTest` for integration, `@DataJpaTest` for repos, `@WebMvcTest` for controllers
- Flyway: one migration per change, never edit an applied migration
- REST Docs: generate snippets in tests, reference from AsciiDoc `.adoc` files
