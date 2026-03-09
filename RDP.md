# RDP — HH Match Service

> **Документ для автоматизированной реализации через Claude Code.**
> Реализовать ТОЛЬКО описанный функционал. Не добавлять ничего сверх спецификации.

---

## 1. ОБЩИЕ СВЕДЕНИЯ О ПРОЕКТЕ

### 1.1 Назначение
Микросервис `hh-match-service` — бэкенд-компонент, который:
1. Получает из Redis OAuth2 access-token пользователя HH по session ID (UUID из cookie, проставляемой внешним Gateway на Go).
2. Загружает первое резюме пользователя через API hh.ru, парсит и сохраняет в PostgreSQL.
3. Ищет релевантные вакансии по данным резюме через API hh.ru, сохраняет в PostgreSQL (с дедупликацией).
4. Отправляет пары (resume, vacancy) в Kafka-топик для расчёта match-score внешним LLM-сервисом.
5. Принимает результаты match-score из Kafka response-топика, сохраняет в БД и стримит клиенту через SSE.

### 1.2 Стек технологий

| Компонент              | Технология                                  |
|------------------------|---------------------------------------------|
| Язык                   | Java 25 (LTS, GA 16 Sep 2025)              |
| Фреймворк              | Spring Boot 4.0.3 (Spring Framework 7.x)    |
| ORM                    | Hibernate 7.1 (через Spring Data JPA)       |
| Миграции БД            | Flyway 11.x                                 |
| Сборка                 | Gradle Kotlin DSL                           |
| СУБД                   | PostgreSQL 16                               |
| Кеш / Сессии           | Redis 7                                     |
| Брокер сообщений       | Apache Kafka (KRaft mode, без Zookeeper)    |
| Контейнеризация        | Docker, Docker Compose                      |
| Тестирование           | JUnit 5, Mockito, AssertJ                   |
| Логирование            | SLF4J + Logback                             |

### 1.3 Артефакт

```
groupId:    ru.hh.match
artifactId: hh-match-service
version:    0.1.0-SNAPSHOT
```

---

## 2. АРХИТЕКТУРА (Clean Architecture)

### 2.1 Слои пакетов

```
src/main/java/ru/hh/match/
├── domain/                    # Слой домена (Entity, Value Objects, интерфейсы репозиториев)
│   ├── model/
│   │   ├── Resume.java                # JPA Entity
│   │   ├── Vacancy.java               # JPA Entity
│   │   ├── MatchResult.java           # JPA Entity
│   │   └── enums/
│   │       └── MatchStatus.java       # PENDING, COMPLETED, FAILED
│   ├── repository/
│   │   ├── ResumeRepository.java      # interface extends JpaRepository
│   │   ├── VacancyRepository.java     # interface extends JpaRepository
│   │   └── MatchResultRepository.java # interface extends JpaRepository
│   └── exception/
│       ├── HhApiException.java
│       ├── SessionNotFoundException.java
│       ├── ResumeNotFoundException.java
│       └── MatchingException.java
│
├── application/               # Слой use-case'ов (бизнес-логика, интерфейсы сервисов)
│   ├── port/
│   │   ├── in/                        # Входные порты (интерфейсы use-case'ов)
│   │   │   ├── SyncResumeUseCase.java
│   │   │   ├── SearchVacanciesUseCase.java
│   │   │   └── StartMatchingUseCase.java
│   │   └── out/                       # Выходные порты (интерфейсы внешних сервисов)
│   │       ├── HhResumePort.java
│   │       ├── HhVacancyPort.java
│   │       ├── SessionPort.java
│   │       ├── MatchRequestPort.java
│   │       └── CachePort.java
│   └── service/
│       ├── ResumeService.java         # implements SyncResumeUseCase
│       ├── VacancyService.java        # implements SearchVacanciesUseCase
│       └── MatchingService.java       # implements StartMatchingUseCase
│
├── infrastructure/            # Слой инфраструктуры (адаптеры)
│   ├── adapter/
│   │   ├── hh/                        # Адаптер HH API
│   │   │   ├── HhApiClient.java       # Общий HTTP-клиент для hh.ru
│   │   │   ├── HhResumeAdapter.java   # implements HhResumePort
│   │   │   ├── HhVacancyAdapter.java  # implements HhVacancyPort
│   │   │   └── dto/                   # DTO для маппинга JSON-ответов HH API
│   │   │       ├── HhResumeListDto.java
│   │   │       ├── HhResumeDto.java
│   │   │       ├── HhVacancySearchDto.java
│   │   │       └── HhVacancyDto.java
│   │   ├── redis/
│   │   │   ├── RedisSessionAdapter.java  # implements SessionPort
│   │   │   └── RedisCacheAdapter.java    # implements CachePort
│   │   └── kafka/
│   │       ├── KafkaMatchProducer.java   # implements MatchRequestPort
│   │       ├── KafkaMatchConsumer.java   # Kafka listener (response topic)
│   │       └── dto/
│   │           ├── MatchRequestMessage.java
│   │           └── MatchResponseMessage.java
│   ├── config/
│   │   ├── AppProperties.java            # @ConfigurationProperties("app")
│   │   ├── RedisConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── KafkaConsumerConfig.java
│   │   ├── RestClientConfig.java
│   │   └── JpaConfig.java
│   └── mapper/
│       ├── ResumeMapper.java             # HhResumeDto -> Resume entity
│       └── VacancyMapper.java            # HhVacancyDto -> Vacancy entity
│
├── presentation/              # Слой представления (REST-контроллеры)
│   ├── controller/
│   │   ├── ResumeController.java
│   │   ├── VacancyController.java
│   │   ├── MatchingController.java
│   │   └── SseController.java
│   ├── dto/
│   │   ├── response/
│   │   │   ├── ApiResponse.java          # Обёртка {success, data, error, timestamp}
│   │   │   ├── ResumeResponse.java
│   │   │   ├── VacancyResponse.java
│   │   │   └── MatchResultResponse.java
│   │   └── request/
│   │       └── VacancySearchRequest.java # (опционально — параметры фильтра)
│   └── handler/
│       └── GlobalExceptionHandler.java   # @RestControllerAdvice
│
└── HhMatchServiceApplication.java       # @SpringBootApplication
```

