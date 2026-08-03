# fitness-manager

Spring Boot backend for managing a gym: clients, trainers, appointments,
schedules, payments, and notifications. Built as the foundation for a
Master's thesis; there is currently no frontend.

For architecture, domain model, auth flow, conventions, and known issues, see
[`AGENTS.md`](./AGENTS.md) - it is the canonical, up-to-date reference kept in
sync every session, and is shared by both Codex CLI and Claude Code
(`CLAUDE.md` is a symlink to it).

## Prerequisites

- JDK 21
- Docker (for Postgres + Redis)
- A Gmail account with an [App Password](https://myaccount.google.com/apppasswords)
  for outgoing email (activation/reset/notification mails)

## Running locally

1. **Configure secrets.** Copy `.env.example` to `.env` and fill in
   `MAIL_USERNAME`, `MAIL_PASSWORD` (Gmail App Password), and `JWT_SECRET`
   (>= 32 characters). Spring Boot does not read `.env` files itself - export
   these as real environment variables before starting the app:
   ```bash
   # bash
   set -a; source .env; set +a
   ```
   ```powershell
   # PowerShell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^\s*([^#=]+)=(.*)$') { Set-Item -Path "Env:$($Matches[1])" -Value $Matches[2] }
   }
   ```
   Or set them in your IDE's run configuration instead.

2. **Start infrastructure** (Postgres on `8877`, Redis on `6379`):
   ```bash
   docker compose -f Docker/docker-compose.yaml up -d
   ```

3. **Run the app** (dev profile is active by default):
   ```bash
   cd Backend/demo
   ./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
   ```
   Flyway migrations run automatically on startup, including dev-only seed
   data (a test trainer and client account, see
   `Backend/demo/src/main/resources/db/dev-data/`).

4. **Verify it's up:**
   - Swagger UI: http://localhost:8088/swagger-ui/index.html
   - OpenAPI spec: http://localhost:8088/v3/api-docs

## How the baseline (`baseline-v1`) was verified

Before tagging `baseline-v1`, the following was confirmed against a fresh
Postgres volume:

```bash
# 1. Clean infrastructure
docker compose -f Docker/docker-compose.yaml down
rm -rf Docker/postgres_data/*   # keep .gitkeep
docker compose -f Docker/docker-compose.yaml up -d

# 2. App starts cleanly and Flyway migrates from scratch
cd Backend/demo
./mvnw spring-boot:run
# -> check console: no errors, "Flyway ... Successfully applied N migrations"

# 3. Swagger UI reachable
curl -i http://localhost:8088/v3/api-docs

# 4. End-to-end auth flow: login with a dev-seeded user, then call a
#    protected endpoint with the returned access token
curl -X POST http://localhost:8088/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"email":"<dev-seed-email>","password":"<dev-seed-password>"}'

curl -H "Authorization: Bearer <accessToken from above>" \
  http://localhost:8088/api/calendar?date=2026-08-03
```

Repeat this whenever infrastructure, migrations, or auth-related config
changes, to confirm the app still boots and authenticates end-to-end on a
clean database.
