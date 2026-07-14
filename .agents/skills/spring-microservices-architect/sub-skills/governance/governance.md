# Governance Sub-Skill — Quality Gates & Compliance

This sub-skill defines nine mandatory quality gates that are validated after every code-generation or modification pass. Each gate has an explicit severity classification and measurable pass criteria.

> `${basePackage}` is a project-configurable placeholder. Substitute your project's base package when checking compliance.

---

## Gate 1 — Project Structure

**Severity:** BLOCKER  
**Pass criteria:** All modules declared in `settings.gradle` exist on disk; no orphan directories under `microservices/` or `spring-cloud/`.

**Check:**
```bash
# Extract expected modules from settings.gradle
grep "include " settings.gradle | tr -d "'" | sed 's/include //' | tr ':' '/' | while read m; do
  [ -d "$m" ] || echo "MISSING: $m"
done
```

---

## Gate 2 — Build Configuration

**Severity:** BLOCKER  
**Pass criteria:** Every `build.gradle` file uses the correct Spring Boot, dependency management, and Java toolchain configuration.

**Required values:**
| Property | Expected |
|----------|----------|
| `org.springframework.boot` plugin | `4.1.0` |
| `io.spring.dependency-management` plugin | `1.1.7` |
| Java toolchain | `java { toolchain { languageVersion = JavaLanguageVersion.of(26) } }` |
| Spring Cloud BOM | `spring-cloud-dependencies:2025.1.2` |

**Check:**
```bash
# Verify Spring Boot version
find . -name "build.gradle" -exec grep -l "version '4.1.0'" {} \;

# Verify Java 26 toolchain (NOT sourceCompatibility)
find . -name "build.gradle" -exec grep -l "languageVersion" {} \;
# Should NOT find sourceCompatibility
find . -name "build.gradle" -exec grep -l "sourceCompatibility" {} \; | grep -v ".gradle/wrapper"
```

**Action on failure:** Update mismatched versions or migrate from `sourceCompatibility` to toolchain syntax.

---

## Gate 3 — Package Naming

**Severity:** CRITICAL  
**Pass criteria:** All Java source packages follow the `${basePackage}.*` convention. No orphan packages outside the base package hierarchy.

**Expected packages (prefix `${basePackage}`):**
| Module | Package |
|--------|---------|
| api | `${basePackage}.api.core.<service>` (one per core service) |
| api | `${basePackage}.api.composite.<domain>` |
| api | `${basePackage}.api.event` |
| api | `${basePackage}.api.exceptions` |
| util | `${basePackage}.util.http` |
| core services | `${basePackage}.microservices.core.<service>` |
| composite services | `${basePackage}.microservices.composite.<domain>` |
| Spring Cloud modules | `${basePackage}.springcloud.<module>` |

> Service names are derived dynamically from `settings.gradle` — there is no fixed list. Any service declared in the project is valid.

**Check:**
```bash
# Find any Java files outside the expected base package
find . -name "*.java" -path "*/src/main/java/*" | while read f; do
  head -1 "$f" | grep -q "^package ${basePackage}\." || echo "BAD PACKAGE: $f"
done
```

---

## Gate 4 — Dockerfile Standards

**Severity:** CRITICAL  
**Pass criteria:** All Dockerfiles use the approved multi-stage build pattern with the correct base image.

**Required base image:** `eclipse-temurin:26-jre`

**Check:**
```bash
find . -name "Dockerfile" -exec grep -l "eclipse-temurin" {} \;
find . -name "Dockerfile" -exec grep "FROM" {} \; | sort -u
```

**Expected pattern:**
```dockerfile
FROM eclipse-temurin:26-jre AS builder
# ...extract layers...

FROM eclipse-temurin:26-jre
# ...copy layers...
```

---

## Gate 5 — Docker Compose Consistency

**Severity:** CRITICAL  
**Pass criteria:** The base `docker-compose.yml` includes all microservices and infrastructure containers with health checks.

