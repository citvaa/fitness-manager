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
- **A TRAINER can only view/record progress data for a client they have
  actually trained - not any client by id.** This was missed in the initial
  Phase 2 implementation: `@RoleRequired({"MANAGER", "TRAINER"})` alone only
  checks the caller's *role*, not their relationship to the specific
  `clientId` in the request, so any trainer could read or write any
  client's `ClientProgressEntry`/`ClientPersonalRecord`/AI narrative via
  the `/client/{clientId}` endpoints - a real authorization gap, not a
  style nitpick. Fixed with a new `TrainerClientAccessGuard`
  (`com.example.demo.security`), called from `ClientProgressEntryServiceImpl
  .create`/`.getForClient`, `ClientPersonalRecordServiceImpl.create`/
  `.getForClient`, and `ClientProgressInsightServiceImpl.getSummary` (the
  AI narrative endpoint has the exact same gap and was fixed alongside the
  two CRUD services, even though only those two were flagged - the check is
  identical regardless of what's being read/written for the client).
  MANAGER is exempt (checked via the `roles` JWT claim) and can access any
  client, matching every other MANAGER-vs-TRAINER split in this codebase.
  - **"Has trained" is derived from `Appointment`/`ClientAppointment`
    history** (`ClientAppointmentRepository
    .existsByClientIdAndAppointmentTrainerId`) - a client is "the trainer's"
    if they've shared at least one appointment where that trainer was
    assigned, exactly how a trainer is already linked to a client
    everywhere else in this codebase (there is no separate, explicit
    trainer-client assignment table, and adding one would be a schema
    change and a bigger behavior change than this fix warrants). This is
    permanent, not "currently scheduled" - once a trainer has trained a
    client, they keep access to that client's progress history, which
    matches how coaching relationships and historical record-keeping
    actually work (a trainer who worked with a client last month should
    still be able to see the progress they recorded).
  - **A `@Component`, not a service interface + impl.** `TrainerClientAccessGuard`
    is cross-cutting infrastructure (an authorization check reused by three
    otherwise-unrelated services), not a business-domain service - it
    follows the existing `JwtUtil`/`JsonUtil` pattern of a concrete,
    directly-injected helper class rather than forcing an interface split
    that has no second implementation.
  - **Not applied inside the `getMine()`/`getMySummary()` CLIENT-facing
    methods**, which resolve "the current client" from the JWT and read the
    repository/cache directly instead of delegating to the guarded
    `getForClient(clientId)`/`getSummary(clientId)` methods - calling
    through the guard there would misread a CLIENT caller as a TRAINER (no
    `Trainer` row for that email) and incorrectly reject their own data.
    This meant duplicating the one-line repository call in each service
    instead of reusing the "public" method - a small, deliberate trade
    for correctness over avoiding duplication, consistent with this
    codebase's existing tolerance for that kind of duplication (see the
    JWT-extraction duplication note above).
  - **Verified with a real login/token per role**: a trainer with a shared
    appointment with the client succeeds (`200`); a second trainer with no
    such history is rejected (`403`) on the entry, record, and insight
    endpoints alike; a manager succeeds regardless; the client's own
    `/me` endpoints are unaffected.
- **Verified end-to-end on this branch**, against a fresh Postgres/Redis
  volume: `mvn compile` succeeds; the app starts cleanly; a full flow was
  exercised over HTTP - created a `Gym` and a `Room`, checked a client into
  the room, confirmed `GET /api/gym/occupancy` reflected it, confirmed a
  second check-in for the same client returns `409` with a message,
  checked the client back out, confirmed occupancy returned to zero, and
  created a `ClientProgressEntry` and a `ClientPersonalRecord`. The AI
  endpoints were also verified with a **real** Claude API call once
  `ANTHROPIC_API_KEY` was available: `GET /api/insights/manager`,
  `POST /api/insights/manager/refresh`, and `GET /api/progress/insight/
  client/{id}` all returned `200` with genuinely model-generated prose
  grounded in the actual aggregated numbers (e.g. the progress narrative
  correctly cited the exact weight/body-fat/bench-press figures just
  entered) - not a mock or placeholder - and `claude-haiku-4-5` was accepted
  by the API with no invalid-model error, confirming it's a currently valid
  model id. The first verification attempt failed with the intended,
  explicit `IllegalStateException` ("ANTHROPIC_API_KEY is not set...")
  rather than silently mocking or crashing the app - the root cause turned
  out to be that the terminal session running the app had `.env` sourced
  only partially (`MAIL_*`/`JWT_SECRET` exported manually, not `.env`
  itself), so `ANTHROPIC_API_KEY` was simply never in that process's
  environment; re-running with `.env` actually `source`d fixed it
  immediately, no code change needed.

