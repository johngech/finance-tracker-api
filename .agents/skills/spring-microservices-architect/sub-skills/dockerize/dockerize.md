# Dockerize Sub-Skill — Docker & Docker Compose

This sub-skill generates Dockerfiles and Docker Compose configurations for the microservices landscape. It handles multiple deployment variants (default, Kafka, partitioned) and ensures all services follow the layered jar pattern.

## Pre-Checks

Before dockerizing, verify:
1. All microservices have been built: `./gradlew build`
2. Each microservice has a `build/libs/*.jar` artifact
3. The current iteration layer has been detected

## Workflows

### Workflow 1: Generate Dockerfiles

For each microservice in `microservices/` and `spring-cloud/`, generate a Dockerfile:

```dockerfile
FROM eclipse-temurin:26-jre as builder
WORKDIR extracted
ADD ./build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:26-jre
WORKDIR application
COPY --from=builder extracted/dependencies/ ./
COPY --from=builder extracted/spring-boot-loader/ ./
COPY --from=builder extracted/snapshot-dependencies/ ./
COPY --from=builder extracted/application/ ./

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
```

**Port overrides:**
- Gateway service: `EXPOSE 8443` (HTTPS)
- Eureka server: `EXPOSE 8761`
- Config server: `EXPOSE 8888`
- Authorization server: `EXPOSE 9999`

### Workflow 2: Generate docker-compose.yml

#### Base Layer (Core Microservices)

Generate one entry per service from `.agents/project.yml` (see [project-config.md](../../references/project-config.md)):

```yaml
services:
  <service-a>:
    build: microservices/<service-a>
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  <service-b>:
    build: microservices/<service-b>
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker

  <composite-service>:
    build: microservices/<composite-service>
    mem_limit: 512m
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

> Replace `<service-a>`, `<service-b>`, `<composite-service>` with your actual service names.

#### Persistence Layer (Add Databases)

Add MongoDB and MySQL services with health checks:

```yaml
  mongodb:
    image: mongo:7.0
    mem_limit: 512m
    ports:
      - "27017:27017"
    command: mongod
    healthcheck:
      test: "mongostat -n 1"
      interval: 5s
      timeout: 2s
      retries: 60

  mysql:
    image: mysql:8.4
    mem_limit: 512m
    ports:
      - "3306:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=rootpwd
      - MYSQL_DATABASE=review-db
      - MYSQL_USER=user
      - MYSQL_PASSWORD=pwd
    healthcheck:
      test: "/usr/bin/mysql --user=user --password=pwd --execute \"SHOW DATABASES;\""
      interval: 5s
      timeout: 2s
      retries: 60
```

Add `depends_on` with `condition: service_healthy` to MongoDB-backed and MySQL-backed services as appropriate.

#### Reactive Layer (Add Messaging)

Add RabbitMQ:

```yaml
  rabbitmq:
    image: rabbitmq:3.13-management
    mem_limit: 512m
    ports:
      - 5672:5672
      - 15672:15672
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 5s
      timeout: 2s
      retries: 60
```

Add `depends_on` → rabbitmq to all microservices.

#### Discovery Layer (Add Eureka)

```yaml
  eureka:
    build: spring-cloud/eureka-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
```

#### Edge-Server Layer (Add Gateway)

```yaml
  gateway:
    build: spring-cloud/gateway
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    ports:
      - "8443:8443"
```

Remove port mapping from composite service (now accessed via gateway).

#### Security Layer (Add Auth Server)

```yaml
  auth-server:
    build: spring-cloud/authorization-server
    mem_limit: 512m
    healthcheck:
      test: ["CMD", "curl", "-fs", "http://localhost:9999/actuator/health"]
      interval: 5s
      timeout: 2s
      retries: 60
```

Add `depends_on` → auth-server to gateway and composite service.

#### Centralized-Config Layer (Add Config Server)

```yaml
  config-server:
    build: spring-cloud/config-server
    mem_limit: 512m
    environment:
      - SPRING_PROFILES_ACTIVE=docker,native
      - ENCRYPT_KEY=${ENCRYPT_KEY}
      - SPRING_SECURITY_USER_NAME=${CONFIG_SERVER_USR}
      - SPRING_SECURITY_USER_PASSWORD=${CONFIG_SERVER_PWD}
    volumes:
      - $PWD/config-repo:/config-repo
```

Add `CONFIG_SERVER_USR` and `CONFIG_SERVER_PWD` env vars to all services. Create `.env` file.

### Workflow 3: Generate docker-compose-kafka.yml

Copy `docker-compose.yml` and modify:
- Do **NOT** include a top-level `version:` key (Compose V2)
- Replace RabbitMQ with Kafka + Zookeeper services
- Change `SPRING_PROFILES_ACTIVE` to include `kafka` profile
- Example: `SPRING_PROFILES_ACTIVE=docker,kafka`

### Workflow 4: Generate docker-compose-partitions.yml

Copy `docker-compose.yml` (no `version:` key) and add:
- Duplicate services with `-p1` suffix for partition instance 1
- Set `SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_0` on originals
- Set `SPRING_PROFILES_ACTIVE=docker,streaming_partitioned,streaming_instance_1` on `-p1` copies

## Build & Test Commands

```bash
# Build all projects
./gradlew build

# Build Docker images
docker compose build

# Start landscape
docker compose up -d

# Run integration tests
./test-em-all.bash

# Stop landscape
docker compose down
```

## Post-Dockerize Checklist

- [ ] Every microservice has a Dockerfile using the layered pattern
- [ ] `docker-compose.yml` includes all services with correct `depends_on`
- [ ] Memory limits set on all services
- [ ] Spring profiles set correctly
- [ ] Health checks on all infrastructure services
- [ ] Port mappings only on edge services
- [ ] `.env` file created (if centralized-config layer includes config server)
- [ ] `docker compose build` succeeds
- [ ] `docker compose up -d` starts all services
- [ ] `./test-em-all.bash` passes