**Check:**
```bash
# Verify all services are present
grep "^  [a-z]" compose.yaml | tr -d ':' | sort
# Verify health checks
grep -c "healthcheck" compose.yaml
```

**Required services (after discovery layer):**
- **Core services:** All services declared in `settings.gradle` under `microservices/`
- **Infrastructure:** Databases per service persistence strategy (see [project-config.md](../../references/project-config.md))
- **Reactive:** rabbitmq (or kafka) when messaging layer is present
- **Discovery:** eureka when discovery layer is present
- **Domain extensions:** Only services relevant to the project's current scope

> Validate dynamically: extract service names from `settings.gradle`, then verify each has a matching entry in `docker-compose.yml`. No fixed service list is enforced.

---

## Gate 6 — Test Coverage

**Severity:** MAJOR  
**Pass criteria:** Every microservice module has at least one integration test class. `test-em-all.bash` exits with code 0.

**Check:**
```bash
find . -name "*Test*.java" -path "*/src/test/*" | wc -l
# Should be >= number of microservice modules
```

---

## Gate 7 — API Contract Integrity

**Severity:** MAJOR  
**Pass criteria:** All REST endpoints defined in `api/` interfaces are reachable; response DTOs match the declared fields.

**Check:** Run `test-em-all.bash` which validates endpoints, status codes, and response structure.

---

## Gate 8 — Configuration Consistency

**Severity:** MAJOR  
**Pass criteria:** Every `application.yml` references the correct service name, port, and profiles. No port conflicts between microservices.

**Default port assignments:**

Port assignments are **project-specific**. Check ports against the service registry in `.agents/project.yml` (see [project-config.md](../../references/project-config.md)).

| Port Range | Purpose |
|-----------|--------|
| `7000–7099` | Application microservices (assigned per service in project.yml) |
| `8443` | Gateway (HTTPS) |
| `8761` | Eureka Server |
| `8888` | Config Server |
| `9999` | Authorization Server |

**Validation rule:** Extract each service's `server.port` from `application.yml`, verify no two services share the same port, and verify ports match the project's service registry.

---

## Gate 9 — Event-Driven Consistency

**Severity:** MAJOR (when reactive/messaging layer is present)  
**Pass criteria:** Every `MessageProcessorConfig` consumer handles all `Event.Type` cases. `StreamBridge` publishers use correct topic names.

**Check:**
```bash
# Verify consumer completeness
grep -rn "case CREATE" microservices/ --include="*.java" | wc -l
grep -rn "case DELETE" microservices/ --include="*.java" | wc -l
# Both counts should match

# Verify topic naming
grep -rn "streamBridge.send" microservices/ --include="*.java"
```

---

## Severity Classification

| Level | Impact | Action |
|-------|--------|--------|
| **BLOCKER** | Build / deploy will fail | Fix immediately, no further scaffolding until resolved |
| **CRITICAL** | Runtime failures likely | Fix before next test run |
| **MAJOR** | Feature gaps or maintenance risk | Fix before next layer scaffolding |

---

## Compliance Report Template

After running all gates, produce a report:

```markdown
## Governance Report — [Layer Name]

| Gate | Status | Notes |
|------|--------|-------|
| 1 — Project Structure | ✅ PASS | — |
| 2 — Build Configuration | ⚠️ WARN | <service-a> had 4.0.4 → 4.1.0 fixed |
| 3 — Package Naming | ✅ PASS | — |
| 4 — Dockerfile Standards | ✅ PASS | — |
| 5 — Docker Compose | ✅ PASS | — |
| 6 — Test Coverage | ✅ PASS | 12 test classes found |
| 7 — API Contract | ✅ PASS | test-em-all.bash exit 0 |
| 8 — Configuration | ✅ PASS | — |
| 9 — Event-Driven | ⏭️ SKIP | Messaging layer not yet applied |

**Overall:** PASS — 7/9 gates passed, 1 warning fixed, 1 skipped (not applicable yet)
```