## Upgrade: frontend decisions

Phase 3 of the upgrade (`upgrade/claude-code` branch) built the actual
`Frontend/` app from scratch against the Phase 2 API: auth, role-based
routing/shell, the manager room editor, and the live gym floor plan. This
section documents the non-obvious decisions, same spirit as the two sections
above - comparison material for the thesis, not just an internal note.

- **React + TypeScript + Vite, not another framework.** Chosen specifically
  because the floor-plan editor (item 4/5 of this phase) needs a 2D
  canvas-with-drag/resize/rotate library, and `react-konva` (a React
  wrapper around Konva) has first-class support for exactly the rectangle-
  with-rotation shape `RoomDTO` already models (see "Upgrade: schema
  decisions" - the rectangle-not-polygon choice pays off directly here: no
  polygon math needed on the frontend either). Vite over Create React App/
  Next.js because this is a pure client-rendered SPA talking to an existing
  REST+WebSocket backend - no server-side rendering or file-based routing
  requirement that would justify Next.js's extra weight.
- **Tailwind CSS v4 (`@tailwindcss/vite`), not MUI/shadcn/Chakra.** The live
  floor-plan view (the "wow" screen) needs bespoke per-room tiles whose
  color/glow/pulse are driven by live numeric data (occupancy percent,
  at-capacity flag) - that's easier to hand-roll with utility classes and
  inline computed styles than to fight a component library's theming API
  for. A component library would have paid off more for form-heavy CRUD
  screens (which this phase also has - the room settings sidebar), but one
  consistent styling approach across the whole app was judged more valuable
  than mixing Tailwind for the canvas screen with a component library
  elsewhere.
- **Zustand for auth state, not Redux/Context.** The only genuinely global,
  cross-route client state in this phase is the auth session (tokens, decoded
  user, active role) - a single small store (`auth/store.ts`) with no
  reducers/actions boilerplate fits that better than Redux, and avoids the
  re-render-the-whole-tree cost of a plain Context provider wrapping the
  router (every token refresh would otherwise re-render every consumer).
  Feature-local state (room list, selected room, occupancy snapshot) is
  plain `useState`/`useEffect` in the owning component - no global store for
  data that only one screen ever reads.
- **JWT decoded client-side (`jwt-decode`), not fetched from a `/me`
  endpoint.** The access token already carries `sub`/`email`/`roles` (see
  `JwtUtil` in the Auth flow section above) - decoding it locally avoids an
  extra round-trip on every login/refresh and matches how the backend
  itself treats the token as the source of truth for identity/roles
  (`RoleInterceptor` reads the same claim). No backend endpoint needed to be
  added for this.
- **Silent refresh: proactive timer + reactive 401 fallback, both wired to
  the same `refreshAccessToken()` call, deduplicated with a single in-flight
  promise.** `auth/RefreshScheduler.tsx` decodes the access token's `exp`
  and schedules a refresh ~60s before it expires (access tokens live 15 min
  - see the Auth flow section) so a live WebSocket-driven screen never
  visibly hits a 401. `lib/http.ts`'s axios response interceptor is the
  fallback for the case the proactive timer didn't fire yet (e.g. the tab
  was suspended) - it retries the failed request once after a refresh. Both
  paths call the same `refreshAccessToken()`, which caches its in-flight
  promise so concurrent 401s from multiple simultaneous requests trigger
  exactly one `POST /api/user/login-refresh` call, not one per failed
  request.
