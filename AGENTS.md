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
- **Gym** (`model/gym/Gym.java`) - single-installation config (name, address,
  contact info, logo/brand color, timezone). Real table, not a code-level
  singleton, even though exactly one row is expected in practice - see
  "Upgrade: schema decisions" below.
- **Room** (`model/gym/Room.java`) - belongs to a `Gym`; name, `RoomType`,
  capacity, and rectangle geometry (`posX`/`posY`/`width`/`height`/
  `rotationDegrees`) for the 2D floor-plan editor/live view. `Appointment`
  optionally references a `Room` (nullable).
- **RoomCheckIn** (`model/gym/RoomCheckIn.java`) - a manual check-in/check-out
  event of a `Client` into a `Room`; `checkedOutAt == null` means currently
  inside. Occupancy computed from in-progress `Appointment`s does not use
  this table - see "Upgrade: schema decisions" below.
- **ClientProgressEntry** (`model/progress/ClientProgressEntry.java`) - a
  dated body-measurement snapshot for a `Client` (weight, body fat %, waist/
  chest/hip/thigh/arm circumference, notes).
- **ClientPersonalRecord** (`model/progress/ClientPersonalRecord.java`) - a
  `Client`'s best result for a free-text exercise name (value + `RecordUnit`
  + date).

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

Phase 1 of the upgrade (data layer only - Flyway migrations `V1.0011`-
`V1.0014`, entities, repositories, DTOs, MapStruct mappers; no services,
controllers, WebSocket, or LLM calls yet) added the tables behind three
planned features: the live gym floor plan, AI manager insights, and visual
client progress tracking. This section documents every non-obvious design
choice made and why, since it is intended as comparison material for the
thesis, not just an internal note.

- **Single-installation model, real tables anyway.** The product is deployed
  once per gym (no multi-tenant row-level isolation anywhere), so `Gym` is
  expected to have exactly one row. It is still a normal `@Entity`/table
  (not a `@ConfigurationProperties` bean or a hardcoded constant) so it goes
  through the same audit/versioning/DTO machinery as everything else and can
  be edited through a future settings endpoint without a schema change.
- **Room geometry: rectangle, not polygon.** `Room` stores `posX`/`posY`/
  `width`/`height`/`rotationDegrees` (all `double precision`) instead of an
  arbitrary polygon (point list / PostGIS geometry / GeoJSON column).
  Reasoning: (1) real gym floor plans are overwhelmingly rectangular rooms;
  (2) `react-konva` (the planned frontend canvas library) has first-class
  `Rect` support with rotation, so this maps directly to a shape prop, no
  polygon-fill/rendering logic needed; (3) it keeps validation and any future
  overlap/collision checks simple (axis-aligned rectangle math vs.
  point-in-polygon); (4) it avoids pulling in PostGIS or a JSON polygon
  column + a geometry library on both backend and frontend for a feature
  that doesn't need arbitrary shapes. If a future gym genuinely needs an
  L-shaped or curved room, the fix is additive: a separate
  `room_shape_point` table (ordered vertices per room) behind the same
  `Room.id`, without touching the rectangle columns non-polygon rooms keep
  using.
- **Appointment-Room link is optional (nullable FK).** Mirrors the existing
  nullable `Appointment.trainer` - an appointment can be unassigned to a
  room. This also matters for backward compatibility: all appointments
  created before this phase have no room, and nothing forces one to be
  chosen going forward until a later phase adds that validation at the
  service layer.
- **Explicit `RoomCheckIn` entity, not derived-only occupancy.** Two
  occupancy signals are planned per the vision: (1) computed occupancy from
  appointments currently in progress in a room (derived at query time from
  `Appointment`/`ClientAppointment` - no new entity needed, since that data
  already exists), and (2) optional manual check-in. For (2), a real entity
  was added (`RoomCheckIn`, `checkedInAt`/`checkedOutAt`) instead of e.g. a
  single "current occupants" join table, specifically so check-in *history*
  survives past checkout (attendance patterns/analytics for the AI-insights
  feature) rather than only ever reflecting the current instant. A row with
  `checkedOutAt IS NULL` is the "currently inside" signal; the partial index
  `idx_room_check_in_open` is built specifically for that
  `WHERE checked_out_at IS NULL` query shape.
