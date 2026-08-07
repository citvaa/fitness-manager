# AGENTS.md

## Purpose of this repository

`fitness-manager` is a Spring Boot backend for managing a gym: clients, trainers,
appointments/schedules, payments, and notifications. It is the foundation of a
Master's thesis (diplomski rad). There is currently no frontend (the
`Frontend/` folder is an empty placeholder).

**This exact state of the repository (tagged `baseline-v1`) is the fixed
starting point for a comparison study.** Two separate AI-upgrade sessions will
branch off from this tag independently: one driven by Claude Code, one by
Codex CLI. They must start from identical context. Concretely, that means:

- This file is read by **both** tools (Codex CLI reads `AGENTS.md` natively;
  `CLAUDE.md` in this repo is a symlink to this file so Claude Code picks up
  the same content). Do not fork the content between two files.
- Everything below must be tool-neutral. Do not add Claude Code-specific
  constructs (skills, subagents, `.claude/` settings, hooks) to convey
  architecture or conventions - Codex has no equivalent, and doing so would
  bias the comparison. Plain instructions in this file are the only channel
  that is fair to both tools.
- **Update this file whenever you discover something new about the
  architecture, make an architectural decision, or change a convention - in
  every session, not just this one. Don't wait to be asked.** Stale docs are
  worse than no docs for a comparison study where both sides read the same
  source of truth.

## Tech stack

- Java 21, Spring Boot 3.4.5, Maven (`Backend/demo/pom.xml`)
- PostgreSQL (JPA/Hibernate + Flyway migrations, `ddl-auto: none` - schema is
  migration-driven only)
- Redis (Spring Cache abstraction, `spring-boot-starter-data-redis`)
- Spring Security + a custom JWT/refresh-token layer (`io.jsonwebtoken:jjwt`)
- Hibernate Envers for entity audit history
- Spring Mail (Gmail SMTP) + Thymeleaf for HTML email templates
- WebSocket/STOMP for real-time notifications
- MapStruct for entity<->DTO mapping, Lombok, springdoc-openapi (Swagger UI)

## Running locally

1. Copy `.env.example` to `.env` and fill in `MAIL_USERNAME`, `MAIL_PASSWORD`
   (a Gmail **App Password**, not the account password), and `JWT_SECRET`
   (>= 32 characters - the app fails to start otherwise). Spring Boot does
   not load `.env` files itself; export these as real environment variables
   before starting the app (IDE run-configuration env vars, or
   `set -a; source .env; set +a` in bash / equivalent in PowerShell).
2. Start infrastructure: `docker compose -f Docker/docker-compose.yaml up -d`
   - Postgres on host port `8877` (mapped to container `5432`), db `fm`,
     user `fm_dbuser` / password `password`
   - Redis on `6379`, password `password` (enforced via `--requirepass`)
   - Postgres data persists in `Docker/postgres_data/` (git-ignored except
     `.gitkeep`) via a bind-mounted volume.
3. Run the app from `Backend/demo/`: `./mvnw spring-boot:run` (Windows:
   `mvnw.cmd spring-boot:run`). Default active profile is `dev`
   (`spring.profiles.active: dev` in `application.yaml`), which additionally
   loads `db/dev-data` Flyway migrations (seed trainer/client test accounts,
   see `V1.0009__insert_test_data.sql`).
4. App listens on port `8088`. Swagger UI: `http://localhost:8088/swagger-ui/index.html`.
   OpenAPI JSON: `http://localhost:8088/v3/api-docs`.

There is no containerized app service in `docker-compose.yaml` - only
Postgres and Redis. The Spring Boot app itself always runs locally
(IDE/`mvnw`) against those two containers.

## Domain model

All entities extend `model/common/BaseEntity` (`@MappedSuperclass`): `version`,
`createdAt`/`createdBy`, `updatedAt`/`updatedBy` via Spring Data JPA auditing.
Every entity is also `@Audited` (Hibernate Envers).

