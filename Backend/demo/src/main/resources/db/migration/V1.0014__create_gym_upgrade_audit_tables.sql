CREATE TABLE gym_aud (
    id INT NOT NULL,
    rev INT NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    name VARCHAR(150), address VARCHAR(255), phone VARCHAR(50), email VARCHAR(255),
    logo_url VARCHAR(500), brand_color VARCHAR(7), timezone VARCHAR(100),
    PRIMARY KEY (rev, id)
);

CREATE TABLE room_aud (
    id INT NOT NULL,
    rev INT NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    gym_id INT, name VARCHAR(100), type VARCHAR(50), capacity INT,
    pos_x DOUBLE PRECISION, pos_y DOUBLE PRECISION, width DOUBLE PRECISION,
    height DOUBLE PRECISION, rotation_degrees DOUBLE PRECISION,
    PRIMARY KEY (rev, id)
);

CREATE TABLE room_check_in_aud (
    id INT NOT NULL,
    rev INT NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    room_id INT, client_id INT, checked_in_at TIMESTAMP, checked_out_at TIMESTAMP,
    PRIMARY KEY (rev, id)
);

CREATE TABLE client_progress_entry_aud (
    id INT NOT NULL,
    rev INT NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    client_id INT, entry_date DATE, weight_kg NUMERIC(6,2), body_fat_percent NUMERIC(5,2),
    waist_cm NUMERIC(6,2), chest_cm NUMERIC(6,2), hip_cm NUMERIC(6,2),
    thigh_cm NUMERIC(6,2), arm_cm NUMERIC(6,2), notes TEXT,
    PRIMARY KEY (rev, id)
);

CREATE TABLE client_personal_record_aud (
    id INT NOT NULL,
    rev INT NOT NULL REFERENCES revinfo(rev),
    revtype SMALLINT,
    client_id INT, exercise_name VARCHAR(150), value NUMERIC(10,2), unit VARCHAR(20), record_date DATE,
    PRIMARY KEY (rev, id)
);

ALTER TABLE appointment_aud ADD COLUMN room_id INT;
