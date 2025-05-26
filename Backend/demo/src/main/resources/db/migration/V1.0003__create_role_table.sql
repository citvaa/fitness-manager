CREATE TABLE role (
    name VARCHAR PRIMARY KEY
);

INSERT INTO role (name) VALUES ('MANAGER'), ('TRAINER'), ('CLIENT');

CREATE SEQUENCE user_role_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE user_role (
    id INT DEFAULT nextval('user_role_s') PRIMARY KEY,
    user_id INT REFERENCES "user"(id) NOT NULL,
    role VARCHAR REFERENCES role(name) NOT NULL,
    CONSTRAINT unique_user_role UNIQUE (user_id, role)
);

INSERT INTO user_role (user_id, role) VALUES (1, 'MANAGER')