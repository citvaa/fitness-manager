CREATE SEQUENCE gym_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE gym (
    id INT DEFAULT nextval('gym_s') PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    logo_url VARCHAR(500),
    primary_color VARCHAR(20),
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Belgrade',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE SEQUENCE room_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE room (
    id INT DEFAULT nextval('room_s') PRIMARY KEY,
    gym_id INT NOT NULL REFERENCES gym(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL
        CHECK (type IN ('WORKOUT_FLOOR', 'STUDIO', 'POOL', 'LOCKER_ROOM', 'RECEPTION', 'OFFICE', 'OTHER')),
    capacity INT NOT NULL,
    pos_x DOUBLE PRECISION NOT NULL,
    pos_y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    rotation_degrees DOUBLE PRECISION NOT NULL DEFAULT 0,
    color VARCHAR(20),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE INDEX idx_room_gym_id ON room (gym_id);

-- Optional link from an appointment to the room it takes place in (nullable: existing
-- appointments predate the Room concept, and a trainer/appointment can remain unassigned to a
-- room the same way it can be unassigned to a trainer - see AGENTS.md).
ALTER TABLE appointment ADD COLUMN room_id INT NULL REFERENCES room(id);

CREATE INDEX idx_appointment_room_id ON appointment (room_id);
