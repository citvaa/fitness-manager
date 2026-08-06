-- Dev-only seed data for the live gym floor plan (Frontend upgrade phase,
-- upgrade/claude-code branch). Without this, a freshly cloned dev database
-- has no Gym/Room rows at all, so the live floor plan and room editor have
-- nothing to show until a manager creates them by hand through the UI.
--
-- Guarded with WHERE NOT EXISTS (rather than the unconditional plain INSERT
-- V1.0009 uses) because, unlike that file, this one can run against a
-- database that already has a manually-created Gym/Room from testing the
-- editor UI - the guard makes it a no-op there instead of adding a
-- duplicate Gym row or a second copy of every seed room.
--
-- Version number picked as V1.0016 (not V1.0010) because dev-data and
-- db/migration share one Flyway schema history table (see
-- application-dev.yaml `flyway.locations`) and therefore one global version
-- sequence - V1.0010 through V1.0015 are already taken by db/migration.

INSERT INTO gym (name, address, contact_email, contact_phone, primary_color, timezone)
SELECT
    'FitPro Gym',
    'Bulevar oslobođenja 12, Novi Sad',
    'info@fitpro.rs',
    '+381601234567',
    '#2f83fb',
    'Europe/Belgrade'
WHERE NOT EXISTS (SELECT 1 FROM gym);

INSERT INTO room (gym_id, name, type, capacity, pos_x, pos_y, width, height, rotation_degrees, color)
SELECT g.id, r.name, r.type, r.capacity, r.pos_x, r.pos_y, r.width, r.height, r.rotation_degrees, r.color
FROM (SELECT id FROM gym ORDER BY id LIMIT 1) g
CROSS JOIN (VALUES
    ('Sala za tegove', 'WORKOUT_FLOOR', 25, 0.0, 0.0, 12.0, 10.0, 0.0, '#2f83fb'),
    ('Kardio zona', 'WORKOUT_FLOOR', 20, 13.0, 0.0, 10.0, 10.0, 0.0, '#0ea5e9'),
    ('Joga studio', 'STUDIO', 15, 0.0, 11.0, 8.0, 6.0, 0.0, '#a855f7'),
    ('Svlačionica', 'LOCKER_ROOM', 30, 9.0, 11.0, 6.0, 6.0, 0.0, '#64748b'),
    ('Recepcija', 'RECEPTION', 5, 16.0, 11.0, 7.0, 4.0, 0.0, '#f59e0b')
) AS r(name, type, capacity, pos_x, pos_y, width, height, rotation_degrees, color)
WHERE NOT EXISTS (SELECT 1 FROM room);
