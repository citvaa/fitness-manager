# Fitness Manager — Frontend

React + TypeScript + Vite SPA for the `fitness-manager` backend. See the
repo root `AGENTS.md` ("Upgrade: frontend decisions") for why this stack and
every other non-obvious choice was made.

## Running locally

1. Backend must be running on `http://localhost:8088` (see the root
   `AGENTS.md` "Running locally" section) with Postgres/Redis up.
2. `npm install`
3. `npm run dev` — dev server on `http://localhost:5173` (already allowed by
   the backend's CORS config).
4. Log in with a dev-seeded account. **Dev-only credentials, never valid in
   production** (see `db/dev-data/V1.0017__set_known_dev_test_passwords.sql`
   - only applied on the `dev` Flyway profile):

   | Email    | Password      | Role    |
   |----------|---------------|---------|
   | `admin`  | `password123` | MANAGER |
   | `ogi`    | `password123` | TRAINER |
   | `citva`  | `password123` | CLIENT  |

## Structure

- `src/auth/` — token storage (Zustand store), silent refresh, protected
  route guards.
- `src/lib/http.ts` — axios instance with auth header injection and 401
  retry-after-refresh.
- `src/features/gym/` — Room/Gym API client, the drag/resize/rotate room
  editor (react-konva), and the live occupancy floor plan (WebSocket).
- `src/layout/AppShell.tsx` — role-gated nav shell with a role switcher for
  multi-role accounts.
