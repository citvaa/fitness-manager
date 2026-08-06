-- Dev-only seed data for the trainer/client progress-tracking screens (frontend
-- upgrade phase 4, upgrade/claude-code branch). "Has this trainer trained this
-- client" is derived entirely from shared appointment history (see AGENTS.md,
-- TrainerClientAccessGuard) - without at least one Appointment + ClientAppointment
-- row linking the seeded trainer (ogi) and client (citva), the trainer's "Moji
-- klijenti" list has nothing to show on a freshly cloned dev database, and the
-- client progress-tracking screens can't be exercised at all without first
-- creating a real appointment/reservation flow by hand.
--
-- Guarded with WHERE NOT EXISTS per the same reasoning as V1.0016 - this must
-- stay a no-op against a database that already has appointment data (e.g. from
-- manual testing), not add a duplicate past appointment every time Flyway runs
-- against an already-seeded dev database.
--
-- Looked up by trainer/client email rather than hardcoding ids 1/1, since a
-- database that has run V1.0009 with row order changes (or additional trainers/
-- clients created via the API before this migration runs) shouldn't silently
-- link the wrong two rows.

INSERT INTO appointment (date, start_time, end_time, session_id, trainer_id, version)
SELECT CURRENT_DATE - INTERVAL '3 days', '10:00', '11:00', s.id, t.id, 0
FROM (SELECT id FROM session WHERE type = 'INDIVIDUAL' ORDER BY id LIMIT 1) s
CROSS JOIN (
    SELECT tr.id FROM trainer tr JOIN "user" u ON u.id = tr.user_id WHERE u.email = 'ogi'
) t
WHERE NOT EXISTS (SELECT 1 FROM appointment);

INSERT INTO client_appointment (client_id, appointment_id, version)
SELECT c.id, a.id, 0
FROM (
    SELECT cl.id FROM client cl JOIN "user" u ON u.id = cl.user_id WHERE u.email = 'citva'
) c
CROSS JOIN (SELECT id FROM appointment ORDER BY id LIMIT 1) a
WHERE NOT EXISTS (SELECT 1 FROM client_appointment);
