# Decision log — upgrade/codex

Puna istorija odluka i verifikacija za `upgrade/codex` granu — detaljna referenca, ne čita se automatski svaku sesiju; `AGENTS.md` sadrži trenutno-tačan sažetak.

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

### Upgrade Phase 6 decisions

- **Invite activation remains email-first with an explicit demo escape hatch.**
  The public completion screen accepts the registration key from the activation
  URL and never creates an account by itself. After a manager creates a user,
  trainer, or client profile, the administration UI shows the complete link in
  a clearly labelled dev/demo modal with copy/open actions. The modal states
  that production delivery is email-only; this keeps the real invite contract
  visible while allowing a defense environment without working SMTP credentials.
- **Administration separates accounts from domain profiles.** One manager page
  uses tabs for paginated/searchable User accounts, Trainer profiles, and Client
  profiles. Email is shown on every profile row, while roles and activation
  state remain account concerns. Thin list and Client CRUD endpoints were added
  because the baseline services already exposed the data but the controllers
  could not support a usable administration UI.
- **Operational calendars share one role-aware surface.** Managers see weekly
  gym opening hours, holidays, a trainer selector, and the selected trainer's
  shifts. Trainers enter the same visual language through “Moj raspored” but
  receive only `/me` data and controls. The backend derives the trainer ID from
  the JWT for every self-service write and checks ownership again for update and
  delete, so a changed URL or request body cannot target another trainer.
- **Schedule reads and edits are additive API capabilities, not a schema
  redesign.** Gym schedules, holidays, trainers, clients, and trainer schedules
  gained the minimum list/update/delete operations required by the UI. Existing
  manager create routes remain intact, `TrainerScheduleDTO` now includes its
  existing entity date, and no migration was necessary.
- **Payments are role-scoped views of appointment credits, not monetary
  revenue.** Managers can list all Payment rows, optionally filter by client,
  and record a package through the existing write contract; clients can only
  read `/api/payment/me`, whose client identity is derived from the JWT. The UI
  deliberately labels values as purchased appointments because Payment still
  has no amount or currency columns.
- **The daily calendar is manager-only.** Its aggregate payload contains every
  appointment and its linked trainer/client identities, so exposing it to all
  authenticated roles would exceed both trainer ownership and client self-data
  boundaries. Managers receive a dedicated date-driven timeline; trainers keep
  their narrower self-service schedule view.
- **Deleting a domain profile also removes its matching role, not its User.**
  Trainer and Client deletion now remove `TRAINER`/`CLIENT` through the existing
  `UserService.removeRole` mechanism in the same transaction. The account and
  any unrelated roles remain available until a manager explicitly deletes the
  User from the separate user-administration tab.
- **Serbian operational dates explicitly request Latin script.** Schedule and
  holiday labels use the `sr-Latn-RS` locale instead of relying on the browser's
  default script for `sr-RS`, preventing Cyrillic weekday/month abbreviations
  from appearing inside the otherwise Latin-script interface.
- **Phase 6 uses a deliberately broad validation fallback, with authorization
  kept distinct.** The existing `ApiException` and database-integrity handlers
  retain their specific statuses, while otherwise-unclassified
  `RuntimeException`s return HTTP 400 with a minimal `{message}` body. This
  includes `IllegalArgumentException`, `EntityNotFoundException`, and genuine
  unexpected runtime failures such as a null dereference; the coarse
  classification matches the comparison scope and is not presented as a
  complete production exception taxonomy. A later regression pass added the
  required more-specific `AccessDeniedException` handler, so appointment
  ownership/profile failures now return HTTP 403 instead of being swallowed by
  this 400 fallback. Frontend error banners prefer the server message and use
  their generic status fallback only when the response has no textual message.

### Upgrade Phase 7 decisions

- **Appointments keep the existing marketplace model and add one self-scoped
  history endpoint.** `GET /api/appointment/me` serves both `TRAINER` and
  `CLIENT`, derives the domain profile from the authenticated JWT email, and
  returns the full past/future list ordered newest-first. No profile id is
  accepted from the caller. The frontend separates upcoming and historical
  rows, while trainers additionally see future unassigned slots and clients see
  future non-full slots. Marketplace queries now exclude past start times.
