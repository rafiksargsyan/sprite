# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Install dependencies
pnpm install

# Dev server (default: http://localhost:5173)
pnpm dev

# Type-check + build
pnpm build

# Lint
pnpm lint
```

Set `VITE_API_BASE_URL` in a `.env.local` file to point at the backend (defaults to `http://localhost:8080`).

## Architecture

**React 19 / TypeScript / Vite** SPA using Material UI v7.

```
src/
  app/           ← App root, routing (react-router-dom v7), AuthContext
  pages/         ← One file per route: Login, Dashboard, JobSpecs, Jobs, ApiKeys
  components/    ← Shared UI: Layout, ProtectedRoute, EmailConfirmation, ConfigDetailDialog
  api/           ← Thin fetch wrappers per domain (jobs, jobSpecs, users, apiKeys)
  types/         ← api.types.ts — all request/response interfaces mirroring backend DTOs
  hooks/         ← useAuth (reads AuthContext)
  contexts/      ← AuthContext (Firebase auth state)
```

### Request Flow

Every authenticated API call goes through `api/client.ts` (`apiRequest` / `apiRequestText`), which:
1. Gets a fresh Firebase ID token from the `User` object
2. Attaches `Authorization: Bearer <token>` and `X-ACCOUNT-ID: <accountId>` headers
3. Throws on non-2xx with the backend's `message` field if present

### Key Pages

- **JobSpecs** — CRUD for job spec templates (named sets of thumbnail configs: JPG/WebP/AVIF/Blurhash)
- **Jobs** — Submit jobs (video URL + spec), poll/list results, download ZIP, preview sprites/blurhash via WebVTT scrubber, cancel in-progress jobs
- **ApiKeys** — Manage API keys for programmatic access

### Type Conventions

`src/types/api.types.ts` is the single source of truth for API shapes. When the backend DTO changes, update the corresponding interface here first, then fix usages.
