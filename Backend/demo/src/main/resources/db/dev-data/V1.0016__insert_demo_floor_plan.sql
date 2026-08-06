WITH inserted_gym AS (
    INSERT INTO gym (name, address, phone, email, brand_color, timezone)
    SELECT 'Momentum Fitness', 'Bulevar oslobođenja 88, Novi Sad', '+381 21 555 018',
           'zdravo@momentum.rs', '#BAF252', 'Europe/Belgrade'
    WHERE NOT EXISTS (SELECT 1 FROM gym)
    RETURNING id
)
INSERT INTO room (gym_id, name, type, capacity, pos_x, pos_y, width, height, rotation_degrees)
SELECT id, room.name, room.type, room.capacity, room.pos_x, room.pos_y, room.width, room.height, room.rotation_degrees
FROM inserted_gym
CROSS JOIN (VALUES
    ('Kardio panorama', 'CARDIO', 18, 55.0, 55.0, 370.0, 205.0, 0.0),
    ('Zona snage', 'WEIGHTS', 22, 455.0, 55.0, 485.0, 205.0, 0.0),
    ('Pulse studio', 'GROUP_STUDIO', 16, 55.0, 295.0, 285.0, 260.0, 0.0),
    ('Funkcionalna arena', 'FUNCTIONAL', 20, 370.0, 295.0, 360.0, 260.0, 0.0),
    ('Svlačionice', 'LOCKER_ROOM', 12, 760.0, 295.0, 180.0, 260.0, 0.0)
) AS room(name, type, capacity, pos_x, pos_y, width, height, rotation_degrees);
