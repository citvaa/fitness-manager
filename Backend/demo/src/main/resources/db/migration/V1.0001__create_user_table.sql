CREATE SEQUENCE user_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE "user" (
    id INT DEFAULT nextval('user_s') PRIMARY KEY,
    username VARCHAR UNIQUE,
    password VARCHAR,
    email VARCHAR UNIQUE,
    registration_key uuid,
    registration_key_validity TIMESTAMP,
    is_activated BOOLEAN,
    reset_token uuid,
    reset_token_validity TIMESTAMP,
    version INT NOT NULL
);