- **At most one active check-in per client, enforced globally, not
  per-room** (`V1.0015__enforce_single_active_check_in_per_client.sql`).
  A client is physically in exactly one place at a time, so "active" (i.e.
  `checkedOutAt IS NULL`) `RoomCheckIn` rows must be unique per `client_id`
  across the whole table, not per `(room_id, client_id)`. This is enforced
  with a `UNIQUE` partial index
  (`uq_room_check_in_one_active_per_client ON room_check_in (client_id)
  WHERE checked_out_at IS NULL`) rather than only at the service layer in a
  later phase, so the invariant holds even against a service bug or a
  concurrent double check-in race - the database rejects the second insert
  outright instead of silently allowing a client to appear "in" two rooms at
  once. `V1.0015` also drops the old non-unique
  `idx_room_check_in_client_open` index from `V1.0012` (added for the same
  "does this client have an open check-in" lookup) since the new unique
  index already serves that lookup and enforces the constraint besides -
  keeping both would have been redundant, not just extra-safe.
- **Body measurements: fixed columns, not JSON/EAV.** `ClientProgressEntry`
  has explicit nullable columns (`weightKg`, `bodyFatPercent`, `waistCm`,
  `chestCm`, `hipCm`, `thighCm`, `armCm`) rather than a flexible JSONB/
  key-value map. Reasoning: (1) nothing else in this codebase uses a JSON
  column or an EAV pattern - staying consistent with existing conventions
  mattered more than maximal flexibility; (2) typed numeric columns are
  directly chartable by Recharts on the frontend without a JSON-parsing/
  pivoting step; (3) MapStruct mapping and query/sorting stay trivial. The
  trade-off, made deliberately: adding a new measurement type later (e.g.
  calf circumference) needs a new migration + column + DTO field, not just a
  new JSON key. Given the small, well-known set of measurements a trainer
  actually tracks, that trade-off favors simplicity here.
- **Exercise as free text, not a catalog entity.** `ClientPersonalRecord.
  exerciseName` is a plain `varchar`, not a foreign key into an `Exercise`
  catalog table. A catalog would enable things like standardized units per
  exercise or cross-client leaderboards, but neither is in scope for the
  three planned features, and building a catalog (entity, seed data, and
  eventually an admin UI to manage it) is a project of its own relative to
  what a diplomski-scale system needs. Free text keeps this phase's surface
  area small; if a catalog is ever justified, the migration path is
  additive - add an `Exercise` table and an optional `exercise_id` FK
  alongside the existing text column, backfill by name match, without a
  breaking change.
- **`RecordUnit` as a fixed enum (`KG`/`LB`/`REPS`/`SECONDS`/`MINUTES`/
  `METERS`/`KM`)**, matching the existing convention of Java enums + a DB
  `CHECK` constraint (see `SessionType`, `WorkStatus`, etc.) rather than a
  free-text unit string, so a strength record ("100 KG") and a cardio/
  endurance record ("5 KM" or "90 SECONDS") are both representable without
  drifting into ad-hoc unit strings.
