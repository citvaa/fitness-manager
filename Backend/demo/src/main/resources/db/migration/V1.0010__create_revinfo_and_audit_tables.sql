CREATE SEQUENCE revinfo_seq
    INCREMENT BY 50
    MINVALUE 1
    MAXVALUE 9223372036854775807
    START 1
    CACHE 1
    NO CYCLE;

CREATE TABLE revinfo (
    rev int4 NOT NULL,
    revtstmp int8 NULL,
    CONSTRAINT revinfo_pkey PRIMARY KEY (rev)
);

CREATE TABLE user_aud (
                          id int4 NOT NULL,
                          rev int4 NOT NULL,
                          revtype int2 NULL,
                          email varchar(255) NULL,
                          is_activated bool NULL,
                          notification_preference varchar(255) NULL,
                          "password" varchar(255) NULL,
                          registration_key varchar(255) NULL,
                          registration_key_validity timestamp(6) NULL,
                          reset_key varchar(255) NULL,
                          reset_key_validity timestamp(6) NULL,
                          CONSTRAINT user_aud_notification_preference_check CHECK (((notification_preference)::text = ANY ((ARRAY['EMAIL'::character varying, 'PUSH'::character varying, 'BOTH'::character varying])::text[]))),
                          CONSTRAINT user_aud_pkey PRIMARY KEY (rev, id),
                          CONSTRAINT fk89ntto9kobwahrwxbne2nqcnr FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE TABLE public.user_role_aud (
                                      id int4 NOT NULL,
                                      rev int4 NOT NULL,
                                      revtype int2 NULL,
                                      "role" varchar(255) NULL,
                                      user_id int4 NULL,
                                      CONSTRAINT user_role_aud_pkey PRIMARY KEY (rev, id),
                                      CONSTRAINT user_role_aud_role_check CHECK (((role)::text = ANY ((ARRAY['MANAGER'::character varying, 'TRAINER'::character varying, 'CLIENT'::character varying])::text[]))),
                                      CONSTRAINT fk2ax4xks5sy1yh2a2gxdndkcmc FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.trainer_aud (
                                    id int4 NOT NULL,
                                    rev int4 NOT NULL,
                                    revtype int2 NULL,
                                    birth_year int4 NULL,
                                    employment_date date NULL,
                                    status varchar(255) NULL,
                                    user_id int4 NULL,
                                    CONSTRAINT trainer_aud_pkey PRIMARY KEY (rev, id),
                                    CONSTRAINT trainer_aud_status_check CHECK (((status)::text = ANY ((ARRAY['FULL_TIME'::character varying, 'CONTRACT'::character varying, 'FORMER_EMPLOYEE'::character varying])::text[]))),
                                    CONSTRAINT fkbpiua3s3kxy9yjhq5m30pid1q FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.gym_schedule_aud (
                                         id int4 NOT NULL,
                                         rev int4 NOT NULL,
                                         revtype int2 NULL,
                                         closing_time time(6) NULL,
                                         "day" varchar(255) NULL,
                                         opening_time time(6) NULL,
                                         CONSTRAINT gym_schedule_aud_day_check CHECK (((day)::text = ANY ((ARRAY['MONDAY'::character varying, 'TUESDAY'::character varying, 'WEDNESDAY'::character varying, 'THURSDAY'::character varying, 'FRIDAY'::character varying, 'SATURDAY'::character varying, 'SUNDAY'::character varying])::text[]))),
                                         CONSTRAINT gym_schedule_aud_pkey PRIMARY KEY (rev, id),
                                         CONSTRAINT fkskrlqtcemw54l8oopcijho8s9 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE holiday_aud (
                             id int4 NOT NULL,
                             rev int4 NOT NULL,
                             revtype int2 NULL,
                             "date" date NULL,
                             description varchar(255) NULL,
                             CONSTRAINT holiday_aud_pkey PRIMARY KEY (rev, id),
                             CONSTRAINT fk9sii8g68wggngh19kjyn4m4k4 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.trainer_schedule_aud (
                                             id int4 NOT NULL,
                                             rev int4 NOT NULL,
                                             revtype int2 NULL,
                                             "date" date NULL,
                                             end_time time(6) NULL,
                                             start_time time(6) NULL,
                                             status varchar(255) NULL,
                                             trainer_id int4 NULL,
                                             CONSTRAINT trainer_schedule_aud_pkey PRIMARY KEY (rev, id),
                                             CONSTRAINT trainer_schedule_aud_status_check CHECK (((status)::text = ANY ((ARRAY['WORKING'::character varying, 'HOLIDAY'::character varying, 'SICK_LEAVE'::character varying, 'VACATION'::character varying])::text[]))),
                                             CONSTRAINT fkmvliw3knp1floy4x7xvyspuhv FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.client_aud (
                                   id int4 NOT NULL,
                                   rev int4 NOT NULL,
                                   revtype int2 NULL,
                                   user_id int4 NULL,
                                   CONSTRAINT client_aud_pkey PRIMARY KEY (rev, id),
                                   CONSTRAINT fkq7rlntwn6l0k20fxnu2ro82h6 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.session_aud (
                                    id int4 NOT NULL,
                                    rev int4 NOT NULL,
                                    revtype int2 NULL,
                                    max_participants int4 NULL,
                                    "type" varchar(255) NULL,
                                    CONSTRAINT session_aud_pkey PRIMARY KEY (rev, id),
                                    CONSTRAINT session_aud_type_check CHECK (((type)::text = ANY ((ARRAY['INDIVIDUAL'::character varying, 'GROUP'::character varying])::text[]))),
                                    CONSTRAINT fk3lb63cx70ekx6n4dbor8xauh0 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.payment_aud (
                                    id int4 NOT NULL,
                                    rev int4 NOT NULL,
                                    revtype int2 NULL,
                                    paid_appointments int4 NULL,
                                    payment_date date NULL,
                                    client_id int4 NULL,
                                    session_id int4 NULL,
                                    CONSTRAINT payment_aud_pkey PRIMARY KEY (rev, id),
                                    CONSTRAINT fk1uumpbl0vkiohnbo3v2pp4qsj FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.appointment_aud (
                                        id int4 NOT NULL,
                                        rev int4 NOT NULL,
                                        revtype int2 NULL,
                                        "date" date NULL,
                                        end_time time(6) NULL,
                                        start_time time(6) NULL,
                                        session_id int4 NULL,
                                        trainer_id int4 NULL,
                                        CONSTRAINT appointment_aud_pkey PRIMARY KEY (rev, id),
                                        CONSTRAINT fklmkpkowitkecxb2u8obk10e56 FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.client_appointment_aud (
                                               id int4 NOT NULL,
                                               rev int4 NOT NULL,
                                               revtype int2 NULL,
                                               appointment_id int4 NULL,
                                               client_id int4 NULL,
                                               CONSTRAINT client_appointment_aud_pkey PRIMARY KEY (rev, id),
                                               CONSTRAINT fkqk50bgso8oqnhkr33nysva13y FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);

CREATE TABLE public.client_session_tracking_aud (
                                                    id int4 NOT NULL,
                                                    rev int4 NOT NULL,
                                                    revtype int2 NULL,
                                                    remaining_appointments int4 NULL,
                                                    reserved_appointments int4 NULL,
                                                    client_id int4 NULL,
                                                    session_id int4 NULL,
                                                    CONSTRAINT client_session_tracking_aud_pkey PRIMARY KEY (rev, id),
                                                    CONSTRAINT fkestcv5ye1djhv2wbfefx56vqm FOREIGN KEY (rev) REFERENCES public.revinfo(rev)
);