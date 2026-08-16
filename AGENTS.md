# AGENTS.md

## Purpose of this repository

`fitness-manager` is a full-stack gym-management app: a Spring Boot backend
plus a React/TypeScript frontend (`Frontend/`) covering gym operations, a live
floor plan, AI insights, and client progress. It is a Master's thesis foundation.

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
  source of truth. Keep this file lean; detailed upgrade rationale and
  verification history lives in `docs/decision-log.md`.

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
- Anthropic Messages API, pinned model `claude-haiku-4-5-20251001`
- React 19, TypeScript, Vite, Zustand, react-konva, STOMP, Recharts, axios

## Running locally

1. Copy `.env.example` to `.env` and fill in `MAIL_USERNAME`, `MAIL_PASSWORD`
   (a Gmail **App Password**, not the account password), and `JWT_SECRET`
   (>= 32 characters - the app fails to start otherwise). The
   `springboot3-dotenv` dependency automatically exposes every value from the
   repository-root `.env` file to Spring's existing `${...}` placeholders for
   local development; `springdotenv.directory: ../../` is required because the
   documented launch directory is `Backend/demo`. Real process environment
   variables retain precedence.
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
  registration/reset keys with validity timestamps, `Set<UserRole>`. Password
  is deliberately absent from `UserDTO`, so it also cannot leak through the
  nested user in `TrainerDTO` or `ClientDTO`; password input belongs only in
  purpose-specific command objects. Registration and password-reset keys are
  persisted only for their dedicated flows and are never exposed through
  `UserDTO`, including nested trainer/client responses.
- **UserRole** - join entity; `role` is one of `MANAGER` / `TRAINER` / `CLIENT` / `ADMIN`.
  Every User has exactly one operational MANAGER/TRAINER/CLIENT role; ADMIN is
  additive only to the single seeded MANAGER administrator and cannot be changed
  through the generic role API. `BaseEntity` intentionally has no
  generated value equality: audit-field equality collapsed distinct unsaved
  `UserRole` values in a `Set` and silently dropped ADMIN during seeding.
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
  `VACATION`) for a given date and time range. Trainer self-service and manager
  forms can create eight weekly WORKING instances; invalid weeks are reported
  and skipped, and total failure includes one reason per attempted date.
  Creating WORKING over an existing row, or unavailability over WORKING, first
  checks trainer appointments. A booked overlap is immutable; otherwise API
  returns 409 code `SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED`, and only an explicit
  `confirmOverwrite` retry replaces the overlapping rows. The frontend uses an
  in-app confirmation modal for both directions.
  Managers retain read-only trainer-schedule access; every write route, including
  legacy non-`/me` URLs, resolves the authenticated Trainer and ignores any
  body-supplied `trainerId`.
- **Holiday** - a gym-wide non-working date.
- **Gym** - audited single-installation configuration with branding/contact
  data and IANA timezone.
- **Room** - belongs to Gym; capacity and rotated-rectangle geometry
  (`posX`/`posY`/`width`/`height`/`rotationDegrees`).
- **RoomCheckIn** - historical manual check-in/out. A DB partial unique index
  permits one active check-in per client globally. Occupancy adds manual and
  in-progress appointment counts without deduplication. Managers receive every
  remaining appointment today across the gym, while trainers receive only their next two
  not-yet-started appointments today; “Započni trening” exposes that appointment's
  roster and fixed room. Manager and trainer rosters use one stateful attendance
  toggle per client; green/check means an authoritative active check-in and the
  neutral/x state means no active check-in. Started state is intentionally page-local, while the
  physical check-in remains the existing persistent room/client event.
- **ClientProgressEntry/ClientPersonalRecord** - fixed-column body measurements
  and free-text exercise records with fixed units. Progress UI charts body
  measurements and one selected personal-record exercise at a time, suggests
  existing exercise names through an editable datalist, and lists every body
  metric. AI summary failures remain visible beside the narrative and never
  erase the previous successful insight.
- Client progress AI text is rendered as two labelled plain-text regions when
  the backend's promised blank-line separator is present: “Sažetak” followed by
  a visually emphasized “Preporuka”. Unexpected single-block output falls back
  to safe per-line paragraphs; neither path interprets Markdown or HTML.
- The personal-record chart's exercise selector uses the shared light form
  language (label typography, padded bordered control, rounded background and
  green focus state), scoped through `personal-record-chart`/`record-chart-filter`.