- **User** (`model/user/User.java`) - email, password (null until account
  activation), `isActivated`, `notificationPreference` (`EMAIL`/`PUSH`/`BOTH`),
  registration/reset keys with validity timestamps, `Set<UserRole>`.
- **UserRole** - join entity; `role` is one of `MANAGER` / `TRAINER` / `CLIENT`.
  A single `User` can hold multiple roles.
- **Trainer** - 1:1 with `User`; employment date, birth year, `EmploymentStatus`
  (`FULL_TIME` / `CONTRACT` / `FORMER_EMPLOYEE`).
- **Client** - 1:1 with `User`; owns `Payment`s, `ClientSessionTracking`s,
  `ClientAppointment`s.
- **Session** - a session *type* (`INDIVIDUAL` / `GROUP`) with `maxParticipants`.
  Seeded rows only (INDIVIDUAL/1, GROUP/3, GROUP/10) - not created via the API.
- **Appointment** - date/start/end time, belongs to a `Session` type, optionally
  a `Trainer` (nullable - can exist unassigned), and a set of `ClientAppointment`s.
- **ClientSessionTracking** - per (client, session type) remaining/reserved
  appointment counters, driven by `Payment`s.
- **GymSchedule** - opening/closing time per `DayOfWeek`.
- **TrainerSchedule** - a trainer's status (`WORKING`/`HOLIDAY`/`SICK_LEAVE`/
  `VACATION`) for a given date and time range.
- **Holiday** - a gym-wide non-working date.

## Auth flow (read this before touching security-adjacent code)

- Login (`UserServiceImpl.login(LoginUserRequest)`) verifies bcrypt password,
  issues an access token (15 min, `app.jwt.accessTokenExpiration`) and a
  refresh token (2h, `app.jwt.refreshTokenExpiration`), both HS256-signed with
  `app.jwt.secret` (`util/JwtUtil.java`).
- Refresh tokens are **stateless** - there is no server-side store/revocation
  list, and refreshing does not rotate the refresh token (the same one is
  echoed back). A leaked refresh token stays valid until its own natural
  expiry.
- **Actual route protection is implemented by two custom
  `HandlerInterceptor`s, not by Spring Security's filter chain**:
  - `interceptor/JwtInterceptor` (order 1) - validates the `Authorization:
    Bearer <token>` signature, 401s if missing/invalid.
  - `interceptor/RoleInterceptor` (order 2) - reads the `@RoleRequired`
    annotation on the handler method and checks the JWT's `roles` claim,
    403s if none match. **A handler with no `@RoleRequired` is reachable by
    any authenticated user regardless of role** (e.g. `CalendarController`).
  - Both are registered in `config/web/WebConfig` with an identical
    exclude-list (register/login/swagger). Note that `forgot-password` and
    `reset-password` are *not* in that exclude list, which looks like a bug
    (a user who forgot their password by definition has no valid JWT to
    present) - flagged here rather than silently fixed, since fixing it
    changes runtime auth behavior and this session is hygiene-only.
- `SecurityConfig`'s `SecurityFilterChain` permits `/api/**` (and
  swagger/websocket paths) via `authorizeHttpRequests(...).permitAll()` and
  otherwise requires authentication via its own `oauth2ResourceServer` JWT
  decoder. Because the app's actual `/api/**` routes are already covered by
  that `permitAll`, Spring Security is **not** the layer doing authorization
  for the REST API today - the interceptors above are. Spring Security's
  `anyRequest().authenticated()` still matters as a defense-in-depth fallback
  for any path that is *not* under the permitted prefixes. Do not assume
  `@PreAuthorize`/`hasRole` do anything here - they are not used; role checks
  are exclusively via `@RoleRequired` + `RoleInterceptor`.
- CORS is configured in `config/web/CorsConfig` (a `CorsConfigurationSource`
  bean wired into `SecurityConfig` via `.cors(...)`), driven by
  `app.cors.allowed-origins` in `application.yaml` (defaults to
  `http://localhost:5173` for local frontend dev). **Change this to the real
  frontend domain in production - never `*`, since credentials are allowed.**

