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
- [x] LoginRequest DTO
- [x] SecurityConfig (CORS, CSRF, Endpoint-Regeln)
- [x] Argon2id PasswordEncoder Bean
- [x] JWT Key Pair generieren (RS256) und laden
- [x] JwtService (Token erstellen, validieren, Claims extrahieren)
- [x] RefreshToken Entity und Repository
- [x] AuthService (register, login, refresh, logout)
- [x] AuthController (POST /auth/register, /auth/login, /auth/refresh, /auth/logout)
- [x] GlobalExceptionHandler (Validation, Auth, NotFound)
- [x] TOTP-Service (Secret generieren, QR-Code URI, Code verifizieren)
- [x] TwoFactorController (POST /auth/2fa/setup, /auth/2fa/verify, /auth/2fa/disable)
- [x] RBAC: Rollen-Enum (ADMIN, MANAGER, USER) und @PreAuthorize Absicherung
- [x] Redis Token-Blacklist fuer Logout
- [x] Unit Tests fuer AuthService und JwtService
- [x] Integration Tests fuer AuthController

---

## Phase 3 – API Gateway

- [x] Server-Port auf 8080 konfigurieren
- [x] Gateway-Routen definieren (auth-service, vault-service, audit-service)
- [x] JWT-Validierungsfilter (Token pruefen, User-ID und Rolle in Header weiterleiten)
- [x] Rate-Limiting Filter mit Redis (z.B. 100 req/min pro IP)
- [x] CORS-Konfiguration (Frontend Origins)
- [x] Health-Check Endpoint
- [x] Tests fuer Routing und JWT-Filter

---

## Phase 4 – Vault Service

- [x] Secret Entity (id, userId, name, encryptedValue, iv, folder, timestamps)
- [x] Folder Entity (id, userId, name, parentFolder)
- [x] SharedSecret Entity (secretId, sharedWithUserId, permission)
- [x] Repositories fuer Secret, Folder, SharedSecret
- [x] EncryptionService (AES-256-GCM encrypt/decrypt)
- [x] SecretService (CRUD mit Verschluesselung)
- [x] SecretController (GET/POST/PUT/DELETE /secrets)
- [x] FolderService und FolderController (/folders)
- [x] SharingService und SharingController (/secrets/{id}/share)
- [x] DTOs mit Validierung (CreateSecretRequest, SecretResponse, etc.)
- [x] Zugriffskontrolle: User sieht nur eigene Secrets + geteilte
- [x] Unit Tests fuer EncryptionService
- [x] Integration Tests fuer SecretController

---

## Phase 5 – Audit Service

- [x] AuditEvent Entity (id, userId, action, resourceType, resourceId, ip, timestamp)
- [x] AuditEventRepository mit Filter-Queries (by user, action, date range)
- [x] AuditService (Event speichern)
- [x] REST-Endpoint zum Empfangen von Events (POST /audit/events)
- [x] AuditQueryController (GET /audit/events mit Filtern und Pagination)
- [x] CSV/JSON Export Endpoint
- [x] Auth- und Vault-Service: Audit-Events bei relevanten Aktionen senden
- [x] Tests fuer AuditService

---

## Phase 6a – React App (User-facing)

- [x] React-Projekt mit Vite + TypeScript initialisieren
- [x] API-Client: Axios mit JWT-Interceptors + TypeScript Types
- [x] AuthContext + ProtectedRoute Komponente
- [x] Login- und Register-Seiten
- [x] Vault-Seite: Secrets-Liste, Erstellen, Bearbeiten, Loeschen

---

## Phase 6b – Angular App (Admin Dashboard)

- [x] Angular-Projekt mit Angular CLI initialisieren
- [x] AuthService + JWT HttpInterceptor
- [x] Routing mit AuthGuard + Login-Seite
- [x] Audit-Log-Seite: Tabelle mit Filtern und Pagination
- [x] Dashboard: Event-Zaehler nach Typ

---

## Phase 7 – DevOps & CI/CD

- [ ] Dockerfiles fuer alle Services (Multi-Stage Build)
- [ ] docker-compose.prod.yml mit allen Services containerisiert
- [ ] Environment-Management und Secrets-Strategie
- [ ] GitHub Actions CI Pipeline (Build + Test bei Push/PR)
- [ ] GitHub Actions CD Pipeline (Docker Images bauen + pushen)
- [ ] Health Checks und Monitoring Basics (Actuator)
- [ ] README aktualisieren mit Deployment-Anleitung