- **Future-only actions are enforced on both sides.** Trainers may assign or
  unassign only future appointments and cannot take an already assigned slot.
  Clients cannot reserve a past or duplicate appointment. Client cancellation
  retains the baseline service rule requiring at least 24 hours of notice; the
  UI disables that action inside the deadline and explains it, while the
  backend remains authoritative.
- **The demo dataset is application-seeded and relative to startup date.** A
  dev-profile `ApplicationRunner` uses JDBC inside one transaction after Flyway
  has completed. It adds three activated trainers, five activated clients,
  seven-day gym hours, one upcoming maintenance holiday, trainer work shifts,
  eight weeks of past and four weeks of future appointments, payments, room
  check-in history, and seven biweekly measurement points plus three personal
  records per demo client. Existing Phase 3 Gym/Room seed data is reused rather
  than duplicated. JDBC is deliberate here: this is demo-fixture generation,
  not business workflow, and direct batched relational inserts avoid paid email
  notifications and hundreds of service-level side effects at every dev start.
- **Idempotence uses a known activated trainer account as the dataset marker.**
  If `marko.trener@momentum.demo` exists, the runner skips the entire dataset;
  otherwise all inserts succeed or roll back together. This preserves developer
  edits and prevents duplicates on restart. It intentionally does not attempt a
  partial repair if someone manually deletes part of the fixture; removing the
  marker opts into a fresh all-or-nothing seed on an otherwise clean database.
- **Cancelled appointments cannot be represented historically by the current
  schema.** `Appointment` and `ClientAppointment` have no status/cancellation
  timestamp. A cancellation is therefore represented only by removal of the
  client link, so the seeder creates realistic occupied and open historical
  slots but cannot label former reservations as cancelled without an additive
  audited schema migration. Phase 7 does not invent that larger lifecycle.
- **JWT signing is explicitly HS256.** JJWT previously selected HS384
  automatically when a valid secret happened to exceed 47 bytes, while Spring's
  resource-server decoder accepts HS256; bearer requests then failed with 401.
  Both access and refresh token generation now pin HS256, matching the documented
  auth contract for every allowed secret length.

### Upgrade Phase 9 decisions

- **Manager appointment operations use a dedicated screen backed by the daily
  calendar snapshot.** The manager creates a slot and then manages its trainer
  and roster in one date-scoped surface. This keeps the Phase 6 read-only daily
  timeline intact while making the previously backend-only marketplace source
  usable. The create contract now accepts an optional `roomId`, and a
  manager-only session-type read endpoint supplies the seeded session IDs so
  the frontend never hardcodes database identifiers. No schema change was
  required because `Appointment.room` already existed. The screen defaults to
  tomorrow rather than today so its initial 09:00 slot is always a valid future
  planning target, including when a manager opens the page late in the day.
- **Manager roster changes preserve appointment credits.** The inherited helper
  decremented a client's remaining credits twice when staff added them and did
  not restore a credit on staff removal. The manager flow now adjusts each
  tracking row exactly once, rejects capacity overflow and scheduling overlap,
  and treats adding an already-linked client as idempotent.
- **Progress correction reuses the existing ownership-protected API.** PUT and
  DELETE already existed for both measurements and personal records, so Phase 9
  adds trainer-only edit/delete controls and pre-populated forms without a new
  backend contract. Client progress remains read-only. Every mutation reloads
  entries, records, charts and the cache-backed narrative state immediately.
- **Manual occupancy stays on the live plan and is available to both staff
  roles.** The live route is now accessible to MANAGER and TRAINER, with a small
  room/client check-in form beside the existing WebSocket-driven snapshot. A
  narrow staff-only occupancy-client endpoint supplies selector options instead
  of exposing manager account administration to trainers. Check-in/out uses the
  existing REST mutations; the existing `/topic/gym/occupancy` full snapshot is
  still the sole real-time update mechanism.
- **Phase 9 service tests emphasize security-sensitive state transitions.**
  Mockito coverage now exercises login success/failure, valid registration,
  password-reset key lifecycle, role add/remove, Trainer and Client profile
  ownership cleanup, and GymSchedule/Holiday CRUD boundaries. These remain fast
  unit tests with mocked repositories/email/JWT boundaries; browser QA covers
  the cross-role operational flows.

