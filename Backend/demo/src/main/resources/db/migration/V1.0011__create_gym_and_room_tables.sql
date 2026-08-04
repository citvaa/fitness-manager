CREATE SEQUENCE gym_s START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE;

CREATE TABLE gym (
    id INT DEFAULT nextval('gym_s') PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    logo_url VARCHAR(500),
    brand_color VARCHAR(7),
    timezone VARCHAR(100) NOT NULL DEFAULT 'Europe/Belgrade',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT,
    CONSTRAINT gym_brand_color_check CHECK (brand_color IS NULL OR brand_color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE SEQUENCE room_s START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE;

CREATE TABLE room (
    id INT DEFAULT nextval('room_s') PRIMARY KEY,
    gym_id INT NOT NULL REFERENCES gym(id),
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    pos_x DOUBLE PRECISION NOT NULL,
    pos_y DOUBLE PRECISION NOT NULL,
    width DOUBLE PRECISION NOT NULL,
    height DOUBLE PRECISION NOT NULL,
    rotation_degrees DOUBLE PRECISION NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT,
    CONSTRAINT room_type_check CHECK (type IN ('CARDIO', 'WEIGHTS', 'GROUP_STUDIO', 'FUNCTIONAL', 'LOCKER_ROOM', 'OTHER')),
    CONSTRAINT room_capacity_check CHECK (capacity > 0),
    CONSTRAINT room_width_check CHECK (width > 0),
    CONSTRAINT room_height_check CHECK (height > 0),
    CONSTRAINT room_name_per_gym_unique UNIQUE (gym_id, name)
);

CREATE INDEX idx_room_gym ON room(gym_id);

CREATE SEQUENCE room_check_in_s START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE;

CREATE TABLE room_check_in (
    id INT DEFAULT nextval('room_check_in_s') PRIMARY KEY,
    room_id INT NOT NULL REFERENCES room(id),
    client_id INT NOT NULL REFERENCES client(id),
    checked_in_at TIMESTAMP NOT NULL,
    checked_out_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT,
    CONSTRAINT room_check_in_time_check CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
);

CREATE INDEX idx_room_check_in_open ON room_check_in(room_id) WHERE checked_out_at IS NULL;
CREATE INDEX idx_room_check_in_client_open ON room_check_in(client_id) WHERE checked_out_at IS NULL;
