# AGENTS.md

## Purpose of this repository

`fitness-manager` is a full-stack gym-management app: a Spring Boot backend (clients, trainers,
appointments/schedules, payments, notifications, a live gym floor plan, AI-generated manager
insights, and client progress tracking) plus a React/TypeScript frontend (`Frontend/`). It is the
foundation of a Master's thesis (diplomski rad).

**This exact state of the repository (tagged `baseline-v1`) was the fixed starting point for a
comparison study.** Two separate AI-upgrade sessions branched off from that tag independently: one
driven by Claude Code, one by Codex CLI, both starting from identical context. Concretely, that
still means:

- This file is read by **both** tools (Codex CLI reads `AGENTS.md` natively; `CLAUDE.md` in this
  repo is a symlink to this file so Claude Code picks up the same content). Do not fork the
  content between two files.
- Everything below must be tool-neutral. Do not add Claude Code-specific constructs (skills,
  subagents, `.claude/` settings, hooks) to convey architecture or conventions - Codex has no
  equivalent, and doing so would bias the comparison. Plain instructions in this file are the only
  channel that is fair to both tools.
- **Update this file whenever you discover something new about the architecture, make an
  architectural decision, or change a convention - in every session, not just this one. Don't wait
  to be asked.** Stale docs are worse than no docs for a comparison study where both sides read the
  same source of truth. Keep this file itself lean and currently-accurate; put phase-by-phase
  reasoning and verification narrative in `docs/decision-log.md` instead (see below), not here.

For the full phase-by-phase history of every upgrade decision on the `upgrade/claude-code` branch
and how each was verified, see **`docs/decision-log.md`**. It is not required reading for every
session - this file is the current-state summary; the decision log is the detailed reference for
"why is it built this way" and "what was actually tested."

## Tech stack

**Backend** (`Backend/demo/`):
- Java 21, Spring Boot 3.4.5, Maven (`Backend/demo/pom.xml`)
- PostgreSQL (JPA/Hibernate + Flyway migrations, `ddl-auto: none` - schema is migration-driven
  only)
- Redis (Spring Cache abstraction, `spring-boot-starter-data-redis`)
- Spring Security + a custom JWT/refresh-token layer (`io.jsonwebtoken:jjwt`)
- Hibernate Envers for entity audit history
- Spring Mail (Gmail SMTP) + Thymeleaf for HTML email templates
- WebSocket/STOMP for real-time notifications and live gym-occupancy updates
- MapStruct for entity<->DTO mapping, Lombok, springdoc-openapi (Swagger UI)
- Anthropic Claude API (`anthropic-java` SDK, model `claude-haiku-4-5`) for AI manager insights and
  client progress narratives (`AnthropicConfig`, `ClaudeInsightServiceImpl`)

**Frontend** (`Frontend/`):
- React + TypeScript + Vite (pure client-rendered SPA)
- Tailwind CSS v4 (`@tailwindcss/vite`)
- Zustand for auth state only; feature-local state is plain `useState`/`useEffect`
- react-router-dom, react-konva (2D room editor), `@stomp/stompjs` (WebSocket), Recharts (progress
  charts), axios, `jwt-decode`

## Running locally

1. Copy `.env.example` to `.env` (repo root) and fill in `MAIL_USERNAME`, `MAIL_PASSWORD` (a Gmail
   **App Password**, not the account password), `JWT_SECRET` (>= 32 characters - the app fails to
   start otherwise), and `ANTHROPIC_API_KEY`. The backend auto-loads this `.env` file
   (`me.paulschwarz:spring-dotenv`, configured via `Backend/demo/src/main/resources/.env.properties`)
   - no manual export/`source` step needed. All four are bound via `${...}` placeholders in
   `application.yaml` (e.g. `app.anthropic.api-key: ${ANTHROPIC_API_KEY:}`); see
   `docs/decision-log.md`'s "Upgrade: dev-tooling decisions" for how this was wired up and the one
   gotcha hit along the way. An optional fifth var, `FRONTEND_URL` (defaults to
   `http://localhost:5173`), controls the origin activation/reset-password email links point to -
   only worth overriding once there's a real deployed frontend to point at.
2. Start infrastructure: `docker compose -f Docker/docker-compose.yaml up -d`
   - Postgres on host port `8877` (mapped to container `5432`), db `fm`, user `fm_dbuser` /
     password `password`
   - Redis on `6379`, password `password` (enforced via `--requirepass`)
   - Postgres data persists in `Docker/postgres_data/` (git-ignored except `.gitkeep`) via a
     bind-mounted volume.