- **Multi-role handling: a role *switcher* in the sidebar, not simultaneous
  merged views.** A `User` can hold multiple `UserRole`s (see the Domain
  model section above). Rather than trying to merge MANAGER+TRAINER+CLIENT
  content into one screen, the shell exposes one "active role" at a time
  (`auth/store.ts` `activeRole`, persisted only for the session) with a
  switcher shown only when `user.roles.length > 1`; routes are gated by
  active role (`RequireActiveRole`), and each role's nav items and home
  route are looked up from that single active role. Default active role on
  login follows a fixed priority (MANAGER > TRAINER > CLIENT) - reasoning:
  if someone holds the MANAGER role at all, the management screens are
  almost certainly what they logged in to do; a manager who's also a
  trainer isn't disadvantaged since they can switch instantly, and this
  avoids either an extra "pick your role" screen on every login or an
  arbitrary alphabetical default.
- **`app.cors.allowed-origins` unchanged (`http://localhost:5173`).** Vite's
  default dev port was used as-is, so no `application.yaml` edit was needed
  this phase - noted here per the task brief in case a future phase changes
  the frontend's dev port and needs to update that value too.
- **Manager room editor: canvas units are treated as meters, rendered at a
  fixed 20px/unit scale (`PX_PER_UNIT`).** `RoomDTO`'s `posX/posY/width/
  height` are unitless `double precision` columns (see "Upgrade: schema
  decisions") - the frontend imposes "meters" as the working unit purely for
  human-readable labels in the editor sidebar (e.g. "Š: 6.0m"); nothing
  backend-side enforces or assumes a unit, so this is a presentation-layer
  convention only, easy to change without a migration if a future phase
  wants a different scale or unit.
- **Room editor persists on drag-end/transform-end, not on every mouse-move
  frame, and optimistically updates local state before the API call
  resolves.** Konva's `onDragEnd`/`onTransformEnd` fire once per gesture,
  not per frame, so this is naturally debounced without extra code; the
  local `rooms` state is updated synchronously in the same handler that
  fires the `PUT /api/gym/room/{id}` call, so the shape doesn't visually
  snap back while the request is in flight. No optimistic-rollback-on-error
  handling was added (a failed save just leaves the UI ahead of the DB until
  the next reload) - acceptable for a diplomski-scale internal tool talking
  to a backend on the same machine/network, revisit if this ever needs to
  tolerate a flaky connection.
- **Live floor plan renders with plain positioned `<div>`s + CSS
  transitions, not a second Konva canvas.** The editor needs Konva for
  interactive drag/resize/rotate; the live view is read-only and specifically
  wants smooth *color* transitions on occupancy change (the "wow" requirement
  in the task brief) - CSS `transition-colors`/`box-shadow`/`animate-pulse`
  on absolutely-positioned divs gets that essentially for free, whereas
  animating a Konva `Rect`'s fill smoothly needs manual `Konva.Tween`s. Using
  two different rendering approaches for two screens that share the same
  `RoomDTO` geometry was a deliberate trade: simpler code per screen, at the
  cost of not sharing a single "draw a room" component between them.
- **Occupancy color thresholds are a frontend-only convention** (0% grey,
  >0% green, ≥60% amber, `atCapacity`/100% red, matching the legend shown on
  the live view) - `RoomOccupancyDTO` has no status enum (see "Upgrade:
  service layer decisions" - it's plain counts/percent/boolean), so these
  thresholds live in `LiveFloorPlanPage.tsx` only and can be retuned without
  touching the backend.
- **WebSocket client uses `@stomp/stompjs` with a raw `ws://` `brokerURL`,
  no SockJS fallback.** `WebSocketConfig` registers `/ws` without
  `.withSockJS()` (plain STOMP-over-WebSocket only), so the client connects
  directly rather than going through the SockJS handshake/fallback protocol
  - `sockjs-client` was installed anticipating this but turned out to be
  unnecessary once the actual endpoint registration was checked; left as an
  unused dependency rather than removed, low priority to clean up.
- **Occupancy WebSocket payload is parsed with a single `JSON.parse`.**
  `NotificationServiceImpl.sendGymOccupancyUpdate` sends
  `JsonUtil.convertToJson(occupancies)` as the STOMP frame body - i.e. the
  list is serialized to a JSON string once server-side and that string *is*
  the frame body, not a double-encoded string - so the client does exactly
  one `JSON.parse(message.body)` to get the `RoomOccupancyDTO[]` array.
  Verified against a real check-in/check-out over the WebSocket during this
  phase's manual testing (see below), not just inferred from the source.
