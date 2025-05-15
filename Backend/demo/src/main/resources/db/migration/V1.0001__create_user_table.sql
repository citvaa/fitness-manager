CREATE SEQUENCE user_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE "user" (
    id INT DEFAULT nextval('user_s') PRIMARY KEY,
    username VARCHAR UNIQUE NOT NULL,
    password VARCHAR NULL,
    email VARCHAR UNIQUE NOT NULL,
    registration_key VARCHAR NULL,
    registration_key_validity TIMESTAMP NULL,
    is_activated BOOLEAN NOT NULL,
    reset_key VARCHAR NULL,
    reset_key_validity TIMESTAMP NULL,
    version INT NOT NULL
);