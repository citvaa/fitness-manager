-- Introduces a super-admin (ADMIN) role, additive to the existing MANAGER/TRAINER/CLIENT set.
-- Only an ADMIN may grant/revoke the MANAGER role from other users - see UserServiceImpl
-- addRole/removeRole and AGENTS.md "Upgrade: manager-hierarchy decisions" for the full rationale.
--
-- ADMIN is deliberately additive (kept alongside MANAGER), not a replacement: the seed admin
-- account still needs MANAGER for ordinary admin-area access (client/trainer/appointment
-- management etc.), plus ADMIN exclusively for managing the MANAGER role itself. Exactly one
-- account (the seed admin, user_id 1) gets ADMIN.
INSERT INTO role (name) VALUES ('ADMIN');

INSERT INTO user_role (user_id, role)
SELECT id, 'ADMIN' FROM "user" WHERE email = 'admin'
    AND NOT EXISTS (
        SELECT 1 FROM user_role ur
        JOIN "user" u ON u.id = ur.user_id
        WHERE u.email = 'admin' AND ur.role = 'ADMIN'
    );
