# SecureVault – TODO

Schrittweise Checkliste zum Durcharbeiten. Phasen bauen aufeinander auf.

---

## Phase 1 – Setup & Infrastruktur

- [x] Git-Repository initialisieren, README.md anlegen
- [x] Gradle-Projekte erstellen (auth-service, vault-service, audit-service, gateway)
- [x] docker-compose.yml mit PostgreSQL 16 und Redis 7
- [x] .gitignore fuer alle Services und Root
- [x] application.properties mit Profilen (dev/prod) fuer auth-service
- [x] application.properties mit Profilen (dev/prod) fuer vault-service
- [x] application.properties mit Profilen (dev/prod) fuer audit-service
- [x] application.properties mit Profilen (dev/prod) fuer gateway
- [x] .env Datei fuer Docker-Secrets (nicht committen)

---

## Phase 2 – Auth Service

- [x] User Entity mit UUID, Email, Passwort-Hash, Rolle, TOTP-Felder
- [x] UserRepository mit findByEmail und existsByEmail
- [x] RegisterRequest DTO mit Validierung
- [x] AuthResponse DTO
- [ ] LoginRequest DTO
- [ ] SecurityConfig (CORS, CSRF, Endpoint-Regeln)
- [ ] Argon2id PasswordEncoder Bean
- [ ] JWT Key Pair generieren (RS256) und laden
- [ ] JwtService (Token erstellen, validieren, Claims extrahieren)
- [ ] RefreshToken Entity und Repository
- [ ] AuthService (register, login, refresh, logout)
- [ ] AuthController (POST /auth/register, /auth/login, /auth/refresh, /auth/logout)
- [ ] GlobalExceptionHandler (Validation, Auth, NotFound)
- [ ] TOTP-Service (Secret generieren, QR-Code URI, Code verifizieren)
- [ ] TwoFactorController (POST /auth/2fa/setup, /auth/2fa/verify, /auth/2fa/disable)
- [ ] RBAC: Rollen-Enum (ADMIN, MANAGER, USER) und @PreAuthorize Absicherung
- [ ] Redis Token-Blacklist fuer Logout
- [ ] Unit Tests fuer AuthService und JwtService
- [ ] Integration Tests fuer AuthController

---

## Phase 3 – API Gateway

- [ ] Server-Port auf 8080 konfigurieren
- [ ] Gateway-Routen definieren (auth-service, vault-service, audit-service)
- [ ] JWT-Validierungsfilter (Token pruefen, User-ID und Rolle in Header weiterleiten)
- [ ] Rate-Limiting Filter mit Redis (z.B. 100 req/min pro IP)
- [ ] CORS-Konfiguration (Frontend Origins)
- [ ] Health-Check Endpoint
- [ ] Tests fuer Routing und JWT-Filter

---

## Phase 4 – Vault Service

- [ ] Secret Entity (id, userId, name, encryptedValue, iv, folder, timestamps)
- [ ] Folder Entity (id, userId, name, parentFolder)
- [ ] SharedSecret Entity (secretId, sharedWithUserId, permission)
- [ ] Repositories fuer Secret, Folder, SharedSecret
- [ ] EncryptionService (AES-256-GCM encrypt/decrypt)
- [ ] SecretService (CRUD mit Verschluesselung)
- [ ] SecretController (GET/POST/PUT/DELETE /secrets)
- [ ] FolderService und FolderController (/folders)
- [ ] SharingService und SharingController (/secrets/{id}/share)
- [ ] DTOs mit Validierung (CreateSecretRequest, SecretResponse, etc.)
- [ ] Zugriffskontrolle: User sieht nur eigene Secrets + geteilte
- [ ] Unit Tests fuer EncryptionService
- [ ] Integration Tests fuer SecretController

---

## Phase 5 – Audit Service

- [ ] AuditEvent Entity (id, userId, action, resourceType, resourceId, ip, timestamp)
- [ ] AuditEventRepository mit Filter-Queries (by user, action, date range)
- [ ] AuditService (Event speichern)
- [ ] REST-Endpoint zum Empfangen von Events (POST /audit/events)
- [ ] AuditQueryController (GET /audit/events mit Filtern und Pagination)
- [ ] CSV/JSON Export Endpoint
- [ ] Auth- und Vault-Service: Audit-Events bei relevanten Aktionen senden
- [ ] Tests fuer AuditService

---

## Phase 6 – Frontend

- [ ] React-Projekt mit Vite + TypeScript initialisieren
- [ ] Routing Setup (React Router)
- [ ] Auth-Pages: Login, Register, 2FA-Setup
- [ ] JWT Token-Handling (Access + Refresh, Axios Interceptor)
- [ ] Vault-UI: Secrets-Liste, Detail, Erstellen, Bearbeiten, Loeschen
- [ ] Folder-Navigation (Baumstruktur oder Breadcrumbs)
- [ ] Sharing-UI: Secret teilen, Berechtigungen verwalten
- [ ] Admin-Bereich: User-Verwaltung, Audit-Log Einsicht
- [ ] Responsive Design und Loading States
- [ ] Error-Handling und Toast-Notifications

---

## Phase 7 – Integration & Production

- [ ] End-to-End Tests (kompletter Flow: Register, Login, Secret erstellen, teilen)
- [ ] docker-compose.prod.yml (alle Services als Container)
- [ ] Dockerfiles fuer jeden Service (Multi-Stage Build)
- [ ] Environment-Variablen Dokumentation
- [ ] Security Review (OWASP Top 10 Checklist durchgehen)
- [ ] Performance: Connection Pooling, Caching-Strategie pruefen
- [ ] CI/CD Pipeline (GitHub Actions: Build, Test, Lint)
- [ ] README aktualisieren (Screenshots, API-Doku Link)