## Session log

## Codex parity audit (items 1-20)

- **Items 1, 3, 5 and 6:** added additive ADMIN and the backend MANAGER-role
  gate; user creation now flushes before activation email; activation/reset
  links use `FRONTEND_URL`; network errors no longer disclose backend port.
- **Items 7, 9, 10, 12 and 14:** adjacent overnight gym hours cannot overlap.
  Appointment create/assignment rejects holidays and trainer/room conflicts,
  distinguishes missing shift from double-booking, and reports email/name plus
  the exact conflicting time instead of numeric IDs.
- **Items 11 and 13:** room create/update and Konva resize share content-aware
  minimum dimensions; `.content-error` preserves multiline server messages.
- **Item 8:** immutable applied migrations were not edited. Dev-only manager
  `POST /api/dev/reseed` truncates application tables except Flyway history,
  rebuilds roles/sessions/base accounts/floor plan, then generates relative
  operational data in one transaction.
- **Items 16-18:** all trainer/client notification methods honor EMAIL/PUSH/BOTH.
  Self lookup and preference endpoints let `NotificationCenter` subscribe to
  every held trainer/client topic and edit preference without a foreign ID.
- **Item 19:** full reseed replaces username-only legacy identities with
  email-shaped `admin@momentum.rs`, `ogi@momentum.rs`, `citva@momentum.rs`.
  Deployment must map these to owned mailboxes for real delivery.
- **Items 2, 4, 15 and 20:** no change: DB exceptions were already masked;
  explicit repository deletion avoids the BaseEntity/Set cascade path; the
  seeder's two daily slots/resource rotation do not collide; and appointment
  room selection already excludes LOCKER_ROOM. The rebuilt fifth room is also
  training-suitable.

Verification on 2026-08-15: `mvnw.cmd test -q` and `npm run build` passed.

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
- 2026-08-08: Upgrade Phase 6 (`upgrade/codex`). Added public invite activation,
  forgot-password and reset-password screens; manager administration for user,
  trainer, and client records with an explicitly labelled demo activation link;
  gym hours and holiday management; manager trainer-schedule oversight; and
  JWT-derived trainer self-service schedule routes/UI. Backend and frontend
  builds passed, and an isolated fresh database verified activation/login,
  schedule and holiday writes, own-schedule reads, and HTTP 403 on a cross-trainer
  self-service update attempt.
- 2026-08-08: Phase 6 continuation (`upgrade/codex`). Added manager payment
  history/filter/create and client self-payment history, restricted the global
  daily calendar to managers and added its timeline UI, and made Trainer/Client
  profile deletion transactionally remove the corresponding role while
  preserving the underlying User account.
- 2026-08-08: Upgrade Phase 7 (`upgrade/codex`). Added JWT-scoped trainer/client
  appointment history, trainer self-assignment marketplace and client booking/
  cancellation UI, plus an idempotent relative dev seeder with a defense-ready
  operational dataset. A fresh-volume rehearsal applied all 17 migrations,
  generated past/future data, verified restart idempotence, and completed live
  client reserve/cancel and trainer assign/own-list flows.
- 2026-08-08: Phase 6/7 completion hardening (`upgrade/codex`). Added focused
  tests for trainer self-service ownership, Client CRUD, payment scoping,
  manager-only calendar access, appointment marketplace actions and self-scoped
  history. Fixed the broad runtime handler's `AccessDeniedException` regression
  with an explicit HTTP 403 handler and MVC test. Rewrote the defense runbook as
  a complete activation-to-booking scenario. A destructive fresh-volume run
  cleaned stale Maven output, applied all 17 production/dev migrations, started
  backend and frontend, and verified activation/login, manager holiday creation,
  five-room live occupancy check-in/out, seeded progress, trainer self-service,
  client reserve/cancel, trainer assign/unassign, live forbidden response, and
  unchanged fixture counts after backend restart. Runtime AI calls returned the
  intentional HTTP 503 because this verification environment had no
  `ANTHROPIC_API_KEY`; fake-Claude automated tests remained green.

