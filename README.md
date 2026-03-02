# Securevault

This is a small secure vault application that allows multiple users to store and manage their sensitive information
securely.
The application uses encryption to protect the data and provides a user-friendly interface for easy access.

The goal of this project is to show the knowledge of building secure real-world applications with best practices in
security, auth and data protection.
The application is build using react for frontend and a microservice architecture for the backend with java spring boot
services and a postgresql database with redis for caching and rate limiting.

## Tech Stack

| Layer      | Technology                                |
|------------|-------------------------------------------|
| Backend    | Java 21, Spring Boot 4.0.2, Gradle 9.3.0 |
| Database   | PostgreSQL 16                             |
| Cache      | Redis 7                                   |
| Frontend   | React 18, TypeScript, Vite                |
| Admin      | Angular 19, TypeScript                    |
| Security   | JWT RS256, Argon2id, AES-256-GCM, TOTP   |
| DevOps     | Docker, Docker Compose, GitHub Actions    |

## Key Features

- Encryption: All data is encrypted using strong encryption algorithms to ensure confidentiality.
- Authentication: Users must authenticate themselves before accessing their vault with JWT token and 2FA.
- Roles: Different user roles (admin, manager, user) with varying levels of access and permissions.
- Team sharing: Share individual secrets with team members (read or write access).
- Folders: Organize secrets into nested folders.
- Audit: Every action is logged, who accessed what, when, and from where.
- AI Advisor: AI-powered analysis of your secrets with rotation recommendations and risk assessment.

## Architecture

The application follows a microservice architecture with the following components (diagram generated with claude):

```
                    ┌──────────────────┐
                    │   React Frontend │
                    │   (Vite + TS)    │
                    └────────┬─────────┘
                             │ HTTPS
                             ▼
                    ┌──────────────────┐
                    │   API Gateway    │
                    │   (Port 8080)    │
                    │   Routing, Rate  │
                    │   Limiting, JWT  │
                    └──┬─────┬──────┬──┘
                       │     │      │
              ┌────────┘     │      └────────┐
              ▼              ▼               ▼
     ┌───────────────┐ ┌──────────────┐ ┌──────────────┐
     │ Auth Service  │ │ Vault Service│ │ Audit Service│
     │ (Port 8081)   │ │ (Port 8082)  │ │ (Port 8083)  │
     │               │ │              │ │              │
     │ • Registration│ │ • CRUD       │ │ • Activity   │
     │ • Login/JWT   │ │ • Encryption │ │   Logs       │
     │ • 2FA (TOTP)  │ │ • Sharing    │ │ • Alerts     │
     │ • RBAC        │ │ • Folders    │ │ • Reports    │
     └──────┬────────┘ └──────┬───────┘ └──────┬───────┘
            └────────┬────────┴────────┬───────┘
                     ▼                 ▼
              ┌────────────┐   ┌────────────┐
              │ PostgreSQL │   │   Redis    │
              │            │   │ (Sessions, │
              │ Users,     │   │  Cache,    │
              │ Secrets,   │   │  Rate      │
              │ Audit Logs │   │  Limiting) │
              └────────────┘   └────────────┘
```

## Microservices decision

The decision to use a microservice architecture for this secure vault application was driven by the fact that all
services have only a single responsibility and can be developed, tested, and deployed independently.

- API Gateway: Single entry point for all client requests. Handles routing, rate limiting (Redis-based), CORS, and JWT
  token validation before forwarding requests to the appropriate service.
- Auth Service: Manages everything related to identity. User registration, login, JWT token generation (RS256), refresh
  token rotation, TOTP-based 2FA, and role-based access control.
- Vault Service: The core of the application. Handles CRUD operations for secrets, AES-256-GCM encryption/decryption,
  folder organization, team sharing, and AI-powered security analysis.
- Audit Service: Records every security-relevant action (logins, secret access, sharing changes) for compliance and
  monitoring. Provides filterable logs and export capabilities.

# Getting Started

## Prerequisites
- Java 21
- Node.js 20+
- Docker & Docker Compose

## Option 1: Development Mode (recommended for daily work)

Development mode runs Postgres and Redis in Docker, but all application services run locally with hot-reload. Code changes are reflected immediately without rebuilding Docker images.

```bash
git clone <repository-url>
cd securevault

# Copy environment variables
cp .env.example .env

# Start everything in one terminal
./dev.sh
```

This will:
- Start Postgres and Redis containers (if not already running)
- Launch all 4 backend services with Gradle (`bootRun`)
- Start the React frontend with Vite HMR
- Show color-coded logs from all services
- Stop everything cleanly with `Ctrl+C`

| Service           | URL                        |
|-------------------|----------------------------|
| React Frontend    | http://localhost:5173       |
| Angular Admin     | http://localhost:4200       |
| API Gateway       | http://localhost:8080       |
| Auth Service      | http://localhost:8081       |
| Vault Service     | http://localhost:8082       |
| Audit Service     | http://localhost:8083       |

## Option 2: Full Docker (for demos and production)

Everything runs in Docker containers. No local Java or Node.js needed.

```bash
git clone <repository-url>
cd securevault

# Copy environment variables
cp .env.example .env

# Build and start all containers
docker compose -f docker-compose.prod.yml up --build
```

| Service           | URL                        |
|-------------------|----------------------------|
| React Frontend    | http://localhost:5173       |
| Angular Admin     | http://localhost:4200       |
| API Gateway       | http://localhost:8080       |

## Health Checks

All services expose health endpoints via Spring Boot Actuator:

```bash
curl http://localhost:8080/actuator/health   # Gateway
curl http://localhost:8081/actuator/health   # Auth Service
curl http://localhost:8082/actuator/health   # Vault Service
curl http://localhost:8083/actuator/health   # Audit Service
```

## CI/CD

The project uses GitHub Actions for continuous integration and deployment:

- **CI** (`ci.yml`): Runs on every push and PR to `main`. Tests all backend services in parallel with PostgreSQL and Redis service containers. Builds both frontends.
- **CD** (`cd.yml`): Runs on push to `main`. Builds Docker images for all services and pushes them to GitHub Container Registry (`ghcr.io`).
