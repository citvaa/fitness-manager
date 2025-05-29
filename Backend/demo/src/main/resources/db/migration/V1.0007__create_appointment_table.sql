CREATE SEQUENCE appointment_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE appointment (
    id INT DEFAULT nextval('appointment_s') PRIMARY KEY,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session_id INT NOT NULL,
    trainer_id INT NULL
);

CREATE SEQUENCE client_appointment_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client_appointment (
    id INT DEFAULT nextval('client_appointment_s') PRIMARY KEY,
    client_id INT NOT NULL,
    appointment_id INT NOT NULL
);