- **No client-side check-in/check-out UI built in this phase.** The task
  brief explicitly scoped verification of the live view to "manually check
  in via Swagger/curl while watching the frontend" - front-desk check-in is
  a `MANAGER`/`TRAINER` action per the Phase 2 decision, but building that
  screen wasn't asked for in Phase 3 and was left out to keep this phase
  focused on the floor-plan editor and live view specifically.
- **TRAINER and CLIENT areas are single placeholder screens
  (`TrainerPlaceholderPage`/`ClientPlaceholderPage`)** - by explicit
  instruction, all UI effort in this phase went into the MANAGER area
  (editor + live view). Both placeholders are already wired into the
  role-gated routing (`RequireActiveRole`) so the *routing* shape - what
  role sees what area - is real and won't need rework when their actual
  content is built in the client-progress-tracking phase; only the page
  bodies are stubs.
- **Dev-seed data (`db/dev-data/V1.0016__insert_dev_gym_and_rooms.sql`)
  guards every insert with `WHERE NOT EXISTS`, unlike `V1.0009`'s
  unconditional inserts.** Necessary because, unlike the user-seed data
  `V1.0009` adds (which only ever runs once against an empty table set),
  this phase's manual testing routinely creates a `Gym`/`Room` through the
  editor UI *before* this migration might run against that same database -
  an unconditional insert would leave a duplicate `Gym` row (schema allows
  it; nothing enforces the "exactly one row" expectation at the DB level,
  see "Upgrade: schema decisions"). The guard makes the migration a no-op on
  a database that already has gym data and a real seed on a fresh one.
  Verified both ways: applied cleanly against this phase's already-populated
  dev database (correctly skipped both inserts, existing `Test Gym`/
  `Studio A` untouched) - a fresh-volume run was not re-verified in this
  phase since the existing dev Postgres volume had test data from Phase 2
  that was more valuable to keep than to discard for a from-scratch replay.
- **Verified end-to-end in this phase**: `mvn compile`/`tsc -b` both clean;
  backend restarted against the existing dev Postgres/Redis volume with the
  `login-refresh` fix and the new dev-seed migration (applied cleanly, see
  above); logged in through the actual UI as the dev `admin` (MANAGER)
  account; created a room in the editor via drag-to-create, dragged it,
  confirmed the new position persisted across a page reload, then deleted
  it; confirmed no console errors on either manager screen; and - the core
  "wow" check - checked a client into a room via `curl` while the live
  floor-plan page was open and watched the tile flip from grey/0 to green/
  1 with no page reload, then checked out and watched it flip back, purely
  from the `/topic/gym/occupancy` WebSocket push (confirmed both an
  HTTP-loaded initial snapshot and a WebSocket-delivered live update render
  identically, as intended). The manual testing above initially used an
  ad-hoc `UPDATE "user" SET password = ...` against the running dev database
  to get a known password for the `admin` account, since V1.0002/V1.0009's
  bcrypt hashes have no documented plaintext anywhere in the repo - flagged
  in this file at the time as a shortcut needing a proper fix, and fixed
  immediately after in the same session (see the next entry below) instead
  of being left as a follow-up.
- **Known dev test passwords, done properly as a migration** (`V1.0017__
  set_known_dev_test_passwords.sql`), replacing the ad-hoc DB `UPDATE` above.
  Sets the bcrypt hash of a single documented password (`password123`) for
  all three V1.0009 seeded accounts (`admin`/MANAGER, `ogi`/TRAINER,
  `citva`/CLIENT) so they're usable immediately after any reset, not just in
  whatever database happened to have the manual `UPDATE` applied to it. A
  new file rather than editing `V1.0002`/`V1.0009` directly, per the
  "don't edit existing migrations" rule - Flyway checksums those as already
  applied. Plaintext credentials are documented in `Frontend/README.md` and
  the root `README.md`, both explicitly labeled dev-only/never-production.
  **Verified against a genuinely fresh volume this time** (the gap called
  out above): `docker compose down`, deleted `Docker/postgres_data/pgdata`,
  `docker compose up -d`, started the backend - Flyway applied all 17
  migrations from empty in one run (confirmed in the startup log, ending at
  `v1.0017`) - and all three accounts (`admin`/`ogi`/`citva`, password
  `password123`) returned `200` from `POST /api/user/login` immediately
  after, with no manual database step in between.

