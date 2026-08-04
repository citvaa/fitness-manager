CREATE SEQUENCE room_check_in_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE room_check_in (
    id INT DEFAULT nextval('room_check_in_s') PRIMARY KEY,
    room_id INT NOT NULL REFERENCES room(id),
    client_id INT NOT NULL REFERENCES client(id),
    checked_in_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_out_at TIMESTAMP NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

-- Fast lookup of who is currently checked into a room (checked_out_at IS NULL).
CREATE INDEX idx_room_check_in_open ON room_check_in (room_id) WHERE checked_out_at IS NULL;
CREATE INDEX idx_room_check_in_client_open ON room_check_in (client_id) WHERE checked_out_at IS NULL;