### 2.2 Правило зависимостей

```
presentation → application → domain
infrastructure → application → domain
```

- `domain` НЕ зависит ни от чего (никаких Spring-аннотаций, кроме JPA на entities).
- `application` зависит только от `domain` и определяет порты (интерфейсы).
- `infrastructure` реализует порты и может зависеть от Spring, Kafka, Redis, HTTP-клиентов.
- `presentation` вызывает только use-case интерфейсы из `application.port.in`.

### 2.3 Ключевые паттерны проектирования

| Паттерн              | Где применяется                                       |
|----------------------|-------------------------------------------------------|
| **Ports & Adapters** | `application.port.in/out` ↔ `infrastructure.adapter`  |
| **Strategy**         | `HhResumePort`, `HhVacancyPort` — сменяемые адаптеры  |
| **Template Method**  | Базовый `AbstractHhAdapter` для общей логики HH API    |
| **Observer**         | SSE-подписки на match-score результаты                 |
| **DTO**              | Все слои общаются через DTO, маппинг явный             |
| **Repository**       | Spring Data JPA репозитории                            |
| **Factory**          | Создание Kafka-сообщений через `MatchMessageFactory`   |

---

## 3. КОНФИГУРАЦИЯ — `application.yaml`

**ВСЕ** настраиваемые параметры выносятся в конфигурацию. Жёстко зашитых значений быть не должно.

```yaml
server:
  port: 8080

spring:
  application:
    name: hh-match-service

  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:hh_match}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate  # Flyway управляет схемой
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    open-in-view: false

  flyway:
    enabled: true
    locations: classpath:db/migration
    schemas: resume, vacancy, matching

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 5000

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: hh-match-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "ru.hh.match.*"
      auto-offset-reset: earliest

# ========== APPLICATION-SPECIFIC ==========
app:
  hh-api:
    base-url: https://api.hh.ru
    user-agent: "HhMatchService/0.1 (contact@example.com)"   # HH API требует User-Agent
    rate-limit:
      max-requests-per-second: 5       # Лимит запросов к HH API
      retry-max-attempts: 3
      retry-delay-ms: 1000
    endpoints:
      resumes: /resumes/mine
      vacancy-search: /vacancies
      vacancy-detail: /vacancies/{id}

  vacancy:
    search-limit: ${VACANCY_SEARCH_LIMIT:50}     # 50 или 100 — из конфига
    relevance-fields:                              # Поля резюме, используемые для поиска
      - title
      - skills

  kafka:
    topics:
      match-request: match-request-topic
      match-response: match-response-topic
    batch-size: ${KAFKA_BATCH_SIZE:1}              # Кол-во вакансий в одном сообщении

  cache:
    match-results-ttl: ${CACHE_MATCH_TTL:3600}     # TTL кэша match-results в секундах
    resume-ttl: ${CACHE_RESUME_TTL:1800}           # TTL кэша резюме в секундах

  sse:
    timeout-ms: 300000                              # 5 минут таймаут SSE-соединения

# ========== LOGGING ==========
logging:
  level:
    root: INFO
    ru.hh.match: DEBUG
    org.hibernate.SQL: DEBUG
  file:
    name: logs/hh-match-service.log
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 30
```

### 3.1 `AppProperties.java`

