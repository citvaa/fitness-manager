CREATE SEQUENCE client_session_tracking_s
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE;

CREATE TABLE client_session_tracking (
    id INT DEFAULT nextval('client_session_tracking_s') PRIMARY KEY,
    client_id INT NOT NULL REFERENCES client(id),
    session_id INT NOT NULL REFERENCES session(id),
    remaining_appointments INT NOT NULL,
    reserved_appointments INT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by INT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by INT NULL,
    CONSTRAINT unique_client_session UNIQUE (client_id, session_id)
)