# Securevault

This is a small secure vault application that allows multiple users to store and manage their sensitive information
securely.
The application uses encryption to protect the data and provides a user-friendly interface for easy access.

The goal of this project is to show the knowledge of building secure real-world applications with best practices in
security, auth and data protection.
The application is build using react for frontend and a microservice architecture for the backend with java spring boot
services and a postgresql database with redis for caching and rate limiting.

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

```bash
git clone https://github.com/username/securevault.git
cd securevault

# Start PostgreSQL and Redis
docker-compose up -d postgres redis

# Start backend services (each in a separate terminal)
cd auth-service && ./gradlew bootRun
cd vault-service && ./gradlew bootRun
cd audit-service && ./gradlew bootRun
cd gateway && ./gradlew bootRun

# Start frontend
cd frontend && npm install && npm run dev
```

The frontend will be available at `http://localhost:5173` and the API at `http://localhost:8080`.