```java
@ConfigurationProperties(prefix = "app")
public record AppProperties(
    HhApi hhApi,
    VacancyConfig vacancy,
    KafkaTopics kafka,
    CacheConfig cache,
    SseConfig sse
) {
    public record HhApi(
        String baseUrl,
        String userAgent,
        RateLimit rateLimit,
        Endpoints endpoints
    ) {
        public record RateLimit(int maxRequestsPerSecond, int retryMaxAttempts, long retryDelayMs) {}
        public record Endpoints(String resumes, String vacancySearch, String vacancyDetail) {}
    }

    public record VacancyConfig(int searchLimit, List<String> relevanceFields) {}
    public record KafkaTopics(Topics topics, int batchSize) {
        public record Topics(String matchRequest, String matchResponse) {}
    }
    public record CacheConfig(long matchResultsTtl, long resumeTtl) {}
    public record SseConfig(long timeoutMs) {}
}
```

---

## 4. МОДЕЛЬ ДАННЫХ

### 4.1 PostgreSQL Schemas

Три отдельных schema: `resume`, `vacancy`, `matching`.

### 4.2 Flyway-миграции

Расположение: `src/main/resources/db/migration/`

#### `V1__create_schemas.sql`

```sql
CREATE SCHEMA IF NOT EXISTS resume;
CREATE SCHEMA IF NOT EXISTS vacancy;
CREATE SCHEMA IF NOT EXISTS matching;
```

#### `V2__create_resume_table.sql`

```sql
CREATE TABLE resume.resumes (
    id              BIGSERIAL PRIMARY KEY,
    hh_resume_id    VARCHAR(64)  NOT NULL UNIQUE,
    session_id      UUID         NOT NULL,
    grade           VARCHAR(64)  NOT NULL DEFAULT '',
    job_title       VARCHAR(512) NOT NULL DEFAULT '',
    location        VARCHAR(256) NOT NULL DEFAULT '',
    salary_val      INTEGER      NOT NULL DEFAULT 0,
    salary_curr     VARCHAR(16)  NOT NULL DEFAULT 'RUB',
    skills_res      TEXT         NOT NULL DEFAULT '',
    about_me        TEXT         NOT NULL DEFAULT '',
    exp_count       INTEGER      NOT NULL DEFAULT 0,
    exp_text        TEXT         NOT NULL DEFAULT '',
    edu_uni         VARCHAR(512) NOT NULL DEFAULT '',
    edu_year        VARCHAR(32)  NOT NULL DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resumes_session_id ON resume.resumes(session_id);
CREATE INDEX idx_resumes_hh_resume_id ON resume.resumes(hh_resume_id);
```

#### `V3__create_vacancy_table.sql`

```sql
CREATE TABLE vacancy.vacancies (
    id              BIGSERIAL PRIMARY KEY,
    hh_vacancy_id   VARCHAR(64)  NOT NULL UNIQUE,
    target_role     VARCHAR(512) NOT NULL DEFAULT '',
    job_title       VARCHAR(512) NOT NULL DEFAULT '',
    experience      VARCHAR(256) NOT NULL DEFAULT '',
    grade           VARCHAR(64)  NOT NULL DEFAULT '',
    skills_vac      TEXT         NOT NULL DEFAULT '',
    vacancy_text    TEXT         NOT NULL DEFAULT '',
    salary          VARCHAR(256) NOT NULL DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vacancies_hh_vacancy_id ON vacancy.vacancies(hh_vacancy_id);
```

#### `V4__create_match_result_table.sql`

```sql
CREATE TABLE matching.match_results (
    id              BIGSERIAL PRIMARY KEY,
    resume_id       BIGINT       NOT NULL,
    vacancy_id      BIGINT       NOT NULL,
    session_id      UUID         NOT NULL,
    score           DOUBLE PRECISION,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_match_resume  FOREIGN KEY (resume_id)  REFERENCES resume.resumes(id),
    CONSTRAINT fk_match_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancy.vacancies(id),
    CONSTRAINT uq_resume_vacancy UNIQUE (resume_id, vacancy_id)
);

CREATE INDEX idx_match_results_session_id ON matching.match_results(session_id);
CREATE INDEX idx_match_results_status ON matching.match_results(status);
```

### 4.3 JPA Entities

#### `Resume.java`

```java
@Entity
@Table(name = "resumes", schema = "resume")
public class Resume {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hh_resume_id", nullable = false, unique = true)
    private String hhResumeId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    private String grade = "";
    private String jobTitle = "";
    private String location = "";
    private Integer salaryVal = 0;
    private String salaryCurr = "RUB";

    @Column(columnDefinition = "TEXT")
    private String skillsRes = "";

    @Column(columnDefinition = "TEXT")
    private String aboutMe = "";

    private Integer expCount = 0;

    @Column(columnDefinition = "TEXT")
    private String expText = "";

    private String eduUni = "";
    private String eduYear = "";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Конструкторы, геттеры, сеттеры
    // @PrePersist, @PreUpdate для timestamps
}
```