## Notifications

- Email (`service/impl/notification/email/`): activation and password-reset
  emails use Thymeleaf templates; appointment-reminder and trainer-schedule
  emails are built as inline strings (inconsistent with the templated ones,
  not yet unified).
- WebSocket/STOMP (`config/web/WebSocketConfig`, endpoint `/ws`, simple broker
  on `/topic`): `NotificationServiceImpl` pushes to per-user topics and
  additionally sends email based on `User.notificationPreference`.
- `NotificationScheduler` (`@Scheduled`): daily trainer/client appointment
  digests at 20:00, and an hourly sweep for appointments starting within the
  next hour.
- `websocket/StompWebSocketClient` is a manual `public static void main` test
  harness left in `src/main/java` (not part of runtime wiring, not test code)
  - known clutter, not removed in this session to keep the diff hygiene-only
  and avoid touching anything outside the explicitly scoped cleanup items.

## Audit (Hibernate Envers)

Every entity is `@Audited`. Audit tables (`*_aud`, `revinfo`) are **hand-written
Flyway migrations**, not Envers-generated at runtime (`ddl-auto: none`).
**Adding or changing an `@Audited` entity's columns requires manually writing
the matching migration** - Envers will not create it for you, and nothing
will fail loudly if you forget; it will just silently not persist history for
the new column.

Known gap: `AuditorAwareImpl` reads the current user from
`SecurityContextHolder`, but nothing in the request pipeline populates the
`SecurityContext` (auth is interceptor-based, see above) - so `createdBy`/
`updatedBy` are always `null` in practice. Not fixed in this session (it's a
behavior change, not a pure hygiene fix); noted here so it isn't mistaken for
an intentional design choice.

## Caching

Redis via Spring Cache, one global `RedisCacheConfiguration` (10 min TTL,
JSON serialization) in `config/cache/RedisConfig`. Currently only
`TrainerServiceImpl` actually uses caching (`TRAINER_CACHE`); no other
service caches anything.

## Conventions

- Layered packages: `controller` (thin, `@RoleRequired`-gated) -> `service`
  interface + `service.impl` -> `repository` (Spring Data JPA) -> `model`
  (JPA entities). Config classes are grouped by concern under
  `config/{audit,cache,core,security,web}`.
- Every entity has a matching DTO (`dto/**`) and a MapStruct
  `@Mapper(componentModel = "spring")` interface (`mapper/**`).
- Write-side request/response objects live under `service/params/request/**`
  and `service/params/response/**`, separate from the read-side `dto/**` -
  this is a deliberate three-way split (persistence model / read DTO / write
  request-response), keep new endpoints consistent with it.
- Lombok `@Builder` on entities that are constructed programmatically in
  service code; plain constructors on the rarely-constructed ones.
- `@Slf4j` logging throughout; scheduler/notification logs use an
  emoji-prefixed style (`🔥`/`✅`/`❌`) as an established (if unusual)
  convention - match it in that area rather than "fixing" it to plain text.
- No global `@ControllerAdvice`/exception handler exists; unhandled service
  exceptions currently fall through to Spring Boot's default error response.
- **Do not edit existing Flyway migration files** (`db/migration/V1.00XX__*`)
  - their checksums are locked once applied. If schema changes are needed,
  add a new `V1.00XX__*.sql` file. Dev-only seed data lives in the separate
  `db/dev-data/` location, only loaded on the `dev` profile.

## Upgrade: schema decisions

Phase 1 of the upgrade is deliberately limited to Flyway migrations, JPA
entities, repositories, DTOs, and MapStruct mappers. No services, controllers,
WebSocket wiring, LLM calls, or frontend code are part of this phase.

