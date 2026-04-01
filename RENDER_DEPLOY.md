# Render Deployment Guide

This project can run locally with Docker Compose and on Render with the same
database settings. The application now accepts both local Spring datasource
variables and Render-style `DB_*` variables.

## Render Environment Variables

Add these to your Render web service:

- `DB_URL`
- `DB_USER`
- `DB_PASS`
- `PORT`

## JDBC Format

Render PostgreSQL often shows a connection URL like this:

```text
postgresql://user:password@host:5432/database
```

For Spring Boot, use:

```text
jdbc:postgresql://host:5432/database
```

Keep the username and password in separate variables.

## Deploy Steps

1. Create a PostgreSQL database in Render.
2. Copy the internal hostname, database name, username, and password.
3. Build `DB_URL` as `jdbc:postgresql://HOST:5432/DATABASE`.
4. Create a Web Service from this repository.
5. Let Render build using the root `Dockerfile`.
6. Add the Render environment variables listed above.
7. Deploy the service.

## Relevant Files

- `Dockerfile`
- `render.yaml`
- `src/main/resources/application.yaml`
- `docker-compose.yml`
- `.env.example`
