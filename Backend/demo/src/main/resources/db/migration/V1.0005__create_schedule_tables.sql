CREATE SEQUENCE gym_schedule_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE gym_schedule (
    id INT DEFAULT nextval('gym_schedule_s') PRIMARY KEY,
    day VARCHAR(10) UNIQUE NOT NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL
);

CREATE SEQUENCE trainer_schedule_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE trainer_schedule (
    id INT DEFAULT nextval('trainer_schedule_s') PRIMARY KEY,
    trainer_id INT NOT NULL REFERENCES trainer(id),
    day VARCHAR(10) UNIQUE NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    status VARCHAR NOT NULL
);