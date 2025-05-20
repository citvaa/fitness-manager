CREATE SEQUENCE trainer_s
START WITH 1
INCREMENT BY 1
MINVALUE 1
NO MAXVALUE;

CREATE TABLE trainer (
    id INT DEFAULT nextval('trainer_s') PRIMARY KEY,
    user_id INT REFERENCES "user"(id) UNIQUE NOT NULL,
    employment_date DATE NOT NULL,
    birth_year INT NOT NULL,
    status VARCHAR NOT NULL
);