## Final upgrade summary (Phases 1-7)

The upgrade now covers the full usable application lifecycle, not only its three
original demonstration pillars. **Phase 1** added the migration-driven, audited
Gym/Room/check-in and client-progress data model: one real installation row,
rotated-rectangle room geometry, an optional appointment-room link, historical
manual check-ins with one globally active room per client, typed body metrics,
and free-text personal records with fixed units. **Phase 2** exposed that model
through manager-controlled floor-plan CRUD, staff check-in/out, timezone-aware
REST/STOMP occupancy snapshots, trainer-owned/client-self progress APIs, and
pinned Claude Haiku narratives with purpose-specific Redis TTLs. Manager
“revenue” remains an explicitly labelled purchased-appointment proxy because
Payment has no amount or currency.

**Phases 3 and 4** introduced the React 19/TypeScript/Vite SPA, durable multi-role
JWT sessions with refresh, a responsive React-Konva room editor, animated live
occupancy, manager insight presentation, and a shared Recharts progress view.
AI text is rendered as plain React text rather than HTML/Markdown, trainer client
discovery follows the same appointment-history ownership rule as the backend,
and clients receive read-only self-scoped progress screens. The frontend stores
bearer tokens in local storage as a documented thesis-scale trade-off.

**Phase 5** hardened the three pillars with focused repository/service tests, a
fake Claude boundary, explicit loading/disconnection UI, a defense runbook, and
the first destructive fresh-volume rehearsal. **Phase 6** completed public
invite activation and password recovery, manager account/Trainer/Client CRUD,
weekly gym hours and holidays, manager schedule oversight, JWT-derived trainer
self-service shifts/absence, role-scoped payment history, and a manager-only
aggregate daily calendar. Deleting a domain profile removes only its matching
role, while the underlying User and unrelated roles remain.

**Phase 7** completed the trainer/client booking lifecycle: one JWT-scoped
`/api/appointment/me` history endpoint, future-only client marketplace
reservation with a 24-hour cancellation deadline, and trainer self-assignment/
unassignment of future unassigned slots. A transactional dev `ApplicationRunner`
creates a date-relative, defense-ready dataset and uses one known trainer email
as an all-or-nothing idempotence marker. Cancellation history cannot be retained
because the inherited schema represents cancellation by deleting the
ClientAppointment link and has no status/timestamp column.

The final completion pass extends automated coverage to Phase 6/7 security and
business flows and makes Spring Security `AccessDeniedException` consistently
return HTTP 403 ahead of the broad RuntimeException-to-400 fallback. The final
defense runbook follows activation, manager operations, all three wow pillars,
trainer self-service, and client/trainer booking in one role-labelled scenario,
with core/time-permitting branches and a plan B for every external dependency.

The implementation deliberately retains the baseline's interceptor-based REST
authorization, stateless non-rotating refresh tokens, explicit Flyway/Envers
migrations, single-installation Gym assumption, browser-local token storage, and
the remaining known issues above. AI endpoints require a real
`ANTHROPIC_API_KEY` and return HTTP 503 rather than fabricated text when it is
absent; automated tests fake only the Claude boundary. Fresh-volume verification
requires a clean Maven output directory as well as an empty database volume so
deleted/renamed resource artifacts cannot survive in `target/classes` and appear
to Flyway as duplicate migrations.

## 2026-08-15 - Security and full-stack live-audit follow-up (`upgrade/codex`)

- **Password hashes are no longer part of the read DTO contract.** `UserDTO.password`
  and the now-unneeded `UserMapper.toEntity(UserDTO)` direction were removed rather
  than merely annotated or ignored. No legitimate caller read that field; write flows
  already use purpose-specific requests. This makes the guarantee structural for
  `/api/user/me`, manager user reads, and nested users in `TrainerDTO`/`ClientDTO`.
  A serialization regression test covers all three shapes, and live requests as an
  ordinary TRAINER and CLIENT returned no `password` key.
- **Fresh startup exposed a production/dev Flyway version collision.** Production
  `add_admin_role` and dev `fix_demo_trainer_birth_year` were both numbered 1.0017,
  so a clean dev database could not start. The production migration was moved to the
  next unused version, 1.0018. Stale `target/classes` can independently retain renamed
  migrations, so live rehearsals begin with `mvnw clean`.
