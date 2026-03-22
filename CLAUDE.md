# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repo Structure

```
sprite/
  services/
    main-ctx/       ← Spring Boot backend (Java 21)
  web-ui/           ← React frontend (TypeScript, Vite)
```

Each sub-project has its own `CLAUDE.md` with commands and architecture details.

## What This Is

Sprite is a video thumbnail sprite sheet generation platform. Users submit video URLs, the backend processes them with FFmpeg to produce sprite sheets (WebP/AVIF/JPG/Blurhash), and the frontend lets users manage job specs, submit jobs, and preview results.

## Auth Model

Firebase handles identity. The frontend obtains a Firebase ID token and sends it as `Authorization: Bearer <token>` plus `X-ACCOUNT-ID: <accountId>` on every authenticated request. The backend also supports API key auth via `X-API-KEY-ID` + `X-API-KEY` headers.