#### `Vacancy.java`

```java
@Entity
@Table(name = "vacancies", schema = "vacancy")
public class Vacancy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hh_vacancy_id", nullable = false, unique = true)
    private String hhVacancyId;

    private String targetRole = "";
    private String jobTitle = "";
    private String experience = "";
    private String grade = "";

    @Column(columnDefinition = "TEXT")
    private String skillsVac = "";

    @Column(columnDefinition = "TEXT")
    private String vacancyText = "";

    private String salary = "";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### `MatchResult.java`

```java
@Entity
@Table(name = "match_results", schema = "matching")
public class MatchResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacancy_id", nullable = false)
    private Vacancy vacancy;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    private Double score;

    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.PENDING;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

## 5. REDIS

### 5.1 Структура данных в Redis

| Ключ                                      | Значение              | TTL            | Описание                                           |
|-------------------------------------------|-----------------------|----------------|-----------------------------------------------------|
| `session:{sessionId}`                     | HH access_token       | Управляет Gateway | Устанавливается Gateway. Читается нашим сервисом   |
| `cache:match:{sessionId}`                 | JSON list MatchResult | `app.cache.match-results-ttl` | Кешированные результаты матчинга для быстрой отдачи |
| `cache:resume:{sessionId}`                | JSON Resume           | `app.cache.resume-ttl`        | Кеш резюме пользователя                            |
| `session:seen:{sessionId}`                | "1"                   | Совпадает с TTL сессии        | Маркер «первый заход обработан»                    |

### 5.2 Определение первого захода

При каждом входящем запросе:
1. Проверить наличие ключа `session:seen:{sessionId}` в Redis.
2. Если ключ отсутствует — это первый заход: выполнить `SETNX session:seen:{sessionId} "1"`, загрузить/обновить резюме с HH.
3. Если ключ существует — резюме уже актуально, пропускаем синхронизацию.

---

## 6. БИЗНЕС-ЛОГИКА (USE CASES)

### 6.1 Use Case: SyncResumeUseCase

**Триггер:** Первый запрос с новым session ID (маркер `session:seen` отсутствует).

**Алгоритм:**
1. Извлечь `sessionId` из cookie `session_id`.
2. Получить `accessToken` из Redis по ключу `session:{sessionId}`.
3. Если accessToken нет → бросить `SessionNotFoundException` → 401.
4. Вызвать HH API `GET /resumes/mine` с Bearer-токеном.
5. Взять первое резюме из списка (`items[0]`).
6. Вызвать HH API `GET /resumes/{id}` для получения полного резюме.
7. Смаппить JSON-ответ в `Resume` entity (маппинг полей — см. секцию 6.4).
8. Сохранить / обновить (upsert по `hh_resume_id`) в PostgreSQL.
9. Закешировать в Redis (`cache:resume:{sessionId}`).
10. Установить маркер `session:seen:{sessionId}`.

### 6.2 Use Case: SearchVacanciesUseCase

**Триггер:** `GET /api/vacancies`

**Алгоритм:**
1. Извлечь `sessionId`, получить `accessToken`.
2. Загрузить резюме пользователя (из кеша или БД).
3. Сформировать поисковый запрос для HH API из полей резюме: `job_title` → параметр `text`, `skills_res` → добавить к `text`.
4. Вызвать HH API `GET /vacancies?text={query}&per_page={app.vacancy.search-limit}`.
5. Для каждой вакансии из ответа:
   - Проверить наличие `hh_vacancy_id` в БД.
   - Если отсутствует → смаппить и сохранить.
   - Если существует → использовать существующую.
6. Вернуть список вакансий.

### 6.3 Use Case: StartMatchingUseCase

**Триггер:** `POST /api/matching/start`

**Алгоритм:**
1. Извлечь `sessionId`, загрузить резюме пользователя.
2. Загрузить список вакансий, связанных с текущей сессией (из последнего поиска).
3. Для каждой вакансии:
   a. Проверить, есть ли уже `MatchResult` для пары `(resume_id, vacancy_id)` со статусом `COMPLETED`.
   b. Если есть и резюме не менялось — пропустить.
   c. Если нет или резюме обновилось — создать/обновить `MatchResult` со статусом `PENDING`.
   d. Сформировать `MatchRequestMessage` и отправить в Kafka-топик `match-request-topic`.
4. Количество вакансий в одном Kafka-сообщении определяется параметром `app.kafka.batch-size`.
5. Вернуть `202 Accepted` с количеством отправленных запросов.

