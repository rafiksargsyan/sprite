# Sprite

Generate video thumbnail sprite sheets from a URL.

Sprite is a self-hostable service that takes a video URL and produces sprite sheet images + WebVTT files ready to drop into any video player's thumbnail preview. You configure your output formats once (as a *job spec*), then submit jobs via a REST API or the web dashboard.

---

## Features

- **Multiple output formats** — WebP, AVIF, JPG, and Blurhash in a single job
- **Configurable sprite grids** — set rows, columns, resolution, and interval per format
- **WebVTT output** — every job produces a `.vtt` file alongside the sprites, compatible with Video.js, Plyr, Shaka Player, and any player that supports the WebVTT thumbnail spec
- **Async job processing** — jobs are queued and processed asynchronously with automatic retries and heartbeat tracking
- **Job cancellation** — cancel in-progress jobs via the API or dashboard; running FFmpeg processes are killed within seconds
- **In-browser preview** — scrub through generated thumbnails directly in the dashboard before downloading
- **REST API + API key auth** — integrate from your backend without OAuth; Firebase JWT also supported
- **Job specs** — define named, reusable output configurations and reference them when submitting jobs

---

## Architecture

### Backend — `services/main-ctx`

Spring Boot 3 / Java 21 service following **hexagonal architecture (ports & adapters)** with **Domain-Driven Design** principles.

```
adapters/
  driving/        ← Inbound: REST controllers, security filters, RabbitMQ worker
  driven/         ← Outbound: JPA repository implementations
core/
  app/            ← Application services, domain event listeners
  domain/         ← Aggregates, entities, value objects
  ports/          ← Repository interfaces (contracts between core and adapters)
  exception/      ← Domain exceptions
```

**Key domain aggregates:**
- `Account` — root ownership container
- `UserProfile` — user with name and API keys, scoped to an account
- `ThumbnailsGenerationJob` — job record with a well-defined status lifecycle (`SUBMITTED → QUEUED → RECEIVED → IN_PROGRESS → SUCCESS / FAILURE / CANCELLED`)

All aggregates use TSID-based Long primary keys (collision-resistant, sortable, no UUID overhead).

**Async job flow:**

```
POST /thumbnails-generation-job
  → Application service creates job (SUBMITTED)
  → Domain event published
  → ApplicationEventListener sends message to RabbitMQ
  → Worker consumes with manual ACK
  → VideoThumbnailGenerator runs FFmpeg (frame extraction + sprite tiling)
  → Sprites uploaded to Backblaze B2
  → Job marked SUCCESS
```

Retries, heartbeat timeouts, and stuck-job reaping are all handled server-side. Each JVM instance tracks its own active FFmpeg processes and polls for cancellation requests every few seconds.

**Authentication** — two mechanisms, both resolved before controllers execute:
- API Key — `X-API-KEY-ID` + `X-API-KEY` headers, validated against the database
- Firebase JWT — Bearer token with `X-ACCOUNT-ID` header, validated by Spring's OAuth2 resource server

### Frontend — `web-ui`

React 19 / TypeScript SPA built with Vite and Material UI.

- `pages/` — one file per route: Jobs, JobSpecs, ApiKeys, Dashboard
- `api/` — thin fetch wrappers; all authenticated requests attach a fresh Firebase ID token + `X-ACCOUNT-ID`
- `types/api.types.ts` — single source of truth for all request/response shapes

### Infrastructure

| Concern | Technology |
|---|---|
| Primary datastore | PostgreSQL |
| Async job queue | RabbitMQ (CloudAMQP) |
| Sprite/asset storage | Backblaze B2 (S3-compatible) |
| Auth provider | Firebase |
| Video processing | FFmpeg (invoked via `ProcessBuilder`) |

---

## Getting Started

### Prerequisites

- Java 21
- Node.js + pnpm
- Docker (for local PostgreSQL and RabbitMQ)
- FFmpeg installed on the host running the backend

### Backend

```bash
# Start dependencies
docker compose up -d postgres rabbitmq

# Configure environment (copy and fill in values)
cp services/main-ctx/.env.example services/main-ctx/.env

# Run
cd services/main-ctx
./gradlew bootRun
```

**Required environment variables:**

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD

RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VIRTUAL_HOST

S3_ACCESS_KEY_ID
S3_SECRET_ACCESS_KEY
S3_REGION
S3_ENDPOINT
S3_BUCKET

FIREBASE_PROJECT_ID
```

### Frontend

```bash
cd web-ui
pnpm install

# Point at the backend
echo "VITE_API_BASE_URL=http://localhost:8080" > .env.local

pnpm dev
```

---

## Project Structure

```
sprite/
  services/
    main-ctx/       ← Spring Boot backend
  web-ui/           ← React frontend
  landing/          ← Static landing page (deployed to GitHub Pages)
```

---

## License

MIT