- **Gym remains a real table in a single-installation product.** One row is
  expected per deployed installation, but a normal audited entity preserves
  versioning and enables a future settings endpoint. Besides name/address it
  stores contact details, logo URL, hex brand color, and IANA timezone. The
  timezone belongs to the installation because appointment and insight logic
  must not depend on the server's local timezone.
- **Room geometry uses rotated rectangles.** `posX`, `posY`, `width`, `height`,
  and `rotationDegrees` map directly to a `react-konva` rectangle and keep
  validation, editing, and future overlap calculations simple. Polygons would
  require ordered point persistence and substantially more frontend/backend
  geometry logic without a current requirement for irregular room shapes. A
  future polygon table can be added alongside these columns if needed.
- **Appointment-to-Room is optional.** Existing appointments have no room and
  scheduling can reasonably create an unassigned appointment, just as trainer
  assignment is currently optional. A later service layer may enforce room
  assignment for selected workflows without making this migration breaking.
  `AppointmentDTO` exposes only a lightweight `RoomSummaryDTO` (id, name, type,
  capacity), not the complete room geometry/configuration, because appointment
  lists are frequent and do not need the heavier floor-plan representation.
- **Manual occupancy has an explicit event entity.** Current planned occupancy
  combines appointments in progress (derived from existing appointment/client
  data) with optional `RoomCheckIn` rows. Keeping check-in/check-out timestamps
  preserves attendance history for later analytics instead of storing only a
  mutable current-room state. A null `checkedOutAt` denotes an active check-in;
  partial indexes support active occupancy and active-client lookups. A unique
  partial index on `client_id WHERE checked_out_at IS NULL` enforces the domain
  rule that a client can physically occupy only one room at a time across the
  entire gym, rather than one active check-in per room.
- **Body measurements use fixed numeric columns.** Weight, body-fat percentage,
  waist, chest, hip, thigh, and arm measurements cover the expected thesis
  scope and remain directly queryable/chartable. This follows the project's
  typed relational model and MapStruct conventions. JSON/EAV would be more
  flexible but would weaken validation and complicate aggregation; adding a
  future measurement requires an additive migration by design.
- **Exercises are free text, not a catalog.** A catalog would require its own
  lifecycle, seed data, normalization rules, and administration UI, none of
  which is needed for per-client progress tracking. `exerciseName` therefore
  stays free text and can later be supplemented by an optional catalog foreign
  key without removing historical names.
- **Record units are a fixed enum.** `KG`, `LB`, `REPS`, `SECONDS`, `MINUTES`,
  `METERS`, and `KM` cover strength and endurance records while preventing
  inconsistent unit strings. The database mirrors the Java enum with a check
  constraint, following existing enum conventions.
- **All new audited schema is explicit.** Migrations `V1.0011`-`V1.0014` create
  the gym/room/check-in and progress tables, add nullable `appointment.room_id`,
  and create matching Envers audit tables. `appointment_aud.room_id` is added
  in the same additive audit migration because `ddl-auto` is disabled.

## Upgrade: service layer decisions

Phase 2 exposes the Phase 1 data model through REST services and the existing
STOMP broker. No schema migration was needed for this phase.

- **Floor-plan writes are manager-only; reads are authenticated.** `MANAGER`
  owns Gym/Room create, update, and delete operations. `MANAGER`, `TRAINER`,
  and `CLIENT` can read the gym and rooms because the floor plan is normal
  operational information. The API preserves the single-installation rule by
  rejecting a second Gym row, and refuses to delete a Gym until its rooms are
  removed. Room geometry and positive capacity/dimension validation are
  performed before persistence so callers get a useful 4xx response instead
  of only a database constraint error.
- **Manual check-in is a staff operation.** `MANAGER` and `TRAINER` can check a
  client in or out; all three roles can read current occupancy. Clients cannot
  create their own attendance history. The service checks for an existing
  active check-in and also translates a race against the database's global
  unique partial index into HTTP 409 with an explicit message.
