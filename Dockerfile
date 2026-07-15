# syntax=docker/dockerfile:1
# ==========================================
# FinanceTracker — Multi-stage Dockerfile
# ==========================================
# Requires: Docker Engine >= 23.0 or Docker Desktop >= 4.17 (BuildKit enabled)
# Stage 1: Build with Maven (dependency layer cached by pom.xml)
# Stage 2: Run with JRE only (small, secure image)
#
# Build:  docker build -t financetracker .
# Run:    docker run --rm -p 8080:8080 financetracker
#

# ── Stage 1: Build ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Cache dependency resolution — layer re-runs only when pom.xml changes.
# -DexcludeScope=test skips test dependencies (JUnit, Mockito, H2, etc.)
# which are unused in Docker (tests run in CI and pre-commit hooks).
# This cuts initial dependency download by ~40%.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B  dependency:go-offline -DexcludeScope=test

# Build the fat JAR and rename to a deterministic filename.
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B package -DskipTests \
    && mv target/financetracker-*.jar target/app.jar

# ── Stage 2: Runtime ───────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# curl for Docker HEALTHCHECK; apk (not apt) — Alpine-based image.
# --no-commit keeps the image small; cache is discarded in same layer.
RUN apk add --no-cache curl

# Run as non-root (security best practice).
RUN addgroup -S appuser && adduser -S -G appuser appuser
WORKDIR /app

COPY --from=builder /build/target/app.jar app.jar
RUN chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

# JVM container flags:
#   UseContainerSupport  — respect Docker memory/cpu limits (default in JDK 17, explicit for clarity)
#   MaxRAMPercentage=75  — use 75% of container memory (Docker-friendly, no hardcoded -Xmx)
#   UseG1GC              — low-pause GC for server workloads
#
# Graceful shutdown: Docker sends SIGTERM → JVM exits → Spring lifecycle
# timeout (30s, configured in application.yaml) drains in-flight requests.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:+UseG1GC", \
  "-jar", "app.jar"]