Phase 4 of the upgrade (`upgrade/claude-code` branch) filled in the two
placeholder role areas and added the MANAGER AI-insights screen: manager AI
insights (new screen), trainer progress-tracking (replacing
`TrainerPlaceholderPage`), and read-only client progress-tracking (replacing
`ClientPlaceholderPage`). Same spirit as the sections above - documenting the
non-obvious decisions as comparison material.

- **One backend change, scoped exactly as the task brief allowed: `GET
  /api/trainer/me/clients`.** No "my clients" endpoint existed anywhere
  (checked `TrainerController`/`ClientController`/`AppointmentController`
  before adding anything). Added `TrainerService.getMyClients()` +
  `ClientAppointmentRepository.findDistinctClientsByAppointmentTrainerId`,
  reusing the exact trainer-from-JWT-email pattern already used by
  `AppointmentServiceImpl`/`TrainerClientAccessGuard`, and returning the
  existing lightweight `ClientSummaryDTO` (`{id, email}`) rather than the
  heavier `ClientDTO` - the trainer progress screen only needs an id to pass
  to the existing `/client/{clientId}` progress endpoints and a label to
  show in the sidebar list, so the heavier DTO (payments, session trackings,
  appointments) would have been unused payload. `@RoleRequired("TRAINER")`
  only (not `MANAGER` too) - a manager isn't "a trainer" and has no
  equivalent "my clients" concept; the manager area has no analogous list
  screen in this phase.
  - **Hit and fixed a real PostgreSQL-specific bug while verifying this
    endpoint, not just a Java compile error.** The first version of the
    query (`select distinct ca.client from ClientAppointment ca ... order by
    ca.client.id`) compiled fine and passed `mvn compile`, but failed at
    runtime with `ERROR: for SELECT DISTINCT, ORDER BY expressions must
    appear in select list` - Hibernate translated `ca.client.id` to the
    `client_appointment.client_id` join column, which isn't part of the
    projected column list when the select list is the joined `Client`
    entity's own columns (PostgreSQL requires exact expression identity
    between `SELECT DISTINCT` and `ORDER BY`, even when the two columns hold
    provably identical values). Fixed by selecting `Client` as the root
    (`select distinct c from Client c join c.clientAppointments ca where
    ca.appointment.trainer.id = :trainerId order by c.id`) so the order-by
    expression is unambiguously the same column as the projection. Caught
    only because this session actually ran the endpoint against Postgres
    during manual QA rather than stopping at a clean compile - worth calling
    out since it's exactly the kind of bug `mvn compile`/unit tests without
    a real database would never surface.
- **AI insights screen renders the narrative as plain paragraphs split on
  newlines, not as parsed Markdown.** `ManagerInsightsDTO.insightText` (and
  `ClientProgressInsightDTO.narrative`, reused by the same rendering
  approach on both progress screens) is a Claude-generated prose string with
  no guaranteed Markdown syntax - the system prompts (see
  `ClaudeInsightServiceImpl`/`ManagerInsightsServiceImpl`) ask for plain
  readable text, not Markdown. Splitting on blank lines into `<p>` tags is
  therefore both simpler and more correct than pulling in a Markdown
  renderer dependency (`react-markdown` or similar) for a format the
  backend was never asked to produce.