- **Occupancy is a source-labelled sum.** Each room payload contains
  `manualCheckIns`, `scheduledParticipants`, and `totalOccupancy`. Scheduled
  participants are client-appointment links for appointments whose
  `[startTime,endTime)` contains the current time. The total intentionally adds
  both sources rather than attempting identity de-duplication: a manual
  check-in is independent attendance evidence and current appointment rows do
  not declare that check-in is their attendance record. Consumers can display
  the two source counts if this conservative sum exceeds capacity. All current
  time calculations use the Gym's IANA timezone.
- **Live occupancy reuses the existing broker.** Snapshots are sent to
  `/topic/gym/occupancy` after check-in/check-out and every 60 seconds so
  appointment boundary changes appear without a write event. The message is
  the same `OccupancySnapshotResponse` returned by REST: `generatedAt` plus an
  ordered list of per-room source counts, capacity, and total. Sharing one
  representation keeps polling and STOMP clients consistent.
- **Claude calls use a pinned fast model.** Both insight features call the
  Anthropic Messages API directly with `ANTHROPIC_API_KEY` and pinned model ID
  `claude-haiku-4-5-20251001`. Anthropic's current model documentation describes
  Haiku 4.5 as its fastest current model and the least expensive current tier,
  which fits short narrative summaries. Pinning avoids silent behavior changes
  from an alias. The app may start without a key for non-AI development, but AI
  endpoints return HTTP 503; they never return mocked text.
- **AI caches have workload-specific TTLs.** Manager insights use Redis cache
  `managerInsights` with a six-hour TTL because they aggregate a 30-day window
  and are relatively expensive; `force=true` bypasses and replaces the entry.
  Client narratives use `clientProgressInsights` with a one-hour TTL per client
  and are evicted on every progress-entry or personal-record create/update/delete.
  These explicit regions override the existing global ten-minute TTL without
  changing `TRAINER_CACHE` behavior.
- **Manager revenue insight is currently a documented proxy.** `Payment` stores
  the number of paid appointments but no price, currency, or monetary amount.
  The prompt therefore supplies payment count and paid appointment units and
  explicitly forbids Claude from inventing monetary revenue. True revenue
  analytics requires a future additive payment-price schema decision.
- **Progress ownership uses existing appointment relationships.** There is no
  trainer-client assignment table. A trainer may CRUD and summarize progress
  only when at least one existing appointment links that trainer to that client.
  Clients have read-only access to their own entries, records, and cached AI
  summary. This avoids granting every trainer access to every client's health-
  adjacent data while preserving the current data model.
- **Phase 2 introduces consistent feature error responses.** `ApiException` and
  `ApiExceptionHandler` return timestamp/status/error/message JSON for explicit
  API failures, including not-found, invalid geometry, ownership violations,
  duplicate check-ins, missing AI configuration, and upstream Claude errors.
  A database-integrity fallback returns HTTP 409. Existing unclassified runtime
  exceptions still use Spring Boot's default response.

## Upgrade: frontend decisions

Phase 3 introduces the first frontend under `Frontend/`, focused on
authentication and the manager floor-plan experience. AI-insights UI is
deliberately deferred to the following phase.

- **React 19 + TypeScript + Vite remain the frontend foundation.** The room
  schema was explicitly designed around React canvas tooling, Vite's default
  port `5173` already matches the backend CORS configuration, and a client-side
  SPA is the simplest fit for a live operational dashboard backed entirely by
  the existing Spring REST/STOMP API. No backend CORS change is required.
- **State is split by lifetime, with no general-purpose global server cache.**
  Zustand owns the small durable auth/session state; page-local React state
  owns editor forms and fetched floor-plan data. This avoids introducing a
  larger query-state abstraction for the handful of endpoints in this phase.
  Axios is the single REST client and centralizes bearer injection, pre-expiry
  refresh, one shared in-flight refresh request, and a single 401 retry.
  A session timer also refreshes one minute before JWT expiry even while the
  UI is idle; protected API requests perform the same check as a fallback.
