INSERT INTO role (name) VALUES ('ADMIN');

ALTER TABLE user_role_aud DROP CONSTRAINT user_role_aud_role_check;
ALTER TABLE user_role_aud ADD CONSTRAINT user_role_aud_role_check
    CHECK (role IN ('MANAGER', 'TRAINER', 'CLIENT', 'ADMIN'));

INSERT INTO user_role (user_id, role)
SELECT id, 'ADMIN' FROM "user" WHERE email = 'admin'
ON CONFLICT (user_id, role) DO NOTHING;