- Destructive frontend actions use the shared promise-based `useConfirm` dialog
  and the existing in-app overlay/card visual language; browser-native
  `confirm()` is not part of the UI contract.
- Administration email changes use an in-app validated modal; browser-native
  `prompt()` is not used for account editing.
- **Payment** has no amount/currency; manager “revenue” is a purchased-
  appointment-count proxy.
- Payment status is computed per `SessionType` from actually held client
  appointments (`date + endTime` before current Gym-zone time) versus summed
  purchased appointments. It always returns every type and clamps debt at zero;
  tracking reserved/remaining counters are not an input.
- Payment-form selects are width-constrained to their cards so long account
  emails cannot expand the layout. The reusable header `.client-picker` keeps
  its content-width/min-width behavior outside the existing mobile breakpoint.
- **Appointment** optionally belongs to Room and has no persisted status or
  cancellation timestamp.

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
    exclude-list covering register, login, login-refresh, forgot-password,
    reset-password, and Swagger, so password recovery and token refresh do
    not require a valid JWT.
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
  additionally sends email based on `User.notificationPreference`. Every
  trainer/client path observes EMAIL/PUSH/BOTH; frontend `NotificationCenter`
  subscribes to held-role topics and exposes the current user's preference. Its
  sidebar UI uses an explicit dark-theme notification pill and upward-opening
  card panel so expanding history does not displace the bottom profile controls.
  The preference select owns its visual contract through
  `.notification-preference`, rather than relying on the broad `.sidebar select` rule.
- Managers subscribe to `/topic/manager` and receive live operational broadcasts
  for client self-reservations, trainer self-assignment, and completed user
  registration. Payment creation sends the client a preference-aware PUSH/email
  confirmation; manual add-trainer also emits the trainer assignment notification.
- `NotificationScheduler` (`@Scheduled`): daily trainer/client appointment
  digests at 20:00, and an hourly sweep for appointments starting within the
  next hour. The latter accepts `app.notifications.upcoming-cron` for isolated
  live tests while retaining the hourly production default.
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
JSON serialization) in `config/cache/RedisConfig`. `TRAINER_CACHE` uses the
default TTL; `managerInsights` uses six hours and `clientProgressInsights`
uses one hour per client, with explicit refresh/eviction.

Manager AI insights calculate every numeric metric in Java (per-room live
occupancy, individual/group mix, check-in ratio, and paid appointment units).
Claude receives those fixed values and returns JSON containing only summary,
recommendations, and per-key rating/comment. Missing or invalid AI fields fall
back to `AVERAGE` and a generic comment without hiding the calculated cards.

## Conventions

- `ADMIN` is additive to `MANAGER` and immutable through REST. Generic role
  mutation rejects a second or zero operational roles. Trainer/Client creation
  atomically creates its profile and role; deleting either profile deletes the
  whole account so a role-less User cannot remain. The loaded profile is
  detached before bulk cleanup and UserRole orphans are flushed first, avoiding
  stale managed profile-to-deleted-user references.
- Administration renders employment statuses through one Serbian label map;
  raw `FULL_TIME`/`CONTRACT`/`FORMER_EMPLOYEE` values remain API-only.
- Appointment creation rejects holidays, missing trainer shifts, trainer/room
  overlaps and client overlaps. Conflict text names trainers by email and rooms
  by name, and includes the conflicting slot.
- Manager-created appointments always require a Room. The recurring manager
  command attempts eight weekly instances through the same ordinary trainer,
  room, client, gym-hours, and holiday validation; invalid weeks are reported
  and skipped, and only total failure rejects the whole command.
- Trainer self-assignment is the one missing-shift exception: claiming an open
  appointment creates an exact-time WORKING row in the same transaction when no
  schedule row overlaps it. A real appointment conflict is checked first and
  still rejects the claim; existing non-WORKING/other schedule rows are never
  silently overwritten. Before the generated shift is saved, current gym hours
  and holidays are revalidated so stale open slots cannot create invalid shifts.
