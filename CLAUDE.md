# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

This project uses **Gradle 9.3.1** with a Kotlin DSL build file. On Windows use `gradlew.bat`; on Unix use `./gradlew`.

```
# Build
gradlew build

# Run the application
gradlew bootRun

# Run all tests
gradlew test

# Run a single test class
gradlew test --tests "ru.hh.match.infrastructure.mapper.ResumeMapperTest"

# Build JAR
gradlew bootJar

# Clean
gradlew clean

# Docker (all services)
docker-compose up --build
```

## Architecture

**Package root:** `ru.hh.match`
**Spring Boot version:** 4.0.3 | **Java:** 25 (Gradle toolchain)

Clean Architecture with Ports & Adapters:

```
ru.hh.match/
├── domain/          # Entities, Enums, Exceptions, Repository interfaces (JPA only)
├── application/     # Use-case interfaces (port/in), output port interfaces (port/out), Service impls
├── infrastructure/  # Adapters (HH API, Redis, Kafka), Config, Mappers
├── presentation/    # REST Controllers, Response DTOs, GlobalExceptionHandler
└── HhMatchServiceApplication.java
```

**Dependency rule:** `presentation → application → domain` and `infrastructure → application → domain`.

### Key Components
- **ResumeService** — syncs resume from HH API on first visit, caches in Redis
- **VacancyService** — searches vacancies via HH API, deduplicates in DB
- **MatchingService** — creates match pairs, sends to Kafka for LLM scoring
- **KafkaMatchConsumer** — receives match scores, updates DB, pushes SSE events
- **SseController** — manages SSE emitters per session for real-time streaming

### External Dependencies
- **PostgreSQL** — 3 schemas: resume, vacancy, matching (Flyway managed)
- **Redis** — session tokens, caching (resume, match results, seen markers)
- **Kafka** — match-request-topic / match-response-topic (KRaft mode)

### Mock LLM Service
Separate Spring Boot app in `llm-mock/` — consumes match requests, returns random scores.

Configuration is in `src/main/resources/application.yaml`. All values externalized via env vars.

Tests use JUnit 5 + Mockito (unit tests only, no integration tests).

**No Lombok** — standard Java classes for entities, records for DTOs/configs.
