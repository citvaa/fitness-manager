CREATE TABLE role (
    name VARCHAR PRIMARY KEY
);

INSERT INTO role (name) VALUES ('MANAGER'), ('TRAINER'), ('USER');

CREATE SEQUENCE user_role_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE user_role (
    id INT DEFAULT nextval('user_role_s') PRIMARY KEY,
    user_id INT REFERENCES "user"(id) NOT NULL,
    role_name VARCHAR REFERENCES role(name) NOT NULL,
    CONSTRAINT unique_user_role UNIQUE (user_id, role_name)
);

INSERT INTO user_role (user_id, role_name) VALUES (1, 'MANAGER')