- **Live reseeding exposed broken entity equality.** Lombok `@Data` on `BaseEntity`
  made distinct unsaved `UserRole` objects equal when their audit fields matched;
  the seed ADMIN role vanished from the user's `Set`, despite both database rows
  existing. `BaseEntity` now generates accessors only and uses identity equality.
  The JWT and `/api/user/me` now retain `MANAGER,ADMIN`, with a focused Set test.
- **ADMIN is kept as a capability, not selected as a frontend workspace.** Once the
  lost role was restored, login role selection chose ADMIN first and sent the seed
  administrator to an unrelated fallback UI. `authStore` now selects only operational
  MANAGER/TRAINER/CLIENT roles while retaining ADMIN in the JWT role set.
- **The delivered activation URL used the right origin but the wrong frontend path.**
  The template now points to `/complete-registration?key=...`, matching the router;
  a real template-rendering test protects the full URL. Live startup also showed that
  the documented repository-root `.env` was not found from `Backend/demo`, so
  `springdotenv.directory` now explicitly points two levels upward.
- **Notification timing is test-configurable.** The upcoming-appointment sweep keeps
  its hourly default but accepts `app.notifications.upcoming-cron`; the live audit
  temporarily used five-second sweeps to exercise CLIENT EMAIL/PUSH/BOTH without
  changing production behavior.

### Live verification evidence

An isolated `fm_codex_live` database was migrated from empty and reseeded through
`POST /api/dev/reseed`. The run verified the five expected rooms; ADMIN-only manager
grant (ADMIN 200, ordinary MANAGER 403); adjacent overnight gym-hour rejection;
holiday rejection with date; trainer and room overlaps with email/name and exact
slot; frontend and API room minimum enforcement; multiline `pre-wrap` errors;
backend-offline login text without port 8088; persisted sidebar preferences; and
readable STOMP notifications for trainer and client EMAIL/PUSH/BOTH choices. SMTP
dispatch was observed, but placeholder Gmail credentials cannot prove delivery to a
real inbox; template rendering is independently covered. Captured UI states are
`docs/live-qa-floor-editor.png` and `docs/live-qa-notification-center.png`.

## 2026-08-15 - Five-part operational upgrade

### Part 1: realistic current-month demo data

- The dev fixture now represents one plausible gym: exactly five trainers and
  fifty transliterated Serbian client identities, 140 appointments across every
  date of the current month, a 15/35/50 weighting across individual/small-group/
  large-group sessions, and exactly 25% trainer-less marketplace slots.
- WORKING schedule rows are derived after appointment generation from each
  trainer/date's actual earliest and latest assigned slot. Payments are derived
  from real bookings: 45 clients are fully paid and five intentionally owe two
  booked sessions. Every client receives seven measurement snapshots spanning
  six months and three monthly points for one personal-record exercise.
- Live `POST /api/dev/reseed` against isolated `fm_codex_live` returned 204 and
  SQL verification produced: 5 trainers, 50 clients, 140 appointments over all
  31 August dates, 35 unassigned, 105 WORKING rows, 350 measurements, 150 records,
  two holidays, and five debtors.
- Live QA also exposed a reseed/scheduler deadlock risk in the former procedural
  per-table TRUNCATE loop. Reseed now builds one ordered `TRUNCATE ... RESTART
  IDENTITY CASCADE` statement, acquiring the wipe's locks as a set.

### Part 2: complete progress history and honest AI state

- Personal-record entry remains free text but uses a datalist populated from the
  selected client's existing exercise names. A dedicated chart groups records by
  exercise and lets the user select one series, avoiding mixed units/scales. The
  measurement history now prints all seven metrics rather than weight alone.
- Summary loading no longer attaches `.catch(() => null)`. Data and AI requests are
  handled separately: a failed regeneration shows the backend message in the
  narrative card and deliberately leaves the last successful narrative visible.
