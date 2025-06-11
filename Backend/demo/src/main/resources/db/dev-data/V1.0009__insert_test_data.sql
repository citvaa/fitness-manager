INSERT INTO "user" (id, email, password, notification_preference, registration_key, registration_key_validity, is_activated, reset_key, reset_key_validity, version)
VALUES
    (2, 'ogi', '$2a$10$.nadLkHtJb655aoMMfJgYu6tTWp9AC7/sjVK2MhXrLcImHnv.7GLK', 'PUSH', null, null, true, null, null, 1);

INSERT INTO trainer (user_id, employment_date, birth_year, status)
VALUES (2, '2025-06-11', 0, 'FULL_TIME');

INSERT INTO "user" (id, email, password, notification_preference, registration_key, registration_key_validity, is_activated, reset_key, reset_key_validity, version)
VALUES
    (3, 'citva', '$2a$10$6ub4nf5/FogjSs1hHWWRouUIUigcoLlNpS.j7/wD1m9qNX1gno3pa', 'PUSH', null, null, true, null, null, 1);

INSERT INTO client (user_id) VALUES (3);

INSERT INTO user_role (user_id, role) VALUES (2, 'TRAINER'), (3, 'CLIENT');

INSERT INTO gym_schedule (day, opening_time, closing_time) VALUES ('MONDAY', '00:00:00', '23:00:00');