### 6.4 Маппинг полей HH API → Entity

#### Resume (HH API → Resume Entity)

| Поле Entity   | Источник из HH JSON                                                          |
|---------------|-------------------------------------------------------------------------------|
| hhResumeId    | `id`                                                                          |
| grade         | Определить из `experience` массива (количество лет → Junior/Middle/Senior)    |
| jobTitle      | `title`                                                                       |
| location      | `area.name`                                                                   |
| salaryVal     | `salary.amount` (если есть)                                                   |
| salaryCurr    | `salary.currency` (если есть, иначе "RUB")                                   |
| skillsRes     | `skill_set` (массив → строка через запятую)                                  |
| aboutMe       | `skills` (текстовое поле «О себе» в HH API)                                 |
| expCount      | `total_experience.months` / 12 (округлить)                                    |
| expText       | Конкатенация `experience[].position + " в " + experience[].company`          |
| eduUni        | `education.primary[0].name` (первый вуз)                                     |
| eduYear       | `education.primary[0].year` (год окончания)                                  |

#### Vacancy (HH API → Vacancy Entity)

| Поле Entity   | Источник из HH JSON                                                          |
|---------------|-------------------------------------------------------------------------------|
| hhVacancyId   | `id`                                                                          |
| targetRole    | `professional_roles[0].name` (если есть)                                     |
| jobTitle      | `name`                                                                        |
| experience    | `experience.name`                                                             |
| grade         | Вывести из `experience.id` (noExperience→Junior, between1And3→Middle, etc.)  |
| skillsVac     | `key_skills` (массив → строка через запятую)                                 |
| vacancyText   | `description` (HTML → plain text, strip tags)                                 |
| salary        | Форматировать как строку: `"от X до Y CUR"` из `salary` объекта             |

---

## 7. KAFKA

### 7.1 Топики

| Топик                  | Producer          | Consumer          | Партиции | Описание                           |
|------------------------|-------------------|-------------------|----------|-------------------------------------|
| `match-request-topic`  | hh-match-service  | LLM-сервис (mock) | 3        | Запросы на расчёт match-score       |
| `match-response-topic` | LLM-сервис (mock) | hh-match-service  | 3        | Результаты match-score              |

### 7.2 Формат сообщений

#### `MatchRequestMessage` (в `match-request-topic`)

```json
{
  "correlationId": "uuid-string",
  "sessionId": "uuid-string",
  "resumeId": 123,
  "vacancyId": 456,
  "resume": {
    "resume_id": 123,
    "grade": "Middle",
    "job_title": "Java Developer",
    "location": "Москва",
    "salary_val": 200000,
    "salary_curr": "RUB",
    "skills_res": "Java, Spring, Kafka",
    "about_me": "...",
    "exp_count": 5,
    "exp_text": "...",
    "edu_uni": "МГУ",
    "edu_year": "2019"
  },
  "vacancy": {
    "vacancy_id": 456,
    "target_role": "Backend Developer",
    "job_title": "Senior Java Developer",
    "experience": "3-6 лет",
    "grade": "Senior",
    "skills_vac": "Java, Spring Boot, Microservices",
    "vacancy_text": "...",
    "salary": "от 250000 до 350000 RUR"
  }
}
```

#### `MatchResponseMessage` (из `match-response-topic`)

```json
{
  "correlationId": "uuid-string",
  "sessionId": "uuid-string",
  "resumeId": 123,
  "vacancyId": 456,
  "score": 0.85,
  "status": "COMPLETED"
}
```

**Ключ сообщения (Kafka key):** `sessionId` — обеспечивает ordering per session.

### 7.3 Kafka Consumer (response)

При получении `MatchResponseMessage`:
1. Найти `MatchResult` по `(resumeId, vacancyId)` в БД.
2. Обновить `score` и `status`.
3. Обновить кеш `cache:match:{sessionId}` в Redis.
4. Отправить событие через SSE-эмиттер (если клиент подписан).

---

## 8. SSE (Server-Sent Events)

### 8.1 Эндпоинт

```
GET /api/matching/stream?sessionId={sessionId}
```

Возвращает `text/event-stream`.

### 8.2 Механизм

1. Контроллер создаёт `SseEmitter` с таймаутом `app.sse.timeout-ms`.
2. `SseEmitter` регистрируется в `SseEmitterRegistry` (in-memory `ConcurrentHashMap<UUID, SseEmitter>`).
3. Когда `KafkaMatchConsumer` получает ответ — ищет эмиттер по `sessionId` и отправляет событие.
4. Если эмиттер не найден (пользователь отключился) — событие всё равно сохраняется в БД и кеше.
5. При завершении всех match-result для данной сессии — отправить событие `complete` и закрыть эмиттер.

### 8.3 Формат SSE-события

