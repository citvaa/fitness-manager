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
4. Log in with a dev-seeded account (see `db/dev-data/V1.0009__insert_test_data.sql`
   for seeded roles; the `admin` MANAGER account's password is not
   documented in plaintext anywhere in the repo — set one via the database
   directly for local testing, e.g. a bcrypt hash of a known password).

## Structure

- `src/auth/` — token storage (Zustand store), silent refresh, protected
  route guards.
- `src/lib/http.ts` — axios instance with auth header injection and 401
  retry-after-refresh.
- `src/features/gym/` — Room/Gym API client, the drag/resize/rotate room
  editor (react-konva), and the live occupancy floor plan (WebSocket).
- `src/layout/AppShell.tsx` — role-gated nav shell with a role switcher for
  multi-role accounts.
