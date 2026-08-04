-- Hand-written Envers audit tables for the entities added in V1.0011-V1.0013 (ddl-auto: none,
-- so these are never generated at runtime - see AGENTS.md "Audit" section).

CREATE TABLE gym_aud (
    id int4 NOT NULL,
    rev int4 NOT NULL,
    revtype int2 NULL,
    name varchar(255) NULL,
    address varchar(500) NULL,
    contact_email varchar(255) NULL,
    contact_phone varchar(50) NULL,
    logo_url varchar(500) NULL,
    primary_color varchar(20) NULL,
    timezone varchar(100) NULL,
    CONSTRAINT gym_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk_gym_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE room_aud (
    id int4 NOT NULL,
    rev int4 NOT NULL,
    revtype int2 NULL,
    gym_id int4 NULL,
    name varchar(255) NULL,
    type varchar(50) NULL,
    capacity int4 NULL,
    pos_x float8 NULL,
    pos_y float8 NULL,
    width float8 NULL,
    height float8 NULL,
    rotation_degrees float8 NULL,
    color varchar(20) NULL,
    CONSTRAINT room_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk_room_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE room_check_in_aud (
    id int4 NOT NULL,
    rev int4 NOT NULL,
    revtype int2 NULL,
    room_id int4 NULL,
    client_id int4 NULL,
    checked_in_at timestamp(6) NULL,
    checked_out_at timestamp(6) NULL,
    CONSTRAINT room_check_in_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk_room_check_in_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE client_progress_entry_aud (
    id int4 NOT NULL,
    rev int4 NOT NULL,
    revtype int2 NULL,
    client_id int4 NULL,
    entry_date date NULL,
    weight_kg numeric(6, 2) NULL,
    body_fat_percent numeric(5, 2) NULL,
    waist_cm numeric(6, 2) NULL,
    chest_cm numeric(6, 2) NULL,
    hip_cm numeric(6, 2) NULL,
    thigh_cm numeric(6, 2) NULL,
    arm_cm numeric(6, 2) NULL,
    notes varchar(2000) NULL,
    CONSTRAINT client_progress_entry_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk_client_progress_entry_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE client_personal_record_aud (
    id int4 NOT NULL,
    rev int4 NOT NULL,
    revtype int2 NULL,
    client_id int4 NULL,
    exercise_name varchar(255) NULL,
    value numeric(10, 2) NULL,
    unit varchar(20) NULL,
    record_date date NULL,
    notes varchar(1000) NULL,
    CONSTRAINT client_personal_record_aud_pkey PRIMARY KEY (rev, id),
    CONSTRAINT fk_client_personal_record_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

-- appointment_aud already exists (V1.0010) - the new nullable Appointment.room field needs the
-- matching column added by hand, same as any other @Audited column change (see AGENTS.md).
ALTER TABLE appointment_aud ADD COLUMN room_id int4 NULL;
