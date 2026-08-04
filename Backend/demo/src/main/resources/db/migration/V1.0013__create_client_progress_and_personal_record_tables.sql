CREATE SEQUENCE client_progress_entry_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client_progress_entry (
    id INT DEFAULT nextval('client_progress_entry_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    entry_date DATE NOT NULL,
    weight_kg NUMERIC(6, 2),
    body_fat_percent NUMERIC(5, 2),
    waist_cm NUMERIC(6, 2),
    chest_cm NUMERIC(6, 2),
    hip_cm NUMERIC(6, 2),
    thigh_cm NUMERIC(6, 2),
    arm_cm NUMERIC(6, 2),
    notes VARCHAR(2000),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE INDEX idx_client_progress_entry_client_date ON client_progress_entry (client_id, entry_date);

CREATE SEQUENCE client_personal_record_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client_personal_record (
    id INT DEFAULT nextval('client_personal_record_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    exercise_name VARCHAR(255) NOT NULL,
    value NUMERIC(10, 2) NOT NULL,
    unit VARCHAR(20) NOT NULL
        CHECK (unit IN ('KG', 'LB', 'REPS', 'SECONDS', 'MINUTES', 'METERS', 'KM')),
    record_date DATE NOT NULL,
    notes VARCHAR(1000),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE INDEX idx_client_personal_record_client_exercise ON client_personal_record (client_id, exercise_name);