- Trainer and client appointment pages, plus trainer own-schedule, use the shared dependency-free
  `MonthCalendar`; full API lists remain loaded, selected-day rows are filtered
  locally, and dates with data are highlighted. On both appointment pages, past
  selected dates show only the held section (including no marketplace), future
  dates only the upcoming section plus marketplace, and today shows both plus
  marketplace; trainer-unassigned and client-available marketplace rows are
  scoped to the selected date.
  Both trainer calendars visually mute personal HOLIDAY/SICK_LEAVE/VACATION
  dates and gym-wide Holiday dates without changing appointment highlights;
  authenticated trainers may read the holiday list for this purpose. Both
  client calendars also mute gym holidays and weekdays without valid GymSchedule
  coverage; CLIENT may read both lists, and booking controls are unavailable on
  those dates. Manager creation uses the same closure model while backend create
  and reserve validation remains authoritative.
- Client booking is split into two routes: “Zakaži trening” contains only the
  date-scoped available marketplace, while “Moji termini” contains only the
  client's existing reservations. The latter shows held-only for past dates,
  upcoming-only for future dates, and both sections today.
- The manager daily schedule also uses `MonthCalendar`; changing the selected
  day reloads its date-scoped aggregate timeline. Its create-slot form sits
  directly below the calendar in the left planning column, while the selected
  day's roster occupies the full right column.
- Activation/reset links use `app.frontend-url` (`FRONTEND_URL`, default
  `http://localhost:5173`); activation targets the frontend's real
  `/complete-registration?key=...` route, and user creation flushes before
  email is queued.
- `ADMIN` is an authorization capability, not a standalone workspace. The
  frontend active-role switcher considers MANAGER/TRAINER/CLIENT only, while
  preserving ADMIN in the held-role set used for backend authorization.
- Room geometry minimums are content-aware on server and canvas: width
  `max(100, trimmed-name-length * 10 + 32)`, height 80.
- Gym timezone editing uses an IANA-zone select sourced from browser-supported
  values, with a stable common-zone fallback containing `Europe/Belgrade`.
- Dev data can be destructively rebuilt through manager-only `POST
  /api/dev/reseed`; it preserves Flyway history and rebuilds all application
  tables plus the relative operational fixture. The fixture contains exactly
  5 trainers and 50 clients, every day of the current month (~140 appointments,
  25% trainer-less, group-weighted), WORKING ranges derived from assigned
  appointments, 90% fully paid clients plus intentional debtors, seven six-month
  measurements and three monthly personal-record points per client, two holidays,
  and weekday/weekend gym hours. Reseed truncates all application tables in one
  statement so the occupancy scheduler cannot deadlock a partially locked wipe.
  Appointment generation queries the persisted holiday and gym-hour rows before
  every candidate slot; skipped slots cannot produce derived WORKING shifts.
- `docs/defense-demo-script.md` is the current end-to-end defense runbook. Keep
  its CORE + optional structure and update it only after the user-visible flows
  it demonstrates are final, so its roles, routes, credentials, and plans B do
  not describe removed UI.

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
- `ApiExceptionHandler` preserves explicit statuses, maps database conflicts
  to 409 and access denial to 403, then broadly maps other runtime failures to
  400 with a `{"message":"..."}` body.
- **Do not edit existing Flyway migration files** (`db/migration/V1.00XX__*`)
  - their checksums are locked once applied. If schema changes are needed,
  add a new `V1.00XX__*.sql` file. Dev-only seed data lives in the separate
  `db/dev-data/` location, only loaded on the `dev` profile.

## Known issues

Only currently open items belong here; resolved history is in `docs/decision-log.md`.

- Refresh tokens have no rotation or server-side revocation.
- `AuditorAwareImpl` cannot identify interceptor-authenticated users, so `createdBy`/`updatedBy` remain empty.
- `TrainerSchedule.date` has incorrect entity-level `unique = true`; schema has no matching constraint. It should be `(trainer_id, date)`.
- Broad runtime handling reports not-found cases and bugs as 400 instead of a complete 404/500 taxonomy.
- `application.yaml` and `application-dev.yaml` duplicate nearly every property.
- A Gmail App Password remains recoverable from Git history and must be rotated.
- Appointment reservation/roster addition can reduce remaining session credits below zero.
- `RoleInterceptor` casts every resolved handler to `HandlerMethod`; an authenticated
  request to an unmapped `/api/**` URL can therefore return 400 with a class-cast
  message instead of the normal 404 response.
- GymSchedule writes do not validate that opening time precedes closing time;
  live QA found a persisted Wednesday row of `16:00-05:00`.
- `ClientMapper` does not populate `roles` on the nested `UserDTO` inside a
  `ClientDTO` (the direct user endpoint still returns roles); MapStruct reports
  this explicitly during compilation.