- **Tokens use browser local storage for this thesis-scale SPA.** It provides
  session persistence across refreshes and allows the frontend to use the
  backend's existing bearer-token contract without a backend cookie change.
  The accepted trade-off is exposure to successful XSS; the UI does not render
  untrusted HTML and a production hardening phase should prefer HttpOnly secure
  cookies if the backend auth contract is changed accordingly.
- **Login treats the backend `email` field as an identifier string.** Existing
  dev accounts include the username-like value `admin`, so the login control
  uses `type="text"` with `inputMode="email"` instead of native `type="email"`;
  otherwise browser constraint validation prevents the seeded manager login
  before the request reaches the backend.
- **Multi-role users choose an active area.** All roles from the JWT remain
  available in a role switcher; the last valid active role persists with the
  session. `MANAGER` enters the live plan, while `TRAINER` and `CLIENT` enter
  the Phase 4 placeholder. This is less surprising than silently imposing a
  fixed role priority and preserves access to every area granted by the JWT.
- **Visual styling is purpose-built CSS rather than a component kit.** The
  defense's central artifact is a spatial canvas, so a generic dashboard kit
  would contribute substantial unused surface and a recognizable stock look.
  A small tokenized CSS system provides the dark botanical/lime GymOS identity,
  responsive shell, motion, and accessible focus/error states with less bundle
  and tighter control over the live-plan presentation.
- **The plan uses a fixed 1000x620 logical coordinate system.** Both editor and
  live view scale that surface to the available viewport while persisting only
  logical coordinates, so dragging/resizing on one screen does not rewrite the
  layout for another resolution. React-Konva provides editor transforms;
  ordinary positioned DOM elements render the live view because CSS transitions
  produce smoother occupancy color/count motion and keep room content accessible.
  Konva `Layer` children are emitted through a single array expression because
  literal JSX whitespace becomes an invalid text node in React-Konva and can
  prevent the canvas from rendering under React 19.
- **Live state is REST-first, then STOMP snapshots.** The page fetches
  `GET /api/gym/occupancy` for deterministic initial render, subscribes to
  `/topic/gym/occupancy`, and replaces its complete snapshot on each message.
  The STOMP client reconnects automatically; no parallel frontend polling is
  added because the backend already broadcasts appointment-boundary changes
  every minute.
- **The dev profile seeds a complete demonstration floor plan.** Dev migration
  `V1.0016__insert_demo_floor_plan.sql` creates one branded Gym and five rooms
  only after the production schema migrations and only when no Gym row already
  exists, so an established developer database is never given a second Gym.
  Production receives no sample
  location data, while a fresh local database opens immediately into a useful
  defense-ready plan that remains fully editable through the manager UI.

### Upgrade Phase 4 UI decisions

- **AI narratives are rendered as safe plain-text paragraphs.** Manager and client-progress prompts explicitly prohibit Markdown syntax and request blank-line-separated plain text; responses are split on line breaks and displayed as React text nodes rather than interpreted as HTML or Markdown. This preserves readable formatting while maintaining the Phase 3 rule that the SPA does not render untrusted markup. Both screens expose the backend generation timestamp and model, and only role-authorized trainer/manager views expose force regeneration.

- **Trainer client discovery mirrors progress ownership.** A small GET /api/trainer/clients endpoint returns distinct lightweight client summaries only for clients linked to the authenticated trainer through an appointment. The progress UI uses this list as its sole selector source, so it cannot invite navigation to arbitrary client IDs.
- **Progress visualization uses one shared role-aware page and Recharts.** Trainer and client areas share chart, record-list, and narrative presentation so equivalent data cannot drift visually; write forms and force-refresh controls are rendered only in trainer mode. Seven typed measurement series share one responsive line chart, with null values connected so partially completed measurements remain useful.

