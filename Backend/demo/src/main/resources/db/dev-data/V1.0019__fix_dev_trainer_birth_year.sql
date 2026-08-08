-- Cosmetic dev-only fix, upgrade/claude-code branch: V1.0009's seeded trainer
-- ("ogi") has birth_year = 0, which the Faza 6 admin "Treneri" screen renders
-- literally as "rodjen 0" - not a real value, just an unset placeholder that
-- was never meant to be shown to a manager. V1.0009 itself is never edited
-- (Flyway checksums it as already applied), so this is a separate migration
-- doing a plain UPDATE instead.
--
-- Looked up by email rather than hardcoding user_id/trainer id, same
-- reasoning as V1.0018 - stays correct even if row insertion order ever
-- changes. Guarded on birth_year = 0 (not WHERE NOT EXISTS, since the row
-- already exists from V1.0009) so this is a no-op if it's ever run again
-- after a manager has since set a real birth year through the admin UI -
-- this migration should only ever touch the untouched placeholder value.
--
-- 1990 is an arbitrary placeholder, not a real fact about this test
-- account - accuracy doesn't matter here, only that it stops reading as "0".

UPDATE trainer t
SET birth_year = 1990
FROM "user" u
WHERE t.user_id = u.id
  AND u.email = 'ogi'
  AND t.birth_year = 0;
