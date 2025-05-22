CREATE SEQUENCE client_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client (
    id INT DEFAULT nextval('client_s') PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(id),
    remaining_sessions INT NOT NULL
);

CREATE SEQUENCE session_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE session (
    id INT DEFAULT nextval('session_s') PRIMARY KEY,
    type VARCHAR NOT NULL,
    max_participants INT NOT NULL
);

CREATE SEQUENCE payment_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE payment (
    id INT DEFAULT nextval('payment_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    session_id INT NOT NULL REFERENCES session(id),
    paid_sessions INT NOT NULL,
    payment_date DATE NOT NULL
);