```
event: match-result
data: {"vacancyId": 456, "jobTitle": "Senior Java Developer", "score": 0.85}

event: complete
data: {"total": 50, "completed": 50}
```

---

## 9. REST API

### 9.1 Эндпоинты

Все эндпоинты ожидают cookie `session_id` (UUID).

| Метод | Путь                       | Описание                                  | Ответ      |
|-------|----------------------------|-------------------------------------------|------------|
| GET   | `/api/resume`              | Получить/синхронизировать резюме           | 200 + JSON |
| GET   | `/api/vacancies`           | Поиск релевантных вакансий                 | 200 + JSON |
| POST  | `/api/matching/start`      | Запустить расчёт match-score               | 202        |
| GET   | `/api/matching/results`    | Получить текущие результаты (из кеша/БД)   | 200 + JSON |
| GET   | `/api/matching/stream`     | SSE-стрим match-score по мере поступления  | SSE stream |
| GET   | `/actuator/health`         | Health-check (Spring Actuator)             | 200        |

### 9.2 Формат ответа

```java
public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }
    public static <T> ApiResponse<T> fail(String error) {
        return new ApiResponse<>(false, null, error, LocalDateTime.now());
    }
}
```

### 9.3 Обработка ошибок

`GlobalExceptionHandler` (`@RestControllerAdvice`) ловит ВСЕ исключения:

| Exception                  | HTTP Status | Описание                                   |
|----------------------------|-------------|---------------------------------------------|
| SessionNotFoundException   | 401         | session_id отсутствует или токен не найден  |
| ResumeNotFoundException    | 404         | У пользователя нет резюме на HH             |
| HhApiException             | 502         | Ошибка при обращении к HH API               |
| MatchingException          | 500         | Ошибка в процессе матчинга                  |
| RateLimitException         | 429         | Превышен rate limit HH API (проксируем)     |
| Exception (fallback)       | 500         | Неизвестная ошибка — залогировать stacktrace |

Каждая ошибка логируется на уровне `ERROR` с полным стектрейсом.

---

## 10. HH API ИНТЕГРАЦИЯ

### 10.1 HTTP-клиент

Использовать `RestClient` (Spring Framework 7). Настройка через `RestClientConfig`.

### 10.2 Обязательные заголовки

```
Authorization: Bearer {accessToken}
User-Agent: {app.hh-api.user-agent}
```

HH API **требует** User-Agent. Без него — 403.

### 10.3 Rate Limiting

- Реализовать через `Semaphore` или `RateLimiter` (самописный на основе token bucket).
- Параметры из конфига: `app.hh-api.rate-limit.max-requests-per-second`.
- При получении 429 от HH — retry с задержкой `app.hh-api.rate-limit.retry-delay-ms`, максимум `retry-max-attempts` попыток.
- При исчерпании попыток — бросить `HhApiException`.

### 10.4 Основные эндпоинты HH API

| Наш метод        | HH endpoint              | Параметры                                                   |
|------------------|--------------------------|--------------------------------------------------------------|
| Список резюме     | `GET /resumes/mine`      | —                                                            |
| Полное резюме     | `GET /resumes/{id}`      | —                                                            |
| Поиск вакансий    | `GET /vacancies`         | `text`, `per_page`, `page`, `order_by=relevance`             |

---

## 11. ЛОГИРОВАНИЕ

### 11.1 Стратегия

- **Общий лог:** `logs/hh-match-service.log` — все уровни от INFO.
- **Отдельные файлы** через Logback appenders:
  - `logs/error.log` — только ERROR.
  - `logs/hh-api.log` — логи обращений к HH API (запрос, статус ответа, время).
  - `logs/kafka.log` — логи Kafka producer/consumer.

### 11.2 Что логировать

- Каждый входящий HTTP-запрос: метод, путь, sessionId (маскировать последние 4 символа).
- Каждый исходящий запрос к HH API: URL, статус ответа, время выполнения.
- Каждое отправленное/полученное Kafka-сообщение: topicName, correlationId.
- Все exceptions: полный stacktrace на уровне ERROR.
- Бизнес-события: «Резюме синхронизировано», «Найдено N вакансий», «Матчинг запущен для N пар».

### 11.3 Logback конфигурация

Файл `src/main/resources/logback-spring.xml` — настроить appenders для каждого файла + консольный output.

---

## 12. DOCKER

### 12.1 `Dockerfile` (multi-stage build)

```dockerfile
# Stage 1: Build
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 12.2 `docker-compose.yaml`

```yaml
version: "3.9"