- **Existing `AppointmentDTO`/`AppointmentMapper` intentionally left
  untouched.** `Appointment.room` was added to the entity (and its Flyway/
  Envers migrations), but the read-side DTO was not updated to expose it.
  This phase is data-layer-only by design ("entiteti/repozitorijumi mogu se
  koristiti, ali ništa nije povezano u API") - wiring the room into the
  existing appointment API is left to whichever future phase actually builds
  the room-aware appointment endpoints, so as not to speculatively shape an
  API contract before its consumer exists.
- **New audit tables follow the existing hand-written pattern exactly**
  (`V1.0014__create_gym_upgrade_audit_tables.sql`): only entity-declared
  columns are mirrored into `*_aud` (no `version`/`created_at`/etc., matching
  every existing `*_aud` table), and the pre-existing `appointment_aud` table
  got an `ALTER TABLE ... ADD COLUMN room_id` in the same migration, per the
  "Audit" section's existing warning that Envers won't create this for you.
- Verified end-to-end on this branch: `mvn compile` succeeds, and a full
  `docker compose up` + `mvnw spring-boot:run` against a **fresh** Postgres
  volume applies all 15 migrations cleanly (`V1.0001`-`V1.0015`) and the app
  starts normally on port 8088 with Envers registering all new `@Audited`
  entities without error.

## Upgrade: service layer decisions

Phase 2 of the upgrade (`upgrade/claude-code` branch) wired the Phase 1 data
layer into services, controllers, and WebSocket - gym/room CRUD, room check-
in/occupancy, AI manager insights, and client progress-tracking CRUD +
narrative. No new migrations were needed (Phase 1's tables were sufficient).
This section documents the non-obvious decisions, since - like the schema
decisions above - it is comparison material for the thesis, not just an
internal note.

- **Gym/Room CRUD authorization: MANAGER writes, any authenticated role
  reads.** Only `MANAGER` can create/update the `Gym` config or
  create/update/delete `Room`s (`GymController`, `RoomController`) - editing
  the floor plan is a management action. Reads (`GET /api/gym`,
  `GET /api/gym/room`, `GET /api/gym/room/{id}`) are open to `MANAGER`,
  `TRAINER`, and `CLIENT` alike, matching the existing `@RoleRequired`
  pattern used elsewhere (e.g. `AppointmentController.getAvailable`) for
  data every role needs to see (trainers/clients need room names for
  check-in and the live floor-plan view).
- **`Gym` is upserted, not created via a normal POST.** Consistent with the
  Phase 1 decision that exactly one `Gym` row is expected in practice: there
  is a single `PUT /api/gym` that creates the row on the first call and
  updates it on every later call (`GymServiceImpl.upsertGym`), rather than a
  `POST`+`PUT{id}` pair that would require the frontend to already know
  whether a row exists. A future multi-location redesign would need a real
  per-row endpoint, but that is out of scope here.
- **Room check-in/check-out is a staff operation, not client self-service.**
  `POST /api/gym/room/{roomId}/check-in?clientId=...` and
  `POST /api/gym/check-in/{id}/check-out` are `MANAGER`/`TRAINER`-only, with
  the client passed explicitly as a parameter - modeled as a front-desk
  action (a staff member checks a client in), not a kiosk/self-service flow.
  The task brief didn't specify the actor; this was the more conservative
  reading given there's no existing self-service pattern (e.g. no client
  self-cancel-without-staff analog) to extend, and clients can still read
  their own room's occupancy per the read policy above.
- **The "one active check-in per client" violation is caught in two places,
  by design, and surfaces as a real error - not a silent 500.** The service
  pre-checks via `RoomCheckInRepository.findByClientIdAndCheckedOutAtIsNull`
  before inserting (the common, non-racing case), and separately catches
  `DataIntegrityViolationException` around the save (the Phase 1
  `uq_room_check_in_one_active_per_client` unique partial index rejecting a
  genuinely concurrent second check-in) - both paths throw the same
  `IllegalStateException`. Because this codebase has no global exception
  handler (see "Known issues"), that exception alone would fall through to a
  bare `{"status":500}` with no message - so `RoomCheckInController` locally
  catches `IllegalStateException` on check-in/check-out and returns
  `409 Conflict` with the message, the one place in this phase a controller
  does its own error translation, specifically because the task called for
  "a meaningful error" here. Every other new endpoint follows the existing
  codebase convention of letting service exceptions fall through unhandled.
- **Computed room occupancy combines two signals additively, without
  deduplication.** `RoomCheckInServiceImpl.toOccupancyDto` sums currently-
  active manual check-ins (`checkedOutAt IS NULL`) with clients on
  appointments currently in progress in that room (a new
  `AppointmentRepository` query: room + today's date + start/end time
  bracketing "now"). A client both manually checked in *and* on an
  in-progress appointment in the same room is counted twice - there is no
  entity linking a `RoomCheckIn` to an `Appointment`/`ClientAppointment` to
  de-duplicate against, and the task explicitly asked for "a combination" of
  the two signals rather than a deduplicated headcount. Revenue/attendance-
  grade exactness was judged less important here than keeping the two
  occupancy sources structurally independent (each can be reasoned about and
  tested on its own).
- **One WebSocket topic, `/topic/gym/occupancy`, carrying the full room list
  every time - not per-room topics, not deltas.** Chosen for the same reason
  the manager-insights/progress-narrative prompts avoid inventing structure
  the caller has to reverse-engineer: a floor-plan UI showing every room at
  once wants "the current state of the world," and a single full-snapshot
  message is trivial for a frontend store to apply wholesale (replace) with
  no merge logic, at the cost of a slightly larger payload than a per-room
  delta would be - a reasonable trade at gym-scale room counts. The message
  body is `List<RoomOccupancyDTO>`, the exact same shape returned by
  `GET /api/gym/occupancy`, so the initial-load HTTP call and every
  subsequent WebSocket update deserialize identically on the frontend.
- **Occupancy is broadcast two ways: event-driven and periodic, both to the
  same topic.** `RoomCheckInServiceImpl` pushes an update immediately after
  every check-in/check-out (so the staff action feels instant), and a new
  `OccupancyScheduler` (`@Scheduled(cron = "0 * * * * ?")`, modeled directly
  on the existing `NotificationScheduler` pattern) additionally broadcasts
  once a minute. The periodic sweep exists specifically for occupancy
  changes that aren't check-in/check-out-driven at all - an appointment
  starting or ending changes `appointmentOccupantCount` with no application
  event to hang a push off of, so without the sweep the live view would only
  update on the next unrelated check-in/check-out in that room, potentially
  hours later.
- **AI model choice: `claude-haiku-4-5`, not the platform-wide "always use
  the flagship model" default.** Both AI features (manager insights, client
  progress narrative) are single-turn, already-aggregated-data-in /
  short-text-out calls - closer to summarization/classification than to
  open-ended reasoning or agentic work - and are gated only by a cache TTL
  rather than by user action, meaning they can be called relatively often.
  That combination (low task complexity, moderate call frequency, cost-
  sensitive) is exactly the profile the cheapest current Claude model fits;
  see `ClaudeInsightServiceImpl` for the up-to-date model-selection
  reasoning (verified against current Anthropic documentation rather than
  assumed from training data, since model names/availability change).
