CREATE SEQUENCE gym_schedule_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE gym_schedule (
    id INT DEFAULT nextval('gym_schedule_s') PRIMARY KEY,
    day VARCHAR(10) UNIQUE NOT NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL
);

CREATE SEQUENCE holiday_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE holiday (
    id INT DEFAULT nextval('holiday_s') PRIMARY KEY,
    date DATE UNIQUE NOT NULL,
    description VARCHAR NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE SEQUENCE trainer_schedule_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE trainer_schedule (
    id INT DEFAULT nextval('trainer_schedule_s') PRIMARY KEY,
    trainer_id INT NOT NULL REFERENCES trainer(id),
    date DATE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    status VARCHAR NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);