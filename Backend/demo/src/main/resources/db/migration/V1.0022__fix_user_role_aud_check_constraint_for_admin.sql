-- V1.0010 created user_role_aud with a CHECK constraint listing only the three roles that
-- existed at the time (MANAGER/TRAINER/CLIENT). V1.0020 added the ADMIN role afterwards via a
-- raw SQL INSERT INTO user_role - which bypasses Hibernate Envers entirely, so no audit row (and
-- therefore no constraint violation) was ever produced for it. The gap stayed latent until
-- DevDataSeeder started creating/deleting the admin account's ADMIN UserRole through JPA (see
-- AGENTS.md "Upgrade: dev-data ownership decisions" - the reseed() bulk-delete path is Envers-
-- audited and hit this immediately). Widening the constraint to include ADMIN, matching the
-- Role enum.
ALTER TABLE public.user_role_aud DROP CONSTRAINT user_role_aud_role_check;
ALTER TABLE public.user_role_aud ADD CONSTRAINT user_role_aud_role_check
    CHECK (((role)::text = ANY ((ARRAY['MANAGER'::character varying, 'TRAINER'::character varying, 'CLIENT'::character varying, 'ADMIN'::character varying])::text[])));
