INSERT INTO "user" (email, password, notification_preference, is_activated)
VALUES
    ('ogi', '$2a$10$.nadLkHtJb655aoMMfJgYu6tTWp9AC7/sjVK2MhXrLcImHnv.7GLK', 'PUSH', true);

INSERT INTO trainer (user_id, employment_date, birth_year, status)
VALUES (2, '2025-06-11', 0, 'FULL_TIME');

INSERT INTO "user" (email, password, notification_preference, is_activated)
VALUES
    ('citva', '$2a$10$6ub4nf5/FogjSs1hHWWRouUIUigcoLlNpS.j7/wD1m9qNX1gno3pa', 'PUSH', true);

INSERT INTO client (user_id) VALUES (3);

INSERT INTO user_role (user_id, role) VALUES (2, 'TRAINER'), (3, 'CLIENT');

INSERT INTO gym_schedule (day, opening_time, closing_time) VALUES ('MONDAY', '00:00:00', '23:00:00');