3. Run the app from `Backend/demo/`: `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`).
   Default active profile is `dev` (`spring.profiles.active: dev` in `application.yaml`), which
   additionally loads `db/dev-data` Flyway migrations (kept only for their historical role on a
   truly fresh Postgres volume - see "Conventions" below) and runs `DevDataSeeder` - a
   `@Profile("dev")` `CommandLineRunner` (not a migration - it needs a "now"-relative dataset) that
   is the **sole source of truth** for a seeded dev database: the Gym and its 5 rooms, the
   `admin`/`ogi`/`citva` accounts (find-or-create, not assumed-to-exist), 4 more trainers, ~50
   clients, ~110-225 realistic appointments across the current calendar month plus matching
   payments, room check-ins, and progress entries. Idempotent on first boot (skips entirely if a
   marker trainer's email already exists) - and, on the `dev` profile only, can be re-run **at any
   time** without a container restart or volume wipe via `POST /api/dev/reseed` (MANAGER JWT
   required): wipes every table it owns and rebuilds from scratch. See `docs/decision-log.md`
   ("Upgrade: manager-testing round 3 decisions") for why this moved off Flyway and how the wipe is
   ordered.
4. App listens on port `8088`. Swagger UI: `http://localhost:8088/swagger-ui/index.html`. OpenAPI
   JSON: `http://localhost:8088/v3/api-docs`.
5. Frontend: from `Frontend/`, `npm install` then `npm run dev` (Vite, default port `5173` - matches
   `app.cors.allowed-origins`).
6. Known dev-seed login accounts (password `password123` for all): `admin` (MANAGER), `ogi`
   (TRAINER), `citva` (CLIENT).

There is no containerized app service in `docker-compose.yaml` - only Postgres and Redis. The
Spring Boot app itself always runs locally (IDE/`mvnw`) against those two containers.

## Domain model

All entities extend `model/common/BaseEntity` (`@MappedSuperclass`): `version`,
`createdAt`/`createdBy`, `updatedAt`/`updatedBy` via Spring Data JPA auditing. Every entity is also
`@Audited` (Hibernate Envers).

- **User** (`model/user/User.java`) - email, password (null until account activation),
  `isActivated`, `notificationPreference` (`EMAIL`/`PUSH`/`BOTH`), registration/reset keys with
  validity timestamps, `Set<UserRole>`.
- **UserRole** - join entity; `role` is one of `MANAGER` / `TRAINER` / `CLIENT` / `ADMIN`. A single
  `User` can hold multiple roles. Adding a role via `UserService.addRole` only inserts this join
  row - it does **not** create the matching `Trainer`/`Client` domain entity (only
  `TrainerController.create`/`ClientController.create` do that); removing a `Trainer`/`Client`
  correspondingly also removes the matching role via `UserService.removeRole`. `ADMIN` is additive
  to `MANAGER` (never held alone) and is the only role allowed to grant/revoke `MANAGER` itself -
  see "Upgrade: manager-hierarchy decisions" below and `docs/decision-log.md`. Exactly one seed
  account (`admin`, via migration `V1.0020__add_admin_role.sql`) has it.
- **Trainer** - 1:1 with `User`; employment date, birth year, `EmploymentStatus` (`FULL_TIME` /
  `CONTRACT` / `FORMER_EMPLOYEE`).
- **Client** - 1:1 with `User`; owns `Payment`s, `ClientSessionTracking`s, `ClientAppointment`s.
- **Session** - a session *type* (`INDIVIDUAL` / `GROUP`) with `maxParticipants`. Seeded rows only
  (INDIVIDUAL/1, GROUP/3, GROUP/10) - not created via the API. Neither `Session` nor `Payment` has
  a price/amount column - manager-insights "revenue" is a paid-appointment-count proxy, not
  currency.
- **Appointment** - date/start/end time, belongs to a `Session` type, a `Trainer`, and a `Room`,
  plus a set of `ClientAppointment`s. `Room` is mandatory on `CreateAppointmentRequest` (manager-
  testing round 3 - `AppointmentServiceImpl.create()` rejects a null `roomId` before any other
  validation runs). `Trainer` is **optional again** as of the trainer self-assign round -
  `CreateAppointmentRequest.trainerId` may be null, producing a "termin bez trenera" that a
  TRAINER can later claim via the pre-existing `POST /{id}/assign`/`DELETE /{id}/unassign`
  marketplace (see "Auth flow"/below - that marketplace existed since Faza 7 but round 3 had made
  it unreachable by requiring a trainer at creation time; see `docs/decision-log.md` "Upgrade:
  trainer self-assign decisions"). `validateTrainerWorkingSchedule`/`validateTrainerNotDoubleBooked`
  are both no-ops when `trainerId` is null, so no other change was needed to re-enable this.
  `DevDataSeeder` generates a handful of trainer-less open slots (~1 in 4 dates) reusing the same
  per-date room/slot occupancy tracking the rest of that method already builds, so they can never
  collide with a real trainer-led appointment's room/time. `POST /api/appointment/recurring`
  (`CreateAppointmentRequest.recurring = true`) generates weekly-repeating instances of one request
  8 weeks ahead from its `date`, each independently validated - one week's conflict is skipped, not
  fatal to the series. There is no persisted appointment status column - "cancelled"/"never
  booked" are indistinguishable after the fact; state is entirely implicit (capacity reached or
  not, trainer assigned or not). `AppointmentServiceImpl.validateAppointment()` runs, in order: a
  holiday check (`HolidayService.isGymClosedOn`, separate from gym-hours - a holiday on a day the
  gym is normally open must not read as "gym closed that day of week"), gym-hours (message states
  the exact date and that day's opening/closing time), trainer-working-schedule coverage (a
  `TrainerSchedule` `WORKING` row must cover the slot - message is honest that this is "no shift",
  not "already busy"), trainer-double-booking (a genuine appointment-vs-appointment time overlap
  check - this did not exist before this session, only the working-schedule check did, so two
  overlapping appointments for one trainer could previously both be created as long as one
  `WORKING` shift covered both), room-double-booking (same overlap check, newly added - rooms had
  **no** conflict check at all before this session), then client availability. Both double-booking
  messages name the exact conflicting appointment's date/time, not a generic "already busy" - and
  name the trainer/room by their *email*/*name* (`trainerLabel()`/`roomLabel()` helpers, looked up
  only when a message is actually being built), not a bare numeric ID, since a manager has no way
  to map an ID to a person/room from the error text alone. See `docs/decision-log.md`'s "Upgrade:
  appointment conflict-message decisions" for the full before/after and how each was
  live-verified. `createRecurringWeekly()`'s final "nothing could be created" error is now a
  per-date breakdown (`date: reason`, one line per attempted week) instead of one generic sentence
  - a holiday hit on one of the 8 dates is not itself reported as a "problem" unless it's part of
  why the *whole* series failed (see the decision log for why this falls out naturally from always
  collecting reasons but only surfacing them on total failure). `GET
  /api/appointment/available-trainers`/`/available-rooms` (both `date`/`startTime`/`endTime` query
  params, MANAGER-only) expose exactly the trainer-working-schedule/trainer-double-booking/
  room-double-booking predicates `validateAppointment()` itself uses (factored into shared private
  helpers, not reimplemented) - they back the admin "new appointment" form's trainer/room pickers
  so a manager can't even select a doomed combination, but are a UX narrowing only: `create()`/
  `createRecurringWeekly()` still re-validate from scratch and remain the sole authority (a
  concurrent booking between listing and submit is still caught there). See `docs/decision-log.md`
  "Upgrade: appointment picker filtering decisions" for the endpoint shape and the room-capacity
  criterion the frontend uses to filter session types once a room is picked.
- **ClientSessionTracking** - per (client, session type) remaining/reserved appointment counters,
  driven by `Payment`s.
- **GymSchedule** - opening/closing time per `DayOfWeek`; upserted per day (`create` finds-or-builds
  by `DayOfWeek`), not insert-only. `closingTime <= openingTime` means "closes the next calendar
  day at this time" (deliberately allowed, not clamped to midnight) - but that overnight portion
  is validated against the adjacent day's own hours both directions
  (`GymScheduleServiceImpl.validateNoAdjacentDayOverlap`), so e.g. Thursday open until 02:00 can't
  coexist with Friday opening at 01:00.
- **TrainerSchedule** - a trainer's status (`WORKING`/`HOLIDAY`/`SICK_LEAVE`/`VACATION`) for a given
  date and time range. A trainer's rows never overlap regardless of status mix (a `WORKING` shift
  can't overlap an existing `VACATION` day and vice versa) - enforced both directions in
  `TrainerScheduleServiceImpl` via the same status-agnostic
  `existsByTrainerIdAndDateAndTimeRange` check. `POST /api/schedule/trainer/me/recurring`
  (TRAINER self-service only - see "Upgrade: trainer fixed-schedule decisions" in
  `docs/decision-log.md`) generates weekly-repeating `WORKING` shifts from one request, same
  `RECURRING_WEEKS_AHEAD = 8` convention and "skip one bad week, only report reasons if the whole
  series fails" behavior as `AppointmentService#createRecurringWeekly`.
- **Holiday** - a gym-wide non-working date; insert-only by design (no update/delete).
- **Gym** (`model/gym/Gym.java`) - single-installation config (name, address, contact info,
  logo/brand color, timezone). A real table (not a `@ConfigurationProperties` bean), even though
  exactly one row is expected in practice; upserted via a single `PUT /api/gym`, not created via a
  normal POST.
- **Room** (`model/gym/Room.java`) - belongs to a `Gym`; name, `RoomType`, capacity, and
  **rectangle** geometry (`posX`/`posY`/`width`/`height`/`rotationDegrees`, all `double precision`)
  for the 2D floor-plan editor/live view - deliberately not an arbitrary polygon, since real gym
  rooms are overwhelmingly rectangular and `react-konva`'s `Rect` maps to this directly. The
  minimum size a room may be resized to is no longer a single fixed constant (previously a flat
  4m x 2.5m floor) - it is now computed **per room from its own name/type** (name length is the
  dominant factor, since it's the only unbounded string), so the live floor-plan tile
  (`LiveFloorPlanPage`'s `RoomTile`, which has no `overflow-hidden`) is guaranteed to fit its
  icon+name, type label, progress bar, and count/percent badge without truncating or spilling
  outside the rectangle. See `docs/decision-log.md`'s "Upgrade: room minimum-size decisions" for
  the formula, the frontend/backend split, and why the backend's heuristic is deliberately a
  looser approximation
  tuned to stay at least as strict as the frontend's exact canvas measurement - enforced on both
  create and update (including a name-only rename, since a longer name on an already-valid room
  must not be savable without a matching size increase), and re-checked but **not** retroactively
  against rooms already smaller than the newly-computed minimum for their content. The 5 seed
  rooms (including their exact dimensions/capacity) are now defined solely in `DevDataSeeder` (see
  "Conventions" below) rather than a Flyway migration - one of them (`Svlačionica`) had its width
  bumped from 6.0 to 7.5 units as part of this change, since the old value no longer satisfies its
  own name's computed minimum.
- **RoomCheckIn** (`model/gym/RoomCheckIn.java`) - a manual check-in/check-out event of a `Client`
  into a `Room`; `checkedOutAt == null` means currently inside. At most one active check-in per
  client is enforced **globally** (not per-room) by a DB unique partial index
  (`uq_room_check_in_one_active_per_client ON room_check_in (client_id) WHERE checked_out_at IS
  NULL`), not just at the service layer. Computed room occupancy additively combines active
  check-ins with clients on in-progress appointments in that room, without deduplication (a client
  counted both ways is double-counted - a deliberate simplicity trade, not a bug).
- **ClientProgressEntry** (`model/progress/ClientProgressEntry.java`) - a dated body-measurement
  snapshot for a `Client` (weight, body fat %, waist/chest/hip/thigh/arm circumference, notes) as
  fixed nullable columns, not a JSON/EAV blob.
- **ClientPersonalRecord** (`model/progress/ClientPersonalRecord.java`) - a `Client`'s best result
  for a free-text exercise name (value + `RecordUnit` + date), and every `create()` call is a new
  row (no unique constraint on client+exerciseName) - the full history this way already backed
  `PersonalRecordsList.tsx`'s list view before a later round added a chart on top of it
  (`PersonalRecordChart`, exported from that same file but rendered separately - see "Conventions"
  below) - one exercise at a time via a dropdown (defaulting to whichever has the most history),
  since different exercises use different units/scales (`RecordUnit` is a fixed enum:
  `KG`/`LB`/`REPS`/`SECONDS`/`MINUTES`/`METERS`/`KM`) and can't share one Recharts axis the way
  `ProgressCharts.tsx`'s body-measurement lines can. `RecordForm.tsx`'s exercise-name field is a
  free-text `<input>` backed by a `<datalist>` of that client's own existing exercise names (see
  "Upgrade: exercise-name dropdown decisions" in `docs/decision-log.md`) - suggests, doesn't
  force, since a client's first record of any given exercise still needs to accept brand-new text;
  this is a UX nudge only, there is still no DB-level uniqueness constraint preventing two
  differently-spelled rows for what a human would consider the same exercise.

## Auth flow (read this before touching security-adjacent code)

- Login (`UserServiceImpl.login(LoginUserRequest)`) verifies bcrypt password, issues an access
  token (15 min, `app.jwt.accessTokenExpiration`) and a refresh token (2h,
  `app.jwt.refreshTokenExpiration`), both explicitly `Jwts.SIG.HS256`-signed with `app.jwt.secret`
  (a `javax.crypto.SecretKey`, `util/JwtUtil.java`) - pinned explicitly rather than left to jjwt's
  key-length-based algorithm auto-selection, since the decoder (`JwtConfig.jwtDecoder()`) only ever
  validates HS256.
- Refresh tokens are **stateless** - there is no server-side store/revocation list, and refreshing
  does not rotate the refresh token (the same one is echoed back). A leaked refresh token stays
  valid until its own natural expiry.
- **Actual route protection is implemented by two custom `HandlerInterceptor`s, not by Spring
  Security's filter chain**:
  - `interceptor/JwtInterceptor` (order 1) - validates the `Authorization: Bearer <token>`
    signature, 401s if missing/invalid.
  - `interceptor/RoleInterceptor` (order 2) - reads the `@RoleRequired` annotation on the handler
    method and checks the JWT's `roles` claim, 403s if none match. **A handler with no
    `@RoleRequired` is reachable by any authenticated user regardless of role.**
  - Both are registered in `config/web/WebConfig` with an identical exclude-list covering
    register/login/login-refresh/forgot-password/reset-password/swagger - a user who forgot their
    password by definition has no valid JWT to present, so these must stay reachable without one.
  - Self-service endpoints that resolve "the current trainer/client" from the JWT (e.g.
    `POST /api/schedule/trainer/me`) use request DTOs with no `trainerId`/`clientId` field at all,
    so writing another user's data is unrepresentable, not just permission-checked.
- `SecurityConfig`'s `SecurityFilterChain` permits `/api/**` (and swagger/websocket paths) via
  `authorizeHttpRequests(...).permitAll()` and otherwise requires authentication via its own
  `oauth2ResourceServer` JWT decoder. Because the app's actual `/api/**` routes are already covered
  by that `permitAll`, Spring Security is **not** the layer doing authorization for the REST API
  today - the interceptors above are. Spring Security's `anyRequest().authenticated()` still
  matters as a defense-in-depth fallback for any path that is *not* under the permitted prefixes.
  Do not assume `@PreAuthorize`/`hasRole` do anything here - they are not used; role checks are
  exclusively via `@RoleRequired` + `RoleInterceptor`.
- A TRAINER's access to a specific CLIENT's progress data (`ClientProgressEntry`/
  `ClientPersonalRecord`/AI narrative) is additionally gated by `TrainerClientAccessGuard` - see
  Conventions below. `AccessDeniedException` from that guard (and from schedule-ownership checks)
  maps to `403` via `GlobalExceptionHandler`.
- CORS is configured in `config/web/CorsConfig` (a `CorsConfigurationSource` bean wired into
  `SecurityConfig` via `.cors(...)`), driven by `app.cors.allowed-origins` in `application.yaml`
  (defaults to `http://localhost:5173` for local frontend dev). **Change this to the real frontend
  domain in production - never `*`, since credentials are allowed.**
- **Manager hierarchy**: only an `ADMIN` may grant/revoke the `MANAGER` role
  (`UserServiceImpl.addRole`/`removeRole`, gated on `role == Role.MANAGER` via a new
  `isCurrentUserAdmin()` helper that reads the `roles` claim off the JWT `Authentication` principal
  - same idiom as `isCurrentlyAuthenticatedUser`). An ordinary `MANAGER` calling either endpoint
  for `MANAGER` gets `403` (`AccessDeniedException`), same as any other `@RoleRequired` gap - there
  is deliberately no separate `@RoleRequired("ADMIN")` annotation-level gate, since `addRole`/
  `removeRole` are one shared endpoint pair for every role and only the `MANAGER` case needs the
  extra check. Frontend (`UsersTab`/`ManagersTab`) hides the now-403-doomed buttons/form for
  non-`ADMIN` users, but the backend check is what actually enforces this.

## Notifications

- Email (`service/impl/notification/email/`): activation and password-reset emails use Thymeleaf
  templates; appointment-reminder and trainer-schedule emails are built as inline strings
  (inconsistent with the templated ones, not yet unified). Activation/reset links are built
  server-side via `app.frontend.url` (env var `FRONTEND_URL`, defaults to
  `http://localhost:5173`) - wired through `ActivationEmailData`/`ForgetPasswordEmailData` into
  both templates as `${frontendUrl} + '/register/complete?registration_key=' + ...` (and
  `/reset-password?reset_key=...`). This used to be hardcoded to a placeholder domain
  (`https://nesto.com`) in both templates - a real, previously-shipped bug, not a dev/demo
  placeholder; fixed once real Gmail credentials made these emails actually deliverable. The
  frontend's on-screen activation-link banner (a dev/demo stand-in for when email wasn't real) has
  been removed accordingly - the link now reaches the user exclusively via email, in every
  environment.
- WebSocket/STOMP (`config/web/WebSocketConfig`, endpoint `/ws`, no SockJS fallback, simple broker
  on `/topic`): `NotificationServiceImpl` pushes per-user notifications and additionally sends
  email based on `User.notificationPreference`. A second topic, `/topic/gym/occupancy`, carries the
  full live room-occupancy snapshot (`List<RoomOccupancyDTO>`, JSON-serialized once server-side)
  for the live floor-plan view - broadcast both event-driven (every check-in/check-out) and via a
  once-a-minute `OccupancyScheduler` sweep (for occupancy changes driven by appointments
  starting/ending, which have no application event to hang a push off of).
- `NotificationScheduler` (`@Scheduled`): daily trainer/client appointment digests at 20:00, and an
  hourly sweep for appointments starting within the next hour.
- `websocket/StompWebSocketClient` is a manual `public static void main` test harness left in
  `src/main/java` (not part of runtime wiring, not test code) - known clutter, intentionally left
  alone.

## Audit (Hibernate Envers)

Every entity is `@Audited`. Audit tables (`*_aud`, `revinfo`) are **hand-written Flyway
migrations**, not Envers-generated at runtime (`ddl-auto: none`). **Adding or changing an
`@Audited` entity's columns requires manually writing the matching migration** - Envers will not
create it for you, and nothing will fail loudly if you forget; it will just silently not persist
history for the new column.

Known gap: `AuditorAwareImpl` reads the current user from `SecurityContextHolder`, but nothing in
the request pipeline populates the `SecurityContext` (auth is interceptor-based, see above) - so
`createdBy`/`updatedBy` are always `null` in practice (see Known issues).

## Caching

Redis via Spring Cache, one global `RedisCacheConfiguration` (10 min TTL, JSON serialization) in
`config/cache/RedisConfig`. Three cache regions exist:
- `TRAINER_CACHE` (default TTL) - `TrainerServiceImpl`.
- `MANAGER_INSIGHTS_CACHE` (30 min TTL) - `@Cacheable` on `ManagerInsightsServiceImpl.getInsights()`;
  a separate `refreshInsights()` evicts+regenerates+re-populates directly via an injected
  `CacheManager` (never calling the cached method internally, to avoid the Spring AOP
  self-invocation pitfall), backing `POST /api/insights/manager/refresh`. Caches the full
  structured `ManagerInsightsDTO` (see "Upgrade: manager-insights dashboard decisions" in
  `docs/decision-log.md`) - changing that DTO's shape again will leave a stale/incompatible entry
  under the `'current'` key for up to the 30 min TTL on existing Redis data; flush that key (or
  the whole cache) after a shape change during local dev, the same way this session had to.
- `CLIENT_PROGRESS_INSIGHT_CACHE` (10 min TTL) - manual `CacheManager` lookup/populate (no
  `@Cacheable` annotation, for the same self-invocation reason) in
  `ClientProgressInsightServiceImpl`; evicted explicitly whenever a `ClientProgressEntry` is
  created/updated/deleted for that client id (personal-record writes do not evict it). The
  cached `ClientProgressInsightDTO.narrative` string changed shape in the trainer-testing round -
  "short intro + `- `-prefixed bullets" instead of one 3-5 sentence paragraph (see
  `docs/decision-log.md` "Upgrade: progress-insight readability decisions") - `InsightPanel.tsx`
  parses it by splitting on lines and pulling out any `- `-prefixed ones as bullets, everything
  before the first bullet as the intro. Same stale-cache caveat as `MANAGER_INSIGHTS_CACHE` above
  applies to any existing cached entries from before this change.

Both AI cache regions back Claude-generated narratives (`claude-haiku-4-5`) - chosen as the
cheapest current model since both features are single-turn, already-aggregated-data-in /
short-text-out calls, not open-ended reasoning.

## Conventions

- Layered packages: `controller` (thin, `@RoleRequired`-gated) -> `service` interface +
  `service.impl` -> `repository` (Spring Data JPA) -> `model` (JPA entities). Config classes are
  grouped by concern under `config/{audit,cache,core,security,web}`.
- Every entity has a matching DTO (`dto/**`) and a MapStruct `@Mapper(componentModel = "spring")`
  interface (`mapper/**`).
- Write-side request/response objects live under `service/params/request/**` and
  `service/params/response/**`, separate from the read-side `dto/**` - a deliberate three-way split
  (persistence model / read DTO / write request-response); keep new endpoints consistent with it.
  Update endpoints generally reuse the corresponding `Create...Request` DTO rather than adding a
  parallel `Update...Request` type (e.g. progress entry/personal record update).
- Lombok `@Builder` on entities that are constructed programmatically in service code; plain
  constructors on the rarely-constructed ones.
- `@Slf4j` logging throughout; scheduler/notification logs use an emoji-prefixed style
  (`🔥`/`✅`/`❌`) as an established (if unusual) convention - match it in that area rather than
  "fixing" it to plain text.
- `GlobalExceptionHandler` (`com.example.demo.exception`, `@RestControllerAdvice`) maps
  `IllegalArgumentException` and bare `RuntimeException` (including `EntityNotFoundException`) to
  `400` with a `{"message": "..."}` body, and `AccessDeniedException` to `403` explicitly (more
  specific, so Spring matches it ahead of the `RuntimeException` handler). It is **not** a complete
  REST exception taxonomy - see Known issues. `IllegalStateException` is deliberately left
  unhandled globally; `RoomCheckInController` catches it locally around check-in/check-out and
  returns `409` for the one-active-check-in-per-client conflict.
- Endpoints that resolve "the current user" from the JWT (client/trainer self-service - booking,
  self-service scheduling, "my appointments", "my progress") all use the same idiom:
  `SecurityContextHolder` -> `Jwt` -> `jwt.getClaim("email")` -> repository
  `findByUserEmail(...)`, duplicated per service rather than factored into a shared abstraction -
  an accepted, deliberate trade-off in this codebase (small duplication over cross-service
  coupling).
- `TrainerClientAccessGuard` (`com.example.demo.security`, a plain `@Component`, not a service
  interface) enforces that a TRAINER can only access a CLIENT's progress data if they share
  appointment history (`ClientAppointmentRepository.existsByClientIdAndAppointmentTrainerId`);
  MANAGER is exempt. Authorization on update/delete is always checked against the entity's own
  already-persisted `clientId`, never a request body's `clientId` (which may be attacker-controlled
  and is otherwise ignored for that purpose).
- **`DevDataSeeder` is the sole source of truth for all dev/test data** (as of manager-testing
  round 3 - see `docs/decision-log.md`) - Gym/Rooms, `admin`/`ogi`/`citva` accounts, trainers,
  clients, appointments/payments/check-ins/progress. `db/dev-data/*.sql` Flyway migrations are
  never edited/deleted (checksums locked) and still create their original rows on a truly fresh
  Postgres volume, but every value they insert is now find-or-create-redundant with what
  `DevDataSeeder` builds - and on a `reseed()` (below), those migrations don't run again, so the
  seeder alone recreates everything. Flyway migrations (`db/migration` **and** `db/dev-data`)
  should only ever contain schema changes and static reference data going forward - actual
  test/demo rows belong in `DevDataSeeder`. The seed body lives in a private `seedAll()`, shared by
  `run()` (guarded by a marker-record check on first boot, matching the `WHERE NOT EXISTS` spirit
  of the legacy dev-data migrations) and the public `reseed()` (unconditional: bulk-deletes every
  table this class owns in FK-safe order, then calls `seedAll()` again) - exposed as `POST
  /api/dev/reseed`, `@Profile("dev")` + `@RoleRequired("MANAGER")`, so a manager can rebuild a
  clean dataset on the live dev database at any time without touching Docker.
  `seedAppointmentsForCurrentMonth()` writes appointments directly via `appointmentRepository.
  saveAll(...)`, bypassing `AppointmentServiceImpl.create()`'s conflict checks entirely - it is
  therefore itself responsible for never generating a same-trainer or same-room time overlap.
  Per date, it tracks already-claimed `(trainer, time)` and `(room, time)` pairs (a plain
  `Set`/`Map`, shared across the individual/small-group/big-group session types being generated
  that date) and walks the slot/trainer combo space from a single deterministic counter until an
  unclaimed pair is found, instead of two independent rotating counters (which could - and did,
  before this fix - revisit the same pair within one date's booking loop); a client is skipped
  for that date only if every combo is already taken (a real capacity ceiling, e.g. Sunday has
  just one working trainer and 3 slots = 3 combos total), never by double-booking. See
  `docs/decision-log.md`'s "Upgrade: dev-seeder double-booking fix" for the before/after conflict
  counts. As of the trainer-testing round, the same method additionally rolls a ~1-in-4 chance per
  date to append one trainer-less "open slot" appointment (no trainer, no clients, `individual`
  session type) - reusing that date's own `occupiedRoomsAtSlot` map so it can never double-book a
  room a trainer-led appointment above already claimed at that exact slot; these back the TRAINER
  self-assign marketplace's "Termini bez trenera" screen with real data (see "Upgrade: trainer
  self-assign decisions").
- **Do not edit existing Flyway migration files** (`db/migration/V1.00XX__*` or
  `db/dev-data/V1.00XX__*`) - their checksums are locked once applied. If schema changes are
  needed, add a new `V1.00XX__*.sql` file (either location - they share one version sequence).
- Frontend (`Frontend/`): one feature module per concern under `src/features/<name>/{types,api}.ts`
  plus one page per screen; flat per-page routes in `App.tsx` (no nested layouts/routes). A small
  `src/components/` directory (new in manager-testing round 3, grown in the trainer-testing round)
  holds cross-feature UI: `MonthCalendar` (a from-scratch month-grid day picker, used by the admin
  Termini tab, `/manager/dnevni-raspored`, and - as of the trainer-testing round - both TRAINER
  self-service pages, `TrainerSchedulePage`/`TrainerAppointmentsPage`), `SearchableSelect` (a
  from-scratch filterable combobox, used by Plaćanja's client picker), and `DateInput` (see below)
  - all built without adding a dependency, since none existed for any of these needs. Both TRAINER
  pages that pair a `MonthCalendar` day-picker with the trainer's own history
  (`TrainerAppointmentsPage.tsx`/`TrainerSchedulePage.tsx`) also carry a collapsible, always-
  reachable "Istorija ..." section (a plain `useState` toggle, collapsed by default) below the
  calendar - a calendar alone is a poor history browser (finding "what did I do last month"
  requires navigating month-by-month and clicking each date individually), so a day-picker and a
  flat reverse-chronological list are complementary here, not substitutes for each other. See
  `docs/decision-log.md` "Upgrade: appointment/schedule history visibility decisions". A shared
  `extractErrorMessage(err, fallback)` helper (duplicated per
  feature; reads
  `err.response.data.message` via axios's `isAxiosError`) surfaces `GlobalExceptionHandler`
  messages in the UI. A backend error message can be multi-line (`\n`-joined, e.g.
  `createRecurringWeekly()`'s per-date breakdown - see "Upgrade: appointment conflict-message
  decisions" in `docs/decision-log.md`) - a plain `<p>{message}</p>` silently collapses those
  newlines into an unreadable single line, so any component that renders a raw backend error string
  should render it through a small heading-plus-bulleted-list helper instead (see
  `AppointmentsTab.tsx`'s local `ErrorMessage` component) rather than a bare paragraph; this has not
  yet been swept across every feature that calls `extractErrorMessage`, only the appointment form
  where the multi-line message was found. Destructive actions use the browser's native
  `window.confirm(...)`, not a custom modal (no modal/dialog pattern exists in this frontend). Multi-role accounts get a role
  *switcher*, not a merged view - one "active role" at a time gates routes/nav
  (`RequireActiveRole`). `AdminPage`'s tabs (`features/admin/`) each own one domain: `UsersTab`
  is the full cross-role account list (search/edit/delete/toggle MANAGER); `ManagersTab`/
  `TrainersTab`/`ClientsTab` each have their own create form that defaults that tab's role - there
  is deliberately no "create an account with no role" path. Every date field in the app now goes
  through `components/DateInput.tsx` (all ~16 former ad-hoc `<input type="date" lang="sr-Latn-RS">`
  call sites replaced in the trainer-testing round - see `docs/decision-log.md` "Upgrade: DateInput
  decisions") instead of a bare native input, both `input[type='date']`/`input[type='time']` still
  rely on the global `color-scheme: dark` rule in `index.css` for a visible calendar/clock-picker
  icon. **The `lang="sr-Latn-RS"` attribute alone does not actually change the empty-state segment
  placeholder ("dd.mm.gggg"/its Chromium equivalent) in Chrome/Edge** - confirmed during
  "manager-testing round 2" live testing: Chromium derives that placeholder's format from the
  browser/OS UI language, not the page's `lang` attribute (Firefox does honor it, which is
  presumably why this was believed fixed), and there is no reachable CSS or `placeholder`-attribute
  lever into it either. `DateInput` works around this with a separate overlay label ("Izaberite
  datum") shown only while the input is empty and unfocused, rather than fighting the native
  placeholder - see `docs/decision-log.md` for why this approach (over a CSS-only attempt) and how
  it keeps the native picker's click-to-open behavior intact (`pointer-events-none` on the overlay).
  The overlay alone only draws on top - it does **not** hide the native placeholder text
  underneath by itself, so `DateInput` also sets an inline `style={{ color: 'transparent' }}` on
  the native input for exactly that same `showPlaceholder` window (an inline style, not a
  `className`, since it must reliably out-rank whatever text-color class each call site passes in)
  - without it, the native "dd-----yyyy" segments and the overlay text render simultaneously,
  overlapping and unreadable (confirmed live, screenshotted, and fixed in a later round - see
  `docs/decision-log.md` "Upgrade: DateInput placeholder-overlap fix").

## Known issues

These are open, unresolved items - fair game to pick up in a future session. (Items resolved during
the `upgrade/claude-code` branch's work are documented, with full fix/verification detail, in
`docs/decision-log.md` rather than listed here.)

- Refresh tokens have no rotation and no server-side revocation - a leaked refresh token stays
  valid until its own natural (2h) expiry.
- Claude's manager-insights JSON response occasionally slips a Cyrillic-alphabet word into an
  otherwise-Latin-script Serbian sentence (e.g. "занетост" mid-sentence) despite the system prompt
  explicitly saying "latinica... do not use ćirilica" - observed live during the manager-insights
  dashboard upgrade (see `docs/decision-log.md`, "Upgrade: manager-insights dashboard decisions").
  Not a code bug - it's model output variance the prompt doesn't fully constrain - but worth
  knowing before assuming a rendering bug if a Cyrillic word shows up on `/manager/insights`.
- `AuditorAwareImpl` always returns empty (nothing populates `SecurityContext` in this
  interceptor-based auth model) - `createdBy`/`updatedBy` are effectively dead columns on every
  entity.
- `TrainerSchedule.date` is annotated `unique = true` at the entity level
  (`model/schedule/TrainerSchedule.java`), which would incorrectly allow only one trainer total to
  have a schedule row on any given date - it should almost certainly be a composite
  `(trainer_id, date)` constraint. No DB-level unique constraint actually exists in the migrations,
  so entity and schema disagree; this has had no observed effect yet but is worth fixing carefully
  (with a new migration) before relying on it.
- `GlobalExceptionHandler` is not a complete REST exception taxonomy: `EntityNotFoundException`
  maps to `400` instead of a semantically correct `404`, and any genuinely unexpected
  `RuntimeException` (i.e. a real bug, not a validation failure) also reports as `400` instead of
  `500`.
- `application.yaml` and `application-dev.yaml` duplicate almost every property instead of the dev
  file overriding only what differs (currently just `flyway.locations`) - keep both in sync
  manually until this is restructured.
- The Gmail account used for `MAIL_USERNAME` had its app password committed in git history (now
  moved to an env var, but the old value is still recoverable from history) - **the app password
  must be rotated in the Gmail account**, this repo change alone does not invalidate it.
- `BaseEntity`'s Lombok `@Data`-generated `equals()`/`hashCode()` only compares `BaseEntity`'s own
  fields (`version`, `createdAt`, etc.), never the subclass's `id` - so two distinct entities of the
  same type with all-null audit fields (e.g. two freshly-built, unsaved `ClientAppointment`s)
  compare as equal. Affects any code that relies on `HashSet`/`equals()` semantics for unsaved
  entities of the same type - a real fix would override `equals()`/`hashCode()` per-entity on `id`,
  or switch collections holding these entities to `List`. **Confirmed to actually bite in
  production, not just a theoretical edge case**: entity-level cascade delete of a `Client` (via
  its `cascade = ALL, orphanRemoval = true` collections) throws
  `TransientObjectException`/`OptimisticLockException` once an `Appointment`'s own bidirectional
  `clientAppointments` collection gets pulled into the same cascade graph (e.g. any shared GROUP
  session) - see `UserServiceImpl.delete()`'s workaround (bulk JPQL deletes for every `client_id`/
  `trainer_id`-FK'd table instead of relying on JPA cascade), which sidesteps this without fixing
  the underlying bug. Any other future code path that cascade-deletes a `Client` or `User` entity
  (not via bulk JPQL) is still exposed to this.
- A client's appointment reservation can still go negative against their remaining-session balance
  - `reserve()`/`addClients()` increment `ClientSessionTracking.reservedAppointments` with no floor
  check against `remainingAppointments`.
- No CI pipeline runs `mvn test`/`tsc -b`/`npm run build` automatically - all three must be run
  manually, and `mvn test` requires Postgres/Redis to be up first.
- ~~`<input type="date">`'s native empty-state placeholder cannot be reliably localized~~ - fixed via
  `components/DateInput.tsx`'s overlay label approach (see "Conventions" above and
  `docs/decision-log.md` "Upgrade: DateInput decisions"/"Upgrade: DateInput placeholder-overlap
  fix"). Live-screenshotted and confirmed working in Chromium (Playwright) in a later round -
  the initial overlay-only version had a real bug (native placeholder text and the overlay both
  rendered at once, overlapping) that a first round's non-visual verification (tsc/build only) had
  missed; fixed with an inline `color: transparent` style on the native input for the same window
  the overlay is shown. Worth remembering for any future CSS-overlay-over-a-native-control pattern
  in this app: drawing over an element is not the same as hiding what's under it, and this class of
  bug is invisible to type-checking/build-passing verification - it needs an actual screenshot.
- `docs/decision-log.md`'s "manager-testing round 2" also hit the `BaseEntity` id-less
  `equals()`/`hashCode()` issue above a third time, in `DevDataSeeder`'s new month-long appointment
  generator: adding several freshly-built, unsaved `ClientAppointment`s to the same `Appointment`'s
  `Set<ClientAppointment>` silently kept only the first (all compared equal), capping every
  generated appointment at exactly one participant regardless of session capacity. Fixed there by
  tracking participant counts in an `IdentityHashMap` and saving `ClientAppointment` rows directly
  via their own repository instead of through the entity's `Set` field - not a general fix, just
  the same per-call-site workaround pattern used elsewhere in the codebase for this issue.
- Making trainer/room mandatory on appointment creation (manager-testing round 3) surfaced that no
  seeded trainer has any `TrainerSchedule` row - `DevDataSeeder`'s generated appointments are
  inserted directly via the repository, bypassing `AppointmentServiceImpl.create()`'s
  `validateTrainerWorkingSchedule` check (renamed from `validateTrainerAvailability` - see
  "Upgrade: appointment conflict-message decisions" in `docs/decision-log.md`) entirely, so it
  never mattered before. A manager creating a brand-new appointment for a dev-seeded trainer via
  the admin Termini form will get "Trener sa ID ... nema radnu smenu koja pokriva ..." until that
  trainer has a matching `WORKING` `TrainerSchedule` row for that date/time (create one via the
  `/manager/dnevni-raspored` trainer-schedule tab first). This is the correct existing business
  rule, not a bug - just worth knowing before assuming appointment creation is broken.
  `DevDataSeeder` could be extended to seed matching `TrainerSchedule` rows for its generated
  trainers; left out of this round's scope.
- `ManagerInsightsServiceImplTest` (`src/test/java/.../service/impl/insights/`) does not compile
  against the current `ManagerInsightsServiceImpl`/`ManagerInsightsDTO` shape - it still constructs
  the service with a 5-arg constructor (missing the `ObjectMapper` param) and asserts on a
  `dto.getInsightText()` getter that no longer exists on the structured DTO. This blocks `mvn test`
  for the **entire module** (a test-compile failure is global), not just this one test class -
  confirmed pre-existing on `main`/before the appointment-conflict-message session's changes (via
  `git stash`), not caused by any session's work. Left unfixed as out of scope for the appointment
  work that found it; whoever picks this up next should update the test to match the current
  constructor/DTO shape from the "Upgrade: manager-insights dashboard decisions" entry in
  `docs/decision-log.md`.
- `RoomServiceImplTest.create_buildsRoomFromRequestAndSaves` fails (not a compile error, an
  assertion/thrown-exception failure) against the current room minimum-size formula - it builds a
  room named "Studio A" that the "Upgrade: room minimum-size decisions" formula now rejects as
  smaller than the computed minimum (6.0m x 5.0m) for that name. Found while running the full
  suite to verify the appointment-conflict-message changes above; not caused by this session's
  appointment work and left unfixed as out of scope - the fix is either changing the test's room
  dimensions/name or reviewing whether the minimum-size formula is too strict for that case.