- **Two Redis cache regions beyond the existing global `TRAINER_CACHE`
  default, each with a different invalidation strategy - not one shared AI
  cache.** `RedisConfig` adds `MANAGER_INSIGHTS_CACHE` (30 min TTL, longer
  than the 10 min global default: it summarizes slow-changing historical
  data - room check-in history, payments - and every miss is a paid Claude
  call, so a longer TTL directly cuts cost) and
  `CLIENT_PROGRESS_INSIGHT_CACHE` (kept at the 10 min global default, but
  invalidated explicitly - `ClientProgressEntryServiceImpl.create` evicts
  the cache entry for that client's id whenever a new progress entry is
  recorded, since a stale narrative that ignores a just-entered measurement
  is a worse failure mode than a few extra Claude calls). Manager insights
  has an additional `POST /api/insights/manager/refresh` that regenerates
  and re-populates the cache immediately (for "I just want to see the latest
  now" on the manager dashboard); the progress narrative relies on its
  automatic per-entry eviction instead; a fresh call after eviction
  naturally repopulates it.
- **`ManagerInsightsServiceImpl`/`ClientProgressInsightServiceImpl` avoid
  Spring AOP self-invocation caching bugs with two different, deliberate
  patterns** - worth documenting because getting this wrong silently breaks
  caching with no compile or runtime error. Manager insights uses
  `@Cacheable` on the public `getInsights()` method (always called through
  the Spring proxy from the controller) and a *separate* `refreshInsights()`
  method that evicts+regenerates+re-populates via an injected `CacheManager`
  directly, calling the same private `generateInsights()` helper as
  `getInsights()` - neither cached method ever calls the other internally.
  Client progress insight instead does the cache lookup/populate manually
  with `CacheManager` inside a single method (no `@Cacheable` annotation at
  all), specifically so `getMySummary()` can delegate to `getSummary(id)` as
  a plain Java call and still get caching, instead of needing to go back
  through the proxy (self-invocation on an annotated method silently skips
  the annotation - a well-known Spring AOP pitfall that produces no error,
  just a cache that never hits).
- **Manager-insights "revenue" is a paid-appointment-count proxy, not
  currency.** The schema has no per-session price field anywhere
  (`Session`/`Payment` have no `amount`/`price` column - see `Payment`/
  `Session` entities), so `ManagerInsightsServiceImpl` aggregates paid
  appointments purchased per `SessionType` from `Payment` and labels it
  explicitly as a proxy in both the code comment and the prompt sent to
  Claude ("proxy for revenue - the schema has no per-session price"), so the
  model doesn't fabricate a currency figure it was never given. Adding real
  pricing is a schema change and out of scope for this phase.
- **Client progress-tracking CRUD follows the existing trainer-writes/
  client-reads-own-data split**, mirroring `AppointmentController`'s
  `reserve`/`cancel` pattern for resolving "the current client" from the JWT
  (`SecurityContextHolder` + `Jwt.getClaim("email")` +
  `ClientRepository.findByUserEmail`) rather than introducing a new shared
  abstraction - kept consistent with how `AppointmentServiceImpl` already
  does this, duplication and all, per the existing convention.
- **Verified end-to-end on this branch**, against a fresh Postgres/Redis
  volume: `mvn compile` succeeds; the app starts cleanly; a full flow was
  exercised over HTTP - created a `Gym` and a `Room`, checked a client into
  the room, confirmed `GET /api/gym/occupancy` reflected it, confirmed a
  second check-in for the same client returns `409` with a message,
  checked the client back out, confirmed occupancy returned to zero, and
  created a `ClientProgressEntry` and a `ClientPersonalRecord`. **Not
  verified**: an actual Claude API response. `ANTHROPIC_API_KEY` was not set
  in the session this branch was developed in, so
  `GET /api/insights/manager` was only confirmed to fail with the intended,
  explicit `IllegalStateException` ("ANTHROPIC_API_KEY is not set...") - not
  to fail silently, not mocked - rather than to actually call Claude; the
  request/response wiring against the Anthropic SDK could not be exercised
  live. Whoever has a real key configured should do that one check before
  relying on this phase's AI features in anger.

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
  refresh endpoint, whose point is normally to work *after* expiry.
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
- No global exception handler - error responses aren't a consistent JSON
  shape yet.
- `pom.xml` sets `maven.test.skip=true`; combined with there being effectively
  only one trivial `contextLoads()` test, there is no real test coverage and
  tests don't run by default even if written.
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
- 2026-08-04: Upgrade Phase 1, data layer only (`upgrade/claude-code` branch).
  Added `Gym`, `Room`, `RoomCheckIn`, `ClientProgressEntry`,
  `ClientPersonalRecord` entities + repositories + DTOs + MapStruct mappers,
  an optional `Appointment.room` link, and Flyway migrations `V1.0011`-
  `V1.0014` (new tables + matching Envers audit tables). No services,
  controllers, WebSocket wiring, or LLM calls added - see "Upgrade: schema
  decisions" above for every design choice and its rationale. Verified with a
  full migration run against a fresh Postgres volume and a normal app
  startup; no existing files' behavior changed.
- 2026-08-05: Upgrade Phase 2, service layer + API (`upgrade/claude-code`
  branch). Wired Phase 1's tables into gym/room CRUD, room check-in/
  occupancy (with WebSocket broadcast on `/topic/gym/occupancy`, both event-
  driven and via a new `OccupancyScheduler`), AI manager insights, and
  client progress-tracking CRUD + AI narrative summary - see "Upgrade:
  service layer decisions" above for every design choice and its rationale.
  Added the `anthropic-java` SDK dependency and `AnthropicConfig`. No new
  migrations needed. Verified end-to-end over HTTP against a fresh Postgres/
  Redis volume (gym/room CRUD, check-in/check-out including the 409 conflict
  path, occupancy computation, progress entry/record creation); the AI
  endpoints' Claude API call itself was not exercised live -
  `ANTHROPIC_API_KEY` was unset in the development session, so only the
  intended explicit failure path was confirmed (see "Upgrade: service layer
  decisions" for detail). No existing files' behavior changed.

## Imported Claude Cowork project instructions
