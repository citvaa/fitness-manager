-- Dev-only, known passwords for the three seeded test accounts from
-- V1.0009 (admin/MANAGER, ogi/TRAINER, citva/CLIENT) - NEVER use this
-- pattern outside the dev profile.
--
-- V1.0002/V1.0009 give these accounts real bcrypt hashes, but for
-- plaintext passwords nobody wrote down anywhere in the repo - which meant
-- the only way to log in as any of them locally was a one-off manual
-- UPDATE against the running dev database, which doesn't survive a
-- `docker compose down` + fresh volume. This migration replaces those
-- hashes with the bcrypt hash of a single known dev password so login
-- works out of the box after any reset. See README.md for the actual
-- plaintext password documented alongside these emails.
--
-- Bcrypt hash below is BCryptPasswordEncoder-compatible (Spring Security
-- accepts the $2a/$2b/$2y prefixes interchangeably) and was generated
-- with a bcrypt library, not typed by hand.

UPDATE "user"
SET password = '$2b$10$htamZPwNeKZiqPdnr19YiugQsOsWze.ADjn8sqfporVj7jl0.opbK'
WHERE email IN ('admin', 'ogi', 'citva');