- The real root `.env` key had a valid Anthropic shape. A live forced request for a
  seeded client returned model `claude-haiku-4-5-20251001` and a substantive
  narrative. Browser QA then verified the chart, editable exercise suggestion,
  complete metric row, and—using a backend-shaped forced 503 response—that the
  visible error does not erase the previous text. Evidence:
  `docs/live-qa-progress-ai.png`.

### Part 3: trainer month calendars and recurring shifts

- A reusable dependency-free `MonthCalendar` owns only its visible month and
  receives selected date/highlight sets from each page. Trainer appointments and
  own schedules still fetch complete lists, then filter locally to the selected
  ISO date; marketplace data remains independently visible.
- Both manager and trainer shift forms expose an eight-week fixed-schedule option.
  The backend validates every weekly instance independently, skips failed dates,
  returns `createdCount` plus skipped reasons, and throws a multiline per-date
  error only when none can be created.
- Live self-service creation returned `createdCount=8` and eight persisted weekly
  rows. Browser QA found 21 highlighted appointment days, 21 schedule days, and
  the recurring checkbox. Evidence: `docs/live-qa-appointments-calendar.png` and
  `docs/live-qa-schedule-calendar.png`.

### Part 4: confirmed symmetric schedule overwrite

- Both schedule request shapes carry `confirmOverwrite`. A shared overlap resolver
  returns HTTP 409 with code `SCHEDULE_OVERLAP_CONFIRMATION_REQUIRED`; the frontend
  displays a modal and retries explicitly. Confirmed replacement deletes all rows
  overlapping the new range before inserting it, in one transaction.
- Any trainer appointment overlapping the requested range blocks replacement even
  when confirmed, with a date-specific message. Live API QA verified WORKING→away
  and away→WORKING as 409 then 201, and a booked 2026-08-29 slot as HTTP 400.
- Live QA found a precision bug in the first full-day implementation: JDBC rounded
  `LocalTime.MAX` to midnight, making away-side overlap queries empty. The boundary
  is now explicit `23:59:59`. Evidence: `docs/live-qa-schedule-overwrite.png`.

### Part 5: appointment-scoped trainer check-in

- `GET /api/appointment/me/today-upcoming` is TRAINER-only, derives identity from
  JWT, filters today's assigned appointments to starts at/after now, sorts them,
  and returns at most two. The trainer live plan no longer downloads arbitrary
  occupancy clients or offers a room selector; managers retain that reception flow.
- “Započni trening” is page-local UI state because it only expands controls and has
  no operational meaning after refresh. No `appointmentId` was added to
  `RoomCheckIn`: the persisted event describes physical client/room presence and
  occupancy already derives scheduled participation separately. The appointment
  safely constrains the frontend's client and room arguments without conflating the
  two evidence sources in the schema.
- Live API QA returned the 17:00 Pulse studio appointment with three reserved
  clients. Browser QA exposed exactly those three, no global client selector,
  preselected the room, and completed check-in/check-out. Evidence:
  `docs/live-qa-trainer-appointment-checkin.png`.

## 2026-08-15 - Selected-day appointment clarity

- Trainer “Moji termini” now classifies the selected calendar day using only its
  ISO date. Past dates omit the structurally irrelevant upcoming section, future
  dates omit history, and today retains both because its start times can straddle
  the current moment.
- The trainer marketplace is filtered by the same selected date before count,
  empty-state, and cards are rendered. Client marketplace behavior remains a
  complete future list because the client view has no selected-day calendar.
- Live browser QA selected 14, 16, and 15 August respectively and confirmed the
  past-only, future-only, and today-both section states; marketplace cards on the
  16th all belonged to that date. Evidence: `docs/live-qa-appointments-date-sections.png`.

## 2026-08-15 - Structured progress narrative presentation

- The client-progress narrative now splits on the blank line required by the
  backend prompt and presents labelled “Sažetak” and “Preporuka” regions. The
  recommendation receives a distinct accent treatment so the actionable part is
  visually separate without changing or interpreting model content.
- If an upstream response lacks a blank line, the former per-line plain-text
  rendering remains the fallback. Both paths strip only optional bullet prefixes
  and continue to render React text nodes, never HTML or Markdown.
- Live browser QA loaded a real cached Anthropic narrative for a seeded trainer
  client, asserted both labelled regions and non-empty recommendation content,
  and captured `docs/live-qa-progress-narrative.png`.