- **Client progress stays backend-enforced read-only.** Client mode calls only the existing `/api/client/progress` self endpoints, never accepts a client ID, and renders neither measurement/record forms nor force regeneration. This duplicates the backend boundary in the UI without treating hidden controls as authorization.

## Known issues (intentionally not fixed in the baseline-hygiene session)

These were found during the repo-hygiene pass that produced `baseline-v1`.
They are documented here rather than fixed because fixing them changes
runtime behavior, and the goal of that session was a stable, unchanged-behavior
baseline - not new features or behavior changes. They are fair game for
either upgrade session to pick up:

- `forgot-password`/`reset-password` endpoints are not excluded from
  `JwtInterceptor`/`RoleInterceptor`, effectively making them unreachable
  without an existing valid JWT.
- `POST /api/user/login-refresh` is also not excluded from `JwtInterceptor`,
  so refreshing requires a currently-valid access token - unusual for a
  refresh endpoint, whose point is normally to work *after* expiry. **Resolved
  in Phase 3:** `/api/user/login-refresh` is now excluded from both custom
  interceptors and accepts a valid refresh token without an access token.
- Refresh tokens have no rotation and no server-side revocation.
- `AuditorAwareImpl` always returns empty (see Audit section above) -
  `createdBy`/`updatedBy` are effectively dead columns.
- `TrainerSchedule.date` is annotated `unique = true` at the entity level
  (`model/schedule/TrainerSchedule.java`), which would incorrectly allow only
  one trainer total to have a schedule row on any given date - it should
  almost certainly be a composite `(trainer_id, date)` constraint. No DB-level
  unique constraint actually exists in the migrations, so entity and schema
  disagree; this has had no observed effect yet but is worth fixing carefully
  (with a new migration) before relying on it.
- `UserController`'s `reset-password` mapping is missing a leading `/`
  (`"reset-password"` instead of `"/reset-password"`) - verify the resolved
  path before assuming it works as intended.
- `CalendarController.getScheduleForDay` has no `@RoleRequired`, so any
  authenticated user (any role) can call it.
- Only Phase 2's explicit `ApiException` and database-integrity failures have a
  consistent JSON error shape; older unclassified exceptions still fall
  through to Spring Boot's default response.
- **Resolved in the final hardening phase:** `maven.test.skip` was removed and
  focused upgrade-service tests now run by default with `mvn test`. The suite
  uses mocked repositories and a fake Claude boundary, so it needs neither
  infrastructure nor network/API budget.
- `application.yaml` and `application-dev.yaml` duplicate almost every
  property instead of the dev file overriding only what differs (currently
  just `flyway.locations`) - keep both in sync manually until this is
  restructured.
- The Gmail account used for `MAIL_USERNAME` had its app password committed
  in git history (now moved to an env var, but the old value is still
  recoverable from history) - **the app password must be rotated in the
  Gmail account**, this repo change alone does not invalidate it.

## Session log

- 2026-08-03: Repo-hygiene baseline pass (`chore/repo-hygiene` -> `main`,
  tagged `baseline-v1`). Merged the already-diverged `feature/notification`
  work into `main` (it turned out `origin/main` already had it via a merged
  PR; only the local `main` ref was stale). Moved Gmail/JWT secrets to env
  vars (`.env.example`), added CORS config (`app.cors.allowed-origins`,
  default `http://localhost:5173`), removed the dead-code `"/**"` permitAll
  entry from `SecurityConfig` and documented the real (interceptor-based)
  authorization model above, enabled Postgres volume persistence in
  `docker-compose.yaml` and fixed Redis auth (`--requirepass`, since the
  official image ignores `REDIS_PASSWORD`), added root `.gitignore` and this
  file. No functional/behavioral changes beyond those explicitly listed here.
- 2026-08-04: Upgrade Phase 1 data layer (`upgrade/codex`). Added `Gym`, `Room`,
  `RoomCheckIn`, `ClientProgressEntry`, and `ClientPersonalRecord` entities with
  repositories, DTOs, MapStruct mappers, an optional `Appointment.room` link,
  and Flyway migrations `V1.0011`-`V1.0014`, including matching Envers tables.
  No service, controller, WebSocket, LLM, or frontend code was added.
