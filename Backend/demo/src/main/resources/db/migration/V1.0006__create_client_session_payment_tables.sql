CREATE SEQUENCE client_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client (
    id INT DEFAULT nextval('client_s') PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(id),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);

CREATE SEQUENCE session_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE session (
    id INT DEFAULT nextval('session_s') PRIMARY KEY,
    type VARCHAR NOT NULL,
    max_participants INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL,
    CHECK (type <> 'INDIVIDUAL' OR max_participants = 1),
    CONSTRAINT unique_group_participants UNIQUE (type, max_participants)
);
CREATE UNIQUE INDEX unique_individual_type ON session(type) WHERE type = 'INDIVIDUAL';

INSERT INTO session (type, max_participants) VALUES
    ('INDIVIDUAL', 1),
    ('GROUP', 3),
    ('GROUP', 10);

CREATE SEQUENCE payment_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE payment (
    id INT DEFAULT nextval('payment_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    session_id INT NOT NULL REFERENCES session(id),
    paid_appointments INT NOT NULL,
    payment_date DATE NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL
);