services:
  # =================== INFRASTRUCTURE ===================
  postgres:
    image: postgres:16-alpine
    container_name: hh-match-postgres
    environment:
      POSTGRES_DB: hh_match
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    container_name: hh-match-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

  kafka:
    image: apache/kafka:3.9.0
    container_name: hh-match-kafka
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_LOG_DIRS: /tmp/kraft-combined-logs
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    ports:
      - "9092:9092"
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092 > /dev/null 2>&1"]
      interval: 10s
      timeout: 10s
      retries: 15

  # =================== APPLICATION ===================
  hh-match-service:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: hh-match-service
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: hh_match
      DB_USER: postgres
      DB_PASSWORD: postgres
      REDIS_HOST: redis
      REDIS_PORT: 6379
      KAFKA_BOOTSTRAP: kafka:9092
      VACANCY_SEARCH_LIMIT: 50
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy

  # =================== MOCK LLM SERVICE ===================
  llm-mock-service:
    build:
      context: ./llm-mock
      dockerfile: Dockerfile
    container_name: llm-mock-service
    environment:
      KAFKA_BOOTSTRAP: kafka:9092
      REQUEST_TOPIC: match-request-topic
      RESPONSE_TOPIC: match-response-topic
    depends_on:
      kafka:
        condition: service_healthy

volumes:
  postgres_data:
```

### 12.3 Mock LLM Service

Отдельное мини-приложение в папке `llm-mock/`. Реализовать на Spring Boot (минимальный).

**Функционал:**
1. Слушает Kafka-топик `match-request-topic`.
2. Для каждого сообщения генерирует случайный `score` от 0.0 до 1.0 с задержкой 500–2000 мс (имитация LLM).
3. Отправляет `MatchResponseMessage` в `match-response-topic`.

---

## 13. ТЕСТИРОВАНИЕ

### 13.1 Стратегия

Только **unit-тесты** с JUnit 5 + Mockito. Без Testcontainers / интеграционных тестов.

### 13.2 Что тестировать

| Класс                     | Что проверяем                                                        | Моки                                    |
|---------------------------|----------------------------------------------------------------------|-----------------------------------------|
| `ResumeService`           | Логика синхронизации, обработка первого захода, upsert               | HhResumePort, SessionPort, Repository   |
| `VacancyService`          | Поиск, дедупликация, маппинг                                        | HhVacancyPort, Repository               |
| `MatchingService`         | Формирование пар, отправка в Kafka, пропуск существующих             | MatchRequestPort, Repositories           |
| `KafkaMatchConsumer`      | Обработка response, обновление статуса, отправка SSE                 | Repository, SseEmitterRegistry           |
| `ResumeMapper`            | Корректность маппинга JSON → Entity (все поля)                      | —                                        |
| `VacancyMapper`           | Корректность маппинга JSON → Entity (все поля)                      | —                                        |
| `HhResumeAdapter`         | Формирование запросов, парсинг ответов, обработка ошибок            | RestClient (mocked)                      |
| `HhVacancyAdapter`        | Формирование поисковых запросов, парсинг, rate limit                | RestClient (mocked)                      |
| `GlobalExceptionHandler`  | Корректные HTTP-коды и тело ответа для каждого типа исключений       | —                                        |
| `ResumeController`        | Вызов use-case, формат ответа                                       | SyncResumeUseCase                        |
| `VacancyController`       | Вызов use-case, формат ответа                                       | SearchVacanciesUseCase                   |
| `MatchingController`      | Вызов use-case, SSE-логика                                          | StartMatchingUseCase, SseEmitterRegistry |

### 13.3 Пример теста

```java
@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private HhResumePort hhResumePort;
    @Mock private SessionPort sessionPort;
    @Mock private ResumeRepository resumeRepository;
    @Mock private CachePort cachePort;

    @InjectMocks private ResumeService resumeService;

    @Test
    void syncResume_firstVisit_shouldFetchAndSave() {
        UUID sessionId = UUID.randomUUID();
        String accessToken = "test-token";

        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.of(accessToken));
        when(cachePort.exists("session:seen:" + sessionId)).thenReturn(false);
        when(hhResumePort.fetchFirstResume(accessToken)).thenReturn(testResumeDto());
        when(resumeRepository.findByHhResumeId(any())).thenReturn(Optional.empty());

        Resume result = resumeService.syncResume(sessionId);

        assertThat(result.getJobTitle()).isEqualTo("Java Developer");
        verify(resumeRepository).save(any(Resume.class));
        verify(cachePort).set(eq("session:seen:" + sessionId), any(), anyLong());
    }

    @Test
    void syncResume_noToken_shouldThrow() {
        UUID sessionId = UUID.randomUUID();
        when(sessionPort.getAccessToken(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.syncResume(sessionId))
            .isInstanceOf(SessionNotFoundException.class);
    }
}
```

### 13.4 Расположение тестов

```
src/test/java/ru/hh/match/
├── application/service/
│   ├── ResumeServiceTest.java
│   ├── VacancyServiceTest.java
│   └── MatchingServiceTest.java
├── infrastructure/adapter/
│   ├── hh/
│   │   ├── HhResumeAdapterTest.java
│   │   └── HhVacancyAdapterTest.java
│   └── kafka/
│       └── KafkaMatchConsumerTest.java
├── infrastructure/mapper/
│   ├── ResumeMapperTest.java
│   └── VacancyMapperTest.java
└── presentation/
    ├── controller/
    │   ├── ResumeControllerTest.java
    │   ├── VacancyControllerTest.java
    │   └── MatchingControllerTest.java
    └── handler/
        └── GlobalExceptionHandlerTest.java