- 2026-08-04: Clarified shared requirements after Phase 1: the database now
  enforces one active room check-in per client globally (`V1.0015`), and
  appointments expose a lightweight `RoomSummaryDTO` instead of full room
  geometry. These are product requirements for subsequent phases.
- 2026-08-05: Upgrade Phase 2 service/API layer (`upgrade/codex`). Added
  manager-controlled Gym/Room CRUD, staff check-in/check-out, timezone-aware
  combined occupancy REST/STOMP snapshots, cached Anthropic-backed manager and
  client-progress narratives, and trainer-owned/client-self progress APIs.
  Added no frontend or schema migrations.
- 2026-08-06: Upgrade Phase 3 frontend preparation (`upgrade/codex`). Made
  `POST /api/user/login-refresh` a public auth endpoint in both custom
  interceptor registrations so silent refresh continues after access-token
  expiry.
- 2026-08-06: Upgrade Phase 3 manager frontend (`upgrade/codex`). Added the
  React/Vite auth and multi-role shell, React-Konva room editor, animated
  REST/STOMP live occupancy plan, and dev-only Momentum Fitness floor-plan seed.
- 2026-08-06: Phase 3 browser QA (`upgrade/codex`). Installed a Playwright MCP
  browser runtime and changed the login identifier control so the seeded
  `admin` manager account can submit through native browser validation.
- 2026-08-07: Phase 4 AI text formatting fix (`upgrade/codex`). Updated both
  Anthropic system prompts to require blank-line-separated plain text without
  Markdown syntax, preserving the frontend's safe React-text rendering. Forced
  real Claude regeneration for manager and trainer progress insights and
  captured browser QA screenshots confirming that `#` and `**` are absent.
- 2026-08-07: Final defense hardening (`upgrade/codex`). Enabled Maven tests and
  added focused coverage for Gym/Room invariants, occupancy source summation and
  the global active-check-in guard, trainer-client ownership, and AI cache
  hit/forced-refresh behavior using a fake Claude service. Added explicit live
  loading/disconnection states and a branded favicon, plus the committed
  `docs/defense-demo-script.md` runbook. A destructive fresh-volume rehearsal
  applied all 16 Flyway migrations and verified seeded logins, five demo rooms,
  live check-in/checkout with duplicate HTTP 409, API-created trainer schedule
  and appointment, trainer-owned progress writes, client read-only visibility,
  and the expected HTTP 503 AI response when no Anthropic key is configured.

## Final upgrade summary

The completed upgrade has three connected pillars. The **live gym plan** stores
an audited installation and rotated-rectangle rooms, combines manual check-ins
with in-progress appointment participation in timezone-aware occupancy
snapshots, and distributes the same representation through REST and STOMP. The
**manager insight** pillar aggregates a rolling 30-day operational window and
uses a pinned Claude Haiku model with a six-hour Redis cache; payment analytics
remain an explicitly labelled appointment-unit proxy because the baseline has
no monetary amount. The **client progress** pillar stores typed measurements
and free-text exercise records, charts them in a shared role-aware UI, restricts
trainer access through existing appointment relationships, keeps client routes
self-scoped/read-only, and caches narratives per client for one hour with
write-through eviction.

The implementation deliberately retains the existing interceptor-based JWT
authorization model, stateless non-rotating refresh tokens, explicit Flyway and
Envers migrations, browser-local token storage, and the single-installation Gym
assumption. Other inherited known issues listed above also remain out of scope.
AI endpoints require a real `ANTHROPIC_API_KEY` and return HTTP 503 rather than
mocked production text when it is absent; automated tests instead fake the
Claude boundary. The final defense runbook documents demo accounts, the two-tab
live check-in sequence, preparation steps, and offline/API/WebSocket fallbacks.



