-- A client can only be physically inside one room at a time, so an active manual check-in
-- (checked_out_at IS NULL) must be unique per client GLOBALLY, not per (room, client) - see
-- AGENTS.md ("Upgrade: schema decisions").
--
-- The non-unique idx_room_check_in_client_open index added in V1.0012 only sped up the
-- "does this client have an open check-in" lookup; it did not prevent a client from having
-- more than one open check-in across different rooms. Replace it with a UNIQUE partial index
-- that both enforces the invariant at the DB level and still serves that same lookup.
DROP INDEX IF EXISTS idx_room_check_in_client_open;

CREATE UNIQUE INDEX uq_room_check_in_one_active_per_client
    ON room_check_in (client_id)
    WHERE checked_out_at IS NULL;
