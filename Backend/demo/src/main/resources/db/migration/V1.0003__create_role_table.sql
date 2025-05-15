CREATE SEQUENCE role_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE role (
    id INT DEFAULT nextval('role_s') PRIMARY KEY,
    name VARCHAR UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id INT REFERENCES "user"(id),
    role_id INT REFERENCES role(id),
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO role (name) VALUES ('MANAGER'), ('TRAINER'), ('USER');