```

---

## 14. GRADLE BUILD (`build.gradle.kts`)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "ru.hh.match"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.kafka:spring-kafka")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // JSON
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Logging
    implementation("ch.qos.logback:logback-classic")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.assertj:assertj-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

## 15. СТРУКТУРА ФАЙЛОВ ПРОЕКТА (ПОЛНАЯ)

```
hh-match-service/
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── Dockerfile
├── docker-compose.yaml
├── llm-mock/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── Dockerfile
│   └── src/main/java/ru/hh/match/llmmock/
│       ├── LlmMockApplication.java
│       ├── KafkaMockConsumer.java
│       ├── KafkaMockProducer.java
│       └── application.yaml
├── src/
│   ├── main/
│   │   ├── java/ru/hh/match/
│   │   │   ├── HhMatchServiceApplication.java
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   ├── infrastructure/
│   │   │   └── presentation/
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── logback-spring.xml
│   │       └── db/migration/
│   │           ├── V1__create_schemas.sql
│   │           ├── V2__create_resume_table.sql
│   │           ├── V3__create_vacancy_table.sql
│   │           └── V4__create_match_result_table.sql
│   └── test/
│       └── java/ru/hh/match/
│           ├── application/service/
│           ├── infrastructure/
│           └── presentation/
```

---

## 16. ПОРЯДОК РЕАЛИЗАЦИИ

> Claude Code должен реализовывать проект строго в указанном порядке.

1. **Инициализация проекта:** `build.gradle.kts`, `settings.gradle.kts`, `application.yaml`, `Dockerfile`, `docker-compose.yaml`.
2. **Domain layer:** Entities, Enums, Exceptions, Repository interfaces.
3. **Flyway-миграции:** Все SQL-файлы.
4. **Application layer:** Port interfaces (in/out), Service implementations.
5. **Infrastructure layer:** Config classes (`AppProperties`, Redis, Kafka, RestClient, JPA configs).
6. **Infrastructure adapters:** Redis adapters, HH API adapters с DTOs и Mappers.
7. **Kafka:** Producer, Consumer, DTO messages.
8. **SSE:** `SseEmitterRegistry`, `SseController`.
9. **Presentation layer:** Controllers, Response DTOs, GlobalExceptionHandler.
10. **Logback:** `logback-spring.xml`.
11. **Mock LLM Service:** Отдельный модуль `llm-mock/`.
12. **Unit-тесты:** Все тесты из секции 13.
13. **Финальная проверка:** `docker-compose up --build`, убедиться что всё стартует, health-check проходит.

---

## 17. ОГРАНИЧЕНИЯ И ЗАПРЕТЫ

- **НЕ** реализовывать Gateway.
- **НЕ** реализовывать фронтенд.
- **НЕ** реализовывать реальный LLM-сервис (только mock).
- **НЕ** добавлять Swagger / OpenAPI.
- **НЕ** добавлять Spring Security (авторизация — ответственность Gateway).
- **НЕ** добавлять Prometheus / Micrometer (кроме Spring Actuator health).
- **НЕ** использовать Lombok (Java 25 records достаточно для DTO; entities — стандартные классы).
- **НЕ** использовать `spring.jpa.hibernate.ddl-auto: create/update` — только Flyway.
- **НЕ** делать интеграционные тесты и Testcontainers.
- **НЕ** хардкодить URL, пароли, лимиты — всё в `application.yaml`.

---

## 18. КРИТЕРИИ ГОТОВНОСТИ

- [ ] `docker-compose up --build` запускает все 5 сервисов (Postgres, Redis, Kafka, App, LLM Mock).
- [ ] Health-check `/actuator/health` возвращает 200.
- [ ] Flyway-миграции проходят при старте.
- [ ] Все unit-тесты проходят (`./gradlew test`).
- [ ] Сборка успешна (`./gradlew bootJar`).
- [ ] Нет жёстко зашитых значений — всё в конфиге.
- [ ] Каждый публичный класс имеет интерфейс или является реализацией интерфейса (кроме entities, DTOs, configs).
- [ ] Все exceptions логируются на уровне ERROR.
