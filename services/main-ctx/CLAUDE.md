# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests com.rsargsyan.sprite.main_ctx.SomeTestClass

# Run the application
./gradlew bootRun

# Clean
./gradlew clean
```

## Architecture

This is a **Spring Boot 3.5.7 / Java 21** microservice for the Sprite platform — a video thumbnail sprite sheet generation service. It follows **hexagonal architecture (ports & adapters)** organized under `src/main/java/com/rsargsyan/sprite/main_ctx/`:

```
adapters/driving/       ← Inbound: REST controllers, security filters, RabbitMQ workers
adapters/driven/        ← Outbound: repository implementations (JPA)
core/
  app/                  ← Application services, DTOs, domain event listeners
  domain/               ← Aggregates, value objects, entities
  ports/repository/     ← Repository interfaces (contracts between core and adapters)
  exception/            ← Domain exceptions
```

### Request Flow

```
HTTP → Security Filters (JWT or API Key) → Controllers → Application Services → Domain Model
                                                                              → Repository Ports → JPA
                                                          ↓ (async events)
                                                    RabbitMQ Queue → Worker → Video Processing (FFmpeg/ImageMagick) → S3
```

### Authentication

Two supported auth mechanisms, both resolved before controllers execute:

1. **API Key auth** — `X-API-KEY-ID` + `X-API-KEY` headers → `ApiKeyAuthenticationFilter` → `CustomApiKeyAuthenticationProvider` validates against DB
2. **OAuth2 JWT** (Firebase) — Bearer token with `sub` claim + `X-ACCOUNT-ID` header → Spring's OAuth2 resource server

`UserContextHolder` (ThreadLocal) carries the resolved user context through the request. Set by `UserContextInterceptor`, cleared post-request.

### Key Domain Aggregates

- `Account` — root ownership container
- `UserProfile` — user with name and API keys, scoped to Account
- `Principal` — external identity (Firebase `sub` claim), linked to UserProfile
- `ApiKey` — hashed credentials with access tracking, linked to UserProfile
- `ThumbnailsGenerationJob` — job record with status lifecycle, scoped to Account

All aggregates use TSID-based Long primary keys (`@Tsid` from Hypersistence).

### Async Job Processing

Jobs created via REST → `ThumbnailsGenerationJobUpsertEvent` published → `ApplicationEventListener` sends to RabbitMQ → Worker consumes with manual ACK → `VideoThumbnailGenerator` runs FFmpeg + ImageMagick → uploads sprites to S3/Backblaze B2.

### REST Endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/user/signup-external` | None |
| POST | `/user/{userId}/api-key` | JWT or API Key |
| POST | `/thumbnails-generation-job` | JWT or API Key |
| GET | `/error` | None |

### External Dependencies

- **PostgreSQL** — primary datastore
- **RabbitMQ** (CloudAMQP) — async job queue
- **Backblaze B2** (S3-compatible) — sprite/asset storage
- **Firebase** — JWT issuer for OAuth2
- **FFmpeg + ImageMagick** — video processing (invoked via `ProcessBuilder`)

### Environment Variables

```
SPRING_DATASOURCE_URL / USERNAME / PASSWORD
RABBITMQ_HOST / PORT / USERNAME / PASSWORD / VIRTUAL_HOST
S3_ACCESS_KEY_ID / SECRET_ACCESS_KEY / REGION / ENDPOINT / BUCKET
FIREBASE_PROJECT_ID
```