## 2026-08-15 - Self-assign creates the missing exact shift

- Trainer marketplace assignment now checks genuine appointment overlap first.
  When there is no conflict and no WORKING row covers the slot, it creates one
  exact appointment-length WORKING schedule row and assigns the appointment in
  the same transaction. Manager appointment creation and manager trainer changes
  retain the existing strict missing-shift rejection.
- An overlapping holiday/vacation/other schedule row is still rejected instead
  of being silently overwritten; the explicit schedule-overwrite confirmation
  flow remains the only operation authorized to replace schedule state.
- Focused tests cover the exact generated date/time/status and prove that a real
  appointment collision prevents both appointment assignment and schedule save.
- Live browser QA claimed previously unassigned 09:00–10:00 slots on 16 and 17
  August for a trainer with no covering shift. Each claim moved into “Moji
  termini”, and “Moj raspored” immediately showed an exact 09:00–10:00 “Radno
  vreme” row on the selected date. Evidence: `docs/live-qa-auto-shift-assignment.png`.
- Unrelated observation (reported, not fixed in this scoped change): probing an
  incorrect appointment helper URL reproduced the existing `RoleInterceptor`
  assumption that every handler is a `HandlerMethod`; the unmapped request became
  a 400 class-cast response instead of 404. It is now listed under Known issues.

## 2026-08-15 - Auto-shift revalidates current calendar constraints

- An open appointment can outlive the gym-hours/holiday configuration under
  which it was created. The self-assign missing-shift branch therefore reuses
  `validateGymSchedule(...)` and the same holiday predicate/message immediately
  after schedule-overlap validation and before persisting the generated shift.
- Regression tests prove both a newly-added holiday and shortened gym hours
  reject assignment without saving either the WORKING row or appointment.
- Live QA added a holiday over the open 2026-08-18 07:00–08:00 slot, then tried
  to claim appointment 77 as `ogi@momentum.rs`: the API returned 400 with the
  holiday date, the appointment remained unassigned, and no exact shift existed.

## 2026-08-15 - Past trainer dates are archive-only

- The selected-day marketplace is now guarded by the same `isPastDate` condition
  as the upcoming section. A past trainer date renders only “Održani termini”;
  today and future dates retain the date-filtered marketplace.
- Live browser QA selected 14 August and asserted that “Održani termini” was
  visible while both “Termini izabranog dana” and “Termini bez trenera” were
  absent from the rendered accessibility tree.

## 2026-08-15 - Personal-record chart filter styling

- `PersonalRecordChart` now gives its wrapper and exercise label explicit,
  component-scoped classes. The selector follows the existing light form
  border, radius, padding and background, with matching uppercase label and a
  visible green keyboard-focus state; chart behavior and data remain unchanged.
- Browser QA asserted the rendered 9px radius and `#fafcf9` background. Matched
  full-page captures are `docs/live-qa-record-filter-before.png` and
  `docs/live-qa-record-filter-after.png`.

## 2026-08-15 - Sidebar notification-center redesign

- `NotificationCenter` now has a separated sidebar section, the same uppercase
  preference-label language as other sidebar controls, and a custom summary pill
  with hidden native marker, green bell/count accents and hover/open states.
- Notification history opens as a bounded, scrollable dark card above the pill.
  Opening upward is deliberate: the center lives immediately above the bottom
  profile card, so an in-flow/downward panel would push or clip account controls.
- The first live screenshot exposed that an upward panel also covered the
  preference control when that control preceded the bell. Reordering the bell
  before the preference label keeps both visible while the card is open.
- Browser QA kept the panel open, asserted its absolute card placement/dark
  background and the pill radius, and confirmed the preference label/select
  remained visible. Before/after evidence is
  `docs/live-qa-notification-sidebar-before.png` and
  `docs/live-qa-notification-sidebar-after.png`.

## 2026-08-15 - Explicit notification-preference styling

- The preference select now carries `notification-preference` and directly owns
  the same padding, border, radius, foreground and dark background it previously
  received only incidentally from `.sidebar select`. The rendered appearance is
  unchanged, but future sidebar selector refactors cannot silently unstyle it.
