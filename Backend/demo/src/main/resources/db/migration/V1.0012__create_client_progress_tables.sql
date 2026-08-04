CREATE SEQUENCE client_progress_entry_s START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE;

CREATE TABLE client_progress_entry (
    id INT DEFAULT nextval('client_progress_entry_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    entry_date DATE NOT NULL,
    weight_kg NUMERIC(6,2),
    body_fat_percent NUMERIC(5,2),
    waist_cm NUMERIC(6,2),
    chest_cm NUMERIC(6,2),
    hip_cm NUMERIC(6,2),
    thigh_cm NUMERIC(6,2),
    arm_cm NUMERIC(6,2),
    notes TEXT,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT,
    CONSTRAINT progress_weight_check CHECK (weight_kg IS NULL OR weight_kg > 0),
    CONSTRAINT progress_body_fat_check CHECK (body_fat_percent IS NULL OR body_fat_percent BETWEEN 0 AND 100),
    CONSTRAINT progress_waist_check CHECK (waist_cm IS NULL OR waist_cm > 0),
    CONSTRAINT progress_chest_check CHECK (chest_cm IS NULL OR chest_cm > 0),
    CONSTRAINT progress_hip_check CHECK (hip_cm IS NULL OR hip_cm > 0),
    CONSTRAINT progress_thigh_check CHECK (thigh_cm IS NULL OR thigh_cm > 0),
    CONSTRAINT progress_arm_check CHECK (arm_cm IS NULL OR arm_cm > 0)
);

CREATE INDEX idx_client_progress_entry_client_date ON client_progress_entry(client_id, entry_date DESC);

CREATE SEQUENCE client_personal_record_s START WITH 1 INCREMENT BY 1 MINVALUE 1 NO MAXVALUE;

CREATE TABLE client_personal_record (
    id INT DEFAULT nextval('client_personal_record_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    exercise_name VARCHAR(150) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    record_date DATE NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT,
    CONSTRAINT personal_record_value_check CHECK (value > 0),
    CONSTRAINT personal_record_unit_check CHECK (unit IN ('KG', 'LB', 'REPS', 'SECONDS', 'MINUTES', 'METERS', 'KM'))
);

CREATE INDEX idx_client_personal_record_client_date ON client_personal_record(client_id, record_date DESC);
CREATE INDEX idx_client_personal_record_client_exercise ON client_personal_record(client_id, exercise_name);