- **"Regeneriši"/"Osveži" behave differently on the two AI screens, and this
  is a deliberate consequence of the Phase 2 caching design, not an
  inconsistency to fix.** The manager insights screen's "Regeneriši" button
  calls the real force-refresh endpoint (`POST /api/insights/manager/
  refresh`, `MANAGER_INSIGHTS_CACHE`'s manual evict+repopulate path) and is
  guaranteed to show newly generated text. The progress-narrative panel's
  "Osveži" button (shared by both the trainer and client progress screens)
  has no equivalent backend endpoint to call - per the Phase 2 design,
  `CLIENT_PROGRESS_INSIGHT_CACHE` is invalidated only as a side effect of
  `ClientProgressEntryServiceImpl.create` (a new measurement), not on
  personal-record creation and not on demand - so "Osveži" there just
  re-fetches `GET /api/progress/insight/...`, which can legitimately return
  the same cached text if nothing has evicted it yet. Verified this exact
  behavior during manual QA: adding a progress entry for a client produced a
  freshly generated narrative that referenced the new measurements; adding a
  personal record immediately afterward and clicking "Osveži" correctly
  still showed the pre-record narrative (10-minute TTL not yet expired,
  record creation doesn't evict) - this is the documented Phase 2 trade-off
  surfacing in the UI, not a bug in this phase's frontend work. Adding a
  force-refresh endpoint for progress insights (mirroring the manager one)
  would be a reasonable follow-up but is a backend behavior change beyond
  what this phase's task brief scoped as "the existing endpoint."
- **Trainer and client progress screens share every display component
  (`ProgressCharts`, `PersonalRecordsList`, `InsightPanel`) and only the
  page-level component differs** (`TrainerProgressPage` adds the client
  picker sidebar and the two input forms; `ClientProgressPage` is read-only
  and resolves "which client" from the JWT via the existing `/me` endpoints
  instead of a `clientId` prop) - same reasoning as the Phase 2 backend's
  read-DTO/write-request split: keep the read-side rendering identical
  regardless of who's looking, and isolate the role difference to
  presence/absence of the write forms, rather than forking the chart/list
  components per role.
- **Two Recharts `LineChart`s per client, not one.** `weightKg`/
  `bodyFatPercent` (roughly 0-100 in magnitude) and the five circumference
  measurements (roughly 20-120 cm) share plausible Y-axis ranges with each
  other but would visually flatten out a single combined chart's scale
  differences less usefully than two purpose-grouped charts ("Telesna masa i
  procenat masti" / "Obimi tela"); this also keeps the legend for each chart
  short enough to read at a glance. `connectNulls` is set on every line
  since any of the seven `ClientProgressEntryDTO` measurement fields can be
  `null` per entry (see the Phase 1 schema decision to keep them nullable
  columns) - without it, a single missing measurement on one date would
  break the line into disconnected segments either side of that point.
- **Personal records: a flat exercise-name list, not grouped/charted per
  exercise.** `ClientPersonalRecordDTO.exerciseName` is free text (see the
  Phase 1 schema decision), so there's no fixed vocabulary to group or chart
  against - a client doing "Bench press" once and "5km run" once would
  produce two incomparable single-point series if charted. A simple
  reverse-chronological list (exercise, value+unit, date, notes) was judged
  more honest to the data shape than a chart implying trend data that isn't
  there yet for most exercises.
- **Dev-data migration `V1.0018__insert_dev_client_trainer_appointment.sql`**
  adds one `Appointment` + `ClientAppointment` row linking the seeded `ogi`
  (TRAINER) and `citva` (CLIENT) accounts, guarded with `WHERE NOT EXISTS`
  per the `V1.0016` precedent. Without this, a freshly cloned dev database
  has trainer `ogi` with zero clients in "Moji klijenti" until a real
  appointment/reservation flow is exercised by hand first - this migration
  makes the trainer progress screen immediately testable after a fresh
  clone, the same motivation as `V1.0016` did for the floor plan. Looked up
  by email (`WHERE u.email = 'ogi'`/`'citva'`) rather than hardcoding ids 1/1
  so it stays correct even if row insertion order ever changes.
  During this phase's own manual QA, the link was first created with an
  ad-hoc direct `INSERT` against the running dev database (to unblock
  testing immediately) and then replaced with this proper guarded migration
  in the same session, the same "shortcut now, fix immediately after"
  pattern the Phase 3 password fix used - verified that re-running the app
  against the now-populated database applied `V1.0018` as a clean no-op
  (Flyway log: "Successfully applied 1 migration ... now at version
  v1.0018"), i.e. the guard correctly detected the existing row and skipped
  the insert.
- **Verified end-to-end in this phase**: `mvn compile` clean; `tsc -b` clean
  with zero errors; backend restarted against the existing dev Postgres/
  Redis volume with `ANTHROPIC_API_KEY` exported from `.env`. Manual QA over
  the real running app (screenshots in `docs/browser-qa/phase4-*.jpg`):
  logged in as `admin` (MANAGER), opened the new "AI uvid" screen, confirmed
  it rendered a genuinely Claude-generated narrative grounded in real
  check-in/payment data with a real `generatedAt` timestamp, then clicked
  "Regeneriši" and confirmed (via `read_network_requests`) a real `POST
  /api/insights/manager/refresh` fired and returned a new timestamp and
  freshly generated text. Logged in as `ogi` (TRAINER), confirmed "Moji
  klijenti" listed `citva` (after adding/fixing the dev-data migration
  above), entered a real progress measurement and a real personal record
  through the form, and confirmed both immediately appeared in the charts/
  list without a page reload. Logged in as `citva` (CLIENT), confirmed the
  read-only screen showed the exact same measurement/record/AI-narrative
  data the trainer had just entered, with no input forms present. No
  existing files' runtime behavior changed other than the new
  `/api/trainer/me/clients` endpoint.
- **Follow-up fix: both AI prompts now explicitly ask Claude to respond in
  Serbian.** Neither `ManagerInsightsServiceImpl.SYSTEM_PROMPT` nor
  `ClientProgressInsightServiceImpl.SYSTEM_PROMPT` said anything about
  output language, and Claude defaulted to English - jarring next to a UI
  that's entirely Serbian. Added one line to each prompt ("Respond in
  Serbian (srpski jezik) - the rest of the application's UI is in Serbian,
  so the summary must be too") rather than post-processing/translating the
  response client-side or server-side, since the model can write natural
  Serbian directly and translating a second time would be both slower and
  lossier. Verified with real regenerate calls on both endpoints after the
  prompt change - `POST /api/insights/manager/refresh` and a cache-expired
  `GET /api/progress/insight/client/{id}` both returned genuinely Serbian
  prose (screenshots `docs/browser-qa/phase4-05-*`/`phase4-06-*`). No DTO or
  frontend change needed - `insightText`/`narrative` were always
  freeform strings.

## Known issues (intentionally not fixed in the baseline-hygiene session)

These were found during the repo-hygiene pass that produced `baseline-v1`.
They are documented here rather than fixed because fixing them changes
runtime behavior, and the goal of that session was a stable, unchanged-behavior
baseline - not new features or behavior changes. They are fair game for
either upgrade session to pick up:

- `forgot-password`/`reset-password` endpoints are not excluded from
  `JwtInterceptor`/`RoleInterceptor`, effectively making them unreachable
  without an existing valid JWT.
- ~~`POST /api/user/login-refresh` is also not excluded from `JwtInterceptor`~~
  **Fixed 2026-08-06** (`upgrade/claude-code` branch, frontend-upgrade
  session): added `/api/user/login-refresh` to the `excludePathPatterns` list
  for both `JwtInterceptor` and `RoleInterceptor` in `WebConfig`, same
  pattern as `/api/user/login`. This endpoint's entire purpose is to work
  *after* the access token has expired, so requiring a currently-valid
  access token to reach it was self-defeating - the frontend's silent-
  refresh flow depends on this working. Verified with `curl`: logged in,
  then called `login-refresh` with only the refresh token (no
  `Authorization` header at all) and got `200` with a fresh access token,
  where it would previously have 401'd.
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
  path, occupancy computation, progress entry/record creation), and, once
  `ANTHROPIC_API_KEY` was supplied, with real Claude API calls against all
  three AI endpoints returning genuine model-generated text grounded in the
  actual data - `claude-haiku-4-5` confirmed as a currently valid model id
  in the process (see "Upgrade: service layer decisions" for detail). No
  existing files' behavior changed.
- 2026-08-05: Fixed a real authorization gap in Phase 2's client
  progress-tracking endpoints (`upgrade/claude-code` branch): a TRAINER
  could read/write any client's progress data by `clientId`, since
  `@RoleRequired` only checked role, not trainer-client ownership. Added
  `TrainerClientAccessGuard` (derives "has this trainer trained this
  client" from `Appointment`/`ClientAppointment` history) and wired it into
  `ClientProgressEntryServiceImpl`, `ClientPersonalRecordServiceImpl`, and
  `ClientProgressInsightServiceImpl` - see "Upgrade: service layer
  decisions" for the full rationale. Verified with real logins across
  MANAGER, a trainer who has trained the client, and a trainer who hasn't
  (`403`). No migrations, no behavior change for MANAGER or CLIENT callers.
- 2026-08-06: Upgrade Phase 3, frontend (`upgrade/claude-code` branch).
  First fixed a real bug ahead of the frontend work: `POST /api/user/
  login-refresh` was unreachable after access-token expiry because it
  wasn't excluded from `JwtInterceptor`/`RoleInterceptor` (see "Known
  issues" - now fixed there). Then scaffolded `Frontend/` from scratch
  (React + TypeScript + Vite, Tailwind CSS v4, Zustand, react-router-dom,
  react-konva, `@stomp/stompjs`): login screen against `/api/user/login`,
  token storage with a proactive silent-refresh timer plus a reactive
  401 fallback, protected routes, a role-gated shell with a role switcher
  for multi-role accounts, a MANAGER 2D room editor (drag/resize/rotate via
  react-konva, full CRUD against the Phase 2 Room API), and the live gym
  floor-plan view (HTTP initial snapshot + `/topic/gym/occupancy` WebSocket
  updates, CSS-animated occupancy coloring) - see "Upgrade: frontend
  decisions" above for every design choice and its rationale. Added
  dev-seed data (`V1.0016`, guarded with `WHERE NOT EXISTS`) so a fresh dev
  database has a starter gym/room layout. TRAINER/CLIENT areas are routed
  but only placeholder screens, per the phase's explicit scope. Verified
  end-to-end against the existing dev Postgres/Redis volume: `mvn compile`
  and `tsc -b` both clean, logged into the real running frontend as the dev
  `admin` (MANAGER) account, created/dragged/deleted a room in the editor
  with persistence confirmed across a reload, and confirmed a real
  check-in/check-out via `curl` updates the live floor-plan view instantly
  over the WebSocket with no page reload and no console errors.
- 2026-08-06: Follow-up to Phase 3 (`upgrade/claude-code` branch) - replaced
  the ad-hoc manual-DB-`UPDATE` password fix mentioned above with a proper
  dev-data migration, `V1.0017__set_known_dev_test_passwords.sql`, setting
  a documented known password (`password123`) for the three V1.0009 seeded
  accounts (`admin`/MANAGER, `ogi`/TRAINER, `citva`/CLIENT). Documented in
  `Frontend/README.md` and the root `README.md`, explicitly dev-only.
  Verified against a genuinely fresh Postgres volume this time (`docker
  compose down`, deleted `Docker/postgres_data/pgdata`, `docker compose up
  -d`, started the backend): all 17 migrations applied from empty in one
  run, and all three accounts logged in successfully immediately after with
  no manual database step. See "Upgrade: frontend decisions" above for the
  full rationale.
- 2026-08-06: Upgrade Phase 4 (`upgrade/claude-code` branch) - filled in the
  two remaining placeholder role areas and added the MANAGER AI-insights
  screen: a new `GET /api/insights/manager`-backed "AI uvid" page with a
  working "Regeneriši" force-refresh button; a real trainer progress-
  tracking screen (`TrainerProgressPage`, replacing `TrainerPlaceholderPage`)
  with a client list, new-measurement/new-personal-record forms, Recharts
  progress charts, a records list, and the AI narrative summary; and a
  read-only client progress-tracking screen (`ClientProgressPage`, replacing
  `ClientPlaceholderPage`) showing the same data via the `/me` endpoints. One
  scoped backend addition: `GET /api/trainer/me/clients` (plus the dev-data
  migration `V1.0018` seeding one trainer-client appointment link so the
  screen has data immediately on a fresh clone) - see "Upgrade: frontend
  decisions" above for every design choice, the PostgreSQL `SELECT DISTINCT`/
  `ORDER BY` bug hit and fixed while verifying the new endpoint, and the full
  manual QA record (`docs/browser-qa/phase4-*.jpg`). Added `recharts` as a
  new frontend dependency. `mvn compile` and `tsc -b` both clean; verified
  end-to-end against the existing dev Postgres/Redis volume with a real
  `ANTHROPIC_API_KEY`-backed Claude call for both AI screens (manager
  insights force-refresh and the trainer/client progress narrative). No
  existing files' runtime behavior changed other than the new endpoint.
- 2026-08-06: Follow-up to Phase 4 (`upgrade/claude-code` branch) - both AI
  prompts (`ManagerInsightsServiceImpl`, `ClientProgressInsightServiceImpl`)
  were generating English text despite the rest of the UI being Serbian.
  Added an explicit "respond in Serbian" instruction to each system prompt -
  see "Upgrade: frontend decisions" above. Verified with real regenerate
  calls on both endpoints; screenshots in `docs/browser-qa/phase4-05-*`/
  `phase4-06-*`. No DTO/frontend changes needed.

## Imported Claude Cowork project instructions
